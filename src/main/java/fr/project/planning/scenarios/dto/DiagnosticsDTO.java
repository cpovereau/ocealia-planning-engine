package fr.project.planning.scenarios.dto;

import java.util.List;

public class DiagnosticsDTO {

    private List<ScenarioAlertDTO> alerts;
    private IgnoredCreneauxDTO ignoredCreneaux;

    public DiagnosticsDTO() {}

    public DiagnosticsDTO(List<ScenarioAlertDTO> alerts, IgnoredCreneauxDTO ignoredCreneaux) {
        this.alerts = alerts;
        this.ignoredCreneaux = ignoredCreneaux;
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
}