package fr.project.planning.scenarios.builder;

import fr.project.planning.scenarios.alerte.AlertCode;
import fr.project.planning.scenarios.alerte.AlertSeverity;
import fr.project.planning.scenarios.alerte.ScenarioAlert;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scenarios.builder.ScenarioDatasetBuilderSc01.BuildRequest;
import fr.project.planning.scenarios.builder.ScenarioDatasetBuilderSc01.BuildResult;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Qualification du repos hebdomadaire et alertes associées (SC-01).
 *
 * Vérifie que :
 * - un horaire réduit (< 5 jours travaillés) n'est pas restitué comme une anomalie
 * - le repos hebdomadaire suit le week-end et non l'ordre des jours
 * - l'absence totale de repos reste une erreur
 * - les alertes de configuration ne sont pas dupliquées par semaine
 */
class ScenarioDatasetBuilderSc01WeeklyRestTest {

    private final ScenarioDatasetBuilderSc01 builder = new ScenarioDatasetBuilderSc01();

    private static final LocalDate LUNDI    = LocalDate.of(2026, 7, 27);
    private static final LocalDate DIMANCHE = LocalDate.of(2026, 8, 2);
    private static final LocalDate MERCREDI = LocalDate.of(2026, 7, 29);

    private static final Set<DayOfWeek> QUATRE_JOURS = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    private static final Set<DayOfWeek> SEMAINE_STANDARD = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    /**
     * [T-WR-01] Un temps partiel à 4 jours produit une information, pas une anomalie.
     */
    @Test
    void horaire_reduit_produit_une_alerte_info_et_aucune_erreur() {
        BuildResult result = builder.build(request(LUNDI, DIMANCHE, QUATRE_JOURS));

        List<ScenarioAlert> alerts = result.alerts();
        assertEquals(1, alerts.size(), "Une seule alerte attendue : " + alerts);

        ScenarioAlert alert = alerts.get(0);
        assertEquals(AlertCode.TOO_MANY_NON_WORKED_DAYS, alert.code());
        assertEquals(AlertSeverity.INFO, alert.severity(),
                "Un horaire réduit est une configuration valide, pas une anomalie");
        assertTrue(alert.message().contains("WEDNESDAY"),
                "Le message doit désigner le jour hors repos hebdomadaire : " + alert.message());
    }

    /**
     * [T-WR-02] Le mercredi non coché ne consomme pas le repos hebdomadaire :
     * le samedi et le dimanche restent RH/RHD, donc aucune erreur de repos.
     */
    @Test
    void jour_non_travaille_en_semaine_ne_consomme_pas_le_repos_hebdomadaire() {
        BuildResult result = builder.build(request(LUNDI, DIMANCHE, QUATRE_JOURS));

        assertTrue(
                result.alerts().stream().noneMatch(a -> a.code() == AlertCode.INSUFFICIENT_WEEKLY_REST),
                "Le repos hebdomadaire est qualifiable (samedi + dimanche non travaillés)"
        );
    }

    /**
     * [T-WR-03] Non-régression : la requalification en NON_TRAVAILLE ne fait pas
     * apparaître de créneau sur un jour non coché.
     */
    @Test
    void aucun_creneau_genere_sur_un_jour_non_coche() {
        BuildResult result = builder.build(request(LUNDI, DIMANCHE, QUATRE_JOURS));

        List<Creneau> creneaux = result.creneaux();
        assertEquals(8, creneaux.size(), "4 jours travaillés × 2 créneaux = 8");
        assertTrue(creneaux.stream().noneMatch(c -> MERCREDI.equals(c.getDate())),
                "Le mercredi n'est pas coché : aucun créneau ne doit y être généré");
    }

    /**
     * [T-WR-04] Une semaine standard lun-ven ne produit aucune alerte de repos.
     */
    @Test
    void semaine_standard_ne_produit_aucune_alerte_de_repos() {
        BuildResult result = builder.build(request(LUNDI, DIMANCHE, SEMAINE_STANDARD));

        assertTrue(result.alerts().isEmpty(),
                "Repos samedi + dimanche : aucune alerte attendue, obtenu " + result.alerts());
    }

