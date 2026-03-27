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
 * Tests Phase 7 — Durcissement progressif de la validation du contrat d'entrée SC-03.
 *
 * Vérifie :
 * - les guards ERROR échouent avant solveur avec un message explicite
 * - les signaux WARN ne bloquent pas le pipeline
 * - le comportement solveur sur les cas valides est inchangé (non-régression)
 *
 * Cas volontairement hors périmètre :
 * - creneau.date == null (toléré, cas limite Phase 3 documenté)
 * - salarie.id == null (WARN logué, pas un rejet)
 */
class ScenarioSc03PreparationServicePhase7Test {

    private final ScenarioSc03PreparationService service =
            new ScenarioSc03PreparationService(new ScenarioResourceMapper(), new ScenarioCreneauMapper());

    private static final LocalDate DATE_DEBUT = LocalDate.of(2026, 6, 2);
    private static final LocalDate DATE_FIN   = LocalDate.of(2026, 6, 8);

    // ----------------------------------------------------------
    // ERROR — horizon
    // ----------------------------------------------------------

    /**
     * [T-P7-01] horizon absent → IllegalArgumentException avant solveur.
     * Remplace la NPE silencieuse sur getHorizon().getDateDebut().
     */
    @Test
    void horizon_null_leve_IllegalArgumentException() {
        Sc03ScenarioRequestDTO req = requeteBase();
        creneauDansDataset(req, "CRE-01", "ACT-CONNU");
        req.getPlanningContext().setHorizon(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("horizon"),
                "Le message doit mentionner 'horizon' — message reçu : " + ex.getMessage());
    }

    /**
     * [T-P7-02] dateDebut absente → IllegalArgumentException avant solveur.
     */
    @Test
    void dateDebut_null_leve_IllegalArgumentException() {
        Sc03ScenarioRequestDTO req = requeteBase();
        creneauDansDataset(req, "CRE-01", "ACT-CONNU");
        req.getPlanningContext().getHorizon().setDateDebut(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("dateDebut"),
                "Le message doit mentionner 'dateDebut' — message reçu : " + ex.getMessage());
    }

    /**
     * [T-P7-03] dateFin absente → IllegalArgumentException avant solveur.
     */
    @Test
    void dateFin_null_leve_IllegalArgumentException() {
        Sc03ScenarioRequestDTO req = requeteBase();
        creneauDansDataset(req, "CRE-01", "ACT-CONNU");
        req.getPlanningContext().getHorizon().setDateFin(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("dateFin"),
                "Le message doit mentionner 'dateFin' — message reçu : " + ex.getMessage());
    }

