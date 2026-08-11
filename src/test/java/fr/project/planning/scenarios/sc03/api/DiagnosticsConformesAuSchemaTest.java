package fr.project.planning.scenarios.sc03.api;

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DiagnosticsConformesAuSchemaTest — lot S8.4
 *
 * <p>{@code 50_ScenarioResponse.schema.json} est publié aux intégrateurs, et rien ne le
 * confrontait à ce que le moteur produit réellement. Il avait dérivé sans bruit : le bloc
 * {@code IgnoredCreneaux} déclarait une propriété {@code sansRessource} — le seul alias
 * d'<em>entrée</em>, jamais sérialisé — et exigeait un {@code saucuneRessourceDansDataset} qui
 * n'existe nulle part. Avec {@code additionalProperties: false}, un client qui validait sa réponse
 * contre ce schéma la voyait donc rejetée.</p>
 *
 * <p>Ce test compare les noms, dans les deux sens, sur le sous-arbre {@code diagnostics}. Il ne
 * remplace pas un validateur JSON Schema — le projet n'en embarque pas — mais il attrape la classe
 * d'erreur qui s'est produite : un champ ajouté au code et pas au schéma, ou l'inverse.</p>
 *
 * <p><strong>Limite connue</strong> : le schéma vit sous {@code docs/} et n'est donc pas déclaré
 * comme entrée de la tâche de test. Une modification portant sur le seul schéma peut laisser
 * Gradle considérer le test à jour ; {@code gradle cleanTest test} le force. Toute modification de
 * code le rejoue normalement.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class DiagnosticsConformesAuSchemaTest {

    private static final Path SCHEMA = Path.of("docs/50_ScenarioResponse.schema.json");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Les diagnostics produits n'ont ni champ non déclaré ni champ requis manquant")
    void diagnosticsProduits_correspondentAuSchemaPublie() throws Exception {
        JsonNode schema = objectMapper.readTree(Files.readString(SCHEMA));
        JsonNode defs = schema.get("$defs");

        // Un jeu qui déclenche les deux blocs : des créneaux écartés, et des alertes.
        JsonNode diagnostics = solve("sc03_reglementaire_degrade").get("diagnostics");
        JsonNode ecartes = solve("sc03_hors_horizon").get("diagnostics");

        List<String> ecarts = new ArrayList<>();
        confronter(diagnostics, defs.get("Diagnostics"), defs, "diagnostics", ecarts);
        confronter(ecartes, defs.get("Diagnostics"), defs, "diagnostics(écartés)", ecarts);

        assertTrue(ecarts.isEmpty(),
                "Le schéma publié et la réponse produite divergent :\n  - "
                        + String.join("\n  - ", ecarts));
    }

    /**
     * Compare les noms de propriétés d'un nœud produit à ceux que le schéma déclare, puis
     * redescend dans les objets et les tableaux d'objets.
     */
    private void confronter(JsonNode produit, JsonNode definition, JsonNode defs,
                            String chemin, List<String> ecarts) {

        if (produit == null || definition == null) {
            return;
        }
        JsonNode proprietes = definition.get("properties");
        if (proprietes == null) {
            return;
        }

        Set<String> declarees = new LinkedHashSet<>();
        proprietes.fieldNames().forEachRemaining(declarees::add);

        Set<String> produites = new LinkedHashSet<>();
        produit.fieldNames().forEachRemaining(produites::add);

        for (String champ : produites) {
            if (!declarees.contains(champ)) {
                ecarts.add(chemin + "." + champ + " est restitué mais absent du schéma");
            }
        }

        JsonNode requis = definition.get("required");
        if (requis != null) {
            for (JsonNode champ : requis) {
                if (!produites.contains(champ.asText())) {
                    ecarts.add(chemin + "." + champ.asText()
                            + " est exigé par le schéma mais absent de la réponse");
                }
            }
        }

        for (String champ : produites) {
            JsonNode sousDefinition = resoudre(proprietes.get(champ), defs);
            if (sousDefinition == null) {
                continue;
            }
            JsonNode valeur = produit.get(champ);

            if (valeur.isObject()) {
                confronter(valeur, sousDefinition, defs, chemin + "." + champ, ecarts);
            } else if (valeur.isArray() && sousDefinition.get("items") != null) {
                JsonNode elementDefinition = resoudre(sousDefinition.get("items"), defs);
                int index = 0;
                for (Iterator<JsonNode> it = valeur.elements(); it.hasNext(); index++) {
                    JsonNode element = it.next();
                    if (element.isObject()) {
                        confronter(element, elementDefinition, defs,
                                chemin + "." + champ + "[" + index + "]", ecarts);
                    }
                }
            }
        }
    }

    /** Suit un {@code $ref} local, ou rend la définition telle quelle. */
    private JsonNode resoudre(JsonNode noeud, JsonNode defs) {
        if (noeud == null) {
            return null;
        }
        JsonNode ref = noeud.get("$ref");
        if (ref == null) {
            return noeud;
        }
        String nom = ref.asText().substring(ref.asText().lastIndexOf('/') + 1);
        return defs.get(nom);
    }

    private JsonNode solve(String fixture) throws Exception {
        String json = Files.readString(
                Path.of("src/test/resources/scenarios/sc03/" + fixture + ".json"));
        String reponse = mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(json)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(reponse);
    }
}
