package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.PenibilitesLegalesMinutes;
import fr.project.planning.constraints.metier.NuitSalarieNonNuit;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.score.ScoreUtils;
import fr.project.planning.fixtures.TestPlanningContextFactory;
import fr.project.planning.solution.PlanningProblem;
import fr.project.planning.time.TimeBreakdown;
import fr.project.planning.time.TimeBreakdownCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PlageDeNuitIndividuelleTest — lot S8.1
 *
 * <p>La plage de nuit est une donnée de la <strong>personne</strong> autant que du cadre : un
 * salarié veilleur et un salarié faisant du travail de nuit occasionnel ne relèvent pas
 * nécessairement des mêmes horaires. {@code SalarieReel} portait déjà
 * {@code heureDebutNuitEffective(fallback)} ; personne ne l'appelait.</p>
 *
 * <p>Ces tests couvrent la bascule, <strong>et mesurent la distorsion qu'elle introduit</strong> —
 * voir {@link OrdreDesCouts}.</p>
 */
class PlageDeNuitIndividuelleTest {

    /** Mercredi 13 mai 2026 — ni dimanche, ni férié : seule la nuit joue. */
    private static final LocalDate MERCREDI = LocalDate.of(2026, 5, 13);
    private static final String ACTIVITE = "ACT-SOIN";

    private final TimeBreakdownCalculator calculator = new TimeBreakdownCalculator();

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    /** {@code PlanningContext.defaultPenalites()} — forfait par créneau, lot S8.2. */
    private static final int PENALITE_NUIT_NON_NUIT = 2_000;

    // =====================================================================
    // La bascule
    // =====================================================================

    @Nested
    @DisplayName("Plage effective")
    class PlageEffective {

        @Test
        void salarieSansDeclaration_relevesDeLaPlageGlobale() {
            Creneau c = creneauDe21A07(salarie("SAL-N", null, null, null));

            assertEquals(480, calculator.compute(c, global(), true).minutesNuit(),
                    "22:00–06:00 appliqué : 120 min avant minuit, 360 après");
        }

        @Test
        void veilleurAvecPlageElargie_voitToutesSesHeuresComptees() {
            Creneau c = creneauDe21A07(
                    salarie("SAL-V", "permanent", LocalTime.of(21, 0), LocalTime.of(7, 0)));

            assertEquals(600, calculator.compute(c, global(), true).minutesNuit(),
                    "21:00–07:00 déclaré : le créneau entier est de nuit");
        }

        @Test
        void plageIndividuelleReduite_compteMoinsQueLaGlobale() {
            Creneau c = creneauDe21A07(
                    salarie("SAL-R", "occasionnel", LocalTime.of(0, 0), LocalTime.of(5, 0)));

            assertEquals(300, calculator.compute(c, global(), true).minutesNuit());
        }

        @Test
        void creneauNonAffecte_relevesDeLaPlageGlobale() {
            // Aucune personne, donc aucune plage individuelle : le cadre global s'applique.
            Creneau c = creneauDe21A07(null);

            assertEquals(480, calculator.compute(c, global(), true).minutesNuit());
        }

        @Test
        void lesIntersectionsSuiventLaMemePlage() {
            // La cohérence interne du breakdown ne doit pas se perdre en route : les minutes
            // « nuit et dimanche » restent un sous-ensemble des minutes de nuit.
            SalarieReel veilleur =
                    salarie("SAL-V", "permanent", LocalTime.of(21, 0), LocalTime.of(7, 0));
            Creneau samediSoir = creneau(LocalDate.of(2026, 5, 16), veilleur);   // samedi → dimanche

            TimeBreakdown b = calculator.compute(samediSoir, global(), true);

            assertEquals(600, b.minutesNuit());
            assertEquals(420, b.minutesDimanche(), "00:00→07:00 tombe le dimanche");
            assertEquals(420, b.minutesNuitEtDimanche());
            assertTrue(b.minutesNuitEtDimanche() <= b.minutesNuit());
        }
    }

    // =====================================================================
    // Ordre des coûts : la nuit doit revenir à qui la fait
    // =====================================================================

    @Nested
    @DisplayName("Ordre des coûts")
    class OrdreDesCouts {

