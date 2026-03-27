package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.AlternanceJourNuit;
import fr.project.planning.constraints.legales.AmplitudeJournaliere;
import fr.project.planning.constraints.legales.HeuresMinimumParJour;
import fr.project.planning.constraints.legales.JoursConsecutifsMax;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestPlanningContextFactory;
import fr.project.planning.fixtures.TestPlanningRequestFactory;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.solution.PlanningProblem;
import org.junit.jupiter.api.Test;
import org.optaplanner.test.api.score.stream.ConstraintVerifier;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * CodeActiviteIdFallbackConstraintsTest — Phase 10A
 *
 * Valide que les contraintes JoursConsecutifsMax, AmplitudeJournaliere,
 * HeuresMinimumParJour et AlternanceJourNuit utilisent bien le fallback
 * codeActiviteId → activite introduit en Phase 10A.
 *
 * Cas couverts :
 * - Créneau avec uniquement codeActiviteId (activite = null) : la contrainte s'active correctement
 * - Non-régression SC-01 : créneau avec activite non null et codeActiviteId = null → comportement inchangé
 *
 * Avant Phase 10A, les quatre contraintes lisaient uniquement creneau.getActivite() :
 * un créneau SC-03 avec seulement codeActiviteId était silencieusement exclu du calcul.
 */
class CodeActiviteIdFallbackConstraintsTest {

    private static final String ACT_CODE = "ACT-10A";
    private static final int PENALITE_JOURS_CONSECUTIFS = 5_000;
    private static final int PENALITE_AMPLITUDE         = 50;
    private static final int PENALITE_HEURES_MIN        = 50;
    private static final int PENALITE_ALTERNANCE        = 3_000;

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // =========================================================
    // JoursConsecutifsMax — fallback codeActiviteId
    // =========================================================

    @Test
    void joursConsecutifsMax_avecSeulementCodeActiviteId_doitEtrePenalise() {
        // [Phase 10A] Créneau SC-03 : codeActiviteId = "ACT-10A", activite = null
        // Avant fix : ref.getByCode(null) = null → créneau exclu → 0 pénalité (bug)
        // Après fix : ref.getByCode("ACT-10A") = ComptabiliteActivite → créneau inclus → pénalité
        SalarieReel salarie = salarieJoursConsecutifs("SAL-10A-JC", 3);
        PlanningContext context = TestPlanningContextFactory.contexteNeutre(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielCodeActivite();

        // 4 jours consécutifs → seuil=3 → dépassement de 1 jour
        Creneau c1 = creneauCodeActiviteId("C-JC-01", lundi(),                 salarie, TypePlageHoraire.JOUR);
        Creneau c2 = creneauCodeActiviteId("C-JC-02", lundi().plusDays(1), salarie, TypePlageHoraire.JOUR);
        Creneau c3 = creneauCodeActiviteId("C-JC-03", lundi().plusDays(2), salarie, TypePlageHoraire.JOUR);
        Creneau c4 = creneauCodeActiviteId("C-JC-04", lundi().plusDays(3), salarie, TypePlageHoraire.JOUR);

        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, c1, c2, c3, c4, ref, context)
                .penalizesBy(PENALITE_JOURS_CONSECUTIFS);
    }

    @Test
    void joursConsecutifsMax_sc01StyleActivite_doitResterInchange() {
        // Non-régression SC-01 : codeActiviteId = null, activite = "travail"
        // Après fix : fallback sur activite → comportement identique
        SalarieReel salarie = salarieJoursConsecutifs("SAL-SC01-JC", 3);
        PlanningContext context = TestPlanningContextFactory.contexteNeutre(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielActiviteTravail();

        // 4 jours consécutifs → seuil=3 → dépassement de 1 jour
        Creneau c1 = creneauActivite("C-JC-SC01-01", lundi(),                 salarie, TypePlageHoraire.JOUR);
        Creneau c2 = creneauActivite("C-JC-SC01-02", lundi().plusDays(1), salarie, TypePlageHoraire.JOUR);
        Creneau c3 = creneauActivite("C-JC-SC01-03", lundi().plusDays(2), salarie, TypePlageHoraire.JOUR);
        Creneau c4 = creneauActivite("C-JC-SC01-04", lundi().plusDays(3), salarie, TypePlageHoraire.JOUR);

        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, c1, c2, c3, c4, ref, context)
                .penalizesBy(PENALITE_JOURS_CONSECUTIFS);
    }

    // =========================================================
    // AmplitudeJournaliere — fallback codeActiviteId
    // =========================================================

