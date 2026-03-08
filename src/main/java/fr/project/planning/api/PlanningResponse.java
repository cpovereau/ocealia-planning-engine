package fr.project.planning.api;

import fr.project.planning.solution.PlanningProblem;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;
import fr.project.planning.scenarios.dto.ScoreBreakdownItemDTO;

import java.util.List;
import java.util.Objects;

/**
 * Contrat interne (service) : résultat d'une résolution de planning.
 * Ne pas confondre avec ScenarioResponseDTO (contrat HTTP).
 */
public final class PlanningResponse {

    private final PlanningProblem solution;
    private final List<ScenarioAlertDTO> alerts;
    private final List<ScoreBreakdownItemDTO> scoreBreakdown;

    public PlanningResponse(
            PlanningProblem solution,
            List<ScenarioAlertDTO> alerts,
            List<ScoreBreakdownItemDTO> scoreBreakdown
    ) {
        this.solution = Objects.requireNonNull(solution, "solution");
        this.alerts = List.copyOf(Objects.requireNonNull(alerts, "alerts"));
        this.scoreBreakdown = List.copyOf(Objects.requireNonNull(scoreBreakdown, "scoreBreakdown"));
    }

    public PlanningProblem solution() {
        return solution;
    }

    public List<ScenarioAlertDTO> alerts() {
        return alerts;
    }

    public List<ScoreBreakdownItemDTO> scoreBreakdown() {
        return scoreBreakdown;
    }
}