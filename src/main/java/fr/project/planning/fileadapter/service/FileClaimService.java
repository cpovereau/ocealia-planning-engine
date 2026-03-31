package fr.project.planning.fileadapter.service;

import fr.project.planning.fileadapter.config.FileAdapterProperties;
import fr.project.planning.fileadapter.model.FileJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Revendique atomiquement un fichier depuis l'inbox en le déplaçant vers processing/.
 *
 * Le déplacement atomique (ATOMIC_MOVE) évite qu'une instance concurrente
 * traite le même fichier simultanément.
 */
@Service
public class FileClaimService {

    private static final Logger log = LoggerFactory.getLogger(FileClaimService.class);

    private final FileAdapterProperties properties;

    public FileClaimService(FileAdapterProperties properties) {
        this.properties = properties;
    }

    /**
     * Tente de revendiquer le fichier donné en le déplaçant dans processing/.
     *
     * Validation §14.7 : le nom de fichier est vérifié avant tout déplacement.
     * Les noms contenant "..", "/" ou "\" sont refusés pour éviter les injections de chemin.
     *
     * @param inboxFile fichier à revendiquer depuis l'inbox
     * @return Optional contenant le FileJobContext si le claim a réussi, vide sinon
     */
    public Optional<FileJobContext> claim(Path inboxFile) {
        String jobId = UUID.randomUUID().toString();
        String fileName = inboxFile.getFileName().toString();

        if (!isSafeFileName(fileName)) {
            log.warn("[FileAdapter] Nom de fichier refusé (non sécurisé) : {}", fileName);
            return Optional.empty();
        }

        Path processingDir = Path.of(properties.getPaths().getProcessing());
        Path processingFile = processingDir.resolve(fileName);

        try {
            Files.move(inboxFile, processingFile, StandardCopyOption.ATOMIC_MOVE);
            log.info("[FileAdapter] Claim OK : {} → processing/ (jobId={})", fileName, jobId);
            return Optional.of(new FileJobContext(jobId, fileName, processingFile, Instant.now()));
        } catch (IOException e) {
            log.warn("[FileAdapter] Claim échoué pour {} (déjà traité ?) : {}", fileName, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Vérifie que le nom de fichier est sûr : aucun séparateur de chemin,
     * pas de traversal (..), composant unique (§14.7).
     */
    private boolean isSafeFileName(String fileName) {
        return fileName != null
                && !fileName.contains("..")
                && !fileName.contains("/")
                && !fileName.contains("\\")
                && Path.of(fileName).getNameCount() == 1;
    }
}
