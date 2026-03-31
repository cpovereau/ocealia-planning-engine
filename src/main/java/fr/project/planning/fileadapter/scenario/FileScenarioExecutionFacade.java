package fr.project.planning.fileadapter.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;

/**
 * Contrat pour les façades d'exécution de scénario dans le FileAdapter.
 *
 * Chaque implémentation est responsable de :
 * - déclarer le type de scénario qu'elle supporte (ex : "SC-01")
 * - désérialiser le payload JSON vers le DTO métier attendu
 * - déléguer l'exécution au service métier existant correspondant
 *
 * Ajouter un nouveau scénario = implémenter cette interface + déclarer le bean Spring.
 */
public interface FileScenarioExecutionFacade {

    /**
     * Identifiant du type de scénario supporté par cette façade.
     * Doit correspondre exactement à la valeur du champ "scenarioType" dans les fichiers JSON.
     */
    String supportedScenarioType();

    /**
     * Exécute le scénario à partir du payload JSON brut.
     *
     * @param payload JsonNode représentant la requête complète (depuis le fichier)
     * @return la réponse du solveur
     * @throws Exception si la désérialisation ou l'exécution échoue
     */
    ScenarioResponseDTO execute(JsonNode payload) throws Exception;
}
