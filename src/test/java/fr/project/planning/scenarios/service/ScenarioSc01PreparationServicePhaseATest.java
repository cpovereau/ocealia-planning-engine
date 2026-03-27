package fr.project.planning.scenarios.service;

import fr.project.planning.scenarios.dto.DataSetDTO;
import fr.project.planning.scenarios.dto.HorizonDTO;
import fr.project.planning.scenarios.dto.PlanningContextDTO;
import fr.project.planning.scenarios.dto.RessourcesDTO;
import fr.project.planning.scenarios.dto.ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
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
 * Tests Phase A — Transparence et guards de ScenarioSc01PreparationService.
 *
 * Vérifie :
 * - les guards ERROR échouent avant toute logique métier, avec un message explicite
 * - les warnings A1/A2/A3 ne bloquent pas le pipeline (assertDoesNotThrow)
 * - la non-régression sur un cas valide standard
 */
class ScenarioSc01PreparationServicePhaseATest {

    private final ScenarioSc01PreparationService service =
            new ScenarioSc01PreparationService(new ScenarioResourceMapper());

    private static final LocalDate DATE_DEBUT = LocalDate.of(2026, 5, 11);
    private static final LocalDate DATE_FIN   = LocalDate.of(2026, 5, 17);

    // ----------------------------------------------------------
    // A4 — Guards horizon
    // ----------------------------------------------------------

    /**
     * [T-A4-01] planningContext absent → IllegalArgumentException avant solveur.
     */
    @Test
    void planningContext_null_leve_IllegalArgumentException() {
        ScenarioRequestDTO req = requeteBase();
        req.setPlanningContext(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("planningContext"),
                "Le message doit mentionner 'planningContext' — reçu : " + ex.getMessage());
    }

    /**
     * [T-A4-02] horizon absent → IllegalArgumentException avant solveur.
     */
    @Test
    void horizon_null_leve_IllegalArgumentException() {
        ScenarioRequestDTO req = requeteBase();
        req.getPlanningContext().setHorizon(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("horizon"),
                "Le message doit mentionner 'horizon' — reçu : " + ex.getMessage());
    }

    /**
     * [T-A4-03] dateDebut absente → IllegalArgumentException avant solveur.
     */
    @Test
    void dateDebut_null_leve_IllegalArgumentException() {
        ScenarioRequestDTO req = requeteBase();
        req.getPlanningContext().getHorizon().setDateDebut(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("dateDebut"),
                "Le message doit mentionner 'dateDebut' — reçu : " + ex.getMessage());
    }

    /**
     * [T-A4-04] dateFin absente → IllegalArgumentException avant solveur.
     */
    @Test
    void dateFin_null_leve_IllegalArgumentException() {
        ScenarioRequestDTO req = requeteBase();
        req.getPlanningContext().getHorizon().setDateFin(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("dateFin"),
                "Le message doit mentionner 'dateFin' — reçu : " + ex.getMessage());
    }