    @Test
    void amplitudeJournaliere_avecSeulementCodeActiviteId_doitEtrePenalisee() {
        // Créneau 08h-20h : amplitude = 720 min, seuil = 10h (600 min) → excédent = 120
        // Pénalité = 50 × 120 = 6 000
        SalarieReel salarie = salarieAmplitude("SAL-10A-AMP", 10.0);
        PlanningContext context = TestPlanningContextFactory.contexteNeutre(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielCodeActivite();

        Creneau c = creneauCodeActiviteIdHeures("C-AMP-01", lundi(), salarie,
                LocalTime.of(8, 0), LocalTime.of(20, 0), 720);

        constraintVerifier
                .verifyThat((provider, factory) -> AmplitudeJournaliere.amplitudeJournaliere(factory))
                .given(salarie, c, ref, context)
                .penalizesBy(PENALITE_AMPLITUDE * 120);
    }

    @Test
    void amplitudeJournaliere_sc01StyleActivite_doitResterInchange() {
        // Non-régression SC-01 : même calcul avec activite = "travail"
        SalarieReel salarie = salarieAmplitude("SAL-SC01-AMP", 10.0);
        PlanningContext context = TestPlanningContextFactory.contexteNeutre(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielActiviteTravail();

        Creneau c = creneauActiviteHeures("C-AMP-SC01-01", lundi(), salarie,
                LocalTime.of(8, 0), LocalTime.of(20, 0), 720);

        constraintVerifier
                .verifyThat((provider, factory) -> AmplitudeJournaliere.amplitudeJournaliere(factory))
                .given(salarie, c, ref, context)
                .penalizesBy(PENALITE_AMPLITUDE * 120);
    }

    // =========================================================
    // HeuresMinimumParJour — fallback codeActiviteId
    // =========================================================

    @Test
    void heuresMinimumParJour_avecSeulementCodeActiviteId_doitEtrePenalisee() {
        // Créneau de 3h (180 min), seuil = 4h (240 min) → déficit = 60
        // Pénalité = 50 × 60 = 3 000
        SalarieReel salarie = salarieHeuresMin("SAL-10A-HMP", 4.0);
        PlanningContext context = TestPlanningContextFactory.contexteNeutre(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielCodeActivite();

        Creneau c = creneauCodeActiviteIdHeures("C-HMP-01", lundi(), salarie,
                LocalTime.of(8, 0), LocalTime.of(11, 0), 180);

        constraintVerifier
                .verifyThat((provider, factory) -> HeuresMinimumParJour.heuresMinimumParJour(factory))
                .given(salarie, c, ref, context)
                .penalizesBy(PENALITE_HEURES_MIN * 60);
    }

    @Test
    void heuresMinimumParJour_sc01StyleActivite_doitResterInchange() {
        // Non-régression SC-01 : même calcul avec activite = "travail"
        SalarieReel salarie = salarieHeuresMin("SAL-SC01-HMP", 4.0);
        PlanningContext context = TestPlanningContextFactory.contexteNeutre(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielActiviteTravail();

        Creneau c = creneauActiviteHeures("C-HMP-SC01-01", lundi(), salarie,
                LocalTime.of(8, 0), LocalTime.of(11, 0), 180);

        constraintVerifier
                .verifyThat((provider, factory) -> HeuresMinimumParJour.heuresMinimumParJour(factory))
                .given(salarie, c, ref, context)
                .penalizesBy(PENALITE_HEURES_MIN * 60);
    }

    // =========================================================
    // AlternanceJourNuit — fallback codeActiviteId
    // =========================================================

    @Test
    void alternanceJourNuit_avecSeulementCodeActiviteId_doitEtrePenalisee() {
        // JOUR lundi → NUIT mardi : 1 alternance avec créneaux ayant uniquement codeActiviteId
        SalarieReel salarie = salarie("SAL-10A-ALT");
        PlanningContext context = TestPlanningContextFactory.contexteNeutre(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielCodeActivite();

        Creneau cJour = creneauCodeActiviteId("C-ALT-JOUR", lundi(),                 salarie, TypePlageHoraire.JOUR);
        Creneau cNuit = creneauCodeActiviteId("C-ALT-NUIT", lundi().plusDays(1), salarie, TypePlageHoraire.NUIT);

        constraintVerifier
                .verifyThat((provider, factory) -> AlternanceJourNuit.alternanceJourNuit(factory))
                .given(salarie, cJour, cNuit, ref, context)
                .penalizesBy(PENALITE_ALTERNANCE);
    }

    @Test
    void alternanceJourNuit_sc01StyleActivite_doitResterInchange() {
        // Non-régression SC-01 : même scénario avec activite = "travail"
        SalarieReel salarie = salarie("SAL-SC01-ALT");
        PlanningContext context = TestPlanningContextFactory.contexteNeutre(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielActiviteTravail();

        Creneau cJour = creneauActivite("C-ALT-SC01-JOUR", lundi(),                 salarie, TypePlageHoraire.JOUR);
        Creneau cNuit = creneauActivite("C-ALT-SC01-NUIT", lundi().plusDays(1), salarie, TypePlageHoraire.NUIT);

        constraintVerifier
                .verifyThat((provider, factory) -> AlternanceJourNuit.alternanceJourNuit(factory))
                .given(salarie, cJour, cNuit, ref, context)
                .penalizesBy(PENALITE_ALTERNANCE);
    }

    // =========================================================
    // Helpers — dates
    // =========================================================

    private static LocalDate lundi() {
        return LocalDate.of(2026, 5, 11);
    }

    // =========================================================
    // Helpers — salariés
    // =========================================================

    private SalarieReel salarie(String id) {
        return TestPlanningRequestFactory.buildSalarie(id);
    }

    private SalarieReel salarieJoursConsecutifs(String id, int joursMax) {
        SalarieReel s = TestPlanningRequestFactory.buildSalarie(id);
        s.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, null, null, null, null, joursMax
        ));
        return s;
    }

    private SalarieReel salarieAmplitude(String id, double amplitudeMaxHeures) {
        SalarieReel s = TestPlanningRequestFactory.buildSalarie(id);
        s.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, amplitudeMaxHeures, null, null, null, null, null
        ));
        return s;
    }

    private SalarieReel salarieHeuresMin(String id, double heuresMinParJour) {
        SalarieReel s = TestPlanningRequestFactory.buildSalarie(id);
        s.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                heuresMinParJour, null, null, null, null, null, null, null
        ));
        return s;
    }

