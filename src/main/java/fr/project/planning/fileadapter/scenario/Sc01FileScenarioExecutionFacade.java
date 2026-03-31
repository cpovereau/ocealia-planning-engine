package fr.project.planning.fileadapter.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.scenarios.dto.ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.service.ScenarioSc01ExecutionService;
import org.springframework.stereotype.Service;

/**
 * Façade FileAdapter pour le scénario SC-01 (mono-ressource, génération paramétrique de créneaux).
 *
 * Désérialise le payload JSON en ScenarioRequestDTO et délègue à ScenarioSc01ExecutionService.
 * Aucune logique métier ici : ce service est un pont entre le transport fichier et le service SC-01.
 *
 * Point de branchement vers l'existant :
 * - ScenarioSc01ExecutionService.solve(ScenarioRequestDTO) → ScenarioResponseDTO
 *
 * Valeur "SC-01" confirmée par les fixtures de tests existantes
 * (sc01_dataset_reference.json, ScenarioControllerValidationTest).
 */
@Service
public class Sc01FileScenarioExecutionFacade implements FileScenarioExecutionFacade {

    private final ScenarioSc01ExecutionService executionService;
    private final ObjectMapper objectMapper;

    public Sc01FileScenarioExecutionFacade(ScenarioSc01ExecutionService executionService,
                                            ObjectMapper objectMapper) {
        this.executionService = executionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String supportedScenarioType() {
        return "SC-01";
    }

    @Override
    public ScenarioResponseDTO execute(JsonNode payload) throws Exception {
        ScenarioRequestDTO request = objectMapper.treeToValue(payload, ScenarioRequestDTO.class);
        return executionService.solve(request);
    }
}
