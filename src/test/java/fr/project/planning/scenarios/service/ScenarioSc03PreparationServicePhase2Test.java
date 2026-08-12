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
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Phase 2 — politique sur les activités inconnues
 *
 * Vérifie que les créneaux dont l'activité est absente du référentiel
 * sont réellement exclus avant solveur (et non plus seulement comptés).
 */
class ScenarioDatasetPreparationServicePhase2Test {

    private final ScenarioDatasetPreparationService service =
            new ScenarioDatasetPreparationService(new ScenarioResourceMapper(), new ScenarioCreneauMapper());

    private static final LocalDate DATE_DEBUT = LocalDate.of(2026, 5, 12);
    private static final LocalDate DATE_FIN   = LocalDate.of(2026, 5, 18);

    // ----------------------------------------------------------
    // Cas 1 — exclusion réelle avant solveur
    // ----------------------------------------------------------

    @Test
    void activiteInconnue_estExcluAvantSolveur() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        creneauDansDataset(request, "CRE-CONNU",   "ACT-CONNU",   null);
        creneauDansDataset(request, "CRE-INCONNU", "ACT-INCONNU", null);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(1, scenario.planningRequest().creneaux().size(),
                "Seul le créneau à activité connue doit être transmis au solveur");
        assertEquals("CRE-CONNU", scenario.planningRequest().creneaux().get(0).getId(),
                "Le créneau transmis doit être celui à activité connue");
    }

    @Test
    void activiteInconnue_estCompteInIgnoredCreneaux() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        creneauDansDataset(request, "CRE-CONNU",   "ACT-CONNU",   null);
        creneauDansDataset(request, "CRE-INCONNU", "ACT-INCONNU", null);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(1, scenario.ignoredCreneaux().getActiviteInconnue(),
                "Le créneau exclu doit être comptabilisé dans ignoredCreneaux.activiteInconnue");
    }

    // ----------------------------------------------------------
    // Cas 2 — créneau connu continue de passer
    // ----------------------------------------------------------

    @Test
    void activiteConnue_passeToujoursAuSolveur() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        creneauDansDataset(request, "CRE-01", "ACT-CONNU", null);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(1, scenario.planningRequest().creneaux().size());
        assertEquals(0, scenario.ignoredCreneaux().getActiviteInconnue());
    }

    // ----------------------------------------------------------
    // Cas 3 — mix connu / inconnu
    // ----------------------------------------------------------

    @Test
    void mixte_1Connu1Inconnu_seulConnnuPasseAuSolveur() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        creneauDansDataset(request, "CRE-A", "ACT-CONNU",   null);
        creneauDansDataset(request, "CRE-B", "ACT-INCONNU", null);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(1, scenario.planningRequest().creneaux().size(),
                "1 créneau valide attendu côté solveur");
        assertEquals(1, scenario.ignoredCreneaux().getActiviteInconnue(),
                "1 créneau exclu attendu dans les diagnostics");
    }

    // ----------------------------------------------------------
    // Cas 4 — fallback codeActiviteId→activite : activité connue
    // ----------------------------------------------------------

    @Test
    void fallback_activiteConnueViaChampActivite_passeToujoursAuSolveur() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        // codeActiviteId absent, activite présente dans le référentiel → fallback réussi
        creneauDansDataset(request, "CRE-FALLBACK-OK", null, "ACT-CONNU");
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(1, scenario.planningRequest().creneaux().size(),
                "Fallback réussi : le créneau doit passer au solveur");
        assertEquals(0, scenario.ignoredCreneaux().getActiviteInconnue());
    }

    // ----------------------------------------------------------
    // Cas 5 — fallback échoué : activité inconnue même via fallback
    // ----------------------------------------------------------

    @Test
    void fallback_activiteInconnueViaChampActivite_estExclu() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        // codeActiviteId absent, activite non référencée → fallback échoué
        creneauDansDataset(request, "CRE-FALLBACK-KO", null, "ACT-INCONNUE");
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(0, scenario.planningRequest().creneaux().size(),
                "Fallback échoué : le créneau doit être exclu avant solveur");
        assertEquals(1, scenario.ignoredCreneaux().getActiviteInconnue());
    }

    // ----------------------------------------------------------
    // Cas 6 — tous les créneaux inconnus : solveur reçoit liste vide sans exception
    // ----------------------------------------------------------

    @Test
    void tousCreneauxInconnus_solveurRecoit0Creneaux_sansBlocage() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        creneauDansDataset(request, "CRE-A", "ACT-INCONNUE-1", null);
        creneauDansDataset(request, "CRE-B", "ACT-INCONNUE-2", null);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(0, scenario.planningRequest().creneaux().size(),
                "Aucun créneau valide : le solveur reçoit une liste vide");
        assertEquals(2, scenario.ignoredCreneaux().getActiviteInconnue());
    }

    // ----------------------------------------------------------
    // Cas 7 — plusieurs créneaux connus, zéro exclu
    // ----------------------------------------------------------

    @Test
    void plusieursCreneauxConnus_tousPassentAuSolveur() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        creneauDansDataset(request, "CRE-1", "ACT-A", null);
        creneauDansDataset(request, "CRE-2", "ACT-B", null);
        creneauDansDataset(request, "CRE-3", "ACT-A", null);
        activiteInReferentiel(request, "ACT-A");
        activiteInReferentiel(request, "ACT-B");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(3, scenario.planningRequest().creneaux().size());
        assertEquals(0, scenario.ignoredCreneaux().getActiviteInconnue());
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

        dataSet.setCreneaux(new ArrayList<>());
        req.setDataSet(dataSet);
        return req;
    }

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