    /**
     * [T-WR-05] Aucun jour de repos reste une erreur bloquante à signaler.
     */
    @Test
    void absence_totale_de_repos_est_une_erreur() {
        BuildResult result = builder.build(request(LUNDI, DIMANCHE, Set.of(DayOfWeek.values())));

        ScenarioAlert alert = result.alerts().stream()
                .filter(a -> a.code() == AlertCode.INSUFFICIENT_WEEKLY_REST)
                .findFirst()
                .orElseThrow(() -> new AssertionError("INSUFFICIENT_WEEKLY_REST attendue"));

        assertEquals(AlertSeverity.ERROR, alert.severity());
    }

    /**
     * [T-WR-06] Week-end entièrement travaillé : le RH est reporté sur le premier
     * jour non coché, le second relève de l'horaire contractuel.
     */
    @Test
    void week_end_travaille_reporte_le_repos_hebdomadaire() {
        Set<DayOfWeek> reposDebutSemaine = Set.of(
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        );

        BuildResult result = builder.build(request(LUNDI, DIMANCHE, reposDebutSemaine));

        ScenarioAlert alert = result.alerts().stream()
                .filter(a -> a.code() == AlertCode.TOO_MANY_NON_WORKED_DAYS)
                .findFirst()
                .orElseThrow(() -> new AssertionError("TOO_MANY_NON_WORKED_DAYS attendue"));

        assertEquals(AlertSeverity.INFO, alert.severity());
        assertTrue(alert.message().contains("TUESDAY"),
                "Le lundi porte le RH reporté, le mardi est hors repos : " + alert.message());
        assertFalse(alert.message().contains("MONDAY"),
                "Le lundi porte le repos hebdomadaire, il ne doit pas être listé : " + alert.message());
    }

    /**
     * [T-WR-07] L'alerte porte sur la configuration, pas sur la semaine :
     * elle n'est pas répétée sur un horizon multi-semaines.
     */
    @Test
    void alerte_de_configuration_non_dupliquee_sur_plusieurs_semaines() {
        LocalDate finTroisiemeSemaine = LocalDate.of(2026, 8, 16);

        BuildResult result = builder.build(request(LUNDI, finTroisiemeSemaine, QUATRE_JOURS));

        long nb = result.alerts().stream()
                .filter(a -> a.code() == AlertCode.TOO_MANY_NON_WORKED_DAYS)
                .count();

        assertEquals(1, nb, "3 semaines couvertes, une seule alerte de configuration attendue");
        assertEquals(LUNDI, result.alerts().get(0).date(),
                "L'alerte est ancrée sur le début de l'horizon");
    }

    // ----------------------------------------------------------
    // Utilitaire
    // ----------------------------------------------------------

    private BuildRequest request(LocalDate debut, LocalDate fin, Set<DayOfWeek> workedDays) {
        BuildRequest req = new BuildRequest();
        req.dateDebut = debut;
        req.dateFin = fin;
        req.ressource = new SalarieReel(
                "10212", null, "CDI",
                Set.of("CASTRES"), Set.of("travail"), Set.of("PC-100")
        );
        req.dailyAmplitudeHours = 7.0;
        req.shiftStart = LocalTime.of(9, 0);
        req.shiftEndAlert = LocalTime.of(17, 0);
        req.lunchBreakStart = LocalTime.of(12, 0);
        req.lunchBreakEnd = LocalTime.of(13, 0);
        req.workedDays = workedDays;
        req.holidayDates = Set.of();
        // Code activité déclaré : ces tests portent sur le repos hebdomadaire, pas sur le repli
        // de code activité. Le déclarer évite une alerte ACTIVITY_CODE_DEFAULTED parasite.
        req.codeActiviteId = "ACT-TRAVAIL";
        return req;
    }
}
