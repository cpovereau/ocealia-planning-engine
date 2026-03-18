package fr.project.planning.scenarios.sc03.api;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests négatifs du contrat d'entrée SC-03.
 *
 * Vérifie les rejets attendus avant l'appel au solveur :
 * - scenarioType invalide (pas SC-03)
 * - dataSet absent
 * - creneaux absents ou vides
 * - JSON invalide (400 Spring)
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
    void should_raise_illegal_argument_if_scenarioType_is_not_sc03() {
        String json = """
        {
          "scenarioType": "SC-01",
          "planningContext": {
            "horizon": { "dateDebut": "2026-05-11", "dateFin": "2026-05-17" },
            "strategieScoring": "EXPLOITATION"
          },
          "dataSet": {
            "ressources": { "salaries": [], "postesVirtuels": [] },
            "creneaux": [
              {
                "id": "C-001", "posteVirtuelId": "PV-001",
                "codeActiviteId": "ACT-SOIN",
                "dateJour": "2026-05-12",
                "heureDebut": "08:00", "heureFin": "12:00",
                "siteId": "SITE-A", "dureeMinutes": 240
              }
            ]
          }
        }
        """;

        assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json))
                        .andExpect(status().isOk()),
                "Une IllegalArgumentException était attendue pour scenarioType=SC-01"
        );
    }

    // ---------------------------------------------------------
    // 2. dataSet absent
    // ---------------------------------------------------------

    @Test
    void should_raise_exception_if_dataset_is_absent() {
        String json = """
        {
          "scenarioType": "SC-03",
          "planningContext": {
            "horizon": { "dateDebut": "2026-05-11", "dateFin": "2026-05-17" },
            "strategieScoring": "EXPLOITATION"
          }
        }
        """;

        assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json))
                        .andExpect(status().isOk())
        );
    }

    // ---------------------------------------------------------
    // 3. creneaux vides
    // ---------------------------------------------------------

    @Test
    void should_raise_illegal_argument_if_creneaux_are_empty() {
        String json = """
        {
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

        Exception ex = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json))
                        .andExpect(status().isOk())
        );
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    // ---------------------------------------------------------
    // 4. JSON invalide → 400 Spring
    // ---------------------------------------------------------

    @Test
    void should_return_400_for_malformed_json() throws Exception {
        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }
}
