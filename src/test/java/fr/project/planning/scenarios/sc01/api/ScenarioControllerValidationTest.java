package fr.project.planning.scenarios.sc01.api;

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
 * Tests négatifs du contrat d'entrée de l'API SC-01.
 *
 * Tous les payloads incluent requestId + metadata valides pour que la validation
 * Bean Validation passe et que les guards métier restent atteignables.
 *
 * Phase 4.2 : les erreurs métier (IllegalArgumentException) sont désormais
 * interceptées par GlobalExceptionHandler et retournent HTTP 422 BUSINESS_ERROR.
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    // ---------------------------------------------------------
    // 1. scenarioType invalide
    // ---------------------------------------------------------

    @Test
    void should_raise_illegal_argument_if_scenarioType_is_invalid() throws Exception {

        String json = """
        {
          "requestId": "REQ-TEST-001",
          "metadata": { "clientId": "CLIENT-TEST", "timestamp": "2026-05-11T08:00:00Z" },
          "scenarioType": "SC-99",
          "planningContext": {
            "horizon": {
              "dateDebut": "2026-05-11",
              "dateFin": "2026-05-17"
            },
            "strategieScoring": "EXPLOITATION"
          },
          "scenarioParameters": {},
          "dataSet": {
            "ressources": {
              "salaries": [],
              "postesVirtuels": []
            },
            "creneaux": []
          }
        }
        """;

        mockMvc.perform(post("/scenarios/sc01/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
    }

    // ---------------------------------------------------------
    // 2. resourceRef absent
    // ---------------------------------------------------------

    @Test
    void should_raise_illegal_argument_if_resource_reference_missing() throws Exception {

        String json = """
        {
          "requestId": "REQ-TEST-002",
          "metadata": { "clientId": "CLIENT-TEST", "timestamp": "2026-05-11T08:00:00Z" },
          "scenarioType": "SC-01",
          "planningContext": {
            "horizon": {
              "dateDebut": "2026-05-11",
              "dateFin": "2026-05-17"
            },
            "strategieScoring": "EXPLOITATION"
          },
          "scenarioParameters": {
            "dailyAmplitudeHours": 8.0
          },
          "dataSet": {
            "ressources": {
              "salaries": [],
              "postesVirtuels": []
            },
            "creneaux": []
          }
        }
        """;

        mockMvc.perform(post("/scenarios/sc01/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
    }

    // ---------------------------------------------------------
    // 3. dataset absent
    // ---------------------------------------------------------

    /**
     * Quand dataSet est absent, le service lève une NullPointerException avant
     * d'atteindre le guard IllegalArgumentException — ce qui produit un 500.
     *
     * TODO Phase 4.6 : ajouter un guard explicite (throw IAE) dans
     *   ScenarioSc01PreparationService avant tout accès à dataSet,
     *   afin de retourner 422 BUSINESS_ERROR à la place du 500 actuel.
     */
    @Test
    void should_return_500_if_dataset_missing() throws Exception {

        String json = """
        {
          "requestId": "REQ-TEST-003",
          "metadata": { "clientId": "CLIENT-TEST", "timestamp": "2026-05-11T08:00:00Z" },
          "scenarioType": "SC-01",
          "planningContext": {
            "horizon": {
              "dateDebut": "2026-05-11",
              "dateFin": "2026-05-17"
            },
            "strategieScoring": "EXPLOITATION"
          },
          "scenarioParameters": {
            "resourceRef": {
              "kind": "SALARIE",
              "id": "1041"
            },
            "dailyAmplitudeHours": 8.0,
            "shiftStart": "08:00"
          }
        }
        """;

        mockMvc.perform(post("/scenarios/sc01/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
    }

    // ---------------------------------------------------------
    // 4. JSON invalide
    // ---------------------------------------------------------

    @Test
    void should_return_400_if_json_is_invalid() throws Exception {

        String invalidJson = "{ \"scenarioType\": \"SC-01\", ";

        mockMvc.perform(post("/scenarios/sc01/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_JSON"));
    }

    // ---------------------------------------------------------
    // 5. resourceRef inconnue dans le dataset
    // ---------------------------------------------------------

    @Test
    void should_raise_illegal_argument_if_resource_not_found_in_dataset() throws Exception {

        String json = """
        {
          "requestId": "REQ-TEST-005",
          "metadata": { "clientId": "CLIENT-TEST", "timestamp": "2026-05-11T08:00:00Z" },
          "scenarioType": "SC-01",
          "planningContext": {
            "horizon": {
              "dateDebut": "2026-05-11",
              "dateFin": "2026-05-17"
            },
            "strategieScoring": "EXPLOITATION"
          },
          "scenarioParameters": {
            "resourceRef": {
              "kind": "SALARIE",
              "id": "9999"
            }
          },
          "dataSet": {
            "ressources": {
              "salaries": [
                {
                  "id": "1041",
                  "type": "SALARIE",
                  "capaciteCible": 2400,
                  "activitesAutorisees": ["TRAVAIL"],
                  "lieuxAutorises": ["CASTRES"],
                  "postesComptablesCompatibles": ["PC-100"]
                }
              ],
              "postesVirtuels": []
            },
            "creneaux": []
          }
        }
        """;

        mockMvc.perform(post("/scenarios/sc01/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
    }

    // ---------------------------------------------------------
    // 6. horizon incohérent (dateFin < dateDebut)
    // ---------------------------------------------------------

    @Test
    void should_raise_illegal_argument_if_horizon_is_invalid() throws Exception {

        String json = """
        {
          "requestId": "REQ-TEST-006",
          "metadata": { "clientId": "CLIENT-TEST", "timestamp": "2026-05-11T08:00:00Z" },
          "scenarioType": "SC-01",
          "planningContext": {
            "horizon": {
              "dateDebut": "2026-05-17",
              "dateFin": "2026-05-11"
            },
            "strategieScoring": "EXPLOITATION"
          },
          "scenarioParameters": {
            "resourceRef": {
              "kind": "SALARIE",
              "id": "1041"
            },
            "dailyAmplitudeHours": 8.0,
            "shiftStart": "08:00",
            "shiftEndAlert": "17:00",
            "workedDays": ["MONDAY"]
          },
          "dataSet": {
            "ressources": {
              "salaries": [
                {
                  "id": "1041",
                  "type": "SALARIE",
                  "capaciteCible": 2400,
                  "activitesAutorisees": ["TRAVAIL"],
                  "lieuxAutorises": ["CASTRES"],
                  "postesComptablesCompatibles": ["PC-100"]
                }
              ],
              "postesVirtuels": []
            },
            "creneaux": []
          }
        }
        """;

        mockMvc.perform(post("/scenarios/sc01/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_ERROR"));
    }
}
