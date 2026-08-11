package fr.project.planning.scenarios.builder;

import fr.project.planning.scenarios.alerte.AlertCode;
import fr.project.planning.scenarios.alerte.AlertSeverity;
import fr.project.planning.scenarios.alerte.ScenarioAlert;
import fr.project.planning.domain.creneau.Creneau;
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
 * Lot L1 — code activité déclaré par l'appelant en SC-01.
 *
 * Le moteur ne doit pas nommer une donnée métier à la place du client : le code activité
 * porté par les créneaux générés vient du vocabulaire de l'appelant, faute de quoi le
 * résultat ne lui est pas réintégrable sans table de correspondance.
 *
 * Le repli historique "travail" est conservé pour ne pas rompre les intégrations existantes,
 * mais il est signalé.
 */
class ScenarioDatasetBuilderSc01CodeActiviteTest {

    private final ScenarioDatasetBuilderSc01 builder = new ScenarioDatasetBuilderSc01();

    private static final LocalDate LUNDI    = LocalDate.of(2026, 7, 27);
    private static final LocalDate VENDREDI = LocalDate.of(2026, 7, 31);

    /**
     * [T-CA-01] Le code déclaré est porté par tous les créneaux générés.
     */
    @Test
    void code_declare_est_porte_par_les_creneaux() {
        BuildResult result = builder.build(request("ACT-SOIN", null));

        assertFalse(result.creneaux().isEmpty());
        assertTrue(result.creneaux().stream().allMatch(c -> "ACT-SOIN".equals(c.getCodeActiviteId())),
                "Tous les créneaux doivent porter le code déclaré");
    }

    /**
     * [T-CA-02] Le code déclaré alimente aussi le champ legacy `activite`.
     */
    @Test
    void code_declare_alimente_le_champ_legacy() {
        BuildResult result = builder.build(request("ACT-SOIN", null));

        Creneau premier = result.creneaux().get(0);
        assertEquals("ACT-SOIN", premier.getCodeActiviteId());
        assertEquals("ACT-SOIN", premier.getActivite());
    }

    /**
     * [T-CA-03] Code déclaré → aucune alerte de repli.
     */
    @Test
    void code_declare_ne_produit_aucune_alerte_de_repli() {
        BuildResult result = builder.build(request("ACT-SOIN", null));

        assertTrue(alerteRepli(result).isEmpty(),
                "Le code est déclaré : aucun repli à signaler");
    }

    /**
     * [T-CA-04] Code absent → repli sur "travail", signalé en WARNING et sans date.
     */
    @Test
    void code_absent_produit_un_repli_signale() {
        BuildResult result = builder.build(request(null, null));

        ScenarioAlert alert = alerteRepli(result)
                .orElseThrow(() -> new AssertionError("ACTIVITY_CODE_DEFAULTED attendue"));

        assertEquals(AlertSeverity.WARNING, alert.severity());
        assertNull(alert.date(), "L'alerte porte sur la configuration, pas sur un jour");
        assertTrue(result.creneaux().stream().allMatch(c -> "travail".equals(c.getCodeActiviteId())),
                "Le repli historique doit rester 'travail'");
    }

    /**
     * [T-CA-05] Une chaîne vide vaut absence — pas un code activité nommé "".
     */
    @Test
    void code_blanc_est_traite_comme_absent() {
        BuildResult result = builder.build(request("   ", null));

        assertTrue(alerteRepli(result).isPresent(), "Une chaîne blanche n'est pas un code");
        assertTrue(result.creneaux().stream().allMatch(c -> "travail".equals(c.getCodeActiviteId())));
    }

    /**
     * [T-CA-06] La vérification référentiel porte sur le code déclaré, pas sur le repli.
     */
    @Test
    void activite_inconnue_designe_le_code_declare() {
        BuildResult result = builder.build(request("ACT-SOIN", referentielAvec("travail")));

        ScenarioAlert alert = result.alerts().stream()
                .filter(a -> a.code() == AlertCode.UNKNOWN_ACTIVITY)
                .findFirst()
                .orElseThrow(() -> new AssertionError("UNKNOWN_ACTIVITY attendue"));

        assertTrue(alert.message().contains("ACT-SOIN"),
                "Le message doit nommer le code déclaré : " + alert.message());
        assertFalse(alert.message().contains("'travail'"),
                "Le repli ne doit pas apparaître alors qu'un code est déclaré : " + alert.message());
    }

    /**
     * [T-CA-07] Code déclaré et présent au référentiel → aucune alerte du tout.
     */
    @Test
    void code_declare_et_reference_ne_produit_aucune_alerte() {
        BuildResult result = builder.build(request("ACT-SOIN", referentielAvec("ACT-SOIN")));

        assertTrue(result.alerts().isEmpty(),
                "Configuration nominale : aucune alerte attendue, obtenu " + result.alerts());
    }

    // ----------------------------------------------------------
    // Utilitaires
    // ----------------------------------------------------------

    private Optional<ScenarioAlert> alerteRepli(BuildResult result) {
        return result.alerts().stream()
                .filter(a -> a.code() == AlertCode.ACTIVITY_CODE_DEFAULTED)
                .findFirst();
    }

    private ReferentielComptabiliteActivite referentielAvec(String code) {
        return new ReferentielComptabiliteActivite(Map.of(code, new ComptabiliteActivite(
                code, true, false, false, false,
                ComptabiliteActivite.TypeImpactActivite.CHARGE_STANDARD
        )));
    }

    private BuildRequest request(String codeActiviteId, ReferentielComptabiliteActivite referentiel) {
        BuildRequest req = new BuildRequest();
        req.dateDebut = LUNDI;
        req.dateFin = VENDREDI;
        req.ressource = new SalarieReel(
                "10212", null, "CDI",
                Set.of("CASTRES"), Set.of("ACT-SOIN"), Set.of("PC-100")
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
        req.codeActiviteId = codeActiviteId;
        req.referentiel = referentiel;
        return req;
    }
}
