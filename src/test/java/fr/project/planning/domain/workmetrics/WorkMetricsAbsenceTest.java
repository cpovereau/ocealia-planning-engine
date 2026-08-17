package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.ContratSalarie;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningContextFactory;
import fr.project.planning.fixtures.TestReferentielFactory;
import fr.project.planning.fixtures.TestRegulatoryParametersFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WorkMetricsAbsenceTest — rang 14, bout en bout sur la mesure.
 *
 * <h3>Le défaut corrigé</h3>
 * <p>Le moteur ne plaçait déjà aucun créneau dans un congé. Mais il rapportait ce que chacun fait à
 * ce qu'il doit <strong>sur l'horizon entier</strong> : une absence se lisait comme du temps
 * disponible non travaillé, le salarié revenant de congé apparaissait sous son contrat, donc
 * préférable, et le moteur lui rattrapait son absence de part et d'autre.</p>
 *
 * <p><strong>On n'optimise pas un planning en annulant les congés</strong>, et compenser une
 * absence est une manière de l'annuler.</p>
 *
 * <h3>Le montage</h3>
 * <p>Fenêtre de deux semaines, contrat de 35 h. Le salarié absent la seconde semaine travaille
 * 35 h dans la première : il fait donc <em>exactement</em> son contrat sur le temps où il était
 * là. Sans la déduction il serait lu à −50 %.</p>
 */
class WorkMetricsAbsenceTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final LocalDate FIN = LUNDI.plusDays(13);
    private static final double HEURES_HEBDO = 35.0;

    /** 7 h par jour ouvré, cinq jours : 35 h dans la première semaine. */
    private static final LocalTime DEBUT = LocalTime.of(9, 0);
    private static final LocalTime FIN_JOURNEE = LocalTime.of(16, 0);

    @Test
    @DisplayName("Le salarié absent la moitié de la fenêtre n'observe que la moitié des jours")
    void lesJoursObservesSontPropresAChacun() {
        Map<String, WorkMetrics> metriques = calculer(true);

        assertEquals(7, metriques.get("ABSENT").getJoursObserves(),
                "Sept jours d'absence déclarée retirent sept jours observés.");
        assertEquals(14, metriques.get("PRESENT").getJoursObserves(),
                "Le salarié présent garde la fenêtre entière : la mesure est propre à chacun.");
    }

    @Test
    @DisplayName("Faire son contrat sur le temps où l'on était là, c'est être à zéro — pas à −50 %")
    void faireSonContratSurLeTempsOuLOnEtaitLaVautZero() {
        Map<String, WorkMetrics> metriques = calculer(true);

        Double ecartAbsent = metriques.get("ABSENT").getEcartContratPourcent();
        assertNotNull(ecartAbsent);
        assertEquals(0.0, ecartAbsent, 0.01,
                "Le congé est un fait, pas un déficit : sans la déduction, cet écart vaudrait −50 %.");
    }

    @Test
    @DisplayName("À travail identique, l'absent n'est pas lu plus sous-chargé que le présent")
    void aTravailIdentiqueLAbsentNEstPasLuPlusSousCharge() {
        Map<String, WorkMetrics> metriques = calculer(true);

        Double ecartAbsent = metriques.get("ABSENT").getEcartContratPourcent();
        Double ecartPresent = metriques.get("PRESENT").getEcartContratPourcent();

        assertNotNull(ecartAbsent);
        assertNotNull(ecartPresent);
        assertTrue(ecartAbsent > ecartPresent,
                "Les deux font le même travail ; seul le présent avait deux semaines pour le faire. "
                        + "C'est lui, et lui seul, que l'équité doit désigner. Mesuré : absent="
                        + ecartAbsent + ", présent=" + ecartPresent);
    }

    @Test
    @DisplayName("Absent toute la fenêtre : hors de comparaison, et non à −100 %")
    void absentToutLaFenetreEstHorsDeComparaison() {
        Map<String, WorkMetrics> metriques = calculerAvecAbsenceTotale();

        WorkMetrics absent = metriques.get("ABSENT");
        assertEquals(0, absent.getJoursObserves());
        assertNull(absent.getEcartContratPourcent(),
                "Rien n'est comparable pour qui n'a pas eu un jour pour travailler. Le désigner "
                        + "comme le plus sous-chargé de tous serait lui faire rattraper son absence.");
    }

    @Test
    @DisplayName("Un poste virtuel garde la largeur brute de la fenêtre")
    void unPosteVirtuelGardeLaLargeurBrute() {
        Map<String, WorkMetrics> metriques = calculer(true);

        assertEquals(14, metriques.get("PV").getJoursObserves(),
                "Il ne porte pas de contrat : la fenêtre ne dit chez lui que ce qui a été vu.");
    }

    @Test
    @DisplayName("Sans indisponibilité déclarée, la mesure est celle d'avant le correctif")
    void sansIndisponibiliteLaMesureNeBougePas() {
        Map<String, WorkMetrics> metriques = calculer(false);

        assertEquals(14, metriques.get("ABSENT").getJoursObserves());
        assertEquals(14, metriques.get("PRESENT").getJoursObserves());
        assertEquals(metriques.get("ABSENT").getEcartContratPourcent(),
                metriques.get("PRESENT").getEcartContratPourcent(),
                "Même contrat, même travail, aucune absence : rien ne les distingue.");
    }

    // =====================================================================
    // Montage
    // =====================================================================

    /** Deux salariés au même contrat, au même travail ; l'un déclaré absent la seconde semaine. */
    private Map<String, WorkMetrics> calculer(boolean declarerLAbsence) {
        List<Indisponibilite> absences = declarerLAbsence
                ? List.of(new Indisponibilite("ABSENT", LUNDI.plusDays(7), FIN, "CONGE"))
                : List.of();

        return executer(absences);
    }

    private Map<String, WorkMetrics> calculerAvecAbsenceTotale() {
        return executer(List.of(new Indisponibilite("ABSENT", LUNDI, FIN, "MALADIE")));
    }

    private Map<String, WorkMetrics> executer(List<Indisponibilite> absences) {

        SalarieReel absent = salarieAvecContrat("ABSENT");
        SalarieReel present = salarieAvecContrat("PRESENT");
        PosteVirtuel posteVirtuel = TestRessourceFactory.posteVirtuelStandard("PV");

        List<Creneau> creneaux = new ArrayList<>();
        creneaux.addAll(semaineDeTravail("ABSENT", absent));
        creneaux.addAll(semaineDeTravail("PRESENT", present));

        PlanningContext contexte = TestPlanningContextFactory.contexteNeutre(LUNDI, FIN);
        ReferentielComptabiliteActivite referentiel =
                TestReferentielFactory.referentielActiviteSansDetteRepos();

        PlanningProblem problem = new PlanningProblem(
                contexte,
                TestRegulatoryParametersFactory.neutre(),
                referentiel,
                List.of(absent, present, posteVirtuel),
                creneaux);
        problem.setIndisponibilites(absences);

        Map<Ressource, WorkMetrics> brut = new WorkMetricsCalculator().compute(problem);

        Map<String, WorkMetrics> parId = new java.util.HashMap<>();
        for (Map.Entry<Ressource, WorkMetrics> entry : brut.entrySet()) {
            parId.put(entry.getKey().getId(), entry.getValue());
        }
        return parId;
    }

    /** Cinq jours ouvrés de 7 h dans la PREMIÈRE semaine — 35 h, soit le contrat hebdomadaire. */
    private List<Creneau> semaineDeTravail(String prefixe, SalarieReel titulaire) {
        List<Creneau> creneaux = new ArrayList<>();
        for (int jour = 0; jour < 5; jour++) {
            Creneau creneau = new Creneau(
                    prefixe + "-C" + jour,
                    LUNDI.plusDays(jour),
                    DEBUT,
                    FIN_JOURNEE,
                    420,
                    TestRessourceFactory.SITE_CANON,
                    TestReferentielFactory.ID_ACTIVITE_PLANNING_TRAVAIL,
                    "ACTIVITE",
                    TestRessourceFactory.POSTE_COMPTABLE_CANON,
                    PrioriteCreneau.NORMALE,
                    TypeCreneau.IMPOSE,
                    TypePlageHoraire.JOUR,
                    false,
                    QualificationJour.OUVRE);
            creneau.setRessourceAffectee(titulaire);
            creneaux.add(creneau);
        }
        return creneaux;
    }

    private SalarieReel salarieAvecContrat(String id) {
        SalarieReel salarie = TestRessourceFactory.salarieStandard(id);
        salarie.setContrat(new ContratSalarie(null, HEURES_HEBDO, null, null));
        return salarie;
    }
}
