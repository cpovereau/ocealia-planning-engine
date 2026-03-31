package fr.project.planning.fileadapter.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileScenarioDispatcherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void dispatch_routeVersLaBonneFacade() throws Exception {
        FileScenarioExecutionFacade sc01Facade = mock(FileScenarioExecutionFacade.class);
        when(sc01Facade.supportedScenarioType()).thenReturn("SC-01");
        ScenarioResponseDTO expectedResponse = new ScenarioResponseDTO();
        when(sc01Facade.execute(any())).thenReturn(expectedResponse);

        FileScenarioDispatcher dispatcher = new FileScenarioDispatcher(List.of(sc01Facade));

        JsonNode payload = objectMapper.readTree("{\"scenarioType\": \"SC-01\"}");
        ScenarioResponseDTO result = dispatcher.dispatch(payload);

        assertThat(result).isSameAs(expectedResponse);
        verify(sc01Facade).execute(payload);
    }

    @Test
    void dispatch_nAppellePasLaMauvaiseFacade() throws Exception {
        FileScenarioExecutionFacade sc01Facade = mock(FileScenarioExecutionFacade.class);
        when(sc01Facade.supportedScenarioType()).thenReturn("SC-01");
        when(sc01Facade.execute(any())).thenReturn(new ScenarioResponseDTO());

        FileScenarioExecutionFacade sc03Facade = mock(FileScenarioExecutionFacade.class);
        when(sc03Facade.supportedScenarioType()).thenReturn("SC-03");

        FileScenarioDispatcher dispatcher = new FileScenarioDispatcher(List.of(sc01Facade, sc03Facade));
        dispatcher.dispatch(objectMapper.readTree("{\"scenarioType\": \"SC-01\"}"));

        // supportedScenarioType() est appelé dans le constructeur (map building) — on vérifie uniquement execute()
        verify(sc03Facade, never()).execute(any());
    }

    @Test
    void dispatch_leveException_quandScenarioTypeInconnu() throws Exception {
        FileScenarioDispatcher dispatcher = new FileScenarioDispatcher(List.of());

        JsonNode payload = objectMapper.readTree("{\"scenarioType\": \"SC-99\"}");

        assertThatThrownBy(() -> dispatcher.dispatch(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SC-99");
    }

    @Test
    void dispatch_leveException_quandScenarioTypeAbsent() throws Exception {
        FileScenarioDispatcher dispatcher = new FileScenarioDispatcher(List.of());

        JsonNode payload = objectMapper.readTree("{}");

        assertThatThrownBy(() -> dispatcher.dispatch(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scenarioType");
    }

    @Test
    void dispatch_leveException_quandScenarioTypeVide() throws Exception {
        FileScenarioDispatcher dispatcher = new FileScenarioDispatcher(List.of());

        JsonNode payload = objectMapper.readTree("{\"scenarioType\": \"\"}");

        assertThatThrownBy(() -> dispatcher.dispatch(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scenarioType");
    }
}
