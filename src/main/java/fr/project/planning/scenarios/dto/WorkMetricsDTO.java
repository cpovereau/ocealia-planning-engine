package fr.project.planning.scenarios.dto;

import java.util.List;

public class WorkMetricsDTO {

    private List<WorkMetricsByRessourceDTO> byRessource;
    private GlobalWorkMetricsDTO global;

    public WorkMetricsDTO() {}

    public WorkMetricsDTO(List<WorkMetricsByRessourceDTO> byRessource, GlobalWorkMetricsDTO global) {
        this.byRessource = byRessource;
        this.global = global;
    }

    public List<WorkMetricsByRessourceDTO> getByRessource() {
        return byRessource;
    }

    public void setByRessource(List<WorkMetricsByRessourceDTO> byRessource) {
        this.byRessource = byRessource;
    }

    public GlobalWorkMetricsDTO getGlobal() {
        return global;
    }

    public void setGlobal(GlobalWorkMetricsDTO global) {
        this.global = global;
    }
}