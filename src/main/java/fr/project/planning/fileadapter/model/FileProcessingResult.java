package fr.project.planning.fileadapter.model;

import java.time.Duration;

/**
 * Résultat du traitement d'un job fichier.
 *
 * Produit par FileAdapterService à la fin de chaque cycle de traitement.
 */
public class FileProcessingResult {

    private final FileProcessingStatus status;
    private final String jobId;
    private final String sourceFileName;
    private final Duration processingTime;
    private final String errorMessage;

    private FileProcessingResult(Builder builder) {
        this.status = builder.status;
        this.jobId = builder.jobId;
        this.sourceFileName = builder.sourceFileName;
        this.processingTime = builder.processingTime;
        this.errorMessage = builder.errorMessage;
    }

    public FileProcessingStatus getStatus() { return status; }
    public String getJobId() { return jobId; }
    public String getSourceFileName() { return sourceFileName; }
    public Duration getProcessingTime() { return processingTime; }
    /** Null si success, message de l'exception sinon. */
    public String getErrorMessage() { return errorMessage; }
    public boolean isSuccess() { return status == FileProcessingStatus.SUCCESS; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private FileProcessingStatus status;
        private String jobId;
        private String sourceFileName;
        private Duration processingTime;
        private String errorMessage;

        public Builder status(FileProcessingStatus status) { this.status = status; return this; }
        public Builder jobId(String jobId) { this.jobId = jobId; return this; }
        public Builder sourceFileName(String s) { this.sourceFileName = s; return this; }
        public Builder processingTime(Duration d) { this.processingTime = d; return this; }
        public Builder errorMessage(String msg) { this.errorMessage = msg; return this; }
        public FileProcessingResult build() { return new FileProcessingResult(this); }
    }
}
