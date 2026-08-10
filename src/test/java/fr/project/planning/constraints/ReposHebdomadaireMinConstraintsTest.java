package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.ReposHebdomadaireMin;
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
import org.optaplanner.test.api.score.stream.SingleConstraintVerification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ReposHebdomadaireMinConstraintsTest — lot S7.5
 *
 * <p>Valide le <strong>plancher légal</strong> de R7 : sur toute fenêtre glissante de sept jours,
 * au moins un jour sans travail.</p>
 *
 * <h3>La seule contrainte du chantier sans seuil individuel</h3>
 * <p>Un plancher légal ne se négocie pas au contrat. Aucun salarié de ces tests ne porte donc de
 * {@code contraintesReglementaires} : la contrainte s'applique du seul fait qu'elle existe. C'est
 * ce qui la distingue de {@code ReposHebdomadaireGlissant}, son volet conventionnel.</p>
 *
 * <p>Cas couverts :</p>
 * <ol>
 *   <li>Sept jours d'affilée → violation HARD, sans qu'aucun seuil soit transmis</li>
 *   <li>Six jours travaillés → pas de violation</li>
 *   <li>Jour de repos au milieu → pas de violation</li>
 *   <li>Huit jours d'affilée → une seule violation par salarié</li>
 *   <li>Sept jours dont un en activité hors charge → pas de violation</li>
 *   <li>Sept jours hors horizon → non comptés</li>
 *   <li>Aucun créneau → pas de violation</li>
 * </ol>
 */
class ReposHebdomadaireMinConstraintsTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final String ACTIVITE_HORS_CHARGE = "formation";

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    @Test
    void septJoursDAffilee_violationHard() {
        // Le salarié ne porte aucune contrainte réglementaire : le plancher s'applique quand même.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RM-01");

        verifier().given(faits(salarie, joursTravailles(salarie, 0, 1, 2, 3, 4, 5, 6))).penalizesBy(1);
    }

    @Test
    void sixJoursTravailles_pasDeViolation() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RM-02");

        verifier().given(faits(salarie, joursTravailles(salarie, 0, 1, 2, 3, 4, 5))).penalizesBy(0);
    }

    @Test
    void jourDeReposAuMilieu_pasDeViolation() {
        // Huit jours couverts, mais le jeudi est off : aucune fenêtre de 7 n'est pleine.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RM-03");

        verifier().given(faits(salarie, joursTravailles(salarie, 0, 1, 2, 4, 5, 6, 7))).penalizesBy(0);
    }

    @Test
    void huitJoursDAffilee_uneSeuleViolationParSalarie() {
        // Deux fenêtres de 7 sont pleines, mais la contrainte se déclenche une fois par salarié :
        // elle constate une situation, elle ne compte pas des occurrences.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RM-04");

        verifier().given(faits(salarie, joursTravailles(salarie, 0, 1, 2, 3, 4, 5, 6, 7))).penalizesBy(1);
    }

    @Test
    void septJoursDontUnHorsCharge_pasDeViolation() {
        // Une formation ne remplit pas la journée au sens de la charge : c'est un jour off.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RM-05");

        List<Creneau> creneaux = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            creneaux.add(i == 3
                    ? journeeHorsCharge("C-RM-05-" + i, LUNDI.plusDays(i), salarie)
                    : journee("C-RM-05-" + i, LUNDI.plusDays(i), salarie));
        }

        List<Object> faits = new ArrayList<>(List.of(salarie, referentielMixte(),
                TestPlanningContextFactory.contexteNeutre(LUNDI, LUNDI.plusDays(13))));
        faits.addAll(creneaux);

        verifier().given(faits.toArray()).penalizesBy(0);
    }

    @Test
    void joursHorsHorizon_nonComptes() {
        // Horizon réduit aux trois premiers jours : les quatre suivants sortent du périmètre.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RM-06");

        List<Object> faits = new ArrayList<>(List.of(salarie, referentielAvecCharge(),
                TestPlanningContextFactory.contexteNeutre(LUNDI, LUNDI.plusDays(2))));
        faits.addAll(joursTravailles(salarie, 0, 1, 2, 3, 4, 5, 6));

        verifier().given(faits.toArray()).penalizesBy(0);
    }

    @Test
    void aucunCreneau_pasDeViolation() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RM-07");

        verifier().given(faits(salarie, List.of())).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private SingleConstraintVerification<PlanningProblem> verifier() {
        return constraintVerifier.verifyThat(
                (p, factory) -> ReposHebdomadaireMin.reposHebdomadaireMin(factory));
    }

    private static Object[] faits(SalarieReel salarie, List<Creneau> creneaux) {
        List<Object> faits = new ArrayList<>();
        faits.add(salarie);
        faits.add(referentielAvecCharge());
        faits.add(TestPlanningContextFactory.contexteNeutre(LUNDI, LUNDI.plusDays(13)));
        faits.addAll(creneaux);
        return faits.toArray();
    }

    private static List<Creneau> joursTravailles(SalarieReel salarie, int... decalages) {
        List<Creneau> creneaux = new ArrayList<>();
        for (int d : decalages) {
            creneaux.add(journee("C-RM-" + d, LUNDI.plusDays(d), salarie));
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
