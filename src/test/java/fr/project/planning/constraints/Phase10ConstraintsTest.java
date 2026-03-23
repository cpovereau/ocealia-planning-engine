package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.JoursConsecutifsMax;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
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
 * Phase10ConstraintsTest
 *
 * Valide la contrainte SOFT introduite en Phase 10 :
 * - JoursConsecutifsMax : pénalise le dépassement du nombre maximal de jours
 *   consécutifs travaillés par salarié.
 *
 * Cas couverts :
 * 1. Séquence exactement égale au seuil → 0 pénalité (pas de dépassement)
 * 2. Séquence de 1 jour au-delà du seuil → pénalité = base × 1
 * 3. Séquence de 2 jours au-delà du seuil → pénalité = base × 2
 * 4. Plusieurs créneaux le même jour → compte pour 1 seul jour
 * 5. Activité sans charge (compteDansCharge=false) → ne compte pas comme jour travaillé
 * 6. Salarié sans contraintes réglementaires → contrainte ne s'applique pas
 * 7. Non-régression SC-01 : salarié standard sans joursConsecutifsMaximum → 0 pénalité
 */
class Phase10ConstraintsTest {

    private static final int PENALITE_BASE = 5_000; // valeur de defaultPenalites()

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1. Séquence exactement égale au seuil → pas de pénalité
    // ---------------------------------------------------------

    @Test
    void sequenceEgaleAuSeuil_doitNePasEtrePenalisee() {
        SalarieReel salarie = salarieAvecSeuil("SAL-A", 3);
        PlanningContext context = contexteAvecHorizon(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // 3 jours consécutifs (lundi, mardi, mercredi) = exactement le seuil
        Creneau c1 = creneauJour("C-01", lundi(), salarie);
        Creneau c2 = creneauJour("C-02", lundi().plusDays(1), salarie);
        Creneau c3 = creneauJour("C-03", lundi().plusDays(2), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, c1, c2, c3, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 2. Dépassement de 1 jour → pénalité = base × 1
    // ---------------------------------------------------------

    @Test
    void depassementDe1Jour_doitEtrePenalise() {
        SalarieReel salarie = salarieAvecSeuil("SAL-B", 3);
        PlanningContext context = contexteAvecHorizon(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // 4 jours consécutifs → 1 jour de dépassement
        Creneau c1 = creneauJour("C-11", lundi(), salarie);
        Creneau c2 = creneauJour("C-12", lundi().plusDays(1), salarie);
        Creneau c3 = creneauJour("C-13", lundi().plusDays(2), salarie);
        Creneau c4 = creneauJour("C-14", lundi().plusDays(3), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, c1, c2, c3, c4, ref, context)
                .penalizesBy(PENALITE_BASE);
    }

    // ---------------------------------------------------------
    // 3. Dépassement de 2 jours → pénalité = base × 2
    // ---------------------------------------------------------

    @Test
    void depassementDe2Jours_doitEtrePenalise() {
        SalarieReel salarie = salarieAvecSeuil("SAL-C", 3);
        PlanningContext context = contexteAvecHorizon(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // 5 jours consécutifs → 2 jours de dépassement
        Creneau c1 = creneauJour("C-21", lundi(), salarie);
        Creneau c2 = creneauJour("C-22", lundi().plusDays(1), salarie);
        Creneau c3 = creneauJour("C-23", lundi().plusDays(2), salarie);
        Creneau c4 = creneauJour("C-24", lundi().plusDays(3), salarie);
        Creneau c5 = creneauJour("C-25", lundi().plusDays(4), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, c1, c2, c3, c4, c5, ref, context)
                .penalizesBy(PENALITE_BASE * 2);
    }

    // ---------------------------------------------------------
    // 4. Plusieurs créneaux le même jour → compte pour 1 jour
    // ---------------------------------------------------------

    @Test
    void plusieursCreneauxMemeJour_comptentPourUnJour() {
        SalarieReel salarie = salarieAvecSeuil("SAL-D", 3);
        PlanningContext context = contexteAvecHorizon(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // 2 créneaux lundi + 1 mardi + 1 mercredi = 3 jours distincts = seuil exact
        Creneau c1 = creneauJour("C-31", lundi(), salarie);
        Creneau c2 = creneauJour("C-32", lundi(), salarie);       // même jour que c1
        Creneau c3 = creneauJour("C-33", lundi().plusDays(1), salarie);
        Creneau c4 = creneauJour("C-34", lundi().plusDays(2), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, c1, c2, c3, c4, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 5. Activité sans charge → ne compte pas comme jour travaillé
    // ---------------------------------------------------------

    @Test
    void activiteSansCharge_neDoitPasCompterCommeJourTravaille() {
        SalarieReel salarie = salarieAvecSeuil("SAL-E", 2);
        PlanningContext context = contexteAvecHorizon(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielSansCharge();

        // 3 créneaux sur 3 jours, mais activité sans charge → 0 jours travaillés
        Creneau c1 = creneauJour("C-41", lundi(), salarie);
        Creneau c2 = creneauJour("C-42", lundi().plusDays(1), salarie);
        Creneau c3 = creneauJour("C-43", lundi().plusDays(2), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, c1, c2, c3, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 6. Salarié sans contraintes réglementaires → pas de pénalité
    // ---------------------------------------------------------

    @Test
    void salarieSansContraintesReglementaires_doitNePasEtrePenalise() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-F");
        // contraintesReglementaires = null (valeur par défaut)
        PlanningContext context = contexteAvecHorizon(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c1 = creneauJour("C-51", lundi(), salarie);
        Creneau c2 = creneauJour("C-52", lundi().plusDays(1), salarie);
        Creneau c3 = creneauJour("C-53", lundi().plusDays(2), salarie);
        Creneau c4 = creneauJour("C-54", lundi().plusDays(3), salarie);
        Creneau c5 = creneauJour("C-55", lundi().plusDays(4), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, c1, c2, c3, c4, c5, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 7. Non-régression SC-01 : salarié standard sans seuil configuré
    // ---------------------------------------------------------

    @Test
    void sc01_salarieStandard_doitNePasEtrePenalise() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(TestPlanningRequestFactory.ID_SALARIE_1041);
        PlanningContext context = TestPlanningContextFactory.contexteNeutre(
                TestPlanningRequestFactory.SC01_DATE_DEBUT,
                TestPlanningRequestFactory.SC01_DATE_FIN
        );
        ReferentielComptabiliteActivite ref = TestPlanningRequestFactory.buildReferentielSc01();

        Creneau c1 = creneauJour("C-SC01-001", TestPlanningRequestFactory.SC01_DATE_DEBUT, salarie);
        Creneau c2 = creneauJour("C-SC01-002", TestPlanningRequestFactory.SC01_DATE_DEBUT.plusDays(1), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, c1, c2, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private static LocalDate lundi() {
        return LocalDate.of(2026, 5, 11); // lundi
    }

    private SalarieReel salarieAvecSeuil(String id, int joursConsecutifsMaximum) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, null, null, null, null,
                joursConsecutifsMaximum
        ));
        return salarie;
    }

    private PlanningContext contexteAvecHorizon(LocalDate debut, LocalDate fin) {
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
}
