package fr.project.planning.constraints;

import fr.project.planning.constraints.metier.IndisponibiliteSalarie;
import fr.project.planning.constraints.metier.JourFerieRefuse;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


/**
 * Phase4ConstraintsTest
 *
 * Valide les deux contraintes HARD introduites en Phase 4 :
 * - JourFerieRefuse : salarié avec travailleJourFerie = false interdit sur jour férié
 * - IndisponibiliteSalarie : salarié interdit sur la plage de son indisponibilité
 *
 * Utilise ConstraintVerifier pour tester les contraintes en isolation, sans solveur.
 */
class Phase4ConstraintsTest {

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // -------------------------------------------------------
    // JourFerieRefuse
    // -------------------------------------------------------

    @Test
    void travailleJourFerie_false_doitLeverViolationHard() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-TEST");
        salarie.setTravailleJourFerie(false);
        Creneau creneau = creneauFerie("C-FERIE-001", TestPlanningRequestFactory.SC01_JOUR_FERIE, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JourFerieRefuse.jourFerieRefuse(factory))
                .given(creneau, calendrier(TestPlanningRequestFactory.SC01_JOUR_FERIE))
                .penalizesBy(1);
    }

    @Test
    void travailleJourFerie_true_doitNePasLeverViolation() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-TEST");
        salarie.setTravailleJourFerie(true);
        Creneau creneau = creneauFerie("C-FERIE-002", TestPlanningRequestFactory.SC01_JOUR_FERIE, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JourFerieRefuse.jourFerieRefuse(factory))
                .given(creneau, calendrier(TestPlanningRequestFactory.SC01_JOUR_FERIE))
                .penalizesBy(0);
    }

    @Test
    void travailleJourFerie_null_doitNePasLeverViolation() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-TEST");
        // travailleJourFerie non défini (null) : absence d'information ≠ interdiction explicite
        Creneau creneau = creneauFerie("C-FERIE-003", TestPlanningRequestFactory.SC01_JOUR_FERIE, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JourFerieRefuse.jourFerieRefuse(factory))
                .given(creneau, calendrier(TestPlanningRequestFactory.SC01_JOUR_FERIE))
                .penalizesBy(0);
    }

    // -------------------------------------------------------
    // [Lot S8.3] JourFerieRefuse lit le calendrier réglementaire
    //
    // La règle lisait le drapeau isJourFerie du créneau, la valorisation lisait le calendrier
    // de RegulatoryParameters. Le lot S8.0, en ouvrant regulatoryParameters au contrat, avait
    // rendu l'écart atteignable : ces trois tests le ferment.
    // -------------------------------------------------------

    @Test
    void dateFerieeAuCalendrier_sansDrapeauSurLeCreneau_doitLeverViolationHard() {
        // Le trou ouvert par S8.0 : l'appelant déclare son calendrier de fériés et ne marque pas
        // ses créneaux. Les minutes étaient valorisées comme fériées, mais un salarié refusant
        // le travail férié pouvait y être affecté sans le moindre point HARD.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-TEST");
        salarie.setTravailleJourFerie(false);
        Creneau creneau = creneauNormal("C-FERIE-004", TestPlanningRequestFactory.SC01_JOUR_FERIE, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JourFerieRefuse.jourFerieRefuse(factory))
                .given(creneau, calendrier(TestPlanningRequestFactory.SC01_JOUR_FERIE))
                .penalizesBy(1);
    }

    @Test
    void drapeauSurLeCreneau_maisDateAbsenteDuCalendrier_doitNePasLeverViolation() {
        // Réciproque : le calendrier fait autorité. Un drapeau isolé que l'arbitrage du mapper
        // n'a pas retenu — divergence déjà tracée en WARN — ne crée plus d'interdiction.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-TEST");
        salarie.setTravailleJourFerie(false);
        Creneau creneau = creneauFerie("C-FERIE-005", TestPlanningRequestFactory.SC01_JOUR_FERIE, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JourFerieRefuse.jourFerieRefuse(factory))
                .given(creneau, calendrier(TestPlanningRequestFactory.SC01_JOUR_FERIE.plusDays(3)))
                .penalizesBy(0);
    }

    @Test
    void creneauDeNuit_dontLeLendemainEstFerie_doitLeverViolationHard() {
        // 22:00–06:00 la veille d'un férié : six heures tombent le jour férié. La valorisation
        // les comptait déjà séparément ; l'interdiction les voit désormais aussi.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-TEST");
        salarie.setTravailleJourFerie(false);
        LocalDate veille = TestPlanningRequestFactory.SC01_JOUR_FERIE.minusDays(1);
        Creneau creneau = creneau("C-FERIE-006", veille, LocalTime.of(22, 0), LocalTime.of(6, 0), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JourFerieRefuse.jourFerieRefuse(factory))
                .given(creneau, calendrier(TestPlanningRequestFactory.SC01_JOUR_FERIE))
                .penalizesBy(1);
    }

    @Test
    void creneauSArretantAMinuit_laVeilleDUnFerie_doitNePasLeverViolation() {
        // Borne du cas précédent : 14:00–00:00 s'arrête à minuit pile. Aucune minute n'est
        // travaillée le jour férié, donc aucune interdiction.
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-TEST");
        salarie.setTravailleJourFerie(false);
        LocalDate veille = TestPlanningRequestFactory.SC01_JOUR_FERIE.minusDays(1);
        Creneau creneau = creneau("C-FERIE-007", veille, LocalTime.of(14, 0), LocalTime.of(0, 0), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JourFerieRefuse.jourFerieRefuse(factory))
                .given(creneau, calendrier(TestPlanningRequestFactory.SC01_JOUR_FERIE))
                .penalizesBy(0);
    }

    // -------------------------------------------------------
    // IndisponibiliteSalarie
    // -------------------------------------------------------

    @Test
    void indisponibilite_doitLeverViolationHard() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-INDISPO");
        LocalDate date = LocalDate.of(2026, 5, 11);
        Creneau creneau = creneauNormal("C-INDISPO-001", date, salarie);
        Indisponibilite indispo = new Indisponibilite("SAL-INDISPO", date, date, "CONGE_POSE");

        constraintVerifier
                .verifyThat((provider, factory) -> IndisponibiliteSalarie.indisponibiliteSalarie(factory))
                .given(creneau, indispo)
                .penalizesBy(1);
    }

    @Test
    void indisponibilite_horsDate_doitNePasLeverViolation() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-INDISPO");
        LocalDate date = LocalDate.of(2026, 5, 11);
        Creneau creneau = creneauNormal("C-INDISPO-002", date, salarie);
        Indisponibilite indispo = new Indisponibilite("SAL-INDISPO",
                LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 22), "CONGE");

        constraintVerifier
                .verifyThat((provider, factory) -> IndisponibiliteSalarie.indisponibiliteSalarie(factory))
                .given(creneau, indispo)
                .penalizesBy(0);
    }

    @Test
    void indisponibilite_autreSalarie_doitNePasLeverViolation() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-A");
        LocalDate date = LocalDate.of(2026, 5, 11);
        Creneau creneau = creneauNormal("C-INDISPO-003", date, salarie);
        Indisponibilite indispo = new Indisponibilite("SAL-B", date, date, "CONGE");

        constraintVerifier
                .verifyThat((provider, factory) -> IndisponibiliteSalarie.indisponibiliteSalarie(factory))
                .given(creneau, indispo)
                .penalizesBy(0);
    }

    // -------------------------------------------------------
    // IndisponibiliteSalarie et le passage de minuit — lot S0 de SC-02
    // -------------------------------------------------------

    /**
     * Le défaut que ce lot répare : le filtre comparait la seule {@code date} du créneau aux
     * bornes de l'absence. Un créneau de nuit du 3 mars couvre pourtant six heures du 4 mars.
     */
    @Test
    @DisplayName("Un créneau de nuit qui déborde sur le premier jour d'absence est sanctionné")
    void indisponibilite_creneauDeNuitDebordantSurLAbsence_doitLeverViolationHard() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NUIT");
        LocalDate troisMars = LocalDate.of(2026, 3, 3);
        Creneau nuit = creneau("C-NUIT-001", troisMars, LocalTime.of(22, 0), LocalTime.of(6, 0), salarie);
        Indisponibilite indispo = new Indisponibilite("SAL-NUIT",
                troisMars.plusDays(1), troisMars.plusDays(1), "ARRET_MALADIE");

        constraintVerifier
                .verifyThat((provider, factory) -> IndisponibiliteSalarie.indisponibiliteSalarie(factory))
                .given(nuit, indispo)
                .penalizesBy(1);
    }

    /** La comparaison reste stricte : un créneau qui finit à minuit pile ne touche pas le lendemain. */
    @Test
    @DisplayName("Un créneau qui s'arrête à minuit pile ne touche pas l'absence du lendemain")
    void indisponibilite_creneauFinissantAMinuit_doitNePasLeverViolation() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NUIT");
        LocalDate troisMars = LocalDate.of(2026, 3, 3);
        Creneau soiree = creneau("C-NUIT-002", troisMars, LocalTime.of(14, 0), LocalTime.of(0, 0), salarie);
        Indisponibilite indispo = new Indisponibilite("SAL-NUIT",
                troisMars.plusDays(1), troisMars.plusDays(1), "ARRET_MALADIE");

        constraintVerifier
                .verifyThat((provider, factory) -> IndisponibiliteSalarie.indisponibiliteSalarie(factory))
                .given(soiree, indispo)
                .penalizesBy(0);
    }

    /** Non-régression : un créneau qui <em>commence</em> pendant l'absence était déjà sanctionné. */
    @Test
    @DisplayName("Un créneau de nuit qui démarre le dernier jour d'absence reste sanctionné")
    void indisponibilite_creneauDeNuitDemarrantPendantLAbsence_doitLeverViolationHard() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NUIT");
        LocalDate troisMars = LocalDate.of(2026, 3, 3);
        Creneau nuit = creneau("C-NUIT-003", troisMars, LocalTime.of(22, 0), LocalTime.of(6, 0), salarie);
        Indisponibilite indispo = new Indisponibilite("SAL-NUIT",
                troisMars.minusDays(1), troisMars, "CONGE_POSE");

        constraintVerifier
                .verifyThat((provider, factory) -> IndisponibiliteSalarie.indisponibiliteSalarie(factory))
                .given(nuit, indispo)
                .penalizesBy(1);
    }

    /** L'élargissement ne déborde pas de l'autre côté : le lendemain de l'absence reste libre. */
    @Test
    @DisplayName("Un créneau de jour postérieur à l'absence n'est pas sanctionné")
    void indisponibilite_creneauApresLAbsence_doitNePasLeverViolation() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NUIT");
        LocalDate troisMars = LocalDate.of(2026, 3, 3);
        Creneau lendemain = creneauNormal("C-NUIT-004", troisMars.plusDays(1), salarie);
        Indisponibilite indispo = new Indisponibilite("SAL-NUIT",
                troisMars.minusDays(1), troisMars, "CONGE_POSE");

        constraintVerifier
                .verifyThat((provider, factory) -> IndisponibiliteSalarie.indisponibiliteSalarie(factory))
                .given(lendemain, indispo)
                .penalizesBy(0);
    }

    /**
     * Une absence sans dates faisait exploser la contrainte en {@code NullPointerException} :
     * {@code !c.getDate().isBefore(null)}. Elle est désormais sans effet, comme une borne absente
     * l'est partout ailleurs dans le moteur.
     */
    @Test
    @DisplayName("Une absence aux bornes nulles est sans effet, et ne fait plus exploser la contrainte")
    void indisponibilite_bornesNulles_doitEtreSansEffet() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie("SAL-NUIT");
        Creneau creneau = creneauNormal("C-NUIT-005", LocalDate.of(2026, 3, 3), salarie);
        Indisponibilite indispo = new Indisponibilite("SAL-NUIT", null, null, "SAISIE_INCOMPLETE");

        constraintVerifier
                .verifyThat((provider, factory) -> IndisponibiliteSalarie.indisponibiliteSalarie(factory))
                .given(creneau, indispo)
                .penalizesBy(0);
    }

    // -------------------------------------------------------
    // Non-régression SC-01
    // -------------------------------------------------------

    @Test
    void sc01_sansIndisponibilitesNiJourFerie_doitResteStable() {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(TestPlanningRequestFactory.ID_SALARIE_1041);
        Creneau creneau = creneauNormal("C-SC01-001", LocalDate.of(2026, 5, 11), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JourFerieRefuse.jourFerieRefuse(factory))
                .given(creneau, RegulatoryParameters.neutre())
                .penalizesBy(0);

        constraintVerifier
                .verifyThat((provider, factory) -> IndisponibiliteSalarie.indisponibiliteSalarie(factory))
                .given(creneau)
                .penalizesBy(0);
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    /** Calendrier réglementaire réduit aux dates fournies, plage de nuit par défaut. */
    private RegulatoryParameters calendrier(LocalDate... feries) {
        return new RegulatoryParameters(LocalTime.of(22, 0), LocalTime.of(6, 0), List.of(feries));
    }

    private Creneau creneauFerie(String id, LocalDate date, Ressource ressource) {
        Creneau c = new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.JOUR,
                true, QualificationJour.FERIE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }

    private Creneau creneauNormal(String id, LocalDate date, Ressource ressource) {
        Creneau c = new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }

    /** Créneau aux horaires libres, sans drapeau férié : seul le calendrier qualifie la date. */
    private Creneau creneau(String id, LocalDate date, LocalTime debut, LocalTime fin,
                            Ressource ressource) {
        Creneau c = new Creneau(
                id, date, debut, fin, 480,
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.NUIT,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }
}
