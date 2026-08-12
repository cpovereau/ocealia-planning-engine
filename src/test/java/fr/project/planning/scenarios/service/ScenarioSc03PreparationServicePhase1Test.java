package fr.project.planning.scenarios.service;

import fr.project.planning.scenarios.dto.DataSetDTO;
import fr.project.planning.scenarios.dto.HorizonDTO;
import fr.project.planning.scenarios.dto.PlanningContextDTO;
import fr.project.planning.scenarios.dto.RessourcesDTO;
import fr.project.planning.scenarios.dto.Sc03ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
import fr.project.planning.scenarios.dto.input.IndisponibilitesDTO;
import fr.project.planning.scenarios.dto.input.ReferentielActiviteDTO;
import fr.project.planning.scenarios.dto.input.ReferentielsDTO;
import fr.project.planning.scenarios.dto.input.SalarieInputDTO;
import fr.project.planning.scenarios.mapper.ScenarioCreneauMapper;
import fr.project.planning.scenarios.mapper.ScenarioResourceMapper;
import fr.project.planning.scenarios.dto.request.Sc03ScenarioParametersDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Phase 1 visibilité — ScenarioDatasetPreparationService
 *
 * Vérifie les comportements implicites rendus observables en Phase 1 :
 * - activité inconnue : compteur correct, créneau transmis au solveur
 * - fallback codeActiviteId→activite : activité comptée comme inconnue si activite non référencée
 * - prioriteCouverture et periode non exploités : pas d'effet sur le PlanningContext
 */
class ScenarioDatasetPreparationServicePhase1Test {

    private final ScenarioDatasetPreparationService service =
            new ScenarioDatasetPreparationService(new ScenarioResourceMapper(), new ScenarioCreneauMapper());

    private static final LocalDate DATE_DEBUT = LocalDate.of(2026, 5, 12);
    private static final LocalDate DATE_FIN   = LocalDate.of(2026, 5, 18);

    // ----------------------------------------------------------
    // Sujet 1 — Activité inconnue : compteur et non-rejet du créneau
    // ----------------------------------------------------------

