package fr.project.planning.scenarios.builder;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scenarios.alerte.AlertCode;
import fr.project.planning.scenarios.alerte.AlertSeverity;
import fr.project.planning.scenarios.alerte.ScenarioAlert;
import fr.project.planning.scenarios.dto.DataSetDTO;
import fr.project.planning.scenarios.dto.ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.input.SalarieInputDTO;
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
 * - dimanche non travaillé : RHD (repos dominical)
 * - samedi non travaillé : RH (repos hebdomadaire accolé)
 * - week-end entièrement travaillé : RH reporté sur le 1er jour non coché
 * - autres jours non cochés : NON_TRAVAILLE (horaire contractuel, pas un repos hebdomadaire)
 * - 0 jour non coché : alerte repos insuffisant
 *
 * Les jours fériés sont non travaillés.
 */
public class ScenarioDatasetBuilderSc01 {

    /**
     * Code activité appliqué aux créneaux générés lorsque l'appelant n'en déclare aucun.
     *
     * Repli historique : SC-01 a longtemps imposé ce code. Il est conservé pour ne pas rompre
     * les intégrations existantes, mais son emploi est signalé au client (ACTIVITY_CODE_DEFAULTED)
     * — le moteur ne doit pas nommer une donnée métier à la place de l'appelant.
     */
    static final String CODE_ACTIVITE_DEFAUT = "travail";

    // =========================
    // API publique
    // =========================

    public BuildResult build(BuildRequest req) {
        Objects.requireNonNull(req, "req");
        validateRequest(req);

        List<ScenarioAlert> alerts = new ArrayList<>();
        List<Creneau> generated = new ArrayList<>();

        // Code activité porté par les créneaux générés : déclaré par l'appelant, sinon défaut.
        String codeActivite = resolveCodeActivite(req.codeActiviteId, alerts);

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
                continue;
            }

            // 2) Repos hebdo (RH/RHD) selon règle "jours non cochés"
            QualificationJour restQ = weeklyRestQualification.get(date);
            if (restQ == QualificationJour.RH || restQ == QualificationJour.RHD) {
                continue;
            }

            // 3) Jour travaillé ?
            if (!req.workedDays.contains(dow)) {
                continue;
            }

            // 4) Génération des créneaux matin/aprem
            LocalTime shiftStart = req.shiftStart;
            LocalTime finPrevue = shiftStart.plusMinutes(req.dailyAmplitudeMinutes);

