package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.DimanchesTravaillesMax;
import fr.project.planning.constraints.legales.DureeMaximaleLegaleParSalarie;
import fr.project.planning.constraints.legales.NuitsConsecutivesMax;
import fr.project.planning.constraints.legales.ReposHebdomadaireGlissant;
import fr.project.planning.constraints.legales.ReposHebdomadaireMin;
import fr.project.planning.constraints.legales.ReposObligatoireApresNuits;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * SocleReglementaireBaselineTest — état de service du socle réglementaire (chantier S7).
 *
 * <h2>Ce que ce test mesure</h2>
 * <p>Six contraintes déclarées dans {@code ConstraintProviderImpl} ne se déclenchaient jamais
 * pour un client conforme au contrat. Elles lisent l'activité du créneau via
 * {@code getActivite()} — le champ <strong>déprécié</strong> — sans repli vers
 * {@code codeActiviteId}. Or SC-03 et SC-06 n'envoient que {@code codeActiviteId} : le
 * référentiel ne trouve rien, le filtre « compte dans la charge » est faux, aucun match n'est
 * produit.</p>
 *
 * <p>La suite de tests historique n'a rien vu parce qu'elle alimente {@code activite}, pas
 * {@code codeActiviteId}. C'est précisément ce que ce fichier corrige : chaque situation est
 * jouée <strong>deux fois</strong>, avec le champ historique puis avec le champ du contrat.</p>
 *
 * <h2>Comment le lire — trois blocs</h2>
 * <ul>
 *   <li>{@code ClientHistorique} — le créneau porte {@code activite}. Ces assertions décrivent
 *       la situation comme <em>réellement fautive</em> : sans elles, un {@code penalizesBy(0)}
 *       plus bas passerait pour la mauvaise raison.</li>
 *   <li>{@code ClientConformeAuContrat} — le créneau porte {@code codeActiviteId}, comme WinDev.
 *       Chaque assertion à 0 est un <strong>constat, pas un objectif</strong>, et nomme le lot
 *       qui la fera changer.</li>
 *   <li>{@code Reveillees} — contraintes remises en service. Elles réagissent aux
 *       <em>deux</em> champs et restent inactives tant qu'aucun seuil individuel n'est transmis.</li>
 * </ul>
 *
 * <p>Ce fichier est l'instrument de bascule des lots S7.1 à S7.6 : réveiller une contrainte
 * consiste à déplacer une assertion d'un bloc à l'autre. Un écart de score non voulu se lit
 * ici, sur une contrainte isolée, avant de se lire dans un scénario complet.</p>
 *
 * <p><strong>Lots traités</strong> : S7.1 — {@code DimanchesTravaillesMax}.</p>
 *
 * <h2>Deux causes d'extinction, pas une</h2>
 * <p>{@code ReposObligatoireApresNuits} et {@code ReposHebdomadaireGlissant} sont muettes même
 * pour le client historique : leurs seuils globaux valent 0 en production et leur garde interne
 * les désactive. Réparer le repli d'activité seul ne les réveillerait pas — et réveillerait en
 * revanche brutalement {@code NuitsConsecutivesMax}, dont le seuil vaut 0 sans aucune garde.</p>
 */
class SocleReglementaireBaselineTest {

    private static final LocalDate DIMANCHE = LocalDate.of(2026, 5, 10);
    private static final LocalDate LUNDI = DIMANCHE.plusDays(1);

    /** Valeur de Penalites.depassementMaxDimanchesTravailles dans le contexte neutre. */
    private static final int PENALITE_DIMANCHE = 5_000;

    /** Constante DUREE_MAX_LEGALE de DureeMaximaleLegaleParSalarie (minutes). */
    private static final int DUREE_MAX_LEGALE = 780;

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // =====================================================================
    // Témoin — le champ historique déclenche bien les contraintes
    // =====================================================================

    @Nested
    @DisplayName("Client historique (champ 'activite') — les situations sont bien fautives")
    class ClientHistorique {

