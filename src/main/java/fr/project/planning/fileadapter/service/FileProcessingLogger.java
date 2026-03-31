package fr.project.planning.fileadapter.service;

import fr.project.planning.fileadapter.model.FileProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logs structurés pour les événements du pipeline FileAdapter.
 *
 * Centralise les messages de monitoring pour faciliter l'observabilité
 * (filtrage par préfixe [FileAdapter] dans les outils de log).
 */
@Service
public class FileProcessingLogger {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingLogger.class);

    public void logStart(String jobId, String fileName) {
        log.info("[FileAdapter] [START] jobId={} fichier={}", jobId, fileName);
    }

    public void logSuccess(FileProcessingResult result) {
        log.info("[FileAdapter] [SUCCESS] jobId={} fichier={} durée={}ms",
                result.getJobId(),
                result.getSourceFileName(),
                result.getProcessingTime().toMillis());
    }

    public void logError(FileProcessingResult result) {
        log.error("[FileAdapter] [ERROR] jobId={} fichier={} erreur={}",
                result.getJobId(),
                result.getSourceFileName(),
                result.getErrorMessage());
    }
}
