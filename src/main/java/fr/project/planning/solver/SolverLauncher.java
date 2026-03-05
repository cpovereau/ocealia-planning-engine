package fr.project.planning.solver;

import fr.project.planning.solution.PlanningProblem;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class SolverLauncher {

    private final SolverFactory<PlanningProblem> solverFactory;

    public SolverLauncher(SolverFactory<PlanningProblem> solverFactory) {
        this.solverFactory = Objects.requireNonNull(solverFactory);
    }

    /**
     * Lance une résolution OptaPlanner et retourne la meilleure solution trouvée.
     * Le timeout/termination est piloté par la configuration (SolverConfigFactory).
     */
    public PlanningProblem solve(PlanningProblem problem) {
        Objects.requireNonNull(problem, "problem ne doit pas être null");

        Solver<PlanningProblem> solver = solverFactory.buildSolver();
        return solver.solve(problem);
    }
}