package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.NuitsConsecutivesMax;
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
import org.optaplanner.test.api.score.stream.SingleConstraintVerification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NuitsConsecutivesMaxConstraintsTest — lot S7.2
 *
 * <p>Valide la contrainte HARD R3 « nuits consécutives maximum », remise en service au lot S7.2.
 * Aucun test ne la couvrait jusqu'ici : elle était enregistrée, muette, et personne ne
 * l'interrogeait.</p>
 *
 * <p>Le comptage porte sur des <strong>dates calendaires distinctes et consécutives</strong>.
 * Les créneaux portent {@code codeActiviteId}, comme les clients réels.</p>
 *
 * <p>Cas couverts :</p>
 * <ol>
 *   <li>Plafond non transmis → contrainte inactive</li>
 *   <li>Plafond transmis à 0 → contrainte inactive</li>
 *   <li>Séquence égale au plafond → pas de violation</li>
 *   <li>Séquence supérieure au plafond → violation HARD</li>
 *   <li>Nuits non consécutives → pas de violation, même à effectif égal</li>
 *   <li>Deux créneaux de nuit la même date → une seule nuit</li>
 *   <li>Créneau de jour intercalé → n'interrompt pas la séquence de nuits mais n'y compte pas</li>
 *   <li>Activité hors charge → n'entre pas dans la séquence</li>
 *   <li>Nuits hors horizon → non comptées</li>
 * </ol>
 */
class NuitsConsecutivesMaxConstraintsTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final String ACTIVITE_HORS_CHARGE = "formation";

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1-2. Activation
    // ---------------------------------------------------------

    @Test
    void plafondNonTransmis_contrainteInactive() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NC-01");

        verifier().given(faits(salarie, nuits(salarie, 5))).penalizesBy(0);
    }

    @Test
    void plafondTransmisAZero_contrainteInactive() {
        // Le contrat impose d'omettre le champ ; un 0 reçu est lu comme une désactivation
        // et non comme « aucune nuit consécutive autorisée ».
        SalarieReel salarie = salarieAvecPlafond("SAL-NC-02", 0);

        verifier().given(faits(salarie, nuits(salarie, 5))).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 3-4. Comparaison au plafond
    // ---------------------------------------------------------

    @Test
    void sequenceEgaleAuPlafond_pasDeViolation() {
        SalarieReel salarie = salarieAvecPlafond("SAL-NC-03", 3);

        verifier().given(faits(salarie, nuits(salarie, 3))).penalizesBy(0);
    }

    @Test
    void sequenceSuperieureAuPlafond_violationHard() {
        SalarieReel salarie = salarieAvecPlafond("SAL-NC-04", 3);

        verifier().given(faits(salarie, nuits(salarie, 4))).penalizesBy(1);
    }

    // ---------------------------------------------------------
    // 5-6. Ce que « consécutif » veut dire
    // ---------------------------------------------------------

    @Test
    void nuitsNonConsecutives_pasDeViolation() {
        // 4 nuits au total, mais jamais plus de 2 d'affilée : lundi, mardi, jeudi, vendredi.
        SalarieReel salarie = salarieAvecPlafond("SAL-NC-05", 2);

        List<Creneau> creneaux = List.of(
                nuit("C-NC-05A", LUNDI,               salarie),
                nuit("C-NC-05B", LUNDI.plusDays(1),   salarie),
                nuit("C-NC-05C", LUNDI.plusDays(3),   salarie),
                nuit("C-NC-05D", LUNDI.plusDays(4),   salarie));

        verifier().given(faits(salarie, creneaux)).penalizesBy(0);
    }

    @Test
    void deuxCreneauxLaMemeNuit_comptentPourUne() {
        // Le comptage se fait par date distincte : deux créneaux le lundi soir restent une nuit.
        SalarieReel salarie = salarieAvecPlafond("SAL-NC-06", 2);

        List<Creneau> creneaux = List.of(
                nuit("C-NC-06A", LUNDI,             salarie),
                nuit("C-NC-06B", LUNDI,             salarie),
                nuit("C-NC-06C", LUNDI.plusDays(1), salarie));

        verifier().given(faits(salarie, creneaux)).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 7-8. Périmètre du comptage
    // ---------------------------------------------------------

    @Test
    void creneauDeJourIntercale_neCompensePasEtNeCptePas() {
        // Le mardi de jour ne rompt pas la suite des DATES de nuit : lundi et mercredi ne sont
        // pas consécutifs, la séquence reste donc à 1. La règle ne compte que des nuits.
        SalarieReel salarie = salarieAvecPlafond("SAL-NC-07", 1);

        List<Creneau> creneaux = List.of(
                nuit("C-NC-07A", LUNDI,             salarie),
                journee("C-NC-07B", LUNDI.plusDays(1), salarie),
                nuit("C-NC-07C", LUNDI.plusDays(2), salarie));

        verifier().given(faits(salarie, creneaux)).penalizesBy(0);
    }

    @Test
    void activiteHorsCharge_nEntrePasDansLaSequence() {
        // 3 nuits calendaires, mais celle du mardi ne compte pas dans la charge : la plus longue
        // séquence retombe à 1. Avec une activité chargée, ces mêmes créneaux violeraient.
        SalarieReel salarie = salarieAvecPlafond("SAL-NC-08", 2);

        List<Creneau> creneaux = List.of(
                nuit("C-NC-08A", LUNDI,             salarie),
                nuitHorsCharge("C-NC-08B", LUNDI.plusDays(1), salarie),
                nuit("C-NC-08C", LUNDI.plusDays(2), salarie));

        verifier().given(salarie, referentielMixte(),
                        TestPlanningContextFactory.contexteNeutre(LUNDI, LUNDI.plusDays(13)),
                        creneaux.get(0), creneaux.get(1), creneaux.get(2))
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 9. Horizon
    // ---------------------------------------------------------

    @Test
    void nuitsHorsHorizon_nonComptees() {
        // Horizon réduit aux trois premiers jours : les nuits suivantes sortent du périmètre.
        SalarieReel salarie = salarieAvecPlafond("SAL-NC-09", 3);
        PlanningContext contexte = TestPlanningContextFactory.contexteNeutre(LUNDI, LUNDI.plusDays(2));

        List<Object> faits = new ArrayList<>(List.of(salarie, referentielAvecCharge(), contexte));
        faits.addAll(nuits(salarie, 5));

        verifier().given(faits.toArray()).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private SingleConstraintVerification<PlanningProblem> verifier() {
        return constraintVerifier.verifyThat(
                (p, factory) -> NuitsConsecutivesMax.maxNuitsConsecutives(factory));
    }

    private static Object[] faits(SalarieReel salarie, List<Creneau> creneaux) {
        List<Object> faits = new ArrayList<>();
        faits.add(salarie);
        faits.add(referentielAvecCharge());
        faits.add(TestPlanningContextFactory.contexteNeutre(LUNDI, LUNDI.plusDays(13)));
        faits.addAll(creneaux);
        return faits.toArray();
    }

    private static SalarieReel salarieAvecPlafond(String id, int plafond) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, null, null, null, null, null,
                plafond, null, null, null, null));
        return salarie;
    }

    /** {@code nb} nuits calendaires consécutives à partir du lundi. */
    private static List<Creneau> nuits(SalarieReel salarie, int nb) {
        List<Creneau> creneaux = new ArrayList<>();
        for (int i = 0; i < nb; i++) {
            creneaux.add(nuit("C-NC-" + i, LUNDI.plusDays(i), salarie));
        }
        return creneaux;
    }

    private static Creneau nuit(String id, LocalDate date, SalarieReel salarie) {
        return creneau(id, date, LocalTime.of(22, 0), LocalTime.of(6, 0),
                TypePlageHoraire.NUIT, TestPlanningRequestFactory.ACTIVITE_TRAVAIL, salarie);
    }

    private static Creneau nuitHorsCharge(String id, LocalDate date, SalarieReel salarie) {
        return creneau(id, date, LocalTime.of(22, 0), LocalTime.of(6, 0),
                TypePlageHoraire.NUIT, ACTIVITE_HORS_CHARGE, salarie);
    }

    private static Creneau journee(String id, LocalDate date, SalarieReel salarie) {
        return creneau(id, date, LocalTime.of(8, 0), LocalTime.of(16, 0),
                TypePlageHoraire.JOUR, TestPlanningRequestFactory.ACTIVITE_TRAVAIL, salarie);
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
