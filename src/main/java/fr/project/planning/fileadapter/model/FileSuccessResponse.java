package fr.project.planning.fileadapter.model;

import fr.project.planning.scenarios.dto.ScenarioResponseDTO;

/**
 * Enveloppe de transport fichier pour les réponses SUCCESS.
 *
 * Décision de conception (2026-03-30) :
 * - {@link ScenarioResponseDTO} reste le contrat métier du moteur, inchangé.
 * - Le mode fichier y ajoute une enveloppe de transport avec {@code requestId} et {@code status}.
 * - L'API HTTP n'est pas impactée : elle continue de retourner {@code ScenarioResponseDTO} directement.
 *
 * Format produit dans outbox/ :
 * <pre>
 * {
 *   "requestId": "REQ12345",
 *   "status": "SUCCESS",
 *   "response": { ...ScenarioResponseDTO... }
 * }
 * </pre>
 */
public class FileSuccessResponse {

    private final String requestId;
    private final String status = "SUCCESS";
    private final ScenarioResponseDTO response;

    public FileSuccessResponse(String requestId, ScenarioResponseDTO response) {
        this.requestId = requestId != null ? requestId : "UNKNOWN";
        this.response = response;
    }

    public String getRequestId() { return requestId; }
    public String getStatus() { return status; }
    public ScenarioResponseDTO getResponse() { return response; }
}
