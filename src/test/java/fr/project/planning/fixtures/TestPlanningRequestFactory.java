package fr.project.planning.fixtures;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.domain.contexte.HypotheseHistorique;
import fr.project.planning.domain.contexte.ObjectifResolution;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.ResolutionType;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.ressource.TypePosteVirtuel;
import fr.project.planning.scoring.StrategieScoring;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Factory de PlanningRequest pour les tests service (sans couche HTTP).
 * Construit un dataset minimal mais réaliste pour SC-01.
 */
public final class TestPlanningRequestFactory {

    // -----------------------
    // Constantes SC-01
    // -----------------------

    public static final LocalDate SC01_DATE_DEBUT = LocalDate.of(2026, 5, 11);
    public static final LocalDate SC01_DATE_FIN = LocalDate.of(2026, 5, 17);
    public static final LocalDate SC01_JOUR_FERIE = LocalDate.of(2026, 5, 13);

    public static final LocalTime SC01_SHIFT_START = LocalTime.of(8, 0);
    public static final double SC01_DAILY_AMPLITUDE_HOURS = 8.0; // inclut pause (conforme builder SC-01)
    public static final LocalTime SC01_LUNCH_START = LocalTime.of(12, 0);
    public static final LocalTime SC01_LUNCH_END = LocalTime.of(13, 0);

    public static final String ID_SALARIE_1041 = "1041";
    public static final String ID_SALARIE_1042 = "1042";
    public static final String ID_POSTE_VIRTUEL = "PV-001";

    public static final String LIEU_CASTRES = "CASTRES";
    public static final String POSTE_COMPTABLE_PC_100 = "PC-100";

    /** Aligné sur ScenarioController (référentiel map.put("travail", ...)) */
    public static final String ACTIVITE_TRAVAIL = "travail";

    private TestPlanningRequestFactory() {
        // utilitaire statique
    }

    // -----------------------
    // Point d'entrée
    // -----------------------

    public static PlanningRequest sc01Minimal() {

        PlanningContext context = buildPlanningContextSc01();
        RegulatoryParameters regulatory = buildRegulatoryParametersSc01();
        ReferentielComptabiliteActivite referentiel = buildReferentielSc01();

        List<Ressource> ressources = new ArrayList<>();
        ressources.add(buildSalarie(ID_SALARIE_1041));
        ressources.add(buildSalarie(ID_SALARIE_1042));
        ressources.add(buildPosteVirtuel(ID_POSTE_VIRTUEL));
        // IMPORTANT : doit être dans le range OptaPlanner (cf. ScenarioController)
        ressources.add(RessourceNonAffectee.INSTANCE);

        List<Creneau> creneaux = buildCreneauxSc01();

        return new PlanningRequest(
                context,
                regulatory,
                referentiel,
                ressources,
                creneaux
        );
    }

    // -----------------------
    // Helpers demandés
    // -----------------------

    public static PlanningContext buildPlanningContextSc01() {
        return new PlanningContext(
                ObjectifResolution.ANALYSER_LE_MANQUE,
                StrategieScoring.EXPLOITATION,
                SC01_DATE_DEBUT,
                SC01_DATE_FIN,
                ResolutionType.PLANNING_GLOBAL,
                HypotheseHistorique.NEUTRE
        );
    }

    public static RegulatoryParameters buildRegulatoryParametersSc01() {
        return new RegulatoryParameters(
                LocalTime.of(22, 0),
                LocalTime.of(6, 0),
                List.of(SC01_JOUR_FERIE)
        );
        // Variante strictement identique au contrôleur actuel :
        // return RegulatoryParameters.neutre();
    }

    public static ReferentielComptabiliteActivite buildReferentielSc01() {
        Map<String, ComptabiliteActivite> map = new HashMap<>();

        map.put(ACTIVITE_TRAVAIL, new ComptabiliteActivite(
                ACTIVITE_TRAVAIL,
                true,   // compteDansCharge
                false,  // genereDetteRepos
                false,  // estServiceCritique
                false,  // prioritaireSurConfort
                ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
        ));

        return new ReferentielComptabiliteActivite(map);
    }

    public static SalarieReel buildSalarie(String id) {
        return new SalarieReel(
                id,
                "CDI",
                "ACTIF",
                Set.of(LIEU_CASTRES),
                Set.of(ACTIVITE_TRAVAIL),
                Set.of(POSTE_COMPTABLE_PC_100)
        );
    }

    public static PosteVirtuel buildPosteVirtuel(String id) {
        return new PosteVirtuel(
                id,
                TypePosteVirtuel.POTENTIEL,
                2400,
                Set.of(ACTIVITE_TRAVAIL),
                Set.of(LIEU_CASTRES),
                Set.of(POSTE_COMPTABLE_PC_100)
        );
    }

    public static Creneau buildCreneau(
            String id,
            LocalDate date,
            LocalTime heureDebut,
            LocalTime heureFin
    ) {
        if (heureDebut.equals(heureFin)) {
            throw new IllegalArgumentException("Invariant fixture: heureDebut != heureFin");
        }

        int dureeMinutes = dureeMinutes(heureDebut, heureFin);

        Creneau c = new Creneau(
                id,
                date,
                heureDebut,
                heureFin,
                dureeMinutes,
                LIEU_CASTRES,
                null,                 // codeActiviteId
                ACTIVITE_TRAVAIL,     // activite
                POSTE_COMPTABLE_PC_100,
                PrioriteCreneau.NORMALE,
                TypeCreneau.GENERE,
                TypePlageHoraire.JOUR,
                false,                // jourFerie (source de vérité = RegulatoryParameters côté RH)
                QualificationJour.OUVRE
        );

        c.setRessourceAffectee(RessourceNonAffectee.INSTANCE);
        return c;
    }

    // -----------------------
    // Génération creneaux SC-01
    // -----------------------

    public static List<Creneau> buildCreneauxSc01() {

        LocalTime finPrevue = SC01_SHIFT_START.plusMinutes((long) (SC01_DAILY_AMPLITUDE_HOURS * 60.0));

        // workedDays du JSON: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, SUNDAY + holidayDates = 2026-05-13
        List<LocalDate> joursTravailles = List.of(
                LocalDate.of(2026, 5, 11),
                LocalDate.of(2026, 5, 12),
                LocalDate.of(2026, 5, 14), // 13 férié => aucun créneau
                LocalDate.of(2026, 5, 17)
        );

        List<Creneau> creneaux = new ArrayList<>();
        int seq = 1;

        for (LocalDate d : joursTravailles) {
            if (d.equals(SC01_JOUR_FERIE)) {
                continue;
            }
            String prefix = "SC01-" + d + "-";
            creneaux.add(buildCreneau(prefix + String.format("%03d", seq++), d, SC01_SHIFT_START, SC01_LUNCH_START));
            creneaux.add(buildCreneau(prefix + String.format("%03d", seq++), d, SC01_LUNCH_END, finPrevue));
        }

        return creneaux;
    }

    private static int dureeMinutes(LocalTime debut, LocalTime fin) {
        long minutes = Duration.between(debut, fin).toMinutes();
        if (minutes < 0) {
            minutes += 24 * 60;
        }
        return (int) minutes;
    }
}
