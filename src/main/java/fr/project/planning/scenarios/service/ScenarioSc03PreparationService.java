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

        // [Phase 7] Guards — horizon temporel
        //   Ces vérifications remplacent les NPE silencieuses qui se produiraient plus loin dans la méthode.
        //   dateDebut et dateFin sont déclarées ici pour être réutilisées par la partition Phase 3.
        if (request.getPlanningContext().getHorizon() == null) {
            throw new IllegalArgumentException("[SC-03] planningContext.horizon est requis.");
        }
        LocalDate dateDebut = request.getPlanningContext().getHorizon().getDateDebut();
        LocalDate dateFin   = request.getPlanningContext().getHorizon().getDateFin();
        if (dateDebut == null) {
            throw new IllegalArgumentException("[SC-03] planningContext.horizon.dateDebut est requise.");
        }
        if (dateFin == null) {
            throw new IllegalArgumentException("[SC-03] planningContext.horizon.dateFin est requise.");
        }
        if (dateDebut.isAfter(dateFin)) {
            throw new IllegalArgumentException(
                    "[SC-03] planningContext.horizon incohérent : dateDebut (" + dateDebut + ") est postérieure à dateFin (" + dateFin + ").");
        }

        // [Phase 7] Guard — referentiels requis
        //   Sans référentiel, toReferentiel() retournerait neutre() silencieusement et tous les créneaux
        //   seraient exclus comme activité inconnue — comportement trompeur non signalé.
        if (request.getDataSet().getReferentiels() == null) {
            throw new IllegalArgumentException("[SC-03] dataSet.referentiels est requis.");
        }

        // 1. Référentiel d'activités — construit en premier pour filtrer les créneaux avant solveur
        ReferentielComptabiliteActivite referentiel = resourceMapper.toReferentiel(
                request.getDataSet().getReferentiels()
        );

        // [Phase 7] WARN — référentiel vide
        if (request.getDataSet().getReferentiels().getActivites() == null
                || request.getDataSet().getReferentiels().getActivites().isEmpty()) {
            log.warn("[SC-03] dataSet.referentiels.activites est vide — tous les créneaux seront exclus comme activité inconnue");
        }

        // 2. [Phase 2] Partition des créneaux : valides (activité connue) vs exclus (activité inconnue)
        //    On collecte directement les DTOs valides pour éviter tout dépendance sur l'id du créneau.
        List<CreneauInputDTO> creneauxValides = new ArrayList<>();
        int activiteInconnue = 0;
        for (CreneauInputDTO dto : request.getDataSet().getCreneaux()) {
            // [Phase 7] Guards — champs horaires requis (NPE garantie sinon dans calculerDureeMinutes)
            if (dto.getHeureDebut() == null) {
                throw new IllegalArgumentException(
                        "[SC-03] créneau id='" + dto.getId() + "' : heureDebut est requise.");
            }
            if (dto.getHeureFin() == null) {
                throw new IllegalArgumentException(
                        "[SC-03] créneau id='" + dto.getId() + "' : heureFin est requise.");
            }

            // [S7.8] La règle de repli vit dans CodeActivite ; ici on ne fait que la diagnostiquer.
            // Le repli s'est produit si le code retenu existe et n'est pas la clé du contrat.
            String codeUtilise = dto.getCodeActiviteEffectif();
            boolean estFallback = codeUtilise != null && !codeUtilise.equals(dto.getCodeActiviteId());

            // [Phase 7] WARN — discordance codeActiviteId vs activite
            if (!estFallback && codeUtilise != null
                    && dto.getActivite() != null && !dto.getActivite().isBlank()
                    && !codeUtilise.equals(dto.getActivite())) {
                log.warn("[SC-03] créneau id='{}' : codeActiviteId='{}' et activite='{}' discordants — activite ignorée",
                        dto.getId(), codeUtilise, dto.getActivite());
            }

            if (estFallback) {
                log.warn("[SC-03] créneau id='{}' : codeActiviteId absent — fallback sur activite='{}' utilisé comme clé référentiel",
                        dto.getId(), codeUtilise);
            }

            if (codeUtilise == null || referentiel.getByCode(codeUtilise) == null) {
                activiteInconnue++;
                log.warn("[SC-03] créneau id='{}' : activité '{}' absente du référentiel — créneau exclu avant solveur",
                        dto.getId(), codeUtilise);
            } else {
                creneauxValides.add(dto);
            }
        }

        // 3. [Phase 3] Partition hors-horizon — sur creneauxValides (activité connue) uniquement.
        //    Ordre : activiteInconnue en premier (Phase 2), horsHorizon ensuite (Phase 3).
        //    Un créneau inconnu + hors-horizon est compté dans activiteInconnue uniquement.
        //    Cas limite : créneau à date null — ni compté, ni exclu (hors périmètre Phase 3).
        //    Note : dateDebut et dateFin sont déclarées dans les guards Phase 7 ci-dessus.
        List<CreneauInputDTO> creneauxDansHorizon = new ArrayList<>();
        int horsHorizon = 0;
        for (CreneauInputDTO dto : creneauxValides) {
            if (dto.getDate() != null && (dto.getDate().isBefore(dateDebut) || dto.getDate().isAfter(dateFin))) {
                horsHorizon++;
                log.warn("[SC-03] créneau id='{}' : date '{}' hors horizon [{} — {}] — créneau exclu avant solveur",
                        dto.getId(), dto.getDate(), dateDebut, dateFin);
            } else {
                creneauxDansHorizon.add(dto);
            }
        }

        // [Phase 7] WARN — zéro créneaux transmis au solveur après les deux partitions
        if (creneauxDansHorizon.isEmpty()) {
            log.warn("[SC-03] aucun créneau transmis au solveur après les partitions (activiteInconnue={}, horsHorizon={})",
                    activiteInconnue, horsHorizon);
        }

        // 4. Créneaux — activité connue ET dans l'horizon
        List<Creneau> creneaux = creneauMapper.toCreneaux(creneauxDansHorizon);

        // 5. Ressources (salariés + postes virtuels + RessourceNonAffectee)
        List<Ressource> ressources = resourceMapper.toRessources(request.getDataSet());

        // 6. Indisponibilités
        List<Indisponibilite> indisponibilites = resourceMapper.toIndisponibilites(
                request.getDataSet().getIndisponibilites()
        );

        // 7. Contexte planning
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
        //     horsHorizon est produit par la partition Phase 3 (cf. étape 3 ci-dessus).
        //     aucuneRessourceDansDataset est calculé sur creneauxDansHorizon uniquement :
        //     les créneaux exclus (activiteInconnue ou horsHorizon) ne gonflent pas ce compteur.
        List<SalarieInputDTO> salaries = request.getDataSet().getRessources() != null
                && request.getDataSet().getRessources().getSalaries() != null
                ? request.getDataSet().getRessources().getSalaries() : List.of();

        List<PosteVirtuelInputDTO> postesVirtuelsList = request.getDataSet().getRessources() != null
                && request.getDataSet().getRessources().getPostesVirtuels() != null
                ? request.getDataSet().getRessources().getPostesVirtuels() : List.of();

        // [Phase 7] WARN — salariés sans id
        for (SalarieInputDTO sal : salaries) {
            if (sal.getId() == null || sal.getId().isBlank()) {
                log.warn("[SC-03] salarié sans id — comportement solveur non garanti");
            }
        }

        // [Phase 7] WARN — aucune ressource réelle dans le dataset
        if (salaries.isEmpty() && postesVirtuelsList.isEmpty()) {
            log.warn("[SC-03] aucune ressource réelle dans le dataset (ni salarié, ni poste virtuel) — tous les créneaux seront affectés à RessourceNonAffectee");
        }

        int aucuneRessourceDansDataset = (int) creneauxDansHorizon.stream()
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

        String activiteCode = creneau.getCodeActiviteEffectif();

        for (SalarieInputDTO sal : salaries) {
            Set<String> acts = sal.getActivitesCompatibles();
            if (acts == null || acts.isEmpty() || acts.contains(activiteCode)) return true;
        }
        for (PosteVirtuelInputDTO pv : postesVirtuels) {
            Set<String> acts = pv.getActivitesCompatibles();
            if (acts == null || acts.isEmpty() || acts.contains(activiteCode)) return true;
        }
        return false;
    }
}
