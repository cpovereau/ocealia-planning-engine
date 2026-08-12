package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.project.planning.scenarios.dto.request.Sc03ScenarioParametersDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Sc03ScenarioRequestDTO
 *
 * DTO de requête pour le scénario SC-03 (couverture locale multi-ressources).
 *
 * Distinct de ScenarioRequestDTO (SC-01) pour ne pas modifier le contrat SC-01
 * et permettre un typage fort des paramètres SC-03.
 *
 * Phase 7 : désérialisation complète + exécution solveur.
 * Phase 4.1 : requestId + metadata obligatoires.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sc03ScenarioRequestDTO implements ScenarioDatasetRequest {

    @NotBlank(message = "requestId est obligatoire")
    private String requestId;

    @NotNull(message = "metadata est obligatoire")
    @Valid
    private MetadataDTO metadata;

    private String scenarioType;

    private PlanningContextDTO planningContext;

    /**
     * Paramètres spécifiques SC-03 : prioriteCouverture, periode.
     * Optionnel — SC-03 peut fonctionner avec les seules données du dataSet.
     */
    private Sc03ScenarioParametersDTO scenarioParameters;

    private DataSetDTO dataSet;

    // =========================
    // Getters / Setters
    // =========================

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public MetadataDTO getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataDTO metadata) {
        this.metadata = metadata;
    }

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

    public Sc03ScenarioParametersDTO getScenarioParameters() {
        return scenarioParameters;
    }

    public void setScenarioParameters(Sc03ScenarioParametersDTO scenarioParameters) {
        this.scenarioParameters = scenarioParameters;
    }

    public DataSetDTO getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSetDTO dataSet) {
        this.dataSet = dataSet;
    }
}
