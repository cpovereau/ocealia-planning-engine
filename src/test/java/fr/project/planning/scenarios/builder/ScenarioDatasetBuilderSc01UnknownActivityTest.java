package fr.project.planning.scenarios.builder;

import fr.project.planning.scenarios.alerte.AlertCode;
import fr.project.planning.scenarios.alerte.AlertSeverity;
import fr.project.planning.scenarios.alerte.ScenarioAlert;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scenarios.builder.ScenarioDatasetBuilderSc01.BuildRequest;
import fr.project.planning.scenarios.builder.ScenarioDatasetBuilderSc01.BuildResult;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Alerte « activité inconnue » du builder SC-01.
 *
 * Le builder stampe `codeActiviteId = "travail"` sur tous les créneaux qu'il génère.
 * Si ce code est absent du référentiel injecté, le lookup échoue en aval : les créneaux
 * ne comptent pas dans la charge et les WorkMetrics tombent à zéro, sans que rien ne
 * le signale au client. Ces tests verrouillent la remontée de cette situation.
 */
class ScenarioDatasetBuilderSc01UnknownActivityTest {

    private final ScenarioDatasetBuilderSc01 builder = new ScenarioDatasetBuilderSc01();

    private static final LocalDate LUNDI    = LocalDate.of(2026, 7, 27);
    private static final LocalDate VENDREDI = LocalDate.of(2026, 7, 31);

    /**
     * [T-UA-01] Référentiel ne déclarant pas "travail" → alerte ERROR.
     */
    @Test
    void activite_absente_du_referentiel_produit_une_erreur() {
        BuildResult result = builder.build(request(referentielAvec("ACT-SOIN")));

        ScenarioAlert alert = alerteActiviteInconnue(result)
                .orElseThrow(() -> new AssertionError("UNKNOWN_ACTIVITY attendue"));

        assertEquals(AlertSeverity.ERROR, alert.severity(),
                "Les créneaux ne compteront pas dans la charge : c'est une anomalie");
        assertTrue(alert.message().contains("travail"),
                "Le message doit nommer le code manquant : " + alert.message());
    }

    /**
     * [T-UA-02] L'alerte porte sur le dataset, pas sur un jour : elle n'a pas de date.
     */
    @Test
    void alerte_activite_inconnue_n_a_pas_de_date() {
        BuildResult result = builder.build(request(referentielAvec("ACT-SOIN")));

        ScenarioAlert alert = alerteActiviteInconnue(result).orElseThrow();

        assertNull(alert.date(),
                "Une alerte de dataset ne porte pas de date — le champ est omis à la sérialisation");
    }

    /**
     * [T-UA-03] Une seule alerte, quel que soit le nombre de créneaux concernés.
     */
    @Test
    void alerte_emise_une_seule_fois_pour_tous_les_creneaux() {
        BuildResult result = builder.build(request(referentielAvec("ACT-SOIN")));

        long nb = result.alerts().stream()
                .filter(a -> a.code() == AlertCode.UNKNOWN_ACTIVITY)
                .count();

        assertEquals(10, result.creneaux().size(), "5 jours × 2 créneaux");
        assertEquals(1, nb, "L'alerte porte sur le code activité, pas sur chaque créneau");
    }

    /**
     * [T-UA-04] Référentiel déclarant "travail" → aucune alerte.
     */
    @Test
    void activite_presente_dans_le_referentiel_ne_produit_aucune_alerte() {
        BuildResult result = builder.build(request(referentielAvec("travail")));

        assertTrue(alerteActiviteInconnue(result).isEmpty(),
                "Le code est déclaré : aucune alerte attendue");
    }

    /**
     * [T-UA-05] Référentiel non fourni → vérification désactivée, pas de faux positif.
     */
    @Test
    void referentiel_absent_ne_declenche_pas_l_alerte() {
        BuildResult result = builder.build(request(null));

        assertTrue(alerteActiviteInconnue(result).isEmpty(),
                "Sans référentiel injecté, le builder ne peut rien conclure");
    }

    /**
     * [T-UA-06] Aucun créneau généré → aucune alerte, la situation est sans effet.
     */
    @Test
    void aucun_creneau_genere_ne_declenche_pas_l_alerte() {
        BuildRequest req = request(referentielAvec("ACT-SOIN"));
        req.holidayDates = Set.of(
                LocalDate.of(2026, 7, 27), LocalDate.of(2026, 7, 28), LocalDate.of(2026, 7, 29),
                LocalDate.of(2026, 7, 30), LocalDate.of(2026, 7, 31)
        );

        BuildResult result = builder.build(req);

        assertTrue(result.creneaux().isEmpty(), "Tous les jours sont fériés");
        assertTrue(alerteActiviteInconnue(result).isEmpty(),
                "Sans créneau généré, l'activité inconnue est sans conséquence");
    }

    // ----------------------------------------------------------
    // Utilitaires
    // ----------------------------------------------------------

    private Optional<ScenarioAlert> alerteActiviteInconnue(BuildResult result) {
        return result.alerts().stream()
                .filter(a -> a.code() == AlertCode.UNKNOWN_ACTIVITY)
                .findFirst();
    }

    private ReferentielComptabiliteActivite referentielAvec(String code) {
        return new ReferentielComptabiliteActivite(Map.of(code, new ComptabiliteActivite(
                code,
                true,   // compteDansCharge
                false,  // genereDetteRepos
                false,  // estServiceCritique
                false,  // prioritaireSurConfort
                ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
        )));
    }

    private BuildRequest request(ReferentielComptabiliteActivite referentiel) {
        BuildRequest req = new BuildRequest();
        req.dateDebut = LUNDI;
        req.dateFin = VENDREDI;
        req.ressource = new SalarieReel(
                "10212", null, "CDI",
                Set.of("CASTRES"), Set.of("travail"), Set.of("PC-100")
        );
        req.dailyAmplitudeHours = 7.0;
        req.shiftStart = LocalTime.of(9, 0);
        req.shiftEndAlert = LocalTime.of(17, 0);
        req.lunchBreakStart = LocalTime.of(12, 0);
        req.lunchBreakEnd = LocalTime.of(13, 0);
        req.workedDays = Set.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        );
        req.holidayDates = Set.of();
        req.referentiel = referentiel;
        return req;
    }
}
