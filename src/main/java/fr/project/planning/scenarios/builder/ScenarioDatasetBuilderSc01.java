package fr.project.planning.scenarios.builder;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.scenarios.dto.DataSetDTO;
import fr.project.planning.scenarios.dto.ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.request.ResourceRefDTO;
import fr.project.planning.scenarios.dto.request.Sc01ScenarioParametersDTO;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builder SC-01
 *
 * Construit le dataset de créneaux à partir de paramètres utilisateur :
 * - période
 * - ressource (salarié ou poste virtuel)
 * - amplitude journalière (incluant la pause)
 * - horaires de poste + borne d'alerte fin de poste
 * - pause midi optionnelle (défaut 12:00-13:00)
 * - jours travaillés (cases cochées MON..SUN)
 * - jours fériés (liste de dates, non travaillés)
 *
 * Règles RH/RHD (par bloc lun->dim) :
 * - 2 jours non cochés : 1er RH, 2e RHD
 * - 1 jour non coché : RHD
 * - 0 jour non coché : alerte repos insuffisant
 *
 * Les jours fériés sont non travaillés.
 */
public class ScenarioDatasetBuilderSc01 {

    // =========================
    // API publique
    // =========================

    public BuildResult build(BuildRequest req) {
        Objects.requireNonNull(req, "req");
        validateRequest(req);

        List<ScenarioAlert> alerts = new ArrayList<>();
        List<Creneau> generated = new ArrayList<>();

        // Prépare la qualification RH/RHD par semaine (lun->dim) en fonction des jours NON cochés
        Map<LocalDate, QualificationJour> weeklyRestQualification =
                computeWeeklyRestQualification(req.dateDebut, req.dateFin, req.workedDays, alerts);

        // Parcours de la période jour par jour
        long nbDays = ChronoUnit.DAYS.between(req.dateDebut, req.dateFin) + 1;
        AtomicInteger seq = new AtomicInteger(1);

        for (int i = 0; i < nbDays; i++) {
            LocalDate date = req.dateDebut.plusDays(i);
            DayOfWeek dow = date.getDayOfWeek();

            // 1) Férié => non travaillé
            if (req.holidayDates.contains(date)) {
                // On ne génère aucun créneau, mais on peut qualifier la journée.
                // (La qualification est portée sur les créneaux dans votre modèle, donc on n'a rien à instancier.)
                continue;
            }

            // 2) Repos hebdo (RH/RHD) selon règle "jours non cochés"
            QualificationJour restQ = weeklyRestQualification.get(date);
            if (restQ == QualificationJour.RH || restQ == QualificationJour.RHD) {
                // Jour de repos => aucun créneau
                continue;
            }

            // 3) Jour travaillé ?
            if (!req.workedDays.contains(dow)) {
                // Jour non coché au-delà des 1-2 jours gérés explicitement : on le traite comme repos (pas de créneau).
                // (Optionnel : alerte si vous voulez)
                continue;
            }

            // 4) Génération des créneaux matin/aprem
            LocalTime shiftStart = req.shiftStart;
            LocalTime finPrevue = shiftStart.plusMinutes(req.dailyAmplitudeMinutes);

            // Alerte dépassement fin de poste
            if (finPrevue.isAfter(req.shiftEndAlert)) {
                alerts.add(new ScenarioAlert(
                        AlertCode.SHIFT_END_EXCEEDED,
                        date,
                        "Fin prévue (" + finPrevue + ") au-delà de la borne d'alerte (" + req.shiftEndAlert + ")."
                ));
            }

            // Pause (défaut 12:00-13:00 si non fournie)
            LocalTime lunchStart = req.lunchBreakStart != null ? req.lunchBreakStart : LocalTime.of(12, 0);
            LocalTime lunchEnd = req.lunchBreakEnd != null ? req.lunchBreakEnd : LocalTime.of(13, 0);

            boolean canSplit =
                    lunchEnd.isAfter(lunchStart)
                    && !lunchStart.isBefore(shiftStart)
                    && !lunchEnd.isAfter(finPrevue)
                    && finPrevue.isAfter(lunchStart)
                    && finPrevue.isAfter(lunchEnd);

            if (!canSplit) {
                alerts.add(new ScenarioAlert(
                        AlertCode.LUNCH_BREAK_OUTSIDE_AMPLITUDE,
                        date,
                        "Pause midi incohérente avec l'amplitude : génération d'un seul créneau (" +
                                shiftStart + " -> " + finPrevue + ")."
                ));

                generated.add(createCreneau(
                        req, seq.getAndIncrement(), date,
                        shiftStart, finPrevue,
                        TypePlageHoraire.JOUR,
                        false,
                        QualificationJour.OUVRE
                ));
            } else {
                // Matin
                generated.add(createCreneau(
                        req, seq.getAndIncrement(), date,
                        shiftStart, lunchStart,
                        TypePlageHoraire.JOUR,
                        false,
                        QualificationJour.OUVRE
                ));
                // Après-midi
                generated.add(createCreneau(
                        req, seq.getAndIncrement(), date,
                        lunchEnd, finPrevue,
                        TypePlageHoraire.JOUR,
                        false,
                        QualificationJour.OUVRE
                ));
            }
        }

        return new BuildResult(generated, alerts);
    }

