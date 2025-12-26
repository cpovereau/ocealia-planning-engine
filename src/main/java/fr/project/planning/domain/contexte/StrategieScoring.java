package fr.project.planning.domain.contexte;

/**
 * StrategieScoring
 *
 * Définit l'intention d'évaluation du score.
 */
public enum StrategieScoring {

    /**
     * Priorité à la faisabilité opérationnelle.
     */
    EXPLOITATION,

    /**
     * Analyse des manques et tensions RH.
     */
    ANALYSE_RH,

    /**
     * Audit / conformité / stress-test.
     */
    AUDIT
}
