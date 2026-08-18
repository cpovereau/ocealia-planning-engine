package fr.project.planning.fileadapter.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.scenarios.dto.Sc04ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.service.ScenarioSc04ExecutionService;
import org.springframework.stereotype.Service;

/**
 * Façade FileAdapter pour le scénario SC-04 (optimisation globale) — lot O4.
 *
 * <p>Désérialise le payload en {@link Sc04ScenarioRequestDTO} et délègue à
 * {@link ScenarioSc04ExecutionService}. Aucune logique métier : un pont entre le transport fichier
 * et le service SC-04, sur le modèle exact des façades SC-02, SC-03, SC-05 et SC-06.</p>
 *
 * <p><strong>Les deux canaux ne doivent pas diverger.</strong> Le scénario n'est pas réécrit ici,
 * il est appelé : la voie fichier et la voie HTTP entrent dans le même service, au même point.
 * C'est la seule façon de garantir que l'un ne devienne pas une variante silencieuse de l'autre —
 * et c'est ce que vérifie {@code Sc04FileScenarioExecutionFacadeTest}, en comparant les deux
 * réponses sérialisées.</p>
 *
 * <p>⚠️ SC-04 est le scénario le plus susceptible d'emprunter cette voie : il porte une période
 * large, donc un dataset volumineux, et il s'exécute rarement en interactif.</p>
 *
 * <h3>Une différence de comportement à connaître</h3>
 * <p>La validation déclarative — {@code @NotNull} sur {@code datePivot} — est portée par
 * {@code @Valid} au niveau du contrôleur HTTP. <strong>Par cette voie fichier, elle ne s'applique
 * pas</strong> : ne jouent que les garde-fous du service de préparation. Une date pivot manquante
 * produit donc ici une erreur de préparation — même refus, message différent. Le fichier d'erreur
 * porte la raison, l'appelant n'a rien à deviner.</p>
 */
@Service
public class Sc04FileScenarioExecutionFacade implements FileScenarioExecutionFacade {

    private final ScenarioSc04ExecutionService executionService;
    private final ObjectMapper objectMapper;

    public Sc04FileScenarioExecutionFacade(ScenarioSc04ExecutionService executionService,
                                           ObjectMapper objectMapper) {
        this.executionService = executionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String supportedScenarioType() {
        return "SC-04";
    }

    @Override
    public ScenarioResponseDTO execute(JsonNode payload) throws Exception {
        Sc04ScenarioRequestDTO request =
                objectMapper.treeToValue(payload, Sc04ScenarioRequestDTO.class);
        return executionService.solve(request);
    }
}
