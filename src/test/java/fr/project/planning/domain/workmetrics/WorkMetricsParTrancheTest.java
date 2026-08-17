package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.ContratSalarie;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.workmetrics.TrancheTemporelle.Granularite;
import fr.project.planning.fixtures.TestPlanningContextFactory;
import fr.project.planning.fixtures.TestReferentielFactory;
import fr.project.planning.fixtures.TestRegulatoryParametersFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WorkMetricsParTrancheTest — lot O1 de SC-04.
 *
 * <h3>Le montage</h3>
 * <p>Deux semaines, du lundi 11 au dimanche 24 mai 2026. Un salarié à 35 h qui travaille
 * <strong>35 h la première semaine et 14 h la seconde</strong> — un déséquilibre qu'un chiffre
 * unique sur la période masque presque entièrement : sur quatorze jours il n'est qu'à −30 %, quand
 * la seconde semaine seule le montre à −60 %.</p>
 *
 * <p>C'est précisément ce que SC-04 apporte : <em>quand</em>, et pas seulement <em>combien</em>.</p>
 */
class WorkMetricsParTrancheTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final LocalDate DIMANCHE_S2 = LUNDI.plusDays(13);
    private static final String SALARIE = "SAL-A";

    @Test
    @DisplayName("Trois granularités : deux semaines, un mois, une période")
    void troisGranularites() {
        Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> parTranche = calculer(List.of());

        assertEquals(2, compte(parTranche, Granularite.SEMAINE));
        assertEquals(1, compte(parTranche, Granularite.MOIS));
        assertEquals(1, compte(parTranche, Granularite.PERIODE));
    }

    @Test
    @DisplayName("Le déséquilibre que la période masque, la semaine le montre")
    void leDesequilibreQueLaPeriodeMasque() {
        Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> parTranche = calculer(List.of());

        // Période : 49 h faites pour 70 h dues sur quatorze jours → −30 %.
        assertEquals(-30.0, ecart(parTranche, Granularite.PERIODE, 0), 0.01);

        // Semaine 1 : 35 h pour 35 h → à l'équilibre. Semaine 2 : 14 h pour 35 h → −60 %.
        assertEquals(0.0, ecart(parTranche, Granularite.SEMAINE, 0), 0.01);
        assertEquals(-60.0, ecart(parTranche, Granularite.SEMAINE, 1), 0.01,
                "C'est ce chiffre-là que SC-04 existe pour rendre visible.");
    }

    @Test
    @DisplayName("Les volumes des semaines redonnent celui de la période")
    void lesVolumesDesSemainesRedonnentCeluiDeLaPeriode() {
        Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> parTranche = calculer(List.of());

        double periode = metrique(parTranche, Granularite.PERIODE, 0).getMinutesTravaillees();
        double semaines = metrique(parTranche, Granularite.SEMAINE, 0).getMinutesTravaillees()
                + metrique(parTranche, Granularite.SEMAINE, 1).getMinutesTravaillees();

        assertEquals(periode, semaines, 0.01,
                "Un découpage qui perdrait ou dupliquerait des heures ne servirait à rien.");
    }

    @Test
    @DisplayName("Les absences se déduisent tranche par tranche, pas globalement")
    void lesAbsencesSeDeduisentTrancheParTranche() {
        // Absent toute la seconde semaine : il n'y devait rien, et n'y doit donc rien.
        Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> parTranche = calculer(List.of(
                new Indisponibilite(SALARIE, LUNDI.plusDays(7), DIMANCHE_S2, "CONGE")));

        assertEquals(7, metrique(parTranche, Granularite.SEMAINE, 0).getJoursObserves());
        assertEquals(0, metrique(parTranche, Granularite.SEMAINE, 1).getJoursObserves());
        assertEquals(7, metrique(parTranche, Granularite.PERIODE, 0).getJoursObserves(),
                "Sur la période : quatorze jours moins sept d'absence.");
    }

    @Test
    @DisplayName("Une semaine de congé complet ne se lit pas à −100 %, elle ne se compare à rien")
    void uneSemaineDeCongeCompletNeSeCompareARien() {
        // Congé sur toute la semaine 2, et donc aucun créneau cette semaine-là : le moteur ne
        // place jamais de travail dans un congé, et un jeu d'essai qui en placerait mesurerait
        // une situation que la contrainte HARD interdit.
        SalarieReel salarie = salarie();
        List<Creneau> semaine1 = new ArrayList<>();
        for (int jour = 0; jour < 5; jour++) {
            semaine1.add(travail("S1-" + jour, LUNDI.plusDays(jour), salarie));
        }

        Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> parTranche =
                new WorkMetricsParTranche().calculer(problem(salarie, semaine1, List.of(
                        new Indisponibilite(SALARIE, LUNDI.plusDays(7), DIMANCHE_S2, "CONGE"))));

        assertNull(metrique(parTranche, Granularite.SEMAINE, 1).getEcartContratPourcent(),
                "Sans un jour disponible, rien n'est comparable — et surtout pas un déficit.");

        // Sur la période : 35 h faites pour les 35 h dues sur la seule semaine où il était là.
        assertEquals(0.0, ecart(parTranche, Granularite.PERIODE, 0), 0.01,
                "Il a fait son contrat sur le temps où il était là. Sans la déduction des "
                        + "absences, la période le montrerait à −50 %.");
    }

    @Test
    @DisplayName("⚠️ Une série à cheval sur deux semaines n'est entière que sur la période")
    void uneSerieAChevalNEstEntiereQueSurLaPeriode() {
        // Le salarié travaille du jeudi de la semaine 1 au mercredi de la semaine 2 : sept jours
        // consécutifs, que le découpage hebdomadaire montre 4 puis 3. C'est vrai à l'intérieur de
        // chaque semaine, et trompeur si on l'oublie : seule la PERIODE porte la série réelle.
        List<Creneau> creneaux = new ArrayList<>();
        SalarieReel salarie = salarie();
        for (int jour = 3; jour <= 9; jour++) {
            creneaux.add(travail("C-" + jour, LUNDI.plusDays(jour), salarie));
        }

        Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> parTranche =
                new WorkMetricsParTranche().calculer(problem(salarie, creneaux, List.of()));

        assertEquals(4, metrique(parTranche, Granularite.SEMAINE, 0).getMaxJoursConsecutifsObservees());
        assertEquals(3, metrique(parTranche, Granularite.SEMAINE, 1).getMaxJoursConsecutifsObservees());
        assertEquals(7, metrique(parTranche, Granularite.PERIODE, 0).getMaxJoursConsecutifsObservees(),
                "La série réelle ne se lit que sur la période : 4 et 3 ne font pas 7 par hasard, "
                        + "ils font 7 parce que la coupe est arbitraire.");
    }

    @Test
    @DisplayName("Sans horizon exploitable, aucune tranche")
    void sansHorizonExploitableAucuneTranche() {
        assertTrue(new WorkMetricsParTranche().calculer(null).isEmpty());
    }

    // =====================================================================
    // Montage
    // =====================================================================

    private static Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> calculer(
            List<Indisponibilite> absences) {
        SalarieReel salarie = salarie();
        List<Creneau> creneaux = new ArrayList<>();
        // Semaine 1 : cinq jours de 7 h = 35 h. Semaine 2 : deux jours de 7 h = 14 h.
        for (int jour = 0; jour < 5; jour++) {
            creneaux.add(travail("S1-" + jour, LUNDI.plusDays(jour), salarie));
        }
        for (int jour = 7; jour < 9; jour++) {
            creneaux.add(travail("S2-" + jour, LUNDI.plusDays(jour), salarie));
        }
        return new WorkMetricsParTranche().calculer(problem(salarie, creneaux, absences));
    }

    private static PlanningProblem problem(SalarieReel salarie, List<Creneau> creneaux,
                                           List<Indisponibilite> absences) {
        PlanningContext contexte = TestPlanningContextFactory.contexteNeutre(LUNDI, DIMANCHE_S2);
        PlanningProblem problem = new PlanningProblem(
                contexte,
                TestRegulatoryParametersFactory.neutre(),
                TestReferentielFactory.referentielActiviteSansDetteRepos(),
                List.of(salarie),
                creneaux);
        problem.setIndisponibilites(absences);
        return problem;
    }

    private static SalarieReel salarie() {
        SalarieReel salarie = TestRessourceFactory.salarieStandard(SALARIE);
        salarie.setContrat(new ContratSalarie(null, 35.0, null, null));
        return salarie;
    }

    private static Creneau travail(String id, LocalDate date, SalarieReel salarie) {
        Creneau creneau = new Creneau(
                id, date, LocalTime.of(9, 0), LocalTime.of(16, 0), 420,
                TestRessourceFactory.SITE_CANON,
                TestReferentielFactory.ID_ACTIVITE_PLANNING_TRAVAIL,
                "ACTIVITE", TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE);
        creneau.setRessourceAffectee(salarie);
        return creneau;
    }

    private static long compte(Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> parTranche,
                               Granularite granularite) {
        return parTranche.keySet().stream().filter(t -> t.granularite() == granularite).count();
    }

    /** La n-ième tranche d'une granularité, dans l'ordre du découpage. */
    private static WorkMetrics metrique(Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> parTranche,
                                        Granularite granularite, int rang) {
        TrancheTemporelle tranche = parTranche.keySet().stream()
                .filter(t -> t.granularite() == granularite)
                .toList()
                .get(rang);

        return parTranche.get(tranche).entrySet().stream()
                .filter(e -> SALARIE.equals(e.getKey().getId()))
                .findFirst()
                .orElseThrow()
                .getValue();
    }

    private static double ecart(Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> parTranche,
                                Granularite granularite, int rang) {
        return metrique(parTranche, granularite, rang).getEcartContratPourcent();
    }
}
