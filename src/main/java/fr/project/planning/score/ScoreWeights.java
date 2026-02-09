package fr.project.planning.score;

import fr.project.planning.scoring.PenaliteKey;
import fr.project.planning.scoring.StrategieScoring;

import java.util.EnumMap;
import java.util.Map;

public class ScoreWeights {

    private final Map<StrategieScoring, Map<PenaliteKey, Integer>> weights;

    public ScoreWeights() {
        this.weights = new EnumMap<>(StrategieScoring.class);
        init();
    }

    private void init() {

        // =========================
        // STRATÉGIE : EXPLOITATION
        // =========================
        Map<PenaliteKey, Integer> exploitation = new EnumMap<>(PenaliteKey.class);

        exploitation.put(PenaliteKey.METIER_SOFT_CRENEAU_NON_COUVERT, 10_000);
        exploitation.put(PenaliteKey.METIER_SOFT_AFFECTATION_POSTE_VIRTUEL, 2_000);
        exploitation.put(PenaliteKey.LEGAL_SOFT_TRAVAIL_NUIT_MINUTES, 3);
        exploitation.put(PenaliteKey.LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES, 5);

        weights.put(StrategieScoring.EXPLOITATION, exploitation);

        // =========================
        // STRATÉGIE : ANALYSE_RH
        // =========================
        Map<PenaliteKey, Integer> analyseRH = new EnumMap<>(PenaliteKey.class);

        analyseRH.put(PenaliteKey.METIER_SOFT_CRENEAU_NON_COUVERT, 1_000);
        analyseRH.put(PenaliteKey.METIER_SOFT_AFFECTATION_POSTE_VIRTUEL, 500);
        analyseRH.put(PenaliteKey.LEGAL_SOFT_TRAVAIL_NUIT_MINUTES, 1);
        analyseRH.put(PenaliteKey.LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES, 1);

        weights.put(StrategieScoring.ANALYSE_RH, analyseRH);

        // =========================
        // STRATÉGIE : AUDIT
        // =========================
        Map<PenaliteKey, Integer> audit = new EnumMap<>(PenaliteKey.class);

        audit.put(PenaliteKey.METIER_SOFT_CRENEAU_NON_COUVERT, 5_000);
        audit.put(PenaliteKey.METIER_SOFT_AFFECTATION_POSTE_VIRTUEL, 1_000);
        audit.put(PenaliteKey.LEGAL_SOFT_TRAVAIL_NUIT_MINUTES, 2);
        audit.put(PenaliteKey.LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES, 2);

        weights.put(StrategieScoring.AUDIT, audit);
    }

    public int getWeight(StrategieScoring strategie, PenaliteKey key) {
        return weights
            .getOrDefault(strategie, Map.of())
            .getOrDefault(key, 0);
    }
}