    /**
     * [T-P7-04] dateDebut postérieure à dateFin → IllegalArgumentException.
     * Remplace le comportement silencieux (tous créneaux exclus comme hors-horizon).
     */
    @Test
    void dateDebut_apres_dateFin_leve_IllegalArgumentException() {
        Sc03ScenarioRequestDTO req = requeteBase();
        creneauDansDataset(req, "CRE-01", "ACT-CONNU");
        req.getPlanningContext().getHorizon().setDateDebut(LocalDate.of(2026, 6, 10));
        req.getPlanningContext().getHorizon().setDateFin(LocalDate.of(2026, 6, 5));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("incohérent") || ex.getMessage().contains("postérieure"),
                "Le message doit signaler l'incohérence de l'horizon — message reçu : " + ex.getMessage());
    }

    // ----------------------------------------------------------
    // ERROR — referentiels
    // ----------------------------------------------------------

    /**
     * [T-P7-05] referentiels absent → IllegalArgumentException.
     * Remplace le comportement silencieux (toReferentiel(null) → neutre → tous créneaux exclus).
     */
    @Test
    void referentiels_null_leve_IllegalArgumentException() {
        Sc03ScenarioRequestDTO req = requeteBase();
        creneauDansDataset(req, "CRE-01", "ACT-CONNU");
        req.getDataSet().setReferentiels(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("referentiels"),
                "Le message doit mentionner 'referentiels' — message reçu : " + ex.getMessage());
    }

    // ----------------------------------------------------------
    // ERROR — champs horaires créneau
    // ----------------------------------------------------------

    /**
     * [T-P7-06] heureDebut absente → IllegalArgumentException.
     * Remplace la NPE dans calculerDureeMinutes(null, heureFin).
     */
    @Test
    void heureDebut_null_leve_IllegalArgumentException() {
        Sc03ScenarioRequestDTO req = requeteBase();
        creneauSansHeureDebut(req, "CRE-SANS-DEBUT", "ACT-CONNU");
        activiteInReferentiel(req, "ACT-CONNU");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("heureDebut"),
                "Le message doit mentionner 'heureDebut' — message reçu : " + ex.getMessage());
    }

    /**
     * [T-P7-07] heureFin absente → IllegalArgumentException.
     * Remplace la NPE dans calculerDureeMinutes(heureDebut, null).
     */
    @Test
    void heureFin_null_leve_IllegalArgumentException() {
        Sc03ScenarioRequestDTO req = requeteBase();
        creneauSansHeureFin(req, "CRE-SANS-FIN", "ACT-CONNU");
        activiteInReferentiel(req, "ACT-CONNU");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare(req));
        assertTrue(ex.getMessage().contains("heureFin"),
                "Le message doit mentionner 'heureFin' — message reçu : " + ex.getMessage());
    }

    // ----------------------------------------------------------
    // WARN — pas de blocage, comportement solveur inchangé
    // ----------------------------------------------------------

    /**
     * [T-P7-08] Référentiel vide → pas d'exception, tous créneaux exclus comme activiteInconnue.
     * WARN global émis — comportement conservé.
     */
    @Test
    void referentiel_vide_neLevePasException_tousCreneauxExclus() {
        Sc03ScenarioRequestDTO req = requeteBase();
        creneauDansDataset(req, "CRE-01", "ACT-QUELCONQUE");
        // référentiel vide — aucune activité définie

        PreparedSc03Scenario scenario = assertDoesNotThrow(() -> service.prepare(req));

        assertEquals(0, scenario.planningRequest().creneaux().size(),
                "Aucun créneau ne doit atteindre le solveur");
        assertEquals(1, scenario.ignoredCreneaux().getActiviteInconnue(),
                "Le créneau doit être compté dans activiteInconnue");
    }

    /**
     * [T-P7-09] codeActiviteId et activite présents et discordants → codeActiviteId prime, pas d'exception.
     * WARN émis — comportement réel de résolution inchangé.
     */
    @Test
    void codeActiviteIdEtActiviteDiscordants_codeActiviteIdPrime_pasDException() {
        Sc03ScenarioRequestDTO req = requeteBase();
        CreneauInputDTO dto = new CreneauInputDTO();
        dto.setId("CRE-DISCORD");
        dto.setDate(DATE_DEBUT);
        dto.setHeureDebut(LocalTime.of(8, 0));
        dto.setHeureFin(LocalTime.of(16, 0));
        dto.setCodeActiviteId("ACT-CONNU");
        dto.setActivite("LIBELLE-DIFFERENT"); // discordant — pas une clé du référentiel
        req.getDataSet().getCreneaux().add(dto);
        activiteInReferentiel(req, "ACT-CONNU");

        PreparedSc03Scenario scenario = assertDoesNotThrow(() -> service.prepare(req));

        // codeActiviteId prime → activité connue → créneau transmis au solveur
        assertEquals(1, scenario.planningRequest().creneaux().size(),
                "Le créneau doit être transmis au solveur via codeActiviteId");
        assertEquals(0, scenario.ignoredCreneaux().getActiviteInconnue());
    }

    /**
     * [T-P7-10] Aucune ressource réelle → pas d'exception, créneau transmis, WARN émis.
     * RessourceNonAffectee absorbe — comportement solveur inchangé.
     */
    @Test
    void aucuneRessourceReelle_neLevePasException_creneauTransmis() {
        Sc03ScenarioRequestDTO req = requeteBase();
        req.getDataSet().getRessources().setSalaries(new ArrayList<>());
        req.getDataSet().getRessources().setPostesVirtuels(new ArrayList<>());
        creneauDansDataset(req, "CRE-01", "ACT-CONNU");
        activiteInReferentiel(req, "ACT-CONNU");

        PreparedSc03Scenario scenario = assertDoesNotThrow(() -> service.prepare(req));

        assertEquals(1, scenario.planningRequest().creneaux().size(),
                "Le créneau doit atteindre le solveur même sans ressource déclarée");
    }

    // ----------------------------------------------------------
    // Non-régression — cas valide complet inchangé
    // ----------------------------------------------------------

    /**
     * [T-P7-11] Requête valide complète → comportement inchangé après Phase 7.
     */
    @Test
    void requeteValide_comportementInchange() {
        Sc03ScenarioRequestDTO req = requeteBase();
        creneauDansDataset(req, "CRE-01", "ACT-CONNU");
        activiteInReferentiel(req, "ACT-CONNU");

        PreparedSc03Scenario scenario = assertDoesNotThrow(() -> service.prepare(req));

        assertEquals(1, scenario.planningRequest().creneaux().size());
        assertEquals(0, scenario.ignoredCreneaux().getActiviteInconnue());
        assertEquals(0, scenario.ignoredCreneaux().getHorsHorizon());
    }

    // ----------------------------------------------------------
    // Utilitaires
    // ----------------------------------------------------------

    private Sc03ScenarioRequestDTO requeteBase() {
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
        ressources.setSalaries(new ArrayList<>(List.of(sal)));
        ressources.setPostesVirtuels(new ArrayList<>());
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

    private void creneauDansDataset(Sc03ScenarioRequestDTO req, String id, String codeActiviteId) {
        CreneauInputDTO dto = new CreneauInputDTO();
        dto.setId(id);
        dto.setDate(DATE_DEBUT);
        dto.setHeureDebut(LocalTime.of(8, 0));
        dto.setHeureFin(LocalTime.of(16, 0));
        dto.setCodeActiviteId(codeActiviteId);
        req.getDataSet().getCreneaux().add(dto);
    }

    private void creneauSansHeureDebut(Sc03ScenarioRequestDTO req, String id, String codeActiviteId) {
        CreneauInputDTO dto = new CreneauInputDTO();
        dto.setId(id);
        dto.setDate(DATE_DEBUT);
        // heureDebut volontairement absente
        dto.setHeureFin(LocalTime.of(16, 0));
        dto.setCodeActiviteId(codeActiviteId);
        req.getDataSet().getCreneaux().add(dto);
    }

    private void creneauSansHeureFin(Sc03ScenarioRequestDTO req, String id, String codeActiviteId) {
        CreneauInputDTO dto = new CreneauInputDTO();
        dto.setId(id);
        dto.setDate(DATE_DEBUT);
        dto.setHeureDebut(LocalTime.of(8, 0));
        // heureFin volontairement absente
        dto.setCodeActiviteId(codeActiviteId);
        req.getDataSet().getCreneaux().add(dto);
    }

    private void activiteInReferentiel(Sc03ScenarioRequestDTO req, String codeActiviteId) {
        ReferentielActiviteDTO activite = new ReferentielActiviteDTO();
        activite.setCodeActiviteId(codeActiviteId);
        activite.setCompteDansCharge(true);
        activite.setGenereDetteRepos(false);
        activite.setEstServiceCritique(false);
        req.getDataSet().getReferentiels().getActivites().add(activite);
    }
}
