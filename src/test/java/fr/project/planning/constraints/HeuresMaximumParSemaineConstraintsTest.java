package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.HeuresMaximumParSemaine;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * HeuresMaximumParSemaineConstraintsTest — lot S3
 *
 * Vérifie la contrainte SOFT HeuresMaximumParSemaine : pénalise le dépassement de la durée
 * hebdomadaire maximale, la semaine étant définie du lundi au dimanche calendaire.
 *
 * Cas couverts :
 *  1. heuresMaximumParSemaine null → contrainte inactive
 *  2. contraintesReglementaires null → contrainte inactive
 *  3. total = seuil exact → 0 pénalité
 *  4. total < seuil → 0 pénalité
 *  5. total > seuil → pénalité × minutes de dépassement
 *  6. frontière dimanche / lundi : deux semaines comptées séparément
 *  7. deux semaines en dépassement → pénalités cumulées
 *  8. segment de pause exclu du total
 *  9. activité hors charge exclue du total
 * 10. créneau du dimanche franchissant minuit : rattaché en entier à la semaine du dimanche
 * 11. non-régression SC-01 : salarié sans seuil configuré → 0 pénalité
 */
class HeuresMaximumParSemaineConstraintsTest {

    /** Valeur de PENALITE_HEURES_MAX_PAR_SEMAINE dans HeuresMaximumParSemaine. */
    private static final int PENALITE_BASE = 100;

    // Semaine 1 : lundi 11 mai 2026 → dimanche 17 mai 2026
    private static final LocalDate LUNDI_S1    = LocalDate.of(2026, 5, 11);
    private static final LocalDate MARDI_S1    = LocalDate.of(2026, 5, 12);
    private static final LocalDate MERCREDI_S1 = LocalDate.of(2026, 5, 13);
    private static final LocalDate DIMANCHE_S1 = LocalDate.of(2026, 5, 17);

    // Semaine 2 : lundi 18 mai 2026
    private static final LocalDate LUNDI_S2 = LocalDate.of(2026, 5, 18);
    private static final LocalDate MARDI_S2 = LocalDate.of(2026, 5, 19);

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1-2. Activation
    // ---------------------------------------------------------

