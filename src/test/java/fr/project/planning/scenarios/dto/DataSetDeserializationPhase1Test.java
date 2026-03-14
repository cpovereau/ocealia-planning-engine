package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.scenarios.dto.input.ContraintesReglementairesDTO;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
import fr.project.planning.scenarios.dto.input.SalarieInputDTO;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de désérialisation Phase 1 — couche transport.
 *
 * Valide que le moteur peut recevoir les nouveaux champs du contrat SC-03
 * sans erreur et sans casser SC-01.
 *
 * Critère de sortie Phase 1 : le moteur accepte le JSON enrichi sans casser SC-01.
 */
class DataSetDeserializationPhase1Test {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // ---------------------------------------------------------
    // Test 1 : dataset SC-03 enrichi — désérialisation complète
    // ---------------------------------------------------------

    @Test
    void shouldDeserializeSc03EnrichedDataset() throws Exception {
        String path = "scenarios/sc03/sc03_migration_reference.json";

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "Fichier introuvable : " + path);

            ScenarioRequestDTO request = objectMapper.readValue(input, ScenarioRequestDTO.class);

            assertNotNull(request);
            assertEquals("SC-03", request.getScenarioType());

            assertNotNull(request.getPlanningContext());
            assertNotNull(request.getPlanningContext().getHorizon());

            assertNotNull(request.getDataSet());
            assertNotNull(request.getDataSet().getRessources());

            // 2 salariés
            assertNotNull(request.getDataSet().getRessources().getSalaries());
            assertEquals(2, request.getDataSet().getRessources().getSalaries().size());

            // 1 poste virtuel
            assertNotNull(request.getDataSet().getRessources().getPostesVirtuels());
            assertEquals(1, request.getDataSet().getRessources().getPostesVirtuels().size());

            // 6 créneaux
            assertNotNull(request.getDataSet().getCreneaux());
            assertEquals(6, request.getDataSet().getCreneaux().size());

            // indisponibilités transportées
            assertNotNull(request.getDataSet().getIndisponibilites());
            assertNotNull(request.getDataSet().getIndisponibilites().getItems());
            assertEquals(1, request.getDataSet().getIndisponibilites().getItems().size());
        }
    }

    // ---------------------------------------------------------
    // Test 2 : salarié avec les 4 champs nuit/férié
    // ---------------------------------------------------------

    @Test
    void shouldDeserializeSalarieWithNuitAndFerieFields() throws Exception {
        String path = "scenarios/sc03/sc03_migration_reference.json";

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "Fichier introuvable : " + path);

            ScenarioRequestDTO request = objectMapper.readValue(input, ScenarioRequestDTO.class);

            // SAL-2001 : pas de nuit, pas de férié
            SalarieInputDTO sal2001 = request.getDataSet().getRessources().getSalaries().stream()
                    .filter(s -> "SAL-2001".equals(s.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("SAL-2001 introuvable"));

            assertNull(sal2001.getTravailDeNuit());
            assertNotNull(sal2001.getTravailleJourFerie());
            assertFalse(sal2001.getTravailleJourFerie());

            // SAL-2001 : contraintesReglementaires transportées
            ContraintesReglementairesDTO cr2001 = sal2001.getContraintesReglementaires();
            assertNotNull(cr2001);
            assertEquals(4.0, cr2001.getHeuresMinimumParJour());
            assertEquals(10.0, cr2001.getHeuresMaximumParJour());
            assertEquals(39.0, cr2001.getHeuresMaximumParSemaine());
            assertEquals(0, cr2001.getNuitsMaximumParSemaine());
            assertEquals(5, cr2001.getJoursConsecutifsMaximum());

            // SAL-2002 : nuit occasionnel, autorisé jours fériés
            SalarieInputDTO sal2002 = request.getDataSet().getRessources().getSalaries().stream()
                    .filter(s -> "SAL-2002".equals(s.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("SAL-2002 introuvable"));

            assertEquals("occasionnel", sal2002.getTravailDeNuit());
            assertEquals(LocalTime.of(22, 0), sal2002.getHeureDebutNuit());
            assertEquals(LocalTime.of(6, 0), sal2002.getHeureFinNuit());
            assertNotNull(sal2002.getTravailleJourFerie());
            assertTrue(sal2002.getTravailleJourFerie());

            ContraintesReglementairesDTO cr2002 = sal2002.getContraintesReglementaires();
            assertNotNull(cr2002);
            assertEquals(44.0, cr2002.getHeuresMaximumParSemaine());
            assertEquals(3, cr2002.getNuitsMaximumParSemaine());
        }
    }

    // ---------------------------------------------------------
    // Test 3 : créneaux structurés (blocJourId, groupeBesoinId, créneau nuit)
    // ---------------------------------------------------------

    @Test
    void shouldDeserializeStructuredCreneaux() throws Exception {
        String path = "scenarios/sc03/sc03_migration_reference.json";

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "Fichier introuvable : " + path);

            ScenarioRequestDTO request = objectMapper.readValue(input, ScenarioRequestDTO.class);

            // Créneau standard : CRE-L-01
            CreneauInputDTO creL01 = request.getDataSet().getCreneaux().stream()
                    .filter(c -> "CRE-L-01".equals(c.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("CRE-L-01 introuvable"));

            assertEquals("BLOC-LUNDI-MATIN", creL01.getBlocJourId());
            assertEquals("BESOIN-SEMAINE-01", creL01.getGroupeBesoinId());
            assertEquals(1, creL01.getOrdreDansBloc());
            assertFalse(Boolean.TRUE.equals(creL01.getEstSegmentDePause()));
            assertEquals(LocalTime.of(7, 0), creL01.getHeureDebut());
            assertEquals(LocalTime.of(15, 0), creL01.getHeureFin());

            // Créneau nuit traversant minuit : CRE-VN-01
            CreneauInputDTO creVn01 = request.getDataSet().getCreneaux().stream()
                    .filter(c -> "CRE-VN-01".equals(c.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("CRE-VN-01 introuvable"));

            assertTrue(Boolean.TRUE.equals(creVn01.getSegmentNuit()));
            assertEquals(LocalTime.of(22, 0), creVn01.getHeureDebut());
            assertEquals(LocalTime.of(6, 0), creVn01.getHeureFin());
            assertEquals("BESOIN-NUIT-01", creVn01.getGroupeBesoinId());

            // Créneau jour férié : CRE-ME-01
            CreneauInputDTO creME01 = request.getDataSet().getCreneaux().stream()
                    .filter(c -> "CRE-ME-01".equals(c.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("CRE-ME-01 introuvable"));

            assertTrue(Boolean.TRUE.equals(creME01.getIsJourFerie()));
            assertEquals("BESOIN-FERIE-01", creME01.getGroupeBesoinId());
        }
    }
}
