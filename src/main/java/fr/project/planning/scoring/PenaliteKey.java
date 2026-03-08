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
    METIER_SOFT_DETTE_REPOS_SUR_REPOS_HEBDOMADAIRE,

    // =========================
    // Contraintes légales (SOFT)
    // =========================

    LEGAL_SOFT_DIMANCHES_TRAVAILLES_MAX,
    LEGAL_SOFT_TRAVAIL_NUIT_MINUTES,
    LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES,
    LEGAL_SOFT_TRAVAIL_DIMANCHE_MINUTES,
    LEGAL_SOFT_TRAVAIL_NUIT_ET_DIMANCHE_MINUTES,
    LEGAL_SOFT_TRAVAIL_NUIT_ET_FERIE_MINUTES,
    LEGAL_SOFT_TRAVAIL_DIMANCHE_ET_FERIE_MINUTES,
    LEGAL_SOFT_PENIBILITES_LEGALES_MINUTES,

    // =========================
    // Contraintes légales (HARD)
    // =========================

    LEGAL_HARD_NUITS_CONSECUTIVES_MAX,
    LEGAL_HARD_REPOS_OBLIGATOIRE_APRES_NUITS,
    LEGAL_HARD_REPOS_HEBDOMADAIRE_GLISSANT,
    LEGAL_HARD_REPOS_HEBDOMADAIRE_MIN,
    LEGAL_HARD_DUREE_MAX_LEGALE_PAR_PERIODE,


    // =========================
    // Contraintes physiques (HARD)
    // =========================
    
    PHYSIQUE_HARD_CHEVAUCHEMENT_CRENEAUX,
    PHYSIQUE_HARD_DUREE_CRENEAU_MAX,
    PHYSIQUE_HARD_CUMUL_JOURNALIER_MAX
}
