package fr.project.planning.api;

import fr.project.planning.solution.PlanningProblem;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;

import java.util.List;
import java.util.Objects;

/**
 * Contrat interne (service) : résultat d'une résolution de planning.
 * Ne pas confondre avec ScenarioResponseDTO (contrat HTTP).
 */
public final class PlanningResponse {

    private final PlanningProblem solution;
    private final List<ScenarioAlertDTO> alerts;

    public PlanningResponse(PlanningProblem solution, List<ScenarioAlertDTO> alerts) {
        this.solution = Objects.requireNonNull(solution, "solution");
        this.alerts = List.copyOf(Objects.requireNonNull(alerts, "alerts"));
    }

    public PlanningProblem solution() {
        return solution;
    }

    public List<ScenarioAlertDTO> alerts() {
        return alerts;
    }
}