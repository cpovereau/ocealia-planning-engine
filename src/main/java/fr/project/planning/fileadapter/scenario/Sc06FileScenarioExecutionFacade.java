package fr.project.planning.fileadapter.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.scenarios.dto.Sc06ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.service.ScenarioSc06ExecutionService;
import org.springframework.stereotype.Service;

/**
 * Façade FileAdapter pour le scénario SC-06 (désignation de candidats).
 *
 * <p>Désérialise le payload en {@link Sc06ScenarioRequestDTO} et délègue à
 * {@link ScenarioSc06ExecutionService}. Aucune logique métier : un pont entre le transport
 * fichier et le service SC-06, sur le modèle exact de la façade SC-03.</p>
 *
 * <p>Le FileAdapter est le canal de test initial retenu côté WinDev (§4.10 du cadrage SC-06) :
 * il devait donc être livré avec le scénario, non différé.</p>
 *
 * <p><strong>Une différence de comportement à connaître</strong> : la validation déclarative
 * ({@code @NotBlank}, {@code @NotNull}) est portée par {@code @Valid} au niveau du contrôleur
 * HTTP. Par cette voie fichier, elle ne s'applique pas — seuls jouent les garde-fous du service
 * de préparation, qui couvrent les incohérences réellement dangereuses. Un champ obligatoire
 * absent produira donc ici une erreur de préparation plutôt qu'une erreur de validation.</p>
 */
@Service
public class Sc06FileScenarioExecutionFacade implements FileScenarioExecutionFacade {

    private final ScenarioSc06ExecutionService executionService;
    private final ObjectMapper objectMapper;

    public Sc06FileScenarioExecutionFacade(ScenarioSc06ExecutionService executionService,
                                           ObjectMapper objectMapper) {
        this.executionService = executionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String supportedScenarioType() {
        return "SC-06";
    }

    @Override
    public ScenarioResponseDTO execute(JsonNode payload) throws Exception {
        Sc06ScenarioRequestDTO request = objectMapper.treeToValue(payload, Sc06ScenarioRequestDTO.class);
        return executionService.solve(request);
    }
}
