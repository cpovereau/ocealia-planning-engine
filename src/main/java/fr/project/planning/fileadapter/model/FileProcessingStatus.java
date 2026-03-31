package fr.project.planning.fileadapter.model;

/**
 * Cycle de vie d'un job fichier dans le pipeline du FileAdapter.
 */
public enum FileProcessingStatus {

    /** Fichier détecté dans l'inbox, pas encore revendiqué. */
    PENDING,

    /** Fichier déplacé dans processing/, traitement en cours. */
    CLAIMED,

    /** Traitement terminé avec succès, réponse écrite dans outbox/. */
    SUCCESS,

    /** Traitement échoué, rapport d'erreur écrit dans error/. */
    ERROR
}
