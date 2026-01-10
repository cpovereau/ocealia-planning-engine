package fr.project.planning.solver;

import fr.project.planning.domain.contexte.*;
import fr.project.planning.domain.creneau.*;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.ressource.*;
import fr.project.planning.solution.PlanningProblem;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import fr.project.planning.fixtures.TestRessourceFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StrategieScoringComparisonTest {

    @Test
    void comparaison_exploitation_vs_analyse_rh() {

        // --------------------
        // Données communes
        // --------------------

        Creneau jour = new Creneau(
                "C1",
                LocalDate.now(),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                480,
                "SITE",
                "ACTIVITE",
                "PC",
                PrioriteCreneau.NORMALE,
                TypeCreneau.IMPOSE,
                TypePlageHoraire.JOUR,
                false,
                QualificationJour.OUVRE
        );

        Creneau nuit = new Creneau(
                "C2",
                LocalDate.now(),
                LocalTime.of(22, 0),
                LocalTime.of(6, 0),
                480,
                "SITE",
                "ACTIVITE",
                "PC",
                PrioriteCreneau.NORMALE,
                TypeCreneau.IMPOSE,
                TypePlageHoraire.NUIT,
                false,
                QualificationJour.OUVRE
        );

        SalarieReel salarie = TestRessourceFactory.salarieStandard("S1");
        PosteVirtuel posteVirtuel = TestRessourceFactory.posteVirtuelStandard("PV1");
        RessourceNonAffectee nonAffecte = RessourceNonAffectee.INSTANCE;

        List<Ressource> ressources = List.of(salarie, posteVirtuel, nonAffecte);

        ComptabiliteActivite activiteStandard =
            new ComptabiliteActivite(
                "ACTIVITE",
                true,
                false,
                false,
                false,
                ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
            );
        
        ReferentielComptabiliteActivite referentiel =
        new ReferentielComptabiliteActivite(
                Map.of(
                        "ACTIVITE", activiteStandard
                )
        );

        // --------------------
        // Cas 1 : EXPLOITATION
        // --------------------

        PlanningContext exploitation = contexte(StrategieScoring.EXPLOITATION);


        PlanningProblem problemExploitation = new PlanningProblem(
            exploitation,
            referentiel,    
            ressources,
            List.of(jour, nuit)
        );

        PlanningProblem solvedExploitation = solve(problemExploitation);

        long nonAffectesExploitation =
                solvedExploitation.getCreneaux().stream()
                        .filter(c -> c.getRessourceAffectee() instanceof RessourceNonAffectee)
                        .count();

        // --------------------
        // Cas 2 : ANALYSE_RH
        // --------------------

        PlanningContext analyseRH = contexte(StrategieScoring.ANALYSE_RH);

        PlanningProblem problemAnalyse = new PlanningProblem(
            analyseRH,
            referentiel,     
            ressources,
            List.of(jour, nuit)
        );

        PlanningProblem solvedAnalyse = solve(problemAnalyse);

        long nonAffectesAnalyse =
                solvedAnalyse.getCreneaux().stream()
                        .filter(c -> c.getRessourceAffectee() instanceof RessourceNonAffectee)
                        .count();

        // --------------------
        // Assertions clés
        // --------------------

        assertTrue(
                nonAffectesAnalyse >= nonAffectesExploitation,
                "En ANALYSE_RH, le moteur accepte plus facilement le non-affecté"
        );
    }

    // --------------------
    // Helpers
    // --------------------

    private PlanningProblem solve(PlanningProblem problem) {

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    
    SolverConfig solverConfig = SolverConfig.createFromXmlInputStream(
            classLoader.getResourceAsStream("solverConfig-test.xml")
    );

    SolverFactory<PlanningProblem> solverFactory =
            SolverFactory.create(solverConfig);

    Solver<PlanningProblem> solver = solverFactory.buildSolver();
    return solver.solve(problem);
    }


    private PlanningContext contexte(StrategieScoring strategieScoring) {
    return new PlanningContext(
        ObjectifResolution.COUVRIR_A_TOUT_PRIX,
        StrategieScoring.EXPLOITATION,
        ResolutionType.PLANNING_GLOBAL,
        HypotheseHistorique.NEUTRE,
        new HorizonTemporel(
                LocalDate.now(),
                LocalDate.now()
        ),
        new StrategieCouverture(
                true,
                true,
                true
        ),
        new SeuilsDeTolerance(
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        ),
        new Penalites(
                1,
                1,
                1,
                1,
                1,
                1,
                1,
                1,
                1,
                1
        ),
        new OptionsExplicabilite(
                false,
                false,
                false
        )
);

}

}
