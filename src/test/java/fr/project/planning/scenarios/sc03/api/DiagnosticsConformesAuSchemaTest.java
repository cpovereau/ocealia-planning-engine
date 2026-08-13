package fr.project.planning.scenarios.sc03.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.docs.SchemaPublie;
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
import java.util.List;
import java.util.Objects;

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
 * <p>La confrontation elle-même vit dans {@link SchemaPublie}, partagée avec le bloc
 * {@code remplacement} de SC-02 depuis le lot S4.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class DiagnosticsConformesAuSchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Les diagnostics produits n'ont ni champ non déclaré ni champ requis manquant")
    void diagnosticsProduits_correspondentAuSchemaPublie() throws Exception {
        SchemaPublie schema = SchemaPublie.charger();

        // Un jeu qui déclenche les deux blocs : des créneaux écartés, et des alertes.
        List<String> ecarts = new ArrayList<>();
        ecarts.addAll(schema.confronter(
                solve("sc03_reglementaire_degrade").get("diagnostics"),
                "Diagnostics", "diagnostics"));
        ecarts.addAll(schema.confronter(
                solve("sc03_hors_horizon").get("diagnostics"),
                "Diagnostics", "diagnostics(écartés)"));

        assertTrue(ecarts.isEmpty(),
                "Le schéma publié et la réponse produite divergent :\n  - "
                        + String.join("\n  - ", ecarts));
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
