package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public class ScenarioResponseDTO {

    private String scenarioType;
    private SolverResultDTO solverResult;
    private ScenarioPlanningDTO planning;
    private WorkMetricsDTO workMetrics;
    private SolutionSummaryDTO solutionSummary;
    private DiagnosticsDTO diagnostics;

    /**
     * [Lot S4] Manières de couvrir le besoin, classées — <strong>propre à SC-06</strong>.
     *
     * <p>La clé est omise pour tous les autres scénarios : SC-01 et SC-03 ne produisent pas de
     * classement, et leur réponse ne doit pas gagner un champ vide qui laisserait croire à une
     * capacité inexistante.</p>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<CandidatDTO> candidats;

    /**
     * [Lot S1 de SC-02] Conséquences de l'absence — <strong>propre à SC-02</strong>.
     *
     * <p>Même règle que {@code candidats} : la clé est omise pour tous les autres scénarios.</p>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RemplacementDTO remplacement;

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

    public List<CandidatDTO> getCandidats() {
        return candidats;
    }

    public void setCandidats(List<CandidatDTO> candidats) {
        this.candidats = candidats;
    }

    public RemplacementDTO getRemplacement() {
        return remplacement;
    }

    public void setRemplacement(RemplacementDTO remplacement) {
        this.remplacement = remplacement;
    }
}