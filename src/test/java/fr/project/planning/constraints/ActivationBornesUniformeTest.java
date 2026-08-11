package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.AmplitudeJournaliere;
import fr.project.planning.constraints.legales.HeuresMaximumParSemaine;
import fr.project.planning.constraints.legales.HeuresMinimumParJour;
import fr.project.planning.constraints.legales.JoursConsecutifsMax;
import fr.project.planning.constraints.legales.NuitsMaximumParSemaine;
import fr.project.planning.constraints.legales.ReposQuotidienMinimum;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ActivationBornesUniformeTest — lot S8.3
 *
 * <p>{@link ContraintesReglementairesSalarie#borneRenseignee(Number)} annonce être la source
 * unique de la règle d'activation. Elle ne l'était que de sept contraintes sur douze :
 * {@code AmplitudeJournaliere}, {@code HeuresMaximumParSemaine}, {@code HeuresMinimumParJour},
 * {@code JoursConsecutifsMax} et {@code ReposQuotidienMinimum} testaient {@code != null} en
 * direct.</p>
 *
 * <p>La divergence se voit sur une <strong>borne négative</strong> : les sept premières la
 * tiennent pour non renseignée et s'abstiennent ; les cinq autres l'appliquaient à la lettre et
 * s'activaient avec un seuil négatif. Un même {@code -1} transmis par WinDev produisait donc deux
 * comportements opposés selon la règle qui le lisait — et sur un maximum, un seuil négatif est
 * dépassé par n'importe quelle affectation.</p>
 *
 * <p>Chaque test compare la contrainte corrigée à {@link NuitsMaximumParSemaine}, témoin resté
 * conforme depuis S7.7a.</p>
 */
class ActivationBornesUniformeTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // -------------------------------------------------------
    // La règle elle-même
    // -------------------------------------------------------

    @Test
    @DisplayName("Une borne négative ne décrit aucun seuil : elle vaut absence")
    void borneNegative_estTenuePourNonRenseignee() {
        assertFalse(ContraintesReglementairesSalarie.borneRenseignee(-1));
        assertFalse(ContraintesReglementairesSalarie.borneRenseignee(-0.5));
        assertFalse(ContraintesReglementairesSalarie.borneRenseignee(null));

        // Le zéro, lui, reste littéral — arbitrage du lot S7.7.
        assertTrue(ContraintesReglementairesSalarie.borneRenseignee(0));
        assertTrue(ContraintesReglementairesSalarie.borneRenseignee(3));
    }

    // -------------------------------------------------------
    // Témoin : la contrainte déjà conforme
    // -------------------------------------------------------

    @Test
    @DisplayName("Témoin — NuitsMaximumParSemaine s'abstient sur une borne négative")
    void temoin_nuitsMaximumParSemaine_borneNegative_doitResterSilencieuse() {
        SalarieReel salarie = salarieAvecSeuils(b -> b.nuitsMaximumParSemaine = -1);

        constraintVerifier
                .verifyThat((provider, factory) -> NuitsMaximumParSemaine.nuitsMaximumParSemaine(factory))
                .given(salarie, referentiel(), contexte(),
                        creneauNuit("C-N1", LUNDI, salarie),
                        creneauNuit("C-N2", LUNDI.plusDays(1), salarie))
                .penalizesBy(0);
    }

    // -------------------------------------------------------
    // Les cinq contraintes réalignées
    // -------------------------------------------------------

    @Test
    @DisplayName("AmplitudeJournaliere s'abstient sur une amplitude négative")
    void amplitudeJournaliere_borneNegative_doitResterSilencieuse() {
        // Avant S8.3 : seuil de -60 minutes, dépassé de 540 par une journée de 8 h.
        SalarieReel salarie = salarieAvecSeuils(b -> b.amplitudeJournaliereMaximum = -1.0);

        constraintVerifier
                .verifyThat((provider, factory) -> AmplitudeJournaliere.amplitudeJournaliere(factory))
                .given(salarie, referentiel(), contexte(), creneauJour("C-A", LUNDI, salarie))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("HeuresMaximumParSemaine s'abstient sur un plafond négatif")
    void heuresMaximumParSemaine_borneNegative_doitResterSilencieuse() {
        SalarieReel salarie = salarieAvecSeuils(b -> b.heuresMaximumParSemaine = -1.0);

        constraintVerifier
                .verifyThat((provider, factory) -> HeuresMaximumParSemaine.heuresMaximumParSemaine(factory))
                .given(salarie, referentiel(), contexte(), creneauJour("C-H", LUNDI, salarie))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("HeuresMinimumParJour s'abstient sur un minimum négatif")
    void heuresMinimumParJour_borneNegative_doitResterSilencieuse() {
        SalarieReel salarie = salarieAvecSeuils(b -> b.heuresMinimumParJour = -1.0);

        constraintVerifier
                .verifyThat((provider, factory) -> HeuresMinimumParJour.heuresMinimumParJour(factory))
                .given(salarie, referentiel(), contexte(), creneauJour("C-M", LUNDI, salarie))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("JoursConsecutifsMax s'abstient sur un plafond négatif")
    void joursConsecutifsMax_borneNegative_doitResterSilencieuse() {
        // Avant S8.3 : plafond -1, dépassé de 2 par deux jours travaillés d'affilée.
        SalarieReel salarie = salarieAvecSeuils(b -> b.joursConsecutifsMaximum = -1);

        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, referentiel(), contexte(),
                        creneauJour("C-J1", LUNDI, salarie),
                        creneauJour("C-J2", LUNDI.plusDays(1), salarie))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("ReposQuotidienMinimum s'abstient sur un repos négatif")
    void reposQuotidienMinimum_borneNegative_doitResterSilencieuse() {
        SalarieReel salarie = salarieAvecSeuils(b -> b.reposQuotidienMinimum = -1.0);

        constraintVerifier
                .verifyThat((provider, factory) -> ReposQuotidienMinimum.reposQuotidienMinimum(factory))
                .given(salarie, referentiel(), contexte(),
                        creneauJour("C-R1", LUNDI, salarie),
                        creneauJour("C-R2", LUNDI.plusDays(1), salarie))
                .penalizesBy(0);
    }

    // -------------------------------------------------------
    // Non-régression : une borne renseignée reste appliquée
    // -------------------------------------------------------

    @Test
    @DisplayName("La correction n'a pas éteint les règles : un seuil réel reste appliqué")
    void borneRenseignee_resteAppliquee() {
        // Amplitude de 4 h contre une journée de 8 h : 240 minutes de dépassement,
        // au poids amplitude de 50.
        SalarieReel salarie = salarieAvecSeuils(b -> b.amplitudeJournaliereMaximum = 4.0);

        constraintVerifier
                .verifyThat((provider, factory) -> AmplitudeJournaliere.amplitudeJournaliere(factory))
                .given(salarie, referentiel(), contexte(), creneauJour("C-A", LUNDI, salarie))
                .penalizesBy(240 * 50);
    }

    @Test
    @DisplayName("Un plafond à zéro reste lu à la lettre et interdit tout")
    void borneAZero_resteLitterale() {
        // Le zéro n'est pas un vide : arbitrage S7.7, que S8.3 ne remet pas en cause.
        SalarieReel salarie = salarieAvecSeuils(b -> b.joursConsecutifsMaximum = 0);

        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, referentiel(), contexte(), creneauJour("C-J", LUNDI, salarie))
                .penalizesBy(TestPlanningRequestFactory.buildPlanningContextSc01()
                        .getPenalites().getDepassementMaxJoursConsecutifs());
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    /** Jeu de seuils vierge, dont le test ne renseigne que celui qu'il éprouve. */
    private static final class Seuils {
        Double heuresMinimumParJour;
        Double amplitudeJournaliereMaximum;
        Double reposQuotidienMinimum;
        Double heuresMaximumParSemaine;
        Integer nuitsMaximumParSemaine;
        Integer joursConsecutifsMaximum;
    }

    private SalarieReel salarieAvecSeuils(java.util.function.Consumer<Seuils> reglage) {
        Seuils s = new Seuils();
        reglage.accept(s);

        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-SEUIL");
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                s.heuresMinimumParJour,
                null,                            // heuresMaximumParJour
                s.amplitudeJournaliereMaximum,
                s.reposQuotidienMinimum,
                null,                            // heuresMinimumParSemaine
                s.heuresMaximumParSemaine,
                s.nuitsMaximumParSemaine,
                s.joursConsecutifsMaximum));
        return salarie;
    }

    private ReferentielComptabiliteActivite referentiel() {
        return TestPlanningRequestFactory.buildReferentielSc01();
    }

    private PlanningContext contexte() {
        return TestPlanningRequestFactory.buildPlanningContextSc01();
    }

    private Creneau creneauJour(String id, LocalDate date, Ressource ressource) {
        return creneau(id, date, LocalTime.of(8, 0), LocalTime.of(16, 0),
                TypePlageHoraire.JOUR, ressource);
    }

    private Creneau creneauNuit(String id, LocalDate date, Ressource ressource) {
        return creneau(id, date, LocalTime.of(22, 0), LocalTime.of(6, 0),
                TypePlageHoraire.NUIT, ressource);
    }

    private Creneau creneau(String id, LocalDate date, LocalTime debut, LocalTime fin,
                            TypePlageHoraire plage, Ressource ressource) {
        Creneau c = new Creneau(
                id, date, debut, fin, 480,
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, plage,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }
}
