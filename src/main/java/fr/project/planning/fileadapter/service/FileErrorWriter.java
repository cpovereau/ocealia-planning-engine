package fr.project.planning.fileadapter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Écrit un fichier d'erreur JSON dans le répertoire error/ lorsque le traitement échoue.
 *
 * Format aligné avec le contrat §13.5 du document 50 :
 * { requestId, status: "ERROR", timestamp, error: { code, message, details } }
 *
 * Le requestId est passé depuis le pipeline dès qu'il a pu être extrait du JSON entrant ;
 * il peut être null si le parsing du fichier a lui-même échoué.
 */
@Service
public class FileErrorWriter {

    private static final Logger log = LoggerFactory.getLogger(FileErrorWriter.class);

    private final ObjectMapper objectMapper;

    public FileErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Écrit le rapport d'erreur dans errorFile.
     * Ne lève pas d'exception : une erreur d'écriture est loguée silencieusement.
     *
     * @param requestId requestId extrait du JSON entrant, ou null si non disponible
     * @param sourceFileName nom du fichier source (pour corrélation)
     * @param cause exception ayant provoqué l'échec
     * @param errorFile chemin cible dans error/
     */
    public void write(String requestId, String sourceFileName, Exception cause, Path errorFile) {
        // Utilise le code spécifique si c'est une FileInputValidationException, sinon le nom de classe
        String errorCode = (cause instanceof FileInputValidationException fiv)
                ? fiv.getCode()
                : cause.getClass().getSimpleName();

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", errorCode);
        error.put("message", cause.getMessage() != null ? cause.getMessage() : "");
        error.put("details", List.of());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId != null ? requestId : "UNKNOWN");
        payload.put("status", "ERROR");
        payload.put("timestamp", Instant.now().toString());
        payload.put("sourceFile", sourceFileName);
        payload.put("error", error);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(errorFile.toFile(), payload);
            log.info("[FileAdapter] Erreur écrite : {}", errorFile.getFileName());
        } catch (IOException e) {
            log.error("[FileAdapter] Impossible d'écrire le fichier d'erreur {} : {}", errorFile, e.getMessage());
        }
    }
}
