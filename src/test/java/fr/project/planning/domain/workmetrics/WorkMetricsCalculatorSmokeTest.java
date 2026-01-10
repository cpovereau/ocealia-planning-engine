package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.HypotheseHistorique;
import fr.project.planning.domain.contexte.ObjectifResolution;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.ResolutionType;
import fr.project.planning.domain.contexte.StrategieScoring;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.solution.PlanningProblem;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningContextFactory;
import fr.project.planning.fixtures.TestReferentielFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorkMetricsCalculatorSmokeTest {

    @Test
    void unPlanningVideProduitDesWorkMetricsParRessource() {
        // GIVEN
        SalarieReel salarie =
                TestRessourceFactory.salarieStandard("A");

        PlanningContext context = new PlanningContext(
        ObjectifResolution.COUVRIR_A_TOUT_PRIX,
        StrategieScoring.EXPLOITATION,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31),
        ResolutionType.PLANNING_GLOBAL,
        HypotheseHistorique.NEUTRE
        );

        PlanningProblem problem = new PlanningProblem(
                context,
                /* creneaux */ null,
                /* ressources */ java.util.List.of(salarie),
                /* referentiel */ List.of() 
        );

        WorkMetricsCalculator calculator = new WorkMetricsCalculator();

        // WHEN
        Map<?, WorkMetrics> result =
                calculator.compute(problem);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());

        WorkMetrics wm = result.get(salarie);
        assertNotNull(wm);
        assertEquals(0, wm.getMinutesTravaillees());
    }
    
    @Test
    void unCreneauSimpleGenereDuTravail() {
    // GIVEN
    SalarieReel salarie =
                TestRessourceFactory.salarieStandard("A");

    PlanningContext context = new PlanningContext(
        ObjectifResolution.COUVRIR_A_TOUT_PRIX,
        StrategieScoring.EXPLOITATION,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 31),
        ResolutionType.PLANNING_GLOBAL,
        HypotheseHistorique.NEUTRE
        );
   
     ReferentielComptabiliteActivite referentiel =
        TestReferentielFactory.referentielActiviteSansDetteRepos();

    Creneau rhdMatin = new Creneau(
        "C1",
        LocalDate.of(2026, 1, 11), // dimanche
        LocalTime.of(9, 0),
        LocalTime.of(10, 0),
        60,
        "SITE",
        "ACTIVITE",
        "PC",
        PrioriteCreneau.NORMALE,
        TypeCreneau.IMPOSE,
        TypePlageHoraire.JOUR,
        false,
        QualificationJour.RHD
        );
        rhdMatin.setRessourceAffectee(salarie);

    PlanningProblem problem = new PlanningProblem(
        context,
        referentiel,
        List.of(salarie),
        List.of(rhdMatin)
        );

    WorkMetricsCalculator calculator = new WorkMetricsCalculator();

    // WHEN
    Map<?, WorkMetrics> result =
                calculator.compute(problem);

    // THEN
    WorkMetrics wm = result.get(salarie);
    assertNotNull(wm);
    assertEquals(60, wm.getMinutesTravaillees());
    }

}


