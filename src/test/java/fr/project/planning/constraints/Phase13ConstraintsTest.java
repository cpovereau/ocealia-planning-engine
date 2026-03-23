package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.DimanchesTravaillesMax;
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
 * Phase13ConstraintsTest
 *
 * Valide la contrainte SOFT R9 "Dimanches travaillés maximum".
 *
 * Définition d'un dimanche travaillé (conforme à 40_WORKMETRICS / nbDimanchesTravailles) :
 *   - dimanche calendaire (DayOfWeek.SUNDAY)
 *   - au moins un créneau dont l'activité compteDansCharge = true
 *   - comptage par date distincte : plusieurs créneaux le même jour = 1 dimanche travaillé
 *
 * Seuil : SeuilsDeTolerance.maxDimanchesTravailles (global).
 * Pénalité : depassementMaxDimanchesTravailles (5 000) × dimanches en excédent.
 *
 * Cas couverts :
 * 1. Aucun dimanche travaillé → pas de pénalité
 * 2. Dimanches travaillés < seuil → pas de pénalité
 * 3. Dimanches travaillés = seuil → pas de pénalité (seuil exact)
 * 4. Dépassement de 1 dimanche → pénalité = base × 1
 * 5. Dépassement de 2 dimanches → pénalité = base × 2
 * 6. Plusieurs créneaux le même dimanche → compte pour 1 seul dimanche
 * 7. Activité sans charge → ne compte pas
 * 8. Dimanche non travaillé (lundi) → non comptabilisé
 */
class Phase13ConstraintsTest {

    private static final int PENALITE_BASE = 5_000; // depassementMaxDimanchesTravailles dans defaultPenalites()

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1. Aucun dimanche travaillé → pas de pénalité
    // ---------------------------------------------------------