    @Test
    void activiteInconnue_incrementeCompteurMaisNeBloquesPas() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        // Un créneau avec activité absente du référentiel
        creneauDansDataset(request, "CRE-01", "ACT-CONNU", null);
        creneauDansDataset(request, "CRE-02", "ACT-INCONNU", null);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(1, scenario.ignoredCreneaux().getActiviteInconnue(),
                "Un seul créneau avec activité inconnue attendu");
        assertEquals(1, scenario.planningRequest().creneaux().size(),
                "Seul le créneau à activité connue est transmis au solveur (Phase 2 : exclusion réelle)");
    }

    @Test
    void activiteInconnue_codeActiviteIdNullEtActiviteNonReferencee_compteCommeInconnu() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        // codeActiviteId absent, activite non présente dans le référentiel → fallback + activité inconnue
        creneauDansDataset(request, "CRE-FALLBACK", null, "LibelleNonReference");
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(1, scenario.ignoredCreneaux().getActiviteInconnue());
    }

    // ----------------------------------------------------------
    // Sujet 2 — Fallback codeActiviteId→activite : comportement observable
    // ----------------------------------------------------------

    @Test
    void fallback_codeActiviteIdNull_activiteReferencee_pasCompteeInconnue() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        // codeActiviteId absent, activite présente dans le référentiel → fallback réussi
        creneauDansDataset(request, "CRE-FALLBACK-OK", null, "ACT-CONNU");
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(0, scenario.ignoredCreneaux().getActiviteInconnue(),
                "Le fallback sur activite réussit : le créneau n'est pas comptabilisé comme activité inconnue");
    }

    @Test
    void fallback_codeActiviteIdPresent_prioritaireSurActivite() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        // codeActiviteId présent et dans le référentiel, activite différente (non référencée)
        creneauDansDataset(request, "CRE-PRIO", "ACT-CONNU", "LibelleIncorrecte");
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(0, scenario.ignoredCreneaux().getActiviteInconnue(),
                "codeActiviteId est prioritaire sur activite pour la résolution référentiel");
    }

    // ----------------------------------------------------------
    // Sujet 4 — prioriteCouverture et periode non exploités
    // ----------------------------------------------------------

    @Test
    void prioriteCouverture_nAucunEffetSurLePlanningContext() {
        Sc03ScenarioRequestDTO requestSans = requeteMinimale();
        creneauDansDataset(requestSans, "CRE-01", "ACT-CONNU", null);
        activiteInReferentiel(requestSans, "ACT-CONNU");

        Sc03ScenarioRequestDTO requestAvec = requeteMinimale();
        creneauDansDataset(requestAvec, "CRE-01", "ACT-CONNU", null);
        activiteInReferentiel(requestAvec, "ACT-CONNU");
        Sc03ScenarioParametersDTO params = new Sc03ScenarioParametersDTO();
        params.setPrioriteCouverture("CRITIQUE");
        requestAvec.setScenarioParameters(params);

        PreparedDatasetScenario scenarioSans  = service.prepare(requestSans);
        PreparedDatasetScenario scenarioAvec  = service.prepare(requestAvec);

        assertEquals(
                scenarioSans.planningRequest().planningContext().getStrategieScoring(),
                scenarioAvec.planningRequest().planningContext().getStrategieScoring(),
                "prioriteCouverture ne doit pas modifier la strategieScoring du PlanningContext"
        );
        assertEquals(
                scenarioSans.planningRequest().planningContext().getHorizonTemporel().getDateDebut(),
                scenarioAvec.planningRequest().planningContext().getHorizonTemporel().getDateDebut(),
                "prioriteCouverture ne doit pas modifier l'horizon"
        );
    }

    @Test
    void periode_nAucunEffetSurLHorizonDuPlanningContext() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        creneauDansDataset(request, "CRE-01", "ACT-CONNU", null);
        activiteInReferentiel(request, "ACT-CONNU");

        HorizonDTO horizonOverride = new HorizonDTO();
        horizonOverride.setDateDebut(LocalDate.of(2099, 1, 1));
        horizonOverride.setDateFin(LocalDate.of(2099, 12, 31));
        Sc03ScenarioParametersDTO params = new Sc03ScenarioParametersDTO();
        params.setPeriode(horizonOverride);
        request.setScenarioParameters(params);

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(DATE_DEBUT, scenario.planningRequest().planningContext().getHorizonTemporel().getDateDebut(),
                "La periode de scenarioParameters ne doit pas surcharger planningContext.horizon (non implémenté)");
        assertEquals(DATE_FIN, scenario.planningRequest().planningContext().getHorizonTemporel().getDateFin());
    }

    // ----------------------------------------------------------
    // Utilitaires
    // ----------------------------------------------------------

    private Sc03ScenarioRequestDTO requeteMinimale() {
        Sc03ScenarioRequestDTO req = new Sc03ScenarioRequestDTO();
        req.setScenarioType("SC-03");

        HorizonDTO horizon = new HorizonDTO();
        horizon.setDateDebut(DATE_DEBUT);
        horizon.setDateFin(DATE_FIN);
        PlanningContextDTO ctx = new PlanningContextDTO();
        ctx.setHorizon(horizon);
        ctx.setStrategieScoring("ANALYSE_RH");
        req.setPlanningContext(ctx);

        DataSetDTO dataSet = new DataSetDTO();
        RessourcesDTO ressources = new RessourcesDTO();
        SalarieInputDTO sal = new SalarieInputDTO();
        sal.setId("SAL-TEST");
        sal.setStatut("CDI");
        sal.setSitesAutorises(Set.of("SITE-A"));
        ressources.setSalaries(List.of(sal));
        ressources.setPostesVirtuels(List.of());
        dataSet.setRessources(ressources);

        ReferentielsDTO referentiels = new ReferentielsDTO();
        referentiels.setActivites(new ArrayList<>());
        dataSet.setReferentiels(referentiels);

        IndisponibilitesDTO indisponibilites = new IndisponibilitesDTO();
        indisponibilites.setItems(new ArrayList<>());
        dataSet.setIndisponibilites(indisponibilites);

        // creneaux initialisé à liste vide — sera alimenté par creneauDansDataset()
        dataSet.setCreneaux(new ArrayList<>());

        req.setDataSet(dataSet);
        return req;
    }

    /** Ajoute un créneau dans le dataSet de la requête. */
    private void creneauDansDataset(Sc03ScenarioRequestDTO req, String id, String codeActiviteId, String activite) {
        CreneauInputDTO dto = new CreneauInputDTO();
        dto.setId(id);
        dto.setDate(DATE_DEBUT);
        dto.setHeureDebut(LocalTime.of(8, 0));
        dto.setHeureFin(LocalTime.of(16, 0));
        dto.setCodeActiviteId(codeActiviteId);
        dto.setActivite(activite);
        req.getDataSet().getCreneaux().add(dto);
    }

    /** Ajoute une activité au référentiel du dataSet. */
    private void activiteInReferentiel(Sc03ScenarioRequestDTO req, String codeActiviteId) {
        ReferentielActiviteDTO activite = new ReferentielActiviteDTO();
        activite.setCodeActiviteId(codeActiviteId);
        activite.setLibelle(codeActiviteId);
        activite.setCompteDansCharge(true);
        activite.setGenereDetteRepos(false);
        activite.setEstServiceCritique(false);
        req.getDataSet().getReferentiels().getActivites().add(activite);
    }
}
