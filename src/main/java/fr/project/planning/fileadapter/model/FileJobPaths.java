package fr.project.planning.fileadapter.model;

import java.nio.file.Path;

/**
 * Chemins de sortie résolus pour un job fichier.
 *
 * Calculés à partir du nom du fichier source et des répertoires configurés.
 * Les chemins d'archive ne sont pas stockés ici car ils dépendent du clientId
 * et de la date de traitement (structure dynamique YYYY/MM/clientId/).
 */
public class FileJobPaths {

    private final Path outboxFile;
    private final Path errorFile;

    public FileJobPaths(Path outboxFile, Path errorFile) {
        this.outboxFile = outboxFile;
        this.errorFile = errorFile;
    }

    /** Chemin de sortie en cas de succès (outbox/). */
    public Path getOutboxFile() { return outboxFile; }

    /** Chemin de sortie en cas d'erreur (error/). */
    public Path getErrorFile() { return errorFile; }
}
