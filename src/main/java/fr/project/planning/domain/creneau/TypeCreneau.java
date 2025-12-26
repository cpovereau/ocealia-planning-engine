package fr.project.planning.domain.creneau;

/**
 * Type d'un créneau de travail.
 */
public enum TypeCreneau {

    /**
     * Créneau imposé par le métier ou le planning.
     */
    IMPOSE,

    /**
     * Créneau généré par le moteur (besoin révélé).
     */
    GENERE
}
