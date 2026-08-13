package fr.project.planning.scenarios.sc05.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioSc05ArbitrageTest — lot A1 de SC-05 (bout-en-bout).
 *
 * <h3>Le cas</h3>
 * <pre>
 * SAL-A      lundi · mardi · mercredi · jeudi, 8 h    → 32 h   hors périmètre, épinglé
 * SAL-B      lundi · mardi · mercredi, 8 h            → 24 h   hors périmètre, épinglé
 * SAL-TIERS  samedi 08:00–12:00                       →  4 h   DANS le périmètre
 *
 * PLN-ARB-V  vendredi 8 h, tenu par SAL-A             → DANS le périmètre, remis en jeu
 * </pre>
 *
 * <h3>Ce que ce cas prouve, et qu'un cas plus simple ne prouverait pas</h3>
 * <p>SAL-TIERS est de loin le plus sous-servi des trois. <strong>Sans la contrainte HARD du lot
 * A0, le score lui donnerait le créneau arbitré</strong> — c'est l'affectation la moins inéquitable
 * de toutes (77 points contre 79 pour SAL-B). Le jeu d'essai est construit pour que l'arbitrage
 * borné et l'arbitrage libre <em>ne donnent pas le même résultat</em> : sans quoi le test passerait
 * même si le périmètre n'était pas transmis au solveur.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioSc05ArbitrageTest {

    private static final Path ARBITRAGE = Path.of("src/test/resources/scenarios/sc05/sc05_arbitrage.json");

    private static final String CRENEAU_ARBITRE = "PLN-ARB-V";
    private static final String CRENEAU_DU_TIERS = "PLN-ARB-TIERS";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    // =========================================================
    // Ce que l'arbitrage borne
    // =========================================================

    @Test
    @DisplayName("Le créneau arbitré ne peut revenir qu'à l'un des deux salariés désignés")
    void leCreneauArbitre_neRevientQuAuxDeuxDesignes() throws Exception {
        // Sans la borne, le score le donnerait à SAL-TIERS, le plus sous-servi des trois.
        String repreneur = titulaire(solve(avecTolerance(10.0)), CRENEAU_ARBITRE);

        assertTrue(List.of("SAL-A", "SAL-B").contains(repreneur),
                "Le créneau du périmètre est revenu à '" + repreneur + "', hors de l'arbitrage.");
        assertEquals("SAL-B", repreneur,
                "SAL-B est à 24 h contre 32 h pour SAL-A : c'est la répartition la moins inéquitable "
                        + "parmi celles que la borne autorise.");
    }

    @Test
    @DisplayName("Sans tolérance déclarée, la borne s'applique quand même")
    void sansTolerance_laBorneSApplique() throws Exception {
        // La contrainte HARD ne dépend d'aucune équité : elle dit à qui un créneau peut revenir,
        // pas lequel est le plus juste. Confondre les deux ferait de l'arbitrage une préférence.
        String repreneur = titulaire(solve(lire()), CRENEAU_ARBITRE);

        assertTrue(List.of("SAL-A", "SAL-B").contains(repreneur),
                "Le créneau du périmètre est revenu à '" + repreneur + "', hors de l'arbitrage.");
    }

    @Test
    @DisplayName("La borne ne rend pas le problème insoluble : aucun point HARD")
    void laBorne_neRendPasLeProblemeInsoluble() throws Exception {
        assertEquals(0, solve(avecTolerance(10.0)).get("solverResult").get("score").get("hard").asInt(),
                "Une contrainte HARD sans issue ne rend pas visible l'impossible : elle le cache.");
    }

    // =========================================================
    // Ce que l'arbitrage ne touche pas
    // =========================================================

    @Test
    @DisplayName("Hors du périmètre, rien ne bouge — y compris chez A et chez B")
    void horsDuPerimetre_rienNeBouge() throws Exception {
        // SC-05 exige le planning complet de la période pour que les bornes hebdomadaires soient
        // vérifiables — pas pour le remanier.
        JsonNode reponse = solve(avecTolerance(10.0));

        for (String creneau : List.of("PLN-A-L", "PLN-A-MA", "PLN-A-ME", "PLN-A-J")) {
            assertEquals("SAL-A", titulaire(reponse, creneau));
        }
        for (String creneau : List.of("PLN-B-L", "PLN-B-MA", "PLN-B-ME")) {
            assertEquals("SAL-B", titulaire(reponse, creneau));
        }
    }

    @Test
    @DisplayName("Un créneau du périmètre tenu par un tiers est épinglé, et signalé")
    void creneauTenuParUnTiers_estEpingleEtSignale() throws Exception {
        // §5.2 : ni refus, ni reprise. Le tiers n'a rien demandé ; on ne lui retire pas son travail
        // pour équilibrer deux autres personnes. Mais l'appelant l'a désigné : il doit l'apprendre.
        JsonNode reponse = solve(avecTolerance(10.0));

        assertEquals("SAL-TIERS", titulaire(reponse, CRENEAU_DU_TIERS),
                "Le créneau du tiers a changé de main : l'existant ne se réécrit pas.");

        JsonNode alerte = alerte(reponse, "CRENEAU_ARBITRE_TENU_PAR_UN_TIERS");
        assertNotNull(alerte, "Sans l'alerte, l'appelant lirait une répartition qui ignore un "
                + "créneau qu'il a désigné, sans savoir pourquoi.");
        assertTrue(alerte.get("message").asText().contains(CRENEAU_DU_TIERS),
                "L'alerte doit nommer le créneau écarté.");
    }

    // =========================================================
    // Ce que la préparation refuse, et ce qu'elle signale
    // =========================================================

    @Test
    @DisplayName("Arbitrer quelqu'un avec lui-même est refusé")
    void memeSalarieDesDeuxCotes_estRefuse() throws Exception {
        ObjectNode requete = (ObjectNode) lire();
        ((ObjectNode) requete.get("scenarioParameters")).put("salarieBId", "SAL-A");

        mockMvc.perform(post("/scenarios/sc05/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Un périmètre vide est refusé : il est transmis, jamais déduit")
    void perimetreVide_estRefuse() throws Exception {
        ObjectNode requete = (ObjectNode) lire();
        ((ObjectNode) requete.get("scenarioParameters"))
                .set("creneauxArbitres", objectMapper.createArrayNode());

        mockMvc.perform(post("/scenarios/sc05/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un créneau du périmètre absent du dataset est signalé, l'arbitrage continue")
    void creneauDuPerimetreIntrouvable_estSignale() throws Exception {
        ObjectNode requete = (ObjectNode) lire();
        ((ArrayNode) requete.get("scenarioParameters").get("creneauxArbitres")).add("PLN-FANTOME");

        JsonNode reponse = solve(requete);

        JsonNode alerte = alerte(reponse, "CRENEAU_ARBITRE_INTROUVABLE");
        assertNotNull(alerte, "Un périmètre partiellement inconnu rendrait l'arbitrage plus étroit "
                + "que demandé, sans que personne ne le voie.");
        assertTrue(alerte.get("message").asText().contains("PLN-FANTOME"));
    }

    @Test
    @DisplayName("Un salarié de l'arbitrage absent du dataset est signalé")
    void salarieIntrouvable_estSignale() throws Exception {
        ObjectNode requete = (ObjectNode) lire();
        ((ObjectNode) requete.get("scenarioParameters")).put("salarieBId", "SAL-FANTOME");

        JsonNode alerte = alerte(solve(requete), "SALARIE_ARBITRE_INTROUVABLE");
        assertNotNull(alerte, "Sans lui, les créneaux du périmètre ne peuvent revenir qu'aux "
                + "autres, ou rester à pourvoir — l'appelant doit le savoir.");
        assertTrue(alerte.get("message").asText().contains("SAL-FANTOME"));
    }

    @Test
    @DisplayName("Un arbitrage sans anomalie ne produit aucune alerte de périmètre")
    void arbitrageOrdinaire_neSignaleQueLeTiers() throws Exception {
        // Le pendant indispensable des trois tests ci-dessus : une alerte qui se déclenche toujours
        // n'est pas un signal.
        JsonNode reponse = solve(avecTolerance(10.0));

        assertNull(alerte(reponse, "CRENEAU_ARBITRE_INTROUVABLE"));
        assertNull(alerte(reponse, "SALARIE_ARBITRE_INTROUVABLE"));
    }

    // =========================================================
    // Le bloc arbitrage — lot A2
    // =========================================================

    @Test
    @DisplayName("Le bloc arbitrage dit ce qui a bougé, et combien")
    void leBlocArbitrage_ditCeQuiABouge() throws Exception {
        JsonNode arbitrage = solve(avecTolerance(10.0)).get("arbitrage");

        assertNotNull(arbitrage, "Le bloc arbitrage est à SC-05 ce que remplacement est à SC-02.");
        assertEquals(2, arbitrage.get("creneauxArbitres").asInt());
        assertEquals(1, arbitrage.get("creneauxDeplaces").asInt(), "Seul PLN-ARB-V change de main.");
        assertEquals(1, arbitrage.get("creneauxEpinglesSurUnTiers").asInt());
        assertEquals(0, arbitrage.get("creneauxNonCouverts").asInt());
        assertEquals(List.of("SAL-A", "SAL-B"),
                List.of(arbitrage.get("ressourcesArbitrees").get(0).asText(),
                        arbitrage.get("ressourcesArbitrees").get(1).asText()));
    }

    @Test
    @DisplayName("L'avant et l'après sont mesurés, et c'est leur comparaison qui justifie")
    void lAvantEtLApres_sontMesures() throws Exception {
        // SAL-A cède le vendredi : 40 h → 32 h, et son écart au contrat passe de +14,29 % à
        // −8,57 %. SAL-B le reprend : 24 h → 32 h, de −31,43 % à −8,57 %. C'est ce rapprochement
        // qui rend l'arbitrage justifiable ligne à ligne devant les deux intéressés.
        JsonNode parSalarie = solve(avecTolerance(10.0)).get("arbitrage").get("parSalarie");

        JsonNode a = mouvement(parSalarie, "SAL-A");
        assertEquals(0, a.get("creneauxRepris").asInt());
        assertEquals(1, a.get("creneauxCedes").asInt());
        assertEquals(40.0, a.get("heuresAvant").asDouble(), 0.01);
        assertEquals(32.0, a.get("heuresApres").asDouble(), 0.01);
        assertEquals(14.29, a.get("ecartContratAvantPourcent").asDouble(), 0.01);
        assertEquals(-8.57, a.get("ecartContratApresPourcent").asDouble(), 0.01);

        JsonNode b = mouvement(parSalarie, "SAL-B");
        assertEquals(1, b.get("creneauxRepris").asInt());
        assertEquals(0, b.get("creneauxCedes").asInt());
        assertEquals(24.0, b.get("heuresAvant").asDouble(), 0.01);
        assertEquals(32.0, b.get("heuresApres").asDouble(), 0.01);
        assertEquals(-31.43, b.get("ecartContratAvantPourcent").asDouble(), 0.01);
        assertEquals(-8.57, b.get("ecartContratApresPourcent").asDouble(), 0.01);
    }

    @Test
    @DisplayName("Chaque créneau du périmètre a son détail, déplacé ou non")
    void chaqueCreneauDuPerimetre_aSonDetail() throws Exception {
        // Un arbitrage qui n'a rien changé est une information, pas un silence : le créneau du
        // tiers figure au détail avec deplace=false, pas par son absence.
        JsonNode details = solve(avecTolerance(10.0)).get("arbitrage").get("details");
        assertEquals(2, details.size());

        JsonNode deplace = detail(details, CRENEAU_ARBITRE);
        assertEquals("SAL-A", deplace.get("ressourceAvantId").asText());
        assertEquals("SAL-B", deplace.get("ressourceApresId").asText());
        assertTrue(deplace.get("deplace").asBoolean());
        assertTrue(!deplace.get("tenuParUnTiers").asBoolean());
        assertEquals("SALARIE", deplace.get("nature").asText());

        JsonNode tiers = detail(details, CRENEAU_DU_TIERS);
        assertEquals("SAL-TIERS", tiers.get("ressourceAvantId").asText());
        assertEquals("SAL-TIERS", tiers.get("ressourceApresId").asText());
        assertTrue(!tiers.get("deplace").asBoolean());
        assertTrue(tiers.get("tenuParUnTiers").asBoolean(),
                "Le créneau n'a pas pu bouger, et ce n'est pas un échec de l'arbitrage.");
    }

    @Test
    @DisplayName("Le détail est trié par date, comme le planning")
    void leDetail_estTrieParDate() throws Exception {
        JsonNode details = solve(avecTolerance(10.0)).get("arbitrage").get("details");

        assertEquals(CRENEAU_ARBITRE, details.get(0).get("creneauId").asText());
        assertEquals(CRENEAU_DU_TIERS, details.get(1).get("creneauId").asText());
    }

    @Test
    @DisplayName("Un créneau du périmètre absent du dataset ne gonfle pas les compteurs")
    void creneauIntrouvable_neGonflePasLesCompteurs() throws Exception {
        // creneauxArbitres compte le périmètre EFFECTIVEMENT soumis. Compter ce qui a été demandé
        // ferait lire « un créneau sur trois déplacé » là où il n'y en avait que deux à arbitrer.
        ObjectNode requete = (ObjectNode) avecTolerance(10.0);
        ((ArrayNode) requete.get("scenarioParameters").get("creneauxArbitres")).add("PLN-FANTOME");

        assertEquals(2, solve(requete).get("arbitrage").get("creneauxArbitres").asInt());
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static JsonNode mouvement(JsonNode parSalarie, String ressourceId) {
        for (JsonNode mouvement : parSalarie) {
            if (ressourceId.equals(mouvement.get("ressourceId").asText())) {
                return mouvement;
            }
        }
        throw new AssertionError("'" + ressourceId + "' est absent de arbitrage.parSalarie.");
    }

    private static JsonNode detail(JsonNode details, String creneauId) {
        for (JsonNode detail : details) {
            if (creneauId.equals(detail.get("creneauId").asText())) {
                return detail;
            }
        }
        throw new AssertionError("'" + creneauId + "' est absent de arbitrage.details.");
    }

    /** Identifiant de la ressource qui tient un créneau dans le planning restitué. */
    private static String titulaire(JsonNode reponse, String creneauId) {
        for (JsonNode jour : reponse.get("planning").get("jours")) {
            for (JsonNode creneau : jour.get("creneaux")) {
                if (creneauId.equals(creneau.get("id").asText())) {
                    return creneau.get("ressourceAffecteeId").asText();
                }
            }
        }
        throw new AssertionError("Le créneau '" + creneauId + "' est absent du planning restitué.");
    }

    /** Première alerte portant ce code, ou {@code null}. */
    private static JsonNode alerte(JsonNode reponse, String code) {
        for (JsonNode alerte : reponse.get("diagnostics").get("alerts")) {
            if (code.equals(alerte.get("code").asText())) {
                return alerte;
            }
        }
        return null;
    }

    private JsonNode lire() throws Exception {
        return objectMapper.readTree(Files.readString(ARBITRAGE));
    }

    private JsonNode avecTolerance(double ecartTolerePourcent) throws Exception {
        ObjectNode requete = (ObjectNode) lire();
        ObjectNode equite = objectMapper.createObjectNode();
        equite.put("ecartTolerePourcent", ecartTolerePourcent);
        ((ObjectNode) requete.get("planningContext")).set("equite", equite);
        return requete;
    }

    private JsonNode solve(JsonNode requete) throws Exception {
        String reponse = mockMvc.perform(post("/scenarios/sc05/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(reponse);
    }
}
