package fr.project.planning.fileadapter.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.scenarios.dto.CandidatDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sc06FileScenarioExecutionFacadeTest — lot S6
 *
 * <p>Le FileAdapter est le canal de test initial retenu côté WinDev : SC-06 doit y être
 * accessible au même titre que SC-01 et SC-03.</p>
 *
 * <p>Le dispatcher enregistre automatiquement toutes les façades déclarées comme beans. Ce test
 * vérifie que SC-06 y est bien routé et que le résultat est identique à celui de la voie HTTP —
 * l'un des deux canaux ne doit pas devenir une variante silencieuse de l'autre.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
class Sc06FileScenarioExecutionFacadeTest {

    private static final Path REFERENCE = Path.of("src/test/resources/scenarios/sc06/sc06_reference.json");

    @Autowired
    private FileScenarioDispatcher dispatcher;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void leDispatcherRouteSc06VersSaFacade() throws Exception {
        JsonNode payload = objectMapper.readTree(Files.readString(REFERENCE));

        ScenarioResponseDTO response = dispatcher.dispatch(payload);

        assertEquals("SC-06", response.getScenarioType());
    }

    @Test
    void laVoieFichierProduitLeMemePodiumQueLaVoieHttp() throws Exception {
        JsonNode payload = objectMapper.readTree(Files.readString(REFERENCE));

        ScenarioResponseDTO response = dispatcher.dispatch(payload);
        List<CandidatDTO> candidats = response.getCandidats();

        assertNotNull(candidats, "Le bloc candidats[] doit être présent par la voie fichier aussi.");
        assertEquals(3, candidats.size());
        assertEquals("SAL-2001", candidats.get(0).getAffectations().get(0).getRessourceId());
        assertEquals("SAL-2002", candidats.get(1).getAffectations().get(0).getRessourceId());
        assertEquals("SAL-2003", candidats.get(2).getAffectations().get(0).getRessourceId());
        assertTrue(candidats.get(0).isConforme());
        assertTrue(candidats.get(2).getMotifs().stream()
                        .anyMatch(m -> "REPOS_QUOTIDIEN_INSUFFISANT".equals(m.getCode())),
                "Le motif éliminatoire doit être restitué par la voie fichier également.");
    }

    @Test
    void lesImpactsSontRestituesParLaVoieFichier() throws Exception {
        JsonNode payload = objectMapper.readTree(Files.readString(REFERENCE));

        ScenarioResponseDTO response = dispatcher.dispatch(payload);

        assertEquals(1, response.getCandidats().get(0).getImpacts().size());
        assertEquals(14.0,
                response.getCandidats().get(0).getImpacts().get(0)
                        .getAmplitudeJournaliere().getApres());
    }
}
