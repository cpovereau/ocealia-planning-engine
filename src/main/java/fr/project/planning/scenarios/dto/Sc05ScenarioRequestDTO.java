package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.project.planning.scenarios.dto.request.Sc05ScenarioParametersDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Sc05ScenarioRequestDTO — requête du scénario SC-05, arbitrage de répartition entre deux salariés.
 *
 * <p>Même socle que SC-02 et SC-03 — {@code planningContext} et {@code dataSet} — d'où
 * {@link ScenarioDatasetRequest}, qui permet à la préparation d'être écrite une seule fois. Ce que
 * SC-05 ajoute tient en un bloc : {@code scenarioParameters}.</p>
 *
 * <p><strong>Aucun champ nouveau hors {@code scenarioParameters}</strong> : le planning existant,
 * les contrats, les indisponibilités, le cadre réglementaire, la tolérance d'équité et l'échelle de
 * pénibilité étaient déjà au contrat et déjà lus.</p>
 *
 * <p>⚠️ Comme SC-06, SC-05 exige que le <strong>planning complet de la période</strong> soit
 * transmis pour les deux salariés — sans quoi les bornes hebdomadaires sont invérifiables et le
 * moteur déclarerait conforme une répartition qui ne l'est pas.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sc05ScenarioRequestDTO implements ScenarioDatasetRequest {

    @NotBlank(message = "requestId est obligatoire")
    private String requestId;

    @NotNull(message = "metadata est obligatoire")
    @Valid
    private MetadataDTO metadata;

    private String scenarioType;

    private PlanningContextDTO planningContext;

    @NotNull(message = "scenarioParameters est obligatoire pour SC-05")
    @Valid
    private Sc05ScenarioParametersDTO scenarioParameters;

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

    public Sc05ScenarioParametersDTO getScenarioParameters() {
        return scenarioParameters;
    }

    public void setScenarioParameters(Sc05ScenarioParametersDTO scenarioParameters) {
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
