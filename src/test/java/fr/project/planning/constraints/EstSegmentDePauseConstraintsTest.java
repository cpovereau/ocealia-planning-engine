package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.AmplitudeJournaliere;
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
 * EstSegmentDePauseConstraintsTest — Phase 8
 *
 * Vérifie que les créneaux marqués estSegmentDePause = true
 * sont bien exclus des contraintes concernées :
 * - AmplitudeJournaliere : la pause n'élargit pas l'amplitude
 * - JoursConsecutifsMax  : la pause seule ne compte pas comme jour travaillé
 *
 * Cas couverts :
 * [Amplitude]
 *  1. Pause encadrant un créneau de travail → amplitude calculée sur le travail seul
 *  2. Créneau de travail seul (pas de pause) → comportement inchangé (non-régression)
 *  3. Pause seule sur une journée → amplitude = 0 (pas de pénalité)
 *
 * [JoursConsecutifs]
 *  4. Pause seule sur une journée → jour non compté comme travaillé
 *  5. Pause + créneau travail le même jour → jour bien compté (la pause n'efface pas le travail)
 *  6. Créneaux travail seuls (pas de pause) → comportement inchangé (non-régression)
 */
class EstSegmentDePauseConstraintsTest {

    private static final int PENALITE_AMPLITUDE_BASE = 50; // valeur de defaultPenalites()
    private static final int PENALITE_JOURS_BASE     = 5_000;

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // =========================================================
    // AMPLITUDE JOURNALIERE
    // =========================================================

    // ---------------------------------------------------------
    // 1. Pause encadrant le travail → amplitude calculée sur le travail seul
    // ---------------------------------------------------------

    @Test
    void amplitude_pauseEncadrante_nElargitPasAmplitude() {
        // Salarié avec amplitude max = 8h (480 min)
        SalarieReel salarie = salarieAvecAmplitude("SAL-AMP-01", 8.0);
        PlanningContext context = contexteNeutre();
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // Travail de 09:00 à 17:00 → amplitude = 480 min = seuil exact → pas de pénalité
        Creneau travail = creneauJour("C-TRAV-01", lundi(), LocalTime.of(9, 0), LocalTime.of(17, 0), 480, salarie);

        // Pause avant le travail : 07:00–09:00 (estSegmentDePause = true)
        // Sans filtre : amplitude serait 07:00–17:00 = 600 min → pénalité de 120 × 50
        // Avec filtre [Phase 8] : amplitude = 480 min → 0 pénalité
        Creneau pauseAvant = pauseCreneauJour("C-PAUSE-01", lundi(), LocalTime.of(7, 0), LocalTime.of(9, 0), 120, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AmplitudeJournaliere.amplitudeJournaliere(factory))
                .given(salarie, travail, pauseAvant, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 2. Créneau de travail seul → comportement inchangé (non-régression)
    // ---------------------------------------------------------

    @Test
    void amplitude_sansPause_comportementInchange() {
        // Salarié avec amplitude max = 6h (360 min)
        SalarieReel salarie = salarieAvecAmplitude("SAL-AMP-02", 6.0);
        PlanningContext context = contexteNeutre();
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // Travail de 08:00 à 16:00 → amplitude = 480 min > 360 min → excédent = 120 min
        Creneau travail = creneauJour("C-TRAV-02", lundi(), LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AmplitudeJournaliere.amplitudeJournaliere(factory))
                .given(salarie, travail, ref, context)
                .penalizesBy(PENALITE_AMPLITUDE_BASE * 120);
    }

    // ---------------------------------------------------------
    // 3. Pause seule sur une journée → amplitude = 0, pas de pénalité
    // ---------------------------------------------------------

    @Test
    void amplitude_pauseSeule_pasDePenalite() {
        // Salarié avec amplitude max = 4h (240 min) — seuil très bas pour détecter tout dépassement
        SalarieReel salarie = salarieAvecAmplitude("SAL-AMP-03", 4.0);
        PlanningContext context = contexteNeutre();
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // Seul un créneau de pause (9h–14h = 300 min) — doit être exclu
        Creneau pause = pauseCreneauJour("C-PAUSE-03", lundi(), LocalTime.of(9, 0), LocalTime.of(14, 0), 300, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AmplitudeJournaliere.amplitudeJournaliere(factory))
                .given(salarie, pause, ref, context)
                .penalizesBy(0);
    }

    // =========================================================
    // JOURS CONSECUTIFS MAX
    // =========================================================

    // ---------------------------------------------------------
    // 4. Pause seule sur une journée → ne compte pas comme jour travaillé
    // ---------------------------------------------------------

    @Test
    void joursConsecutifs_pauseSeule_neComptesPasCommeJourTravaille() {
        // Seuil = 2 jours consécutifs
        SalarieReel salarie = salarieAvecJoursConsecutifs("SAL-JC-01", 2);
        PlanningContext context = contexteAvecHorizon(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // Lundi : créneau travail
        Creneau c1 = creneauJour("C-JC-01", lundi(), LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        // Mardi : pause seulement (doit être exclu)
        // Sans filtre : lundi+mardi = 2 jours = seuil exact → 0 pénalité
        // Avec filtre [Phase 8] : seulement lundi = 1 jour → 0 pénalité également
        // Le test démontre que la pause n'est pas comptée : ajouter mercredi doit toujours donner 1 jour de dépassement
        Creneau pause = pauseCreneauJour("C-PAUSE-JC-01", lundi().plusDays(1),
                LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        Creneau c3 = creneauJour("C-JC-02", lundi().plusDays(2), LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        // Lundi (travail) + mardi (pause = exclu) + mercredi (travail) = 2 jours non consécutifs
        // → séquence max = 1 → 0 pénalité (seuil = 2)
        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, c1, pause, c3, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 5. Pause + travail le même jour → jour bien compté (la pause n'efface pas le travail)
    // ---------------------------------------------------------

    @Test
    void joursConsecutifs_pausePlusTravailMemeJour_jourBienCompte() {
        // Seuil = 2 jours consécutifs
        SalarieReel salarie = salarieAvecJoursConsecutifs("SAL-JC-02", 2);
        PlanningContext context = contexteAvecHorizon(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        // Lundi : travail + pause → doit compter comme 1 jour travaillé
        Creneau travailLundi = creneauJour("C-JC-11", lundi(), LocalTime.of(8, 0), LocalTime.of(12, 0), 240, salarie);
        Creneau pauseLundi   = pauseCreneauJour("C-PAUSE-JC-11", lundi(), LocalTime.of(12, 0), LocalTime.of(13, 0), 60, salarie);
        // Mardi : travail
        Creneau travailMardi = creneauJour("C-JC-12", lundi().plusDays(1), LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        // Mercredi : travail → 3 jours consécutifs → 1 jour de dépassement
        Creneau travailMercredi = creneauJour("C-JC-13", lundi().plusDays(2), LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, travailLundi, pauseLundi, travailMardi, travailMercredi, ref, context)
                .penalizesBy(PENALITE_JOURS_BASE);
    }

    // ---------------------------------------------------------
    // 6. Créneaux de travail seuls → comportement inchangé (non-régression)
    // ---------------------------------------------------------

    @Test
    void joursConsecutifs_sansPause_comportementInchange() {
        // Seuil = 3 jours ; 4 jours consécutifs → 1 pénalité
        SalarieReel salarie = salarieAvecJoursConsecutifs("SAL-JC-03", 3);
        PlanningContext context = contexteAvecHorizon(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c1 = creneauJour("C-JC-21", lundi(),                 LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        Creneau c2 = creneauJour("C-JC-22", lundi().plusDays(1), LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        Creneau c3 = creneauJour("C-JC-23", lundi().plusDays(2), LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);
        Creneau c4 = creneauJour("C-JC-24", lundi().plusDays(3), LocalTime.of(8, 0), LocalTime.of(16, 0), 480, salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> JoursConsecutifsMax.maxJoursConsecutifs(factory))
                .given(salarie, c1, c2, c3, c4, ref, context)
                .penalizesBy(PENALITE_JOURS_BASE);
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static LocalDate lundi() {
        return LocalDate.of(2026, 5, 11);
    }

    private static SalarieReel salarieAvecAmplitude(String id, double amplitudeMax) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, amplitudeMax, null, null, null, null, null
        ));
        return salarie;
    }

    private static SalarieReel salarieAvecJoursConsecutifs(String id, int joursMax) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, null, null, null, null, null, joursMax
        ));
        return salarie;
    }

    private static PlanningContext contexteNeutre() {
        return TestPlanningContextFactory.contexteNeutre(lundi(), lundi());
    }

    private static PlanningContext contexteAvecHorizon(LocalDate debut, LocalDate fin) {
        return TestPlanningContextFactory.contexteNeutre(debut, fin);
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

    /** Créneau de travail (estSegmentDePause = false / null). */
    private static Creneau creneauJour(String id, LocalDate date,
                                       LocalTime debut, LocalTime fin, int duree,
                                       SalarieReel salarie) {
        Creneau c = new Creneau(
                id, date, debut, fin, duree,
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(salarie);
        // estSegmentDePause = null → traité comme false par le filtre Boolean.TRUE.equals()
        return c;
    }

    /** Créneau de pause (estSegmentDePause = true). */
    private static Creneau pauseCreneauJour(String id, LocalDate date,
                                             LocalTime debut, LocalTime fin, int duree,
                                             SalarieReel salarie) {
        Creneau c = new Creneau(
                id, date, debut, fin, duree,
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(salarie);
        c.setEstSegmentDePause(true);
        return c;
    }
}
