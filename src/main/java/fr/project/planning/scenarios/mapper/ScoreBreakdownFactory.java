package fr.project.planning.scenarios.mapper;

import fr.project.planning.scenarios.dto.ScoreBreakdownItemDTO;
import fr.project.planning.scoring.PenaliteKey;

public final class ScoreBreakdownFactory {

    private ScoreBreakdownFactory() {
        // utilitaire
    }

    public static ScoreBreakdownItemDTO item(PenaliteKey key, double quantity, int weightedImpact) {
        return new ScoreBreakdownItemDTO(
                key.name(),
                key.getUnit().name(),
                quantity,
                weightedImpact
        );
    }
}