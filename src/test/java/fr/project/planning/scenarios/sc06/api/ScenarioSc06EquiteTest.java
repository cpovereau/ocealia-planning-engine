package fr.project.planning.scenarios.sc06.api;

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
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioSc06EquiteTest — lot L4 du chantier équité (V5, bout-en-bout).
 *
 * <h3>Le cas</h3>
 * <p>Besoin du jeudi 14 mai, 13:00 → 17:00, soit 4 h. Trois salariés à 35 h, tous
 * <strong>déjà en poste ce jeudi matin</strong> — les paliers 1 à 4 sont donc ex æquo, et le
 * classement se joue entièrement sur les critères d'équité.</p>
 *
 * <pre>
 *                 planning existant                     série au jeudi   semaine + besoin   écart
 * SAL-DISPO       mer 8 h · jeu 4 h                            2                16 h       −54,29 %
 * SAL-CHARGE      lun 8 h · mer 8 h · jeu 4 h                  2                24 h       −31,43 %
 * SAL-ENCHAINE    lun 2 h · mar 2 h · mer 2 h · jeu 2 h        4                12 h       −65,71 %
 * </pre>
 *
 * <h3>Ce que le classement doit dire</h3>
 * <p><strong>SAL-ENCHAINE est le moins servi de tous</strong> — et il arrive dernier. C'est
 * exactement l'arbitrage : l'aptitude prime sur le partage, et on ne fait pas revenir quelqu'un
 * qui enchaîne parce qu'il se trouve être en dessous de son contrat. Entre les deux qui restent,
 * à série égale, c'est l'écart au contrat qui tranche — et il désigne le moins sollicité.</p>
 *
 * <p>Aucun de ces deux départages n'existait avant le lot L4 : le classement passait du palier
 * « déjà en poste » au score SOFT, qui ne dit rien de l'équité et beaucoup d'autre chose.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioSc06EquiteTest {

    private static final Path EQUITE = Path.of("src/test/resources/scenarios/sc06/sc06_equite.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Le podium suit les critères d'équité, l'aptitude avant le partage")
    void lePodiumSuitLesCriteresDEquite() throws Exception {
        postSc06()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidats.length()").value(3))

                // Palier 6 départage les deux séries courtes : le moins servi passe devant.
                .andExpect(jsonPath("$.candidats[0].impacts[0].ressourceId").value("SAL-DISPO"))
                .andExpect(jsonPath("$.candidats[1].impacts[0].ressourceId").value("SAL-CHARGE"))

                // Palier 5 relègue celui qui enchaîne, malgré l'écart le plus favorable des trois.
                .andExpect(jsonPath("$.candidats[2].impacts[0].ressourceId").value("SAL-ENCHAINE"));
    }

    @Test
    @DisplayName("L'écart au contrat qui a départagé est restitué, signé")
    void lEcartAuContratEstRestitue() throws Exception {
        postSc06()
                .andExpect(status().isOk())

                // 16 h sur 35 h attendues : la personne est très en dessous de son contrat, et
                // c'est ce qui la rend préférable — pas seulement « non exclue ».
                .andExpect(jsonPath("$.candidats[0].impacts[0].ecartContratPourcent").value(-54.29))
                .andExpect(jsonPath("$.candidats[1].impacts[0].ecartContratPourcent").value(-31.43))
                .andExpect(jsonPath("$.candidats[2].impacts[0].ecartContratPourcent").value(-65.71));
    }

    @Test
    @DisplayName("La série de jours qui a relégué le troisième est restituée, avant et après")
    void laSerieDeJoursEstRestituee() throws Exception {
        postSc06()
                .andExpect(status().isOk())

                // Le besoin ne rallonge aucune série : ces trois-là travaillaient déjà ce jeudi.
                .andExpect(jsonPath("$.candidats[0].impacts[0].joursConsecutifs.avant").value(2.0))
                .andExpect(jsonPath("$.candidats[0].impacts[0].joursConsecutifs.apres").value(2.0))
                .andExpect(jsonPath("$.candidats[0].impacts[0].joursConsecutifs.plafond").value(6.0))
                .andExpect(jsonPath("$.candidats[0].impacts[0].joursConsecutifs.depassement").value(false))

                // Quatre jours d'affilée : la raison du rang 3, lisible dans la réponse.
                .andExpect(jsonPath("$.candidats[2].impacts[0].joursConsecutifs.apres").value(4.0));
    }

    @Test
    @DisplayName("L'amplitude après affectation reste restituée — c'est elle qui départage en dernier")
    void lAmplitudeApresResteRestituee() throws Exception {
        postSc06()
                .andExpect(status().isOk())

                // Jeudi : 08:00-12:00 seul, puis 08:00 → 17:00 avec le besoin.
                .andExpect(jsonPath("$.candidats[0].impacts[0].amplitudeJournaliere.avant").value(4.0))
                .andExpect(jsonPath("$.candidats[0].impacts[0].amplitudeJournaliere.apres").value(9.0));
    }

    @Test
    @DisplayName("Le candidat produit n'a ni champ non déclaré ni champ requis manquant")
    void leCandidatProduit_correspondAuSchemaPublie() throws Exception {
        // Deux champs viennent d'être ajoutés à impacts[]. Avec additionalProperties: false, les
        // oublier au schéma ferait rejeter la réponse chez un client qui la valide — c'est
        // exactement le défaut trouvé au lot L2 sur workMetrics, autre bloc.
        JsonNode reponse = new ObjectMapper().readTree(postSc06()
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        List<String> ecarts = SchemaPublie.charger()
                .confronter(reponse.get("candidats").get(0), "Candidat", "candidats[0]");

        assertTrue(ecarts.isEmpty(),
                "Le schéma publié et la réponse produite divergent :\n  - "
                        + String.join("\n  - ", ecarts));
    }

    private ResultActions postSc06() throws Exception {
        return mockMvc.perform(post("/scenarios/sc06/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Files.readString(EQUITE)));
    }
}
