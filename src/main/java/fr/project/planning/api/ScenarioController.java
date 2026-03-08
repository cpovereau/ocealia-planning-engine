package fr.project.planning.api;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.ObjectifResolution;
import fr.project.planning.domain.contexte.ResolutionType;
import fr.project.planning.domain.contexte.HypotheseHistorique;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.domain.workmetrics.WorkMetricsCalculator;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.scenarios.builder.ScenarioDatasetBuilderSc01;
import fr.project.planning.scenarios.dto.ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.dto.request.ResourceKind;
import fr.project.planning.scenarios.dto.request.Sc01ScenarioParametersDTO;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;
import fr.project.planning.scenarios.mapper.ScenarioResponseMapper;
import fr.project.planning.scoring.StrategieScoring;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/scenarios")
public class ScenarioController {

    private static final Logger log = LoggerFactory.getLogger(ScenarioController.class);

    private final ScenarioResponseMapper responseMapper = new ScenarioResponseMapper();
    private final ScenarioDatasetBuilderSc01 builder = new ScenarioDatasetBuilderSc01();
    private final PlanningService planningService;

    public ScenarioController(PlanningService planningService) {
    this.planningService = planningService;
    }

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

        // 5️⃣ Construction des facts solveur

        // PlanningContext "neutre" : on prend l'horizon + la stratégie issue du DTO
        StrategieScoring strategieScoring =
            StrategieScoring.valueOf(request.getPlanningContext().getStrategieScoring());


        PlanningContext planningContext = new PlanningContext(
            ObjectifResolution.ANALYSER_LE_MANQUE,
            strategieScoring,
            br.dateDebut,
            br.dateFin,
            ResolutionType.PLANNING_GLOBAL,
            HypotheseHistorique.NEUTRE
        );

        // RegulatoryParameters : neutre (22h-6h, pas de fériés) ou à partir de br.holidayDates
        RegulatoryParameters regulatoryParameters = RegulatoryParameters.neutre();

        // Referentiel : pour l’instant vide/neutre (on branchera plus tard une vraie source)
       Map<String, ComptabiliteActivite> map = new HashMap<>();

        map.put("travail", new ComptabiliteActivite(
            "travail",
            true,   // compteDansCharge
            false,  // genereDetteRepos
            false,  // estServiceCritique
            false,  // prioritaireSurConfort
            ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
        ));

        ReferentielComptabiliteActivite referentiel =
            new ReferentielComptabiliteActivite(map);

        // Value range des ressources (⚠️ inclure NonAffectee)
        List<Ressource> ressources = new java.util.ArrayList<>();
        ressources.addAll(request.getDataSet().getRessources().getSalaries());
        ressources.addAll(request.getDataSet().getRessources().getPostesVirtuels());
        ressources.add(RessourceNonAffectee.INSTANCE);

        log.info("Ressources range: {}", ressources.stream().map(Ressource::getId).toList());
        log.info("1041 present? {}", ressources.stream().anyMatch(r -> "1041".equals(r.getId())));

        // 6️⃣ Solve
        PlanningRequest pr = new PlanningRequest(
            planningContext,
            regulatoryParameters,
            referentiel,
            ressources,
            creneauxGeneres
        );

        PlanningResponse solved = planningService.solve(pr);

        WorkMetricsCalculator calculator = new WorkMetricsCalculator();
        var metrics = calculator.compute(solved.solution());

        Map<String, WorkMetrics> byId = new HashMap<>();
        metrics.forEach((r, wm) -> byId.put(r.getId(), wm));

        byId.forEach((id, wm) -> log.info(
            "[WorkMetrics] {} -> travail={} nuit={} ferie={} dimanches={} seqJours={} seqNuits={}",
            id,
            wm.getMinutesTravaillees(),
            wm.getMinutesNuit(),
            wm.getMinutesJourFerie(),
            wm.getNbDimanchesTravailles(),
            wm.getMaxJoursConsecutifsObservees(),
            wm.getMaxNuitsConsecutivesObservees()
        ));

        log.info("[SC-01] Score final OptaPlanner = {}", solved.solution().getScore());
        log.info("[SC-01] Affectations = {}", solved.solution().getCreneaux().stream()
            .map(c -> c.getId() + " -> " + (c.getRessourceAffectee() != null ? c.getRessourceAffectee().getId() : "null"))
            .toList()
        );
        
        List<Creneau> creneauxResolus = solved.solution().getCreneaux();

        List<ScenarioAlertDTO> alerts = buildResult.alerts().stream()
            .map(a -> new ScenarioAlertDTO(a.code().name(), a.date(), a.message()))
            .toList();

        return responseMapper.toResponse(
            request.getScenarioType(),
            "SOLVED",
            solved.solution().getScore().hardScore(),
            solved.solution().getScore().softScore(),
            solved.scoreBreakdown(),
            params.getResourceRef().getId(),
            creneauxResolus,
            byId,
            alerts
        );

        }

    // Endpoint pour télécharger le résultat de SC-01 en JSON
   @PostMapping(value = "/sc-01/solve/file", produces = MediaType.APPLICATION_JSON_VALUE)
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