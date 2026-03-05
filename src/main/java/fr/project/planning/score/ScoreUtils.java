package fr.project.planning.score;

import fr.project.planning.domain.contexte.DominancePenibilites;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.scoring.PenaliteKey;
import fr.project.planning.scoring.PenibiliteType;
import fr.project.planning.scoring.StrategieScoring;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
    // Sécurité minimale (évite des volumes négatifs si l'appelant se trompe)
    minutesNuit = Math.max(0, minutesNuit);
    minutesDimanche = Math.max(0, minutesDimanche);
    minutesFerie = Math.max(0, minutesFerie);

    minutesNuitEtDimanche = clampNonNeg(minutesNuitEtDimanche);
    minutesNuitEtFerie = clampNonNeg(minutesNuitEtFerie);
    minutesDimancheEtFerie = clampNonNeg(minutesDimancheEtFerie);
    minutesNuitEtDimancheEtFerie = clampNonNeg(minutesNuitEtDimancheEtFerie);

    // 1) Construire les segments exclusifs (inclusion-exclusion)
    // Triple intersection retirée des doubles
    long ndf = minutesNuitEtDimancheEtFerie;

    long nd = Math.max(0, minutesNuitEtDimanche - ndf);
    long nf = Math.max(0, minutesNuitEtFerie - ndf);
    long df = Math.max(0, minutesDimancheEtFerie - ndf);

    long nSeul = Math.max(0, minutesNuit - nd - nf - ndf);
    long dSeul = Math.max(0, minutesDimanche - nd - df - ndf);
    long fSeul = Math.max(0, minutesFerie - nf - df - ndf);

    // 2) Accumulateur générique
    Map<PenibiliteType, Long> minutesFinales = new EnumMap<>(PenibiliteType.class);
    for (PenibiliteType t : PenibiliteType.values()) {
        minutesFinales.put(t, 0L);
    }

    // Segments “simples” vont dans leur catégorie
    add(minutesFinales, PenibiliteType.NUIT, nSeul);
    add(minutesFinales, PenibiliteType.DIMANCHE, dSeul);
    add(minutesFinales, PenibiliteType.FERIE, fSeul);

    // 3) Segments multi-catégories : on applique la dominance configurable
    DominancePenibilites dominance = context.getDominancePenibilites();
    List<PenibiliteType> ordre = dominance.getOrdreDominance();

    distributeToDominant(minutesFinales, ordre, EnumSet.of(PenibiliteType.NUIT, PenibiliteType.DIMANCHE), nd);
    distributeToDominant(minutesFinales, ordre, EnumSet.of(PenibiliteType.NUIT, PenibiliteType.FERIE), nf);
    distributeToDominant(minutesFinales, ordre, EnumSet.of(PenibiliteType.DIMANCHE, PenibiliteType.FERIE), df);
    distributeToDominant(minutesFinales, ordre, EnumSet.of(PenibiliteType.NUIT, PenibiliteType.DIMANCHE, PenibiliteType.FERIE), ndf);

    // 4) Construire le score (uniquement les 3 clés d’arbitrage)
    HardSoftScore score = HardSoftScore.ZERO;

    score = score.add(soft(context,
            PenaliteKey.LEGAL_SOFT_TRAVAIL_NUIT_MINUTES,
            minutesFinales.get(PenibiliteType.NUIT)
    ));

    score = score.add(soft(context,
            PenaliteKey.LEGAL_SOFT_TRAVAIL_DIMANCHE_MINUTES,
            minutesFinales.get(PenibiliteType.DIMANCHE)
    ));

    score = score.add(soft(context,
            PenaliteKey.LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES,
            minutesFinales.get(PenibiliteType.FERIE)
    ));

    return score;
}

private static long clampNonNeg(long v) {
    return Math.max(0, v);
}

private static void add(Map<PenibiliteType, Long> map, PenibiliteType type, long delta) {
    if (delta <= 0) return;
    map.put(type, map.get(type) + delta);
}

private static void distributeToDominant(
        Map<PenibiliteType, Long> minutesFinales,
        List<PenibiliteType> ordreDominance,
        Set<PenibiliteType> candidats,
        long volume
) {
    if (volume <= 0) return;

    for (PenibiliteType t : ordreDominance) {
        if (candidats.contains(t)) {
            add(minutesFinales, t, volume);
            return;
        }
    }

    // Normalement impossible si DominancePenibilites exige les 3 valeurs.
    // Mais on garde une fallback safe :
    // => attribuer à la 1ère valeur candidate
    add(minutesFinales, candidats.iterator().next(), volume);
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
