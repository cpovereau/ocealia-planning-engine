package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.ReposQuotidienMinimum;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * ReposQuotidienMinimumConstraintsTest — lot S3
 *
 * Vérifie la contrainte SOFT ReposQuotidienMinimum : pénalise un repos insuffisant
 * entre deux journées travaillées successives.
 *
 * Cas couverts :
 *  1. reposQuotidienMinimum null → contrainte inactive
 *  2. contraintesReglementaires null → contrainte inactive
 *  3. repos exactement égal au seuil → 0 pénalité
 *  4. repos supérieur au seuil → 0 pénalité
 *  5. repos inférieur au seuil → pénalité × minutes manquantes
 *  6. créneau de nuit à cheval sur minuit → repos mesuré depuis la fin réelle
 *  7. jour de repos intercalé → aucune pénalité (repos long)
 *  8. déficits cumulés sur plusieurs transitions
 *  9. plusieurs créneaux le même jour → seules les bornes de la journée comptent
 * 10. segment de pause exclu du calcul des bornes
 * 11. activité hors charge exclue
 * 12. chevauchement → repos négatif, déficit supérieur au seuil
 * 13. non-régression SC-01 : salarié sans seuil configuré → 0 pénalité
 */
class ReposQuotidienMinimumConstraintsTest {

    /** Valeur de PENALITE_REPOS_QUOTIDIEN dans ReposQuotidienMinimum. */
    private static final int PENALITE_BASE = 100;

    private static final LocalDate LUNDI    = LocalDate.of(2026, 5, 11);
    private static final LocalDate MARDI    = LocalDate.of(2026, 5, 12);
    private static final LocalDate MERCREDI = LocalDate.of(2026, 5, 13);

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1-2. Activation
    // ---------------------------------------------------------

