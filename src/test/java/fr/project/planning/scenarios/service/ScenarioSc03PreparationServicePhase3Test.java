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
 * Tests Phase 3 — exclusion réelle des créneaux hors-horizon et rebasage de aucuneRessourceDansDataset.
 *
 * Vérifie :
 * - les créneaux hors-horizon sont exclus avant solveur (activité connue)
 * - l'ordre de partition : activiteInconnue d'abord, horsHorizon ensuite
 * - aucuneRessourceDansDataset est calculé sur les créneaux réellement transmis au solveur
 * - cas limite : date null ni comptée, ni exclue
 */
class ScenarioDatasetPreparationServicePhase3Test {

    private final ScenarioDatasetPreparationService service =
            new ScenarioDatasetPreparationService(new ScenarioResourceMapper(), new ScenarioCreneauMapper());

    private static final LocalDate DATE_DEBUT = LocalDate.of(2026, 5, 12);
    private static final LocalDate DATE_FIN   = LocalDate.of(2026, 5, 18);

    private static final LocalDate AVANT_HORIZON = LocalDate.of(2026, 5, 11);
    private static final LocalDate APRES_HORIZON = LocalDate.of(2026, 5, 19);
    private static final LocalDate DANS_HORIZON  = DATE_DEBUT;

    // ----------------------------------------------------------
    // Cas 1 — exclusion réelle avant solveur
    // ----------------------------------------------------------

