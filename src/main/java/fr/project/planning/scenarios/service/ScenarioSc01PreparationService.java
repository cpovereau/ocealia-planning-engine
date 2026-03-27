package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.domain.contexte.HypotheseHistorique;
import fr.project.planning.domain.contexte.ObjectifResolution;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.ResolutionType;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.scenarios.builder.ScenarioDatasetBuilderSc01;
import fr.project.planning.scenarios.dto.ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.input.PosteVirtuelInputDTO;
import fr.project.planning.scenarios.dto.input.ReferentielsDTO;
import fr.project.planning.scenarios.dto.request.Sc01ScenarioParametersDTO;
import fr.project.planning.scenarios.mapper.ScenarioResourceMapper;
import fr.project.planning.scoring.StrategieScoring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ScenarioSc01PreparationService
 *
 * Prépare toutes les données nécessaires avant l'appel au solveur pour SC-01.
 * Extrait la logique de préparation métier du ScenarioController.
 *
 * Responsabilités :
 * - valider la requête SC-01
 * - résoudre la ressource cible via ScenarioResourceMapper
 * - construire le BuildRequest et générer les créneaux
 * - construire PlanningContext, RegulatoryParameters, ReferentielComptabiliteActivite
 * - assembler le PlanningRequest (avec indisponibilités Phase 3)
 * - retourner un PreparedSc01Scenario prêt pour l'exécution
 */
