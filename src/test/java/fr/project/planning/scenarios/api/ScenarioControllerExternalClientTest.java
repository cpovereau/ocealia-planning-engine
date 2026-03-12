package fr.project.planning.scenarios.api;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import fr.project.planning.TestSpringConfig;

@SpringBootTest(classes = TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioControllerExternalClientTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void windev_should_be_able_to_call_engine() throws Exception {

        String json = Files.readString(
            Path.of("src/test/resources/scenarios/sc01/sc01_dataset_reference.json")
        );

        MvcResult result = mockMvc.perform(post("/scenarios/sc01/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .content(json != null ? json : ""))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        assertFalse(response.isBlank(), "La réponse du moteur ne doit pas être vide");

        assertTrue(response.contains("solverResult"));
        assertTrue(response.contains("planning"));
        assertTrue(response.contains("solutionSummary"));

        System.out.println(result.getResponse().getContentAsString());
    }
}