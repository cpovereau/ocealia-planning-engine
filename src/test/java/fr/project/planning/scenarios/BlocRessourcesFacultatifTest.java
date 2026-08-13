package fr.project.planning.scenarios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BlocRessourcesFacultatifTest — ne pas déclarer un bloc facultatif n'est pas une erreur.
 *
 * <h3>Le défaut que ces tests verrouillent</h3>
 * <p>{@code ScenarioResourceMapper.toRessources} itérait {@code salaries} et {@code postesVirtuels}
 * sans les tester : une demande qui n'en déclarait qu'un recevait <strong>500 INTERNAL_ERROR</strong>.
 * Or aucun des deux n'est exigé par le contrat, et n'en déclarer qu'un décrit quelque chose de
 * parfaitement légitime — un dataset sans poste virtuel dit simplement qu'aucun n'est mobilisable.</p>
 *
 * <p>Constaté au lot L4 du chantier équité, sur un jeu d'essai SC-06 qui ne déclarait pas de poste
 * virtuel parce qu'il n'en avait pas besoin. Le défaut touchait les quatre scénarios : le mapper
 * est commun.</p>
 *
 * <p>L'absence de <em>toute</em> ressource reste, elle, une situation que le moteur signale —
 * {@code AUCUNE_RESSOURCE_DANS_DATASET} — parce qu'elle rend tout créneau non couvert d'avance.
 * Signaler n'est pas refuser : la réponse est produite, et elle montre l'impasse.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class BlocRessourcesFacultatifTest {

    private static final Path REFERENCE =
            Path.of("src/test/resources/scenarios/sc03/sc03_migration_reference.json");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Un dataset sans postes virtuels est traité, pas rejeté")
    void sansPostesVirtuels_leDatasetEstTraite() throws Exception {
        solve(sansBloc("postesVirtuels")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un dataset sans salariés est traité : reste les postes virtuels")
    void sansSalaries_leDatasetEstTraite() throws Exception {
        solve(sansBloc("salaries")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sans aucune ressource, le moteur signale l'impasse au lieu de la refuser")
    void sansAucuneRessource_leMoteurSignaleLimpasse() throws Exception {
        ObjectNode requete = sansBloc("postesVirtuels");
        ((ObjectNode) requete.get("dataSet").get("ressources")).remove("salaries");

        solve(requete)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnostics.alerts[?(@.code=='AUCUNE_RESSOURCE_DANS_DATASET')]")
                        .isNotEmpty());
    }

    private ObjectNode sansBloc(String bloc) throws Exception {
        ObjectNode requete = (ObjectNode) objectMapper.readTree(Files.readString(REFERENCE));
        ((ObjectNode) requete.get("dataSet").get("ressources")).remove(bloc);
        return requete;
    }

    private org.springframework.test.web.servlet.ResultActions solve(JsonNode requete)
            throws Exception {
        return mockMvc.perform(post("/scenarios/sc03/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(requete)));
    }
}
