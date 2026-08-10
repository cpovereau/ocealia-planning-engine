package fr.project.planning.scenarios.sc06.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioControllerSc06RuntimeTest — lot S4
 *
 * <p>Test d'intégration de bout en bout sur le jeu de référence SC-06.</p>
 *
 * <p>Le jeu est construit pour que <strong>chaque palier du classement soit observable</strong> :</p>
 * <ul>
 *   <li><b>SAL-2001</b> travaille déjà le mercredi → conforme, pas de rappel ;</li>
 *   <li><b>SAL-2002</b> est libre ce jour-là → conforme, mais rappelé sur son repos ;</li>
 *   <li><b>SAL-2003</b> reprend le jeudi à 04:00 → 6 h de repos seulement, non conforme ;</li>
 *   <li><b>SAL-2004</b> ne pratique pas l'activité → écarté par le filtre d'éligibilité ;</li>
 *   <li><b>SAL-2005</b> est en congé ce jour-là → écarté également.</li>
 * </ul>
 *
 * <p>Le podium attendu est donc, dans l'ordre : SAL-2001, SAL-2002, SAL-2003.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioControllerSc06RuntimeTest {

    private static final Path REFERENCE = Path.of("src/test/resources/scenarios/sc06/sc06_reference.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sc06_classeLesCandidatsSelonLesPaliers() throws Exception {

        mockMvc.perform(post("/scenarios/sc06/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(Files.readString(REFERENCE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioType").value("SC-06"))
                .andExpect(jsonPath("$.candidats").isArray())
                .andExpect(jsonPath("$.candidats.length()").value(3))

                // Rang 1 — déjà en poste le mercredi : préféré au palier 4
                .andExpect(jsonPath("$.candidats[0].rang").value(1))
                .andExpect(jsonPath("$.candidats[0].conforme").value(true))
                .andExpect(jsonPath("$.candidats[0].couvertureComplete").value(true))
                .andExpect(jsonPath("$.candidats[0].nature").value("MONO_RESSOURCE"))
                .andExpect(jsonPath("$.candidats[0].affectations[0].ressourceId").value("SAL-2001"))
                .andExpect(jsonPath("$.candidats[0].affectations[0].creneauId").value("BES-001"))
                .andExpect(jsonPath("$.candidats[0].affectations[0].activite").value("ACT-SOIN"))
                .andExpect(jsonPath("$.candidats[0].affectations[0].lieu").value("HOPITAL-NORD"))

                // Rang 2 — conforme aussi, mais rappelé sur son repos
                .andExpect(jsonPath("$.candidats[1].rang").value(2))
                .andExpect(jsonPath("$.candidats[1].conforme").value(true))
                .andExpect(jsonPath("$.candidats[1].affectations[0].ressourceId").value("SAL-2002"))
                .andExpect(jsonPath("$.candidats[1].motifs[?(@.code=='RAPPEL_SUR_REPOS')]").exists())

                // Rang 3 — repos quotidien insuffisant : restitué, marqué, classé en dernier
                .andExpect(jsonPath("$.candidats[2].rang").value(3))
                .andExpect(jsonPath("$.candidats[2].conforme").value(false))
                .andExpect(jsonPath("$.candidats[2].affectations[0].ressourceId").value("SAL-2003"))
                .andExpect(jsonPath("$.candidats[2].motifs[?(@.code=='REPOS_QUOTIDIEN_INSUFFISANT')]").exists())
                .andExpect(jsonPath("$.candidats[2].motifs[?(@.severite=='ERROR')]").exists());
    }

    @Test
    void sc06_lesRessourcesNonEligiblesNapparaissentPas() throws Exception {
        // SAL-2004 ne pratique pas l'activité, SAL-2005 est indisponible : ni l'un ni l'autre
        // ne doit figurer au podium, à aucun rang.
        String reponse = mockMvc.perform(post("/scenarios/sc06/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(Files.readString(REFERENCE))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(reponse.contains("SAL-2004"),
                "Un salarié dont l'activité n'est pas compatible ne doit pas être proposé.");
        org.junit.jupiter.api.Assertions.assertFalse(reponse.contains("SAL-2005"),
                "Un salarié indisponible ce jour-là ne doit pas être proposé.");
    }

    @Test
    void sc06_restitueLePlanningEtLesMetriquesDuRangUn() throws Exception {
        // §10.4 : planning ne porte que les créneaux du besoin, workMetrics que les ressources
        // mobilisées par la solution de rang 1. Le planning existant n'est pas réémis.
        mockMvc.perform(post("/scenarios/sc06/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(Files.readString(REFERENCE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solverResult.status").value("SOLVED"))
                .andExpect(jsonPath("$.planning.jours.length()").value(1))
                .andExpect(jsonPath("$.planning.jours[0].date").value("2026-05-13"))
                .andExpect(jsonPath("$.planning.jours[0].creneaux.length()").value(1))
                .andExpect(jsonPath("$.planning.jours[0].creneaux[0].id").value("BES-001"))
                .andExpect(jsonPath("$.planning.jours[0].creneaux[0].ressourceAffecteeId").value("SAL-2001"))
                .andExpect(jsonPath("$.workMetrics.byRessource.length()").value(1))
                .andExpect(jsonPath("$.workMetrics.byRessource[0].resourceId").value("SAL-2001"));
    }

    @Test
    void sc06_estDeterministe() throws Exception {
        // L'énumération ne lance aucune recherche : deux appels identiques doivent produire
        // exactement la même réponse. C'est la propriété qu'un solve() ne garantit pas.
        String json = Objects.requireNonNull(Files.readString(REFERENCE));

        MvcResult premier = mockMvc.perform(post("/scenarios/sc06/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(json)).andExpect(status().isOk()).andReturn();

        MvcResult second = mockMvc.perform(post("/scenarios/sc06/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(json)).andExpect(status().isOk()).andReturn();

        assertEquals(premier.getResponse().getContentAsString(),
                second.getResponse().getContentAsString(),
                "Même entrée, même podium — deux fois sur deux.");
    }

    @Test
    void sc01_etSc03_neGagnentPasLeBlocCandidats() throws Exception {
        // Non-régression : le bloc candidats[] est propre à SC-06. La clé doit être absente
        // ailleurs, et non présente à vide — un tableau vide laisserait croire à un classement.
        String sc03 = Files.readString(
                Path.of("src/test/resources/scenarios/sc03/sc03_migration_reference.json"));

        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(sc03)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidats").doesNotExist());
    }
}