        @Test
        void septJoursDAffilee_violentLeReposHebdomadaireMinimum() {
            verifier(ReposHebdomadaireMin::reposHebdomadaireMin)
                    .given(faits(semaineComplete(Champ.ACTIVITE)))
                    .penalizesBy(1);
        }

        @Test
        void cumulSuperieurAQuatorzeHeures_violeLaDureeMaximaleLegale() {
            // Le seuil est un plafond JOURNALIER (13 h) comparé à un cumul sur tout
            // l'horizon : deux journées ordinaires suffisent à le franchir.
            // Ce défaut de maille est traité au lot S7.6.
            List<Creneau> deuxJours = List.of(
                    journee("C-J1", LUNDI, Champ.ACTIVITE),
                    journee("C-J2", LUNDI.plusDays(1), Champ.ACTIVITE));

            verifier(DureeMaximaleLegaleParSalarie::dureeMaximaleLegaleParSalarie)
                    .given(faits(deuxJours))
                    .penalizesBy(2 * DUREE_JOURNEE - DUREE_MAX_LEGALE);
        }

        @Test
        void reposApresNuits_resteMuetteMemeAvecLeChampHistorique() {
            // Seconde cause d'extinction : joursReposMinimumApresNuits vaut 0 et la
            // garde interne (reposExige <= 0) neutralise la contrainte. Le repli
            // d'activité ne suffira pas à la réveiller — lot S7.3.
            List<Creneau> nuitsPuisReprise = new ArrayList<>(nuits(2, Champ.ACTIVITE));
            nuitsPuisReprise.add(journee("C-REPRISE", LUNDI.plusDays(2), Champ.ACTIVITE));

            verifier(ReposObligatoireApresNuits::reposObligatoireApresNuits)
                    .given(faits(nuitsPuisReprise))
                    .penalizesBy(0);
        }

        @Test
        void reposHebdoGlissant_resteMuetteMemeAvecLeChampHistorique() {
            // Idem : fenetreJours vaut 0, la garde interne neutralise — lot S7.4.
            verifier(ReposHebdomadaireGlissant::reposHebdoGlissant)
                    .given(faits(semaineComplete(Champ.ACTIVITE)))
                    .penalizesBy(0);
        }
    }

    // =====================================================================
    // Constat — le champ du contrat n'en déclenche aucune
    // =====================================================================

    @Nested
    @DisplayName("Client conforme au contrat (champ 'codeActiviteId') — contraintes encore muettes")
    class ClientConformeAuContrat {

        @Test
        void reposHebdomadaireMin_estDormante() {
            // Réveil prévu au lot S7.5.
            verifier(ReposHebdomadaireMin::reposHebdomadaireMin)
                    .given(faits(semaineComplete(Champ.CODE_ACTIVITE_ID)))
                    .penalizesBy(0);
        }

        @Test
        void dureeMaximaleLegale_estDormante() {
            // Réveil prévu au lot S7.6, après correction de la maille (jour, pas horizon).
            List<Creneau> deuxJours = List.of(
                    journee("C-J1", LUNDI, Champ.CODE_ACTIVITE_ID),
                    journee("C-J2", LUNDI.plusDays(1), Champ.CODE_ACTIVITE_ID));

            verifier(DureeMaximaleLegaleParSalarie::dureeMaximaleLegaleParSalarie)
                    .given(faits(deuxJours))
                    .penalizesBy(0);
        }

        @Test
        void reposObligatoireApresNuits_estDormante() {
            // Réveil prévu au lot S7.3 (repli d'activité + seuil individuel).
            List<Creneau> nuitsPuisReprise = new ArrayList<>(nuits(2, Champ.CODE_ACTIVITE_ID));
            nuitsPuisReprise.add(journee("C-REPRISE", LUNDI.plusDays(2), Champ.CODE_ACTIVITE_ID));

            verifier(ReposObligatoireApresNuits::reposObligatoireApresNuits)
                    .given(faits(nuitsPuisReprise))
                    .penalizesBy(0);
        }

