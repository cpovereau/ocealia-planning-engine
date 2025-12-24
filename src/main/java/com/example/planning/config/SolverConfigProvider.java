package com.example.planning.config;

import com.example.planning.domain.Creneau;
import com.example.planning.solver.PlanningConstraintProvider;
import com.example.planning.solver.PlanningSolution;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;

public class SolverConfigProvider {

    public static SolverFactory<PlanningSolution> solverFactory() {

        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(PlanningSolution.class)
                .withEntityClasses(Creneau.class)
                .withConstraintProviderClass(PlanningConstraintProvider.class)
                .withTerminationConfig(
                        new TerminationConfig()
                                .withSecondsSpentLimit(1L)
                );

        return SolverFactory.create(solverConfig);
    }
}
