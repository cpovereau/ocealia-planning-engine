package fr.project.planning.score;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.scoring.PenaliteKey;
import fr.project.planning.scoring.PenibiliteType;
import fr.project.planning.scoring.StrategieScoring;
import fr.project.planning.time.RepartitionPenibilites;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;


public final class ScoreUtils {

    public static HardSoftScore penalitesLegalesAvecDominance(
        PlanningContext context,
        long minutesNuit,
        long minutesDimanche,
        long minutesFerie,
        long minutesNuitEtDimanche,
        long minutesNuitEtFerie,
        long minutesDimancheEtFerie,
        long minutesNuitEtDimancheEtFerie
) {
    // [Équité L1] La répartition par dominance vit désormais dans RepartitionPenibilites,
    // partagée avec le calcul des heures pondérées : deux implémentations de la même règle
    // auraient fini par diverger.
    RepartitionPenibilites repartition = RepartitionPenibilites.de(
            minutesNuit,
            minutesDimanche,
            minutesFerie,
            minutesNuitEtDimanche,
            minutesNuitEtFerie,
            minutesDimancheEtFerie,
            minutesNuitEtDimancheEtFerie,
            context.getDominancePenibilites());

    // Construire le score (uniquement les 3 clés d’arbitrage)
    HardSoftScore score = HardSoftScore.ZERO;

    score = score.add(soft(context,
            PenaliteKey.LEGAL_SOFT_TRAVAIL_NUIT_MINUTES,
            repartition.minutes(PenibiliteType.NUIT)
    ));

    score = score.add(soft(context,
            PenaliteKey.LEGAL_SOFT_TRAVAIL_DIMANCHE_MINUTES,
            repartition.minutes(PenibiliteType.DIMANCHE)
    ));

    score = score.add(soft(context,
            PenaliteKey.LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES,
            repartition.minutes(PenibiliteType.FERIE)
    ));

    return score;
}

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
