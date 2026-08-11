package fr.project.planning.constraints;

import fr.project.planning.constraints.metier.DetteReposSurReposHebdomadaire;
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
import fr.project.planning.domain.ressource.TypePosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningContextFactory;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.solution.PlanningProblem;
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
 * DetteReposSurReposHebdomadaireConstraintsTest — lot S7.9b
 *
 * <p>La contrainte n'avait aucun test, et ne se déclenchait jamais : elle exigeait qu'un créneau
 * soit lui-même qualifié RH/RHD <em>et</em> que son activité compte dans la charge — deux
 * conditions incompatibles — en lisant de surcroît un champ qu'aucun mapper n'alimente.</p>
 *
 * <p>Elle croise désormais deux objets distincts : le créneau de travail, et le fait
 * {@link ReposHebdomadaire} qui dit quel jour ce salarié-là se repose.</p>
 *
 * <p>Cas couverts :</p>
 * <ol>
 *   <li>Travail un jour de repos, activité générant une dette → pénalité</li>
 *   <li>La pénalité est proportionnelle à la durée</li>
 *   <li>Travail un jour ouvré → rien</li>
 *   <li>Repos d'un autre salarié le même jour → rien</li>
 *   <li>Activité sans dette de repos → rien</li>
 *   <li>Activité hors charge → rien</li>
 *   <li>Aucun repos au calendrier → rien</li>
 *   <li>Repos déduit du repli samedi/dimanche → pénalité, comme un repos déclaré</li>
 *   <li>Repos un mardi, travail ce mardi → pénalité, le repos ne suit pas le week-end</li>
 *   <li>Créneau hors horizon → rien</li>
 *   <li>Créneau confié à un poste virtuel → rien, un poste virtuel n'a pas de repos</li>
 * </ol>
 */
class DetteReposSurReposHebdomadaireConstraintsTest {

    /** Lundi 11 mai 2026. */
    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final LocalDate SAMEDI = LUNDI.plusDays(5);
    private static final LocalDate MARDI = LUNDI.plusDays(1);

    /** {@code PlanningContext.defaultPenalites()} : 5 000 par minute travaillée. */
    private static final int PENALITE = 5_000;

    private static final String ACTIVITE_AVEC_DETTE = "ACT-RECUP";
    private static final String ACTIVITE_SANS_DETTE = "ACT-SOIN";
    private static final String ACTIVITE_HORS_CHARGE = "ACT-FORMATION";

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1-2. Le cas nominal
    // ---------------------------------------------------------

    @Test
    void travailUnJourDeRepos_avecDetteDeRepos_estPenalise() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-DR-01");

