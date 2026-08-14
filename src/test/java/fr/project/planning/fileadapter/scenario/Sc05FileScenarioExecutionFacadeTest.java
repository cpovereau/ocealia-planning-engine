package fr.project.planning.fileadapter.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.project.planning.scenarios.dto.ArbitrageDTO;
import fr.project.planning.scenarios.dto.MouvementSalarieDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sc05FileScenarioExecutionFacadeTest — lot A4 de SC-05.
 *
 * <p>Le FileAdapter est le canal de test retenu côté WinDev. SC-05 doit y être accessible au même
 * titre que les quatre autres — et surtout, <strong>y produire exactement le même résultat</strong>.</p>
 *
 * <p>Deux canaux qui appellent le même service peuvent malgré tout diverger : une désérialisation
 * qui ne suit pas les mêmes règles, une valeur par défaut appliquée d'un côté seulement, un bloc
 * omis à la sérialisation. Ce test ne se contente donc pas de vérifier que la voie fichier
 * fonctionne : il <strong>compare les deux réponses entières</strong>, sur deux jeux d'essai.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class Sc05FileScenarioExecutionFacadeTest {

    private static final Path ARBITRAGE =
            Path.of("src/test/resources/scenarios/sc05/sc05_arbitrage.json");
    private static final Path TOLERANCE =
            Path.of("src/test/resources/scenarios/sc05/sc05_tolerance.json");

    @Autowired
    private FileScenarioDispatcher dispatcher;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Le dispatcher route SC-05 vers sa façade")
    void leDispatcherRouteSc05VersSaFacade() throws Exception {
        ScenarioResponseDTO response = dispatcher.dispatch(payload(ARBITRAGE));

        assertEquals("SC-05", response.getScenarioType());
    }

    @Test
    @DisplayName("La voie fichier produit exactement la réponse de la voie HTTP")
    void lesDeuxCanauxProduisentLaMemeReponse() throws Exception {
        for (Path fixture : new Path[]{ARBITRAGE, TOLERANCE}) {
            JsonNode parFichier = objectMapper.valueToTree(dispatcher.dispatch(payload(fixture)));
            JsonNode parHttp = objectMapper.readTree(solveHttp(fixture));

            assertEquals(parHttp, parFichier,
                    fixture + " : les deux canaux divergent. L'un est devenu une variante "
                            + "silencieuse de l'autre.");
        }
    }

    @Test
    @DisplayName("Le bloc arbitrage voyage par fichier, avant et après compris")
    void leBlocArbitrageVoyageParFichier() throws Exception {
        // Un bloc propre au scénario est le premier à disparaître quand un canal est câblé à la
        // hâte : il ne fait partie d'aucun tronc commun. Et c'est l'avant/après qui se perdrait en
        // premier, puisqu'il ne se déduit d'aucune autre partie de la réponse.
        ArbitrageDTO arbitrage = dispatcher.dispatch(payload(TOLERANCE)).getArbitrage();

        assertNotNull(arbitrage, "Le bloc propre à SC-05 doit être présent par la voie fichier.");
        assertEquals(List.of("SAL-A", "SAL-B"), arbitrage.ressourcesArbitrees());
        assertEquals(2, arbitrage.creneauxArbitres());
        assertEquals(1, arbitrage.creneauxDeplaces());
        assertEquals(1, arbitrage.creneauxEpinglesSurUnTiers());
        assertEquals(2, arbitrage.details().size());

        MouvementSalarieDTO a = arbitrage.parSalarie().get(0);
        assertEquals("SAL-A", a.ressourceId());
        assertEquals(40.0, a.heuresAvant());
        assertEquals(32.0, a.heuresApres());
        assertEquals(14.29, a.ecartContratAvantPourcent());
        assertEquals(-8.57, a.ecartContratApresPourcent());
    }

    @Test
    @DisplayName("Sans salarieBId, la voie fichier refuse aussi — mais pas au même endroit")
    void sansSalarieBId_laVoieFichierRefuseElleAussi() throws Exception {
        // @Valid est porté par le contrôleur HTTP : par la voie fichier, seuls jouent les
        // garde-fous du service de préparation. Même refus, message différent — et c'est une
        // différence qu'il vaut mieux avoir écrite que découverte en production.
        ObjectNode sansB = (ObjectNode) payload(ARBITRAGE);
        ((ObjectNode) sansB.get("scenarioParameters")).remove("salarieBId");

        assertThatThrownBy(() -> dispatcher.dispatch(sansB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deux salariés distincts");
    }

    @Test
    @DisplayName("Un paramètre inconnu est refusé par les deux canaux : le bloc est strict")
    void parametreInconnu_estRefuseParLesDeuxCanaux() throws Exception {
        ObjectNode avecIntrus = (ObjectNode) payload(ARBITRAGE);
        ((ObjectNode) avecIntrus.get("scenarioParameters")).put("objectif", "EQUITE_CHARGE");

        assertThatThrownBy(() -> dispatcher.dispatch(avecIntrus))
                .hasMessageContaining("objectif");

        mockMvc.perform(post("/scenarios/sc05/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(avecIntrus)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    // Helpers
    // =========================================================

    private JsonNode payload(Path fixture) throws Exception {
        return objectMapper.readTree(Files.readString(fixture));
    }

    /**
     * Réponse HTTP, lue en UTF-8.
     *
     * <p>{@code getContentAsString()} sans argument décode avec l'encodage déclaré par la réponse,
     * qu'un {@code application/json} ne déclare pas : MockMvc retombe alors sur ISO-8859-1 et les
     * accents reviennent en mojibake. Artefact du harnais, pas du moteur — sans cette précision, la
     * comparaison des deux canaux échouerait sur chaque message d'alerte accentué.</p>
     */
    private String solveHttp(Path fixture) throws Exception {
        return mockMvc.perform(post("/scenarios/sc05/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Files.readString(fixture)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
