package com.example.planning;

import com.example.planning.config.SolverConfigProvider;
import com.example.planning.domain.*;
import com.example.planning.solver.PlanningSolution;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.solver.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Scenario1SimpleAffectationTest {

    @Test
    void un_creneau_est_affecte_a_un_salarie() {

        // --- GIVEN (données d’entrée) ---

        SalarieReel salarie = new SalarieReel("S1");

        Creneau creneau = new Creneau("C1");

        List<Ressource> ressources = List.of(
                salarie,
                AffectationEtat.A_AFFECTER
        );

        PlanningSolution problem = new PlanningSolution(
                ressources,
                List.of(creneau)
        );

        // --- WHEN (résolution) ---

        SolverFactory<PlanningSolution> solverFactory =
                SolverConfigProvider.solverFactory();

        Solver<PlanningSolution> solver = solverFactory.buildSolver();

        PlanningSolution solution = solver.solve(problem);

        // --- THEN (vérifications métier) ---

        Creneau solvedCreneau = solution.getCreneaux().get(0);

        assertThat(solvedCreneau.getRessourceAffectee())
                .isNotNull()
                .isNotEqualTo(AffectationEtat.A_AFFECTER);

        assertThat(solution.getScore())
                .isNotNull();
    }
}