        verifier().given(faits(
                        List.of(travail("C-1", SAMEDI, 480, ACTIVITE_AVEC_DETTE, salarie)),
                        List.of(reposDeclare(salarie, SAMEDI))))
                .penalizesBy(PENALITE * 480);
    }

    @Test
    void laPenaliteSuitLaDureeTravaillee() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-DR-02");

        verifier().given(faits(
                        List.of(travail("C-1", SAMEDI, 240, ACTIVITE_AVEC_DETTE, salarie)),
                        List.of(reposDeclare(salarie, SAMEDI))))
                .penalizesBy(PENALITE * 240);
    }

    // ---------------------------------------------------------
    // 3-4. Ce qui ne concerne pas ce salarié-là, ce jour-là
    // ---------------------------------------------------------

    @Test
    void travailUnJourOuvre_nEstPasPenalise() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-DR-03");

        verifier().given(faits(
                        List.of(travail("C-1", MARDI, 480, ACTIVITE_AVEC_DETTE, salarie)),
                        List.of(reposDeclare(salarie, SAMEDI))))
                .penalizesBy(0);
    }

    @Test
    void reposDUnAutreSalarie_neConcernePasCeCreneau() {
        // Le repos est nominatif : celui de l'un n'interdit rien à l'autre.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-DR-04");
        SalarieReel autre = TestPlanningRequestFactory.buildSalarie("SAL-DR-04-BIS");

        verifier().given(faits(
                        List.of(travail("C-1", SAMEDI, 480, ACTIVITE_AVEC_DETTE, salarie)),
                        List.of(reposDeclare(autre, SAMEDI))))
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 5-7. Ce que le référentiel dit de l'activité
    // ---------------------------------------------------------

    @Test
    void activiteSansDetteDeRepos_nEstPasPenalisee() {
        // Travailler son jour de repos n'est pénalisé que si l'activité ouvre une dette
        // de repos compensateur — c'est le référentiel qui le dit.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-DR-05");

        verifier().given(faits(
                        List.of(travail("C-1", SAMEDI, 480, ACTIVITE_SANS_DETTE, salarie)),
                        List.of(reposDeclare(salarie, SAMEDI))))
                .penalizesBy(0);
    }

    @Test
    void activiteHorsCharge_nEstPasPenalisee() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-DR-06");

        verifier().given(faits(
                        List.of(travail("C-1", SAMEDI, 480, ACTIVITE_HORS_CHARGE, salarie)),
                        List.of(reposDeclare(salarie, SAMEDI))))
                .penalizesBy(0);
    }

    @Test
    void calendrierVide_aucuneJourneeNEstUnRepos() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-DR-07");

        verifier().given(faits(
                        List.of(travail("C-1", SAMEDI, 480, ACTIVITE_AVEC_DETTE, salarie)),
                        List.of()))
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 8-9. Déclaré ou déduit, la règle est la même
    // ---------------------------------------------------------

    @Test
    void reposDeduitDuRepliSamediDimanche_estTraiteCommeUnReposDeclare() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-DR-08");

        verifier().given(faits(
                        List.of(travail("C-1", SAMEDI, 480, ACTIVITE_AVEC_DETTE, salarie)),
                        List.of(new ReposHebdomadaire(salarie.getId(), SAMEDI,
                                QualificationJour.RH, false))))
                .penalizesBy(PENALITE * 480);
    }

    @Test
    void reposUnMardi_leTravailDuMardiEstPenalise() {
        // Un repos hebdomadaire ne tombe pas nécessairement le week-end : c'est précisément
        // ce que le marqueur transmis par l'appelant permet d'exprimer.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-DR-09");

        verifier().given(faits(
                        List.of(travail("C-1", MARDI, 480, ACTIVITE_AVEC_DETTE, salarie)),
                        List.of(reposDeclare(salarie, MARDI))))
                .penalizesBy(PENALITE * 480);
    }

    // ---------------------------------------------------------
    // 10-11. Périmètre
    // ---------------------------------------------------------

    @Test
    void creneauHorsHorizon_nEstPasJuge() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-DR-10");
        LocalDate horsHorizon = LUNDI.plusWeeks(4);

        verifier().given(faits(
                        List.of(travail("C-1", horsHorizon, 480, ACTIVITE_AVEC_DETTE, salarie)),
                        List.of(new ReposHebdomadaire(salarie.getId(), horsHorizon,
                                QualificationJour.RH, true))))
                .penalizesBy(0);
    }

    @Test
    void creneauConfieAUnPosteVirtuel_nEstPasJuge() {
        // Un poste virtuel n'est personne : il n'a ni contrat, ni repos.
        PosteVirtuel posteVirtuel = new PosteVirtuel(
                "PV-001", TypePosteVirtuel.POTENTIEL, 1, Set.of(), Set.of(), Set.of());

        verifier().given(faits(
                        List.of(travail("C-1", SAMEDI, 480, ACTIVITE_AVEC_DETTE, posteVirtuel)),
                        List.of(new ReposHebdomadaire("PV-001", SAMEDI, QualificationJour.RH, false))))
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private SingleConstraintVerification<PlanningProblem> verifier() {
        return constraintVerifier.verifyThat(
                (p, factory) -> DetteReposSurReposHebdomadaire.penaliser(factory));
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

    private static ReposHebdomadaire reposDeclare(SalarieReel salarie, LocalDate date) {
        return new ReposHebdomadaire(salarie.getId(), date, QualificationJour.RH, true);
    }

    private static Creneau travail(String id, LocalDate date, int duree,
                                   String codeActivite, Ressource ressource) {
        Creneau c = new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(8, 0).plusMinutes(duree), duree,
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
                        ACTIVITE_HORS_CHARGE, false, true, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD)));
    }
}
