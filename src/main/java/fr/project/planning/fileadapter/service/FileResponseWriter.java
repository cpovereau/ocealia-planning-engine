package fr.project.planning.fileadapter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.fileadapter.model.FileSuccessResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Écrit l'enveloppe de transport SUCCESS dans le répertoire outbox/ au format JSON indenté.
 *
 * Écrit un {@link FileSuccessResponse} contenant requestId, status:"SUCCESS"
 * et le ScenarioResponseDTO sous la clé "response".
 * ScenarioResponseDTO n'est pas modifié.
 */
@Service
public class FileResponseWriter {

    private static final Logger log = LoggerFactory.getLogger(FileResponseWriter.class);

    private final ObjectMapper objectMapper;

    public FileResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Sérialise l'enveloppe SUCCESS et l'écrit dans le fichier de sortie.
     *
     * @param envelope enveloppe contenant requestId, status et la réponse métier
     * @param outputFile chemin cible dans outbox/
     * @throws IOException si l'écriture échoue
     */
    public void write(FileSuccessResponse envelope, Path outputFile) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile.toFile(), envelope);
        log.info("[FileAdapter] Réponse écrite : {}", outputFile.getFileName());
    }
}
