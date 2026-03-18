package fr.project.planning.constraints;

import fr.project.planning.constraints.metier.NuitSalarieNonNuit;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Phase8ConstraintsTest
 *
 * Valide la contrainte SOFT introduite en Phase 8 :
 * - NuitSalarieNonNuit : créneau de nuit affecté à un salarié non déclaré travailleur de nuit
 *
 * Cas couverts :
 * 1. salarié non-nuit (travailDeNuit = null) sur créneau de nuit → 1 pénalité SOFT
 * 2. salarié non-nuit (valeur inconnue) sur créneau de nuit → 1 pénalité SOFT
 * 3. salarié travailleur de nuit permanent sur créneau de nuit → 0 pénalité
 * 4. salarié travailleur de nuit occasionnel sur créneau de nuit → 0 pénalité
 * 5. salarié non-nuit sur créneau de JOUR → 0 pénalité (pas de nuit)
 * 6. créneau de nuit non affecté (RessourceNonAffectee) → 0 pénalité
 * 7. non-régression SC-01 : salarié standard sur créneau jour → 0 pénalité
 */
class Phase8ConstraintsTest {

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1. salarié non-nuit (null) sur créneau de nuit → pénalité
    // ---------------------------------------------------------

    @Test
    void salarie_nonNuit_null_surCreneauNuit_doitEtrePenalise() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NON-NUIT");
        salarie.setTravailDeNuit(null);
        Creneau creneau = creneauNuit("C-NUIT-001", LocalDate.of(2026, 5, 12), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> NuitSalarieNonNuit.nuitSalarieNonNuit(factory))
                .given(creneau)
                .penalizesBy(1);
    }

    // ---------------------------------------------------------
    // 2. salarié non-nuit (valeur inconnue) sur créneau de nuit → pénalité
    // ---------------------------------------------------------

    @Test
    void salarie_nonNuit_valeurInconnue_surCreneauNuit_doitEtrePenalise() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NON-NUIT-2");
        salarie.setTravailDeNuit("diurne");
        Creneau creneau = creneauNuit("C-NUIT-002", LocalDate.of(2026, 5, 12), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> NuitSalarieNonNuit.nuitSalarieNonNuit(factory))
                .given(creneau)
                .penalizesBy(1);
    }

    // ---------------------------------------------------------
    // 3. salarié permanent sur créneau de nuit → pas de pénalité
    // ---------------------------------------------------------

    @Test
    void salarie_nuitPermanent_surCreneauNuit_doitNePasEtrePenalise() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NUIT-PERM");
        salarie.setTravailDeNuit("permanent");
        Creneau creneau = creneauNuit("C-NUIT-003", LocalDate.of(2026, 5, 12), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> NuitSalarieNonNuit.nuitSalarieNonNuit(factory))
                .given(creneau)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 4. salarié occasionnel sur créneau de nuit → pas de pénalité
    // ---------------------------------------------------------

    @Test
    void salarie_nuitOccasionnel_surCreneauNuit_doitNePasEtrePenalise() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NUIT-OCC");
        salarie.setTravailDeNuit("occasionnel");
        Creneau creneau = creneauNuit("C-NUIT-004", LocalDate.of(2026, 5, 12), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> NuitSalarieNonNuit.nuitSalarieNonNuit(factory))
                .given(creneau)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 5. salarié non-nuit sur créneau de JOUR → pas de pénalité
    // ---------------------------------------------------------

    @Test
    void salarie_nonNuit_surCreneauJour_doitNePasEtrePenalise() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NON-NUIT-3");
        salarie.setTravailDeNuit(null);
        Creneau creneau = creneauJour("C-JOUR-001", LocalDate.of(2026, 5, 12), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> NuitSalarieNonNuit.nuitSalarieNonNuit(factory))
                .given(creneau)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 6. créneau de nuit non affecté (RessourceNonAffectee) → pas de pénalité
    // ---------------------------------------------------------

    @Test
    void creneauNuit_nonAffecte_doitNePasEtrePenalise() {
        Creneau creneau = creneauNuit("C-NUIT-005", LocalDate.of(2026, 5, 12), RessourceNonAffectee.INSTANCE);

        constraintVerifier
                .verifyThat((provider, factory) -> NuitSalarieNonNuit.nuitSalarieNonNuit(factory))
                .given(creneau)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 7. Non-régression SC-01 : créneau jour salarié standard
    // ---------------------------------------------------------

    @Test
    void sc01_creneauJour_salarieStandard_doitNePasEtrePenalise() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(TestPlanningRequestFactory.ID_SALARIE_1041);
        Creneau creneau = creneauJour("C-SC01-NUIT-001", LocalDate.of(2026, 5, 11), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> NuitSalarieNonNuit.nuitSalarieNonNuit(factory))
                .given(creneau)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private Creneau creneauNuit(String id, LocalDate date, fr.project.planning.domain.ressource.Ressource ressource) {
        Creneau c = new Creneau(
                id, date, LocalTime.of(22, 0), LocalTime.of(6, 0), 480,
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.NUIT,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }

    private Creneau creneauJour(String id, LocalDate date, fr.project.planning.domain.ressource.Ressource ressource) {
        Creneau c = new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }
}
