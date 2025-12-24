package com.example.planning;

import com.example.planning.config.SolverConfigProvider;
import com.example.planning.domain.*;
import com.example.planning.solver.PlanningSolution;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.solver.Solver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Scenario5PreferencePersonnelleTest {

    @Test
    void une_preference_personnelle_peut_etre_violee_si_necessaire() {

        // --- GIVEN ---

        SalarieReel salarie = new SalarieReel("S1");

        Creneau c1 = new Creneau("C1");
        Creneau c2 = new Creneau("C2");

        // Préférence personnelle simulée :
        // le salarié n'aime pas le créneau C2
        c2.setRessourceNonPreferee(salarie);

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

        // La préférence est violée, mais aucun créneau n'est laissé vide
        assertThat(affectesAuSalarie).isEqualTo(2);
        assertThat(nonAffectes).isEqualTo(0);
    }
}
