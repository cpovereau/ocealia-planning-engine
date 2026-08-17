package fr.project.planning.solution;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.workmetrics.JoursDisponiblesSalarie;
import fr.project.planning.fixtures.TestPlanningContextFactory;
import fr.project.planning.fixtures.TestReferentielFactory;
import fr.project.planning.fixtures.TestRegulatoryParametersFactory;
import fr.project.planning.fixtures.TestRessourceFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PlanningProblemJoursDisponiblesTest — lot O0 de SC-04, second volet du rang 14.
 *
 * <h3>Ce que ces tests gardent</h3>
 * <p>{@code JoursDisponiblesSalarie} est <strong>dérivé</strong> et non fourni, et c'est tout le
 * sujet : {@code EquiteChargeAuContrat} le joint, une jointure est <em>interne</em>, et un salarié
 * sans fait correspondant sortirait de la contrainte — l'équité serait silencieusement désactivée
 * pour lui, ce qui est pire que le défaut corrigé.</p>
 *
 * <p>Trois propriétés en découlent et ne doivent pas se perdre : <strong>un fait par salarié
 * réel, toujours</strong> ; <strong>un seul</strong>, sinon la pénalité se multiplierait ;
 * <strong>la même identité d'un appel à l'autre</strong>, parce qu'un fait de problème qui change
 * d'objet entre deux lectures corrompt le score.</p>
 */
class PlanningProblemJoursDisponiblesTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final LocalDate DIMANCHE = LUNDI.plusDays(6);

    @Test
    @DisplayName("Un fait par salarié réel, même sans aucune absence déclarée")
    void unFaitParSalarieReelMemeSansAbsence() {
        PlanningProblem problem = problem(List.of());

        Map<String, Long> parId = parId(problem);

        assertEquals(2, parId.size(), "Deux salariés réels, deux faits — pas un de moins.");
        assertEquals(7L, parId.get("SAL-A"));
        assertEquals(7L, parId.get("SAL-B"));
    }

    @Test
    @DisplayName("Le poste virtuel n'en reçoit pas : il ne porte pas de contrat")
    void lePosteVirtuelNEnRecoitPas() {
        PlanningProblem problem = problem(List.of());

        assertTrue(problem.getJoursDisponiblesSalaries().stream()
                        .noneMatch(j -> "PV".equals(j.getSalarieId())),
                "Rien n'est comparable pour un poste virtuel : lui donner un dénominateur "
                        + "laisserait croire qu'il en a un.");
    }

    @Test
    @DisplayName("Trois absences sur le même salarié ne donnent toujours qu'un fait")
    void plusieursAbsencesNeDonnentQuUnFait() {
        // C'est la raison d'être de ce fait : joindre Indisponibilite directement multiplierait
        // le tuple — donc la pénalité d'équité — par le nombre d'absences de la personne.
        PlanningProblem problem = problem(List.of(
                new Indisponibilite("SAL-A", LUNDI, LUNDI, "CONGE"),
                new Indisponibilite("SAL-A", LUNDI.plusDays(1), LUNDI.plusDays(1), "CONGE"),
                new Indisponibilite("SAL-A", LUNDI.plusDays(2), LUNDI.plusDays(2), "MALADIE")));

        List<JoursDisponiblesSalarie> pourA = problem.getJoursDisponiblesSalaries().stream()
                .filter(j -> "SAL-A".equals(j.getSalarieId()))
                .toList();

        assertEquals(1, pourA.size(), "Un tuple, et un seul, quel que soit le nombre d'absences.");
        assertEquals(4L, pourA.get(0).getJours(), "Sept jours moins trois absents.");
    }

    @Test
    @DisplayName("Le fait garde la même identité d'un appel à l'autre")
    void leFaitGardeLaMemeIdentite() {
        // Un fait de problème qui change d'objet entre deux lectures corrompt le score.
        PlanningProblem problem = problem(List.of());

        assertSame(problem.getJoursDisponiblesSalaries(), problem.getJoursDisponiblesSalaries());
    }

    @Test
    @DisplayName("Déclarer une absence après coup recalcule le compte")
    void declarerUneAbsenceApresCoupRecalculeLeCompte() {
        // Les services de préparation posent les indisponibilités après la construction : un
        // compte mémorisé trop tôt resterait faux jusqu'à la résolution.
        PlanningProblem problem = problem(List.of());
        assertEquals(7L, parId(problem).get("SAL-A"));

        problem.setIndisponibilites(List.of(
                new Indisponibilite("SAL-A", LUNDI, LUNDI.plusDays(2), "CONGE")));

        assertEquals(4L, parId(problem).get("SAL-A"),
                "Le compte suit les absences : sinon il resterait celui d'avant leur déclaration.");
    }

    // =====================================================================
    // Montage
    // =====================================================================

    private static Map<String, Long> parId(PlanningProblem problem) {
        return problem.getJoursDisponiblesSalaries().stream()
                .collect(Collectors.toMap(JoursDisponiblesSalarie::getSalarieId,
                        JoursDisponiblesSalarie::getJours));
    }

    private static PlanningProblem problem(List<Indisponibilite> indisponibilites) {
        PlanningContext contexte = TestPlanningContextFactory.contexteNeutre(LUNDI, DIMANCHE);

        List<Ressource> ressources = List.of(
                TestRessourceFactory.salarieStandard("SAL-A"),
                TestRessourceFactory.salarieStandard("SAL-B"),
                TestRessourceFactory.posteVirtuelStandard("PV"));

        PlanningProblem problem = new PlanningProblem(
                contexte,
                TestRegulatoryParametersFactory.neutre(),
                TestReferentielFactory.referentielActiviteSansDetteRepos(),
                ressources,
                List.of());
        problem.setIndisponibilites(indisponibilites);
        return problem;
    }
}
