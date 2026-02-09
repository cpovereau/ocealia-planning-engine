package fr.project.planning.scoring;

/**
 * Identifie de manière unique une pénalité produite par une contrainte.
 *
 * Une PenaliteKey :
 * - ne porte aucune gravité
 * - ne connaît aucune stratégie
 * - définit uniquement l'intention et l'unité
 */
public enum PenaliteKey {

    // =========================
    // Contraintes métier (SOFT)
    // =========================

    METIER_SOFT_CRENEAU_NON_COUVERT,
    METIER_SOFT_AFFECTATION_POSTE_VIRTUEL,

    // =========================
    // Contraintes légales (SOFT)
    // =========================

    LEGAL_SOFT_TRAVAIL_NUIT_MINUTES,
    LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES,

    // =========================
    // Contraintes légales (HARD)
    // =========================

    LEGAL_HARD_NUITS_CONSECUTIVES_MAX,
    LEGAL_HARD_REPOS_OBLIGATOIRE_APRES_NUITS,
    LEGAL_HARD_DUREE_MAX_LEGALE_PAR_PERIODE
}
