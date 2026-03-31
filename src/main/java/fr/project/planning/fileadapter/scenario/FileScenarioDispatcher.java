package fr.project.planning.fileadapter.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Route un payload fichier vers la façade de scénario appropriée,
 * en lisant le champ "scenarioType" du JSON.
 *
 * Les façades disponibles sont injectées automatiquement par Spring
 * (toutes les implémentations de FileScenarioExecutionFacade).
 */
@Service
public class FileScenarioDispatcher {

    private final Map<String, FileScenarioExecutionFacade> facadesByType;

    public FileScenarioDispatcher(List<FileScenarioExecutionFacade> facades) {
        this.facadesByType = facades.stream()
                .collect(Collectors.toMap(
                        FileScenarioExecutionFacade::supportedScenarioType,
                        Function.identity()
                ));
    }

    /**
     * Dispatche le payload vers la façade correspondant au scenarioType.
     *
     * @param payload payload JSON complet (lu depuis le fichier de la requête)
     * @return réponse du solveur
     * @throws IllegalArgumentException si scenarioType est absent ou non supporté
     * @throws Exception si l'exécution du scénario échoue
     */
    public ScenarioResponseDTO dispatch(JsonNode payload) throws Exception {
        String scenarioType = extractScenarioType(payload);

        FileScenarioExecutionFacade facade = facadesByType.get(scenarioType);
        if (facade == null) {
            throw new IllegalArgumentException(
                    "Type de scénario non supporté par le FileAdapter : '" + scenarioType + "'. "
                    + "Types disponibles : " + facadesByType.keySet()
            );
        }

        return facade.execute(payload);
    }

    private String extractScenarioType(JsonNode payload) {
        JsonNode typeNode = payload.get("scenarioType");
        if (typeNode == null || typeNode.isNull() || typeNode.asText().isBlank()) {
            throw new IllegalArgumentException(
                    "Le champ 'scenarioType' est absent ou vide dans le fichier JSON."
            );
        }
        return typeNode.asText();
    }
}
