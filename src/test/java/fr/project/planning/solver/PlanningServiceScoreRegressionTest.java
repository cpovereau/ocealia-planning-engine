package fr.project.planning.solver;

import fr.project.planning.TestSpringConfig;
import fr.project.planning.api.PlanningRequest;
import fr.project.planning.api.PlanningResponse;
import fr.project.planning.api.PlanningService;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.scenarios.dto.ScoreBreakdownItemDTO;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestSpringConfig.class)
class PlanningServiceScoreRegressionTest {

    @Autowired
    private PlanningService planningService;

    @Test
    void sc01_score_breakdown_should_remain_structurally_consistent() {

        PlanningRequest request = TestPlanningRequestFactory.sc01Minimal();

        PlanningResponse response = planningService.solve(request);

        assertNotNull(response);
        assertNotNull(response.solution());
        assertNotNull(response.solution().getScore());

        HardSoftScore score = response.solution().getScore();
        assertNotNull(score, "Le score final ne doit pas être nul");

        List<ScoreBreakdownItemDTO> breakdown = response.scoreBreakdown();
        assertNotNull(breakdown, "Le scoreBreakdown ne doit pas être nul");
        assertFalse(breakdown.isEmpty(), "Le scoreBreakdown ne doit pas être vide");

        for (ScoreBreakdownItemDTO item : breakdown) {
            assertNotNull(item.getPenaliteKey(), "Chaque item doit avoir une penaliteKey");
            assertNotNull(item.getUnit(), "Chaque item doit avoir une unité");
            assertTrue(item.getQuantity() >= 0, "Chaque quantité doit être positive ou nulle");
            assertTrue(item.getWeightedImpact() <= 0, "Chaque impact pondéré doit être négatif ou nul");
        }

        assertTrue(score.softScore() <= 0, "Le score soft ne doit pas devenir positif par accident");
    }
}