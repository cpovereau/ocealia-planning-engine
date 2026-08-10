package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.ReposHebdomadaireGlissant;
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
 * ReposHebdomadaireGlissantConstraintsTest — lot S7.4
 *
 * <p>Valide le volet <strong>conventionnel</strong> de R7 : un nombre minimal de jours non
 * travaillés dans toute fenêtre glissante de largeur donnée. Le plancher légal — au moins un jour
 * off sur sept — relève de {@code ReposHebdomadaireMin} et se teste à part.</p>
 *
 * <p>Cas couverts :</p>
 * <ol>
 *   <li>Paire non transmise → contrainte inactive</li>
 *   <li>Fenêtre seule → contrainte inactive (la paire est indissociable)</li>
 *   <li>Minimum de jours off seul → contrainte inactive</li>
 *   <li>Minimum de jours off à 0 → aucune exigence ; fenêtre à 0 → contrainte inactive</li>
 *   <li>Jours off suffisants → pas de violation</li>
 *   <li>Jours off exactement au minimum → pas de violation</li>
 *   <li>Jours off insuffisants → violation HARD</li>
 *   <li>Repos dispersés mais jamais assez dans une même fenêtre → violation</li>
 *   <li>Activité hors charge → compte comme un jour off</li>
 * </ol>
 */
class ReposHebdomadaireGlissantConstraintsTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final String ACTIVITE_HORS_CHARGE = "formation";

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1-4. Activation, et le caractère indissociable de la paire
    // ---------------------------------------------------------

    @Test
    void paireNonTransmise_contrainteInactive() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RG-01");

        verifier().given(faits(salarie, joursTravailles(salarie, 0, 1, 2, 3, 4, 5, 6))).penalizesBy(0);
    }

    @Test
    void fenetreSeule_contrainteInactive() {
        // Une fenêtre sans minimum de jours off n'interdit rien.
        SalarieReel salarie = salarieAvecPaire("SAL-RG-02", 7, null);

        verifier().given(faits(salarie, joursTravailles(salarie, 0, 1, 2, 3, 4, 5, 6))).penalizesBy(0);
    }

    @Test
    void minimumSeul_contrainteInactive() {
        // Un minimum sans fenêtre ne s'applique nulle part.
        SalarieReel salarie = salarieAvecPaire("SAL-RG-03", null, 2);

        verifier().given(faits(salarie, joursTravailles(salarie, 0, 1, 2, 3, 4, 5, 6))).penalizesBy(0);
    }

    @Test
    void minimumAZero_aucuneExigence() {
        // Lecture littérale (lot S7.7) : 0 jour off exigé sur une fenêtre de 7.
        // La contrainte s'applique et n'est jamais violée — un minimum de zéro est
        // toujours atteint. C'est le pendant du maximum à zéro, qui interdit tout.
        SalarieReel salarie = salarieAvecPaire("SAL-RG-04", 7, 0);

        verifier().given(faits(salarie, joursTravailles(salarie, 0, 1, 2, 3, 4, 5, 6))).penalizesBy(0);
    }

    @Test
    void fenetreAZero_contrainteInactive() {
        // Une fenêtre est une TAILLE, pas une borne : 0 jour ne décrit aucune règle.
        // Seul champ du contrat où le zéro n'a aucune lecture littérale possible.
        SalarieReel salarie = salarieAvecPaire("SAL-RG-04b", 0, 2);

        verifier().given(faits(salarie, joursTravailles(salarie, 0, 1, 2, 3, 4, 5, 6))).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 5-7. Comparaison au minimum
    // ---------------------------------------------------------

    @Test
    void joursOffSuffisants_pasDeViolation() {
        // Fenêtre de 7, minimum 2 jours off. Travail lundi à vendredi : 2 jours off.
        SalarieReel salarie = salarieAvecPaire("SAL-RG-05", 7, 2);

        verifier().given(faits(salarie, joursTravailles(salarie, 0, 1, 2, 3, 4))).penalizesBy(0);
    }

    @Test
    void joursOffExactementAuMinimum_pasDeViolation() {
        // Fenêtre de 5, minimum 1. Travail lundi à jeudi : la fenêtre lundi-vendredi a 1 jour off.
        SalarieReel salarie = salarieAvecPaire("SAL-RG-06", 5, 1);

        verifier().given(faits(salarie, joursTravailles(salarie, 0, 1, 2, 3))).penalizesBy(0);
    }

    @Test
    void joursOffInsuffisants_violationHard() {
        // Fenêtre de 7, minimum 2. Travail 6 jours d'affilée : 1 seul jour off.
        SalarieReel salarie = salarieAvecPaire("SAL-RG-07", 7, 2);

        verifier().given(faits(salarie, joursTravailles(salarie, 0, 1, 2, 3, 4, 5))).penalizesBy(1);
    }

    // ---------------------------------------------------------
    // 8-9. Périmètre
    // ---------------------------------------------------------

    @Test
    void reposDisperses_maisJamaisAssezDansUneMemeFenetre_violation() {
        // Fenêtre de 4, minimum 2. Repos le mercredi et le dimanche : la fenêtre
        // jeudi-dimanche ne compte qu'un jour off.
        SalarieReel salarie = salarieAvecPaire("SAL-RG-08", 4, 2);

        verifier().given(faits(salarie, joursTravailles(salarie, 0, 1, 3, 4, 5))).penalizesBy(1);
    }

    @Test
    void activiteHorsCharge_compteCommeJourOff() {
        // 7 jours couverts, mais le mercredi et le samedi sont des formations :
        // au sens de la charge, ce sont des jours off. Fenêtre de 7, minimum 2 → conforme.
        SalarieReel salarie = salarieAvecPaire("SAL-RG-09", 7, 2);

        List<Creneau> creneaux = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            creneaux.add(i == 2 || i == 5
                    ? journeeHorsCharge("C-RG-09-" + i, LUNDI.plusDays(i), salarie)
                    : journee("C-RG-09-" + i, LUNDI.plusDays(i), salarie));
        }

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
                (p, factory) -> ReposHebdomadaireGlissant.reposHebdoGlissant(factory));
    }

    private static Object[] faits(SalarieReel salarie, List<Creneau> creneaux) {
        List<Object> faits = new ArrayList<>();
        faits.add(salarie);
        faits.add(referentielAvecCharge());
        faits.add(TestPlanningContextFactory.contexteNeutre(LUNDI, LUNDI.plusDays(13)));
        faits.addAll(creneaux);
        return faits.toArray();
    }

    private static SalarieReel salarieAvecPaire(String id, Integer fenetre, Integer joursOff) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, null, null, null, null, null,
                null, null, null, fenetre, joursOff));
        return salarie;
    }

    /** Une journée de travail pour chaque décalage donné, à partir du lundi. */
    private static List<Creneau> joursTravailles(SalarieReel salarie, int... decalages) {
        List<Creneau> creneaux = new ArrayList<>();
        for (int d : decalages) {
            creneaux.add(journee("C-RG-" + d, LUNDI.plusDays(d), salarie));
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
