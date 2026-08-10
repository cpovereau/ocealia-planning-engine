package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.DureeMaximaleLegaleParSalarie;
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
import org.optaplanner.test.api.score.stream.SingleConstraintVerification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DureeMaximaleParJourConstraintsTest — lot S7.6
 *
 * <p>Valide le plafond de <strong>durée travaillée par journée</strong>
 * ({@code contraintesReglementaires.heuresMaximumParJour}).</p>
 *
 * <h3>Ce que ce test fixe surtout : la maille</h3>
 * <p>Avant le lot S7.6, la contrainte agrégeait sur <strong>tout l'horizon</strong> et comparait
 * ce cumul à une constante journalière de 780 minutes. Elle mesurait une période et la comparait
 * à un jour. Plusieurs cas ci-dessous existent uniquement pour interdire le retour de ce défaut.</p>
 *
 * <p>Cas couverts :</p>
 * <ol>
 *   <li>Plafond non transmis → contrainte inactive</li>
 *   <li>Plafond transmis à 0 → contrainte inactive</li>
 *   <li>Journée sous le plafond → pas de pénalité</li>
 *   <li>Journée exactement au plafond → pas de pénalité</li>
 *   <li>Journée au-dessus → pénalité = minutes excédentaires</li>
 *   <li>Deux journées conformes → aucun cumul entre elles</li>
 *   <li>Deux journées en dépassement → une pénalité par journée</li>
 *   <li>Deux créneaux le même jour → ils s'additionnent</li>
 *   <li>Activité hors charge → exclue du cumul journalier</li>
 * </ol>
 */
class DureeMaximaleParJourConstraintsTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final String ACTIVITE_HORS_CHARGE = "formation";

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1-2. Activation
    // ---------------------------------------------------------

    @Test
    void plafondNonTransmis_contrainteInactive() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-DJ-01");

        verifier().given(faits(salarie, List.of(creneau("C-DJ-01", LUNDI, 720, salarie)))).penalizesBy(0);
    }

    @Test
    void plafondTransmisAZero_contrainteInactive() {
        SalarieReel salarie = salarieAvecPlafond("SAL-DJ-02", 0.0);

        verifier().given(faits(salarie, List.of(creneau("C-DJ-02", LUNDI, 720, salarie)))).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 3-5. Comparaison au plafond
    // ---------------------------------------------------------

    @Test
    void journeeSousLePlafond_pasDePenalite() {
        SalarieReel salarie = salarieAvecPlafond("SAL-DJ-03", 10.0);

        verifier().given(faits(salarie, List.of(creneau("C-DJ-03", LUNDI, 480, salarie)))).penalizesBy(0);
    }

    @Test
    void journeeExactementAuPlafond_pasDePenalite() {
        SalarieReel salarie = salarieAvecPlafond("SAL-DJ-04", 8.0);

        verifier().given(faits(salarie, List.of(creneau("C-DJ-04", LUNDI, 480, salarie)))).penalizesBy(0);
    }

    @Test
    void journeeAuDessus_penaliteEnMinutesExcedentaires() {
        // 10 h travaillées pour un plafond de 8 h → 120 minutes.
        SalarieReel salarie = salarieAvecPlafond("SAL-DJ-05", 8.0);

        verifier().given(faits(salarie, List.of(creneau("C-DJ-05", LUNDI, 600, salarie)))).penalizesBy(120);
    }

    // ---------------------------------------------------------
    // 6-7. La maille est la journée — le défaut corrigé au lot S7.6
    // ---------------------------------------------------------

    @Test
    void deuxJourneesConformes_aucunCumulEntreElles() {
        // 8 h + 8 h = 16 h sur l'horizon, mais chaque journée respecte son plafond de 10 h.
        // L'implémentation antérieure aurait comparé 960 minutes à un seuil journalier.
        SalarieReel salarie = salarieAvecPlafond("SAL-DJ-06", 10.0);

        List<Creneau> creneaux = List.of(
                creneau("C-DJ-06A", LUNDI,             480, salarie),
                creneau("C-DJ-06B", LUNDI.plusDays(1), 480, salarie));

        verifier().given(faits(salarie, creneaux)).penalizesBy(0);
    }

    @Test
    void deuxJourneesEnDepassement_unePenaliteParJournee() {
        SalarieReel salarie = salarieAvecPlafond("SAL-DJ-07", 6.0);

        List<Creneau> creneaux = List.of(
                creneau("C-DJ-07A", LUNDI,             480, salarie),   // +120
                creneau("C-DJ-07B", LUNDI.plusDays(1), 540, salarie));  // +180

        verifier().given(faits(salarie, creneaux)).penalizesBy(300);
    }

    // ---------------------------------------------------------
    // 8-9. Composition d'une journée
    // ---------------------------------------------------------

    @Test
    void deuxCreneauxLeMemeJour_sAdditionnent() {
        // 4 h le matin, 6 h le soir : 10 h pour un plafond de 8 h → 120 minutes.
        SalarieReel salarie = salarieAvecPlafond("SAL-DJ-08", 8.0);

        List<Creneau> creneaux = List.of(
                creneau("C-DJ-08A", LUNDI, 240, salarie),
                creneau("C-DJ-08B", LUNDI, 360, salarie));

        verifier().given(faits(salarie, creneaux)).penalizesBy(120);
    }

    @Test
    void activiteHorsCharge_exclueDuCumul() {
        // 8 h de travail + 4 h de formation : seules les 8 h comptent, plafond de 8 h respecté.
        SalarieReel salarie = salarieAvecPlafond("SAL-DJ-09", 8.0);

        List<Object> faits = new ArrayList<>(List.of(salarie, referentielMixte()));
        faits.add(creneau("C-DJ-09A", LUNDI, 480, salarie));
        faits.add(creneauHorsCharge("C-DJ-09B", LUNDI, 240, salarie));

        verifier().given(faits.toArray()).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private SingleConstraintVerification<PlanningProblem> verifier() {
        return constraintVerifier.verifyThat(
                (p, factory) -> DureeMaximaleLegaleParSalarie.dureeMaximaleLegaleParSalarie(factory));
    }

    /** La contrainte ne consulte pas le contexte : horizon et pénalités lui sont indifférents. */
    private static Object[] faits(SalarieReel salarie, List<Creneau> creneaux) {
        List<Object> faits = new ArrayList<>();
        faits.add(salarie);
        faits.add(referentielAvecCharge());
        faits.addAll(creneaux);
        return faits.toArray();
    }

    private static SalarieReel salarieAvecPlafond(String id, double heures) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, heures, null, null, null, null, null, null,
                null, null, null, null, null));
        return salarie;
    }

    private static Creneau creneau(String id, LocalDate date, int dureeMinutes, SalarieReel salarie) {
        return creneau(id, date, dureeMinutes, TestPlanningRequestFactory.ACTIVITE_TRAVAIL, salarie);
    }

    private static Creneau creneauHorsCharge(String id, LocalDate date, int dureeMinutes, SalarieReel salarie) {
        return creneau(id, date, dureeMinutes, ACTIVITE_HORS_CHARGE, salarie);
    }

    private static Creneau creneau(String id, LocalDate date, int dureeMinutes,
                                   String codeActiviteId, SalarieReel salarie) {
        Creneau c = new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(8, 0).plusMinutes(dureeMinutes), dureeMinutes,
                TestRessourceFactory.SITE_CANON,
                codeActiviteId,
                null,                       // activite (déprécié) volontairement vide
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
