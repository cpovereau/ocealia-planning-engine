package fr.project.planning.scenarios.sc06.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioSc06PassesTest — lot S4
 *
 * <p>Couvre les deux passes que le jeu de référence ne déclenche pas : la composition et le
 * repli. Ce sont les plus fragiles de l'énumération, et les moins visibles.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioSc06PassesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void besoinQuUnePersonneNePeutPasPrendreSeule_produitUneSolutionComposee() throws Exception {
        // Deux créneaux simultanés sur deux sites : personne ne peut couvrir les deux.
        // La passe 1 ne rend donc rien, et la composition prend le relais.
        String reponse = postSc06("src/test/resources/scenarios/sc06/sc06_composee.json");
        JsonNode candidats = objectMapper.readTree(reponse).get("candidats");

        assertEquals(3, candidats.size(), "Deux compositions possibles, plus le repli.");

        JsonNode premier = candidats.get(0);
        assertEquals("COMPOSEE", premier.get("nature").asText());
        assertTrue(premier.get("conforme").asBoolean());
        assertTrue(premier.get("couvertureComplete").asBoolean());
        assertEquals(2, premier.get("affectations").size());

        Set<String> ressources = new HashSet<>();
        premier.get("affectations").forEach(a -> ressources.add(a.get("ressourceId").asText()));
        assertEquals(Set.of("SAL-A", "SAL-B"), ressources,
                "Chaque créneau doit revenir à une personne différente.");

        // Le repli complète toujours le podium quand les solutions réelles ne suffisent pas.
        assertEquals("RESSOURCE_A_POURVOIR", candidats.get(2).get("nature").asText());
    }

    @Test
    void aucunSalarieCompetent_produitLeRepliSurPosteVirtuel() throws Exception {
        // Le seul salarié du dataset ne pratique pas l'activité demandée.
        // Restituer une liste vide n'apprendrait rien : le moteur propose ce qu'il reste.
        String reponse = postSc06("src/test/resources/scenarios/sc06/sc06_repli.json");
        JsonNode candidats = objectMapper.readTree(reponse).get("candidats");

        assertEquals(1, candidats.size());
        assertEquals("RESSOURCE_A_POURVOIR", candidats.get(0).get("nature").asText());
        assertEquals("PV-001", candidats.get(0).get("affectations").get(0).get("ressourceId").asText());
        assertTrue(candidats.get(0).get("conforme").asBoolean(),
                "Un poste virtuel ne viole aucune règle : c'est sa nature qui le classe, pas sa conformité.");
    }

    @Test
    void posteVirtuelDeclarantUneAutreActivite_sertQuandMemeDeRepli() throws Exception {
        // [Lot S8.3] Arbitrage rendu : un poste virtuel n'est pas soumis à la règle d'activité,
        // puisqu'il existe justement pour combler le besoin. Le filtrer revenait à retirer le
        // remplaçant au motif qu'il ne fait pas déjà le travail — et SC-06 restituait alors
        // « rien à pourvoir » alors qu'un poste à pourvoir figurait au dataset.
        String reponse = postSc06(
                "src/test/resources/scenarios/sc06/sc06_repli_poste_virtuel_autre_activite.json");
        JsonNode candidats = objectMapper.readTree(reponse).get("candidats");

        assertEquals(1, candidats.size());
        assertEquals("RESSOURCE_A_POURVOIR", candidats.get(0).get("nature").asText());
        assertEquals("PV-001", candidats.get(0).get("affectations").get(0).get("ressourceId").asText(),
                "Le poste virtuel ne déclare que ACT-ADMIN et le besoin est ACT-SOIN : "
                        + "il reste néanmoins le poste à pourvoir.");
    }

    @Test
    void repliSurPosteVirtuel_neCompteAucuneRessourceMobilisee() throws Exception {
        // §10.4 : workMetrics ne porte que les ressources réelles mobilisées par le rang 1.
        // Un poste virtuel n'en est pas une.
        mockMvc.perform(post("/scenarios/sc06/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(lire("src/test/resources/scenarios/sc06/sc06_repli.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workMetrics.byRessource.length()").value(0));
    }

    @Test
    void plageDeNuitInexploitable_estSignaleeAlorsQueSc06NAlertaitJamais() throws Exception {
        // [Lot S8.4] SC-06 émettait `alerts: []` en dur, au motif qu'il n'a pas de builder. Il
        // refuse plutôt qu'il n'ignore — ses garde-fous lèvent des exceptions — mais le cadre
        // réglementaire restait un endroit où il décidait seul : substituer la plage par défaut à
        // celle qu'on lui déclare déplace le score.
        String reponse = postSc06("src/test/resources/scenarios/sc06/sc06_reglementaire_degrade.json");
        JsonNode alerts = objectMapper.readTree(reponse).get("diagnostics").get("alerts");

        assertEquals(1, alerts.size());
        assertEquals("PLAGE_NUIT_PAR_DEFAUT", alerts.get(0).get("code").asText());
        assertEquals("WARNING", alerts.get(0).get("severity").asText());
        assertTrue(alerts.get(0).get("message").asText().contains("22:00"),
                "Le message doit nommer la plage effectivement appliquée.");
    }

    @Test
    void jeuDeReferenceDuRepli_resteSansAlerte() throws Exception {
        String reponse = postSc06("src/test/resources/scenarios/sc06/sc06_repli.json");
        JsonNode diagnostics = objectMapper.readTree(reponse).get("diagnostics");

        assertEquals(0, diagnostics.get("alerts").size(),
                "Une réponse propre doit le rester : ce canal ne sert qu'à dire ce qui cloche.");
        assertEquals(0, diagnostics.get("ignoredCreneaux").get("details").size(),
                "SC-06 refuse au lieu d'ignorer : il n'écarte jamais de créneau en silence.");
    }

    // =========================================================
    // Helpers
    // =========================================================

    private String postSc06(String chemin) throws Exception {
        return mockMvc.perform(post("/scenarios/sc06/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(lire(chemin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String lire(String chemin) throws Exception {
        return Objects.requireNonNull(Files.readString(Path.of(chemin)));
    }
}
