package fr.project.planning.scenarios.dto;

import java.util.List;

public class DiagnosticsDTO {

    private List<ScenarioAlertDTO> alerts;
    private List<AssignmentDiagnosticDTO> assignmentDiagnostics;
    private IgnoredCreneauxDTO ignoredCreneaux;

    public DiagnosticsDTO() {}

    public DiagnosticsDTO(
        List<ScenarioAlertDTO> alerts,
        IgnoredCreneauxDTO ignoredCreneaux,
        List<AssignmentDiagnosticDTO> assignmentDiagnostics
    ) {
        this.alerts = alerts;
        this.ignoredCreneaux = ignoredCreneaux;
        this.assignmentDiagnostics = assignmentDiagnostics;
    }

    public List<ScenarioAlertDTO> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<ScenarioAlertDTO> alerts) {
        this.alerts = alerts;
    }

    public IgnoredCreneauxDTO getIgnoredCreneaux() {
        return ignoredCreneaux;
    }

    public void setIgnoredCreneaux(IgnoredCreneauxDTO ignoredCreneaux) {
        this.ignoredCreneaux = ignoredCreneaux;
    }

    public List<AssignmentDiagnosticDTO> getAssignmentDiagnostics() {
        return assignmentDiagnostics;
    }

    public void setAssignmentDiagnostics(List<AssignmentDiagnosticDTO> assignmentDiagnostics) {
        this.assignmentDiagnostics = assignmentDiagnostics;
    }
}