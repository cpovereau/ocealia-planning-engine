package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.ReposObligatoireApresNuits;
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
import org.optaplanner.test.api.score.stream.SingleConstraintVerification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ReposObligatoireApresNuitsConstraintsTest — lot S7.3
 *
 * <p>Valide la contrainte HARD R4 : après une <strong>séquence</strong> de nuits, un nombre
 * minimal de jours calendaires doit rester sans travail.</p>
 *
 * <p>La contrainte était éteinte deux fois — champ d'activité déprécié et seuil global nul — et
 * n'avait aucune couverture. Les créneaux portent ici {@code codeActiviteId}, comme les clients
 * réels.</p>
 *
 * <p>Cas couverts :</p>
 * <ol>
 *   <li>Seuil non transmis → contrainte inactive</li>
 *   <li>Seuil transmis à 0 → contrainte inactive</li>
 *   <li>Repos respecté → pas de violation</li>
 *   <li>Reprise dans la fenêtre → violation HARD</li>
 *   <li>Reprise le premier jour après la fenêtre → pas de violation (borne exacte)</li>
 *   <li>Séquence de nuits interrompue → chaque fin de séquence ouvre sa propre fenêtre</li>
 *   <li>Aucune nuit → pas de violation</li>
 *   <li>Reprise en activité hors charge → pas une reprise de travail</li>
 * </ol>
 */
class ReposObligatoireApresNuitsConstraintsTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final String ACTIVITE_HORS_CHARGE = "formation";

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1-2. Activation
    // ---------------------------------------------------------

    @Test
    void seuilNonTransmis_contrainteInactive() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RN-01");

        // Deux nuits puis reprise dès le lendemain : fautif si un repos était exigé.
        verifier().given(faits(salarie, nuitsPuisRepriseA(salarie, 2, 2))).penalizesBy(0);
    }

    @Test
    void seuilTransmisAZero_contrainteInactive() {
        SalarieReel salarie = salarieAvecRepos("SAL-RN-02", 0);

        verifier().given(faits(salarie, nuitsPuisRepriseA(salarie, 2, 2))).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 3-5. La fenêtre de repos
    // ---------------------------------------------------------

    @Test
    void reposRespecte_pasDeViolation() {
        // Nuits lundi et mardi, 2 jours de repos exigés → fenêtre mercredi-jeudi.
        // Reprise le vendredi : conforme.
        SalarieReel salarie = salarieAvecRepos("SAL-RN-03", 2);

        verifier().given(faits(salarie, nuitsPuisRepriseA(salarie, 2, 4))).penalizesBy(0);
    }

    @Test
    void repriseDansLaFenetre_violationHard() {
        // Fenêtre mercredi-jeudi, reprise le mercredi.
        SalarieReel salarie = salarieAvecRepos("SAL-RN-04", 2);

        verifier().given(faits(salarie, nuitsPuisRepriseA(salarie, 2, 2))).penalizesBy(1);
    }

    @Test
    void repriseLeDernierJourDeLaFenetre_violationHard() {
        // La fenêtre est inclusive : jeudi en fait encore partie.
        SalarieReel salarie = salarieAvecRepos("SAL-RN-05", 2);

        verifier().given(faits(salarie, nuitsPuisRepriseA(salarie, 2, 3))).penalizesBy(1);
    }

    // ---------------------------------------------------------
    // 6-8. Périmètre
    // ---------------------------------------------------------

    @Test
    void sequenceInterrompue_chaqueFinDeSequenceOuvreSaFenetre() {
        // Nuit lundi, puis nuit jeudi. La première séquence s'achève lundi : sa fenêtre couvre
        // mardi. Un créneau de jour le mardi la viole, même si la seconde nuit est loin.
        SalarieReel salarie = salarieAvecRepos("SAL-RN-06", 1);

        List<Creneau> creneaux = List.of(
                nuit("C-RN-06A", LUNDI,             salarie),
                journee("C-RN-06B", LUNDI.plusDays(1), salarie),
                nuit("C-RN-06C", LUNDI.plusDays(3), salarie));

        verifier().given(faits(salarie, creneaux)).penalizesBy(1);
    }

    @Test
    void aucuneNuit_pasDeViolation() {
        SalarieReel salarie = salarieAvecRepos("SAL-RN-07", 2);

        List<Creneau> creneaux = List.of(
                journee("C-RN-07A", LUNDI,             salarie),
                journee("C-RN-07B", LUNDI.plusDays(1), salarie),
                journee("C-RN-07C", LUNDI.plusDays(2), salarie));

        verifier().given(faits(salarie, creneaux)).penalizesBy(0);
    }

    @Test
    void repriseEnActiviteHorsCharge_nEstPasUneReprise() {
        // Une formation dans la fenêtre de repos n'est pas du travail au sens de la charge :
        // elle est exclue en amont et ne peut donc pas violer le repos.
        SalarieReel salarie = salarieAvecRepos("SAL-RN-08", 2);

        List<Creneau> creneaux = List.of(
                nuit("C-RN-08A", LUNDI,             salarie),
                nuit("C-RN-08B", LUNDI.plusDays(1), salarie),
                journeeHorsCharge("C-RN-08C", LUNDI.plusDays(2), salarie));

        List<Object> faits = new ArrayList<>(List.of(salarie, referentielMixte(),
                TestPlanningContextFactory.contexteNeutre(LUNDI, LUNDI.plusDays(13))));
        faits.addAll(creneaux);

        verifier().given(faits.toArray()).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private SingleConstraintVerification<PlanningProblem> verifier() {
        return constraintVerifier.verifyThat(
                (p, factory) -> ReposObligatoireApresNuits.reposObligatoireApresNuits(factory));
    }

    private static Object[] faits(SalarieReel salarie, List<Creneau> creneaux) {
        List<Object> faits = new ArrayList<>();
        faits.add(salarie);
        faits.add(referentielAvecCharge());
        faits.add(TestPlanningContextFactory.contexteNeutre(LUNDI, LUNDI.plusDays(13)));
        faits.addAll(creneaux);
        return faits.toArray();
    }

    private static SalarieReel salarieAvecRepos(String id, int jours) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, null, null, null, null, null,
                null, jours, null, null, null));
        return salarie;
    }

    /**
     * {@code nbNuits} nuits consécutives à partir du lundi, puis une journée de travail
     * {@code decalageReprise} jours après le lundi.
     */
    private static List<Creneau> nuitsPuisRepriseA(SalarieReel salarie, int nbNuits, int decalageReprise) {
        List<Creneau> creneaux = new ArrayList<>();
        for (int i = 0; i < nbNuits; i++) {
            creneaux.add(nuit("C-RN-N" + i, LUNDI.plusDays(i), salarie));
        }
        creneaux.add(journee("C-RN-REPRISE", LUNDI.plusDays(decalageReprise), salarie));
        return creneaux;
    }

    private static Creneau nuit(String id, LocalDate date, SalarieReel salarie) {
        return creneau(id, date, LocalTime.of(22, 0), LocalTime.of(6, 0),
                TypePlageHoraire.NUIT, TestPlanningRequestFactory.ACTIVITE_TRAVAIL, salarie);
    }

    private static Creneau journee(String id, LocalDate date, SalarieReel salarie) {
        return creneau(id, date, LocalTime.of(8, 0), LocalTime.of(16, 0),
                TypePlageHoraire.JOUR, TestPlanningRequestFactory.ACTIVITE_TRAVAIL, salarie);
    }

    private static Creneau journeeHorsCharge(String id, LocalDate date, SalarieReel salarie) {
        return creneau(id, date, LocalTime.of(8, 0), LocalTime.of(16, 0),
                TypePlageHoraire.JOUR, ACTIVITE_HORS_CHARGE, salarie);
    }

    private static Creneau creneau(String id, LocalDate date, LocalTime debut, LocalTime fin,
                                   TypePlageHoraire plage, String codeActiviteId, SalarieReel salarie) {
        Creneau c = new Creneau(
                id, date, debut, fin, 480,
                TestRessourceFactory.SITE_CANON,
                codeActiviteId,
                null,                       // activite (déprécié) volontairement vide
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, plage,
                false, QualificationJour.OUVRE);
        c.setRessourceAffectee(salarie);
        return c;
    }

    private static ReferentielComptabiliteActivite referentielAvecCharge() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD)));
    }

    private static ReferentielComptabiliteActivite referentielMixte() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD),
                ACTIVITE_HORS_CHARGE,
                new ComptabiliteActivite(
                        ACTIVITE_HORS_CHARGE,
                        false, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD)));
    }
}
