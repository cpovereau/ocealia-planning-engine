package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningResponse;
import fr.project.planning.api.PlanningService;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.domain.workmetrics.WorkMetricsCalculator;
import fr.project.planning.scenarios.dto.Sc05ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.mapper.ScenarioResponseMapper;
import fr.project.planning.scenarios.mapper.ScoreBreakdownFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * ScenarioSc05ExecutionService — préparation → résolution → restitution de l'arbitrage.
 *
 * <p>La résolution est celle de SC-02 : le solveur cherche une affectation, et c'est le score qui
 * arbitre. SC-05 ne classe pas — il <strong>rend une répartition</strong>, pas un podium.</p>
 *
 * <h3>Ce que ce lot restitue, et ce qu'il ne restitue pas encore</h3>
 * <p>Le lot <strong>A1</strong> livre le squelette : le planning arbitré, les indicateurs
 * comparatifs de {@code workMetrics.byRessource}, la justification du score et les alertes de
 * préparation. Le bloc {@code arbitrage} — ce qui a changé pour chacun, créneaux repris et cédés,
 * écart avant / après — est le lot <strong>A2</strong> ; l'alerte d'inéquité résiduelle est le lot
 * <strong>A3</strong>.</p>
 */
@Service
public class ScenarioSc05ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioSc05ExecutionService.class);

    private final ScenarioSc05PreparationService preparationService;
    private final PlanningService planningService;
    private final ScenarioResponseMapper responseMapper = new ScenarioResponseMapper();

    public ScenarioSc05ExecutionService(ScenarioSc05PreparationService preparationService,
                                        PlanningService planningService) {
        this.preparationService = preparationService;
        this.planningService = planningService;
    }

    public ScenarioResponseDTO solve(Sc05ScenarioRequestDTO request) {
        PreparedSc05Scenario prepared = preparationService.prepare(request);

        PlanningResponse solved = planningService.solve(prepared.planningRequest());

        WorkMetricsCalculator calculator = new WorkMetricsCalculator();
        Map<String, WorkMetrics> byId = new HashMap<>();
        calculator.compute(solved.solution()).forEach((r, wm) -> byId.put(r.getId(), wm));

        log.info("[SC-05] Arbitrage entre {} — {} créneau(x) remis en jeu, {} épinglé(s) sur un tiers",
                prepared.ressourcesAutorisees(), prepared.creneauxLiberes().size(),
                prepared.creneauxTenusParUnTiers().size());

        return responseMapper.toResponse(
                "SC-05",
                "SOLVED",
                solved.solution().getScore().hardScore(),
                solved.solution().getScore().softScore(),
                ScoreBreakdownFactory.build(solved.explanation()),
                null,                                   // resourceId — multi-ressources
                solved.solution().getCreneaux(),
                prepared.base().marqueursRepos(),
                byId,
                prepared.alerts(),
                prepared.base().posteVirtuelIds(),
                prepared.base().ignoredCreneaux(),
                prepared.planningRequest().regulatoryParameters()
        );
    }
}
