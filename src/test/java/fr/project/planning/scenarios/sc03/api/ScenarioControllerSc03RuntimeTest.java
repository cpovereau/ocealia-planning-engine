package fr.project.planning.scenarios.sc03.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration SC-03 avec le dataset de référence.
 *
 * Vérifie :
 * - réponse HTTP 200
 * - status SOLVED
 * - score hard = 0 (aucune contrainte hard violée)
 * - présence des blocs planning, workMetrics, solutionSummary
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioControllerSc03RuntimeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sc03_runtime_should_solve_reference_dataset() throws Exception {

        String json = Files.readString(
                Path.of("src/test/resources/scenarios/sc03/sc03_migration_reference.json")
        );

        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(json)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solverResult.status").value("SOLVED"))
                .andExpect(jsonPath("$.solverResult.score.hard").value(0))
                // Garde-fou de score, posé au lot S7.2. Le chantier de remise en service du
                // socle réglementaire rebranche une contrainte par lot ; sans valeur de
                // référence, un écart passerait inaperçu ou resterait inattribuable.
                // Une modification volontaire de cette valeur doit être consignée dans
                // 92_cadrage_socle_reglementaire.md, avec le lot qui la produit.
                .andExpect(jsonPath("$.solverResult.score.soft").value(-960))
                .andExpect(jsonPath("$.planning").exists())
                .andExpect(jsonPath("$.workMetrics").exists())
                .andExpect(jsonPath("$.solutionSummary").exists())
                .andExpect(jsonPath("$.planning.jours").isArray());
    }
}
