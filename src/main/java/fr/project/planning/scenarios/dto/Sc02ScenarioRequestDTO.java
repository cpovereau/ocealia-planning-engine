package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.project.planning.scenarios.dto.request.Sc02ScenarioParametersDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Sc02ScenarioRequestDTO — requête du scénario SC-02, remplacement d'un salarié absent.
 *
 * <p>Même socle que SC-03 — {@code planningContext} et {@code dataSet} — d'où
 * {@link ScenarioDatasetRequest}, qui permet à la préparation d'être écrite une seule fois. Ce que
 * SC-02 ajoute tient en un bloc : {@code scenarioParameters}, typé fortement et distinct de celui
 * de SC-03.</p>
 *
 * <p>Le DTO racine reste tolérant aux champs inconnus, comme celui de SC-03 : WinDev peut y
 * ajouter des métadonnées de transport ({@code correlationId}, version…) sans casser l'appel. La
 * tolérance s'arrête là — les blocs internes, eux, sont stricts.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sc02ScenarioRequestDTO implements ScenarioDatasetRequest {

    @NotBlank(message = "requestId est obligatoire")
    private String requestId;

    @NotNull(message = "metadata est obligatoire")
    @Valid
    private MetadataDTO metadata;

    private String scenarioType;

    private PlanningContextDTO planningContext;

    @NotNull(message = "scenarioParameters est obligatoire pour SC-02")
    @Valid
    private Sc02ScenarioParametersDTO scenarioParameters;

    private DataSetDTO dataSet;

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

    @Override
    public String getScenarioType() {
        return scenarioType;
    }

    public void setScenarioType(String scenarioType) {
        this.scenarioType = scenarioType;
    }

    @Override
    public PlanningContextDTO getPlanningContext() {
        return planningContext;
    }

    public void setPlanningContext(PlanningContextDTO planningContext) {
        this.planningContext = planningContext;
    }

    public Sc02ScenarioParametersDTO getScenarioParameters() {
        return scenarioParameters;
    }

    public void setScenarioParameters(Sc02ScenarioParametersDTO scenarioParameters) {
        this.scenarioParameters = scenarioParameters;
    }

    @Override
    public DataSetDTO getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSetDTO dataSet) {
        this.dataSet = dataSet;
    }
}
