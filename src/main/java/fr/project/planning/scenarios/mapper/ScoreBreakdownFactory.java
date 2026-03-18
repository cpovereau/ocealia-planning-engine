package fr.project.planning.scenarios.mapper;

import fr.project.planning.scenarios.dto.ScoreBreakdownItemDTO;
import fr.project.planning.scoring.PenaliteKey;
import fr.project.planning.solution.PlanningProblem;
import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.constraint.ConstraintMatch;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * ScoreBreakdownFactory
 *
 * Transforme un ScoreExplanation OptaPlanner en List<ScoreBreakdownItemDTO>.
 *
 * Cette factory appartient à la couche scénarios (API) et est le seul endroit
 * qui connaît à la fois les PenaliteKey du domaine et les DTO de restitution.
 * PlanningService ne doit pas dépendre de cette classe.
 */
public final class ScoreBreakdownFactory {

    private static final Logger log = LoggerFactory.getLogger(ScoreBreakdownFactory.class);

    private ScoreBreakdownFactory() {
        // utilitaire
    }

    /**
     * Construit un item de scoreBreakdown pour une contrainte connue.
     */
    public static ScoreBreakdownItemDTO item(PenaliteKey key, double quantity, int weightedImpact) {
        return new ScoreBreakdownItemDTO(
                key.name(),
                key.getUnit().name(),
                quantity,
                weightedImpact
        );
    }

    /**
     * Transforme un ScoreExplanation complet en liste de ScoreBreakdownItemDTO.
     *
     * Les contraintes dont le nom ne correspond à aucune PenaliteKey connue sont
     * ignorées avec un avertissement (tolérance pour l'évolution du modèle).
     */
    public static List<ScoreBreakdownItemDTO> build(
            ScoreExplanation<PlanningProblem, HardSoftScore> explanation
    ) {
        List<ScoreBreakdownItemDTO> items = new ArrayList<>();

        for (ConstraintMatchTotal<HardSoftScore> total : explanation.getConstraintMatchTotalMap().values()) {

            PenaliteKey key;
            try {
                key = PenaliteKey.valueOf(total.getConstraintName());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown penaliteKey: {}", total.getConstraintName());
                continue;
            }

            Set<ConstraintMatch<HardSoftScore>> matches = total.getConstraintMatchSet();

            items.add(
                item(
                    key,
                    resolveQuantity(key, matches),
                    total.getScore().softScore()
                )
            );
        }

        items.sort(Comparator.comparing(ScoreBreakdownItemDTO::getPenaliteKey));
        return items;
    }

    private static double resolveQuantity(
            PenaliteKey key,
            Set<ConstraintMatch<HardSoftScore>> matches
    ) {
        return switch (key) {
            case METIER_SOFT_CRENEAU_NON_COUVERT ->
                    matches.size();

            case LEGAL_SOFT_DIMANCHES_TRAVAILLES_MAX ->
                    matches.size();

            case LEGAL_SOFT_PENIBILITES_LEGALES_MINUTES ->
                    matches.stream()
                            .mapToInt(m -> Math.abs(m.getScore().softScore()))
                            .sum();

            default ->
                    0.0;
        };
    }
}
