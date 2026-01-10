package fr.project.planning.domain.contexte;

/**
 * Hypothèse explicite sur la prise en compte du passé.
 *
 * Le moteur ne reconstruit jamais l'historique :
 * cette information est fournie par l'appelant.
 */
public enum HypotheseHistorique {

    /** Historique considéré comme neutre */
    NEUTRE,

    /** Compteurs initiaux fournis par l'appelant */
    COMPTEURS_INITIAUX,

    /** Le passé est volontairement ignoré */
    IGNORER_ETAT_PASSE
}
