package fr.project.planning.scenarios.sc02.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioSc02ReposDominicalTest — lot L0 du chantier équité (V5, bout-en-bout).
 *
 * <h3>La situation jouée</h3>
 * <p>Marie est absente le dimanche 17 mai et son créneau de 08:00 à 16:00 est libre. Deux
 * remplaçants possibles, et aucun ne convient :</p>
 * <ul>
 *   <li><strong>Paul</strong> a déclaré ce dimanche comme repos dominical. Il est pourtant libre à
 *       cette heure-là — c'est bien la règle, et elle seule, qui l'écarte ;</li>
 *   <li><strong>Sophie</strong> travaille déjà de 08:00 à 16:00 ailleurs.</li>
 * </ul>
 *
 * <p>Les huit heures partent donc à pourvoir. <strong>Sans la règle, Paul les prendrait</strong> —
 * c'est ce qui rend ce jeu discriminant plutôt que décoratif.</p>
 *
 * <h3>Et ce que le planning transmis contient déjà</h3>
 * <p>Paul travaille aussi de 18:00 à 22:00 ce même dimanche, dans le planning existant. Le moteur
 * <strong>ne le défait pas</strong> — c'est un fait épinglé, et le pénaliser rendrait le problème
 * insoluble pour une faute qu'il n'a pas commise. Il le <strong>signale</strong>.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioSc02ReposDominicalTest {

    private static final String RHD = "src/test/resources/scenarios/sc02/sc02_repos_dominical.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Un repos dominical déclaré écarte le remplaçant, même s'il est libre")
    void reposDominicalDeclare_ecarteLeRemplacant() throws Exception {
        JsonNode reponse = objectMapper.readTree(postSc02());
        JsonNode remplacement = reponse.get("remplacement");

        assertEquals(0, remplacement.get("creneauxRepris").asInt(),
                "Paul est libre de 08:00 à 16:00 : seule la règle du repos dominical l'écarte.");
        assertEquals(8.0, remplacement.get("heuresAPourvoir").asDouble());

        for (JsonNode detail : remplacement.get("details")) {
            assertNotEquals("SAL-PAUL", detail.path("ressourceApresId").asText(""),
                    "On ne retire pas à quelqu'un son repos dominical déclaré.");
        }
    }

    @Test
    @DisplayName("Le moteur rend visible l'impossible plutôt que de forcer : la solution reste valide")
    void leProblemeResteSoluble() throws Exception {
        JsonNode solverResult = objectMapper.readTree(postSc02()).get("solverResult");

        assertEquals("SOLVED", solverResult.get("status").asText());
        assertEquals(0, solverResult.get("score").get("hard").asInt(),
                "Interdire de confier n'est jamais interdire de résoudre : le créneau part à "
                        + "pourvoir, et le score dur reste intact.");
    }

    @Test
    @DisplayName("Le repos déjà travaillé dans le planning transmis est signalé, pas pesé")
    void reposDejaTravailleDansLExistant_estSignale() throws Exception {
        // Paul travaille son dimanche de repos de 18:00 à 22:00 dans la demande. Le créneau est
        // épinglé : le moteur ne l'a pas décidé et ne peut pas le défaire.
        JsonNode reponse = objectMapper.readTree(postSc02());

        boolean signale = false;
        for (JsonNode alerte : reponse.get("diagnostics").get("alerts")) {
            if ("REPOS_DOMINICAL_TRAVAILLE".equals(alerte.get("code").asText())) {
                signale = true;
                assertEquals("WARNING", alerte.get("severity").asText());
                assertTrue(alerte.get("message").asText().contains("SAL-PAUL"));
                assertTrue(alerte.get("message").asText().contains("PLN-PAUL-DIM-SOIR"),
                        "Le message doit nommer le créneau en cause, pas seulement la personne.");
            }
        }
        assertTrue(signale, "L'appelant doit savoir que sa demande contient ce que le moteur "
                + "refuserait de produire.");

        assertEquals("SAL-PAUL", ressourceDe(reponse.get("planning"), "PLN-PAUL-DIM-SOIR"),
                "Signalé ne veut pas dire défait : l'existant reste en place.");
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static String ressourceDe(JsonNode planning, String creneauId) {
        for (JsonNode jour : planning.get("jours")) {
            for (JsonNode creneau : jour.get("creneaux")) {
                if (creneauId.equals(creneau.get("id").asText())) {
                    return creneau.get("ressourceAffecteeId").asText();
                }
            }
        }
        throw new AssertionError("Créneau absent du planning restitué : " + creneauId);
    }

    private String postSc02() throws Exception {
        return mockMvc.perform(post("/scenarios/sc02/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(Files.readString(Path.of(RHD)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