    @Test
    void heuresMaximumParSemaine_null_contrainteInactive() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-HS-01");
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, null, null,
                null,  // heuresMaximumParSemaine = null → contrainte inactive
                null, null
        ));

        // 60h sur la semaine : dépasserait tout seuil réaliste
        Creneau c1 = travail("C-HS-01A", LUNDI_S1, LocalTime.of(6, 0), LocalTime.of(22, 0), 960, salarie);
        Creneau c2 = travail("C-HS-01B", MARDI_S1, LocalTime.of(6, 0), LocalTime.of(22, 0), 960, salarie);

        verifier().given(salarie, c1, c2, referentielAvecCharge()).penalizesBy(0);
    }

    @Test
    void contraintesReglementaires_null_contrainteInactive() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-HS-02");
        // contraintesReglementaires = null par défaut

        Creneau c1 = travail("C-HS-02A", LUNDI_S1, LocalTime.of(6, 0), LocalTime.of(22, 0), 960, salarie);
        Creneau c2 = travail("C-HS-02B", MARDI_S1, LocalTime.of(6, 0), LocalTime.of(22, 0), 960, salarie);

        verifier().given(salarie, c1, c2, referentielAvecCharge()).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 3-5. Comparaison au seuil
    // ---------------------------------------------------------

    @Test
    void totalEgalAuSeuil_pasDePenalite() {
        // Seuil 16h (960 min) — deux journées de 8h
        SalarieReel salarie = salarieAvecMaxHebdo("SAL-HS-03", 16.0);

        Creneau c1 = travail("C-HS-03A", LUNDI_S1, LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        Creneau c2 = travail("C-HS-03B", MARDI_S1, LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        verifier().given(salarie, c1, c2, referentielAvecCharge()).penalizesBy(0);
    }

    @Test
    void totalInferieurAuSeuil_pasDePenalite() {
        // Seuil 20h — 16h travaillées
        SalarieReel salarie = salarieAvecMaxHebdo("SAL-HS-04", 20.0);

        Creneau c1 = travail("C-HS-04A", LUNDI_S1, LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        Creneau c2 = travail("C-HS-04B", MARDI_S1, LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        verifier().given(salarie, c1, c2, referentielAvecCharge()).penalizesBy(0);
    }

    @Test
    void totalSuperieurAuSeuil_penaliteProportionnelleAuDepassement() {
        // Seuil 20h (1200 min) — 24h travaillées (1440 min) → dépassement 240 min
        SalarieReel salarie = salarieAvecMaxHebdo("SAL-HS-05", 20.0);

        Creneau c1 = travail("C-HS-05A", LUNDI_S1,    LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        Creneau c2 = travail("C-HS-05B", MARDI_S1,    LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        Creneau c3 = travail("C-HS-05C", MERCREDI_S1, LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        verifier().given(salarie, c1, c2, c3, referentielAvecCharge())
                .penalizesBy(PENALITE_BASE * 240);
    }

    // ---------------------------------------------------------
    // 6-7. Découpage hebdomadaire
    // ---------------------------------------------------------

    @Test
    void frontiereDimancheLundi_deuxSemainesComptesSeparement() {
        // Seuil 10h. Dimanche 8h + lundi suivant 8h.
        // Regroupées : 16h → dépassement de 6h (INCORRECT)
        // Séparées   : 8h et 8h → aucun dépassement
        SalarieReel salarie = salarieAvecMaxHebdo("SAL-HS-06", 10.0);

        Creneau dimanche = travail("C-HS-06A", DIMANCHE_S1, LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        Creneau lundi    = travail("C-HS-06B", LUNDI_S2,    LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        verifier().given(salarie, dimanche, lundi, referentielAvecCharge()).penalizesBy(0);
    }

    @Test
    void deuxSemainesEnDepassement_penalitesCumulees() {
        // Seuil 7h (420 min). Une journée de 8h (480 min) par semaine → 60 min de dépassement chacune.
        SalarieReel salarie = salarieAvecMaxHebdo("SAL-HS-07", 7.0);

        Creneau s1 = travail("C-HS-07A", MARDI_S1, LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        Creneau s2 = travail("C-HS-07B", MARDI_S2, LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        verifier().given(salarie, s1, s2, referentielAvecCharge())
                .penalizesBy(PENALITE_BASE * 120);
    }

    @Test
    void creneauDuDimancheFranchissantMinuit_rattacheEnEntierALaSemaineDuDimanche() {
        // Seuil 7h (420 min). Dimanche 22:00 → 06:00, durée 480 min, date = dimanche.
        // La durée n'est jamais scindée : les 480 min comptent pour la semaine du dimanche.
        // Dépassement = 60 min sur cette seule semaine.
        SalarieReel salarie = salarieAvecMaxHebdo("SAL-HS-08", 7.0);

        Creneau nuit = travail("C-HS-08A", DIMANCHE_S1, LocalTime.of(22, 0), LocalTime.of(6, 0), 480, salarie);

        verifier().given(salarie, nuit, referentielAvecCharge())
                .penalizesBy(PENALITE_BASE * 60);
    }

    // ---------------------------------------------------------
    // 8-9. Périmètre du calcul
    // ---------------------------------------------------------

    @Test
    void segmentDePause_exclutDuTotal() {
        // Seuil 8h (480 min). Travail 8h + pause 1h.
        // Sans exclusion : 540 min → dépassement 60 min (INCORRECT)
        // Avec exclusion  : 480 min = seuil → 0
        SalarieReel salarie = salarieAvecMaxHebdo("SAL-HS-09", 8.0);

        Creneau travail = travail("C-HS-09A", LUNDI_S1, LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        Creneau pause   = pause("C-HS-09P",  LUNDI_S1, LocalTime.of(12, 0), LocalTime.of(13, 0), 60, salarie);

        verifier().given(salarie, travail, pause, referentielAvecCharge()).penalizesBy(0);
    }

    @Test
    void activiteHorsCharge_exclueDuTotal() {
        // Même géométrie : le créneau de formation ne compte pas dans la charge.
        SalarieReel salarie = salarieAvecMaxHebdo("SAL-HS-10", 8.0);

        Creneau travail   = travail("C-HS-10A", LUNDI_S1, LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        Creneau formation = creneau("C-HS-10F", MARDI_S1, LocalTime.of(8, 0), LocalTime.of(12, 0), 240,
                ACTIVITE_HORS_CHARGE, salarie);

        verifier().given(salarie, travail, formation, referentielMixte()).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 11. Non-régression SC-01
    // ---------------------------------------------------------

    @Test
    void sc01_salarieStandard_pasDePenalite() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(TestPlanningRequestFactory.ID_SALARIE_1041);

        Creneau c = travail("C-SC01-HS-01", TestPlanningRequestFactory.SC01_DATE_DEBUT,
                LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        verifier().given(salarie, c, TestPlanningRequestFactory.buildReferentielSc01()).penalizesBy(0);
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static final String ACTIVITE_HORS_CHARGE = "formation";

    private org.optaplanner.test.api.score.stream.SingleConstraintVerification<PlanningProblem> verifier() {
        return constraintVerifier.verifyThat(
                (provider, factory) -> HeuresMaximumParSemaine.heuresMaximumParSemaine(factory));
    }

    private static SalarieReel salarieAvecMaxHebdo(String id, double heuresMax) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, null, null, heuresMax, null, null
        ));
        return salarie;
    }

    private static ReferentielComptabiliteActivite referentielAvecCharge() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
                )
        ));
    }

    private static ReferentielComptabiliteActivite referentielMixte() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
                ),
                ACTIVITE_HORS_CHARGE,
                new ComptabiliteActivite(
                        ACTIVITE_HORS_CHARGE,
                        false, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
                )
        ));
    }

    private static Creneau travail(String id, LocalDate date, LocalTime debut, LocalTime fin,
                                   int duree, SalarieReel salarie) {
        return creneau(id, date, debut, fin, duree, TestPlanningRequestFactory.ACTIVITE_TRAVAIL, salarie);
    }

    private static Creneau creneau(String id, LocalDate date, LocalTime debut, LocalTime fin,
                                   int duree, String activite, SalarieReel salarie) {
        Creneau c = new Creneau(
                id, date, debut, fin, duree,
                TestRessourceFactory.SITE_CANON, null,
                activite,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(salarie);
        return c;
    }

    private static Creneau pause(String id, LocalDate date, LocalTime debut, LocalTime fin,
                                 int duree, SalarieReel salarie) {
        Creneau c = travail(id, date, debut, fin, duree, salarie);
        c.setEstSegmentDePause(true);
        return c;
    }
}
