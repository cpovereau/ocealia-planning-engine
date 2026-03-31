package fr.project.planning.fileadapter.service;

import com.fasterxml.jackson.databind.JsonNode;
import fr.project.planning.fileadapter.config.FileAdapterProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validation défensive des fichiers JSON en entrée du FileAdapter (§14.6).
 *
 * Deux points d'appel distincts dans le pipeline :
 * <ol>
 *   <li>{@link #validateFileMeta(Path)} — avant la lecture : extension et taille</li>
 *   <li>{@link #validatePayload(JsonNode)} — après la lecture : structure JSON et champs obligatoires</li>
 * </ol>
 *
 * Toute violation lève une {@link FileInputValidationException} avec un {@code code} machine-readable
 * et un message lisible par WinDev et le support.
 *
 * Ce composant ne contient aucune logique métier de solveur ni de scénario.
 */
@Service
public class FileInputValidator {

    private final FileAdapterProperties properties;

    public FileInputValidator(FileAdapterProperties properties) {
        this.properties = properties;
    }

    // -------------------------------------------------------------------------
    // 1. Validation métadonnées fichier (avant lecture JSON)
    // -------------------------------------------------------------------------

    /**
     * Valide l'extension et la taille du fichier avant toute lecture du contenu.
     *
     * @param file chemin vers le fichier (dans processing/)
     * @throws FileInputValidationException si la validation échoue
     */
    public void validateFileMeta(Path file) {
        validateExtension(file);
        validateSize(file);
    }

    // -------------------------------------------------------------------------
    // 2. Validation payload JSON (après lecture)
    // -------------------------------------------------------------------------

    /**
     * Valide la structure JSON et la présence des champs obligatoires.
     *
     * Champs requis : {@code requestId}, {@code scenarioType},
     * {@code metadata.clientId}, {@code metadata.timestamp}.
     *
     * @param payload le JsonNode lu depuis le fichier
     * @throws FileInputValidationException si la validation échoue
     */
    public void validatePayload(JsonNode payload) {
        validateJsonStructure(payload);
        validateRequiredField(payload, "requestId");
        validateRequiredField(payload, "scenarioType");
        validateRequiredNestedField(payload, "metadata", "clientId");
        validateRequiredNestedField(payload, "metadata", "timestamp");
    }

    // -------------------------------------------------------------------------
    // private
    // -------------------------------------------------------------------------

    private void validateExtension(Path file) {
        String name = file.getFileName().toString();
        if (!name.endsWith(".json")) {
            String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "(aucune)";
            throw new FileInputValidationException("INVALID_EXTENSION",
                    "Extension de fichier invalide : '" + ext + "' (seul .json est accepté)");
        }
    }

    private void validateSize(Path file) {
        long maxSize = properties.getMaxInputFileSizeBytes();
        try {
            long fileSize = Files.size(file);
            if (fileSize > maxSize) {
                throw new FileInputValidationException("FILE_TOO_LARGE",
                        "Fichier trop volumineux : " + fileSize + " octets (maximum autorisé : " + maxSize + " octets)");
            }
        } catch (IOException e) {
            throw new FileInputValidationException("FILE_UNREADABLE",
                    "Impossible de lire les métadonnées du fichier : " + e.getMessage());
        }
    }

    private void validateJsonStructure(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new FileInputValidationException("INVALID_JSON",
                    "Le fichier JSON n'est pas un objet valide (objet JSON attendu à la racine)");
        }
    }

    private void validateRequiredField(JsonNode payload, String fieldName) {
        JsonNode node = payload.get(fieldName);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new FileInputValidationException("MISSING_FIELD",
                    "Champ obligatoire absent ou vide : '" + fieldName + "'");
        }
    }

    private void validateRequiredNestedField(JsonNode payload, String parent, String child) {
        JsonNode parentNode = payload.get(parent);
        if (parentNode == null || parentNode.isNull() || !parentNode.isObject()) {
            throw new FileInputValidationException("MISSING_FIELD",
                    "Champ obligatoire absent ou vide : '" + parent + "." + child + "'");
        }
        JsonNode childNode = parentNode.get(child);
        if (childNode == null || childNode.isNull() || childNode.asText().isBlank()) {
            throw new FileInputValidationException("MISSING_FIELD",
                    "Champ obligatoire absent ou vide : '" + parent + "." + child + "'");
        }
    }
}
