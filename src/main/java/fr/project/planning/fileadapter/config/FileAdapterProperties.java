package fr.project.planning.fileadapter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration du FileAdapter.
 *
 * Exemple application.yml :
 * <pre>
 * file-adapter:
 *   enabled: true
 *   polling-interval-ms: 5000
 *   paths:
 *     inbox:      data/file-adapter/inbox
 *     processing: data/file-adapter/processing
 *     outbox:     data/file-adapter/outbox
 *     error:      data/file-adapter/error
 *     archive:    data/file-adapter/archive
 * </pre>
 */
@ConfigurationProperties(prefix = "file-adapter")
public class FileAdapterProperties {

    /** Active ou désactive le scheduler de polling. */
    private boolean enabled = false;

    /** Intervalle en ms entre deux scans de l'inbox. */
    private long pollingIntervalMs = 5000;

    /**
     * Taille maximale autorisée pour un fichier JSON en entrée (en octets).
     * Défaut : 10 Mo. Configurable via file-adapter.max-input-file-size-bytes.
     */
    private long maxInputFileSizeBytes = 10L * 1024 * 1024;

    private final Paths paths = new Paths();

    // --- getters / setters ---

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getPollingIntervalMs() {
        return pollingIntervalMs;
    }

    public void setPollingIntervalMs(long pollingIntervalMs) {
        this.pollingIntervalMs = pollingIntervalMs;
    }

    public long getMaxInputFileSizeBytes() {
        return maxInputFileSizeBytes;
    }

    public void setMaxInputFileSizeBytes(long maxInputFileSizeBytes) {
        this.maxInputFileSizeBytes = maxInputFileSizeBytes;
    }

    public Paths getPaths() {
        return paths;
    }

    // --- nested class ---

    public static class Paths {

        private String inbox = "data/file-adapter/inbox";
        private String processing = "data/file-adapter/processing";
        private String outbox = "data/file-adapter/outbox";
        private String error = "data/file-adapter/error";
        private String archive = "data/file-adapter/archive";

        public String getInbox() { return inbox; }
        public void setInbox(String inbox) { this.inbox = inbox; }

        public String getProcessing() { return processing; }
        public void setProcessing(String processing) { this.processing = processing; }

        public String getOutbox() { return outbox; }
        public void setOutbox(String outbox) { this.outbox = outbox; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String getArchive() { return archive; }
        public void setArchive(String archive) { this.archive = archive; }
    }
}
