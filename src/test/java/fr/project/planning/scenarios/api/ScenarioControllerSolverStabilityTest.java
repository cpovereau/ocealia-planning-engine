package fr.project.planning.scenarios.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.junit.jupiter.api.Assertions.assertEquals; 
import fr.project.planning.TestSpringConfig;

@SpringBootTest(classes = TestSpringConfig.class)
@AutoConfigureMockMvc
/**
 * Vérifie qu'un même dataset de référence produit un résultat observable stable
 * via l'API du moteur.
 */
class ScenarioControllerSolverStabilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sc01_same_dataset_should_produce_same_observable_result() throws Exception {
        String json = Files.readString(
                Path.of("src/test/resources/scenarios/sc01/sc01_dataset_reference.json")
        );

        MvcResult run1 = mockMvc.perform(post("/scenarios/sc01/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(json != null ? json : ""))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult run2 = mockMvc.perform(post("/scenarios/sc01/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(json != null ? json : ""))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response1 = objectMapper.readTree(run1.getResponse().getContentAsString());
        JsonNode response2 = objectMapper.readTree(run2.getResponse().getContentAsString());

        assertEquals(
                response1.path("solverResult").path("status").asText(),
                response2.path("solverResult").path("status").asText(),
                "Le statut solveur doit être stable"
        );

        assertEquals(
                response1.path("solverResult").path("score").path("hard").asInt(),
                response2.path("solverResult").path("score").path("hard").asInt(),
                "Le score hard doit être stable"
        );

        assertEquals(
                response1.path("solverResult").path("score").path("soft").asInt(),
                response2.path("solverResult").path("score").path("soft").asInt(),
                "Le score soft doit être stable"
        );

        assertEquals(
                response1.path("solutionSummary"),
                response2.path("solutionSummary"),
                "Le résumé de solution doit être stable"
        );

        assertEquals(
                response1.path("planning"),
                response2.path("planning"),
                "Le planning retourné doit être stable"
        );
    }
}