    @Test
    void reposQuotidienMinimum_null_contrainteInactive() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RQ-01");
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null,
                null,  // reposQuotidienMinimum = null → contrainte inactive
                null, null, null, null
        ));

        // 1h de repos seulement : violerait tout seuil réaliste
        Creneau lundi = travail("C-RQ-01A", LUNDI, LocalTime.of(8, 0), LocalTime.of(23, 0), 900, salarie);
        Creneau mardi = travail("C-RQ-01B", MARDI, LocalTime.of(0, 0), LocalTime.of(8, 0), 480, salarie);

        verifier().given(salarie, lundi, mardi, referentielAvecCharge()).penalizesBy(0);
    }

    @Test
    void contraintesReglementaires_null_contrainteInactive() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-RQ-02");
        // contraintesReglementaires = null par défaut

        Creneau lundi = travail("C-RQ-02A", LUNDI, LocalTime.of(8, 0), LocalTime.of(23, 0), 900, salarie);
        Creneau mardi = travail("C-RQ-02B", MARDI, LocalTime.of(0, 0), LocalTime.of(8, 0), 480, salarie);

        verifier().given(salarie, lundi, mardi, referentielAvecCharge()).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 3-5. Comparaison au seuil
    // ---------------------------------------------------------

    @Test
    void reposEgalAuSeuil_pasDePenalite() {
        // Seuil 11h — fin lundi 20:00, début mardi 07:00 → repos = 11h exactement
        SalarieReel salarie = salarieAvecRepos("SAL-RQ-03", 11.0);

        Creneau lundi = travail("C-RQ-03A", LUNDI, LocalTime.of(12, 0), LocalTime.of(20, 0), 480, salarie);
        Creneau mardi = travail("C-RQ-03B", MARDI, LocalTime.of(7, 0), LocalTime.of(15, 0), 480, salarie);

        verifier().given(salarie, lundi, mardi, referentielAvecCharge()).penalizesBy(0);
    }

    @Test
    void reposSuperieurAuSeuil_pasDePenalite() {
        // Seuil 11h — fin lundi 15:00, début mardi 07:00 → repos = 16h
        SalarieReel salarie = salarieAvecRepos("SAL-RQ-04", 11.0);

        Creneau lundi = travail("C-RQ-04A", LUNDI, LocalTime.of(7, 0), LocalTime.of(15, 0), 480, salarie);
        Creneau mardi = travail("C-RQ-04B", MARDI, LocalTime.of(7, 0), LocalTime.of(15, 0), 480, salarie);

        verifier().given(salarie, lundi, mardi, referentielAvecCharge()).penalizesBy(0);
    }

    @Test
    void reposInferieurAuSeuil_penaliteProportionnelleAuDeficit() {
        // Seuil 11h — fin lundi 22:00, début mardi 07:00 → repos = 9h, déficit = 120 min
        SalarieReel salarie = salarieAvecRepos("SAL-RQ-05", 11.0);

        Creneau lundi = travail("C-RQ-05A", LUNDI, LocalTime.of(14, 0), LocalTime.of(22, 0), 480, salarie);
        Creneau mardi = travail("C-RQ-05B", MARDI, LocalTime.of(7, 0), LocalTime.of(15, 0), 480, salarie);

        verifier().given(salarie, lundi, mardi, referentielAvecCharge())
                .penalizesBy(PENALITE_BASE * 120);
    }

    // ---------------------------------------------------------
    // 6. Créneau à cheval sur minuit
    // ---------------------------------------------------------

    @Test
    void creneauACheValSurMinuit_reposMesureDepuisLaFinReelle() {
        // Nuit du lundi : 22:00 → 06:00 le mardi. Fin réelle = mardi 06:00.
        // Reprise mardi 14:00 → repos = 8h, seuil 11h, déficit = 180 min.
        // Une lecture naïve (fin = lundi 06:00) donnerait 32h de repos et masquerait la violation.
        SalarieReel salarie = salarieAvecRepos("SAL-RQ-06", 11.0);

        Creneau nuit    = travail("C-RQ-06A", LUNDI, LocalTime.of(22, 0), LocalTime.of(6, 0), 480, salarie);
        Creneau reprise = travail("C-RQ-06B", MARDI, LocalTime.of(14, 0), LocalTime.of(22, 0), 480, salarie);

        verifier().given(salarie, nuit, reprise, referentielAvecCharge())
                .penalizesBy(PENALITE_BASE * 180);
    }

    // ---------------------------------------------------------
    // 7. Jour de repos intercalé
    // ---------------------------------------------------------

    @Test
    void jourDeReposIntercale_pasDePenalite() {
        // Lundi puis mercredi : le mardi n'est pas travaillé, le repos dépasse 24h.
        SalarieReel salarie = salarieAvecRepos("SAL-RQ-07", 11.0);

        Creneau lundi    = travail("C-RQ-07A", LUNDI,    LocalTime.of(14, 0), LocalTime.of(22, 0), 480, salarie);
        Creneau mercredi = travail("C-RQ-07B", MERCREDI, LocalTime.of(7, 0),  LocalTime.of(15, 0), 480, salarie);

        verifier().given(salarie, lundi, mercredi, referentielAvecCharge()).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 8. Déficits cumulés
    // ---------------------------------------------------------

    @Test
    void deficitsSurPlusieursTransitions_cumules() {
        // Seuil 11h.
        // Lundi 14:00-22:00 → mardi 07:00 : repos 9h, déficit 120 min
        // Mardi 07:00-15:00 → mercredi 01:00 : repos 10h, déficit 60 min
        // Total attendu : 180 min
        SalarieReel salarie = salarieAvecRepos("SAL-RQ-08", 11.0);

        Creneau lundi    = travail("C-RQ-08A", LUNDI,    LocalTime.of(14, 0), LocalTime.of(22, 0), 480, salarie);
        Creneau mardi    = travail("C-RQ-08B", MARDI,    LocalTime.of(7, 0),  LocalTime.of(15, 0), 480, salarie);
        Creneau mercredi = travail("C-RQ-08C", MERCREDI, LocalTime.of(1, 0),  LocalTime.of(9, 0),  480, salarie);

        verifier().given(salarie, lundi, mardi, mercredi, referentielAvecCharge())
                .penalizesBy(PENALITE_BASE * 180);
    }

    // ---------------------------------------------------------
    // 9. Plusieurs créneaux le même jour
    // ---------------------------------------------------------

    @Test
    void plusieursCreneauxLeMemeJour_seulesLesBornesComptent() {
        // Lundi : 08:00-12:00 puis 18:00-22:00 → borne de fin = 22:00
        // Mardi : 07:00-11:00 puis 14:00-18:00 → borne de début = 07:00
        // Repos = 9h, seuil 11h → déficit 120 min, compté une seule fois.
        SalarieReel salarie = salarieAvecRepos("SAL-RQ-09", 11.0);

        Creneau lundiMatin = travail("C-RQ-09A", LUNDI, LocalTime.of(8, 0),  LocalTime.of(12, 0), 240, salarie);
        Creneau lundiSoir  = travail("C-RQ-09B", LUNDI, LocalTime.of(18, 0), LocalTime.of(22, 0), 240, salarie);
        Creneau mardiMatin = travail("C-RQ-09C", MARDI, LocalTime.of(7, 0),  LocalTime.of(11, 0), 240, salarie);
        Creneau mardiSoir  = travail("C-RQ-09D", MARDI, LocalTime.of(14, 0), LocalTime.of(18, 0), 240, salarie);

        verifier().given(salarie, lundiMatin, lundiSoir, mardiMatin, mardiSoir, referentielAvecCharge())
                .penalizesBy(PENALITE_BASE * 120);
    }

    // ---------------------------------------------------------
    // 10-11. Périmètre du calcul
    // ---------------------------------------------------------

    @Test
    void segmentDePause_exclutDuCalculDesBornes() {
        // La pause de 22:00 à 23:00 le lundi ne doit pas repousser la fin de journée.
        // Sans exclusion : fin = 23:00 → repos 8h → déficit 180 min (INCORRECT)
        // Avec exclusion  : fin = 22:00 → repos 9h → déficit 120 min
        SalarieReel salarie = salarieAvecRepos("SAL-RQ-10", 11.0);

        Creneau lundi = travail("C-RQ-10A", LUNDI, LocalTime.of(14, 0), LocalTime.of(22, 0), 480, salarie);
        Creneau pause = pause("C-RQ-10P", LUNDI, LocalTime.of(22, 0), LocalTime.of(23, 0), 60, salarie);
        Creneau mardi = travail("C-RQ-10B", MARDI, LocalTime.of(7, 0), LocalTime.of(15, 0), 480, salarie);

        verifier().given(salarie, lundi, pause, mardi, referentielAvecCharge())
                .penalizesBy(PENALITE_BASE * 120);
    }

    @Test
    void activiteHorsCharge_exclueDuCalculDesBornes() {
        // Même géométrie que le cas précédent, mais le créneau tardif porte une activité
        // dont compteDansCharge = false : il ne doit pas non plus repousser la fin de journée.
        SalarieReel salarie = salarieAvecRepos("SAL-RQ-11", 11.0);

        Creneau lundi     = travail("C-RQ-11A", LUNDI, LocalTime.of(14, 0), LocalTime.of(22, 0), 480, salarie);
        Creneau formation = creneau("C-RQ-11F", LUNDI, LocalTime.of(22, 0), LocalTime.of(23, 0), 60,
                ACTIVITE_HORS_CHARGE, salarie);
        Creneau mardi     = travail("C-RQ-11B", MARDI, LocalTime.of(7, 0), LocalTime.of(15, 0), 480, salarie);

        verifier().given(salarie, lundi, formation, mardi, referentielMixte())
                .penalizesBy(PENALITE_BASE * 120);
    }

    // ---------------------------------------------------------
    // 12. Chevauchement — repos négatif
    // ---------------------------------------------------------

    @Test
    void chevauchement_reposNegatif_deficitSuperieurAuSeuil() {
        // Lundi 14:00 → 02:00 (mardi), reprise mardi 01:00 : le repos vaut -60 min.
        // Déficit = 660 - (-60) = 720 min. Le chevauchement reste sanctionné en HARD ailleurs.
        SalarieReel salarie = salarieAvecRepos("SAL-RQ-12", 11.0);

        Creneau lundi = travail("C-RQ-12A", LUNDI, LocalTime.of(14, 0), LocalTime.of(2, 0), 720, salarie);
        Creneau mardi = travail("C-RQ-12B", MARDI, LocalTime.of(1, 0), LocalTime.of(9, 0), 480, salarie);

        verifier().given(salarie, lundi, mardi, referentielAvecCharge())
                .penalizesBy(PENALITE_BASE * 720);
    }

    // ---------------------------------------------------------
    // 13. Non-régression SC-01
    // ---------------------------------------------------------

    @Test
    void sc01_salarieStandard_pasDePenalite() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(TestPlanningRequestFactory.ID_SALARIE_1041);

        Creneau lundi = travail("C-SC01-RQ-01", TestPlanningRequestFactory.SC01_DATE_DEBUT,
                LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        verifier().given(salarie, lundi, TestPlanningRequestFactory.buildReferentielSc01()).penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 14. Une seule journée travaillée — aucune transition à mesurer
    // ---------------------------------------------------------

    @Test
    void uneSeuleJourneeTravaillee_pasDePenalite() {
        SalarieReel salarie = salarieAvecRepos("SAL-RQ-13", 11.0);
        Creneau seul = travail("C-RQ-13A", LUNDI, LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        verifier().given(salarie, seul, referentielAvecCharge()).penalizesBy(0);
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static final String ACTIVITE_HORS_CHARGE = "formation";

    private org.optaplanner.test.api.score.stream.SingleConstraintVerification<PlanningProblem> verifier() {
        return constraintVerifier.verifyThat(
                (provider, factory) -> ReposQuotidienMinimum.reposQuotidienMinimum(factory));
    }

    private static SalarieReel salarieAvecRepos(String id, double reposHeures) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, reposHeures, null, null, null, null
        ));
        return salarie;
    }

    private static ReferentielComptabiliteActivite referentielAvecCharge() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
                )
        ));
    }

    private static ReferentielComptabiliteActivite referentielMixte() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
                ),
                ACTIVITE_HORS_CHARGE,
                new ComptabiliteActivite(
                        ACTIVITE_HORS_CHARGE,
                        false, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
                )
        ));
    }

    private static Creneau travail(String id, LocalDate date, LocalTime debut, LocalTime fin,
                                   int duree, SalarieReel salarie) {
        return creneau(id, date, debut, fin, duree, TestPlanningRequestFactory.ACTIVITE_TRAVAIL, salarie);
    }

    private static Creneau creneau(String id, LocalDate date, LocalTime debut, LocalTime fin,
                                   int duree, String activite, SalarieReel salarie) {
        Creneau c = new Creneau(
                id, date, debut, fin, duree,
                TestRessourceFactory.SITE_CANON, null,
                activite,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(salarie);
        return c;
    }

    private static Creneau pause(String id, LocalDate date, LocalTime debut, LocalTime fin,
                                 int duree, SalarieReel salarie) {
        Creneau c = travail(id, date, debut, fin, duree, salarie);
        c.setEstSegmentDePause(true);
        return c;
    }
}
