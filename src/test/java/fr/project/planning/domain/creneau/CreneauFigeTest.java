package fr.project.planning.domain.creneau;

import fr.project.planning.domain.contexte.HypotheseHistorique;
import fr.project.planning.domain.contexte.ObjectifResolution;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.ResolutionType;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestRegulatoryParametersFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.scoring.StrategieScoring;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CreneauFigeTest — lot S1
 *
 * Valide le figement d'un créneau : invariant du domaine, puis comportement réel du solveur.
 *
 * Le test décisif est {@link #creneauFige_solveurNeLeDeplacePas()}, encadré par un témoin
 * {@link #creneauNonFige_solveurLeDeplace()} qui prouve que le solveur <em>voudrait</em>
 * déplacer ce créneau. Sans ce témoin, le premier test passerait même si {@code @PlanningPin}
 * n'était pas honoré.
 */
class CreneauFigeTest {

    private static final String ACTIVITE = "ACTIVITE";
    private static final LocalDate JOUR = LocalDate.of(2026, 5, 11);

    // =========================================================
    // Invariant du domaine
    // =========================================================

    @Test
    void creneauNeuf_nEstPasFige() {
        Creneau creneau = creneau("C-FIG-01");

        assertFalse(creneau.isFige(), "Le figement doit rester opt-in : SC-01 et SC-03 n'en veulent pas.");
        assertNull(creneau.getRessourceAffectee());
    }

    @Test
    void figerSur_affecteEtFigeSimultanement() {
        Creneau creneau = creneau("C-FIG-02");
        SalarieReel salarie = TestRessourceFactory.salarieStandard("S1");

        creneau.figerSur(salarie);

        assertTrue(creneau.isFige());
        assertSame(salarie, creneau.getRessourceAffectee());
    }

    @Test
    void figerSurNull_estRefuse() {
        // Un créneau figé sans ressource laisserait la solution non initialisée à jamais :
        // le solveur n'aurait pas le droit de lui en attribuer une.
        Creneau creneau = creneau("C-FIG-03");

        assertThrows(NullPointerException.class, () -> creneau.figerSur(null));
    }

    @Test
    void setRessourceAffectee_neFigePas() {
        // Affecter n'est pas figer : le solveur reste libre de revenir sur cette affectation.
        Creneau creneau = creneau("C-FIG-04");
        SalarieReel salarie = TestRessourceFactory.salarieStandard("S1");

        creneau.setRessourceAffectee(salarie);

        assertSame(salarie, creneau.getRessourceAffectee());
        assertFalse(creneau.isFige());
    }

    // =========================================================
    // Comportement du solveur
    // =========================================================

    @Test
    void creneauNonFige_solveurLeDeplace() {
        // Témoin. Un créneau libre doit atterrir sur le salarié réel : l'affectation à un poste
        // virtuel est pénalisée (500) et la non-affectation davantage (2000).
        SalarieReel salarie = TestRessourceFactory.salarieStandard("S1");
        PosteVirtuel posteVirtuel = TestRessourceFactory.posteVirtuelStandard("PV1");
        List<Ressource> ressources = List.of(salarie, posteVirtuel, RessourceNonAffectee.INSTANCE);

        Creneau libre = creneau("C-FIG-10");
        libre.setRessourceAffectee(posteVirtuel);   // affecté, mais pas figé

        PlanningProblem resolu = solve(ressources, List.of(libre));

        assertSame(salarie, resolu.getCreneaux().get(0).getRessourceAffectee(),
                "Le solveur doit préférer le salarié réel — sans quoi le test de figement ne prouve rien.");
    }

    @Test
    void creneauFige_solveurNeLeDeplacePas() {
        // Même situation, mais le créneau est figé sur le poste virtuel.
        // Le solveur préférerait le salarié réel ; il n'a pas le droit d'y toucher.
        SalarieReel salarie = TestRessourceFactory.salarieStandard("S1");
        PosteVirtuel posteVirtuel = TestRessourceFactory.posteVirtuelStandard("PV1");
        List<Ressource> ressources = List.of(salarie, posteVirtuel, RessourceNonAffectee.INSTANCE);

        Creneau fige = creneau("C-FIG-11");
        fige.figerSur(posteVirtuel);

        PlanningProblem resolu = solve(ressources, List.of(fige));

        Creneau resultat = resolu.getCreneaux().get(0);
        assertTrue(resultat.isFige());
        assertSame(posteVirtuel, resultat.getRessourceAffectee(),
                "Un créneau figé est un fait d'entrée : le solveur ne peut pas le réaffecter.");
    }

    @Test
    void creneauFige_nEmpechePasLaDecisionSurLesAutres() {
        // Le planning figé doit rester visible sans bloquer le reste :
        // le créneau libre est décidé, le créneau figé ne bouge pas.
        SalarieReel salarie = TestRessourceFactory.salarieStandard("S1");
        PosteVirtuel posteVirtuel = TestRessourceFactory.posteVirtuelStandard("PV1");
        List<Ressource> ressources = List.of(salarie, posteVirtuel, RessourceNonAffectee.INSTANCE);

        Creneau existant = creneau("C-FIG-20");
        existant.figerSur(posteVirtuel);

        Creneau aDecider = creneau("C-FIG-21", LocalTime.of(18, 0), LocalTime.of(22, 0), 240);

        PlanningProblem resolu = solve(ressources, List.of(existant, aDecider));

        Creneau resultatExistant = resolu.getCreneaux().stream()
                .filter(c -> "C-FIG-20".equals(c.getId())).findFirst().orElseThrow();
        Creneau resultatADecider = resolu.getCreneaux().stream()
                .filter(c -> "C-FIG-21".equals(c.getId())).findFirst().orElseThrow();

        assertSame(posteVirtuel, resultatExistant.getRessourceAffectee(), "Le figé ne bouge pas.");
        assertSame(salarie, resultatADecider.getRessourceAffectee(), "Le libre est bien décidé.");
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static Creneau creneau(String id) {
        return creneau(id, LocalTime.of(9, 0), LocalTime.of(17, 0), 480);
    }

    private static Creneau creneau(String id, LocalTime debut, LocalTime fin, int duree) {
        return new Creneau(
                id, JOUR, debut, fin, duree,
                TestRessourceFactory.SITE_CANON, null, ACTIVITE,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
    }

    private static PlanningProblem solve(List<Ressource> ressources, List<Creneau> creneaux) {
        PlanningContext context = new PlanningContext(
                ObjectifResolution.COUVRIR_A_TOUT_PRIX,
                StrategieScoring.EXPLOITATION,
                JOUR,
                JOUR,
                ResolutionType.PLANNING_GLOBAL,
                HypotheseHistorique.NEUTRE
        );

        ReferentielComptabiliteActivite referentiel = new ReferentielComptabiliteActivite(Map.of(
                ACTIVITE,
                new ComptabiliteActivite(ACTIVITE, true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD)
        ));

        PlanningProblem problem = new PlanningProblem(
                context,
                TestRegulatoryParametersFactory.neutre(),
                referentiel,
                ressources,
                creneaux
        );

        SolverConfig solverConfig = SolverConfig.createFromXmlInputStream(
                Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("solverConfig-test.xml"));
        Solver<PlanningProblem> solver = SolverFactory.<PlanningProblem>create(solverConfig).buildSolver();
        return solver.solve(problem);
    }
}