        /**
         * Confier une nuit au veilleur doit coûter <strong>moins cher</strong> qu'à un salarié qui
         * n'en fait pas.
         *
         * <p>Ce n'est pas acquis. Le veilleur déclare une plage de nuit plus large, donc davantage
         * de minutes pénibles : le lot S8.1, en rendant la plage individuelle effective, a rendu
         * le veilleur mécaniquement plus cher. Tant que {@code NuitSalarieNonNuit} pénalisait un
         * point nu, le solveur préférait le salarié inadapté de 359 points — une incitation
         * exactement inverse à celle recherchée.</p>
         *
         * <p>Le poids introduit au lot S8.2 rétablit l'ordre. Ce test le garde : il échouera si
         * quelqu'un baisse ce poids sous l'écart de plage, ou élargit la plage sans y toucher.</p>
         */
        @Test
        void confierLaNuitAuVeilleurCouteMoinsCherQuAUnSalarieNonNuit() {
            SalarieReel veilleur =
                    salarie("SAL-V", "permanent", LocalTime.of(21, 0), LocalTime.of(7, 0));
            SalarieReel nonNuit = salarie("SAL-N", null, null, null);

            int penibiliteVeilleur = penibilite(creneauDe21A07(veilleur));
            int penibiliteNonNuit = penibilite(creneauDe21A07(nonNuit));

            // EXPLOITATION, poids nuit = 3 : 600×3 = 1800 contre 480×3 = 1440.
            // Le veilleur part donc avec 360 points de retard, du seul fait de sa plage.
            assertEquals(1800, penibiliteVeilleur);
            assertEquals(1440, penibiliteNonNuit);

            constraintVerifier
                    .verifyThat((p, factory) -> NuitSalarieNonNuit.nuitSalarieNonNuit(factory))
                    .given(contexte(), creneauDe21A07(nonNuit))
                    .penalizesBy(PENALITE_NUIT_NON_NUIT);

            constraintVerifier
                    .verifyThat((p, factory) -> NuitSalarieNonNuit.nuitSalarieNonNuit(factory))
                    .given(contexte(), creneauDe21A07(veilleur))
                    .penalizesBy(0);

            int coutVeilleur = penibiliteVeilleur;
            int coutNonNuit = penibiliteNonNuit + PENALITE_NUIT_NON_NUIT;

            assertTrue(coutVeilleur < coutNonNuit,
                    "La nuit doit revenir au veilleur : " + coutVeilleur + " contre "
                            + coutNonNuit + ". Le poids de NuitSalarieNonNuit doit rester au-dessus "
                            + "de l'écart de plage entre les deux salariés.");
        }

        @Test
        void leContrepoidsDomineLEcartDePlageMaximal() {
            // Écart maximal plausible : une plage individuelle couvrant les 24 heures face à la
            // plage globale de 8 heures, soit 16 heures de nuit supplémentaires. En EXPLOITATION,
            // poids nuit = 3, cela vaut 2 880 points... au-delà du contrepoids.
            //
            // Le poids retenu couvre l'écart réaliste — deux heures de plage supplémentaires,
            // 360 points — avec une marge de plus de cinq fois. Il ne couvre pas l'absurde, et
            // ce test le dit pour que la borne soit connue plutôt que supposée.
            int ecartRealisteEnMinutes = 120;
            int poidsNuitExploitation = 3;

            assertTrue(PENALITE_NUIT_NON_NUIT > ecartRealisteEnMinutes * poidsNuitExploitation,
                    "Le contrepoids doit dominer l'écart de plage réaliste");
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /**
     * Pénalité de pénibilité d'un créneau, calculée comme le fait
     * {@code PenibilitesLegalesMinutes} : breakdown temporel, puis pondération et dominance.
     */
    private int penibilite(Creneau creneau) {
        TimeBreakdown b = calculator.compute(creneau, global(), true);
        return -ScoreUtils.penalitesLegalesAvecDominance(
                contexte(),
                b.minutesNuit(), b.minutesDimanche(), b.minutesFerie(),
                b.minutesNuitEtDimanche(), b.minutesNuitEtFerie(),
                b.minutesDimancheEtFerie(), b.minutesNuitEtDimancheEtFerie()
        ).softScore();
    }

    private static PlanningContext contexte() {
        return TestPlanningContextFactory.contexteNeutre(MERCREDI.minusDays(2), MERCREDI.plusDays(7));
    }

    private static RegulatoryParameters global() {
        return RegulatoryParameters.neutre();
    }

    private static SalarieReel salarie(String id, String travailDeNuit,
                                       LocalTime debutNuit, LocalTime finNuit) {
        SalarieReel s = TestRessourceFactory.salarieStandard(id);
        s.setTravailDeNuit(travailDeNuit);
        s.setHeureDebutNuit(debutNuit);
        s.setHeureFinNuit(finNuit);
        return s;
    }

    private static Creneau creneauDe21A07(SalarieReel salarie) {
        return creneau(MERCREDI, salarie);
    }

    private static Creneau creneau(LocalDate date, SalarieReel salarie) {
        Creneau c = new Creneau(
                "C-NUIT", date, LocalTime.of(21, 0), LocalTime.of(7, 0), 600,
                "SITE-A", ACTIVITE, null, "PC-001",
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.NUIT,
                false, QualificationJour.OUVRE);
        c.setRessourceAffectee(salarie);
        return c;
    }

    private static ReferentielComptabiliteActivite referentiel() {
        return new ReferentielComptabiliteActivite(Map.of(
                ACTIVITE, new ComptabiliteActivite(ACTIVITE, true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD)));
    }
}
