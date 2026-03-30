package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningResponse;
import fr.project.planning.api.PlanningService;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.domain.workmetrics.WorkMetricsCalculator;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;
import fr.project.planning.scenarios.dto.ScenarioRequestDTO;
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
 * ScenarioSc01ExecutionService
 *
 * Orchestre l'exécution complète du scénario SC-01 :
 * préparation → résolution → métriques → réponse.
 *
 * Le ScenarioController délègue intégralement à ce service.
 */
@Service
public class ScenarioSc01ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioSc01ExecutionService.class);

    private final ScenarioSc01PreparationService preparationService;
    private final PlanningService planningService;
    private final ScenarioResponseMapper responseMapper = new ScenarioResponseMapper();

    public ScenarioSc01ExecutionService(
            ScenarioSc01PreparationService preparationService,
            PlanningService planningService
    ) {
        this.preparationService = preparationService;
        this.planningService = planningService;
    }

    public ScenarioResponseDTO solve(ScenarioRequestDTO request) {
        PreparedSc01Scenario prepared = preparationService.prepare(request);

        PlanningResponse solved = planningService.solve(prepared.planningRequest());

        WorkMetricsCalculator calculator = new WorkMetricsCalculator();
        Map<String, WorkMetrics> byId = new HashMap<>();
        calculator.compute(solved.solution()).forEach((r, wm) -> byId.put(r.getId(), wm));

        log.info("[SC-01] Score final OptaPlanner = {}", solved.solution().getScore());
        log.info("[SC-01] Affectations = {}", solved.solution().getCreneaux().stream()
                .map(c -> c.getId() + " -> " + (c.getRessourceAffectee() != null
                        ? c.getRessourceAffectee().getId() : "null"))
                .toList());

        List<ScenarioAlertDTO> alerts = prepared.buildResult().alerts().stream()
                .map(a -> new ScenarioAlertDTO(a.code().name(), a.date(), a.message()))
                .toList();

        return responseMapper.toResponse(
                prepared.scenarioType(),
                "SOLVED",
                solved.solution().getScore().hardScore(),
                solved.solution().getScore().softScore(),
                ScoreBreakdownFactory.build(solved.explanation()),
                prepared.resourceId(),
                solved.solution().getCreneaux(),
                byId,
                alerts,
                prepared.posteVirtuelIds(),
                prepared.ignoredCreneaux()   // C1 — compteurs diagnostiques calculés à la préparation
        );
    }
}