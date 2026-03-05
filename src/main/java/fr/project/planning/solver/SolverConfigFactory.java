package fr.project.planning.solver;

import fr.project.planning.constraints.ConstraintProviderImpl;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.solution.PlanningProblem;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class SolverConfigFactory {

    /**
     * Timeout de résolution en secondes.
     * Override possible via application.properties :
     * planning.solver.timeLimitSeconds=5
     */
    @Value("${planning.solver.timeLimitSeconds:3}")
    private long timeLimitSeconds;

    @Bean
    public SolverConfig solverConfig() {
        return new SolverConfig()
            .withSolutionClass(PlanningProblem.class)
            .withEntityClassList(List.of(Creneau.class))
            .withScoreDirectorFactory(
                new ScoreDirectorFactoryConfig()
                    .withConstraintProviderClass(ConstraintProviderImpl.class)
            )
            .withTerminationConfig(
                // OptaPlanner attend souvent un String "30s", "5m", etc.
                new TerminationConfig().withSpentLimit(Duration.ofSeconds(timeLimitSeconds))
            );
    }

    /**
     * Optionnel, mais pratique si vous voulez créer un Solver directement (tests, tooling).
     * En Spring Boot, on utilisera plutôt SolverManager (dans SolverLauncher).
     */
    @Bean
    public SolverFactory<PlanningProblem> solverFactory(SolverConfig solverConfig) {
        return SolverFactory.create(solverConfig);
    }
}