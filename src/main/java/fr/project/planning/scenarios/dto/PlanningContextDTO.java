package fr.project.planning.scenarios.dto;

public class PlanningContextDTO {

    private HorizonDTO horizon;
    private String strategieScoring;

    /**
     * [Lot S8.0] Cadre réglementaire : plage de nuit et calendrier des jours fériés.
     *
     * <p>Facultatif. Absent, le moteur applique la plage de nuit légale par défaut et déduit les
     * jours fériés de ce que le dataset déclare — comportement du lot S7.9a.</p>
     */
    private RegulatoryParametersDTO regulatoryParameters;

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

    public RegulatoryParametersDTO getRegulatoryParameters() {
        return regulatoryParameters;
    }

    public void setRegulatoryParameters(RegulatoryParametersDTO regulatoryParameters) {
        this.regulatoryParameters = regulatoryParameters;
    }
}