package fr.project.planning.fileadapter.service;

import fr.project.planning.fileadapter.config.FileAdapterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scanne le répertoire inbox et liste les fichiers JSON en attente de traitement.
 *
 * Le scan est non-récursif (un seul niveau de répertoire).
 */
@Service
public class FileInboxScanner {

    private static final Logger log = LoggerFactory.getLogger(FileInboxScanner.class);

    private final FileAdapterProperties properties;

    public FileInboxScanner(FileAdapterProperties properties) {
        this.properties = properties;
    }

    /**
     * Retourne la liste des fichiers .json présents dans l'inbox.
     * Retourne une liste vide en cas d'erreur I/O (inbox inaccessible).
     */
    public List<Path> scanInbox() {
        Path inboxPath = Path.of(properties.getPaths().getInbox());
        try (Stream<Path> stream = Files.list(inboxPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .toList();
        } catch (IOException e) {
            log.error("[FileAdapter] Erreur lors du scan inbox {} : {}", inboxPath, e.getMessage());
            return Collections.emptyList();
        }
    }
}
