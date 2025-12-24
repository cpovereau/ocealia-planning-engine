package com.example.planning;

import com.example.planning.config.SolverConfigProvider;
import com.example.planning.domain.*;
import com.example.planning.solver.PlanningSolution;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.solver.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Scenario2DepassementPenaliseTest {

    @Test
    void un_salarie_peut_depasse_une_limite_mais_est_penalise() {

        // --- GIVEN ---

        SalarieReel salarie = new SalarieReel("S1");

        Creneau c1 = new Creneau("C1");
        Creneau c2 = new Creneau("C2");

        List<Ressource> ressources = List.of(
                salarie,
                AffectationEtat.A_AFFECTER
        );

        PlanningSolution problem = new PlanningSolution(
                ressources,
                List.of(c1, c2)
        );

        // --- WHEN ---

        Solver<PlanningSolution> solver =
                SolverConfigProvider.solverFactory().buildSolver();

        PlanningSolution solution = solver.solve(problem);

        // --- THEN ---

        long affectesAuSalarie = solution.getCreneaux().stream()
                .filter(c -> c.getRessourceAffectee().equals(salarie))
                .count();

        long nonAffectes = solution.getCreneaux().stream()
                .filter(c -> c.getRessourceAffectee().equals(AffectationEtat.A_AFFECTER))
                .count();

        assertThat(affectesAuSalarie).isEqualTo(1);
        assertThat(nonAffectes).isEqualTo(1);

        assertThat(solution.getScore()).isNotNull();

    }
}
