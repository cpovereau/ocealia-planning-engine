package fr.project.planning.domain.contexte;

/**
 * ObjectifResolution
 *
 * Décrit l'objectif prioritaire de la résolution.
 * Cet objectif influence la pondération globale des contraintes,
 * mais ne conditionne jamais leur existence.
 */
public enum ObjectifResolution {

    /**
     * Couvrir un maximum de créneaux,
     * quitte à accepter des violations pénalisées.
     */
    COUVRIR_A_TOUT_PRIX,

    /**
     * Trouver le meilleur compromis global
     * entre couverture, charge et violations.
     */
    EQUILIBRER,

    /**
     * Minimiser strictement les violations
     * quitte à laisser des créneaux non affectés.
     */
    MINIMISER_LES_VIOLATIONS,

    /**
     * Analyser et révéler les manques de ressources
     * (postes virtuels, non-affectations).
     */
    ANALYSER_LE_MANQUE
}
