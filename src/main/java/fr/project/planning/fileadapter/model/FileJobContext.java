package fr.project.planning.fileadapter.model;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Contexte immuable d'un job fichier, créé lors du claim.
 *
 * Porté tout au long du pipeline de traitement pour assurer la traçabilité.
 */
public class FileJobContext {

    private final String jobId;
    private final String sourceFileName;
    private final Path processingFile;
    private final Instant startTime;

    public FileJobContext(String jobId, String sourceFileName, Path processingFile, Instant startTime) {
        this.jobId = jobId;
        this.sourceFileName = sourceFileName;
        this.processingFile = processingFile;
        this.startTime = startTime;
    }

    /** Identifiant unique du job (UUID généré au moment du claim). */
    public String getJobId() { return jobId; }

    /** Nom du fichier source (tel qu'il était dans l'inbox). */
    public String getSourceFileName() { return sourceFileName; }

    /** Chemin du fichier en cours de traitement (dans processing/). */
    public Path getProcessingFile() { return processingFile; }

    /** Timestamp de début de traitement. */
    public Instant getStartTime() { return startTime; }
}