    /**
     * [T-A4-05] dateDebut postérieure à dateFin → IllegalArgumentException.
     * Remplace le comportement silencieux (builder générait 0 créneau sans erreur).
     */
    @Test
    void dateDebut_apres_dateFin_leve_IllegalArgumentException() {
        ScenarioRequestDTO req = requeteBase();
        req.getPlanningContext().getHorizon().setDateDebut(LocalDate.of(2026, 5, 18));
        req.getPlanningContext().getHorizon().setDateFin(LocalDate.of(2026, 5, 11));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("incohérent") || ex.getMessage().contains("postérieure"),
                "Le message doit signaler l'incohérence de l'horizon — reçu : " + ex.getMessage());
    }

    // ----------------------------------------------------------
    // A4 — Guards scenarioParameters
    // ----------------------------------------------------------

    /**
     * [T-A4-06] resourceRef absent → IllegalArgumentException.
     */
    @Test
    void resourceRef_null_leve_IllegalArgumentException() {
        ScenarioRequestDTO req = requeteBase();
        req.getScenarioParameters().setResourceRef(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("resourceRef"),
                "Le message doit mentionner 'resourceRef' — reçu : " + ex.getMessage());
    }

    /**
     * [T-A4-07] resourceRef.id vide → IllegalArgumentException.
     */
    @Test
    void resourceRef_id_blanc_leve_IllegalArgumentException() {
        ScenarioRequestDTO req = requeteBase();
        req.getScenarioParameters().getResourceRef().setId("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("resourceRef.id"),
                "Le message doit mentionner 'resourceRef.id' — reçu : " + ex.getMessage());
    }

    /**
     * [T-A4-08] dailyAmplitudeHours à zéro → IllegalArgumentException.
     */
    @Test
    void dailyAmplitudeHours_zero_leve_IllegalArgumentException() {
        ScenarioRequestDTO req = requeteBase();
        req.getScenarioParameters().setDailyAmplitudeHours(0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("dailyAmplitudeHours"),
                "Le message doit mentionner 'dailyAmplitudeHours' — reçu : " + ex.getMessage());
    }

    /**
     * [T-A4-09] shiftStart absent → IllegalArgumentException.
     */
    @Test
    void shiftStart_null_leve_IllegalArgumentException() {
        ScenarioRequestDTO req = requeteBase();
        req.getScenarioParameters().setShiftStart(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("shiftStart"),
                "Le message doit mentionner 'shiftStart' — reçu : " + ex.getMessage());
    }

    // ----------------------------------------------------------
    // A1 — Warning dataSet.creneaux ignorés
    // ----------------------------------------------------------

    /**
     * [T-A1-01] dataSet.creneaux non vide → pas d'exception, pipeline inchangé.
     * Le warn est loggué mais n'altère pas le résultat.
     */
    @Test
    void creneaux_non_vides_ne_bloquent_pas_le_pipeline() {
        ScenarioRequestDTO req = requeteBase();
        CreneauInputDTO creneau = new CreneauInputDTO();
        creneau.setId("CRE-EXTRA");
        creneau.setDate(DATE_DEBUT);
        creneau.setHeureDebut(LocalTime.of(8, 0));
        creneau.setHeureFin(LocalTime.of(16, 0));
        creneau.setCodeActiviteId("ACT-QUELCONQUE");
        req.getDataSet().setCreneaux(new ArrayList<>(List.of(creneau)));

        PreparedSc01Scenario scenario = assertDoesNotThrow(() -> service.prepare(req),
                "Un dataSet.creneaux non vide ne doit pas lever d'exception");

        // Le builder génère ses propres créneaux — les créneaux du dataset sont ignorés
        assertNotNull(scenario.planningRequest().creneaux(),
                "Les créneaux du planningRequest ne doivent pas être null");
    }

    // ----------------------------------------------------------
    // A2 — Warning dataSet.referentiels ignoré
    // ----------------------------------------------------------

    /**
     * [T-A2-01] dataSet.referentiels non vide → pas d'exception, pipeline inchangé.
     * Le warn est loggué mais le référentiel hardcodé reste actif.
     */
    @Test
    void referentiels_non_vide_ne_bloque_pas_le_pipeline() {
        ScenarioRequestDTO req = requeteBase();
        ReferentielActiviteDTO activite = new ReferentielActiviteDTO();
        activite.setCodeActiviteId("ACT-CUSTOM");
        activite.setCompteDansCharge(true);
        activite.setGenereDetteRepos(false);
        activite.setEstServiceCritique(false);
        ReferentielsDTO referentiels = new ReferentielsDTO();
        referentiels.setActivites(new ArrayList<>(List.of(activite)));
        req.getDataSet().setReferentiels(referentiels);

        assertDoesNotThrow(() -> service.prepare(req),
                "Un dataSet.referentiels non vide ne doit pas lever d'exception");
    }

    // ----------------------------------------------------------
    // Non-régression — cas valide complet inchangé
    // ----------------------------------------------------------

    /**
     * [T-NR-01] Requête valide standard → comportement inchangé après Phase A.
     */
    @Test
    void requete_valide_retourne_scenario_prepare() {
        ScenarioRequestDTO req = requeteBase();

        PreparedSc01Scenario scenario = assertDoesNotThrow(() -> service.prepare(req));

        assertNotNull(scenario, "Le scenario préparé ne doit pas être null");
        assertNotNull(scenario.planningRequest(), "Le planningRequest ne doit pas être null");
        assertEquals("SC-01", scenario.scenarioType());
        assertEquals("SAL-01", scenario.resourceId());
    }

    // ----------------------------------------------------------
    // Utilitaires
    // ----------------------------------------------------------

    /**
     * Construit une requête SC-01 minimale et valide.
     * Toutes les modifications de test partent de cette base.
     */
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
