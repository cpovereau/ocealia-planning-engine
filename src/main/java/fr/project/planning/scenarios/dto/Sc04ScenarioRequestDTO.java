package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.project.planning.scenarios.dto.request.Sc04ScenarioParametersDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Sc04ScenarioRequestDTO — requête du scénario SC-04, optimisation globale d'un planning existant.
 *
 * <p>Même socle que SC-02, SC-03 et SC-05 — {@code planningContext} et {@code dataSet} — d'où
 * {@link ScenarioDatasetRequest}, qui permet à la préparation d'être écrite une seule fois. Ce que
 * SC-04 ajoute tient en un champ : {@code scenarioParameters.datePivot}.</p>
 *
 * <p><strong>Aucun champ nouveau hors {@code scenarioParameters}</strong>. C'est le résultat de
 * l'arbitrage §3 du cadrage : l'« historique des compteurs » que le contrat annonçait ne se
 * <em>reçoit</em> pas, il se <strong>recalcule</strong> depuis les créneaux transmis. La profondeur
 * est celle de la période demandée — l'appelant élargit {@code planningContext.horizon} à la
 * profondeur qu'il veut voir, et rien n'est conservé entre deux appels.</p>
 *
 * <p>⚠️ SC-04 exige le <strong>planning complet de la période</strong>, pour tout le monde. Ce
 * n'est pas une exigence de confort : sans lui les bornes hebdomadaires sont invérifiables, et
 * surtout la mesure jugerait chacun sur une fraction de son activité. <strong>Plus la fenêtre est
 * large, plus la mesure a de sens</strong> — c'est le levier même de ce scénario.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sc04ScenarioRequestDTO implements ScenarioDatasetRequest {

    @NotBlank(message = "requestId est obligatoire")
    private String requestId;

    @NotNull(message = "metadata est obligatoire")
    @Valid
    private MetadataDTO metadata;

    private String scenarioType;

    private PlanningContextDTO planningContext;

    @NotNull(message = "scenarioParameters est obligatoire pour SC-04")
    @Valid
    private Sc04ScenarioParametersDTO scenarioParameters;

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

    public Sc04ScenarioParametersDTO getScenarioParameters() {
        return scenarioParameters;
    }

    public void setScenarioParameters(Sc04ScenarioParametersDTO scenarioParameters) {
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
