package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.AmplitudeJournaliere;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase12ConstraintsTest
 *
 * Valide la contrainte SOFT introduite en Phase 12 :
 * - AmplitudeJournaliere : pénalise le dépassement de l'amplitude journalière maximale.
 *
 * Amplitude = max(heureFin) - min(heureDebut) des créneaux compteDansCharge = true.
 * Seuil : ContraintesReglementairesSalarie.amplitudeJournaliereMaximum (en heures).
 * Pénalité : penaliteAmplitude (50) × minutes de dépassement.
 *
 * Cas couverts :
 * 1. Amplitude sous seuil → 0 pénalité
 * 2. Dépassement simple (1 jour) → pénalité proportionnelle
 * 3. Multi-créneaux avec pause → amplitude = borne temporelle, pas somme des durées
 * 4. Seuil absent (amplitudeJournaliereMaximum = null) → 0 pénalité
 * 5. Activité sans charge → ne compte pas
 * 6. Créneau unique → amplitude = heureFin - heureDebut du créneau
 * 7. Calcul unitaire de calculerAmplitudeMinutes (cross-midnight)
 */
class Phase12ConstraintsTest {

    private static final int PENALITE_BASE = 50; // valeur de defaultPenalites() pour amplitude

    private final ConstraintVerifier<ConstraintProviderImpl, PlanningProblem> constraintVerifier =
            ConstraintVerifier.build(new ConstraintProviderImpl(), PlanningProblem.class, Creneau.class);

    // ---------------------------------------------------------
    // 1. Amplitude sous seuil → pas de pénalité
    // ---------------------------------------------------------

