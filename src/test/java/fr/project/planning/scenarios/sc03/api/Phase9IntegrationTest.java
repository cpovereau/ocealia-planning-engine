package fr.project.planning.scenarios.sc03.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase9IntegrationTest — Tests d'intégration pipeline SC-03.
 *
 * Vérifie la chaîne complète Windev → API → Solver → ResponseDTO pour des cas
 * spécifiques introduits en Phase 9 :
 *
 * 1. Créneau hors horizon → diagnostics.ignoredCreneaux.horsHorizon = 1
 * 2. Activité inconnue    → diagnostics.ignoredCreneaux.activiteInconnue = 1
 * 3. Dataset de référence → ressourceAffecteeId jamais null ("A_AFFECTER" si non couvert)
 * 4. Dataset de référence → tous les blocs de la réponse présents et non null
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class Phase9IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ------------------------------------------------------------------
    // 1. Créneau hors horizon → horsHorizon = 1
    // ------------------------------------------------------------------

    @Test
    void sc03_creneauHorsHorizon_doitComptabiliserHorsHorizon() throws Exception {
        String json = Files.readString(
                Path.of("src/test/resources/scenarios/sc03/sc03_hors_horizon.json")
        );

        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(json)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solverResult.status").value("SOLVED"))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.horsHorizon").value(1))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.activiteInconnue").value(0))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.aucuneRessourceDansDataset").value(0));
    }

    // ------------------------------------------------------------------
    // 2. Activité inconnue → activiteInconnue = 1
    // ------------------------------------------------------------------

    @Test
    void sc03_activiteInconnue_doitComptabiliserActiviteInconnue() throws Exception {
        String json = Files.readString(
                Path.of("src/test/resources/scenarios/sc03/sc03_activite_inconnue.json")
        );

        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(json)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solverResult.status").value("SOLVED"))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.activiteInconnue").value(1))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.horsHorizon").value(0))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.aucuneRessourceDansDataset").value(0));
    }

    // ------------------------------------------------------------------
    // 3. Dataset de référence — ressourceAffecteeId jamais null
    // ------------------------------------------------------------------

    @Test
    void sc03_referenceDataset_ressourceAffecteeId_jamaisNull() throws Exception {
        String json = Files.readString(
                Path.of("src/test/resources/scenarios/sc03/sc03_migration_reference.json")
        );

        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(json)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solverResult.status").value("SOLVED"))
                // Chaque créneau dans le planning doit avoir ressourceAffecteeId (A_AFFECTER ou id réel)
                .andExpect(jsonPath("$.planning.jours[*].creneaux[*].ressourceAffecteeId").isNotEmpty());
    }

    // ------------------------------------------------------------------
    // 4. Dataset de référence — présence et cohérence des 5 blocs
    // ------------------------------------------------------------------

    @Test
    void sc03_referenceDataset_tousLesBlocs_presents() throws Exception {
        String json = Files.readString(
                Path.of("src/test/resources/scenarios/sc03/sc03_migration_reference.json")
        );

        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(json)))
                .andDo(print())
                .andExpect(status().isOk())
                // solverResult
                .andExpect(jsonPath("$.solverResult").exists())
                .andExpect(jsonPath("$.solverResult.score").exists())
                .andExpect(jsonPath("$.solverResult.score.hard").value(0))
                .andExpect(jsonPath("$.solverResult.scoreBreakdown").isArray())
                // planning
                .andExpect(jsonPath("$.planning").exists())
                .andExpect(jsonPath("$.planning.jours").isArray())
                // workMetrics
                .andExpect(jsonPath("$.workMetrics").exists())
                .andExpect(jsonPath("$.workMetrics.byRessource").isArray())
                .andExpect(jsonPath("$.workMetrics.global").exists())
                .andExpect(jsonPath("$.workMetrics.global.nbCreneaux").exists())
                // solutionSummary
                .andExpect(jsonPath("$.solutionSummary").exists())
                .andExpect(jsonPath("$.solutionSummary.nbCreneaux").value(6))
                // diagnostics
                .andExpect(jsonPath("$.diagnostics").exists())
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux").exists())
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.horsHorizon").value(0))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.activiteInconnue").value(0))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.aucuneRessourceDansDataset").value(0))
                .andExpect(jsonPath("$.diagnostics.assignmentDiagnostics").isArray());
    }

    // ------------------------------------------------------------------
    // 5. Aucune ressource compatible → aucuneRessourceDansDataset = 1
    // ------------------------------------------------------------------

    @Test
    void sc03_aucuneRessourceCompatible_doitComptabiliserAucuneRessource() throws Exception {
        String json = Files.readString(
                Path.of("src/test/resources/scenarios/sc03/sc03_aucune_ressource.json")
        );

        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(json)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solverResult.status").value("SOLVED"))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.aucuneRessourceDansDataset").value(1))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.horsHorizon").value(0))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.activiteInconnue").value(0));
    }
}
