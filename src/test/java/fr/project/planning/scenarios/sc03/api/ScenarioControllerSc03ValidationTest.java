package fr.project.planning.scenarios.sc03.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests négatifs du contrat d'entrée SC-03.
 *
 * Tous les payloads incluent requestId + metadata valides pour que la validation
 * Bean Validation passe et que les guards métier restent atteignables.
 *
 * Phase 4.2 : les erreurs métier (IllegalArgumentException) sont désormais
 * interceptées par GlobalExceptionHandler et retournent HTTP 422 BUSINESS_ERROR.
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioControllerSc03ValidationTest {

    @Autowired
    private MockMvc mockMvc;

    // ---------------------------------------------------------
    // 1. scenarioType invalide
    // ---------------------------------------------------------

    @Test
    void should_raise_illegal_argument_if_scenarioType_is_not_sc03() throws Exception {
        String json = """
        {
          "requestId": "REQ-TEST-SC03-001",
          "metadata": { "clientId": "CLIENT-TEST", "timestamp": "2026-05-11T08:00:00Z" },
          "scenarioType": "SC-01",
          "planningContext": {
            "horizon": { "dateDebut": "2026-05-11", "dateFin": "2026-05-17" },
            "strategieScoring": "EXPLOITATION"
          },
          "dataSet": {
            "ressources": { "salaries": [], "postesVirtuels": [] },
            "creneaux": [
              {
                "id": "C-001",
                "codeActiviteId": "ACT-SOIN",
                "date": "2026-05-12",
                "heureDebut": "08:00", "heureFin": "12:00"
              }
            ]
          }
        }
        """;

        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
    }

    // ---------------------------------------------------------
    // 2. dataSet absent
    // ---------------------------------------------------------

    @Test
    void should_raise_exception_if_dataset_is_absent() throws Exception {
        String json = """
        {
          "requestId": "REQ-TEST-SC03-002",
          "metadata": { "clientId": "CLIENT-TEST", "timestamp": "2026-05-11T08:00:00Z" },
          "scenarioType": "SC-03",
          "planningContext": {
            "horizon": { "dateDebut": "2026-05-11", "dateFin": "2026-05-17" },
            "strategieScoring": "EXPLOITATION"
          }
        }
        """;

        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
    }

    // ---------------------------------------------------------
    // 3. creneaux vides
    // ---------------------------------------------------------

    @Test
    void should_raise_illegal_argument_if_creneaux_are_empty() throws Exception {
        String json = """
        {
          "requestId": "REQ-TEST-SC03-003",
          "metadata": { "clientId": "CLIENT-TEST", "timestamp": "2026-05-11T08:00:00Z" },
          "scenarioType": "SC-03",
          "planningContext": {
            "horizon": { "dateDebut": "2026-05-11", "dateFin": "2026-05-17" },
            "strategieScoring": "EXPLOITATION"
          },
          "dataSet": {
            "ressources": { "salaries": [], "postesVirtuels": [] },
            "creneaux": []
          }
        }
        """;

        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
    }

    // ---------------------------------------------------------
    // 4. JSON invalide → 400 Spring
    // ---------------------------------------------------------

    @Test
    void should_return_400_for_malformed_json() throws Exception {
        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_JSON"));
    }
}