    @Test
    void amplitudeSousSeuil_doitNePasEtrePenalisee() {
        // Amplitude 8h (480 min), seuil 10h → pas de dépassement
        SalarieReel salarie = salarieAvecAmplitude("SAL-A", 10.0);
        PlanningContext context = contexte(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c = creneau("C-01", lundi(), LocalTime.of(8, 0), LocalTime.of(16, 0), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AmplitudeJournaliere.amplitudeJournaliere(factory))
                .given(salarie, c, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 2. Dépassement simple → pénalité = base × excédent en minutes
    // ---------------------------------------------------------

    @Test
    void depassementSimple_doitEtrePenalise() {
        // Amplitude 12h (720 min), seuil 10h (600 min) → excédent = 120 min
        SalarieReel salarie = salarieAvecAmplitude("SAL-B", 10.0);
        PlanningContext context = contexte(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c = creneau("C-11", lundi(), LocalTime.of(8, 0), LocalTime.of(20, 0), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AmplitudeJournaliere.amplitudeJournaliere(factory))
                .given(salarie, c, ref, context)
                .penalizesBy(PENALITE_BASE * 120); // 120 min d'excédent
    }

    // ---------------------------------------------------------
    // 3. Multi-créneaux avec pause → amplitude = borne temporelle
    // ---------------------------------------------------------

    @Test
    void multiCreneauxAvecPause_amplitudeEstBorneTemporelle() {
        // 08:00-12:00 et 14:00-18:00 → amplitude = 18:00-08:00 = 600 min = 10h (pas 480 min de travail)
        // Seuil 9h (540 min) → excédent = 60 min
        SalarieReel salarie = salarieAvecAmplitude("SAL-C", 9.0);
        PlanningContext context = contexte(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c1 = creneau("C-21", lundi(), LocalTime.of(8, 0),  LocalTime.of(12, 0), salarie);
        Creneau c2 = creneau("C-22", lundi(), LocalTime.of(14, 0), LocalTime.of(18, 0), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AmplitudeJournaliere.amplitudeJournaliere(factory))
                .given(salarie, c1, c2, ref, context)
                .penalizesBy(PENALITE_BASE * 60); // 60 min d'excédent
    }

    // ---------------------------------------------------------
    // 4. Seuil absent → pas de pénalité
    // ---------------------------------------------------------

    @Test
    void seuilAbsent_doitNePasEtrePenalise() {
        // amplitudeJournaliereMaximum = null → contrainte non active
        SalarieReel salarie = salarieAvecAmplitude("SAL-D", null);
        PlanningContext context = contexte(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c = creneau("C-31", lundi(), LocalTime.of(6, 0), LocalTime.of(22, 0), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AmplitudeJournaliere.amplitudeJournaliere(factory))
                .given(salarie, c, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 5. Activité sans charge → ne compte pas
    // ---------------------------------------------------------

    @Test
    void activiteSansCharge_neDoitPasCompter() {
        // Activité compteDansCharge = false → amplitude = 0 → 0 pénalité
        SalarieReel salarie = salarieAvecAmplitude("SAL-E", 8.0);
        PlanningContext context = contexte(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielSansCharge();

        Creneau c = creneau("C-41", lundi(), LocalTime.of(6, 0), LocalTime.of(22, 0), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AmplitudeJournaliere.amplitudeJournaliere(factory))
                .given(salarie, c, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 6. Créneau unique → amplitude = durée du créneau (borne)
    // ---------------------------------------------------------

    @Test
    void creneauUnique_amplitudeEgaleADuree() {
        // 08:00-16:00 → amplitude = 480 min = 8h, seuil = 9h → pas de dépassement
        SalarieReel salarie = salarieAvecAmplitude("SAL-F", 9.0);
        PlanningContext context = contexte(lundi(), lundi().plusDays(6));
        ReferentielComptabiliteActivite ref = referentielAvecCharge();

        Creneau c = creneau("C-51", lundi(), LocalTime.of(8, 0), LocalTime.of(16, 0), salarie);

        constraintVerifier
                .verifyThat((provider, factory) -> AmplitudeJournaliere.amplitudeJournaliere(factory))
                .given(salarie, c, ref, context)
                .penalizesBy(0);
    }

    // ---------------------------------------------------------
    // 7. Test unitaire : calculerAmplitudeMinutes — cas cross-midnight
    // ---------------------------------------------------------

    @Test
    void calculerAmplitudeMinutes_creneauCrossMidnight() {
        // 22:00-06:00 → amplitude = (06:00 + 24h) - 22:00 = 30:00 - 22:00 = 480 min
        Creneau c = creneauSansRessource("C-61", lundi(), LocalTime.of(22, 0), LocalTime.of(6, 0));

        int amplitude = AmplitudeJournaliere.calculerAmplitudeMinutes(List.of(c));

        assertThat(amplitude).isEqualTo(480);
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private static LocalDate lundi() {
        return LocalDate.of(2026, 5, 11);
    }

    private SalarieReel salarieAvecAmplitude(String id, Double amplitudeHeures) {
        SalarieReel salarie = TestPlanningRequestFactory.buildSalarie(id);
        salarie.setContraintesReglementaires(new ContraintesReglementairesSalarie(
                null, null, amplitudeHeures, null, null, null, null, null
        ));
        return salarie;
    }

    private PlanningContext contexte(LocalDate debut, LocalDate fin) {
        return TestPlanningContextFactory.contexteNeutre(debut, fin);
    }

    private ReferentielComptabiliteActivite referentielAvecCharge() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        true, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
                )
        ));
    }

    private ReferentielComptabiliteActivite referentielSansCharge() {
        return new ReferentielComptabiliteActivite(Map.of(
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                new ComptabiliteActivite(
                        TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                        false, false, false, false,
                        ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
                )
        ));
    }

    private Creneau creneau(String id, LocalDate date, LocalTime debut, LocalTime fin, SalarieReel ressource) {
        Creneau c = new Creneau(
                id, date, debut, fin, (int) java.time.Duration.between(debut, fin).toMinutes(),
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.JOUR,
                false, QualificationJour.OUVRE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }

    private Creneau creneauSansRessource(String id, LocalDate date, LocalTime debut, LocalTime fin) {
        return new Creneau(
                id, date, debut, fin, 0,
                TestRessourceFactory.SITE_CANON, null,
                TestPlanningRequestFactory.ACTIVITE_TRAVAIL,
                TestRessourceFactory.POSTE_COMPTABLE_CANON,
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.NUIT,
                false, QualificationJour.OUVRE
        );
    }
}