            // Alerte dépassement fin de poste
            if (finPrevue.isAfter(req.shiftEndAlert)) {
                alerts.add(new ScenarioAlert(
                        AlertCode.SHIFT_END_EXCEEDED,
                        AlertSeverity.WARNING,
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
                        AlertSeverity.WARNING,
                        date,
                        "Pause midi incohérente avec l'amplitude : génération d'un seul créneau (" +
                                shiftStart + " -> " + finPrevue + ")."
                ));

                generated.add(createCreneau(
                        codeActivite, seq.getAndIncrement(), date,
                        shiftStart, finPrevue,
                        TypePlageHoraire.JOUR,
                        false,
                        QualificationJour.OUVRE
                ));
            } else {
                // Matin
                generated.add(createCreneau(
                        codeActivite, seq.getAndIncrement(), date,
                        shiftStart, lunchStart,
                        TypePlageHoraire.JOUR,
                        false,
                        QualificationJour.OUVRE
                ));
                // Après-midi
                generated.add(createCreneau(
                        codeActivite, seq.getAndIncrement(), date,
                        lunchEnd, finPrevue,
                        TypePlageHoraire.JOUR,
                        false,
                        QualificationJour.OUVRE
                ));
            }
        }

        emitUnknownActivityAlert(codeActivite, req.referentiel, generated.size(), alerts);

        return new BuildResult(generated, alerts);
    }

    /**
     * Détermine le code activité porté par les créneaux générés.
     *
     * L'appelant est la source de vérité : c'est lui qui sait quelle activité le planning
     * représente, et c'est son vocabulaire qui devra permettre de réintégrer le résultat.
     * À défaut, le moteur applique son code historique et le signale — un repli silencieux
     * imposerait au client un code qu'il n'a pas choisi.
     */
    private String resolveCodeActivite(String codeActiviteDeclare, List<ScenarioAlert> alerts) {
        if (codeActiviteDeclare != null && !codeActiviteDeclare.isBlank()) {
            return codeActiviteDeclare;
        }

        alerts.add(new ScenarioAlert(
                AlertCode.ACTIVITY_CODE_DEFAULTED,
                AlertSeverity.WARNING,
                null,
                "Aucun code activité déclaré (scenarioParameters.codeActiviteId) : les créneaux "
                        + "générés portent le code par défaut '" + CODE_ACTIVITE_DEFAUT + "', "
                        + "qui doit être déclaré dans dataSet.referentiels.activites."
        ));

        return CODE_ACTIVITE_DEFAUT;
    }

    /**
     * Alerte si le code activité stampé sur les créneaux générés est absent du référentiel injecté.
     *
     * Sans cette alerte, la situation est silencieuse pour le client : le lookup échoue,
     * les créneaux ne comptent pas dans la charge, les WorkMetrics tombent à zéro et les
     * contraintes métier restent inertes — sans que rien ne le signale dans la réponse.
     *
     * L'alerte porte sur le dataset et non sur un jour : elle n'a pas de date.
     */
    private void emitUnknownActivityAlert(
            String codeActivite,
            ReferentielComptabiliteActivite referentiel,
            int nbCreneauxGeneres,
            List<ScenarioAlert> alerts
    ) {
        if (referentiel == null || nbCreneauxGeneres == 0) {
            return;
        }
        if (referentiel.contient(codeActivite)) {
            return;
        }

        alerts.add(new ScenarioAlert(
                AlertCode.UNKNOWN_ACTIVITY,
                AlertSeverity.ERROR,
                null,
                "Activité '" + codeActivite + "' absente du référentiel fourni "
                        + "(dataSet.referentiels.activites) : les " + nbCreneauxGeneres
                        + " créneaux générés ne compteront pas dans la charge."
        ));
    }

    // =========================
    // Construction Creneau
    // =========================

    private Creneau createCreneau(
            String codeActivite,
            int sequence,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            TypePlageHoraire plage,
            boolean jourFerie,
            QualificationJour qualificationJour
    ) {
        int dureeMinutes = minutesBetween(start, end);

        String id = "SC01-" + date + "-" + String.format("%03d", sequence);

        Creneau c = new Creneau(
                id,
                date,
                start,
                end,
                dureeMinutes,
                null,                 // lieu
                codeActivite,         // codeActiviteId — clé de lookup référentiel
                codeActivite,         // activite (fallback legacy)
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

        List<DayOfWeek> nonWorked = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            if (!workedDays.contains(d)) {
                nonWorked.add(d);
            }
        }
        nonWorked.sort(Comparator.comparingInt(DayOfWeek::getValue));

        // La semaine type est identique sur tout l'horizon : on la qualifie une seule fois.
        Map<DayOfWeek, QualificationJour> semaineType = qualifyNonWorkedDays(nonWorked);

        LocalDate cursor = debut;

        while (!cursor.isAfter(fin)) {
            LocalDate weekStart = cursor.with(DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);

            LocalDate blockStart = weekStart.isBefore(debut) ? debut : weekStart;
            LocalDate blockEnd = weekEnd.isAfter(fin) ? fin : weekEnd;

            for (LocalDate d = blockStart; !d.isAfter(blockEnd); d = d.plusDays(1)) {
                QualificationJour q = semaineType.get(d.getDayOfWeek());
                if (q != null) {
                    result.put(d, q);
                }
            }

            cursor = weekEnd.plusDays(1);
        }

        // Alertes émises une seule fois : elles portent sur la configuration `workedDays`,
        // pas sur une semaine particulière de l'horizon.
        emitWeeklyRestAlerts(debut, nonWorked, semaineType, alerts);

        return result;
    }

    /**
     * Qualifie les jours non cochés de la semaine type.
     *
     * Le repos dominical (RHD) est par définition le dimanche ; le repos hebdomadaire (RH)
     * lui est accolé, donc le samedi lorsqu'il est non travaillé. Si le week-end est
     * entièrement travaillé, le repos hebdomadaire est reporté sur le premier jour non coché.
     *
     * Les jours non cochés restants relèvent de l'horaire contractuel (temps partiel,
     * horaire réduit) et non du repos hebdomadaire : ils sont qualifiés NON_TRAVAILLE.
     */
    private Map<DayOfWeek, QualificationJour> qualifyNonWorkedDays(List<DayOfWeek> nonWorkedSorted) {
        Map<DayOfWeek, QualificationJour> qualification = new EnumMap<>(DayOfWeek.class);

        if (nonWorkedSorted.isEmpty()) {
            return qualification;
        }

        for (DayOfWeek d : nonWorkedSorted) {
            qualification.put(d, QualificationJour.NON_TRAVAILLE);
        }

        boolean samediRepos = qualification.containsKey(DayOfWeek.SATURDAY);
        boolean dimancheRepos = qualification.containsKey(DayOfWeek.SUNDAY);

        if (dimancheRepos) {
            qualification.put(DayOfWeek.SUNDAY, QualificationJour.RHD);
        }
        if (samediRepos) {
            qualification.put(DayOfWeek.SATURDAY, QualificationJour.RH);
        }
        if (!samediRepos && !dimancheRepos) {
            qualification.put(nonWorkedSorted.get(0), QualificationJour.RH);
        }

        return qualification;
    }

    /**
     * Alertes liées au repos hebdomadaire.
     *
     * Seule l'impossibilité de qualifier un repos hebdomadaire est une anomalie.
     * Un volume de jours non cochés supérieur au repos hebdomadaire est une information
     * de configuration (temps partiel), pas un défaut.
     */
    private void emitWeeklyRestAlerts(
            LocalDate horizonDebut,
            List<DayOfWeek> nonWorked,
            Map<DayOfWeek, QualificationJour> semaineType,
            List<ScenarioAlert> alerts
    ) {
        if (nonWorked.isEmpty()) {
            alerts.add(new ScenarioAlert(
                    AlertCode.INSUFFICIENT_WEEKLY_REST,
                    AlertSeverity.ERROR,
                    horizonDebut,
                    "Aucun jour de repos configuré sur une semaine (0 jour non coché) : "
                            + "repos hebdomadaire impossible à qualifier."
            ));
            return;
        }

        List<DayOfWeek> horsRepos = nonWorked.stream()
                .filter(d -> semaineType.get(d) == QualificationJour.NON_TRAVAILLE)
                .toList();

        if (!horsRepos.isEmpty()) {
            String jours = String.join(", ", horsRepos.stream().map(d -> d.name()).toList());
            alerts.add(new ScenarioAlert(
                    AlertCode.TOO_MANY_NON_WORKED_DAYS,
                    AlertSeverity.INFO,
                    horizonDebut,
                    "Jours non travaillés au-delà du repos hebdomadaire : " + jours
                            + ". Horaire réduit ou temps partiel — configuration valide."
            ));
        }
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
        br.codeActiviteId = params.getCodeActiviteId();

        return build(br);
    }

    /**
     * Résolution de la ressource (salarié) à partir du ResourceRefDTO.
     *
     * Phase 1 : construit un SalarieReel minimal depuis le SalarieInputDTO.
     * Les nouveaux champs (contraintesReglementaires, axesOrganisationnels, etc.)
     * seront mappés vers le domaine en Phase 3.
     */
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

        SalarieInputDTO dto = dataSet.getRessources()
                .getSalaries()
                .stream()
                .filter(s -> ref.getId().equals(s.getId()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Ressource introuvable : " + ref.getId()
                        ));

        // Phase 1 : mapping minimal — les nouveaux champs sont transportés mais pas encore mappés.
        return new SalarieReel(
                dto.getId(),
                null,  // profilContractuel — Phase 3
                dto.getStatut(),
                dto.getSitesAutorises() != null ? dto.getSitesAutorises() : Set.of(),
                dto.getActivitesCompatibles() != null ? dto.getActivitesCompatibles() : Set.of(),
                dto.getPostesComptablesCompatibles() != null ? dto.getPostesComptablesCompatibles() : Set.of()
        );
    }

    // =========================
    // Types internes (simples, MVP)
    // =========================

    public static class BuildRequest {
        public LocalDate dateDebut;
        public LocalDate dateFin;

        public Ressource ressource;

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

        /**
         * Code activité porté par les créneaux générés — optionnel.
         *
         * Déclaré par l'appelant dans le vocabulaire de son propre référentiel, afin que le
         * résultat lui soit réintégrable sans table de correspondance. Absent, le moteur
         * applique {@link #CODE_ACTIVITE_DEFAUT} et l'annonce dans ses alertes.
         */
        public String codeActiviteId;

        /**
         * Référentiel d'activités injecté — optionnel.
         *
         * Sert uniquement à vérifier que le code activité stampé sur les créneaux générés
         * y est déclaré. Null désactive la vérification : le builder ne suppose pas qu'un
         * référentiel lui soit fourni.
         */
        public ReferentielComptabiliteActivite referentiel;

        /** jours fériés dans la période (non travaillés) */
        public Set<LocalDate> holidayDates;
    }

    public record BuildResult(List<Creneau> creneaux, List<ScenarioAlert> alerts) {}
}
