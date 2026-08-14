package fr.project.planning.fileadapter.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.project.planning.scenarios.dto.RemplacementDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sc02FileScenarioExecutionFacadeTest — lot S5 de SC-02.
 *
 * <p>Le FileAdapter est le canal de test retenu côté WinDev. SC-02 doit y être accessible au même
 * titre que SC-01, SC-03 et SC-06 — et surtout, <strong>y produire exactement le même
 * résultat</strong>.</p>
 *
 * <p>Deux canaux qui appellent le même service peuvent malgré tout diverger : une désérialisation
 * qui ne suit pas les mêmes règles, une valeur par défaut appliquée d'un côté seulement, un bloc
 * omis à la sérialisation. Ce test ne se contente donc pas de vérifier que la voie fichier
 * fonctionne : il <strong>compare les deux réponses entières</strong>. Un test qui réaffirme les
 * mêmes valeurs des deux côtés ne prouve rien de plus que ce que son auteur a pensé à recopier.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class Sc02FileScenarioExecutionFacadeTest {

    private static final Path REFERENCE =
            Path.of("src/test/resources/scenarios/sc02/sc02_reference.json");
    private static final Path POSTE_VIRTUEL =
            Path.of("src/test/resources/scenarios/sc02/sc02_poste_virtuel.json");

    @Autowired
    private FileScenarioDispatcher dispatcher;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Le dispatcher route SC-02 vers sa façade")
    void leDispatcherRouteSc02VersSaFacade() throws Exception {
        ScenarioResponseDTO response = dispatcher.dispatch(payload(REFERENCE));

        assertEquals("SC-02", response.getScenarioType());
    }

    @Test
    @DisplayName("La voie fichier produit exactement la réponse de la voie HTTP")
    void lesDeuxCanauxProduisentLaMemeReponse() throws Exception {
        for (Path fixture : new Path[]{REFERENCE, POSTE_VIRTUEL}) {
            JsonNode parFichier = objectMapper.valueToTree(dispatcher.dispatch(payload(fixture)));
            JsonNode parHttp = objectMapper.readTree(solveHttp(fixture));

            assertEquals(parHttp, parFichier,
                    fixture + " : les deux canaux divergent. L'un est devenu une variante "
                            + "silencieuse de l'autre.");
        }
    }

    @Test
    @DisplayName("Le bloc remplacement voyage par fichier, volumes compris")
    void leBlocRemplacementVoyageParFichier() throws Exception {
        // Un bloc propre au scénario est le premier à disparaître quand un canal est câblé à la
        // hâte : il ne fait partie d'aucun tronc commun.
        RemplacementDTO remplacement = dispatcher.dispatch(payload(REFERENCE)).getRemplacement();

        assertNotNull(remplacement, "Le bloc propre à SC-02 doit être présent par la voie fichier.");
        assertEquals(16.0, remplacement.heuresLiberees());
        assertEquals(9.0, remplacement.heuresReprises());
        assertEquals(7.0, remplacement.heuresAPourvoir());
        assertEquals(3, remplacement.details().size());
        assertEquals(2, remplacement.surchargeParRessource().size());
    }

    @Test
    @DisplayName("Sans salarieAbsentId, la voie fichier refuse aussi — mais pas au même endroit")
    void sansSalarieAbsentId_laVoieFichierRefuseElleAussi() throws Exception {
        // @Valid est porté par le contrôleur HTTP : par la voie fichier, seuls jouent les
        // garde-fous du service de préparation. Même refus, message différent — et c'est une
        // différence qu'il vaut mieux avoir écrite que découverte en production.
        ObjectNode sansAbsent = (ObjectNode) payload(REFERENCE);
        ((ObjectNode) sansAbsent.get("scenarioParameters")).remove("salarieAbsentId");

        assertThatThrownBy(() -> dispatcher.dispatch(sansAbsent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salarieAbsentId");

        mockMvc.perform(post("/scenarios/sc02/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(sansAbsent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un paramètre inconnu est refusé par les deux canaux : le bloc est strict")
    void parametreInconnu_estRefuseParLesDeuxCanaux() throws Exception {
        ObjectNode avecIntrus = (ObjectNode) payload(REFERENCE);
        ((ObjectNode) avecIntrus.get("scenarioParameters")).put("remplacantsAutorises", "SAL-PAUL");

        assertThatThrownBy(() -> dispatcher.dispatch(avecIntrus))
                .hasMessageContaining("remplacantsAutorises");

        mockMvc.perform(post("/scenarios/sc02/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(avecIntrus)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Les cinq scénarios exposés sont routables par fichier")
    void lesCinqScenariosSontRoutablesParFichier() throws Exception {
        // [A4] SC-05 rejoint les quatre autres. Le dispatcher enregistre automatiquement tout bean
        // implémentant FileScenarioExecutionFacade : ce test vérifie qu'aucun n'a été oublié.
        for (String type : new String[]{"SC-01", "SC-02", "SC-03", "SC-05", "SC-06"}) {
            JsonNode payload = objectMapper.readTree("{\"scenarioType\": \"" + type + "\"}");

            // La façade est atteinte : elle échoue sur le contenu, pas sur le routage.
            assertThatThrownBy(() -> dispatcher.dispatch(payload))
                    .as("%s doit être routé, pas rejeté comme type inconnu", type)
                    .satisfies(e -> assertThat(e.getMessage())
                            .doesNotContain("non supporté par le FileAdapter"));
        }
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
     * <p>{@code getContentAsString()} sans argument décode avec l'encodage déclaré par la réponse.
     * Un {@code application/json} n'en déclare pas — JSON est UTF-8 par spécification (RFC 8259) —
     * et MockMvc retombe alors sur le défaut servlet, ISO-8859-1 : les accents reviennent en
     * mojibake. C'est un artefact du harnais de test, pas du moteur ; les octets émis sont
     * corrects. Sans cette précision, la comparaison des deux canaux échouerait sur chaque message
     * d'alerte accentué.</p>
     */
    private String solveHttp(Path fixture) throws Exception {
        return mockMvc.perform(post("/scenarios/sc02/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Files.readString(fixture)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