@Service
public class ScenarioSc01PreparationService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioSc01PreparationService.class);

    private final ScenarioResourceMapper resourceMapper;
    private final ScenarioDatasetBuilderSc01 builder = new ScenarioDatasetBuilderSc01();

    public ScenarioSc01PreparationService(ScenarioResourceMapper resourceMapper) {
        this.resourceMapper = resourceMapper;
    }

    public PreparedSc01Scenario prepare(ScenarioRequestDTO request) {
        Objects.requireNonNull(request, "request");

        if (!"SC-01".equals(request.getScenarioType())) {
            throw new IllegalArgumentException("Seul SC-01 est supporté par cet endpoint.");
        }

        Sc01ScenarioParametersDTO params = request.getScenarioParameters();
        if (params == null) {
            throw new IllegalArgumentException("scenarioParameters est requis.");
        }

        // --- A4 : Guards d'entrée ---
        if (request.getPlanningContext() == null) {
            throw new IllegalArgumentException("[SC-01] planningContext est requis.");
        }
        if (request.getPlanningContext().getHorizon() == null) {
            throw new IllegalArgumentException("[SC-01] planningContext.horizon est requis.");
        }
        LocalDate dateDebut = request.getPlanningContext().getHorizon().getDateDebut();
        LocalDate dateFin   = request.getPlanningContext().getHorizon().getDateFin();
        if (dateDebut == null) {
            throw new IllegalArgumentException("[SC-01] planningContext.horizon.dateDebut est requis.");
        }
        if (dateFin == null) {
            throw new IllegalArgumentException("[SC-01] planningContext.horizon.dateFin est requis.");
        }
        if (dateDebut.isAfter(dateFin)) {
            throw new IllegalArgumentException(
                    "[SC-01] horizon incohérent : dateDebut (" + dateDebut + ") est postérieure à dateFin (" + dateFin + ").");
        }
        if (params.getResourceRef() == null) {
            throw new IllegalArgumentException("[SC-01] scenarioParameters.resourceRef est requis.");
        }
        if (params.getResourceRef().getId() == null || params.getResourceRef().getId().isBlank()) {
            throw new IllegalArgumentException("[SC-01] scenarioParameters.resourceRef.id est requis et ne peut pas être vide.");
        }
        if (params.getDailyAmplitudeHours() <= 0) {
            throw new IllegalArgumentException("[SC-01] scenarioParameters.dailyAmplitudeHours doit être > 0.");
        }
        if (params.getShiftStart() == null) {
            throw new IllegalArgumentException("[SC-01] scenarioParameters.shiftStart est requis.");
        }

        // --- A1 : Warning dataSet.creneaux ignorés ---
        if (request.getDataSet() != null
                && request.getDataSet().getCreneaux() != null
                && !request.getDataSet().getCreneaux().isEmpty()) {
            log.warn("[SC-01] dataSet.creneaux contient {} éléments — ignorés (SC-01 génère ses créneaux via le builder)",
                    request.getDataSet().getCreneaux().size());
        }

        // --- A3 : Warning contraintesReglementaires ignorées ---
        if (request.getDataSet() != null
                && request.getDataSet().getRessources() != null
                && request.getDataSet().getRessources().getSalaries() != null) {
            boolean hasContraintes = request.getDataSet().getRessources().getSalaries().stream()
                    .anyMatch(s -> s.getContraintesReglementaires() != null);
            if (hasContraintes) {
                log.warn("[SC-01] contraintesReglementaires fournies mais ignorées (RegulatoryParameters.neutre utilisé)");
            }
        }

        // 1. Ressource cible
        Ressource ressource = resourceMapper.resolveResource(
                request.getDataSet(), params.getResourceRef()
        );

        // 2. BuildRequest
        ScenarioDatasetBuilderSc01.BuildRequest br = new ScenarioDatasetBuilderSc01.BuildRequest();
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

        br.workedDays = params.getWorkedDays() != null ? params.getWorkedDays() : Set.of();
        br.holidayDates = params.getHolidayDates() != null ? params.getHolidayDates() : Set.of();

        // 3. Générer les créneaux
        ScenarioDatasetBuilderSc01.BuildResult buildResult = builder.build(br);

        // 4. Contexte planning
        StrategieScoring strategieScoring = StrategieScoring.valueOf(
                request.getPlanningContext().getStrategieScoring()
        );
        PlanningContext planningContext = new PlanningContext(
                ObjectifResolution.ANALYSER_LE_MANQUE,
                strategieScoring,
                br.dateDebut,
                br.dateFin,
                ResolutionType.PLANNING_GLOBAL,
                HypotheseHistorique.NEUTRE
        );

        // 5. Paramètres réglementaires
        RegulatoryParameters regulatoryParameters = RegulatoryParameters.neutre();

        // 6. Référentiel d'activités — Phase B : injecté depuis dataSet.referentiels (fallback si absent)
        ReferentielComptabiliteActivite referentiel = buildReferentielSc01(
                request.getDataSet().getReferentiels()
        );

        // 7. Value range ressources
        List<Ressource> ressources = resourceMapper.toRessources(request.getDataSet());

        // 8. Indisponibilités (Phase 3 — transportées et mappées, exploitées en Phase 4)
        List<Indisponibilite> indisponibilites = resourceMapper.toIndisponibilites(
                request.getDataSet().getIndisponibilites()
        );

        // 9. Planning Request
        PlanningRequest planningRequest = new PlanningRequest(
                planningContext,
                regulatoryParameters,
                referentiel,
                ressources,
                buildResult.creneaux(),
                indisponibilites
        );

        // 10. IDs postes virtuels (pour les diagnostics)
        Set<String> posteVirtuelIds = request.getDataSet().getRessources().getPostesVirtuels()
                .stream().map(PosteVirtuelInputDTO::getId).collect(Collectors.toSet());

        return new PreparedSc01Scenario(
                planningRequest,
                buildResult,
                request.getScenarioType(),
                params.getResourceRef().getId(),
                posteVirtuelIds
        );
    }

    /**
     * B1/B2 — Construit le référentiel d'activités.
     * Si referentiels fourni et non vide : utilise resourceMapper.toReferentiel() (B1).
     * Sinon : fallback sur le référentiel minimal historique SC-01 avec l'activité "travail" (B2).
     */
    private ReferentielComptabiliteActivite buildReferentielSc01(ReferentielsDTO referentiels) {
        if (referentiels != null
                && referentiels.getActivites() != null
                && !referentiels.getActivites().isEmpty()) {
            return resourceMapper.toReferentiel(referentiels);
        }
        log.warn("[SC-01] dataSet.referentiels absent ou vide — fallback sur un référentiel minimal compatible SC-01");
        Map<String, ComptabiliteActivite> map = new HashMap<>();
        map.put("travail", new ComptabiliteActivite(
                "travail",
                true,   // compteDansCharge
                false,  // genereDetteRepos
                false,  // estServiceCritique
                false,  // prioritaireSurConfort
                ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
        ));
        return new ReferentielComptabiliteActivite(map);
    }
}