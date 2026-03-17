package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.domain.contexte.HypotheseHistorique;
import fr.project.planning.domain.contexte.ObjectifResolution;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.ResolutionType;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.scenarios.dto.Sc03ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.input.PosteVirtuelInputDTO;
import fr.project.planning.scenarios.mapper.ScenarioCreneauMapper;
import fr.project.planning.scenarios.mapper.ScenarioResourceMapper;
import fr.project.planning.scoring.StrategieScoring;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ScenarioSc03PreparationService
 *
 * Prépare toutes les données nécessaires avant l'appel au solveur pour SC-03.
 *
 * Différences vs SC-01 :
 * - les créneaux viennent du dataSet (pas d'un builder SC-01)
 * - le référentiel est construit depuis dataSet.referentiels (pas hardcodé "travail")
 * - pas de ressource cible unique : toutes les ressources du dataSet sont incluses
 * - PlanningContext construit avec les defaults (idem SC-01)
 */
@Service
public class ScenarioSc03PreparationService {

    private final ScenarioResourceMapper resourceMapper;
    private final ScenarioCreneauMapper creneauMapper;

    public ScenarioSc03PreparationService(ScenarioResourceMapper resourceMapper,
                                          ScenarioCreneauMapper creneauMapper) {
        this.resourceMapper = resourceMapper;
        this.creneauMapper  = creneauMapper;
    }

    public PreparedSc03Scenario prepare(Sc03ScenarioRequestDTO request) {
        Objects.requireNonNull(request, "request");

        if (!"SC-03".equals(request.getScenarioType())) {
            throw new IllegalArgumentException("Seul SC-03 est supporté par cet endpoint.");
        }

        if (request.getPlanningContext() == null) {
            throw new IllegalArgumentException("planningContext est requis.");
        }

        if (request.getDataSet() == null) {
            throw new IllegalArgumentException("dataSet est requis.");
        }

        if (request.getDataSet().getCreneaux() == null || request.getDataSet().getCreneaux().isEmpty()) {
            throw new IllegalArgumentException("dataSet.creneaux est requis et ne peut pas être vide.");
        }

        // 1. Créneaux depuis le dataSet
        List<Creneau> creneaux = creneauMapper.toCreneaux(request.getDataSet().getCreneaux());

        // 2. Ressources (salariés + postes virtuels + RessourceNonAffectee)
        List<Ressource> ressources = resourceMapper.toRessources(request.getDataSet());

        // 3. Indisponibilités
        List<Indisponibilite> indisponibilites = resourceMapper.toIndisponibilites(
                request.getDataSet().getIndisponibilites()
        );

        // 4. Référentiel d'activités depuis le dataSet (ACT-SOIN, ACT-ADMIN, etc.)
        ReferentielComptabiliteActivite referentiel = resourceMapper.toReferentiel(
                request.getDataSet().getReferentiels()
        );

        // 5. Contexte planning
        StrategieScoring strategieScoring = StrategieScoring.valueOf(
                request.getPlanningContext().getStrategieScoring()
        );
        PlanningContext planningContext = new PlanningContext(
                ObjectifResolution.ANALYSER_LE_MANQUE,
                strategieScoring,
                request.getPlanningContext().getHorizon().getDateDebut(),
                request.getPlanningContext().getHorizon().getDateFin(),
                ResolutionType.PLANNING_GLOBAL,
                HypotheseHistorique.NEUTRE
        );

        // 6. Paramètres réglementaires — neutres (Phase 8+ branchera les contraintes nuit)
        RegulatoryParameters regulatoryParameters = RegulatoryParameters.neutre();

        // 7. Planning Request
        PlanningRequest planningRequest = new PlanningRequest(
                planningContext,
                regulatoryParameters,
                referentiel,
                ressources,
                creneaux,
                indisponibilites
        );

        // 8. IDs postes virtuels (pour les diagnostics)
        Set<String> posteVirtuelIds = request.getDataSet().getRessources().getPostesVirtuels()
                .stream().map(PosteVirtuelInputDTO::getId).collect(Collectors.toSet());

        return new PreparedSc03Scenario(
                planningRequest,
                request.getScenarioType(),
                posteVirtuelIds
        );
    }
}
