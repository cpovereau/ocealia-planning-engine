package fr.project.planning.api;

import fr.project.planning.solution.PlanningProblem;
import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.Objects;

/**
 * Contrat interne (service) : résultat brut d'une résolution de planning.
 *
 * Ne contient que les données produites par le solveur OptaPlanner.
 * Ne dépend d'aucun DTO de restitution.
 *
 * La transformation en ScenarioResponseDTO appartient à la couche scénarios.
 */
public record PlanningResponse(
        PlanningProblem solution,
        ScoreExplanation<PlanningProblem, HardSoftScore> explanation
) {
    public PlanningResponse {
        Objects.requireNonNull(solution, "solution");
        Objects.requireNonNull(explanation, "explanation");
    }
}
