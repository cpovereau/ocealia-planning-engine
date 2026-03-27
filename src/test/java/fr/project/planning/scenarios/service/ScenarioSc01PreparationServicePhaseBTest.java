package fr.project.planning.scenarios.service;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.scenarios.dto.DataSetDTO;
import fr.project.planning.scenarios.dto.HorizonDTO;
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
 * Tests Phase B — Injection du référentiel dans ScenarioSc01PreparationService.
 *
 * Vérifie :
 * - B1 : le référentiel fourni dans dataSet.referentiels est injecté dans le planningRequest
 * - B2 : si referentiels absent ou vide, fallback "travail" sans exception
 * - B3 : les créneaux générés portent codeActiviteId = "travail"
 * - Cohérence B1+B3 : le référentiel injecté couvre bien le codeActiviteId des créneaux
 */
class ScenarioSc01PreparationServicePhaseBTest {

    private final ScenarioSc01PreparationService service =
            new ScenarioSc01PreparationService(new ScenarioResourceMapper());

    private static final LocalDate DATE_DEBUT = LocalDate.of(2026, 5, 11);
    private static final LocalDate DATE_FIN   = LocalDate.of(2026, 5, 17);

    // ----------------------------------------------------------
    // B1 — Référentiel fourni → injecté dans le planningRequest
    // ----------------------------------------------------------

    /**
     * [T-B1-01] Quand dataSet.referentiels est fourni avec l'activité "travail",
     * le planningRequest doit contenir un référentiel avec cette activité.
     */
    @Test
    void referentiel_fourni_est_injecte_dans_planning_request() {
        ScenarioRequestDTO req = requeteBase();
        req.getDataSet().setReferentiels(referentielAvecTravail());

        PreparedSc01Scenario scenario = assertDoesNotThrow(() -> service.prepare(req));

        ReferentielComptabiliteActivite ref = scenario.planningRequest().referentielComptabiliteActivite();
        assertTrue(ref.contient("travail"),
                "Le référentiel injecté doit contenir l'activité 'travail'");
        assertTrue(ref.getByCode("travail").isCompteDansCharge(),
                "L'activité 'travail' doit avoir compteDansCharge=true");
    }

    // ----------------------------------------------------------
    // B2 — Référentiel absent → fallback "travail"
    // ----------------------------------------------------------

    /**
     * [T-B2-01] Quand dataSet.referentiels est null,
     * SC-01 ne doit pas lever d'exception et le fallback doit contenir "travail".
     */
    @Test
    void referentiel_absent_declenche_fallback_avec_activite_travail() {
        ScenarioRequestDTO req = requeteBase();
        req.getDataSet().setReferentiels(null);

        PreparedSc01Scenario scenario = assertDoesNotThrow(() -> service.prepare(req),
                "Un referentiels absent ne doit pas lever d'exception");

        ReferentielComptabiliteActivite ref = scenario.planningRequest().referentielComptabiliteActivite();
        assertTrue(ref.contient("travail"),
                "Le fallback doit contenir 'travail' pour la compatibilité SC-01");
        assertTrue(ref.getByCode("travail").isCompteDansCharge(),
                "L'activité 'travail' du fallback doit avoir compteDansCharge=true");
    }

    /**
     * [T-B2-02] Quand dataSet.referentiels est fourni mais avec une liste vide,
     * le fallback doit s'appliquer (même comportement que l'absence).
     */
    @Test
    void referentiel_vide_declenche_fallback() {
        ScenarioRequestDTO req = requeteBase();
        ReferentielsDTO referentiels = new ReferentielsDTO();
        referentiels.setActivites(new ArrayList<>());
        req.getDataSet().setReferentiels(referentiels);

        PreparedSc01Scenario scenario = assertDoesNotThrow(() -> service.prepare(req),
                "Un referentiels vide ne doit pas lever d'exception");

        assertTrue(scenario.planningRequest().referentielComptabiliteActivite().contient("travail"),
                "Le fallback doit contenir 'travail' quand referentiels est vide");
    }

    // ----------------------------------------------------------
    // B3 — Créneaux générés portent codeActiviteId = "travail"
    // ----------------------------------------------------------

    /**
     * [T-B3-01] Les créneaux générés par le builder SC-01 doivent tous
     * avoir codeActiviteId = "travail" (clé de lookup référentiel explicite).
     */
    @Test
    void creneaux_generes_ont_codeActiviteId_travail() {
        ScenarioRequestDTO req = requeteBase();

        PreparedSc01Scenario scenario = assertDoesNotThrow(() -> service.prepare(req));

        List<Creneau> creneaux = scenario.buildResult().creneaux();
        assertFalse(creneaux.isEmpty(), "Le builder doit générer au moins un créneau");
        assertTrue(creneaux.stream().allMatch(c -> "travail".equals(c.getCodeActiviteId())),
                "Tous les créneaux SC-01 doivent avoir codeActiviteId = 'travail'");
    }

    // ----------------------------------------------------------
    // Cohérence B1+B3 — Référentiel fourni couvre les créneaux
    // ----------------------------------------------------------

    /**
     * [T-B1B3-01] Le référentiel fourni doit couvrir le codeActiviteId de tous les créneaux générés.
     * Vérifie la cohérence end-to-end : pas de créneau avec activité inconnue.
     */
    @Test
    void referentiel_fourni_couvre_codeActiviteId_des_creneaux() {
        ScenarioRequestDTO req = requeteBase();
        req.getDataSet().setReferentiels(referentielAvecTravail());

        PreparedSc01Scenario scenario = assertDoesNotThrow(() -> service.prepare(req));

        ReferentielComptabiliteActivite ref = scenario.planningRequest().referentielComptabiliteActivite();
        List<Creneau> creneaux = scenario.buildResult().creneaux();

        assertFalse(creneaux.isEmpty(), "Le builder doit générer au moins un créneau");
        for (Creneau c : creneaux) {
            String code = c.getCodeActiviteId();
            assertNotNull(code, "Le codeActiviteId ne doit pas être null sur un créneau SC-01");
            assertTrue(ref.contient(code),
                    "Le référentiel doit couvrir le codeActiviteId '" + code + "' — sinon minutesTravaillees = 0");
        }
    }

    // ----------------------------------------------------------
    // Utilitaires
    // ----------------------------------------------------------

    private ReferentielsDTO referentielAvecTravail() {
        ReferentielActiviteDTO activite = new ReferentielActiviteDTO();
        activite.setCodeActiviteId("travail");
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