    @Test
    void creneauHorsHorizon_activiteConnue_estExcluAvantSolveur() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        creneauDansDataset(request, "CRE-AVANT", "ACT-CONNU", AVANT_HORIZON);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(0, scenario.planningRequest().creneaux().size(),
                "Le créneau hors-horizon doit être exclu avant solveur");
        assertEquals(1, scenario.ignoredCreneaux().getHorsHorizon());
    }

    @Test
    void creneauApresHorizon_activiteConnue_estExcluAvantSolveur() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        creneauDansDataset(request, "CRE-APRES", "ACT-CONNU", APRES_HORIZON);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(0, scenario.planningRequest().creneaux().size());
        assertEquals(1, scenario.ignoredCreneaux().getHorsHorizon());
    }

    // ----------------------------------------------------------
    // Cas 2 — créneau dans l'horizon : non exclu
    // ----------------------------------------------------------

    @Test
    void creneauDansHorizon_activiteConnue_passeToujoursAuSolveur() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        creneauDansDataset(request, "CRE-OK", "ACT-CONNU", DANS_HORIZON);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(1, scenario.planningRequest().creneaux().size());
        assertEquals(0, scenario.ignoredCreneaux().getHorsHorizon());
    }

    // ----------------------------------------------------------
    // Cas 3 — mix dans-horizon / hors-horizon
    // ----------------------------------------------------------

    @Test
    void mixte_1DansHorizon_1HorsHorizon_seulDansHorizonPasseAuSolveur() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        creneauDansDataset(request, "CRE-OK",    "ACT-CONNU", DANS_HORIZON);
        creneauDansDataset(request, "CRE-AVANT", "ACT-CONNU", AVANT_HORIZON);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(1, scenario.planningRequest().creneaux().size());
        assertEquals("CRE-OK", scenario.planningRequest().creneaux().get(0).getId());
        assertEquals(1, scenario.ignoredCreneaux().getHorsHorizon());
    }

    @Test
    void tousCreneauxHorsHorizon_solveurRecoit0Creneaux_sansBlocage() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        creneauDansDataset(request, "CRE-A", "ACT-CONNU", AVANT_HORIZON);
        creneauDansDataset(request, "CRE-B", "ACT-CONNU", APRES_HORIZON);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(0, scenario.planningRequest().creneaux().size());
        assertEquals(2, scenario.ignoredCreneaux().getHorsHorizon());
    }

    // ----------------------------------------------------------
    // Cas 4 — ordre de partition : activiteInconnue prime sur horsHorizon
    // ----------------------------------------------------------

    @Test
    void creneauInconnu_etHorsHorizon_compteUniquementDansActiviteInconnue() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        creneauDansDataset(request, "CRE-DOUBLE", "ACT-INCONNUE", AVANT_HORIZON);
        activiteInReferentiel(request, "ACT-CONNU"); // ACT-INCONNUE n'est pas dans le référentiel

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(0, scenario.planningRequest().creneaux().size());
        assertEquals(1, scenario.ignoredCreneaux().getActiviteInconnue(),
                "Le créneau doit être compté dans activiteInconnue");
        assertEquals(0, scenario.ignoredCreneaux().getHorsHorizon(),
                "Le créneau ne doit pas être double-compté dans horsHorizon");
    }

    // ----------------------------------------------------------
    // Cas 5 — aucuneRessourceDansDataset rebas sur créneaux transmis
    // ----------------------------------------------------------

    @Test
    void aucuneRessourceDansDataset_creneauHorsHorizon_nonCompte() {
        Sc03ScenarioRequestDTO request = requeteMinimaleAvecSalarieRestreint("ACT-AUTRE");
        // Créneau hors-horizon dont ACT-CONNU n'est pas accepté par le salarie → exclu avant comptage
        creneauDansDataset(request, "CRE-HORS", "ACT-CONNU", AVANT_HORIZON);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(0, scenario.ignoredCreneaux().getAucuneRessourceDansDataset(),
                "Un créneau hors-horizon ne doit pas être compté dans aucuneRessourceDansDataset");
    }

    @Test
    void aucuneRessourceDansDataset_creneauInconnu_nonCompte() {
        Sc03ScenarioRequestDTO request = requeteMinimaleAvecSalarieRestreint("ACT-AUTRE");
        // Créneau à activité inconnue → exclu avant comptage
        creneauDansDataset(request, "CRE-INCONNU", "ACT-INCONNUE", DANS_HORIZON);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(0, scenario.ignoredCreneaux().getAucuneRessourceDansDataset(),
                "Un créneau à activité inconnue ne doit pas être compté dans aucuneRessourceDansDataset");
    }

    @Test
    void aucuneRessourceDansDataset_creneauTransmis_sansRessourceCompatible_estCompte() {
        Sc03ScenarioRequestDTO request = requeteMinimaleAvecSalarieRestreint("ACT-AUTRE");
        // Créneau dans l'horizon, activité connue, mais le salarié n'accepte pas ACT-CONNU
        creneauDansDataset(request, "CRE-OK", "ACT-CONNU", DANS_HORIZON);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        assertEquals(1, scenario.planningRequest().creneaux().size(),
                "Le créneau doit être transmis au solveur");
        assertEquals(1, scenario.ignoredCreneaux().getAucuneRessourceDansDataset(),
                "Le créneau transmis sans ressource compatible doit être compté");
    }

    // ----------------------------------------------------------
    // Cas 6 — date null : ni comptée, ni exclue (hors périmètre Phase 3)
    // ----------------------------------------------------------

    @Test
    void creneauDateNull_nEstNiCompteNiExcluCommeHorsHorizon() {
        Sc03ScenarioRequestDTO request = requeteMinimale();
        // date null : le créneau n'est pas évalué par la partition horizon
        creneauDansDataset(request, "CRE-NULL-DATE", "ACT-CONNU", null);
        activiteInReferentiel(request, "ACT-CONNU");

        PreparedDatasetScenario scenario = service.prepare(request);

        // le créneau passe au solveur (activité connue, date null non bloquante)
        assertEquals(1, scenario.planningRequest().creneaux().size());
        assertEquals(0, scenario.ignoredCreneaux().getHorsHorizon(),
                "Un créneau à date null ne doit pas être compté dans horsHorizon");
    }

    // ----------------------------------------------------------
    // Utilitaires
    // ----------------------------------------------------------

    private Sc03ScenarioRequestDTO requeteMinimale() {
        return requeteMinimaleAvecSalarieRestreint(null);
    }

    /**
     * Requête minimale valide avec un salarié dont les activités compatibles sont restreintes.
     * Si {@code activiteCompatible} est null, le salarié accepte toutes les activités (liste vide).
     */
    private Sc03ScenarioRequestDTO requeteMinimaleAvecSalarieRestreint(String activiteCompatible) {
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
        if (activiteCompatible != null) {
            sal.setActivitesCompatibles(Set.of(activiteCompatible));
        }
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

    private void creneauDansDataset(Sc03ScenarioRequestDTO req, String id, String codeActiviteId, LocalDate date) {
        CreneauInputDTO dto = new CreneauInputDTO();
        dto.setId(id);
        dto.setDate(date);
        dto.setHeureDebut(LocalTime.of(8, 0));
        dto.setHeureFin(LocalTime.of(16, 0));
        dto.setCodeActiviteId(codeActiviteId);
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
