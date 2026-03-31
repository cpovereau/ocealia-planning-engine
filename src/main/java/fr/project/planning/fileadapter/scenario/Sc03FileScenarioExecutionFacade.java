package fr.project.planning.fileadapter.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.scenarios.dto.Sc03ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.service.ScenarioSc03ExecutionService;
import org.springframework.stereotype.Service;

/**
 * Façade FileAdapter pour le scénario SC-03 (multi-ressources, dataset-driven).
 *
 * Désérialise le payload JSON en Sc03ScenarioRequestDTO et délègue à ScenarioSc03ExecutionService.
 * Aucune logique métier ici : ce service est un pont entre le transport fichier et le service SC-03.
 *
 * Point de branchement vers l'existant :
 * - ScenarioSc03ExecutionService.solve(Sc03ScenarioRequestDTO) → ScenarioResponseDTO
 *
 * Valeur "SC-03" confirmée par les fixtures de tests existantes
 * (sc03_migration_reference.json, ScenarioControllerSc03ValidationTest).
 */
@Service
public class Sc03FileScenarioExecutionFacade implements FileScenarioExecutionFacade {

    private final ScenarioSc03ExecutionService executionService;
    private final ObjectMapper objectMapper;

    public Sc03FileScenarioExecutionFacade(ScenarioSc03ExecutionService executionService,
                                            ObjectMapper objectMapper) {
        this.executionService = executionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String supportedScenarioType() {
        return "SC-03";
    }

    @Override
    public ScenarioResponseDTO execute(JsonNode payload) throws Exception {
        Sc03ScenarioRequestDTO request = objectMapper.treeToValue(payload, Sc03ScenarioRequestDTO.class);
        return executionService.solve(request);
    }
}
