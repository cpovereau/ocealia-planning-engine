package fr.project.planning.domain.creneau;

/**
 * Priorité métier d'un créneau.
 * Ne porte aucune logique de décision.
 */
public enum PrioriteCreneau {

    /**
     * Créneau critique (service essentiel, obligation forte).
     */
    CRITIQUE,

    /**
     * Créneau standard.
     */
    NORMALE,

    /**
     * Créneau à faible priorité métier.
     */
    FAIBLE
}
