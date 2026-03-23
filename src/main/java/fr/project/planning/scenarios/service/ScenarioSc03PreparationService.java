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
import fr.project.planning.scenarios.dto.IgnoredCreneauxDTO;
import fr.project.planning.scenarios.dto.Sc03ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
import fr.project.planning.scenarios.dto.input.PosteVirtuelInputDTO;
import fr.project.planning.scenarios.dto.input.SalarieInputDTO;
import fr.project.planning.scenarios.mapper.ScenarioCreneauMapper;
import fr.project.planning.scenarios.mapper.ScenarioResourceMapper;
import fr.project.planning.scoring.StrategieScoring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
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

    private static final Logger log = LoggerFactory.getLogger(ScenarioSc03PreparationService.class);

    private final ScenarioResourceMapper resourceMapper;
    private final ScenarioCreneauMapper creneauMapper;

    public ScenarioSc03PreparationService(ScenarioResourceMapper resourceMapper,
                                          ScenarioCreneauMapper creneauMapper) {
        this.resourceMapper = resourceMapper;
        this.creneauMapper  = creneauMapper;
    }

    public PreparedSc03Scenario prepare(Sc03ScenarioRequestDTO request) {
        Objects.requireNonNull(request, "request");

        // [Phase 1 visibilité] Signalement des champs SC-03 reçus mais non exploités
        if (request.getScenarioParameters() != null) {
            if (request.getScenarioParameters().getPrioriteCouverture() != null) {
                log.warn("[SC-03] prioriteCouverture='{}' reçu mais non exploité — champ ignoré (Phase 5 cible)",
                        request.getScenarioParameters().getPrioriteCouverture());
            }
            if (request.getScenarioParameters().getPeriode() != null) {
                log.warn("[SC-03] scenarioParameters.periode reçu mais non exploité — l'horizon de planningContext fait foi (Phase 5 cible)");
            }
        }

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

        // 1. Référentiel d'activités — construit en premier pour filtrer les créneaux avant solveur
        ReferentielComptabiliteActivite referentiel = resourceMapper.toReferentiel(
                request.getDataSet().getReferentiels()
        );

        // 2. [Phase 2] Partition des créneaux : valides (activité connue) vs exclus (activité inconnue)
        //    On collecte directement les DTOs valides pour éviter tout dépendance sur l'id du créneau.
        List<CreneauInputDTO> creneauxValides = new ArrayList<>();
        int activiteInconnue = 0;
        for (CreneauInputDTO dto : request.getDataSet().getCreneaux()) {
            String codeUtilise;
            boolean estFallback = false;
            if (dto.getCodeActiviteId() != null && !dto.getCodeActiviteId().isBlank()) {
                codeUtilise = dto.getCodeActiviteId();
            } else {
                codeUtilise = dto.getActivite();
                estFallback = true;
            }

            if (estFallback && codeUtilise != null && !codeUtilise.isBlank()) {
                log.warn("[SC-03] créneau id='{}' : codeActiviteId absent — fallback sur activite='{}' utilisé comme clé référentiel",
                        dto.getId(), codeUtilise);
            }

            if (codeUtilise == null || codeUtilise.isBlank() || referentiel.getByCode(codeUtilise) == null) {
                activiteInconnue++;
                log.warn("[SC-03] créneau id='{}' : activité '{}' absente du référentiel — créneau exclu avant solveur",
                        dto.getId(), codeUtilise);
            } else {
                creneauxValides.add(dto);
            }
        }

        // 3. Créneaux — uniquement ceux dont l'activité est connue du référentiel
        List<Creneau> creneaux = creneauMapper.toCreneaux(creneauxValides);

        // 4. Ressources (salariés + postes virtuels + RessourceNonAffectee)
        List<Ressource> ressources = resourceMapper.toRessources(request.getDataSet());

        // 5. Indisponibilités
        List<Indisponibilite> indisponibilites = resourceMapper.toIndisponibilites(
                request.getDataSet().getIndisponibilites()
        );

        // 6. Contexte planning
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

        // 7. Paramètres réglementaires — neutres (Phase 8+ branchera les contraintes nuit)
        RegulatoryParameters regulatoryParameters = RegulatoryParameters.neutre();

        // 8. Planning Request
        PlanningRequest planningRequest = new PlanningRequest(
                planningContext,
                regulatoryParameters,
                referentiel,
                ressources,
                creneaux,
                indisponibilites
        );

        // 9. IDs postes virtuels (pour les diagnostics)
        Set<String> posteVirtuelIds = request.getDataSet().getRessources().getPostesVirtuels()
                .stream().map(PosteVirtuelInputDTO::getId).collect(Collectors.toSet());

        // 10. Comptage ignoredCreneaux (pré-résolution)
        LocalDate dateDebut = request.getPlanningContext().getHorizon().getDateDebut();
        LocalDate dateFin   = request.getPlanningContext().getHorizon().getDateFin();

        int horsHorizon = (int) request.getDataSet().getCreneaux().stream()
                .filter(dto -> dto.getDate() != null)
                .filter(dto -> dto.getDate().isBefore(dateDebut) || dto.getDate().isAfter(dateFin))
                .count();

        List<SalarieInputDTO> salaries = request.getDataSet().getRessources() != null
                && request.getDataSet().getRessources().getSalaries() != null
                ? request.getDataSet().getRessources().getSalaries() : List.of();

        List<PosteVirtuelInputDTO> postesVirtuelsList = request.getDataSet().getRessources() != null
                && request.getDataSet().getRessources().getPostesVirtuels() != null
                ? request.getDataSet().getRessources().getPostesVirtuels() : List.of();

        int aucuneRessourceDansDataset = (int) request.getDataSet().getCreneaux().stream()
                .filter(dto -> !auMoinsUneRessourceCompatible(dto, salaries, postesVirtuelsList))
                .count();

        IgnoredCreneauxDTO ignoredCreneaux = new IgnoredCreneauxDTO(horsHorizon, aucuneRessourceDansDataset, activiteInconnue);

        return new PreparedSc03Scenario(
                planningRequest,
                request.getScenarioType(),
                posteVirtuelIds,
                ignoredCreneaux
        );
    }

    /**
     * Retourne {@code true} si au moins une ressource du dataset peut potentiellement
     * couvrir le créneau d'après son activité déclarée.
     *
     * Règle : une ressource avec une liste d'activités vide ou nulle est considérée
     * comme non contrainte (peut couvrir toute activité). Une ressource contrainte
     * doit déclarer explicitement le code activité du créneau.
     *
     * Ce contrôle est structurel et pré-résolution — il ne fait intervenir ni le solveur
     * ni les contraintes OptaPlanner.
     */
    private boolean auMoinsUneRessourceCompatible(
            CreneauInputDTO creneau,
            List<SalarieInputDTO> salaries,
            List<PosteVirtuelInputDTO> postesVirtuels) {

        String activiteCode = (creneau.getCodeActiviteId() != null && !creneau.getCodeActiviteId().isBlank())
                ? creneau.getCodeActiviteId() : creneau.getActivite();

        for (SalarieInputDTO sal : salaries) {
            Set<String> acts = sal.getActivitesCompatibles();
            if (acts == null || acts.isEmpty() || acts.contains(activiteCode)) return true;
        }
        for (PosteVirtuelInputDTO pv : postesVirtuels) {
            Set<String> acts = pv.getActivitesAutorisees();
            if (acts == null || acts.isEmpty() || acts.contains(activiteCode)) return true;
        }
        return false;
    }
}
