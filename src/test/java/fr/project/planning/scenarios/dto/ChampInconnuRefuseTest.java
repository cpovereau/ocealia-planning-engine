package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.project.planning.fileadapter.scenario.FileScenarioDispatcher;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChampInconnuRefuseTest — rang 11 du backlog.
 *
 * <h3>Ce qui n'allait pas</h3>
 * <p>Le contrat annonçait, depuis la phase 10B, que « tout champ inconnu est rejeté ». Il ne
 * l'était pas : Spring Boot désactive {@code FAIL_ON_UNKNOWN_PROPERTIES}, et retirer un
 * {@code @JsonIgnoreProperties} d'un DTO ne le rend donc pas strict. Un appelant qui envoyait un
 * champ que le moteur ne connaît pas recevait une réponse <strong>200</strong> et la conviction
 * d'avoir été entendu.</p>
 *
 * <p>Le test qui prétendait le prouver — {@code StrictDeserializationPhase10CTest}, que celui-ci
 * remplace — construisait <strong>son propre {@code ObjectMapper}</strong>. Jackson est strict par
 * défaut : le test passait donc sur un moteur imaginaire, pendant que l'application, elle,
 * ignorait en silence. D'où la règle que cette classe applique : <strong>on teste le mapper de
 * l'application</strong>, injecté, et de préférence par un aller-retour HTTP réel.</p>
 *
 * <h3>Ce qui reste toléré, et pourquoi c'est nommé</h3>
 * <p>Quatre champs de créneau sont marqués « encore accepté et silencieusement ignoré ; ne plus
 * émettre » dans le schéma publié. Ce ne sont pas des inconnus, ce sont des retraités : ils sont
 * déclarés nommément, et refuser une requête pour un champ que le contrat dit d'ignorer casserait
 * une migration en cours — cinq jeux d'essai du projet en émettent encore.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ChampInconnuRefuseTest {

    private static final Path SC03 = Path.of("src/test/resources/scenarios/sc03/sc03_hors_horizon.json");
    private static final Path SC02 = Path.of("src/test/resources/scenarios/sc02/sc02_reference.json");

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileScenarioDispatcher dispatcher;

    // =========================================================
    // Le refus
    // =========================================================

    @Test
    @DisplayName("Un champ inconnu fait refuser la requête, et la réponse dit lequel et où")
    void champInconnu_estRefuseEtLocalise() throws Exception {
        // La faute est placée sur le SECOND créneau : le seul nom du champ ne suffirait pas dans
        // une requête qui en porte quatre-vingts.
        ObjectNode requete = (ObjectNode) lire(SC03);
        ((ObjectNode) requete.get("dataSet").get("creneaux").get(1)).put("couleur", "rouge");

        JsonNode erreur = postAttendu(requete, "/scenarios/sc03/solve", 400).get("error");

        assertEquals("UNKNOWN_FIELD", erreur.get("code").asText(),
                "Un champ inconnu n'est pas un JSON mal formé : la syntaxe est parfaite.");
        assertEquals("dataSet.creneaux[1].couleur", erreur.get("details").get(0).get("field").asText());
        assertThat(erreur.get("details").get(0).get("message").asText())
                .as("L'appelant doit repartir avec la liste de ce qui est accepté à cet endroit")
                .contains("codeActiviteId")
                .contains("heureDebut");
    }

    @Test
    @DisplayName("Un paramètre de scénario inconnu est refusé lui aussi, et nommé")
    void parametreDeScenarioInconnu_estRefuse() throws Exception {
        // Le cas qui a motivé le rang : le cadrage SC-02 annonce remplacantsAutorises, aucune
        // règle ne le lit. L'envoyer rendait 200 — l'appelant croyait avoir restreint la liste.
        ObjectNode requete = (ObjectNode) lire(SC02);
        ((ObjectNode) requete.get("scenarioParameters")).put("remplacantsAutorises", "SAL-PAUL");

        JsonNode erreur = postAttendu(requete, "/scenarios/sc02/solve", 400).get("error");

        assertEquals("UNKNOWN_FIELD", erreur.get("code").asText());
        assertEquals("scenarioParameters.remplacantsAutorises",
                erreur.get("details").get(0).get("field").asText());
    }

    @Test
    @DisplayName("Le canal fichier refuse le même champ : les deux canaux partagent la règle")
    void leCanalFichier_refuseAussi() throws Exception {
        ObjectNode requete = (ObjectNode) lire(SC02);
        ((ObjectNode) requete.get("scenarioParameters")).put("remplacantsAutorises", "SAL-PAUL");

        assertThatThrownBy(() -> dispatcher.dispatch(requete))
                .hasMessageContaining("remplacantsAutorises");
    }

    // =========================================================
    // Ce qui reste accepté
    // =========================================================

    @Test
    @DisplayName("Les quatre champs dépréciés restent acceptés : une migration en cours ne casse pas")
    void champsDepreciés_restentAcceptes() throws Exception {
        // Le jeu d'essai émet priorite et type — comme le font encore cinq jeux du projet, et
        // vraisemblablement l'appelant. Le schéma publié promet de les accepter et de les ignorer.
        postAttendu(lire(SC03), "/scenarios/sc03/solve", 200);

        for (String deprecie : new String[]{"priorite", "type", "isReposHebdo", "axesOrganisationnels"}) {
            String json = "{\"id\":\"CRE-001\",\"heureDebut\":\"07:00\",\"heureFin\":\"15:00\","
                    + "\"" + deprecie + "\":null}";
            CreneauInputDTO creneau = objectMapper.readValue(json, CreneauInputDTO.class);
            assertEquals("CRE-001", creneau.getId(),
                    "'" + deprecie + "' est un retraité déclaré, pas un inconnu.");
        }
    }

    @Test
    @DisplayName("activite reste accepté ET lu : déprécié ne veut pas dire ignoré")
    void activiteDepreciee_resteLue() throws Exception {
        String json = "{\"id\":\"CRE-001\",\"heureDebut\":\"07:00\",\"heureFin\":\"15:00\","
                + "\"activite\":\"Soins infirmiers\"}";

        CreneauInputDTO creneau = objectMapper.readValue(json, CreneauInputDTO.class);

        assertEquals("Soins infirmiers", creneau.getActivite(),
                "Le repli sur activite est encore actif quand codeActiviteId manque.");
        assertNull(creneau.getCodeActiviteId());
    }

    @Test
    @DisplayName("Une requête conforme passe : le durcissement n'a rien cassé au passage")
    void requeteConforme_passe() throws Exception {
        postAttendu(lire(SC02), "/scenarios/sc02/solve", 200);
    }

    // =========================================================
    // Helpers
    // =========================================================

    private JsonNode lire(Path fixture) throws Exception {
        return objectMapper.readTree(Files.readString(fixture));
    }

    private JsonNode postAttendu(JsonNode corps, String route, int statut) throws Exception {
        String reponse = mockMvc.perform(post(route)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(corps)))
                .andExpect(status().is(statut))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(reponse);
    }
}
