package fr.project.planning.fileadapter.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.scenarios.dto.Sc05ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.service.ScenarioSc05ExecutionService;
import org.springframework.stereotype.Service;

/**
 * Façade FileAdapter pour le scénario SC-05 (arbitrage de répartition) — lot A4.
 *
 * <p>Désérialise le payload en {@link Sc05ScenarioRequestDTO} et délègue à
 * {@link ScenarioSc05ExecutionService}. Aucune logique métier : un pont entre le transport fichier
 * et le service SC-05, sur le modèle exact des façades SC-02, SC-03 et SC-06.</p>
 *
 * <p><strong>Les deux canaux ne doivent pas diverger.</strong> Le scénario n'est pas réécrit ici,
 * il est appelé : la voie fichier et la voie HTTP entrent dans le même service, au même point.
 * C'est la seule façon de garantir que l'un ne devienne pas une variante silencieuse de l'autre —
 * et c'est ce que vérifie {@code Sc05FileScenarioExecutionFacadeTest}, en comparant les deux
 * réponses sérialisées.</p>
 *
 * <h3>Une différence de comportement à connaître</h3>
 * <p>La validation déclarative ({@code @NotBlank} sur {@code salarieAId} et {@code salarieBId},
 * {@code @NotEmpty} sur {@code creneauxArbitres}) est portée par {@code @Valid} au niveau du
 * contrôleur HTTP. <strong>Par cette voie fichier, elle ne s'applique pas</strong> : ne jouent que
 * les garde-fous du service de préparation. Un paramètre manquant produit donc ici une erreur de
 * préparation — même refus, message différent. Le fichier d'erreur porte la raison, l'appelant n'a
 * rien à deviner.</p>
 */
@Service
public class Sc05FileScenarioExecutionFacade implements FileScenarioExecutionFacade {

    private final ScenarioSc05ExecutionService executionService;
    private final ObjectMapper objectMapper;

    public Sc05FileScenarioExecutionFacade(ScenarioSc05ExecutionService executionService,
                                           ObjectMapper objectMapper) {
        this.executionService = executionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String supportedScenarioType() {
        return "SC-05";
    }

    @Override
    public ScenarioResponseDTO execute(JsonNode payload) throws Exception {
        Sc05ScenarioRequestDTO request =
                objectMapper.treeToValue(payload, Sc05ScenarioRequestDTO.class);
        return executionService.solve(request);
    }
}
