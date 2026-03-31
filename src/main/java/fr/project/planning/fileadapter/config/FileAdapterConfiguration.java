package fr.project.planning.fileadapter.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration Spring du FileAdapter.
 *
 * Responsabilités :
 * - Active les @ConfigurationProperties du FileAdapter.
 * - Crée les répertoires manquants au démarrage (inbox, processing, outbox, error, archive).
 * - Active le support @Scheduled pour le scheduler du FileAdapter.
 */
@Configuration
@EnableConfigurationProperties(FileAdapterProperties.class)
@EnableScheduling
public class FileAdapterConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FileAdapterConfiguration.class);

    private final FileAdapterProperties properties;

    public FileAdapterConfiguration(FileAdapterProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void createDirectories() throws IOException {
        FileAdapterProperties.Paths paths = properties.getPaths();
        createIfAbsent(paths.getInbox());
        createIfAbsent(paths.getProcessing());
        createIfAbsent(paths.getOutbox());
        createIfAbsent(paths.getError());
        createIfAbsent(paths.getArchive());
    }

    private void createIfAbsent(String pathStr) throws IOException {
        Path path = Path.of(pathStr);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("[FileAdapter] Répertoire créé : {}", path.toAbsolutePath());
        }
    }
}
