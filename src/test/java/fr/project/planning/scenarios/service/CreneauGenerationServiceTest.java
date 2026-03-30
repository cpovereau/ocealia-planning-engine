package fr.project.planning.scenarios.service;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scenarios.builder.ScenarioDatasetBuilderSc01;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de CreneauGenerationService.
 *
 * Vérifie :
 * - la délégation correcte au builder SC-01
 * - que le service produit des créneaux avec codeActiviteId renseigné
 * - que le résultat est cohérent avec les paramètres fournis
 */
class CreneauGenerationServiceTest {

    private final CreneauGenerationService service = new CreneauGenerationService();

    private static final LocalDate LUNDI    = LocalDate.of(2026, 5, 11); // lundi
    private static final LocalDate VENDREDI = LocalDate.of(2026, 5, 15); // vendredi

    /**
     * [T-CGS-01] Le service produit des créneaux pour une semaine de travail standard.
     */
    @Test
    void genere_des_creneaux_pour_une_semaine_standard() {
        ScenarioDatasetBuilderSc01.BuildRequest req = buildRequest(LUNDI, VENDREDI);

        ScenarioDatasetBuilderSc01.BuildResult result = assertDoesNotThrow(() -> service.generate(req));

        assertFalse(result.creneaux().isEmpty(), "Le service doit générer au moins un créneau");
    }

    /**
     * [T-CGS-02] Tous les créneaux générés ont un codeActiviteId = "travail" (non null).
     */
    @Test
    void creneaux_generes_ont_codeActiviteId_non_null() {
        ScenarioDatasetBuilderSc01.BuildResult result = service.generate(buildRequest(LUNDI, VENDREDI));

        List<Creneau> creneaux = result.creneaux();
        assertFalse(creneaux.isEmpty());
        assertTrue(creneaux.stream().allMatch(c -> "travail".equals(c.getCodeActiviteId())),
                "Tous les créneaux doivent avoir codeActiviteId = 'travail'");
    }

    /**
     * [T-CGS-03] Sur une semaine lun-ven avec pause midi, le builder génère 2 créneaux par jour travaillé.
     */
    @Test
    void genere_deux_creneaux_par_jour_avec_pause_midi() {
        ScenarioDatasetBuilderSc01.BuildRequest req = buildRequest(LUNDI, VENDREDI);
        // lundi-vendredi = 5 jours travaillés, chacun découpé matin + après-midi = 10 créneaux
        ScenarioDatasetBuilderSc01.BuildResult result = service.generate(req);

        assertEquals(10, result.creneaux().size(),
                "5 jours travaillés × 2 créneaux (matin/aprem) = 10 créneaux attendus");
    }

    /**
     * [T-CGS-04] Les alertes de configuration sont accessibles dans le résultat.
     */
    @Test
    void result_expose_les_alertes() {
        ScenarioDatasetBuilderSc01.BuildResult result = service.generate(buildRequest(LUNDI, VENDREDI));

        assertNotNull(result.alerts(), "La liste d'alertes ne doit pas être null");
    }

    // ----------------------------------------------------------
    // Utilitaire
    // ----------------------------------------------------------

    private ScenarioDatasetBuilderSc01.BuildRequest buildRequest(LocalDate debut, LocalDate fin) {
        SalarieReel ressource = new SalarieReel(
                "SAL-01", null, "CDI",
                Set.of("SITE-A"), Set.of("travail"), Set.of()
        );

        ScenarioDatasetBuilderSc01.BuildRequest req = new ScenarioDatasetBuilderSc01.BuildRequest();
        req.dateDebut = debut;
        req.dateFin = fin;
        req.ressource = ressource;
        req.dailyAmplitudeHours = 8.0;
        req.shiftStart = LocalTime.of(8, 0);
        req.shiftEndAlert = LocalTime.of(17, 0);
        req.lunchBreakStart = LocalTime.of(12, 0);
        req.lunchBreakEnd = LocalTime.of(13, 0);
        req.workedDays = Set.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        );
        req.holidayDates = Set.of();
        return req;
    }
}
