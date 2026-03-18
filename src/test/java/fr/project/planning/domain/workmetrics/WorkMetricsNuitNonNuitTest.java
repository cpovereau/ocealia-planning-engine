package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningContextFactory;
import fr.project.planning.fixtures.TestReferentielFactory;
import fr.project.planning.fixtures.TestRegulatoryParametersFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkMetricsNuitNonNuitTest — Phase 8.
 *
 * Vérifie le calcul de nbCreneauxNuitNonNuit dans WorkMetrics :
 * nombre de créneaux de nuit affectés à un salarié non déclaré travailleur de nuit.
 *
 * Champ source : travailDeNuit (SalarieReel.estTravailleurDeNuit() Phase 6).
 */
class WorkMetricsNuitNonNuitTest {

    private static final LocalDate DATE = LocalDate.of(2026, 5, 12);
    private static final String CODE_ACTIVITE = TestReferentielFactory.ID_ACTIVITE_PLANNING_TRAVAIL;

    // ----------------------------------------------------------
    // 1. Salarié non-nuit sur créneau de nuit → nbCreneauxNuitNonNuit = 1
    // ----------------------------------------------------------

    @Test
    void salarie_nonNuit_creneauNuit_doitCompterUnNuitNonNuit() {
        SalarieReel salarie = TestRessourceFactory.salarieStandard("SAL-NON-NUIT");
        salarie.setTravailDeNuit(null);

        Creneau creneauNuit = creneauNuit("C-NUIT-1", DATE, salarie);

        WorkMetrics wm = compute(salarie, List.of(creneauNuit));

        assertEquals(1, wm.getNbCreneauxNuitNonNuit(),
                "Un créneau de nuit sur salarié non-nuit doit comptabiliser 1");
    }

    // ----------------------------------------------------------
    // 2. Salarié travailleur de nuit permanent → nbCreneauxNuitNonNuit = 0
    // ----------------------------------------------------------

    @Test
    void salarie_nuitPermanent_creneauNuit_doitNePasCompter() {
        SalarieReel salarie = TestRessourceFactory.salarieStandard("SAL-NUIT-PERM");
        salarie.setTravailDeNuit("permanent");

        Creneau creneauNuit = creneauNuit("C-NUIT-2", DATE, salarie);

        WorkMetrics wm = compute(salarie, List.of(creneauNuit));

        assertEquals(0, wm.getNbCreneauxNuitNonNuit(),
                "Un salarié permanent de nuit ne doit pas générer d'inadéquation");
    }

    // ----------------------------------------------------------
    // 3. Salarié travailleur de nuit occasionnel → nbCreneauxNuitNonNuit = 0
    // ----------------------------------------------------------

    @Test
    void salarie_nuitOccasionnel_creneauNuit_doitNePasCompter() {
        SalarieReel salarie = TestRessourceFactory.salarieStandard("SAL-NUIT-OCC");
        salarie.setTravailDeNuit("occasionnel");

        Creneau creneauNuit = creneauNuit("C-NUIT-3", DATE, salarie);

        WorkMetrics wm = compute(salarie, List.of(creneauNuit));

        assertEquals(0, wm.getNbCreneauxNuitNonNuit(),
                "Un salarié occasionnel de nuit ne doit pas générer d'inadéquation");
    }

    // ----------------------------------------------------------
    // 4. Salarié non-nuit sur créneau de JOUR → nbCreneauxNuitNonNuit = 0
    // ----------------------------------------------------------

    @Test
    void salarie_nonNuit_creneauJour_doitNePasCompter() {
        SalarieReel salarie = TestRessourceFactory.salarieStandard("SAL-NON-NUIT-2");
        salarie.setTravailDeNuit(null);

        Creneau creneauJour = creneauJour("C-JOUR-1", DATE, salarie);

        WorkMetrics wm = compute(salarie, List.of(creneauJour));

        assertEquals(0, wm.getNbCreneauxNuitNonNuit(),
                "Un créneau de jour ne doit pas comptabiliser d'inadéquation nuit");
    }

    // ----------------------------------------------------------
    // 5. Deux créneaux de nuit sur salarié non-nuit → nbCreneauxNuitNonNuit = 2
    // ----------------------------------------------------------

    @Test
    void salarie_nonNuit_deuxCreneauxNuit_doitCompterDeux() {
        SalarieReel salarie = TestRessourceFactory.salarieStandard("SAL-NON-NUIT-3");
        salarie.setTravailDeNuit(null);

        Creneau nuit1 = creneauNuit("C-NUIT-4A", DATE, salarie);
        Creneau nuit2 = creneauNuit("C-NUIT-4B", DATE.plusDays(1), salarie);

        WorkMetrics wm = compute(salarie, List.of(nuit1, nuit2));

        assertEquals(2, wm.getNbCreneauxNuitNonNuit(),
                "Deux créneaux de nuit sur salarié non-nuit doivent compter 2");
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    private WorkMetrics compute(SalarieReel salarie, List<Creneau> creneaux) {
        ReferentielComptabiliteActivite referentiel =
                TestReferentielFactory.referentielActiviteSansDetteRepos();

        PlanningProblem problem = new PlanningProblem(
                TestPlanningContextFactory.contexteNeutre(DATE, DATE.plusDays(6)),
                TestRegulatoryParametersFactory.neutre(),
                referentiel,
                List.of(salarie),
                creneaux
        );

        Map<?, WorkMetrics> result = new WorkMetricsCalculator().compute(problem);
        WorkMetrics wm = result.get(salarie);
        assertNotNull(wm, "WorkMetrics du salarié doivent exister");
        return wm;
    }

    private Creneau creneauNuit(String id, LocalDate date, SalarieReel salarie) {
        Creneau c = new Creneau(
                id, date,
                LocalTime.of(22, 0), LocalTime.of(6, 0), 480,
                "SITE", CODE_ACTIVITE, "travail", "PC",
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.NUIT,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(salarie);
        return c;
    }

    private Creneau creneauJour(String id, LocalDate date, SalarieReel salarie) {
        Creneau c = new Creneau(
                id, date,
                LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                "SITE", CODE_ACTIVITE, "travail", "PC",
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(salarie);
        return c;
    }
}
