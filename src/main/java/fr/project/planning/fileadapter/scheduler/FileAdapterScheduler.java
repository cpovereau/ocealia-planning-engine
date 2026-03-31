package fr.project.planning.fileadapter.scheduler;

import fr.project.planning.fileadapter.config.FileAdapterProperties;
import fr.project.planning.fileadapter.service.FileAdapterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler qui déclenche le batch de traitement FileAdapter à intervalle fixe.
 *
 * Le polling est actif uniquement si file-adapter.enabled=true.
 * L'intervalle est configurable via file-adapter.polling-interval-ms (défaut : 5000 ms).
 *
 * Désactiver le FileAdapter en production :
 * <pre>
 * file-adapter:
 *   enabled: false
 * </pre>
 */
@Component
public class FileAdapterScheduler {

    private static final Logger log = LoggerFactory.getLogger(FileAdapterScheduler.class);

    private final FileAdapterProperties properties;
    private final FileAdapterService fileAdapterService;

    public FileAdapterScheduler(FileAdapterProperties properties, FileAdapterService fileAdapterService) {
        this.properties = properties;
        this.fileAdapterService = fileAdapterService;
    }

    /**
     * Déclenché à intervalle fixe (fixedDelay pour éviter les chevauchements de batch).
     * Sort immédiatement si le FileAdapter est désactivé.
     */
    @Scheduled(
            fixedDelayString = "${file-adapter.polling-interval-ms:5000}",
            initialDelayString = "${file-adapter.polling-interval-ms:5000}"
    )
    public void poll() {
        if (!properties.isEnabled()) {
            return;
        }
        log.debug("[FileAdapter] Déclenchement du batch polling...");
        fileAdapterService.processBatch();
    }
}
