package fr.project.planning.scenarios.sc04.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.project.planning.docs.SchemaPublie;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioSc04OptimisationTest — lots O2 à O4 de SC-04 (bout-en-bout).
 *
 * <h3>Le cas</h3>
 * <pre>
 * Horizon      lundi 11 → dimanche 24 mai 2026, deux semaines
 * Date pivot   lundi 18 mai — la seconde semaine, et elle seule, est ajustable
 *
 * Semaine 1 (figée)     SAL-A : lundi, mardi          SAL-B : mercredi, jeudi
 * Semaine 2 (rouverte)  SAL-A : les cinq jours        SAL-B : rien
 * </pre>
 *
 * <h3>Ce que ce cas prouve, et qu'un cas plus simple ne prouverait pas</h3>
 * <p>Le déséquilibre est <strong>entièrement dans la partie ajustable</strong>, et la partie figée
 * est, elle, parfaitement équilibrée. Un moteur qui ignorerait le pivot rééquilibrerait les deux
 * semaines et le test passerait quand même ; un moteur qui figerait tout ne bougerait rien. Seul
 * un moteur qui applique exactement le pivot laisse la semaine 1 intacte et redistribue la
 * semaine 2 — ce que les deux premiers tests vérifient séparément.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioSc04OptimisationTest {

    private static final Path REFERENCE = Path.of("src/test/resources/scenarios/sc04/sc04_reference.json");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Le passé est intact : aucun créneau antérieur au pivot n'a changé de main")
    void lePasseEstIntact() throws Exception {
        JsonNode reponse = solve(lire());

        for (JsonNode jour : reponse.get("planning").get("jours")) {
            for (JsonNode creneau : jour.get("creneaux")) {
                String id = creneau.get("id").asText();
                if (!id.startsWith("PASSE-")) {
                    continue;
                }
                String attendu = id.startsWith("PASSE-MATIN") ? "SAL-A" : "SAL-B";
                assertEquals(attendu, creneau.get("ressourceAffecteeId").asText(),
                        "Le passé ne se réécrit pas : " + id + " devait rester à " + attendu + ".");
            }
        }
    }

    @Test
    @DisplayName("La suite est rééquilibrée : SAL-B reprend une part de la seconde semaine")
    void laSuiteEstReequilibree() throws Exception {
        JsonNode optimisation = solve(lire()).get("optimisation");

        assertNotNull(optimisation, "SC-04 doit porter son bloc optimisation.");
        assertEquals(10, optimisation.get("creneauxFiges").asInt());
        assertEquals(10, optimisation.get("creneauxAjustables").asInt());
        assertTrue(optimisation.get("creneauxDeplaces").asInt() > 0,
                "Cinq créneaux sur une personne et rien sur l'autre : il y avait à faire.");

        int reprisParB = 0;
        for (JsonNode salarie : optimisation.get("parSalarie")) {
            if ("SAL-B".equals(salarie.get("ressourceId").asText())) {
                reprisParB = salarie.get("repris").asInt();
            }
        }
        assertTrue(reprisParB > 0,
                "SAL-B ne travaillait pas du tout la seconde semaine : c'est lui que l'équité "
                        + "désigne, et le rééquilibrage doit lui revenir.");
    }

    @Test
    @DisplayName("Les séries disent QUAND, là où la période ne dit que COMBIEN")
    void lesSeriesDisentQuand() throws Exception {
        JsonNode optimisation = solve(lire()).get("optimisation");

        JsonNode salarieB = null;
        for (JsonNode salarie : optimisation.get("parSalarie")) {
            if ("SAL-B".equals(salarie.get("ressourceId").asText())) {
                salarieB = salarie;
            }
        }
        assertNotNull(salarieB);

        boolean semaineTrouvee = false;
        boolean periodeTrouvee = false;
        for (JsonNode tranche : salarieB.get("tranches")) {
            String granularite = tranche.get("granularite").asText();
            if ("SEMAINE".equals(granularite) && "2026-05-18".equals(tranche.get("debut").asText())) {
                semaineTrouvee = true;
                assertEquals(0.0, tranche.get("heuresAvant").asDouble(), 0.01,
                        "SAL-B ne travaillait pas du tout cette semaine-là.");
                assertTrue(tranche.get("heuresApres").asDouble() > 0,
                        "C'est exactement la semaine que l'optimisation devait corriger — et c'est "
                                + "ce qu'un total sur la période aurait noyé.");
            }
            if ("PERIODE".equals(granularite)) {
                periodeTrouvee = true;
            }
        }
        assertTrue(semaineTrouvee, "La semaine du pivot doit figurer dans les séries.");
        assertTrue(periodeTrouvee, "La période fait foi et doit être restituée.");
    }

    @Test
    @DisplayName("Les trois granularités sont restituées, dans l'ordre du découpage")
    void lesTroisGranularitesSontRestituees() throws Exception {
        JsonNode optimisation = solve(lire()).get("optimisation");
        JsonNode tranches = optimisation.get("parSalarie").get(0).get("tranches");

        // Deux semaines pleines, un mois tronqué par l'horizon, une période.
        assertEquals(4, tranches.size());
        assertEquals("SEMAINE", tranches.get(0).get("granularite").asText());
        assertEquals("SEMAINE", tranches.get(1).get("granularite").asText());
        assertEquals("MOIS", tranches.get(2).get("granularite").asText());
        assertEquals("PERIODE", tranches.get(3).get("granularite").asText());
        assertTrue(tranches.get(2).get("partielle").asBoolean(),
                "Mai n'est couvert que du 11 au 24 : ses volumes bruts ne se comparent pas à ceux "
                        + "d'un mois plein, et l'appelant doit pouvoir le savoir.");
    }

    @Test
    @DisplayName("Un pivot postérieur à tout le planning ne change rien, et le dit")
    void unPivotPosterieurNeChangeRien() throws Exception {
        JsonNode reponse = solve(avecPivot("2026-06-01"));
        JsonNode optimisation = reponse.get("optimisation");

        assertEquals(0, optimisation.get("creneauxAjustables").asInt());
        assertEquals(0, optimisation.get("creneauxDeplaces").asInt());
        assertTrue(aLAlerte(reponse, "AUCUN_CRENEAU_AJUSTABLE"),
                "Le moteur ne refuse pas : il rend visible que la demande n'a pas d'objet.");
        assertTrue(aLeMotif(optimisation, "OPTIMISATION_SANS_EFFET"),
                "Sans ce motif, l'appelant recevrait son propre planning et chercherait l'échec.");
    }

    @Test
    @DisplayName("Un pivot antérieur à tout le planning rouvre tout, et le signale")
    void unPivotAnterieurRouvreTout() throws Exception {
        JsonNode reponse = solve(avecPivot("2026-01-01"));

        assertEquals(0, reponse.get("optimisation").get("creneauxFiges").asInt());
        assertTrue(aLAlerte(reponse, "PLANNING_ENTIEREMENT_REOUVERT"),
                "SC-04 promet d'améliorer sans reconstruire : rouvrir tout est légitime, mais "
                        + "c'est rarement ce que l'appelant a voulu demander.");
    }

    @Test
    @DisplayName("Sans date pivot, la demande est refusée : le moteur ne devine pas ce qui peut bouger")
    void sansDatePivotLaDemandeEstRefusee() throws Exception {
        ObjectNode requete = (ObjectNode) lire();
        ((ObjectNode) requete.get("scenarioParameters")).remove("datePivot");

        mockMvc.perform(post("/scenarios/sc04/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Le bloc optimisation produit correspond exactement au schéma publié")
    void optimisationProduite_correspondAuSchemaPublie() throws Exception {
        // Rang 13 : un schéma publié qui dérive de ce que le moteur produit est un mensonge
        // contractuel que rien ne rattrape côté WinDev. additionalProperties:false rend la
        // confrontation stricte dans les deux sens — champ en trop comme champ manquant.
        SchemaPublie schema = SchemaPublie.charger();

        List<String> ecarts = new ArrayList<>();
        ecarts.addAll(schema.confronter(solve(lire()).get("optimisation"),
                "Optimisation", "optimisation"));
        ecarts.addAll(schema.confronter(solve(avecPivot("2026-06-01")).get("optimisation"),
                "Optimisation", "optimisation (pivot postérieur)"));
        ecarts.addAll(schema.confronter(solve(avecPivot("2026-01-01")).get("optimisation"),
                "Optimisation", "optimisation (pivot antérieur)"));

        assertTrue(ecarts.isEmpty(),
                "Le bloc produit et le schéma publié ont divergé :\n  - "
                        + String.join("\n  - ", ecarts));
    }

    // =====================================================================
    // Montage
    // =====================================================================

    private JsonNode lire() throws Exception {
        return objectMapper.readTree(Files.readString(REFERENCE, StandardCharsets.UTF_8));
    }

    private JsonNode avecPivot(String pivot) throws Exception {
        ObjectNode requete = (ObjectNode) lire();
        ((ObjectNode) requete.get("scenarioParameters")).put("datePivot", pivot);
        return requete;
    }

    private JsonNode solve(JsonNode requete) throws Exception {
        String corps = mockMvc.perform(post("/scenarios/sc04/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(corps);
    }

    private static boolean aLAlerte(JsonNode reponse, String code) {
        for (JsonNode alerte : reponse.get("diagnostics").get("alerts")) {
            if (code.equals(alerte.get("code").asText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean aLeMotif(JsonNode optimisation, String code) {
        for (JsonNode motif : optimisation.get("motifs")) {
            if (code.equals(motif.get("code").asText())) {
                return true;
            }
        }
        return false;
    }
}
