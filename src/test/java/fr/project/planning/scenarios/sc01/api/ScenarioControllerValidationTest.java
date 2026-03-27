package fr.project.planning.scenarios.sc01.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V1 réaliste des tests négatifs du contrat d'entrée de l'API SC-01.
 *
 * Cette version ne suppose pas encore l'existence d'un ControllerAdvice global
 * transformant systématiquement les erreurs métier/techniques en HTTP 400.
 *
 * On vérifie donc :
 * - 400 quand Spring le produit réellement,
 * - l'exception résolue quand le contrôleur laisse encore remonter
 *   une NullPointerException / IllegalArgumentException.
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    // ---------------------------------------------------------
    // 1️⃣ scenarioType invalide
    // ---------------------------------------------------------

    @Test
    void should_raise_illegal_argument_if_scenarioType_is_invalid() throws Exception {

        String json = """
        {
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

        ServletException ex = assertThrows(
        ServletException.class,
        () -> mockMvc.perform(post("/scenarios/sc01/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json))
        );

        assertNotNull(ex.getCause());
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    // ---------------------------------------------------------
    // 2️⃣ resourceRef absent
    // ---------------------------------------------------------

    @Test
    void should_raise_illegal_argument_if_resource_reference_missing() throws Exception {

        String json = """
        {
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

        ServletException ex = assertThrows(
        ServletException.class,
        () -> mockMvc.perform(post("/scenarios/sc01/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json))
        );

        assertNotNull(ex.getCause());
        // Phase A : resourceRef absent est désormais détecté par un guard explicite
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    // ---------------------------------------------------------
    // 3️⃣ dataset absent
    // ---------------------------------------------------------

    @Test
    void should_raise_null_pointer_if_dataset_missing() throws Exception {

        // dailyAmplitudeHours et shiftStart sont requis pour passer les guards Phase A
        // et atteindre effectivement le NPE sur le dataset absent
        String json = """
        {
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

        ServletException ex = assertThrows(
        ServletException.class,
        () -> mockMvc.perform(post("/scenarios/sc01/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json))
        );

        assertNotNull(ex.getCause());
        assertInstanceOf(NullPointerException.class, ex.getCause());
    }

    // ---------------------------------------------------------
    // 4️⃣ JSON invalide
    // ---------------------------------------------------------

    @Test
    void should_return_400_if_json_is_invalid() throws Exception {

        String invalidJson = "{ \"scenarioType\": \"SC-01\", ";

        mockMvc.perform(post("/scenarios/sc01/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------
    // 5️⃣ resourceRef inconnue dans le dataset
    // ---------------------------------------------------------

    @Test
    void should_raise_illegal_argument_if_resource_not_found_in_dataset() throws Exception {

        String json = """
        {
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

        ServletException ex = assertThrows(
        ServletException.class,
        () -> mockMvc.perform(post("/scenarios/sc01/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json))
        );

        assertNotNull(ex.getCause());
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    // ---------------------------------------------------------
    // 6️⃣ horizon incohérent (dateFin < dateDebut)
    // ---------------------------------------------------------

    @Test
    void should_raise_illegal_argument_if_horizon_is_invalid() throws Exception {

        String json = """
        {
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

        ServletException ex = assertThrows(
        ServletException.class,
        () -> mockMvc.perform(post("/scenarios/sc01/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json))
        );

        assertNotNull(ex.getCause());
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }
}
