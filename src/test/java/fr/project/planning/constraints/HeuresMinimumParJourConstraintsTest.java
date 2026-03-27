package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.HeuresMinimumParJour;
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
 * HeuresMinimumParJourConstraintsTest — Phase 8
 *
 * Vérifie la contrainte SOFT HeuresMinimumParJour :
 * pénalise les journées où le temps de travail effectif est inférieur
 * au minimum contractuel individuel.
 *
 * Cas couverts :
 * 1. heuresMinimumParJour null → contrainte inactive (0 pénalité)
 * 2. contraintesReglementaires null → contrainte inactive (0 pénalité)
 * 3. Total journalier = minimum exact → 0 pénalité
 * 4. Total journalier < minimum → pénalité = PENALITE × déficit en minutes
 * 5. Total journalier > minimum → 0 pénalité
 * 6. Segment de pause exclu du total → total recalculé sans la pause
 * 7. Non-régression SC-01 : salarié sans seuil configuré → 0 pénalité
 */
class HeuresMinimumParJourConstraintsTest {

    private static final int PENALITE_BASE = 50; // valeur constante dans HeuresMinimumParJour

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1. heuresMinimumParJour null → contrainte inactive
    // ---------------------------------------------------------

    @Test
    void heuresMinimumParJour_null_contrainterInactive() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-HM-01");
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null,  // heuresMinimumParJour = null → contrainte inactive
                null, null, null, null, null, null, null
        ));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // 1h de travail seulement → serait en déficit pour tout seuil > 1h, mais seuil null → pas de pénalité
        Creneau c = creneauTravail("C-HM-01", lundi(), LocalTime.of(8, 0), LocalTime.of(9, 0), 60, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> HeuresMinimumParJour.heuresMinimumParJour(factory))
                .given(salarie, c, ref)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 2. contraintesReglementaires null → contrainte inactive
    // ---------------------------------------------------------

    @Test
    void contraintesReglementaires_null_contrainterInactive() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-HM-02");
        // contraintesReglementaires = null par défaut
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c = creneauTravail("C-HM-02", lundi(), LocalTime.of(8, 0), LocalTime.of(9, 0), 60, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> HeuresMinimumParJour.heuresMinimumParJour(factory))
                .given(salarie, c, ref)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 3. Total = minimum exact → 0 pénalité
    // ---------------------------------------------------------

    @Test
    void totalEgalMinimum_pasDePenalite() {
        // Seuil = 7h (420 min)
        SalarieReel salarie = salarieAvecMinimum("SAL-HM-03", 7.0);
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // Exactement 7h (420 min)
        Creneau c = creneauTravail("C-HM-03", lundi(), LocalTime.of(8, 0), LocalTime.of(15, 0), 420, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> HeuresMinimumParJour.heuresMinimumParJour(factory))
                .given(salarie, c, ref)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 4. Total < minimum → pénalité proportionnelle au déficit
    // ---------------------------------------------------------

    @Test
    void totalInferieurMinimum_pénalitéProportionnelleAuDeficit() {
        // Seuil = 7h (420 min) ; travail = 6h (360 min) → déficit = 60 min
        SalarieReel salarie = salarieAvecMinimum("SAL-HM-04", 7.0);
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c = creneauTravail("C-HM-04", lundi(), LocalTime.of(8, 0), LocalTime.of(14, 0), 360, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> HeuresMinimumParJour.heuresMinimumParJour(factory))
                .given(salarie, c, ref)
                .penalizesBy(PENALITE_BASE * 60);
    }

    // ---------------------------------------------------------
    // 5. Total > minimum → 0 pénalité
    // ---------------------------------------------------------

    @Test
    void totalSuperieurMinimum_pasDePenalite() {
        // Seuil = 6h (360 min) ; travail = 8h (480 min) → pas de déficit
        SalarieReel salarie = salarieAvecMinimum("SAL-HM-05", 6.0);
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c = creneauTravail("C-HM-05", lundi(), LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> HeuresMinimumParJour.heuresMinimumParJour(factory))
                .given(salarie, c, ref)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 6. Pause exclue du total → déficit calculé sans la pause
    // ---------------------------------------------------------

    @Test
    void pauseExclue_deficitCalculeSansLaPause() {
        // Seuil = 7h (420 min)
        // Travail = 6h (360 min) + pause = 1h (60 min)
        // Sans filtre pause : total = 420 min = seuil → 0 pénalité (résultat INCORRECT)
        // Avec filtre [Phase 8] : total = 360 min < 420 → déficit = 60 min → pénalité
        SalarieReel salarie = salarieAvecMinimum("SAL-HM-06", 7.0);
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau travail = creneauTravail("C-HM-06T", lundi(), LocalTime.of(8, 0), LocalTime.of(14, 0), 360, salarie);
        Creneau pause   = pauseCreneau("C-HM-06P",   lundi(), LocalTime.of(12, 0), LocalTime.of(13, 0), 60,  salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> HeuresMinimumParJour.heuresMinimumParJour(factory))
                .given(salarie, travail, pause, ref)
                .penalizesBy(PENALITE_BASE * 60);
    }

    // ---------------------------------------------------------
    // 7. Non-régression SC-01 : salarié standard sans seuil
    // ---------------------------------------------------------

    @Test
    void sc01_salarieStandard_pasDePenalite() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(TestPlanningRequestFactory.ID_SALARIE_1041);
        ReferentielComptabiliteActivite ref = TestPlanningRequestFactory.buildReferentielSc01();

        Creneau c = creneauTravail("C-SC01-HM-01",
                TestPlanningRequestFactory.SC01_DATE_DEBUT,
                LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> HeuresMinimumParJour.heuresMinimumParJour(factory))
                .given(salarie, c, ref)
                .penalizesBy(0);
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static LocalDate lundi() {
        return LocalDate.of(2026, 5, 11);
    }

    private static SalarieReel salarieAvecMinimum(String id, double heuresMin) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                heuresMin, null, null, null, null, null, null, null
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

    private static Creneau creneauTravail(String id, LocalDate date,
                                          LocalTime debut, LocalTime fin, int duree,
                                          SalarieReel salarie) {
        Creneau c = new Creneau(
                id, date, debut, fin, duree,
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(salarie);
        return c;
    }

    private static Creneau pauseCreneau(String id, LocalDate date,
                                        LocalTime debut, LocalTime fin, int duree,
                                        SalarieReel salarie) {
        Creneau c = new Creneau(
                id, date, debut, fin, duree,
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(salarie);
        c.setEstSegmentDePause(true);
        return c;
    }
}
