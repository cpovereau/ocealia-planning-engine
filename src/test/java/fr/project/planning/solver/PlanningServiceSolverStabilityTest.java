package fr.project.planning.solver;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.api.PlanningResponse;
import fr.project.planning.api.PlanningService;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import fr.project.planning.TestSpringConfig;
import fr.project.planning.fixtures.TestPlanningRequestFactory;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestSpringConfig.class)
class PlanningServiceSolverStabilityTest {

    @Autowired
    private PlanningService planningService;


    @Test
    void same_planning_request_should_produce_same_observable_result_via_service() throws Exception {
        
        PlanningRequest request = TestPlanningRequestFactory.sc01Minimal();     

        PlanningResponse response1 = planningService.solve(request);
        PlanningResponse response2 = planningService.solve(request);

        assertNotNull(response1, "La première réponse ne doit pas être nulle");
        assertNotNull(response2, "La seconde réponse ne doit pas être nulle");

        PlanningProblem solution1 = response1.solution();
        PlanningProblem solution2 = response2.solution();

        assertNotNull(solution1, "La première solution ne doit pas être nulle");
        assertNotNull(solution2, "La seconde solution ne doit pas être nulle");

        HardSoftScore score1 = solution1.getScore();
        HardSoftScore score2 = solution2.getScore();

        assertEquals(score1, score2, "Le score final doit être stable");
        assertEquals(flattenBreakdown(response1), flattenBreakdown(response2), "Le scoreBreakdown doit être stable");
        assertEquals(response1.alerts(), response2.alerts(), "Les alertes doivent être stables");

        List<String> affectations1 = solution1.getCreneaux().stream()
                .sorted(Comparator
                        .comparing(fr.project.planning.domain.creneau.Creneau::getDate)
                        .thenComparing(fr.project.planning.domain.creneau.Creneau::getHeureDebut)
                        .thenComparing(fr.project.planning.domain.creneau.Creneau::getHeureFin)
                        .thenComparing(fr.project.planning.domain.creneau.Creneau::getId))
                .map(c -> c.getId() + "->" + (c.getRessourceAffectee() != null ? c.getRessourceAffectee().getId() : "null"))
                .toList();

        List<String> affectations2 = solution2.getCreneaux().stream()
                .sorted(Comparator
                        .comparing(fr.project.planning.domain.creneau.Creneau::getDate)
                        .thenComparing(fr.project.planning.domain.creneau.Creneau::getHeureDebut)
                        .thenComparing(fr.project.planning.domain.creneau.Creneau::getHeureFin)
                        .thenComparing(fr.project.planning.domain.creneau.Creneau::getId))
                .map(c -> c.getId() + "->" + (c.getRessourceAffectee() != null ? c.getRessourceAffectee().getId() : "null"))
                .toList();

        assertEquals(affectations1, affectations2, "Les affectations retournées doivent être stables");
    }
    
    private List<String> flattenBreakdown(PlanningResponse response) {
        return response.scoreBreakdown().stream()
            .sorted(Comparator.comparing(item -> item.getPenaliteKey()))
            .map(item -> item.getPenaliteKey()
                    + "|" + item.getUnit()
                    + "|" + item.getQuantity()
                    + "|" + item.getWeightedImpact())
            .toList();
        }
}