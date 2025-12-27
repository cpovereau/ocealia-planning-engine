package fr.project.planning.score;

import fr.project.planning.domain.contexte.StrategieScoring;

/**
 * ScoreWeights
 *
 * Composant technique chargé de pondérer le score OptaPlanner
 * en fonction de la stratégie de scoring.
 *
 * Aucun sens métier direct.
 */
public class ScoreWeights {

    /**
     * Multiplicateur global appliqué aux contraintes HARD.
     * Exemple : garantir que toute HARD domine n'importe quelle SOFT.
     */
    private final int hardWeight;

    /**
     * Multiplicateur global appliqué aux contraintes SOFT.
     */
    private final int softWeight;

    /**
     * Coefficient d'accentuation selon la stratégie de scoring.
     * (exploitation, analyse RH, audit…)
     */
    private final int strategieMultiplier;

    private ScoreWeights(
            int hardWeight,
            int softWeight,
            int strategieMultiplier
    ) {
        this.hardWeight = hardWeight;
        this.softWeight = softWeight;
        this.strategieMultiplier = strategieMultiplier;
    }

    public int hard(int base) {
        return base * hardWeight;
    }

    public int soft(int base) {
        return base * softWeight * strategieMultiplier;
    }

    /* =========================
       Fabriques statiques
       ========================= */

    public static ScoreWeights fromStrategie(StrategieScoring strategie) {
        return switch (strategie) {
            case EXPLOITATION -> new ScoreWeights(
                1_000_000, // HARD écrase tout
                1,
                3          // on force la qualité
            );
            case ANALYSE_RH -> new ScoreWeights(
                1_000_000,
                1,
                1          // neutre, lisible
            );
            case AUDIT -> new ScoreWeights(
                1_000_000,
                1,
                2          // visible sans trop biaiser
            );
        };
    }
}
