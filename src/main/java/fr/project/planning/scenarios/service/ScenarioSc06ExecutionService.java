package fr.project.planning.scenarios.service;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.domain.workmetrics.WorkMetricsCalculator;
import fr.project.planning.scenarios.dto.AffectationCandidatDTO;
import fr.project.planning.scenarios.dto.CandidatDTO;
import fr.project.planning.scenarios.dto.IgnoredCreneauxDTO;
import fr.project.planning.scenarios.dto.MotifCandidat;
import fr.project.planning.scenarios.dto.MotifCandidatDTO;
import fr.project.planning.scenarios.dto.Sc06ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.mapper.Sc06ImpactFactory;
import fr.project.planning.scenarios.mapper.ScenarioResponseMapper;
import fr.project.planning.scenarios.mapper.ScoreBreakdownFactory;
import fr.project.planning.solution.PlanningProblem;
import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.SolutionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ScenarioSc06ExecutionService — orchestration du scénario SC-06 (lot S4).
 *
 * <p>Chaîne : préparation → énumération et classement → restitution.</p>
 *
 * <p>Différence majeure avec SC-01 et SC-03 : <strong>aucun appel au solveur</strong>. SC-06
 * classe des possibilités, il n'en cherche pas une. Les candidats sont évalués un à un par
 * {@link SolutionManager}, sans recherche.</p>
 *
 * <p>Les blocs {@code planning} et {@code workMetrics} décrivent la <strong>solution de rang 1,
 * et elle seule</strong> : {@code planning} ne porte que les créneaux du besoin, et
 * {@code workMetrics} que les ressources qu'elle mobilise. Le planning existant n'est pas réémis
 * — l'appelant le possède déjà, et le renvoyer pour toutes les ressources candidates alourdirait
 * la réponse sans rien apprendre. Décision §10.4 du cadrage.</p>
 */
@Service
public class ScenarioSc06ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioSc06ExecutionService.class);

    private final ScenarioSc06PreparationService preparationService;
    private final Sc06CandidatEnumerationService enumerationService;
    private final SolutionManager<PlanningProblem, HardSoftScore> solutionManager;
    private final ScenarioResponseMapper responseMapper = new ScenarioResponseMapper();

    public ScenarioSc06ExecutionService(ScenarioSc06PreparationService preparationService,
                                        Sc06CandidatEnumerationService enumerationService,
                                        SolutionManager<PlanningProblem, HardSoftScore> solutionManager) {
        this.preparationService = preparationService;
        this.enumerationService = enumerationService;
        this.solutionManager = solutionManager;
    }

    public ScenarioResponseDTO solve(Sc06ScenarioRequestDTO request) {

        PreparedSc06Scenario prepared = preparationService.prepare(request);
        List<Candidat> candidats = enumerationService.enumerer(prepared);

        // La solution de rang 1 est appliquée au problème : elle sert de base aux blocs
        // planning, workMetrics et solverResult.
        Candidat retenu = candidats.isEmpty() ? null : candidats.get(0);
        appliquer(prepared, retenu);

        ScoreExplanation<PlanningProblem, HardSoftScore> explanation =
                solutionManager.explain(prepared.problem());

        Map<String, WorkMetrics> workMetrics = workMetricsDesRessourcesMobilisees(prepared, retenu);

        log.info("[SC-06] {} candidat(s) restitué(s) — rang 1 : {}",
                candidats.size(),
                retenu == null ? "aucun" : retenu.nature() + ", conforme=" + retenu.conforme());

        ScenarioResponseDTO response = responseMapper.toResponse(
                prepared.scenarioType(),
                "SOLVED",
                explanation.getScore().hardScore(),
                explanation.getScore().softScore(),
                ScoreBreakdownFactory.build(explanation),
                null,                               // idSalarie — multi-ressources
                prepared.creneauxBesoin(),          // planning = les seuls créneaux du besoin
                workMetrics,
                prepared.alerts(),                  // [S8.4] constats de la préparation
                Set.of(),                           // posteVirtuelIds — diagnostics d'affectation neutres
                new IgnoredCreneauxDTO(0, 0, 0),    // aucun créneau écarté : la préparation refuse au lieu d'ignorer
                prepared.problem().getRegulatoryParameters()
        );

        response.setCandidats(toDto(candidats, prepared));
        return response;
    }

    // =========================================================
    // Application de la solution retenue
    // =========================================================

    private void appliquer(PreparedSc06Scenario prepared, Candidat candidat) {
        for (Creneau creneau : prepared.creneauxBesoin()) {
            Ressource affectee = candidat == null ? null : candidat.affectations().get(creneau.getId());
            creneau.setRessourceAffectee(affectee != null ? affectee : RessourceNonAffectee.INSTANCE);
        }
    }

    private Map<String, WorkMetrics> workMetricsDesRessourcesMobilisees(PreparedSc06Scenario prepared,
                                                                       Candidat retenu) {
        Map<String, WorkMetrics> toutes = new HashMap<>();
        new WorkMetricsCalculator().compute(prepared.problem())
                .forEach((ressource, metrics) -> toutes.put(ressource.getId(), metrics));

        if (retenu == null) {
            return Map.of();
        }

        Set<String> mobilisees = new HashSet<>();
        for (Ressource ressource : retenu.affectations().values()) {
            if (ressource instanceof SalarieReel) {
                mobilisees.add(ressource.getId());
            }
        }

        Map<String, WorkMetrics> filtrees = new HashMap<>();
        toutes.forEach((id, metrics) -> {
            if (mobilisees.contains(id)) {
                filtrees.put(id, metrics);
            }
        });
        return filtrees;
    }

    // =========================================================
    // Restitution
    // =========================================================

    private List<CandidatDTO> toDto(List<Candidat> candidats, PreparedSc06Scenario prepared) {
        Map<String, Creneau> besoinParId = new HashMap<>();
        for (Creneau creneau : prepared.creneauxBesoin()) {
            besoinParId.put(creneau.getId(), creneau);
        }

        List<CandidatDTO> dtos = new ArrayList<>(candidats.size());
        for (int i = 0; i < candidats.size(); i++) {
            Candidat candidat = candidats.get(i);

            List<AffectationCandidatDTO> affectations = new ArrayList<>();
            // L'ordre suit celui de la requête, pas celui de la Map : le client retrouve ses créneaux.
            for (Creneau creneau : prepared.creneauxBesoin()) {
                Ressource affectee = candidat.affectations().get(creneau.getId());
                affectations.add(new AffectationCandidatDTO(
                        creneau.getId(),
                        affectee != null ? affectee.getId() : RessourceNonAffectee.INSTANCE.getId(),
                        creneau.getCodeActiviteEffectif(),
                        creneau.getLieu(),
                        creneau.getHeureDebut().toString(),
                        creneau.getHeureFin().toString()
                ));
            }

            List<MotifCandidatDTO> motifs = candidat.motifs().stream()
                    .map(MotifCandidat::toDto)
                    .toList();

            dtos.add(new CandidatDTO(
                    i + 1,
                    candidat.conforme(),
                    candidat.couvertureComplete(),
                    candidat.nature().name(),
                    affectations,
                    Sc06ImpactFactory.build(prepared, candidat),
                    motifs
            ));
        }
        return dtos;
    }
}
