package fr.project.planning.scenarios.dto;

import java.util.List;

public class ScenarioResponseDTO {

    private String status;               // ex: "SOLVED", "ERROR"
    private ScenarioPlanningDTO planning; // bloc planning SC-01

    private List<ScenarioAlertDTO> alerts;

    public ScenarioResponseDTO() {}

    public ScenarioResponseDTO(String status, ScenarioPlanningDTO planning, List<ScenarioAlertDTO> alerts) {
        this.status = status;
        this.planning = planning;
        this.alerts = alerts;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public ScenarioPlanningDTO getPlanning() { return planning; }
    public void setPlanning(ScenarioPlanningDTO planning) { this.planning = planning; }

    public List<ScenarioAlertDTO> getAlerts() { return alerts; }
    public void setAlerts(List<ScenarioAlertDTO> alerts) { this.alerts = alerts; }
}