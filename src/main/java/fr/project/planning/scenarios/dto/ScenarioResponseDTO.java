package fr.project.planning.scenarios.dto;

public class ScenarioResponseDTO {

    private String scenarioType;
    private SolverResultDTO solverResult;
    private ScenarioPlanningDTO planning;
    private WorkMetricsDTO workMetrics;
    private SolutionSummaryDTO solutionSummary;
    private DiagnosticsDTO diagnostics;

    public ScenarioResponseDTO() {
    }

    public ScenarioResponseDTO(
            String scenarioType,
            SolverResultDTO solverResult,
            ScenarioPlanningDTO planning,
            WorkMetricsDTO workMetrics,
            SolutionSummaryDTO solutionSummary,
            DiagnosticsDTO diagnostics
    ) {
        this.scenarioType = scenarioType;
        this.solverResult = solverResult;
        this.planning = planning;
        this.workMetrics = workMetrics;
        this.solutionSummary = solutionSummary;
        this.diagnostics = diagnostics;
    }

    public String getScenarioType() {
        return scenarioType;
    }

    public void setScenarioType(String scenarioType) {
        this.scenarioType = scenarioType;
    }

    public SolverResultDTO getSolverResult() {
        return solverResult;
    }

    public void setSolverResult(SolverResultDTO solverResult) {
        this.solverResult = solverResult;
    }

    public ScenarioPlanningDTO getPlanning() {
        return planning;
    }

    public void setPlanning(ScenarioPlanningDTO planning) {
        this.planning = planning;
    }

    public WorkMetricsDTO getWorkMetrics() {
        return workMetrics;
    }

    public void setWorkMetrics(WorkMetricsDTO workMetrics) {
        this.workMetrics = workMetrics;
    }

    public SolutionSummaryDTO getSolutionSummary() {
        return solutionSummary;
    }

    public void setSolutionSummary(SolutionSummaryDTO solutionSummary) {
        this.solutionSummary = solutionSummary;
    }

    public DiagnosticsDTO getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(DiagnosticsDTO diagnostics) {
        this.diagnostics = diagnostics;
    }
}