    // =========================
    // Construction Creneau
    // =========================

    private Creneau createCreneau(
            BuildRequest req,
            int sequence,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            TypePlageHoraire plage,
            boolean jourFerie,
            QualificationJour qualificationJour
    ) {
        int dureeMinutes = minutesBetween(start, end);

        // NB: votre constructeur Creneau prend beaucoup de champs.
        // Ici on met null sur ce qui n'est pas utilisé en SC-01 MVP (lieu, posteComptable, priorite).
        String id = "SC01-" + date + "-" + String.format("%03d", sequence);

        Creneau c = new Creneau(
                id,
                date,
                start,
                end,
                dureeMinutes,
                null,                 // lieu
                "travail",            // activite (V1)
                null,                 // posteComptable
                null,                 // priorite (PrioriteCreneau) -> null pour MVP
                TypeCreneau.GENERE,
                plage,
                jourFerie,
                qualificationJour
        );

        c.setRessourceAffectee(RessourceNonAffectee.INSTANCE);
        return c;
    }

    private int minutesBetween(LocalTime start, LocalTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 0) {
            // si jamais un créneau passe minuit (rare en SC-01), on corrige
            minutes += 24 * 60;
        }
        return (int) minutes;
    }

    // =========================
    // Qualification RH/RHD (par semaine lun->dim)
    // =========================

    private Map<LocalDate, QualificationJour> computeWeeklyRestQualification(
            LocalDate debut,
            LocalDate fin,
            Set<DayOfWeek> workedDays,
            List<ScenarioAlert> alerts
    ) {
        Map<LocalDate, QualificationJour> result = new HashMap<>();

        // Découpe en blocs "lun->dim". On part du lundi de la semaine de debut.
        LocalDate cursor = debut;

        while (!cursor.isAfter(fin)) {
            LocalDate weekStart = cursor.with(DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);

            // borne réelle dans la période
            LocalDate blockStart = weekStart.isBefore(debut) ? debut : weekStart;
            LocalDate blockEnd = weekEnd.isAfter(fin) ? fin : weekEnd;

            // Jours non cochés dans la semaine (lun->dim)
            List<DayOfWeek> nonWorked = new ArrayList<>();
            for (DayOfWeek d : DayOfWeek.values()) {
                if (!workedDays.contains(d)) {
                    nonWorked.add(d);
                }
            }
            nonWorked.sort(Comparator.comparingInt(DayOfWeek::getValue)); // MON=1 .. SUN=7

            // Alerte si repos insuffisant (0 jour non coché)
            if (nonWorked.isEmpty()) {
                alerts.add(new ScenarioAlert(
                        AlertCode.INSUFFICIENT_WEEKLY_REST,
                        blockStart,
                        "Aucun jour de repos configuré sur une semaine (0 jour non coché)."
                ));
            }

            // Appliquer règle RH/RHD sur le bloc
            //  - 1 jour non coché => RHD
            //  - 2 jours non cochés => RH puis RHD
            //  - >2 => RH, RHD, puis RH... (et alerte)
            if (nonWorked.size() > 2) {
                alerts.add(new ScenarioAlert(
                        AlertCode.TOO_MANY_NON_WORKED_DAYS,
                        blockStart,
                        "Plus de 2 jours non cochés dans la semaine : configuration atypique (" + nonWorked.size() + ")."
                ));
            }

            // Marquage par date dans le bloc
            for (LocalDate d = blockStart; !d.isAfter(blockEnd); d = d.plusDays(1)) {
                DayOfWeek dow = d.getDayOfWeek();
                if (!workedDays.contains(dow)) {
                    QualificationJour q = mapNonWorkedDayToQualification(nonWorked, dow);
                    result.put(d, q);
                }
            }

            // avancer au bloc suivant
            cursor = weekEnd.plusDays(1);
        }

        return result;
    }

    private QualificationJour mapNonWorkedDayToQualification(List<DayOfWeek> nonWorkedSorted, DayOfWeek dow) {
        // 1 jour non coché => RHD
        if (nonWorkedSorted.size() == 1) {
            return QualificationJour.RHD;
        }
        // 2 jours => 1er RH, 2e RHD
        if (nonWorkedSorted.size() >= 2) {
            DayOfWeek first = nonWorkedSorted.get(0);
            DayOfWeek second = nonWorkedSorted.get(1);
            if (dow == first) return QualificationJour.RH;
            if (dow == second) return QualificationJour.RHD;
            // au-delà : RH par défaut
            return QualificationJour.RH;
        }
        // 0 jour non coché : pas de qualification (cas géré par alerte)
        return QualificationJour.OUVRE;
    }

    // =========================
    // Validation
    // =========================

    private void validateRequest(BuildRequest req) {
        if (req.dateDebut == null || req.dateFin == null) {
            throw new IllegalArgumentException("dateDebut/dateFin sont requis.");
        }
        if (req.dateFin.isBefore(req.dateDebut)) {
            throw new IllegalArgumentException("dateFin ne peut pas être avant dateDebut.");
        }
        if (req.ressource == null) {
            throw new IllegalArgumentException("ressource est requise.");
        }
        if (req.dailyAmplitudeHours <= 0) {
            throw new IllegalArgumentException("dailyAmplitudeHours doit être > 0.");
        }
        if (req.shiftStart == null || req.shiftEndAlert == null) {
            throw new IllegalArgumentException("shiftStart/shiftEndAlert sont requis.");
        }
        if (req.workedDays == null || req.workedDays.isEmpty()) {
            throw new IllegalArgumentException("workedDays doit contenir au moins 1 jour.");
        }
        // Calcul minutes amplitude
        req.dailyAmplitudeMinutes = (int) Math.round(req.dailyAmplitudeHours * 60.0);

        if (req.holidayDates == null) req.holidayDates = Set.of();
    }

    // =========================
    // API de construction à partir du DTO de requête (SC-01)
    // =========================

    public BuildResult buildFromScenarioRequest(ScenarioRequestDTO request) {

        Objects.requireNonNull(request, "request");

        Sc01ScenarioParametersDTO params = request.getScenarioParameters();

        BuildRequest br = new BuildRequest();

        br.dateDebut = request.getPlanningContext().getHorizon().getDateDebut();
        br.dateFin = request.getPlanningContext().getHorizon().getDateFin();

        br.ressource = resolveResource(
                params.getResourceRef(),
                request.getDataSet()
        );

        br.dailyAmplitudeHours = params.getDailyAmplitudeHours();
        br.shiftStart = params.getShiftStart();
        br.shiftEndAlert = params.getShiftEndAlert();

        if (params.getLunchBreak() != null) {
            br.lunchBreakStart = params.getLunchBreak().getStart();
            br.lunchBreakEnd = params.getLunchBreak().getEnd();
        }

        br.workedDays = params.getWorkedDays();
        br.holidayDates = params.getHolidayDates();

        return build(br);
    }

    // Résolution de la ressource (salarié ou poste virtuel) à partir du ResourceRefDTO
    private Ressource resolveResource(ResourceRefDTO ref, DataSetDTO dataSet) {

        if (ref == null) {
            throw new IllegalArgumentException("resourceRef requis");
        }

        if (dataSet == null) {
            throw new IllegalArgumentException("dataSet requis");
        }

        if (dataSet.getRessources() == null) {
            throw new IllegalArgumentException("dataSet.ressources requis");
        }

        return dataSet.getRessources()
                .getSalaries()
                .stream()
                .filter(r -> r.getId().equals(ref.getId()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Ressource introuvable : " + ref.getId()
                        ));
    }

    // =========================
    // Types internes (simples, MVP)
    // =========================

    public static class BuildRequest {
        public LocalDate dateDebut;
        public LocalDate dateFin;

        public Ressource ressource; // salarié ou poste virtuel (tous 2 héritent de Ressource)

        /** amplitude journalière incluant pause (en heures, ex: 7.5) */
        public double dailyAmplitudeHours;

        /** calculé lors de validateRequest() */
        private int dailyAmplitudeMinutes;

        public LocalTime shiftStart;
        public LocalTime shiftEndAlert;

        /** optionnel : si null => 12:00-13:00 */
        public LocalTime lunchBreakStart;
        public LocalTime lunchBreakEnd;

        /** jours cochés (MON..SUN) */
        public Set<DayOfWeek> workedDays;

        /** jours fériés dans la période (non travaillés) */
        public Set<LocalDate> holidayDates;
    }

    public record BuildResult(List<Creneau> creneaux, List<ScenarioAlert> alerts) {}

    public record ScenarioAlert(AlertCode code, LocalDate date, String message) {}

    public enum AlertCode {
        SHIFT_END_EXCEEDED,
        LUNCH_BREAK_OUTSIDE_AMPLITUDE,
        INSUFFICIENT_WEEKLY_REST,
        TOO_MANY_NON_WORKED_DAYS
    }
}