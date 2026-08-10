package fr.project.planning.scenarios.dto;

/**
 * NatureCandidat — forme d'une solution proposée par SC-06 (lot S4).
 *
 * <p>L'ordre de déclaration porte la préférence du palier 3 du classement : une seule personne
 * avant plusieurs, salarié réel avant ressource à pourvoir.</p>
 */
public enum NatureCandidat {

    /** Un seul salarié réel couvre l'intégralité du besoin. */
    MONO_RESSOURCE,

    /** Plusieurs salariés réels se répartissent le besoin. */
    COMPOSEE,

    /** Tout ou partie du besoin repose sur un poste virtuel ou reste à pourvoir. */
    RESSOURCE_A_POURVOIR
}
