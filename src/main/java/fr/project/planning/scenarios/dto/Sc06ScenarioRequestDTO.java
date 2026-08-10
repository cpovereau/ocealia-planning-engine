package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.project.planning.scenarios.dto.request.Sc06ScenarioParametersDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Sc06ScenarioRequestDTO — requête du scénario SC-06 (lot S4).
 *
 * <p>SC-06 : désigner, parmi les ressources dont le planning de la semaine est connu, celles
 * les plus à même de couvrir un besoin de 1 à n créneaux sur une même journée.</p>
 *
 * <p>Distinct de {@code Sc03ScenarioRequestDTO} : SC-03 réaffecte un sous-ensemble de créneaux,
 * SC-06 n'en réaffecte aucun. Le contrat SC-03 n'est pas modifié.</p>
 *
 * <p>Structure attendue :</p>
 * <ul>
 *   <li>{@code planningContext.horizon} — la <strong>semaine pleine</strong> lundi → dimanche
 *       contenant la date du besoin ;</li>
 *   <li>{@code scenarioParameters.besoin} — la question posée, seule variable de décision ;</li>
 *   <li>{@code dataSet.creneaux} — le planning existant, chaque créneau portant son
 *       {@code ressourceAffecteeId} : intégralement figé.</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sc06ScenarioRequestDTO {

    @NotBlank(message = "requestId est obligatoire")
    private String requestId;

    @NotNull(message = "metadata est obligatoire")
    @Valid
    private MetadataDTO metadata;

    private String scenarioType;

    private PlanningContextDTO planningContext;

    @NotNull(message = "scenarioParameters est obligatoire")
    @Valid
    private Sc06ScenarioParametersDTO scenarioParameters;

    private DataSetDTO dataSet;

    // =========================
    // Getters / Setters
    // =========================

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public MetadataDTO getMetadata() { return metadata; }
    public void setMetadata(MetadataDTO metadata) { this.metadata = metadata; }

    public String getScenarioType() { return scenarioType; }
    public void setScenarioType(String scenarioType) { this.scenarioType = scenarioType; }

    public PlanningContextDTO getPlanningContext() { return planningContext; }
    public void setPlanningContext(PlanningContextDTO planningContext) { this.planningContext = planningContext; }

    public Sc06ScenarioParametersDTO getScenarioParameters() { return scenarioParameters; }
    public void setScenarioParameters(Sc06ScenarioParametersDTO scenarioParameters) { this.scenarioParameters = scenarioParameters; }

    public DataSetDTO getDataSet() { return dataSet; }
    public void setDataSet(DataSetDTO dataSet) { this.dataSet = dataSet; }
}
