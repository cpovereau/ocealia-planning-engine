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

    /**
     * [Équité L1] Ce que vaut une heure selon quand elle est travaillée.
     *
     * <p>Facultatif. Absent, aucune pondération n'est appliquée — la mesure pondérée vaut alors
     * les heures brutes — et le moteur le signale plutôt que d'inventer une échelle.</p>
     */
    private CoefficientsPenibiliteDTO coefficientsPenibilite;

    /**
     * [Équité L5] À partir de quel écart au contrat il y a inéquité.
     *
     * <p>Facultatif. Absent, la contrainte d'équité ne pèse rien : le moteur mesure l'écart et le
     * restitue, il ne le sanctionne pas. <strong>C'est l'encadrement qui dit à partir de quand un
     * écart gêne</strong>, comme le seuil de surcharge de SC-02 dit jusqu'où l'on accepte d'aller.</p>
     */
    private EquiteDTO equite;

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

    public CoefficientsPenibiliteDTO getCoefficientsPenibilite() {
        return coefficientsPenibilite;
    }

    public void setCoefficientsPenibilite(CoefficientsPenibiliteDTO coefficientsPenibilite) {
        this.coefficientsPenibilite = coefficientsPenibilite;
    }

    public EquiteDTO getEquite() {
        return equite;
    }

    public void setEquite(EquiteDTO equite) {
        this.equite = equite;
    }
}