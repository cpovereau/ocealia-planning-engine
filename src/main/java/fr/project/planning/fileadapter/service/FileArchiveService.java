package fr.project.planning.fileadapter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

/**
 * Archive les fichiers d'un job (entrée + sortie) dans une structure hiérarchique.
 *
 * Structure cible : archive / YYYY / MM / clientId /
 *
 * Le fichier d'entrée est déplacé depuis processing/ (MOVE).
 * Le fichier de sortie (outbox ou error) est copié (COPY) : il reste disponible
 * dans outbox/error pour consultation par WinDev.
 *
 * L'archivage ne lève jamais d'exception : un échec est uniquement loggué
 * pour ne pas interrompre le traitement principal.
 */
@Service
public class FileArchiveService {

    private static final Logger log = LoggerFactory.getLogger(FileArchiveService.class);

    /**
     * Archive le fichier d'entrée (déplacement depuis processing/) et le fichier de sortie
     * (copie depuis outbox/ ou error/) dans archive/YYYY/MM/clientId/.
     *
     * @param processingFile fichier d'entrée à déplacer (dans processing/)
     * @param outputFile     fichier de sortie à copier (dans outbox/ ou error/)
     * @param archiveBaseDir répertoire racine de l'archive (depuis les propriétés)
     * @param clientId       identifiant client pour l'organisation, null → "UNKNOWN"
     */
    public void archiveJob(Path processingFile, Path outputFile,
                           Path archiveBaseDir, String clientId) {
        String safeClientId = (clientId != null && !clientId.isBlank()) ? clientId : "UNKNOWN";
        LocalDate today = LocalDate.now();
        Path archiveDir = archiveBaseDir
                .resolve(String.format("%04d", today.getYear()))
                .resolve(String.format("%02d", today.getMonthValue()))
                .resolve(safeClientId);

        try {
            Files.createDirectories(archiveDir);

            if (Files.exists(processingFile)) {
                Files.move(processingFile, archiveDir.resolve(processingFile.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
                log.info("[FileAdapter] Archivé (input) : {} → {}", processingFile.getFileName(), archiveDir);
            }

            if (Files.exists(outputFile)) {
                Files.copy(outputFile, archiveDir.resolve(outputFile.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
                log.info("[FileAdapter] Archivé (output) : {} → {}", outputFile.getFileName(), archiveDir);
            }

        } catch (IOException e) {
            log.error("[FileAdapter] Echec archivage job {} : {}", processingFile.getFileName(), e.getMessage());
        }
    }
}
