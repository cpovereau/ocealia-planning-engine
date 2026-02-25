package fr.project.planning.scenarios.dto;

public class PlanningContextDTO {

    private HorizonDTO horizon;
    private String strategieScoring;

    public HorizonDTO getHorizon() {
        return horizon;
    }

    public void setHorizon(HorizonDTO horizon) {
        this.horizon = horizon;
    }

    public String getStrategieScoring() {
        return strategieScoring;
    }

    public void setStrategieScoring(String strategieScoring) {
        this.strategieScoring = strategieScoring;
    }
}