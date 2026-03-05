package fr.project.planning.api;

import fr.project.planning.solution.PlanningProblem;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.solver.SolverLauncher;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class PlanningService {

    private final SolverLauncher solverLauncher;

    public PlanningService(SolverLauncher solverLauncher) {
        this.solverLauncher = solverLauncher;
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

        PlanningProblem solved = solverLauncher.solve(problem);

        return new PlanningResponse(solved, List.of());
    }
}