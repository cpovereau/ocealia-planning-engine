package fr.project.planning.fileadapter.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.scenarios.dto.OptimisationDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.dto.SerieSalarieDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sc04FileScenarioExecutionFacadeTest — lot O4 de SC-04.
 *
 * <p>Le FileAdapter est le canal de test retenu côté WinDev, et SC-04 est le scénario qui
 * l'empruntera le plus : il porte une période large, donc un dataset volumineux, et ne s'exécute
 * pas en interactif.</p>
 *
 * <p>Deux canaux qui appellent le même service peuvent malgré tout diverger : une désérialisation
 * qui ne suit pas les mêmes règles, une valeur par défaut appliquée d'un côté seulement, un bloc
 * omis à la sérialisation. Ce test ne se contente donc pas de vérifier que la voie fichier
 * fonctionne : il <strong>compare les deux réponses entières</strong>.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class Sc04FileScenarioExecutionFacadeTest {

    private static final Path REFERENCE =
            Path.of("src/test/resources/scenarios/sc04/sc04_reference.json");

    @Autowired
    private FileScenarioDispatcher dispatcher;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Le dispatcher route SC-04 vers sa façade")
    void leDispatcherRouteSc04VersSaFacade() throws Exception {
        ScenarioResponseDTO response = dispatcher.dispatch(payload(REFERENCE));

        assertEquals("SC-04", response.getScenarioType());
    }

    @Test
    @DisplayName("La voie fichier produit exactement la réponse de la voie HTTP")
    void lesDeuxCanauxProduisentLaMemeReponse() throws Exception {
        JsonNode parFichier = objectMapper.valueToTree(dispatcher.dispatch(payload(REFERENCE)));
        JsonNode parHttp = objectMapper.readTree(solveHttp(REFERENCE));

        assertEquals(parHttp, parFichier,
                "Les deux canaux divergent : l'un est devenu une variante silencieuse de l'autre.");
    }

    @Test
    @DisplayName("Le bloc optimisation voyage par fichier, séries comprises")
    void leBlocOptimisationVoyageParFichier() throws Exception {
        OptimisationDTO optimisation = dispatcher.dispatch(payload(REFERENCE)).getOptimisation();

        assertNotNull(optimisation, "Le bloc propre à SC-04 doit être présent par la voie fichier.");
        assertEquals(10, optimisation.creneauxFiges());
        assertEquals(10, optimisation.creneauxAjustables());
        assertFalse(optimisation.parSalarie().isEmpty());

        SerieSalarieDTO premier = optimisation.parSalarie().get(0);
        assertFalse(premier.tranches().isEmpty(),
                "Les séries sont l'apport de SC-04 : les perdre en route le viderait de son objet.");
    }

    private JsonNode payload(Path fixture) throws Exception {
        return objectMapper.readTree(Files.readString(fixture, StandardCharsets.UTF_8));
    }

    private String solveHttp(Path fixture) throws Exception {
        return mockMvc.perform(post("/scenarios/sc04/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(Files.readString(fixture, StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
