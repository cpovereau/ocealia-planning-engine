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

    /**
     * Lot S2 de SC-02 — un même besoin découpé et confié à plusieurs personnes.
     *
     * <p>Une occurrence par ressource <strong>en excédent de la première</strong> : couvrir avec
     * une seule personne ne coûte rien.</p>
     */
    METIER_SOFT_FRAGMENTATION_CRENEAU_ORIGINE(ScoreBreakdownUnit.OCCURRENCE),

    /**
     * Lot S3 de SC-02 — charge journalière au-delà du seuil de surcharge déclaré par la demande.
     *
     * <p>Borne de <strong>confort</strong>, propre au scénario, à ne pas confondre avec
     * {@link #LEGAL_HARD_DUREE_MAX_PAR_JOUR} qui est individuelle et réglementaire.</p>
     */
    METIER_SOFT_SURCHARGE_JOUR(ScoreBreakdownUnit.MINUTE_PONDEREE),

    /** Lot S3 de SC-02 — charge hebdomadaire au-delà du seuil déclaré par la demande. */
    METIER_SOFT_SURCHARGE_SEMAINE(ScoreBreakdownUnit.MINUTE_PONDEREE),

    // =========================
    // Contraintes légales (SOFT)
    // =========================

    LEGAL_SOFT_JOURS_CONSECUTIFS_MAX(ScoreBreakdownUnit.JOUR),
    LEGAL_SOFT_HEURES_MIN_PAR_JOUR(ScoreBreakdownUnit.MINUTE_PONDEREE),

    /**
     * Lot S3 — Repos insuffisant entre deux journées travaillées successives.
     * Exploite ContraintesReglementairesSalarie.reposQuotidienMinimum.
     */
    LEGAL_SOFT_REPOS_QUOTIDIEN_MINIMUM(ScoreBreakdownUnit.MINUTE_PONDEREE),

    /**
     * Lot S3 — Dépassement de la durée hebdomadaire maximale (semaine lundi → dimanche).
     * Exploite ContraintesReglementairesSalarie.heuresMaximumParSemaine.
     */
    LEGAL_SOFT_HEURES_MAX_PAR_SEMAINE(ScoreBreakdownUnit.MINUTE_PONDEREE),

    /** Sous-emploi hebdomadaire : minutes manquantes pour atteindre le minimum contractuel. Lot S7.7. */
    LEGAL_SOFT_HEURES_MIN_PAR_SEMAINE(ScoreBreakdownUnit.MINUTE_PONDEREE),

    /**
     * Semaine complète sans aucune affectation, pour un salarié qui a un minimum contractuel.
     *
     * <p>Volet indispensable de {@link #LEGAL_SOFT_HEURES_MIN_PAR_SEMAINE} : sans lui, ne rien
     * confier à un salarié coûterait moins cher que lui confier trop peu, et le solveur
     * préférerait ne pas l'employer du tout. Voir {@code HeuresMinimumParSemaine}.</p>
     */
    LEGAL_SOFT_SEMAINE_SANS_AFFECTATION(ScoreBreakdownUnit.MINUTE_PONDEREE),

    /** Nuits travaillées en excédent du plafond hebdomadaire individuel. Lot S7.7. */
    LEGAL_SOFT_NUITS_MAX_PAR_SEMAINE(ScoreBreakdownUnit.OCCURRENCE),

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

    /**
     * Lot S2 de SC-02 — bloc de moins de 30 minutes confié à un salarié.
     *
     * <p>Une occurrence par suite contiguë trop courte. Le morceau a toujours une issue — rester
     * à pourvoir — de sorte que cette contrainte HARD ne rend jamais le problème insoluble.</p>
     */
    METIER_HARD_BLOC_CONFIE_TROP_COURT(ScoreBreakdownUnit.OCCURRENCE),

    // =========================
    // Contraintes légales (HARD)
    // =========================

    LEGAL_HARD_NUITS_CONSECUTIVES_MAX(ScoreBreakdownUnit.JOUR),
    LEGAL_HARD_REPOS_OBLIGATOIRE_APRES_NUITS(ScoreBreakdownUnit.JOUR),
    LEGAL_HARD_REPOS_HEBDOMADAIRE_GLISSANT(ScoreBreakdownUnit.JOUR),
    LEGAL_HARD_REPOS_HEBDOMADAIRE_MIN(ScoreBreakdownUnit.JOUR),
    /**
     * Durée travaillée maximale par <strong>journée</strong>.
     *
     * <p>Renommée au lot S7.6, avec son unité. L'ancien nom
     * {@code LEGAL_HARD_DUREE_MAX_LEGALE_PAR_PERIODE} annonçait un cumul sur la période, alors
     * que le seuil comparé était journalier — la contrainte mesurait une chose et en nommait une
     * autre. Le renommage est sans effet sur les clients : la contrainte étant dormante, cette
     * clé n'a jamais été émise dans un {@code scoreBreakdown}.</p>
     */
    LEGAL_HARD_DUREE_MAX_PAR_JOUR(ScoreBreakdownUnit.MINUTE_PONDEREE),

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