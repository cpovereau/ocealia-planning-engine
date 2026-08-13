package fr.project.planning.fileadapter.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.scenarios.dto.Sc02ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.service.ScenarioSc02ExecutionService;
import org.springframework.stereotype.Service;

/**
 * Façade FileAdapter pour le scénario SC-02 (remplacement d'un salarié absent) — lot S5.
 *
 * <p>Désérialise le payload en {@link Sc02ScenarioRequestDTO} et délègue à
 * {@link ScenarioSc02ExecutionService}. Aucune logique métier : un pont entre le transport fichier
 * et le service SC-02, sur le modèle exact des façades SC-03 et SC-06.</p>
 *
 * <p><strong>Les deux canaux ne doivent pas diverger.</strong> Le scénario n'est pas réécrit ici,
 * il est appelé : la voie fichier et la voie HTTP entrent dans le même service, au même point.
 * C'est la seule façon de garantir que l'un ne devienne pas une variante silencieuse de l'autre —
 * et c'est ce que vérifie {@code Sc02FileScenarioExecutionFacadeTest}, en comparant les deux
 * réponses sérialisées.</p>
 *
 * <h3>Une différence de comportement à connaître</h3>
 * <p>La validation déclarative ({@code @NotBlank} sur {@code salarieAbsentId}) est portée par
 * {@code @Valid} au niveau du contrôleur HTTP. <strong>Par cette voie fichier, elle ne s'applique
 * pas</strong> : ne jouent que les garde-fous du service de préparation. Un
 * {@code salarieAbsentId} absent produit donc ici une erreur de préparation — même refus, message
 * différent. Le fichier d'erreur porte la raison, l'appelant n'a rien à deviner.</p>
 */
@Service
public class Sc02FileScenarioExecutionFacade implements FileScenarioExecutionFacade {

    private final ScenarioSc02ExecutionService executionService;
    private final ObjectMapper objectMapper;

    public Sc02FileScenarioExecutionFacade(ScenarioSc02ExecutionService executionService,
                                           ObjectMapper objectMapper) {
        this.executionService = executionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String supportedScenarioType() {
        return "SC-02";
    }

    @Override
    public ScenarioResponseDTO execute(JsonNode payload) throws Exception {
        Sc02ScenarioRequestDTO request =
                objectMapper.treeToValue(payload, Sc02ScenarioRequestDTO.class);
        return executionService.solve(request);
    }
}
