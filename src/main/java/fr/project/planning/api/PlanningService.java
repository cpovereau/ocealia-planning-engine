package fr.project.planning.api;

import fr.project.planning.solution.PlanningProblem;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.solver.SolverLauncher;
import org.optaplanner.core.api.solver.SolutionManager;
import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * PlanningService — solveur pur.
 *
 * Responsabilité unique : recevoir un PlanningRequest, construire le
 * PlanningProblem, appeler le solveur, retourner la solution brute.
 *
 * Ne contient aucune logique de restitution ni dépendance vers les DTO scénarios.
 */
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
        ReferentielComptabiliteActivite referentiel = request.referentielComptabiliteActivite();

        PlanningProblem problem = new PlanningProblem(
                context,
                regulatory,
                referentiel,
                ressources,
                creneaux,
                request.indisponibilites()
        );
        problem.setReposHebdomadaires(request.reposHebdomadaires());
        if (request.seuilsSurcharge() != null && !request.seuilsSurcharge().estVide()) {
            problem.setSeuilsSurcharge(java.util.List.of(request.seuilsSurcharge()));
        }

        PlanningProblem solved = solverLauncher.solve(problem);

        ScoreExplanation<PlanningProblem, HardSoftScore> explanation = solutionManager.explain(solved);
        log.info("[ScoreExplanation]\n{}", explanation.getSummary());

        return new PlanningResponse(solved, explanation);
    }
}
