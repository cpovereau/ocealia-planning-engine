package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.HeuresMinimumParSemaine;
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
 * HeuresMinimumParSemaineConstraintsTest — lot S7.7
 *
 * <p>Valide les deux volets du sous-emploi hebdomadaire : le déficit d'une semaine travaillée, et
 * la semaine complète sans aucune affectation.</p>
 *
 * <h3>Ce que ces tests protègent avant tout</h3>
 * <p>Un seul volet suffirait à produire l'incitation <em>inverse</em> de celle recherchée : ne
 * rien confier à un salarié ne coûterait rien, tandis que lui confier trop peu coûterait le
 * déficit entier. Le cas {@code semaineVide_coutePlusCherQuUneSemainePartielle} est le garde-fou
 * de cet ordre de coûts — le comportement fautif avait été observé sur le jeu SC-03, où les six
 * créneaux étaient partis au poste virtuel.</p>
 *
 * <p>Cas couverts :</p>
 * <ol>
 *   <li>Minimum non transmis → contrainte inactive, les deux volets</li>
 *   <li>Minimum atteint → pas de pénalité</li>
 *   <li>Minimum dépassé → pas de pénalité (c'est un plancher)</li>
 *   <li>Déficit → pénalité = minutes manquantes × 50</li>
 *   <li>Semaine incomplète dans l'horizon → non jugée</li>
 *   <li>Activité hors charge → ne compte pas dans le total</li>
 *   <li>Semaine complète sans affectation → déficit maximal</li>
 *   <li>Ordre des coûts : semaine vide &gt; semaine partielle</li>
 *   <li>Minimum à 0 → aucune exigence, aucun des deux volets ne pénalise</li>
 * </ol>
 */
class HeuresMinimumParSemaineConstraintsTest {

    /** Lundi 11 mai 2026 — la semaine 11→17 mai est complète. */
    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final LocalDate DIMANCHE = LUNDI.plusDays(6);
    private static final int PENALITE = 50;
    private static final String ACTIVITE_HORS_CHARGE = "formation";

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1. Activation
    // ---------------------------------------------------------

    @Test
    void minimumNonTransmis_lesDeuxVoletsInactifs() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-HM-01");

        deficit().given(faits(salarie, journees(salarie, 1))).penalizesBy(0);
        semaineVide().given(faits(salarie, List.of())).penalizesBy(0);
    }

    @Test
    void minimumAZero_aucuneExigence() {
        // Lecture littérale : un minimum de 0 est toujours atteint.
        SalarieReel salarie = salarieAvecMinimum("SAL-HM-02", 0.0);

        deficit().given(faits(salarie, journees(salarie, 1))).penalizesBy(0);
        semaineVide().given(faits(salarie, List.of())).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 2. Comparaison au minimum
    // ---------------------------------------------------------

    @Test
    void minimumAtteint_pasDePenalite() {
        // 5 journées de 8 h = 40 h, minimum 35 h.
        SalarieReel salarie = salarieAvecMinimum("SAL-HM-03", 35.0);

        deficit().given(faits(salarie, journees(salarie, 5))).penalizesBy(0);
    }

    @Test
    void minimumExactementAtteint_pasDePenalite() {
        SalarieReel salarie = salarieAvecMinimum("SAL-HM-04", 24.0);

        deficit().given(faits(salarie, journees(salarie, 3))).penalizesBy(0);
    }

    @Test
    void deficit_penaliteEnMinutesManquantes() {
        // 3 journées de 8 h = 24 h pour un minimum de 35 h → 11 h manquantes = 660 min.
        SalarieReel salarie = salarieAvecMinimum("SAL-HM-05", 35.0);

        deficit().given(faits(salarie, journees(salarie, 3))).penalizesBy(PENALITE * 660);
    }

    // ---------------------------------------------------------
    // 3. Semaines complètes seulement
    // ---------------------------------------------------------

    @Test
    void semaineIncompleteDansLHorizon_nonJugee() {
        // Un minimum sur une semaine tronquée signalerait un déficit qui n'existe pas :
        // le total y est mécaniquement sous-évalué. C'est le mode de défaillance propre
        // aux minima, que les maxima n'ont pas.
        SalarieReel salarie = salarieAvecMinimum("SAL-HM-06", 35.0);

        List<Object> faits = new ArrayList<>(List.of(salarie, referentielAvecCharge(),
                TestPlanningContextFactory.contexteNeutre(LUNDI, LUNDI.plusDays(3))));
        faits.addAll(journees(salarie, 3));

        deficit().given(faits.toArray()).penalizesBy(0);
        semaineVide().given(salarie, referentielAvecCharge(),
                TestPlanningContextFactory.contexteNeutre(LUNDI, LUNDI.plusDays(3))).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 4. Périmètre du total
    // ---------------------------------------------------------

    @Test
    void activiteHorsCharge_neComptePasDansLeTotal() {
        // 2 journées de travail + 1 de formation : 16 h comptées, pas 24 h.
        SalarieReel salarie = salarieAvecMinimum("SAL-HM-07", 24.0);

        List<Creneau> creneaux = new ArrayList<>(journees(salarie, 2));
        creneaux.add(journeeHorsCharge("C-HM-07F", LUNDI.plusDays(2), salarie));

        List<Object> faits = new ArrayList<>(List.of(salarie, referentielMixte(),
                TestPlanningContextFactory.contexteNeutre(LUNDI, DIMANCHE)));
        faits.addAll(creneaux);

        deficit().given(faits.toArray()).penalizesBy(PENALITE * 480);
    }

    // ---------------------------------------------------------
    // 5. Le volet décisif : la semaine vide
    // ---------------------------------------------------------

    @Test
    void semaineCompleteSansAffectation_deficitMaximal() {
        SalarieReel salarie = salarieAvecMinimum("SAL-HM-08", 35.0);

        semaineVide().given(faits(salarie, List.of())).penalizesBy(PENALITE * 35 * 60);
    }

    @Test
    void semaineVide_coutePlusCherQuUneSemainePartielle() {
        // Garde-fou de l'ordre des coûts. S'il s'inversait, le solveur préférerait ne pas
        // employer le salarié plutôt que de l'employer partiellement — comportement
        // effectivement observé avant l'ajout du second volet.
        SalarieReel salarie = salarieAvecMinimum("SAL-HM-09", 35.0);

        int coutSemaineVide = PENALITE * 35 * 60;   // 105 000 — les 35 h manquent en entier
        int coutSemainePartielle = PENALITE * 660;  //  33 000 — 3 journées de 8 h, 11 h manquantes

        semaineVide().given(faits(salarie, List.of())).penalizesBy(coutSemaineVide);
        deficit().given(faits(salarie, journees(salarie, 3))).penalizesBy(coutSemainePartielle);

        org.junit.jupiter.api.Assertions.assertTrue(coutSemaineVide > coutSemainePartielle,
                "Ne rien confier doit toujours coûter plus cher que confier trop peu.");
    }

    @Test
    void uneSeuleJourneeAffectee_neDeclenchePasLeVoletSemaineVide() {
        // La semaine est pourvue dès le premier créneau : les deux volets ne se cumulent pas.
        SalarieReel salarie = salarieAvecMinimum("SAL-HM-10", 35.0);

        semaineVide().given(faits(salarie, journees(salarie, 1))).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private SingleConstraintVerification<PlanningProblem> deficit() {
        return constraintVerifier.verifyThat(
                (p, factory) -> HeuresMinimumParSemaine.heuresMinimumParSemaine(factory));
    }

    private SingleConstraintVerification<PlanningProblem> semaineVide() {
        return constraintVerifier.verifyThat(
                (p, factory) -> HeuresMinimumParSemaine.semaineSansAffectation(factory));
    }

    private static Object[] faits(SalarieReel salarie, List<Creneau> creneaux) {
        List<Object> faits = new ArrayList<>();
        faits.add(salarie);
        faits.add(referentielAvecCharge());
        faits.add(TestPlanningContextFactory.contexteNeutre(LUNDI, DIMANCHE));
        faits.addAll(creneaux);
        return faits.toArray();
    }

    private static SalarieReel salarieAvecMinimum(String id, double heures) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, null, heures, null, null, null,
                null, null, null, null, null));
        return salarie;
    }

    /** {@code nb} journées de 8 h à partir du lundi. */
    private static List<Creneau> journees(SalarieReel salarie, int nb) {
        List<Creneau> creneaux = new ArrayList<>();
        for (int i = 0; i < nb; i++) {
            creneaux.add(journee("C-HM-" + i, LUNDI.plusDays(i), salarie));
        }
        return creneaux;
    }

    private static Creneau journee(String id, LocalDate date, SalarieReel salarie) {
        return creneau(id, date, TestPlanningRequestFactory.ACTIVITE_TRAVAIL, salarie);
    }

    private static Creneau journeeHorsCharge(String id, LocalDate date, SalarieReel salarie) {
        return creneau(id, date, ACTIVITE_HORS_CHARGE, salarie);
    }

    private static Creneau creneau(String id, LocalDate date, String codeActiviteId, SalarieReel salarie) {
        Creneau c = new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                TestRessourceFactory.SITE_CANON,
                codeActiviteId,
                null,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
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
