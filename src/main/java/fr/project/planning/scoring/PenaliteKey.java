package fr.project.planning.scoring;

import fr.project.planning.scenarios.dto.ScoreBreakdownUnit;

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

    METIER_SOFT_CRENEAU_NON_COUVERT(ScoreBreakdownUnit.OCCURRENCE),
    METIER_SOFT_AFFECTATION_POSTE_VIRTUEL(ScoreBreakdownUnit.OCCURRENCE),
    METIER_SOFT_DETTE_REPOS_SUR_REPOS_HEBDOMADAIRE(ScoreBreakdownUnit.JOUR),

    /**
     * Phase 8 — Créneau de nuit affecté à un salarié non déclaré travailleur de nuit.
     * Exploite travailDeNuit (mappé Phase 3, méthodes utilitaires Phase 6).
     */
    METIER_SOFT_NUIT_SALARIE_NON_NUIT(ScoreBreakdownUnit.OCCURRENCE),

    // =========================
    // Contraintes légales (SOFT)
    // =========================

    LEGAL_SOFT_JOURS_CONSECUTIFS_MAX(ScoreBreakdownUnit.JOUR),
    LEGAL_SOFT_HEURES_MIN_PAR_JOUR(ScoreBreakdownUnit.MINUTE_PONDEREE),
    LEGAL_SOFT_ALTERNANCE_JOUR_NUIT(ScoreBreakdownUnit.OCCURRENCE),
    LEGAL_SOFT_DIMANCHES_TRAVAILLES_MAX(ScoreBreakdownUnit.JOUR),
    LEGAL_SOFT_TRAVAIL_NUIT_MINUTES(ScoreBreakdownUnit.MINUTE_PONDEREE),
    LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES(ScoreBreakdownUnit.MINUTE_PONDEREE),
    LEGAL_SOFT_TRAVAIL_DIMANCHE_MINUTES(ScoreBreakdownUnit.MINUTE_PONDEREE),
    LEGAL_SOFT_TRAVAIL_NUIT_ET_DIMANCHE_MINUTES(ScoreBreakdownUnit.MINUTE_PONDEREE),
    LEGAL_SOFT_TRAVAIL_NUIT_ET_FERIE_MINUTES(ScoreBreakdownUnit.MINUTE_PONDEREE),
    LEGAL_SOFT_TRAVAIL_DIMANCHE_ET_FERIE_MINUTES(ScoreBreakdownUnit.MINUTE_PONDEREE),
    LEGAL_SOFT_PENIBILITES_LEGALES_MINUTES(ScoreBreakdownUnit.MINUTE_PONDEREE),

    // =========================
    // Contraintes métier (HARD)
    // =========================

    METIER_HARD_JOUR_FERIE_REFUSE(ScoreBreakdownUnit.OCCURRENCE),
    METIER_HARD_INDISPONIBILITE(ScoreBreakdownUnit.OCCURRENCE),

    // =========================
    // Contraintes légales (HARD)
    // =========================

    LEGAL_HARD_NUITS_CONSECUTIVES_MAX(ScoreBreakdownUnit.JOUR),
    LEGAL_HARD_REPOS_OBLIGATOIRE_APRES_NUITS(ScoreBreakdownUnit.JOUR),
    LEGAL_HARD_REPOS_HEBDOMADAIRE_GLISSANT(ScoreBreakdownUnit.JOUR),
    LEGAL_HARD_REPOS_HEBDOMADAIRE_MIN(ScoreBreakdownUnit.JOUR),
    LEGAL_HARD_DUREE_MAX_LEGALE_PAR_PERIODE(ScoreBreakdownUnit.JOUR),

    // =========================
    // Contraintes physiques (SOFT)
    // =========================

    /**
     * R10 — Amplitude journalière maximale (physique/confort).
     * Pénalité proportionnelle aux minutes de dépassement par salarié et par jour.
     */
    PHYSIQUE_SOFT_AMPLITUDE_JOURNALIERE(ScoreBreakdownUnit.MINUTE_PONDEREE),

    // =========================
    // Contraintes physiques (HARD)
    // =========================

    PHYSIQUE_HARD_CHEVAUCHEMENT_CRENEAUX(ScoreBreakdownUnit.OCCURRENCE),
    PHYSIQUE_HARD_DUREE_CRENEAU_MAX(ScoreBreakdownUnit.JOUR),
    PHYSIQUE_HARD_CUMUL_JOURNALIER_MAX(ScoreBreakdownUnit.JOUR);

    private final ScoreBreakdownUnit unit;

    PenaliteKey(ScoreBreakdownUnit unit) {
        this.unit = unit;
    }

    public ScoreBreakdownUnit getUnit() {
        return unit;
    }
}