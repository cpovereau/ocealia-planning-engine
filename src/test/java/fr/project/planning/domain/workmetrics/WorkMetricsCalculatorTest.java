package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningContextFactory;
import fr.project.planning.fixtures.TestReferentielFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkMetricsCalculatorTest {

    @Test
    void unRhdTravailleCompteUnDimancheEtUneOccurrenceDeReposHebdo() {

        // --------------------------------------------------
        // GIVEN
        // --------------------------------------------------

        SalarieReel salarie =
                TestRessourceFactory.salarieStandard("S1");

        PlanningContext context =
                TestPlanningContextFactory.contexteNeutre(
                        LocalDate.of(2026, 1, 5),
                        LocalDate.of(2026, 1, 11)
                );

        ReferentielComptabiliteActivite referentiel =
                TestReferentielFactory.referentielActiviteDetteRepos();

        Creneau rhd = new Creneau(
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
        rhd.setRessourceAffectee(salarie);

        PlanningProblem problem = new PlanningProblem(
                context,
                referentiel,
                List.of(salarie),
                List.of(rhd)
        );

        WorkMetricsCalculator calculator = new WorkMetricsCalculator();

        // --------------------------------------------------
        // WHEN
        // --------------------------------------------------

        Map<?, WorkMetrics> result =
                calculator.compute(problem);

        // --------------------------------------------------
        // THEN
        // --------------------------------------------------

        WorkMetrics wm = result.get(salarie);

        assertNotNull(wm, "Les WorkMetrics du salarié doivent exister");

        assertEquals(
                1,
                wm.getNbDimanchesTravailles(),
                "Un créneau RHD doit compter pour un dimanche travaillé"
        );

        assertEquals(
                1,
                wm.getNbCreneauxReposHebdoDetteRepos(),
                "Un créneau RHD doit générer une occurrence de dette repos hebdo"
        );
    }

    @Test
    void uneActiviteSansDetteNeGenereAucuneDetteReposHebdo() {

    // --------------------------------------------------
    // GIVEN
    // --------------------------------------------------

    SalarieReel salarie =
            TestRessourceFactory.salarieStandard("S1");

    PlanningContext context =
            TestPlanningContextFactory.contexteNeutre(
                    LocalDate.of(2026, 1, 5),
                    LocalDate.of(2026, 1, 11)
            );

    ReferentielComptabiliteActivite referentiel =
            TestReferentielFactory.referentielActiviteSansDetteRepos();

    Creneau creneau = new Creneau(
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
    creneau.setRessourceAffectee(salarie);

    PlanningProblem problem = new PlanningProblem(
            context,
            referentiel,
            List.of(salarie),
            List.of(creneau)
    );

    WorkMetricsCalculator calculator = new WorkMetricsCalculator();

    // --------------------------------------------------
    // WHEN
    // --------------------------------------------------

    Map<?, WorkMetrics> result =
            calculator.compute(problem);

    // --------------------------------------------------
    // THEN
    // --------------------------------------------------

    WorkMetrics wm = result.get(salarie);

    assertNotNull(wm, "Les WorkMetrics du salarié doivent exister");

    assertEquals(
            0,
            wm.getNbCreneauxReposHebdoDetteRepos(),
            "Une activité sans dette ne doit pas générer de dette repos"
    );
}

@Test
void deuxRhdSurDeuxDimanchesGenerentDeuxDettesReposHebdo() {

    // --------------------------------------------------
    // GIVEN
    // --------------------------------------------------

    SalarieReel salarie =
            TestRessourceFactory.salarieStandard("S1");

    PlanningContext context =
            TestPlanningContextFactory.contexteNeutre(
                    LocalDate.of(2026, 1, 4),
                    LocalDate.of(2026, 1, 18)
            );

    ReferentielComptabiliteActivite referentiel =
            TestReferentielFactory.referentielActiviteDetteRepos();

    Creneau rhdDimanche1 = new Creneau(
            "C1",
            LocalDate.of(2026, 1, 4),   // dimanche
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
    rhdDimanche1.setRessourceAffectee(salarie);

    Creneau rhdDimanche2 = new Creneau(
            "C2",
            LocalDate.of(2026, 1, 11),  // dimanche suivant
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
    rhdDimanche2.setRessourceAffectee(salarie);

    PlanningProblem problem = new PlanningProblem(
            context,
            referentiel,
            List.of(salarie),
            List.of(rhdDimanche1, rhdDimanche2)
    );

    WorkMetricsCalculator calculator = new WorkMetricsCalculator();

    // --------------------------------------------------
    // WHEN
    // --------------------------------------------------

    Map<?, WorkMetrics> result =
            calculator.compute(problem);

    // --------------------------------------------------
    // THEN
    // --------------------------------------------------

    WorkMetrics wm = result.get(salarie);

    assertNotNull(wm, "Les WorkMetrics du salarié doivent exister");

    assertEquals(
            2,
            wm.getNbDimanchesTravailles(),
            "Deux RHD sur deux dimanches distincts doivent compter pour deux dimanches travaillés"
    );

    assertEquals(
            2,
            wm.getNbCreneauxReposHebdoDetteRepos(),
            "Deux RHD sur deux dimanches distincts doivent générer deux dettes de repos"
    );
}

@Test
void deuxRhdLeMemeDimancheNeComptentQuUneSeuleDetteReposHebdo() {

    // --------------------------------------------------
    // GIVEN
    // --------------------------------------------------

    SalarieReel salarie =
            TestRessourceFactory.salarieStandard("S1");

    PlanningContext context =
            TestPlanningContextFactory.contexteNeutre(
                    LocalDate.of(2026, 1, 11),
                    LocalDate.of(2026, 1, 11)
            );

    ReferentielComptabiliteActivite referentiel =
            TestReferentielFactory.referentielActiviteDetteRepos();

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

    Creneau rhdApresMidi = new Creneau(
            "C2",
            LocalDate.of(2026, 1, 11), // même dimanche
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
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
    rhdApresMidi.setRessourceAffectee(salarie);

    PlanningProblem problem = new PlanningProblem(
            context,
            referentiel,
            List.of(salarie),
            List.of(rhdMatin, rhdApresMidi)
    );

    WorkMetricsCalculator calculator = new WorkMetricsCalculator();

    // --------------------------------------------------
    // WHEN
    // --------------------------------------------------

    Map<?, WorkMetrics> result =
            calculator.compute(problem);

    // --------------------------------------------------
    // THEN
    // --------------------------------------------------

    WorkMetrics wm = result.get(salarie);

    assertNotNull(wm, "Les WorkMetrics du salarié doivent exister");

    assertEquals(
            1,
            wm.getNbDimanchesTravailles(),
            "Plusieurs RHD le même dimanche ne doivent compter que pour un dimanche travaillé"
    );

    assertEquals(
            1,
            wm.getNbCreneauxReposHebdoDetteRepos(),
            "Plusieurs RHD le même jour ne doivent générer qu'une seule dette de repos hebdomadaire"
    );
}

@Test
void lesWorkMetricsSontIsolesParSalarie() {

    // --------------------------------------------------
    // GIVEN
    // --------------------------------------------------

    SalarieReel salarieA =
            TestRessourceFactory.salarieStandard("S1");

    SalarieReel salarieB =
            TestRessourceFactory.salarieStandard("S2");

    PlanningContext context =
            TestPlanningContextFactory.contexteNeutre(
                    LocalDate.of(2026, 1, 11),
                    LocalDate.of(2026, 1, 11)
            );

    ReferentielComptabiliteActivite referentiel =
            TestReferentielFactory.referentielActiviteDetteRepos();

    Creneau rhdPourA = new Creneau(
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
    rhdPourA.setRessourceAffectee(salarieA);

    // Aucun créneau pour le salarié B

    PlanningProblem problem = new PlanningProblem(
            context,
            referentiel,
            List.of(salarieA, salarieB),
            List.of(rhdPourA)
    );

    WorkMetricsCalculator calculator = new WorkMetricsCalculator();

    // --------------------------------------------------
    // WHEN
    // --------------------------------------------------

    Map<?, WorkMetrics> result =
            calculator.compute(problem);

    // --------------------------------------------------
    // THEN
    // --------------------------------------------------

    WorkMetrics wmA = result.get(salarieA);
    WorkMetrics wmB = result.get(salarieB);

    assertNotNull(wmA, "Les WorkMetrics du salarié A doivent exister");
    assertNotNull(wmB, "Les WorkMetrics du salarié B doivent exister");

    assertEquals(
            1,
            wmA.getNbDimanchesTravailles(),
            "Le salarié A doit avoir un dimanche travaillé"
    );

    assertEquals(
            1,
            wmA.getNbCreneauxReposHebdoDetteRepos(),
            "Le salarié A doit avoir une dette de repos hebdomadaire"
    );

    assertEquals(
            0,
            wmB.getNbDimanchesTravailles(),
            "Le salarié B ne doit avoir aucun dimanche travaillé"
    );

    assertEquals(
            0,
            wmB.getNbCreneauxReposHebdoDetteRepos(),
            "Le salarié B ne doit avoir aucune dette de repos"
    );
}

@Test
void uneActiviteAbsenteDuReferentielEstIgnoreeParWorkMetrics() {

    // --------------------------------------------------
    // GIVEN
    // --------------------------------------------------

    SalarieReel salarie =
            TestRessourceFactory.salarieStandard("S1");

    PlanningContext context =
            TestPlanningContextFactory.contexteNeutre(
                    LocalDate.of(2026, 1, 11),
                    LocalDate.of(2026, 1, 11)
            );

    // Référentiel vide (aucune activité connue)
    ReferentielComptabiliteActivite referentiel =
            new ReferentielComptabiliteActivite(Map.of());

    // Créneau avec une activité NON référencée
    Creneau creneauInconnu = new Creneau(
            "C1",
            LocalDate.of(2026, 1, 11), // dimanche
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            60,
            "SITE",
            "ACTIVITE_INCONNUE",
            "PC",
            PrioriteCreneau.NORMALE,
            TypeCreneau.IMPOSE,
            TypePlageHoraire.JOUR,
            false,
            QualificationJour.RHD
    );
    creneauInconnu.setRessourceAffectee(salarie);

    PlanningProblem problem = new PlanningProblem(
            context,
            referentiel,
            List.of(salarie),
            List.of(creneauInconnu)
    );

    WorkMetricsCalculator calculator = new WorkMetricsCalculator();

    // --------------------------------------------------
    // WHEN
    // --------------------------------------------------

    Map<?, WorkMetrics> result =
            calculator.compute(problem);

    // --------------------------------------------------
    // THEN
    // --------------------------------------------------

    WorkMetrics wm = result.get(salarie);

    assertNotNull(wm, "Les WorkMetrics du salarié doivent exister");

    assertEquals(
            0,
            wm.getNbDimanchesTravailles(),
            "Une activité inconnue ne doit pas compter comme dimanche travaillé"
    );

    assertEquals(
            0,
            wm.getNbCreneauxReposHebdoDetteRepos(),
            "Une activité inconnue ne doit générer aucune dette de repos"
    );
}

@Test
void unCreneauHorsHorizonEstIgnoreParWorkMetrics() {

    // --------------------------------------------------
    // GIVEN
    // --------------------------------------------------

    SalarieReel salarie =
            TestRessourceFactory.salarieStandard("S1");

    // Horizon volontairement restreint
    PlanningContext context =
            TestPlanningContextFactory.contexteNeutre(
                    LocalDate.of(2026, 1, 11),
                    LocalDate.of(2026, 1, 11)
            );

    ReferentielComptabiliteActivite referentiel =
            TestReferentielFactory.referentielActiviteDetteRepos();

    // Créneau en dehors de l'horizon (dimanche précédent)
    Creneau creneauHorsHorizon = new Creneau(
            "C1",
            LocalDate.of(2026, 1, 4), // hors [11..11]
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
    creneauHorsHorizon.setRessourceAffectee(salarie);

    PlanningProblem problem = new PlanningProblem(
            context,
            referentiel,
            List.of(salarie),
            List.of(creneauHorsHorizon)
    );

    WorkMetricsCalculator calculator = new WorkMetricsCalculator();

    // --------------------------------------------------
    // WHEN
    // --------------------------------------------------

    Map<?, WorkMetrics> result =
            calculator.compute(problem);

    // --------------------------------------------------
    // THEN
    // --------------------------------------------------

    WorkMetrics wm = result.get(salarie);

    assertNotNull(wm, "Les WorkMetrics du salarié doivent exister");

    assertEquals(
            0,
            wm.getNbDimanchesTravailles(),
            "Un créneau hors horizon ne doit pas compter comme dimanche travaillé"
    );

    assertEquals(
            0,
            wm.getNbCreneauxReposHebdoDetteRepos(),
            "Un créneau hors horizon ne doit générer aucune dette de repos"
    );
}

}
