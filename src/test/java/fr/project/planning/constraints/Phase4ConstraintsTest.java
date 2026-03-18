package fr.project.planning.constraints;

import fr.project.planning.constraints.metier.IndisponibiliteSalarie;
import fr.project.planning.constraints.metier.JourFerieRefuse;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;

import java.time.LocalDate;
import java.time.LocalTime;


/**
 * Phase4ConstraintsTest
 *
 * Valide les deux contraintes HARD introduites en Phase 4 :
 * - JourFerieRefuse : salarié avec travailleJourFerie = false interdit sur jour férié
 * - IndisponibiliteSalarie : salarié interdit sur la plage de son indisponibilité
 *
 * Utilise ConstraintVerifier pour tester les contraintes en isolation, sans solveur.
 */
class Phase4ConstraintsTest {

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // -------------------------------------------------------
    // JourFerieRefuse
    // -------------------------------------------------------

    @Test
    void travailleJourFerie_false_doitLeverViolationHard() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-TEST");
        salarie.setTravailleJourFerie(false);
        Creneau creneau = creneauFerie("C-FERIE-001", TestPlanningRequestFactory.SC01_JOUR_FERIE, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JourFerieRefuse.jourFerieRefuse(factory))
                .given(creneau)
                .penalizesBy(1);
    }

    @Test
    void travailleJourFerie_true_doitNePasLeverViolation() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-TEST");
        salarie.setTravailleJourFerie(true);
        Creneau creneau = creneauFerie("C-FERIE-002", TestPlanningRequestFactory.SC01_JOUR_FERIE, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JourFerieRefuse.jourFerieRefuse(factory))
                .given(creneau)
                .penalizesBy(0);
    }

    @Test
    void travailleJourFerie_null_doitNePasLeverViolation() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-TEST");
        // travailleJourFerie non défini (null) : absence d'information ≠ interdiction explicite
        Creneau creneau = creneauFerie("C-FERIE-003", TestPlanningRequestFactory.SC01_JOUR_FERIE, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JourFerieRefuse.jourFerieRefuse(factory))
                .given(creneau)
                .penalizesBy(0);
    }

    // -------------------------------------------------------
    // IndisponibiliteSalarie
    // -------------------------------------------------------

    @Test
    void indisponibilite_doitLeverViolationHard() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-INDISPO");
        LocalDate date = LocalDate.of(2026, 5, 11);
        Creneau creneau = creneauNormal("C-INDISPO-001", date, salarie);
        Indisponibilite indispo = new Indisponibilite("SAL-INDISPO", date, date, "CONGE_POSE");

        constraintVerifier
                .verifyThat((provider, factory) -> IndisponibiliteSalarie.indisponibiliteSalarie(factory))
                .given(creneau, indispo)
                .penalizesBy(1);
    }

    @Test
    void indisponibilite_horsDate_doitNePasLeverViolation() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-INDISPO");
        LocalDate date = LocalDate.of(2026, 5, 11);
        Creneau creneau = creneauNormal("C-INDISPO-002", date, salarie);
        Indisponibilite indispo = new Indisponibilite("SAL-INDISPO",
                LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 22), "CONGE");

        constraintVerifier
                .verifyThat((provider, factory) -> IndisponibiliteSalarie.indisponibiliteSalarie(factory))
                .given(creneau, indispo)
                .penalizesBy(0);
    }

    @Test
    void indisponibilite_autreSalarie_doitNePasLeverViolation() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-A");
        LocalDate date = LocalDate.of(2026, 5, 11);
        Creneau creneau = creneauNormal("C-INDISPO-003", date, salarie);
        Indisponibilite indispo = new Indisponibilite("SAL-B", date, date, "CONGE");

        constraintVerifier
                .verifyThat((provider, factory) -> IndisponibiliteSalarie.indisponibiliteSalarie(factory))
                .given(creneau, indispo)
                .penalizesBy(0);
    }

    // -------------------------------------------------------
    // Non-régression SC-01
    // -------------------------------------------------------

    @Test
    void sc01_sansIndisponibilitesNiJourFerie_doitResteStable() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(TestPlanningRequestFactory.ID_SALARIE_1041);
        Creneau creneau = creneauNormal("C-SC01-001", LocalDate.of(2026, 5, 11), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JourFerieRefuse.jourFerieRefuse(factory))
                .given(creneau)
                .penalizesBy(0);

        constraintVerifier
                .verifyThat((provider, factory) -> IndisponibiliteSalarie.indisponibiliteSalarie(factory))
                .given(creneau)
                .penalizesBy(0);
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private Creneau creneauFerie(String id, LocalDate date, Ressource ressource) {
        Creneau c = new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.JOUR,
                true, QualificationJour.FERIE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }

    private Creneau creneauNormal(String id, LocalDate date, Ressource ressource) {
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
