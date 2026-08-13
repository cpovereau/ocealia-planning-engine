package fr.project.planning.constraints;

import fr.project.planning.constraints.metier.DetteReposSurReposHebdomadaire;
import fr.project.planning.constraints.metier.ReposDominicalInviolable;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.repos.ReposHebdomadaire;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.ressource.TypePosteVirtuel;
import fr.project.planning.fixtures.TestPlanningContextFactory;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;
import org.optaplanner.test.api.score.stream.SingleConstraintVerification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ReposDominicalInviolableTest — lot L0 du chantier équité.
 *
 * <h3>Ce que la règle dit</h3>
 * <p>On ne retire pas à un salarié son repos dominical <strong>déclaré</strong>. Interdiction, pas
 * pondération : la loi impose un délai de prévenance rarement compatible avec une réorganisation
 * d'urgence.</p>
 *
 * <h3>Ce que ces tests protègent, et qui n'est pas évident</h3>
 * <ul>
 *   <li><strong>Toute activité compte.</strong> L'ancienne règle ne se déclenchait que si
 *       l'activité portait {@code genereDetteRepos} — pour toutes les autres, faire travailler
 *       quelqu'un son dimanche de repos coûtait zéro.</li>
 *   <li><strong>Un repos seulement déduit n'interdit rien.</strong> Le repli fait de tout dimanche
 *       un RHD ; l'interdiction appliquée là rendrait le dimanche impossible à couvrir pour
 *       quiconque ne transmet pas ses marqueurs.</li>
 *   <li><strong>L'existant épinglé n'est pas imputable au solveur.</strong> Le pénaliser rendrait
 *       le problème insoluble pour une faute qu'il n'a pas commise et ne peut pas défaire.</li>
 * </ul>
 */
class ReposDominicalInviolableTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final LocalDate DIMANCHE = LUNDI.plusDays(6);
    private static final LocalDate SAMEDI = LUNDI.plusDays(5);
    private static final LocalDate MARDI = LUNDI.plusDays(1);

    private static final String ACTIVITE_AVEC_DETTE = "ACT-RECUP";
    private static final String ACTIVITE_SANS_DETTE = "ACT-SOIN";
    private static final String ACTIVITE_HORS_CHARGE = "ACT-FORMATION";

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // L'interdiction
    // ---------------------------------------------------------

    @Test
    @DisplayName("Travail confié le jour du repos dominical déclaré : interdit")
    void travailUnRhdDeclare_estInterdit() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RHD-01");

        verifier().given(faits(
                        List.of(travail("C-1", DIMANCHE, ACTIVITE_SANS_DETTE, salarie)),
                        List.of(rhdDeclare(salarie, DIMANCHE))))
                .penalizesBy(1);
    }

    @Test
    @DisplayName("Toute activité compte — c'est ce que l'ancienne règle laissait passer à zéro")
    void touteActivite_estInterdite() {
        // La règle SOFT ne se déclenchait que sur genereDetteRepos. Une garde ordinaire, une
        // formation : rien ne coûtait quoi que ce soit.
        for (String activite : List.of(ACTIVITE_AVEC_DETTE, ACTIVITE_SANS_DETTE, ACTIVITE_HORS_CHARGE)) {
            SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RHD-" + activite);

            verifier().given(faits(
                            List.of(travail("C-1", DIMANCHE, activite, salarie)),
                            List.of(rhdDeclare(salarie, DIMANCHE))))
                    .penalizesBy(1);
        }
    }

    @Test
    @DisplayName("L'interdiction se compte par créneau confié")
    void deuxCreneauxLeMemeDimanche_comptentDeuxFois() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RHD-02");

        verifier().given(faits(
                        List.of(travail("C-1", DIMANCHE, ACTIVITE_SANS_DETTE, salarie),
                                travail("C-2", DIMANCHE, ACTIVITE_SANS_DETTE, salarie)),
                        List.of(rhdDeclare(salarie, DIMANCHE))))
                .penalizesBy(2);
    }

    // ---------------------------------------------------------
    // Ce que la règle ne touche pas
    // ---------------------------------------------------------

    @Test
    @DisplayName("Un dimanche seulement déduit du repli n'interdit rien")
    void rhdDeduit_nInterditRien() {
        // Sans marqueur transmis, tout dimanche vaut RHD. Interdire là rendrait le dimanche
        // impossible à couvrir pour la plupart des appelants — ce n'est pas l'arbitrage rendu.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RHD-03");

        verifier().given(faits(
                        List.of(travail("C-1", DIMANCHE, ACTIVITE_SANS_DETTE, salarie)),
                        List.of(new ReposHebdomadaire(salarie.getId(), DIMANCHE,
                                QualificationJour.RHD, false))))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Le repos hebdomadaire ordinaire reste pesé, jamais interdit")
    void rhOrdinaire_nEstPasInterdit() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RHD-04");

        verifier().given(faits(
                        List.of(travail("C-1", SAMEDI, ACTIVITE_SANS_DETTE, salarie)),
                        List.of(new ReposHebdomadaire(salarie.getId(), SAMEDI,
                                QualificationJour.RH, true))))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Un créneau épinglé n'est pas jugé : le solveur ne l'a pas décidé")
    void creneauEpingle_nEstPasJuge() {
        // SC-02 et SC-06 épinglent l'essentiel de ce qu'ils reçoivent. Pénaliser l'existant
        // rendrait le problème insoluble pour une faute que le solveur ne peut pas défaire.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RHD-05");
        Creneau existant = travail("C-1", DIMANCHE, ACTIVITE_SANS_DETTE, salarie);
        existant.figerSur(salarie);

        verifier().given(faits(List.of(existant), List.of(rhdDeclare(salarie, DIMANCHE))))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Un segment de pause n'est pas un motif de déplacement")
    void segmentDePause_nEstPasJuge() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RHD-06");
        Creneau pause = travail("C-1", DIMANCHE, ACTIVITE_SANS_DETTE, salarie);
        pause.setEstSegmentDePause(true);

        verifier().given(faits(List.of(pause), List.of(rhdDeclare(salarie, DIMANCHE))))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Un poste virtuel n'a pas de repos")
    void posteVirtuel_nEstPasJuge() {
        PosteVirtuel posteVirtuel = new PosteVirtuel(
                "PV-001", TypePosteVirtuel.POTENTIEL, 1, Set.of(), Set.of(), Set.of());

        verifier().given(faits(
                        List.of(travail("C-1", DIMANCHE, ACTIVITE_SANS_DETTE, posteVirtuel)),
                        List.of(new ReposHebdomadaire("PV-001", DIMANCHE,
                                QualificationJour.RHD, true))))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Le repos est nominatif : celui de l'un n'interdit rien à l'autre")
    void reposDUnAutreSalarie_nInterditRien() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RHD-07");
        SalarieReel autre = TestPlanningRequestFactory.buildSalarie("SAL-RHD-07-BIS");

        verifier().given(faits(
                        List.of(travail("C-1", DIMANCHE, ACTIVITE_SANS_DETTE, salarie)),
                        List.of(rhdDeclare(autre, DIMANCHE))))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Un autre jour que le repos n'est pas concerné")
    void unAutreJour_nEstPasConcerne() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RHD-08");

        verifier().given(faits(
                        List.of(travail("C-1", MARDI, ACTIVITE_SANS_DETTE, salarie)),
                        List.of(rhdDeclare(salarie, DIMANCHE))))
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Le partage avec la règle SOFT
    // ---------------------------------------------------------

    @Test
    @DisplayName("Un RHD déclaré n'est plus pesé en SOFT : interdit une fois, pas compté deux")
    void rhdDeclare_nEstPlusPeseEnSoft() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RHD-09");

        constraintVerifier
                .verifyThat((p, factory) -> DetteReposSurReposHebdomadaire.penaliser(factory))
                .given(faits(
                        List.of(travail("C-1", DIMANCHE, ACTIVITE_AVEC_DETTE, salarie)),
                        List.of(rhdDeclare(salarie, DIMANCHE))))
                .penalizesBy(0);
    }

    @Test
    @DisplayName("Un dimanche déduit reste pesé en SOFT — sa seule règle")
    void rhdDeduit_resteePeseEnSoft() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RHD-10");

        constraintVerifier
                .verifyThat((p, factory) -> DetteReposSurReposHebdomadaire.penaliser(factory))
                .given(faits(
                        List.of(travail("C-1", DIMANCHE, ACTIVITE_AVEC_DETTE, salarie)),
                        List.of(new ReposHebdomadaire(salarie.getId(), DIMANCHE,
                                QualificationJour.RHD, false))))
                .penalizesBy(5_000 * 480);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private SingleConstraintVerification<PlanningProblem> verifier() {
        return constraintVerifier.verifyThat(
                (p, factory) -> ReposDominicalInviolable.reposDominicalInviolable(factory));
    }

    private static Object[] faits(List<Creneau> creneaux, List<ReposHebdomadaire> repos) {
        List<Object> faits = new ArrayList<>();
        faits.add(contexte());
        faits.add(referentiel());
        faits.addAll(creneaux);
        faits.addAll(repos);
        return faits.toArray();
    }

    private static PlanningContext contexte() {
        return TestPlanningContextFactory.contexteNeutre(LUNDI, LUNDI.plusDays(13));
    }

    private static ReposHebdomadaire rhdDeclare(SalarieReel salarie, LocalDate date) {
        return new ReposHebdomadaire(salarie.getId(), date, QualificationJour.RHD, true);
    }

    private static Creneau travail(String id, LocalDate date, String codeActivite,
                                   Ressource ressource) {
        Creneau c = new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                "SITE-A", codeActivite, null, "PC-001",
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE);
        c.setRessourceAffectee(ressource);
        return c;
    }

    private static ReferentielComptabiliteActivite referentiel() {
        return new ReferentielComptabiliteActivite(Map.of(
                ACTIVITE_AVEC_DETTE, new ComptabiliteActivite(
                        ACTIVITE_AVEC_DETTE, true, true, false, false,
                        ComptabiliteActivite.TypeImpactActivite.DETTE_REPOS),
                ACTIVITE_SANS_DETTE, new ComptabiliteActivite(
                        ACTIVITE_SANS_DETTE, true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD),
                ACTIVITE_HORS_CHARGE, new ComptabiliteActivite(
                        ACTIVITE_HORS_CHARGE, false, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD)));
    }
}
