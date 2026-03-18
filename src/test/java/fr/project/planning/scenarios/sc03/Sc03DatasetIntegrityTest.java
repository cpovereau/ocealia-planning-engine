package fr.project.planning.scenarios.sc03;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.scenarios.dto.DataSetDTO;
import fr.project.planning.scenarios.dto.ScenarioRequestDTO;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
import fr.project.planning.scenarios.dto.input.ReferentielActiviteDTO;
import fr.project.planning.scenarios.dto.input.SalarieInputDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sc03DatasetIntegrityTest — Phase 2.
 *
 * Valide la cohérence interne du dataset de référence SC-03 utilisé comme
 * base commune pour les tests de migration WinDev → moteur.
 *
 * Critères vérifiés :
 * - complétude du contenu minimal (toutes les situations réglementaires)
 * - unicité des IDs
 * - cohérence des références croisées (activités, salariés, indisponibilités)
 * - cohérence des dates (horizon, créneaux, indisponibilités)
 * - présence des structures de besoins (groupeBesoinId, blocJourId)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Sc03DatasetIntegrityTest {

    private static final String PATH = "scenarios/sc03/sc03_migration_reference.json";
    private static final LocalDate DATE_DEBUT = LocalDate.of(2026, 5, 11);
    private static final LocalDate DATE_FIN = LocalDate.of(2026, 5, 17);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ScenarioRequestDTO request;

    @BeforeAll
    void chargerDataset() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PATH)) {
            assertNotNull(input, "Dataset introuvable : " + PATH);
            request = objectMapper.readValue(input, ScenarioRequestDTO.class);
        }
        assertNotNull(request, "Désérialisation échouée");
    }

    // -------------------------------------------------------
    // 1. Structure de base
    // -------------------------------------------------------

    @Test
    void devraitEtreUnScenarioSc03() {
        assertEquals("SC-03", request.getScenarioType());
    }

    @Test
    void devraitAvoirUnHorizonValide() {
        assertNotNull(request.getPlanningContext());
        assertNotNull(request.getPlanningContext().getHorizon());
        assertEquals(DATE_DEBUT, request.getPlanningContext().getHorizon().getDateDebut());
        assertEquals(DATE_FIN, request.getPlanningContext().getHorizon().getDateFin());
        assertFalse(DATE_FIN.isBefore(DATE_DEBUT), "dateFin doit être >= dateDebut");
    }

    @Test
    void devraitAvoirUnDatasetNonVide() {
        DataSetDTO ds = request.getDataSet();
        assertNotNull(ds);
        assertNotNull(ds.getRessources());
    }

    // -------------------------------------------------------
    // 2. Contenu minimal — ressources
    // -------------------------------------------------------

    @Test
    void devraitAvoirDeuxSalaries() {
        List<SalarieInputDTO> salaries = request.getDataSet().getRessources().getSalaries();
        assertNotNull(salaries);
        assertEquals(2, salaries.size(), "2 salariés attendus");
        salaries.forEach(s -> assertNotNull(s.getId(), "Chaque salarié doit avoir un id"));
    }

    @Test
    void devraitAvoirUnPosteVirtuel() {
        var postes = request.getDataSet().getRessources().getPostesVirtuels();
        assertNotNull(postes);
        assertEquals(1, postes.size(), "1 poste virtuel attendu");
        assertNotNull(postes.get(0).getId());
    }

    @Test
    void devraitAvoirDesIdsUniquesParmiLesSalaries() {
        List<String> ids = request.getDataSet().getRessources().getSalaries()
                .stream().map(SalarieInputDTO::getId).toList();
        assertEquals(ids.size(), Set.copyOf(ids).size(), "IDs salariés doivent être uniques");
    }

    // -------------------------------------------------------
    // 3. Contenu minimal — créneaux
    // -------------------------------------------------------

    @Test
    void devraitAvoirAuMoinsSixCreneaux() {
        List<CreneauInputDTO> creneaux = request.getDataSet().getCreneaux();
        assertNotNull(creneaux);
        assertTrue(creneaux.size() >= 6, "Au moins 6 créneaux attendus, trouvé : " + creneaux.size());
    }

    @Test
    void devraitAvoirDesIdsUniquesParmiLesCreneaux() {
        List<String> ids = request.getDataSet().getCreneaux()
                .stream().map(CreneauInputDTO::getId).toList();
        assertEquals(ids.size(), Set.copyOf(ids).size(), "IDs créneaux doivent être uniques");
    }

    @Test
    void devraitAvoirUnCreneauJour() {
        // Créneau jour : debut < fin ET non nuit ET non férié
        boolean hasJour = request.getDataSet().getCreneaux().stream()
                .anyMatch(c ->
                        c.getHeureDebut() != null && c.getHeureFin() != null
                        && c.getHeureDebut().isBefore(c.getHeureFin())
                        && !Boolean.TRUE.equals(c.getSegmentNuit())
                        && !Boolean.TRUE.equals(c.getIsJourFerie())
                );
        assertTrue(hasJour, "Au moins un créneau de jour attendu");
    }

    @Test
    void devraitAvoirUnCreneauSoiree() {
        // Créneau soirée : heureFin >= 20:00 ET non nuit traversant minuit
        boolean hasSoiree = request.getDataSet().getCreneaux().stream()
                .anyMatch(c ->
                        c.getHeureFin() != null
                        && c.getHeureFin().getHour() >= 20
                        && !Boolean.TRUE.equals(c.getSegmentNuit())
                );
        assertTrue(hasSoiree, "Au moins un créneau soirée attendu (heureFin >= 20:00)");
    }

    @Test
    void devraitAvoirUnCreneauNuit() {
        boolean hasNuit = request.getDataSet().getCreneaux().stream()
                .anyMatch(c -> Boolean.TRUE.equals(c.getSegmentNuit()));
        assertTrue(hasNuit, "Au moins un créneau de nuit (segmentNuit=true) attendu");
    }

    @Test
    void devraitAvoirUnCreneauJourFerie() {
        boolean hasFerie = request.getDataSet().getCreneaux().stream()
                .anyMatch(c -> Boolean.TRUE.equals(c.getIsJourFerie()));
        assertTrue(hasFerie, "Au moins un créneau sur jour férié (isJourFerie=true) attendu");
    }

    @Test
    void devraitAvoirUnCreneauDimanche() {
        boolean hasDimanche = request.getDataSet().getCreneaux().stream()
                .anyMatch(c ->
                        c.getDate() != null
                        && c.getDate().getDayOfWeek() == java.time.DayOfWeek.SUNDAY
                );
        assertTrue(hasDimanche, "Au moins un créneau le dimanche attendu");
    }

    // -------------------------------------------------------
    // 4. Cohérence des dates
    // -------------------------------------------------------

    @Test
    void lesCreneauxDoiventEtreDansLHorizon() {
        request.getDataSet().getCreneaux().forEach(c -> {
            if (c.getDate() != null) {
                assertFalse(c.getDate().isBefore(DATE_DEBUT),
                        "Créneau " + c.getId() + " avant l'horizon : " + c.getDate());
                assertFalse(c.getDate().isAfter(DATE_FIN),
                        "Créneau " + c.getId() + " après l'horizon : " + c.getDate());
            }
        });
    }

    @Test
    void lesIndisponibilitesDoiventEtreDansLHorizon() {
        if (request.getDataSet().getIndisponibilites() == null
                || request.getDataSet().getIndisponibilites().getItems() == null) return;

        request.getDataSet().getIndisponibilites().getItems().forEach(item -> {
            if (item.getDateDebut() != null) {
                assertFalse(item.getDateDebut().isBefore(DATE_DEBUT),
                        "Indispo avant l'horizon : " + item.getDateDebut());
            }
            if (item.getDateFin() != null && item.getDateDebut() != null) {
                assertFalse(item.getDateFin().isBefore(item.getDateDebut()),
                        "dateFin indispo avant dateDebut");
            }
        });
    }

    // -------------------------------------------------------
    // 5. Cohérence des références croisées
    // -------------------------------------------------------

    @Test
    void lesIndisponibilitesDoiventReferencerDesSalarieExistants() {
        if (request.getDataSet().getIndisponibilites() == null
                || request.getDataSet().getIndisponibilites().getItems() == null) return;

        Set<String> salarieIds = request.getDataSet().getRessources().getSalaries()
                .stream().map(SalarieInputDTO::getId).collect(Collectors.toSet());

        request.getDataSet().getIndisponibilites().getItems().forEach(item -> {
            assertTrue(salarieIds.contains(item.getRessourceId()),
                    "Indispo reference un salarié inconnu : " + item.getRessourceId());
        });
    }

    @Test
    void lesActiviteDesCreneauxDoiventExisterDansLesReferentiels() {
        if (request.getDataSet().getReferentiels() == null
                || request.getDataSet().getReferentiels().getActivites() == null) return;

        Set<String> activiteIds = request.getDataSet().getReferentiels().getActivites()
                .stream().map(ReferentielActiviteDTO::getCodeActiviteId).collect(Collectors.toSet());

        request.getDataSet().getCreneaux().forEach(c -> {
            if (c.getCodeActiviteId() != null) {
                assertTrue(activiteIds.contains(c.getCodeActiviteId()),
                        "Créneau " + c.getId() + " référence une activité inconnue : " + c.getCodeActiviteId());
            }
        });
    }

    // -------------------------------------------------------
    // 6. Structuration des besoins
    // -------------------------------------------------------

    @Test
    void auMoinsUnCreneauDevraitAvoirUnGroupeBesoinId() {
        boolean hasGroupe = request.getDataSet().getCreneaux().stream()
                .anyMatch(c -> c.getGroupeBesoinId() != null);
        assertTrue(hasGroupe, "Au moins un créneau avec groupeBesoinId attendu");
    }

    @Test
    void auMoinsUnCreneauDevraitAvoirUnBlocJourId() {
        boolean hasBloc = request.getDataSet().getCreneaux().stream()
                .anyMatch(c -> c.getBlocJourId() != null);
        assertTrue(hasBloc, "Au moins un créneau avec blocJourId attendu");
    }

    @Test
    void lesCreneauxAvecBlocJourIdDoiventAvoirUnOrdre() {
        request.getDataSet().getCreneaux().stream()
                .filter(c -> c.getBlocJourId() != null)
                .forEach(c -> assertNotNull(c.getOrdreDansBloc(),
                        "Créneau " + c.getId() + " a un blocJourId mais pas d'ordreDansBloc"));
    }

    // -------------------------------------------------------
    // 7. Champs nuit/férié par salarié
    // -------------------------------------------------------

    @Test
    void auMoinsUnSalarieDevraitAvoirTravailleJourFerieDefini() {
        boolean hasFerieInfo = request.getDataSet().getRessources().getSalaries().stream()
                .anyMatch(s -> s.getTravailleJourFerie() != null);
        assertTrue(hasFerieInfo, "Au moins un salarié avec travailleJourFerie défini attendu");
    }

    @Test
    void auMoinsUnSalarieDevraitAvoirDesCOntraintesReglementaires() {
        boolean hasConstraints = request.getDataSet().getRessources().getSalaries().stream()
                .anyMatch(s -> s.getContraintesReglementaires() != null);
        assertTrue(hasConstraints, "Au moins un salarié avec contraintesReglementaires attendu");
    }

    @Test
    void auMoinsUnSalarieDevraitAvoirUnProfilNuit() {
        boolean hasNuit = request.getDataSet().getRessources().getSalaries().stream()
                .anyMatch(s -> s.getTravailDeNuit() != null);
        assertTrue(hasNuit, "Au moins un salarié avec travailDeNuit non-null attendu");
    }

    // -------------------------------------------------------
    // 8. Axes organisationnels
    // -------------------------------------------------------

    @Test
    void auMoinsUnCreneauDevraitAvoirDesAxesOrganisationnels() {
        boolean hasAxes = request.getDataSet().getCreneaux().stream()
                .anyMatch(c -> c.getAxesOrganisationnels() != null);
        assertTrue(hasAxes, "Au moins un créneau avec axesOrganisationnels attendu");
    }

    @Test
    void auMoinsUnSalarieDevraitAvoirDesAxesOrganisationnels() {
        boolean hasAxes = request.getDataSet().getRessources().getSalaries().stream()
                .anyMatch(s -> s.getAxesOrganisationnels() != null);
        assertTrue(hasAxes, "Au moins un salarié avec axesOrganisationnels attendu");
    }
}