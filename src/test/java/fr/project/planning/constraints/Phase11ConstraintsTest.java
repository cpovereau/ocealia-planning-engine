package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.AlternanceJourNuit;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningContextFactory;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * Phase11ConstraintsTest
 *
 * Valide la contrainte SOFT introduite en Phase 11 :
 * - AlternanceJourNuit : pénalise les transitions de rythme JOUR/NUIT
 *   sur deux jours calendaires consécutifs travaillés.
 *
 * Règle : un jour est qualifié NUIT s'il comporte au moins un créneau
 * TypePlageHoraire.NUIT avec compteDansCharge = true.
 *
 * Cas couverts :
 * 1. Deux jours JOUR consécutifs → pas d'alternance → 0 pénalité
 * 2. Transition JOUR → NUIT → pénalité = base × 1
 * 3. Transition NUIT → JOUR → pénalité = base × 1
 * 4. Double alternance (JOUR → NUIT → JOUR) → pénalité = base × 2
 * 5. Plusieurs créneaux le même jour (JOUR + NUIT) → classé NUIT → pas de double comptage
 * 6. Activité sans charge → exclue → ne crée pas d'alternance
 * 7. Gap d'un jour OFF entre JOUR et NUIT → jours non consécutifs → 0 pénalité
 */
class Phase11ConstraintsTest {

    private static final int PENALITE_BASE = 3_000; // valeur de defaultPenalites() pour alternanceJourNuit

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1. Deux jours JOUR consécutifs → pas d'alternance
    // ---------------------------------------------------------

    @Test
    void deuxJoursJourConsecutifs_doitNePasEtrePenalise() {
        SalarieReel salarie = salarie("SAL-A");
        PlanningContext context = contexte(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c1 = creneauJour("C-01", lundi(), salarie);
        Creneau c2 = creneauJour("C-02", lundi().plusDays(1), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AlternanceJourNuit.alternanceJourNuit(factory))
                .given(salarie, c1, c2, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 2. Transition JOUR → NUIT → pénalité = base × 1
    // ---------------------------------------------------------

    @Test
    void transitionJourVersNuit_doitEtrePenalisee() {
        SalarieReel salarie = salarie("SAL-B");
        PlanningContext context = contexte(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c1 = creneauJour("C-11", lundi(), salarie);
        Creneau c2 = creneauNuit("C-12", lundi().plusDays(1), salarie); // NUIT le lendemain

        constraintVerifier
                .verifyThat((provider, factory) -> AlternanceJourNuit.alternanceJourNuit(factory))
                .given(salarie, c1, c2, ref, context)
                .penalizesBy(PENALITE_BASE);
    }

    // ---------------------------------------------------------
    // 3. Transition NUIT → JOUR → pénalité = base × 1
    // ---------------------------------------------------------

    @Test
    void transitionNuitVersJour_doitEtrePenalisee() {
        SalarieReel salarie = salarie("SAL-C");
        PlanningContext context = contexte(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c1 = creneauNuit("C-21", lundi(), salarie);
        Creneau c2 = creneauJour("C-22", lundi().plusDays(1), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AlternanceJourNuit.alternanceJourNuit(factory))
                .given(salarie, c1, c2, ref, context)
                .penalizesBy(PENALITE_BASE);
    }

    // ---------------------------------------------------------
    // 4. Double alternance (JOUR → NUIT → JOUR) → pénalité = base × 2
    // ---------------------------------------------------------

    @Test
    void doubleAlternanceJourNuitJour_doitEtrePenaliseDouble() {
        SalarieReel salarie = salarie("SAL-D");
        PlanningContext context = contexte(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c1 = creneauJour("C-31", lundi(), salarie);
        Creneau c2 = creneauNuit("C-32", lundi().plusDays(1), salarie);
        Creneau c3 = creneauJour("C-33", lundi().plusDays(2), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AlternanceJourNuit.alternanceJourNuit(factory))
                .given(salarie, c1, c2, c3, ref, context)
                .penalizesBy(PENALITE_BASE * 2);
    }

    // ---------------------------------------------------------
    // 5. Plusieurs créneaux le même jour (JOUR + NUIT) → jour classé NUIT
    //    Le jour suivant en NUIT → pas d'alternance détectée (même type)
    // ---------------------------------------------------------

    @Test
    void jourMixteJourNuitClasseNuit_etLendemainNuit_doitNePasEtrePenalise() {
        SalarieReel salarie = salarie("SAL-E");
        PlanningContext context = contexte(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // Lundi : 1 créneau JOUR + 1 créneau NUIT → classé NUIT
        Creneau c1Jour = creneauJour("C-41", lundi(), salarie);
        Creneau c1Nuit = creneauNuit("C-42", lundi(), salarie);
        // Mardi : NUIT → même type que lundi (NUIT) → pas d'alternance
        Creneau c2 = creneauNuit("C-43", lundi().plusDays(1), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AlternanceJourNuit.alternanceJourNuit(factory))
                .given(salarie, c1Jour, c1Nuit, c2, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 6. Activité sans charge → exclue → ne crée pas d'alternance
    // ---------------------------------------------------------

    @Test
    void creneauNuitSansCharge_neCreesPasAlternance() {
        SalarieReel salarie = salarie("SAL-F");
        PlanningContext context = contexte(lundi(), lundi().plusDays(6));
        // Référentiel sans charge : aucun créneau ne compte dans la charge
        ReferentielComptabiliteActivite ref = referentielSansCharge();

        Creneau c1 = creneauJour("C-51", lundi(), salarie);
        Creneau c2 = creneauNuit("C-52", lundi().plusDays(1), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AlternanceJourNuit.alternanceJourNuit(factory))
                .given(salarie, c1, c2, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 7. Gap d'un jour OFF entre JOUR et NUIT → non consécutifs → 0 pénalité
    // ---------------------------------------------------------

    @Test
    void gapJourOff_entreJourEtNuit_doitNePasEtrePenalise() {
        SalarieReel salarie = salarie("SAL-G");
        PlanningContext context = contexte(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // Lundi JOUR, mercredi NUIT (mardi = pas de créneau → gap)
        Creneau c1 = creneauJour("C-61", lundi(), salarie);
        Creneau c2 = creneauNuit("C-62", lundi().plusDays(2), salarie); // mercredi

        constraintVerifier
                .verifyThat((provider, factory) -> AlternanceJourNuit.alternanceJourNuit(factory))
                .given(salarie, c1, c2, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private static LocalDate lundi() {
        return LocalDate.of(2026, 5, 11);
    }

    private SalarieReel salarie(String id) {
        return TestPlanningRequestFactory.buildSalarie(id);
    }

    private PlanningContext contexte(LocalDate debut, LocalDate fin) {
        return TestPlanningContextFactory.contexteNeutre(debut, fin);
    }

    private ReferentielComptabiliteActivite referentielAvecCharge() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        true,  // compteDansCharge
                        false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
                )
        ));
    }

    private ReferentielComptabiliteActivite referentielSansCharge() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        false, // compteDansCharge = false
                        false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
                )
        ));
    }

    private Creneau creneauJour(String id, LocalDate date, SalarieReel ressource) {
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

    private Creneau creneauNuit(String id, LocalDate date, SalarieReel ressource) {
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
}
