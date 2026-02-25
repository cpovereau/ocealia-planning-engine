package fr.project.planning.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.scenarios.builder.ScenarioDatasetBuilderSc01;
import fr.project.planning.scenarios.dto.ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.dto.request.ResourceKind;
import fr.project.planning.scenarios.dto.request.Sc01ScenarioParametersDTO;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;
import java.util.stream.Collectors;
import fr.project.planning.scenarios.mapper.ScenarioResponseMapper;

import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/scenarios")
public class ScenarioController {

    private final ScenarioResponseMapper responseMapper = new ScenarioResponseMapper();
    private final ScenarioDatasetBuilderSc01 builder = new ScenarioDatasetBuilderSc01();

    @GetMapping("/ping")
    public String ping() {
        return "OK";
    }

    @PostMapping("/sc-01/solve")
    public ScenarioResponseDTO solveSc01(@RequestBody ScenarioRequestDTO request) {

        // 1️⃣ Validation scénario
        if (!"SC-01".equals(request.getScenarioType())) {
            throw new IllegalArgumentException("Seul SC-01 est supporté par cet endpoint.");
        }

        Sc01ScenarioParametersDTO params = request.getScenarioParameters();
        if (params == null) {
            throw new IllegalArgumentException("scenarioParameters est requis.");
        }

        // 2️⃣ Résolution de la ressource (salarié ou poste virtuel)
        Ressource ressource = resolveRessource(request, params);

        // 3️⃣ Préparation BuildRequest
        ScenarioDatasetBuilderSc01.BuildRequest br =
                new ScenarioDatasetBuilderSc01.BuildRequest();

        br.dateDebut = request.getPlanningContext().getHorizon().getDateDebut();
        br.dateFin = request.getPlanningContext().getHorizon().getDateFin();
        br.ressource = ressource;
        br.dailyAmplitudeHours = params.getDailyAmplitudeHours();
        br.shiftStart = params.getShiftStart();
        br.shiftEndAlert = params.getShiftEndAlert();

        if (params.getLunchBreak() != null) {
            br.lunchBreakStart = params.getLunchBreak().getStart();
            br.lunchBreakEnd = params.getLunchBreak().getEnd();
        }

        br.workedDays = params.getWorkedDays() != null
                ? params.getWorkedDays()
                : Set.of();

        br.holidayDates = params.getHolidayDates() != null
                ? params.getHolidayDates()
                : Set.of();

        // 4️⃣ Génération des créneaux
        var buildResult = builder.build(br);

        List<Creneau> creneauxGeneres = buildResult.creneaux();

        // ⚠ Pour V1 on ne lance pas encore OptaPlanner.
        // On retourne directement ce qui a été généré.
        // Plus tard : solveur.solve(solution)

        List<ScenarioAlertDTO> alerts = buildResult.alerts().stream()
            .map(a -> new ScenarioAlertDTO(a.code().name(), a.date(), a.message()))
            .collect(Collectors.toList());

        ScenarioResponseDTO response = responseMapper.toResponse(
            params.getResourceRef().getId(),
            creneauxGeneres
        );

        response.setAlerts(alerts);

        return response;
    }

    // Endpoint pour télécharger le résultat de SC-01 en JSON
   @PostMapping(value = "/sc-01/solve/file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScenarioResponseDTO> solveSc01File(@RequestBody ScenarioRequestDTO request) {

        ScenarioResponseDTO response = solveSc01(request);

        String filename = "SC-01-result-" +
                request.getPlanningContext().getHorizon().getDateDebut() + "-" +
                request.getPlanningContext().getHorizon().getDateFin() + ".json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // ====================================================
    // Résolution ressource
    // ====================================================

    private Ressource resolveRessource(ScenarioRequestDTO request,
                                       Sc01ScenarioParametersDTO params) {

        String id = params.getResourceRef().getId();
        ResourceKind kind = params.getResourceRef().getKind();

        if (kind == ResourceKind.SALARIE) {

            return request.getDataSet()
                    .getRessources()
                    .getSalaries()
                    .stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException("Salarié introuvable : " + id)
                    );
        }

        if (kind == ResourceKind.POSTE_VIRTUEL) {

            return request.getDataSet()
                    .getRessources()
                    .getPostesVirtuels()
                    .stream()
                    .filter(p -> p.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException("Poste virtuel introuvable : " + id)
                    );
        }

        throw new IllegalArgumentException("Type de ressource non supporté.");
    }
}