package fr.project.planning.config;

import fr.project.planning.solution.PlanningProblem;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.SolutionManager;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OptaPlannerConfig {

    @Bean
    public SolutionManager<PlanningProblem, HardSoftScore> solutionManager(
            SolverFactory<PlanningProblem> solverFactory) {
        return SolutionManager.create(solverFactory);
    }
}