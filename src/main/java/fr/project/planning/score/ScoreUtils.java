package fr.project.planning.score;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.scoring.PenaliteKey;
import fr.project.planning.scoring.StrategieScoring;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

public final class ScoreUtils {

    private static final ScoreWeights SCORE_WEIGHTS = new ScoreWeights();

    private ScoreUtils() {
        // utilitaire
    }

    public static HardSoftScore soft(
    PlanningContext context,
    PenaliteKey key,
    long volume
) {

    if (volume <= 0) {
        return HardSoftScore.ZERO;
    }

    StrategieScoring strategie = context.getStrategieScoring();
    int weight = SCORE_WEIGHTS.getWeight(strategie, key);

    if (weight == 0) {
        return HardSoftScore.ZERO;
    }

    long penaltyLong = volume * weight;

    // Conversion assumée vers int (score OptaPlanner)
    int penalty = Math.toIntExact(penaltyLong);

    return HardSoftScore.ofSoft(-penalty);
    }

}
