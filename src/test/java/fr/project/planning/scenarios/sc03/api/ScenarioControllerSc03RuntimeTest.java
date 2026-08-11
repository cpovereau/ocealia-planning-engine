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
 * Test d'intégration SC-03 avec le dataset de référence.
 *
 * Vérifie :
 * - réponse HTTP 200
 * - status SOLVED
 * - score hard = 0 (aucune contrainte hard violée)
 * - présence des blocs planning, workMetrics, solutionSummary
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioControllerSc03RuntimeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sc03_runtime_should_solve_reference_dataset() throws Exception {

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
                .andExpect(jsonPath("$.solverResult.score.hard").value(0))
                // Garde-fou de score, posé au lot S7.2. Le chantier de remise en service du
                // socle réglementaire rebranche une contrainte par lot ; sans valeur de
                // référence, un écart passerait inaperçu ou resterait inattribuable.
                // Une modification volontaire de cette valeur doit être consignée dans
                // 92_cadrage_socle_reglementaire.md, avec le lot qui la produit.
                //
                // -1440  pénibilités légales, poids 1 en ANALYSE_RH :
                //          480 min de nuit    (CRE-VN-01, vendredi 22:00-06:00)
                //        + 480 min de dimanche (CRE-DI-01, 17 mai)
                //        + 480 min de férié    (CRE-ME-01, 13 mai déclaré isJourFerie) — lot S7.9
                // -66000 sous-emploi : deux salariés à 35 h hebdo pour 48 h de travail
                //        disponible — un déficit de 11 h chacun, inévitable et voulu visible.
                //        Lot S7.7.
                .andExpect(jsonPath("$.solverResult.score.soft").value(-67440))
                // Le sous-emploi ne doit jamais pousser à ne pas employer : les six créneaux
                // restent chez les salariés réels, le poste virtuel n'en reçoit aucun.
                .andExpect(jsonPath("$.workMetrics.byRessource[?(@.resourceId=='PV-001')].heuresTravaillees")
                        .value(org.hamcrest.Matchers.contains(0.0)))
                // [S7.9] Le férié travaillé est désormais compté. Cette métrique valait 0.0 pour
                // tout le monde depuis l'origine : le calendrier de RegulatoryParameters n'était
                // jamais alimenté. Sans cette assertion, la régression repasserait inaperçue.
                .andExpect(jsonPath("$.workMetrics.global.heuresJourFerieTotales").value(8.0))
                // SAL-2001 déclare travailleJourFerie=false : la contrainte HARD JourFerieRefuse
                // lui interdit le créneau du 13 mai, qui revient donc à SAL-2002.
                .andExpect(jsonPath("$.workMetrics.byRessource[?(@.resourceId=='SAL-2001')].heuresJourFerie")
                        .value(org.hamcrest.Matchers.contains(0.0)))
                .andExpect(jsonPath("$.workMetrics.byRessource[?(@.resourceId=='SAL-2002')].heuresJourFerie")
                        .value(org.hamcrest.Matchers.contains(8.0)))
                .andExpect(jsonPath("$.planning").exists())
                .andExpect(jsonPath("$.workMetrics").exists())
                .andExpect(jsonPath("$.solutionSummary").exists())
                .andExpect(jsonPath("$.planning.jours").isArray());
    }
}
