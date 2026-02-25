package fr.project.planning.scenarios.dto;

import fr.project.planning.scenarios.dto.request.Sc01ScenarioParametersDTO;

public class ScenarioRequestDTO {

    private String scenarioType;

    private PlanningContextDTO planningContext;

    /**
     * Paramètres spécifiques au scénario.
     * Pour l'instant, seul SC-01 est supporté.
     */
    private Sc01ScenarioParametersDTO scenarioParameters;

    private DataSetDTO dataSet;

    // =========================
    // Getters / Setters
    // =========================

    public String getScenarioType() {
        return scenarioType;
    }

    public void setScenarioType(String scenarioType) {
        this.scenarioType = scenarioType;
    }

    public PlanningContextDTO getPlanningContext() {
        return planningContext;
    }

    public void setPlanningContext(PlanningContextDTO planningContext) {
        this.planningContext = planningContext;
    }

    public Sc01ScenarioParametersDTO getScenarioParameters() {
        return scenarioParameters;
    }

    public void setScenarioParameters(Sc01ScenarioParametersDTO scenarioParameters) {
        this.scenarioParameters = scenarioParameters;
    }

    public DataSetDTO getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSetDTO dataSet) {
        this.dataSet = dataSet;
    }
}