        @Test
        void reposHebdomadaireGlissant_estDormante() {
            // Réveil prévu au lot S7.4 (repli d'activité + paire de seuils individuels).
            verifier(ReposHebdomadaireGlissant::reposHebdoGlissant)
                    .given(faits(semaineComplete(Champ.CODE_ACTIVITE_ID)))
                    .penalizesBy(0);
        }
    }

    // =====================================================================
    // Remises en service — une contrainte par lot
    // =====================================================================

    @Nested
    @DisplayName("Contraintes réveillées — insensibles au champ d'activité employé")
    class Reveillees {

        /** Deux dimanches travaillés pour un plafond de un : excédent de 1. */
        @Test
        void dimanchesTravaillesMax_reagitAuChampDuContrat() {
            verifier(DimanchesTravaillesMax::maxDimanchesTravailles)
                    .given(faits(deuxDimanches(Champ.CODE_ACTIVITE_ID), salarieAvecPlafondDimanches(1)))
                    .penalizesBy(PENALITE_DIMANCHE);
        }

        /** Le champ historique reste servi : les clients qui ne l'ont pas quitté restent couverts. */
        @Test
        void dimanchesTravaillesMax_reagitEncoreAuChampHistorique() {
            verifier(DimanchesTravaillesMax::maxDimanchesTravailles)
                    .given(faits(deuxDimanches(Champ.ACTIVITE), salarieAvecPlafondDimanches(1)))
                    .penalizesBy(PENALITE_DIMANCHE);
        }

        /** Plafond respecté : aucun match, donc aucune ligne à impact nul au scoreBreakdown. */
        @Test
        void dimanchesTravaillesMax_plafondRespecte_nePenalisePas() {
            verifier(DimanchesTravaillesMax::maxDimanchesTravailles)
                    .given(faits(deuxDimanches(Champ.CODE_ACTIVITE_ID), salarieAvecPlafondDimanches(2)))
                    .penalizesBy(0);
        }

        /**
         * Sans plafond transmis, la contrainte reste inactive — le moteur n'invente pas de limite.
         * C'est ce qui rend la remise en service sans effet sur les payloads existants.
         */
        @Test
        void dimanchesTravaillesMax_sansPlafondTransmis_resteInactive() {
            verifier(DimanchesTravaillesMax::maxDimanchesTravailles)
                    .given(faits(deuxDimanches(Champ.CODE_ACTIVITE_ID)))
                    .penalizesBy(0);
        }

        /** Trois nuits d'affilée pour un plafond de deux. */
        @Test
        void nuitsConsecutivesMax_reagitAuChampDuContrat() {
            verifier(NuitsConsecutivesMax::maxNuitsConsecutives)
                    .given(faits(nuits(3, Champ.CODE_ACTIVITE_ID), salarieAvecPlafondNuits(2)))
                    .penalizesBy(1);
        }

        @Test
        void nuitsConsecutivesMax_reagitEncoreAuChampHistorique() {
            verifier(NuitsConsecutivesMax::maxNuitsConsecutives)
                    .given(faits(nuits(3, Champ.ACTIVITE), salarieAvecPlafondNuits(2)))
                    .penalizesBy(1);
        }

