package fr.project.planning.api;

import fr.project.planning.solution.PlanningProblem;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.scenarios.dto.ScoreBreakdownItemDTO;
import fr.project.planning.scenarios.dto.ScoreBreakdownUnit;
import org.optaplanner.core.api.score.constraint.ConstraintMatch;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.solver.SolverLauncher;
import org.optaplanner.core.api.solver.SolutionManager;
import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PlanningService {

    private static final Logger log = LoggerFactory.getLogger(PlanningService.class);

    private final SolverLauncher solverLauncher;
    private final SolutionManager<PlanningProblem, HardSoftScore> solutionManager;

    public PlanningService(SolverLauncher solverLauncher,
                       SolutionManager<PlanningProblem, HardSoftScore> solutionManager) {
    this.solverLauncher = solverLauncher;
    this.solutionManager = solutionManager;
    }  

    public PlanningResponse solve(PlanningRequest request) {

        Objects.requireNonNull(request, "request");

        PlanningContext context = request.planningContext();
        RegulatoryParameters regulatory = request.regulatoryParameters();
        List<Ressource> ressources = request.ressources();
        List<Creneau> creneaux = request.creneaux();

        ReferentielComptabiliteActivite referentiel =
                request.referentielComptabiliteActivite();

        PlanningProblem problem = new PlanningProblem(
                context,
                regulatory,
                referentiel,
                ressources,
                creneaux
        );

        // ---------------------------------------------------------------------
        // DEBUG (diagnostic) : forcer l'affectation de tous les créneaux à 1041
        // Objectif : révéler les contraintes HARD qui empêchent l'affectation.
        // ---------------------------------------------------------------------
        final String FORCE_RESOURCE_ID = "1041";

        // 1) Trouver la ressource 1041 dans le range
        Ressource r1041 = problem.getRessources().stream()
            .filter(r -> FORCE_RESOURCE_ID.equals(r.getId()))
            .findFirst()
            .orElse(null);

        if (r1041 == null) {
            log.warn("[Forced all->{}] Ressource introuvable dans le range.", FORCE_RESOURCE_ID);
        } else {

        // 2) Construire une copie "forcée" (on évite de muter le problem utilisé pour solve)
        PlanningProblem forced = new PlanningProblem(
            problem.getPlanningContext(),
            problem.getRegulatoryParameters(),
            problem.getReferentielComptabiliteActivite(),
            problem.getRessources(),
            problem.getCreneaux()
        );

        // 3) Forcer l'affectation
        for (Creneau c : forced.getCreneaux()) {
            c.setRessourceAffectee(r1041);
        }

        // 4) Expliquer le score de cette solution forcée
        ScoreExplanation<PlanningProblem, HardSoftScore> forcedExplanation = solutionManager.explain(forced);
        log.info("[Forced all->{}] Score = {}", FORCE_RESOURCE_ID, forcedExplanation.getScore());
        log.info("[Forced all->{}]\n{}", FORCE_RESOURCE_ID, forcedExplanation.getSummary());
        }

        // Résoudre le problème réel
        PlanningProblem solved = solverLauncher.solve(problem);

        ScoreExplanation<PlanningProblem, HardSoftScore> explanation = solutionManager.explain(solved);
        List<ScoreBreakdownItemDTO> breakdown = buildScoreBreakdown(explanation);

        log.info("[ScoreExplanation]\n{}", explanation.getSummary());

        return new PlanningResponse(solved, List.of(), breakdown);
    }

    private List<ScoreBreakdownItemDTO> buildScoreBreakdown(
            ScoreExplanation<PlanningProblem, HardSoftScore> explanation
    ) {
        List<ScoreBreakdownItemDTO> items = new ArrayList<>();

        for (ConstraintMatchTotal<HardSoftScore> total : explanation.getConstraintMatchTotalMap().values()) {
            String penaliteKey = total.getConstraintName();
            Set<ConstraintMatch<HardSoftScore>> matches = total.getConstraintMatchSet();

            ScoreBreakdownItemDTO item = new ScoreBreakdownItemDTO();
            item.setPenaliteKey(penaliteKey);
            item.setUnit(resolveUnit(penaliteKey).name());
            item.setQuantity(resolveQuantity(penaliteKey, matches));
            item.setWeightedImpact(total.getScore().softScore());

            items.add(item);
        }

    items.sort(Comparator.comparing(ScoreBreakdownItemDTO::getPenaliteKey));
    return items;
    }

    private ScoreBreakdownUnit resolveUnit(String penaliteKey) {
        return switch (penaliteKey) {
            case "METIER_SOFT_CRENEAU_NON_COUVERT" -> ScoreBreakdownUnit.OCCURRENCE;
            case "LEGAL_SOFT_DIMANCHES_TRAVAILLES_MAX" -> ScoreBreakdownUnit.JOUR;
            case "LEGAL_SOFT_PENIBILITES_LEGALES_MINUTES" -> ScoreBreakdownUnit.MINUTE_PONDEREE;
            default -> ScoreBreakdownUnit.UNKNOWN;
        };
    }

    private double resolveQuantity(
        String penaliteKey,
        Set<ConstraintMatch<HardSoftScore>> matches
    ) {
    return switch (penaliteKey) {
        case "METIER_SOFT_CRENEAU_NON_COUVERT" ->
                matches.size();

        case "LEGAL_SOFT_DIMANCHES_TRAVAILLES_MAX" ->
                matches.size();

        case "LEGAL_SOFT_PENIBILITES_LEGALES_MINUTES" ->
                matches.stream()
                        .mapToInt(m -> Math.abs(m.getScore().softScore()))
                        .sum();

        default ->
                0.0;
        };
    }
}