    @Test
    void aucunDimanche_doitNePasEtrePenalise() {
        SalarieReel salarie = salarie("SAL-A");
        PlanningContext context = contexteAvecMax(dimanche1(), dimanche1().plusDays(13), 2);
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // Uniquement des lundis — aucun dimanche
        Creneau c = creneauJour("C-01", dimanche1().plusDays(1), salarie); // lundi

        constraintVerifier
                .verifyThat((p, factory) -> DimanchesTravaillesMax.maxDimanchesTravailles(factory))
                .given(salarie, c, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 2. Dimanches travaillés < seuil → pas de pénalité
    // ---------------------------------------------------------

    @Test
    void dimanchesSousSeuil_doitNePasEtrePenalise() {
        SalarieReel salarie = salarie("SAL-B");
        PlanningContext context = contexteAvecMax(dimanche1(), dimanche1().plusDays(13), 2);
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c = creneauJour("C-11", dimanche1(), salarie); // 1 dimanche, seuil = 2

        constraintVerifier
                .verifyThat((p, factory) -> DimanchesTravaillesMax.maxDimanchesTravailles(factory))
                .given(salarie, c, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 3. Dimanches travaillés = seuil exact → pas de pénalité
    // ---------------------------------------------------------

    @Test
    void dimanchesEgauxAuSeuil_doitNePasEtrePenalise() {
        SalarieReel salarie = salarie("SAL-C");
        PlanningContext context = contexteAvecMax(dimanche1(), dimanche1().plusDays(13), 2);
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // 2 dimanches distincts = exactement le seuil
        Creneau c1 = creneauJour("C-21", dimanche1(),             salarie);
        Creneau c2 = creneauJour("C-22", dimanche1().plusDays(7), salarie); // dimanche suivant

        constraintVerifier
                .verifyThat((p, factory) -> DimanchesTravaillesMax.maxDimanchesTravailles(factory))
                .given(salarie, c1, c2, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 4. Dépassement de 1 dimanche → pénalité = base × 1
    // ---------------------------------------------------------

    @Test
    void depassementDe1Dimanche_doitEtrePenalise() {
        SalarieReel salarie = salarie("SAL-D");
        PlanningContext context = contexteAvecMax(dimanche1(), dimanche1().plusDays(20), 2);
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // 3 dimanches distincts → excédent = 1
        Creneau c1 = creneauJour("C-31", dimanche1(),              salarie);
        Creneau c2 = creneauJour("C-32", dimanche1().plusDays(7),  salarie);
        Creneau c3 = creneauJour("C-33", dimanche1().plusDays(14), salarie);

        constraintVerifier
                .verifyThat((p, factory) -> DimanchesTravaillesMax.maxDimanchesTravailles(factory))
                .given(salarie, c1, c2, c3, ref, context)
                .penalizesBy(PENALITE_BASE);
    }

    // ---------------------------------------------------------
    // 5. Dépassement de 2 dimanches → pénalité = base × 2
    // ---------------------------------------------------------

    @Test
    void depassementDe2Dimanches_doitEtrePenalise() {
        SalarieReel salarie = salarie("SAL-E");
        PlanningContext context = contexteAvecMax(dimanche1(), dimanche1().plusDays(27), 2);
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // 4 dimanches distincts → excédent = 2
        Creneau c1 = creneauJour("C-41", dimanche1(),              salarie);
        Creneau c2 = creneauJour("C-42", dimanche1().plusDays(7),  salarie);
        Creneau c3 = creneauJour("C-43", dimanche1().plusDays(14), salarie);
        Creneau c4 = creneauJour("C-44", dimanche1().plusDays(21), salarie);

        constraintVerifier
                .verifyThat((p, factory) -> DimanchesTravaillesMax.maxDimanchesTravailles(factory))
                .given(salarie, c1, c2, c3, c4, ref, context)
                .penalizesBy(PENALITE_BASE * 2);
    }

    // ---------------------------------------------------------
    // 6. Plusieurs créneaux le même dimanche → compte pour 1 seul
    // ---------------------------------------------------------

    @Test
    void plusieursCreneauxMemeDimanche_comptentPourUn() {
        SalarieReel salarie = salarie("SAL-F");
        PlanningContext context = contexteAvecMax(dimanche1(), dimanche1().plusDays(6), 1);
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // 2 créneaux sur le même dimanche = 1 seul dimanche travaillé = seuil exact
        Creneau c1 = creneauJour("C-51", dimanche1(), salarie);
        Creneau c2 = creneauJour("C-52", dimanche1(), salarie); // même date

        constraintVerifier
                .verifyThat((p, factory) -> DimanchesTravaillesMax.maxDimanchesTravailles(factory))
                .given(salarie, c1, c2, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 7. Activité sans charge → ne compte pas comme dimanche travaillé
    // ---------------------------------------------------------

    @Test
    void activiteSansCharge_neDoitPasCompterCommeDimanche() {
        SalarieReel salarie = salarie("SAL-G");
        PlanningContext context = contexteAvecMax(dimanche1(), dimanche1().plusDays(6), 0);
        ReferentielComptabiliteActivite ref = referentielSansCharge();

        // Créneau un dimanche mais activité non chargée → 0 dimanche travaillé → 0 excédent
        Creneau c = creneauJour("C-61", dimanche1(), salarie);

        constraintVerifier
                .verifyThat((p, factory) -> DimanchesTravaillesMax.maxDimanchesTravailles(factory))
                .given(salarie, c, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 8. Jour non dimanche → non comptabilisé
    // ---------------------------------------------------------

    @Test
    void jourNonDimanche_neDoitPasEtreCompte() {
        SalarieReel salarie = salarie("SAL-H");
        PlanningContext context = contexteAvecMax(dimanche1(), dimanche1().plusDays(6), 0);
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // Lundi, mardi, mercredi : aucun dimanche
        Creneau c1 = creneauJour("C-71", dimanche1().plusDays(1), salarie); // lundi
        Creneau c2 = creneauJour("C-72", dimanche1().plusDays(2), salarie); // mardi
        Creneau c3 = creneauJour("C-73", dimanche1().plusDays(3), salarie); // mercredi

        constraintVerifier
                .verifyThat((p, factory) -> DimanchesTravaillesMax.maxDimanchesTravailles(factory))
                .given(salarie, c1, c2, c3, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    /** Premier dimanche de la période de test */
    private static LocalDate dimanche1() {
        return LocalDate.of(2026, 5, 10); // dimanche
    }

    private SalarieReel salarie(String id) {
        return TestPlanningRequestFactory.buildSalarie(id);
    }

    /**
     * Contexte avec un seuil de dimanches spécifique.
     * Utilise contexteNeutre() et patche le SeuilsDeTolerance mutable.
     */
    private PlanningContext contexteAvecMax(LocalDate debut, LocalDate fin, int maxDimanches) {
        PlanningContext ctx = TestPlanningContextFactory.contexteNeutre(debut, fin);
        ctx.getSeuilsDeTolerance().setMaxDimanchesTravailles(maxDimanches);
        return ctx;
    }

    private ReferentielComptabiliteActivite referentielAvecCharge() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
                )
        ));
    }

    private ReferentielComptabiliteActivite referentielSansCharge() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        false, false, false, false,
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
