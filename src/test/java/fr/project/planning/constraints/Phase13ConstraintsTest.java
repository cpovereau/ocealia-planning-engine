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
 * Phase13ConstraintsTest
 *
 * <p>Valide la contrainte SOFT R9 « Dimanches travaillés maximum ».</p>
 *
 * <p>Définition d'un dimanche travaillé (conforme à 40_WORKMETRICS / nbDimanchesTravailles) :</p>
 * <ul>
 *   <li>dimanche calendaire (DayOfWeek.SUNDAY) ;</li>
 *   <li>au moins un créneau dont l'activité a compteDansCharge = true ;</li>
 *   <li>comptage par date distincte : plusieurs créneaux le même jour = 1 dimanche travaillé.</li>
 * </ul>
 *
 * <h3>Lot S7.1 — deux changements</h3>
 * <p><strong>Le seuil est individuel</strong> : {@code contraintesReglementaires.dimanchesTravaillesMaximum},
 * porté par le salarié. Il était auparavant lu dans {@code SeuilsDeTolerance}, où il n'a jamais
 * été alimenté — donc nul.</p>
 *
 * <p><strong>Les créneaux portent {@code codeActiviteId}</strong>, comme les clients réels, et non
 * plus le champ {@code activite} déprécié. C'est ce détail qui rendait ces tests verts alors que
 * la contrainte était morte en production : ils alimentaient un champ que plus personne n'envoie.
 * La couverture des deux champs est assurée par {@code SocleReglementaireBaselineTest}.</p>
 *
 * <p>Cas couverts :</p>
 * <ol>
 *   <li>Aucun dimanche travaillé → pas de pénalité</li>
 *   <li>Dimanches travaillés &lt; seuil → pas de pénalité</li>
 *   <li>Dimanches travaillés = seuil → pas de pénalité (seuil exact)</li>
 *   <li>Dépassement de 1 dimanche → pénalité = base × 1</li>
 *   <li>Dépassement de 2 dimanches → pénalité = base × 2</li>
 *   <li>Plusieurs créneaux le même dimanche → compte pour 1 seul</li>
 *   <li>Activité sans charge → ne compte pas</li>
 *   <li>Jour non dimanche → non comptabilisé</li>
 *   <li>Seuil non transmis → contrainte inactive</li>
 *   <li>Seuil transmis à 0 → contrainte inactive</li>
 * </ol>
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
        SalarieReel salarie = salarieAvecMax("SAL-A", 2);
        PlanningContext context = contexte(dimanche1(), dimanche1().plusDays(13));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // Uniquement des lundis — aucun dimanche
        Creneau c = creneauJour("C-01", dimanche1().plusDays(1), salarie); // lundi

        verifier().given(salarie, c, ref, context).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 2. Dimanches travaillés < seuil → pas de pénalité
    // ---------------------------------------------------------

    @Test
    void dimanchesSousSeuil_doitNePasEtrePenalise() {
        SalarieReel salarie = salarieAvecMax("SAL-B", 2);
        PlanningContext context = contexte(dimanche1(), dimanche1().plusDays(13));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c = creneauJour("C-11", dimanche1(), salarie); // 1 dimanche, seuil = 2

        verifier().given(salarie, c, ref, context).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 3. Dimanches travaillés = seuil exact → pas de pénalité
    // ---------------------------------------------------------

    @Test
    void dimanchesEgauxAuSeuil_doitNePasEtrePenalise() {
        SalarieReel salarie = salarieAvecMax("SAL-C", 2);
        PlanningContext context = contexte(dimanche1(), dimanche1().plusDays(13));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // 2 dimanches distincts = exactement le seuil
        Creneau c1 = creneauJour("C-21", dimanche1(),             salarie);
        Creneau c2 = creneauJour("C-22", dimanche1().plusDays(7), salarie); // dimanche suivant

        verifier().given(salarie, c1, c2, ref, context).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 4. Dépassement de 1 dimanche → pénalité = base × 1
    // ---------------------------------------------------------

    @Test
    void depassementDe1Dimanche_doitEtrePenalise() {
        SalarieReel salarie = salarieAvecMax("SAL-D", 2);
        PlanningContext context = contexte(dimanche1(), dimanche1().plusDays(20));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // 3 dimanches distincts → excédent = 1
        Creneau c1 = creneauJour("C-31", dimanche1(),              salarie);
        Creneau c2 = creneauJour("C-32", dimanche1().plusDays(7),  salarie);
        Creneau c3 = creneauJour("C-33", dimanche1().plusDays(14), salarie);

        verifier().given(salarie, c1, c2, c3, ref, context).penalizesBy(PENALITE_BASE);
    }

    // ---------------------------------------------------------
    // 5. Dépassement de 2 dimanches → pénalité = base × 2
    // ---------------------------------------------------------

    @Test
    void depassementDe2Dimanches_doitEtrePenalise() {
        SalarieReel salarie = salarieAvecMax("SAL-E", 2);
        PlanningContext context = contexte(dimanche1(), dimanche1().plusDays(27));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // 4 dimanches distincts → excédent = 2
        Creneau c1 = creneauJour("C-41", dimanche1(),              salarie);
        Creneau c2 = creneauJour("C-42", dimanche1().plusDays(7),  salarie);
        Creneau c3 = creneauJour("C-43", dimanche1().plusDays(14), salarie);
        Creneau c4 = creneauJour("C-44", dimanche1().plusDays(21), salarie);

        verifier().given(salarie, c1, c2, c3, c4, ref, context).penalizesBy(PENALITE_BASE * 2);
    }

    // ---------------------------------------------------------
    // 6. Plusieurs créneaux le même dimanche → compte pour 1 seul
    // ---------------------------------------------------------

    @Test
    void plusieursCreneauxMemeDimanche_comptentPourUn() {
        SalarieReel salarie = salarieAvecMax("SAL-F", 1);
        PlanningContext context = contexte(dimanche1(), dimanche1().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // 2 créneaux sur le même dimanche = 1 seul dimanche travaillé = seuil exact
        Creneau c1 = creneauJour("C-51", dimanche1(), salarie);
        Creneau c2 = creneauJour("C-52", dimanche1(), salarie); // même date

        verifier().given(salarie, c1, c2, ref, context).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 7. Activité sans charge → ne compte pas comme dimanche travaillé
    // ---------------------------------------------------------

    @Test
    void activiteSansCharge_neDoitPasCompterCommeDimanche() {
        SalarieReel salarie = salarieAvecMax("SAL-G", 1);
        PlanningContext context = contexte(dimanche1(), dimanche1().plusDays(13));
        ReferentielComptabiliteActivite ref = referentielSansCharge();

        // 2 dimanches, mais activité non chargée → 0 dimanche travaillé.
        // Avec une activité chargée, ces mêmes créneaux dépasseraient le seuil de 1.
        Creneau c1 = creneauJour("C-61", dimanche1(),             salarie);
        Creneau c2 = creneauJour("C-62", dimanche1().plusDays(7), salarie);

        verifier().given(salarie, c1, c2, ref, context).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 8. Jour non dimanche → non comptabilisé
    // ---------------------------------------------------------

    @Test
    void jourNonDimanche_neDoitPasEtreCompte() {
        SalarieReel salarie = salarieAvecMax("SAL-H", 1);
        PlanningContext context = contexte(dimanche1(), dimanche1().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // Lundi, mardi, mercredi : aucun dimanche
        Creneau c1 = creneauJour("C-71", dimanche1().plusDays(1), salarie); // lundi
        Creneau c2 = creneauJour("C-72", dimanche1().plusDays(2), salarie); // mardi
        Creneau c3 = creneauJour("C-73", dimanche1().plusDays(3), salarie); // mercredi

        verifier().given(salarie, c1, c2, c3, ref, context).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 9-10. Activation du seuil individuel (lot S7.1)
    // ---------------------------------------------------------

    @Test
    void seuilNonTransmis_contrainteInactive() {
        // Cas de tous les payloads existants : aucun plafond de dimanches n'est envoyé.
        // Le moteur ne suppose pas de limite — il constate qu'on ne lui en a donné aucune.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-I");
        PlanningContext context = contexte(dimanche1(), dimanche1().plusDays(27));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c1 = creneauJour("C-81", dimanche1(),              salarie);
        Creneau c2 = creneauJour("C-82", dimanche1().plusDays(7),  salarie);
        Creneau c3 = creneauJour("C-83", dimanche1().plusDays(14), salarie);

        verifier().given(salarie, c1, c2, c3, ref, context).penalizesBy(0);
    }

    @Test
    void seuilTransmisAZero_contrainteInactive() {
        // Le contrat impose d'omettre le champ. Un 0 reçu est tracé en WARN par le mapper
        // et lu comme une désactivation — jamais comme « aucun dimanche autorisé ».
        SalarieReel salarie = salarieAvecMax("SAL-J", 0);
        PlanningContext context = contexte(dimanche1(), dimanche1().plusDays(13));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c1 = creneauJour("C-91", dimanche1(),             salarie);
        Creneau c2 = creneauJour("C-92", dimanche1().plusDays(7), salarie);

        verifier().given(salarie, c1, c2, ref, context).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private org.optaplanner.test.api.score.stream.SingleConstraintVerification<PlanningProblem> verifier() {
        return constraintVerifier.verifyThat(
                (p, factory) -> DimanchesTravaillesMax.maxDimanchesTravailles(factory));
    }

    /** Premier dimanche de la période de test */
    private static LocalDate dimanche1() {
        return LocalDate.of(2026, 5, 10); // dimanche
    }

    /** Salarié portant son propre plafond de dimanches travaillés. */
    private SalarieReel salarieAvecMax(String id, int maxDimanches) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, null, null, null, null, null,
                null, null, maxDimanches, null, null));
        return salarie;
    }

    /**
     * Contexte neutre. Il ne porte plus le seuil — seulement la valeur de la pénalité,
     * qui reste un poids de scoring global.
     */
    private PlanningContext contexte(LocalDate debut, LocalDate fin) {
        return TestPlanningContextFactory.contexteNeutre(debut, fin);
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

    /**
     * Créneau de journée portant {@code codeActiviteId} — le champ que transmettent les clients.
     * Le champ {@code activite} est laissé vide à dessein : c'est ce qui distingue ce test de sa
     * version antérieure au lot S7.1.
     */
    private Creneau creneauJour(String id, LocalDate date, SalarieReel ressource) {
        Creneau c = new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                TestRessourceFactory.SITE_CANON,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,  // codeActiviteId
                null,                                         // activite (déprécié)
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }
}
