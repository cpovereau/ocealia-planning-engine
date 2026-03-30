package fr.project.planning.scenarios.service;

import fr.project.planning.scenarios.dto.DataSetDTO;
import fr.project.planning.scenarios.dto.HorizonDTO;
import fr.project.planning.scenarios.dto.IgnoredCreneauxDTO;
import fr.project.planning.scenarios.dto.PlanningContextDTO;
import fr.project.planning.scenarios.dto.RessourcesDTO;
import fr.project.planning.scenarios.dto.ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.input.IndisponibilitesDTO;
import fr.project.planning.scenarios.dto.input.ReferentielActiviteDTO;
import fr.project.planning.scenarios.dto.input.ReferentielsDTO;
import fr.project.planning.scenarios.dto.input.SalarieInputDTO;
import fr.project.planning.scenarios.dto.request.ResourceKind;
import fr.project.planning.scenarios.dto.request.ResourceRefDTO;
import fr.project.planning.scenarios.dto.request.Sc01ScenarioParametersDTO;
import fr.project.planning.scenarios.mapper.ScenarioResourceMapper;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Phase C — IgnoredCreneauxDTO dynamique dans ScenarioSc01PreparationService.
 *
 * Vérifie :
 * - C1a : aucuneRessourceDansDataset = 0 toujours pour SC-01
 * - C1b : activiteInconnue = 0 quand le référentiel couvre "travail"
 * - C1c : activiteInconnue > 0 quand le référentiel ne contient pas "travail"
 * - C1d : horsHorizon = 0 (le builder ne génère que des créneaux dans l'horizon)
 */
class ScenarioSc01PreparationServicePhaseCTest {

    private final ScenarioSc01PreparationService service =
            new ScenarioSc01PreparationService(new ScenarioResourceMapper(), new CreneauGenerationService());

    private static final LocalDate DATE_DEBUT = LocalDate.of(2026, 5, 11);
    private static final LocalDate DATE_FIN   = LocalDate.of(2026, 5, 17);

    // ----------------------------------------------------------
    // C1a — aucuneRessourceDansDataset toujours 0 pour SC-01
    // ----------------------------------------------------------

    /**
     * [T-C1-01] SC-01 travaille avec une ressource cible explicite (resourceRef).
     * Le compteur aucuneRessourceDansDataset doit toujours être 0.
     */
    @Test
    void aucuneRessourceDansDataset_est_toujours_zero() {
        PreparedSc01Scenario scenario = assertDoesNotThrow(() -> service.prepare(requeteBase()));

        IgnoredCreneauxDTO ignored = scenario.ignoredCreneaux();
        assertNotNull(ignored);
        assertEquals(0, ignored.getAucuneRessourceDansDataset(),
                "SC-01 : aucuneRessourceDansDataset doit toujours être 0 (ressource cible explicite)");
    }

    // ----------------------------------------------------------
    // C1b — activiteInconnue = 0 quand le référentiel couvre "travail"
    // ----------------------------------------------------------

    /**
     * [T-C1-02] Quand le référentiel injecté contient l'activité "travail",
     * tous les créneaux générés sont reconnus : activiteInconnue = 0.
     */
    @Test
    void activiteInconnue_zero_quand_referentiel_couvre_travail() {
        ScenarioRequestDTO req = requeteBase();
        req.getDataSet().setReferentiels(referentielAvec("travail"));

        PreparedSc01Scenario scenario = assertDoesNotThrow(() -> service.prepare(req));

        IgnoredCreneauxDTO ignored = scenario.ignoredCreneaux();
        assertEquals(0, ignored.getActiviteInconnue(),
                "activiteInconnue doit être 0 quand le référentiel contient 'travail'");
    }

    // ----------------------------------------------------------
    // C1c — activiteInconnue > 0 quand le référentiel ne contient pas "travail"
    // ----------------------------------------------------------

    /**
     * [T-C1-03] Quand le référentiel fourni ne contient pas "travail" (p.ex. uniquement "reunion"),
     * tous les créneaux générés ont une activité inconnue : activiteInconnue > 0.
     * Le pipeline ne lève pas d'exception (diagnostic uniquement en Phase C).
     */
    @Test
    void activiteInconnue_positif_quand_referentiel_sans_travail() {
        ScenarioRequestDTO req = requeteBase();
        req.getDataSet().setReferentiels(referentielAvec("reunion"));

        PreparedSc01Scenario scenario = assertDoesNotThrow(() -> service.prepare(req),
                "Un référentiel sans 'travail' ne doit pas lever d'exception en Phase C");

        IgnoredCreneauxDTO ignored = scenario.ignoredCreneaux();
        assertTrue(ignored.getActiviteInconnue() > 0,
                "activiteInconnue doit être > 0 quand le référentiel ne contient pas 'travail'");
        assertEquals(scenario.buildResult().creneaux().size(), ignored.getActiviteInconnue(),
                "Tous les créneaux doivent être comptés comme activité inconnue");
    }

    // ----------------------------------------------------------
    // C1d — horsHorizon = 0 (le builder respecte toujours l'horizon)
    // ----------------------------------------------------------

    /**
     * [T-C1-04] Le builder SC-01 génère des créneaux strictement dans l'horizon fourni.
     * horsHorizon doit être 0.
     */
    @Test
    void horsHorizon_zero_car_builder_respecte_horizon() {
        PreparedSc01Scenario scenario = assertDoesNotThrow(() -> service.prepare(requeteBase()));

        IgnoredCreneauxDTO ignored = scenario.ignoredCreneaux();
        assertEquals(0, ignored.getHorsHorizon(),
                "Le builder SC-01 ne génère pas de créneaux hors horizon — horsHorizon doit être 0");
    }

    // ----------------------------------------------------------
    // C1e — cas nominal : tous les compteurs à 0 avec référentiel correct
    // ----------------------------------------------------------

    /**
     * [T-C1-05] Cas nominal : référentiel contenant "travail", horizon cohérent.
     * Tous les compteurs diagnostiques doivent être 0.
     */
    @Test
    void tous_les_compteurs_a_zero_en_cas_nominal() {
        ScenarioRequestDTO req = requeteBase();
        req.getDataSet().setReferentiels(referentielAvec("travail"));

        PreparedSc01Scenario scenario = assertDoesNotThrow(() -> service.prepare(req));

        IgnoredCreneauxDTO ignored = scenario.ignoredCreneaux();
        assertAll(
                () -> assertEquals(0, ignored.getHorsHorizon(),       "horsHorizon = 0"),
                () -> assertEquals(0, ignored.getAucuneRessourceDansDataset(), "aucuneRessourceDansDataset = 0"),
                () -> assertEquals(0, ignored.getActiviteInconnue(),   "activiteInconnue = 0")
        );
    }

    // ----------------------------------------------------------
    // Utilitaires
    // ----------------------------------------------------------

    private ReferentielsDTO referentielAvec(String codeActiviteId) {
        ReferentielActiviteDTO activite = new ReferentielActiviteDTO();
        activite.setCodeActiviteId(codeActiviteId);
        activite.setCompteDansCharge(true);
        activite.setGenereDetteRepos(false);
        activite.setEstServiceCritique(false);
        ReferentielsDTO referentiels = new ReferentielsDTO();
        referentiels.setActivites(new ArrayList<>(List.of(activite)));
        return referentiels;
    }

    private ScenarioRequestDTO requeteBase() {
        ScenarioRequestDTO req = new ScenarioRequestDTO();
        req.setScenarioType("SC-01");

        HorizonDTO horizon = new HorizonDTO();
        horizon.setDateDebut(DATE_DEBUT);
        horizon.setDateFin(DATE_FIN);
        PlanningContextDTO ctx = new PlanningContextDTO();
        ctx.setHorizon(horizon);
        ctx.setStrategieScoring("EXPLOITATION");
        req.setPlanningContext(ctx);

        ResourceRefDTO ref = new ResourceRefDTO();
        ref.setKind(ResourceKind.SALARIE);
        ref.setId("SAL-01");
        Sc01ScenarioParametersDTO params = new Sc01ScenarioParametersDTO();
        params.setResourceRef(ref);
        params.setDailyAmplitudeHours(8.0);
        params.setShiftStart(LocalTime.of(8, 0));
        params.setShiftEndAlert(LocalTime.of(17, 0));
        params.setWorkedDays(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
        params.setHolidayDates(Set.of());
        req.setScenarioParameters(params);

        DataSetDTO dataSet = new DataSetDTO();

        SalarieInputDTO salarie = new SalarieInputDTO();
        salarie.setId("SAL-01");
        salarie.setStatut("CDI");
        salarie.setSitesAutorises(Set.of("SITE-A"));
        salarie.setActivitesCompatibles(Set.of("travail"));

        RessourcesDTO ressources = new RessourcesDTO();
        ressources.setSalaries(new ArrayList<>(List.of(salarie)));
        ressources.setPostesVirtuels(new ArrayList<>());
        dataSet.setRessources(ressources);

        dataSet.setCreneaux(new ArrayList<>());
        dataSet.setReferentiels(null);

        IndisponibilitesDTO indisponibilites = new IndisponibilitesDTO();
        indisponibilites.setItems(new ArrayList<>());
        dataSet.setIndisponibilites(indisponibilites);

        req.setDataSet(dataSet);
        return req;
    }
}
