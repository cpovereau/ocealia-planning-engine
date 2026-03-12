package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioRequestDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .findAndRegisterModules()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void shouldDeserializeScenarioRequestDtoFromJson() throws Exception {
        String path = "scenarios/sc01/sc01_dataset_reference.json";

        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream(path)) {

            assertNotNull(input, "Le fichier JSON de test est introuvable : " + path);

            ScenarioRequestDTO request = objectMapper.readValue(input, ScenarioRequestDTO.class);

            assertNotNull(request);
            assertEquals("SC-01", request.getScenarioType());

            assertNotNull(request.getPlanningContext());
            assertNotNull(request.getScenarioParameters());
            assertNotNull(request.getDataSet());

            assertNotNull(request.getDataSet().getRessources());
            assertNotNull(request.getDataSet().getRessources().getSalaries());
            assertFalse(request.getDataSet().getRessources().getSalaries().isEmpty());
        }
    }
}