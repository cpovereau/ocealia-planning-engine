package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningResponse;
import fr.project.planning.api.PlanningService;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.domain.workmetrics.WorkMetricsCalculator;
import fr.project.planning.scenarios.dto.Sc03ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.mapper.ScenarioResponseMapper;
import fr.project.planning.scenarios.mapper.ScoreBreakdownFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ScenarioSc03ExecutionService
 *
 * Orchestre l'exécution complète du scénario SC-03 :
 * préparation → résolution → métriques → réponse.
 *
 * Différences vs SC-01 :
 * - alerts = List.of() (SC-03 n'a pas de BuildResult)
 * - resourceId = null (multi-ressources — pas de salarié cible unique)
 */
@Service
public class ScenarioSc03ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioSc03ExecutionService.class);

    private final ScenarioSc03PreparationService preparationService;
    private final PlanningService planningService;
    private final ScenarioResponseMapper responseMapper = new ScenarioResponseMapper();

    public ScenarioSc03ExecutionService(
            ScenarioSc03PreparationService preparationService,
            PlanningService planningService
    ) {
        this.preparationService = preparationService;
        this.planningService    = planningService;
    }

    public ScenarioResponseDTO solve(Sc03ScenarioRequestDTO request) {
        PreparedSc03Scenario prepared = preparationService.prepare(request);

        PlanningResponse solved = planningService.solve(prepared.planningRequest());

        WorkMetricsCalculator calculator = new WorkMetricsCalculator();
        Map<String, WorkMetrics> byId = new HashMap<>();
        calculator.compute(solved.solution()).forEach((r, wm) -> byId.put(r.getId(), wm));

        log.info("[SC-03] Score final OptaPlanner = {}", solved.solution().getScore());
        log.info("[SC-03] Affectations = {}", solved.solution().getCreneaux().stream()
                .map(c -> c.getId() + " -> " + (c.getRessourceAffectee() != null
                        ? c.getRessourceAffectee().getId() : "null"))
                .toList());

        List<ScenarioAlertDTO> alerts = List.of();  // SC-03 n'utilise pas de builder SC-01

        return responseMapper.toResponse(
                prepared.scenarioType(),
                "SOLVED",
                solved.solution().getScore().hardScore(),
                solved.solution().getScore().softScore(),
                ScoreBreakdownFactory.build(solved.explanation()),
                null,                                   // resourceId — multi-ressources
                solved.solution().getCreneaux(),
                prepared.marqueursRepos(),              // [S7.9b] restitués, jamais résolus
                byId,
                alerts,
                prepared.posteVirtuelIds(),
                prepared.ignoredCreneaux()
        );
    }
}
