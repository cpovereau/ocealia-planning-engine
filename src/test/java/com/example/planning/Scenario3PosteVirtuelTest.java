package com.example.planning;

import com.example.planning.config.SolverConfigProvider;
import com.example.planning.domain.*;
import com.example.planning.solver.PlanningSolution;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.solver.Solver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Scenario3PosteVirtuelTest {

    @Test
    void un_poste_virtuel_est_utilise_quand_les_salaries_sont_insuffisants() {

        // --- GIVEN ---

        SalarieReel salarie = new SalarieReel("S1");
        PosteVirtuel posteVirtuel = new PosteVirtuel("PV1");

        Creneau c1 = new Creneau("C1");
        Creneau c2 = new Creneau("C2");

        List<Ressource> ressources = List.of(
                salarie,
                posteVirtuel,
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

        long affectesAuPosteVirtuel = solution.getCreneaux().stream()
                .filter(c -> c.getRessourceAffectee().equals(posteVirtuel))
                .count();

        long nonAffectes = solution.getCreneaux().stream()
                .filter(c -> c.getRessourceAffectee().equals(AffectationEtat.A_AFFECTER))
                .count();

        assertThat(affectesAuSalarie).isEqualTo(1);
        assertThat(affectesAuPosteVirtuel).isEqualTo(1);
        assertThat(nonAffectes).isEqualTo(0);
    }
}