    // =========================================================
    // Helpers — référentiels
    // =========================================================

    /**
     * Référentiel SC-03 : activité identifiée par codeActiviteId uniquement.
     */
    private ReferentielComptabiliteActivite referentielCodeActivite() {
        return new ReferentielComptabiliteActivite(Map.of(
                ACT_CODE,
                new ComptabiliteActivite(
                        ACT_CODE,
                        true,  // compteDansCharge
                        false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
                )
        ));
    }

    /**
     * Référentiel SC-01 : activité identifiée par le champ activite ("travail").
     */
    private ReferentielComptabiliteActivite referentielActiviteTravail() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        true,  // compteDansCharge
                        false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
                )
        ));
    }

    // =========================================================
    // Helpers — créneaux codeActiviteId uniquement (SC-03)
    // =========================================================

    /**
     * Créneau SC-03 : codeActiviteId = ACT_CODE, activite = null, duree = 480 min.
     */
    private Creneau creneauCodeActiviteId(String id, LocalDate date,
                                          SalarieReel ressource, TypePlageHoraire plage) {
        Creneau c = new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                TestRessourceFactory.SITE_CANON,
                ACT_CODE,  // codeActiviteId
                null,      // activite = null → avant fix, ref.getByCode(null) = null (bug)
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, plage,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }

    /**
     * Créneau SC-03 avec heures personnalisées.
     */
    private Creneau creneauCodeActiviteIdHeures(String id, LocalDate date,
                                                SalarieReel ressource,
                                                LocalTime heureDebut, LocalTime heureFin, int duree) {
        Creneau c = new Creneau(
                id, date, heureDebut, heureFin, duree,
                TestRessourceFactory.SITE_CANON,
                ACT_CODE,  // codeActiviteId
                null,      // activite = null
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }

    // =========================================================
    // Helpers — créneaux activite uniquement (SC-01)
    // =========================================================

    /**
     * Créneau SC-01 : codeActiviteId = null, activite = "travail", duree = 480 min.
     */
    private Creneau creneauActivite(String id, LocalDate date,
                                    SalarieReel ressource, TypePlageHoraire plage) {
        Creneau c = new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                TestRessourceFactory.SITE_CANON,
                null,  // codeActiviteId = null (SC-01)
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, plage,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }

    /**
     * Créneau SC-01 avec heures personnalisées.
     */
    private Creneau creneauActiviteHeures(String id, LocalDate date,
                                          SalarieReel ressource,
                                          LocalTime heureDebut, LocalTime heureFin, int duree) {
        Creneau c = new Creneau(
                id, date, heureDebut, heureFin, duree,
                TestRessourceFactory.SITE_CANON,
                null,  // codeActiviteId = null (SC-01)
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }
}
