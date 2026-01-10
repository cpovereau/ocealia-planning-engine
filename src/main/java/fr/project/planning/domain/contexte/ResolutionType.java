package fr.project.planning.domain.contexte;

/**
 * Type fonctionnel de résolution demandé par l'appelant.
 *
 * Le solveur ne le déduit jamais.
 * Il sert à cadrer l'intention métier et l'explicabilité.
 */
public enum ResolutionType {

    /** Construction ou recalcul global d'un planning */
    PLANNING_GLOBAL,

    /** Application d'un cycle de travail */
    CYCLE,

    /** Remplacement ponctuel sur une période donnée */
    REMPLACEMENT,

    /** Projection / simulation sans engagement */
    PROJECTION
}
