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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioSc02SurchargeTest — lot S3 de SC-02 (V5, bout-en-bout).
 *
 * <h3>La situation jouée</h3>
 * <p>Paul travaille lundi 8 h, mardi 7 h, mercredi 8 h et jeudi 8 h — <strong>31 h</strong> sur la
 * semaine. Marie est absente le mardi, et son créneau de 17h à 21h est libre. Paul finit à 16h : il
 * peut le prendre, et personne d'autre ne le peut.</p>
 *
 * <p>La demande déclare deux seuils : <strong>8 h par jour</strong> et <strong>34 h par
 * semaine</strong>. En reprenant les 4 h, Paul passe à 11 h ce mardi et à 35 h sur la semaine : il
 * franchit les deux.</p>
 *
 * <p>Le moteur le lui confie quand même, et le dit. C'est tout l'arbitrage : un seuil de surcharge
 * est une borne de confort, <strong>pesée et signalée, jamais éliminatoire</strong>.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioSc02SurchargeTest {

    private static final String SURCHARGE = "src/test/resources/scenarios/sc02/sc02_surcharge.json";
    private static final String REFERENCE = "src/test/resources/scenarios/sc02/sc02_reference.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Le dépassement n'empêche pas le remplacement : il est pesé, pas interdit")
    void seuilDepasse_nEmpechePasLeRemplacement() throws Exception {
        JsonNode remplacement = objectMapper.readTree(postSc02(SURCHARGE)).get("remplacement");

        assertEquals(1, remplacement.get("creneauxRepris").asInt(),
                "Paul reprend le créneau bien qu'il dépasse les deux seuils.");
        assertEquals(0.0, remplacement.get("heuresAPourvoir").asDouble(),
                "Laisser des heures à pourvoir coûterait plus cher que la surcharge — "
                        + "c'est exactement ce que la calibration de la pénalité doit garantir.");
    }

    @Test
    @DisplayName("La charge du jour est restituée avant, après et delta, avec son seuil")
    void chargeDuJour_estRestituee() throws Exception {
        JsonNode surcharge = surchargeDe(SURCHARGE, "SAL-PAUL");
        JsonNode jour = surcharge.get("heuresJour");

        assertEquals("2026-05-12", surcharge.get("date").asText());
        assertEquals(7.0, jour.get("avant").asDouble(), "Son mardi déjà planifié : 9h–16h.");
        assertEquals(11.0, jour.get("apres").asDouble(), "Plus les 4 h de Marie.");
        assertEquals(4.0, jour.get("delta").asDouble());
        assertEquals(8.0, jour.get("plafond").asDouble());
        assertTrue(jour.get("depassement").asBoolean());
    }

    @Test
    @DisplayName("La semaine est mesurée à part : elle peut être franchie sans que le jour le soit")
    void chargeDeLaSemaine_estMesureeAPart() throws Exception {
        JsonNode semaine = surchargeDe(SURCHARGE, "SAL-PAUL").get("heuresSemaine");

        assertEquals(31.0, semaine.get("avant").asDouble(), "8 + 7 + 8 + 8.");
        assertEquals(35.0, semaine.get("apres").asDouble());
        assertEquals(34.0, semaine.get("plafond").asDouble());
        assertTrue(semaine.get("depassement").asBoolean());
    }

    @Test
    @DisplayName("Chaque dépassement est signalé, et le message nomme la personne et le seuil")
    void depassements_sontSignales() throws Exception {
        JsonNode alerts = objectMapper.readTree(postSc02(SURCHARGE))
                .get("diagnostics").get("alerts");

        int signales = 0;
        for (JsonNode alerte : alerts) {
            if ("SURCHARGE_ACCEPTABLE_DEPASSEE".equals(alerte.get("code").asText())) {
                signales++;
                assertEquals("WARNING", alerte.get("severity").asText(),
                        "Une borne de confort ne produit pas une erreur.");
                assertTrue(alerte.get("message").asText().contains("SAL-PAUL"));
            }
        }
        assertEquals(2, signales, "Un signalement pour la journée, un pour la semaine.");
    }

    @Test
    @DisplayName("Le dépassement pèse dans le score, et il y pèse le montant attendu")
    void depassement_peseDansLeScore() throws Exception {
        // Sans cette vérification, rien ne prouverait que la contrainte existe : la mesure et
        // l'alerte sont produites par la restitution, qui ne consulte pas le solveur.
        JsonNode breakdown = objectMapper.readTree(postSc02(SURCHARGE))
                .get("solverResult").get("scoreBreakdown");

        assertEquals(900, impact(breakdown, "METIER_SOFT_SURCHARGE_JOUR"),
                "3 h d'excédent, soit 180 minutes à 5 points.");
        assertEquals(300, impact(breakdown, "METIER_SOFT_SURCHARGE_SEMAINE"),
                "1 h d'excédent, soit 60 minutes à 5 points.");
    }

    @Test
    @DisplayName("Sans seuil déclaré, aucun dépassement : une borne absente n'est pas une borne à zéro")
    void sansSeuil_aucunDepassement() throws Exception {
        // Le jeu de référence ne déclare aucun seuil. Paul y reprend pourtant une journée entière.
        JsonNode reponse = objectMapper.readTree(postSc02(REFERENCE));
        JsonNode surcharges = reponse.get("remplacement").get("surchargeParRessource");

        assertFalse(surcharges.isEmpty(), "La mesure est rendue même sans seuil : elle informe.");
        for (JsonNode surcharge : surcharges) {
            assertFalse(surcharge.get("heuresJour").get("depassement").asBoolean());
            assertFalse(surcharge.get("heuresSemaine").get("depassement").asBoolean());
            assertTrue(surcharge.get("heuresJour").get("plafond").isNull(),
                    "Aucun seuil déclaré, aucun plafond affiché.");
        }

        for (JsonNode alerte : reponse.get("diagnostics").get("alerts")) {
            assertFalse("SURCHARGE_ACCEPTABLE_DEPASSEE".equals(alerte.get("code").asText()));
        }
    }

    // =========================================================
    // Helpers
    // =========================================================

    /** Impact pondéré d'une clé de pénalité, en valeur absolue. */
    private static int impact(JsonNode breakdown, String penaliteKey) {
        for (JsonNode item : breakdown) {
            if (penaliteKey.equals(item.get("penaliteKey").asText())) {
                return Math.abs(item.get("weightedImpact").asInt());
            }
        }
        throw new AssertionError("Clé absente du scoreBreakdown : " + penaliteKey);
    }

    private JsonNode surchargeDe(String fixture, String ressourceId) throws Exception {
        JsonNode surcharges = objectMapper.readTree(postSc02(fixture))
                .get("remplacement").get("surchargeParRessource");
        for (JsonNode surcharge : surcharges) {
            if (ressourceId.equals(surcharge.get("ressourceId").asText())) {
                return surcharge;
            }
        }
        throw new AssertionError("Aucune surcharge restituée pour " + ressourceId);
    }

    private String postSc02(String chemin) throws Exception {
        return mockMvc.perform(post("/scenarios/sc02/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(Files.readString(Path.of(chemin)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
