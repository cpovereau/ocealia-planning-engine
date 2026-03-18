package fr.project.planning.api;

import fr.project.planning.scenarios.dto.Sc03ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.service.ScenarioSc01ExecutionService;
import fr.project.planning.scenarios.service.ScenarioSc03ExecutionService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ScenarioController
 *
 * Validation HTTP minimale et délégation.
 * Toute la logique métier est dans les services d'exécution par scénario.
 */
@RestController
@RequestMapping("/scenarios")
public class ScenarioController {

    private final ScenarioSc01ExecutionService scenarioSc01ExecutionService;
    private final ScenarioSc03ExecutionService scenarioSc03ExecutionService;

    public ScenarioController(ScenarioSc01ExecutionService scenarioSc01ExecutionService,
                               ScenarioSc03ExecutionService scenarioSc03ExecutionService) {
        this.scenarioSc01ExecutionService = scenarioSc01ExecutionService;
        this.scenarioSc03ExecutionService = scenarioSc03ExecutionService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "OK";
    }

    @PostMapping("/sc01/solve")
    public ScenarioResponseDTO solveSc01(@RequestBody ScenarioRequestDTO request) {
        return scenarioSc01ExecutionService.solve(request);
    }

    @PostMapping("/sc03/solve")
    public ScenarioResponseDTO solveSc03(@RequestBody Sc03ScenarioRequestDTO request) {
        return scenarioSc03ExecutionService.solve(request);
    }

    @PostMapping(value = "/sc01/solve/file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScenarioResponseDTO> solveSc01File(@RequestBody ScenarioRequestDTO request) {

        ScenarioResponseDTO response = solveSc01(request);

        String filename = "SC-01-result-" +
                request.getPlanningContext().getHorizon().getDateDebut() + "-" +
                request.getPlanningContext().getHorizon().getDateFin() + ".json";

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString())
                .body(response);
    }
}