        /**
         * Sans plafond, trois nuits d'affilée ne déclenchent rien. C'est le cas de tous les
         * payloads existants — et la raison pour laquelle ce lot ne déplace aucun score.
         */
        @Test
        void nuitsConsecutivesMax_sansPlafondTransmis_resteInactive() {
            verifier(NuitsConsecutivesMax::maxNuitsConsecutives)
                    .given(faits(nuits(3, Champ.CODE_ACTIVITE_ID)))
                    .penalizesBy(0);
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Quel champ d'activité le créneau porte — c'est toute la question de ce test. */
    private enum Champ { ACTIVITE, CODE_ACTIVITE_ID }

    private static final int DUREE_JOURNEE = 480;
    private static final String SALARIE_ID = "SAL-S70-DORMANT";

    private org.optaplanner.test.api.score.stream.SingleConstraintVerification<PlanningProblem> verifier(
            Function<ConstraintFactory, org.optaplanner.core.api.score.stream.Constraint> contrainte) {
        return constraintVerifier.verifyThat((provider, factory) -> contrainte.apply(factory));
    }

    /** Salarié sans aucun seuil individuel — l'état de tous les payloads existants. */
    private static Object[] faits(List<Creneau> creneaux) {
        return faits(creneaux, TestPlanningRequestFactory.buildSalarie(SALARIE_ID));
    }

    /** Salarié + référentiel + contexte + créneaux, dans l'ordre attendu par {@code given}. */
    private static Object[] faits(List<Creneau> creneaux, SalarieReel salarie) {
        creneaux.forEach(c -> c.setRessourceAffectee(salarie));

        PlanningContext contexte = TestPlanningContextFactory.contexteNeutre(DIMANCHE, DIMANCHE.plusDays(13));

        List<Object> faits = new ArrayList<>();
        faits.add(salarie);
        faits.add(referentielAvecCharge());
        faits.add(contexte);
        faits.addAll(creneaux);
        return faits.toArray();
    }

    /** Salarié dont seul le plafond de dimanches est renseigné (lot S7.1). */
    private static SalarieReel salarieAvecPlafondDimanches(int plafond) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(SALARIE_ID);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, null, null, null, null, null,
                null, null, plafond, null, null));
        return salarie;
    }

    /** Salarié dont seul le plafond de nuits consécutives est renseigné (lot S7.2). */
    private static SalarieReel salarieAvecPlafondNuits(int plafond) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(SALARIE_ID);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, null, null, null, null, null,
                plafond, null, null, null, null));
        return salarie;
    }

    /** Deux dimanches calendaires distincts, à une semaine d'écart. */
    private static List<Creneau> deuxDimanches(Champ champ) {
        return List.of(
                journee("C-DIM-1", DIMANCHE, champ),
                journee("C-DIM-2", DIMANCHE.plusDays(7), champ));
    }

    /** {@code nb} nuits calendaires consécutives à partir du lundi. */
    private static List<Creneau> nuits(int nb, Champ champ) {
        List<Creneau> creneaux = new ArrayList<>();
        for (int i = 0; i < nb; i++) {
            creneaux.add(creneau("C-NUIT-" + i, LUNDI.plusDays(i),
                    LocalTime.of(22, 0), LocalTime.of(6, 0), DUREE_JOURNEE,
                    TypePlageHoraire.NUIT, champ));
        }
        return creneaux;
    }

    /** Sept jours travaillés d'affilée : aucun jour off dans la fenêtre. */
    private static List<Creneau> semaineComplete(Champ champ) {
        List<Creneau> creneaux = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            creneaux.add(journee("C-SEM-" + i, LUNDI.plusDays(i), champ));
        }
        return creneaux;
    }

    private static Creneau journee(String id, LocalDate date, Champ champ) {
        return creneau(id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), DUREE_JOURNEE,
                TypePlageHoraire.JOUR, champ);
    }

    private static Creneau creneau(String id, LocalDate date, LocalTime debut, LocalTime fin,
                                   int duree, TypePlageHoraire plage, Champ champ) {
        String activite = champ == Champ.ACTIVITE ? TestPlanningRequestFactory.ACTIVITE_TRAVAIL : null;
        String codeActiviteId = champ == Champ.CODE_ACTIVITE_ID ? TestPlanningRequestFactory.ACTIVITE_TRAVAIL : null;

        return new Creneau(
                id, date, debut, fin, duree,
                TestRessourceFactory.SITE_CANON,
                codeActiviteId,
                activite,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, plage,
                false, QualificationJour.OUVRE);
    }

    private static ReferentielComptabiliteActivite referentielAvecCharge() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD)));
    }
}
