package fr.project.planning.scenarios.sc06.api;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioControllerSc06ValidationTest — lot S4
 *
 * <p>Vérifie les garde-fous de SC-06. Aucun d'eux n'est défensif par principe : chacun protège
 * d'un résultat <strong>faux mais crédible</strong>, plus coûteux qu'une erreur franche.</p>
 *
 * <p>Codes attendus : 400 pour une requête malformée (validation de contrat),
 * 422 pour une requête bien formée mais incohérente (garde-fou métier).</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioControllerSc06ValidationTest {

    private static final Path REFERENCE = Path.of("src/test/resources/scenarios/sc06/sc06_reference.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void horizonQuiNeCouvrePasLaSemainePleine_estRefuse() throws Exception {
        // La durée hebdomadaire maximale se mesure sur ce que le moteur reçoit. Une semaine
        // tronquée sous-évalue le total, ne détecte aucun dépassement, et déclare conformes des
        // candidats qui ne le sont pas. Mieux vaut refuser que répondre faux.
        String json = reference().replace("\"dateFin\": \"2026-05-17\"", "\"dateFin\": \"2026-05-15\"");

        postSc06(json).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void horizonDecaleDUnJour_estRefuse() throws Exception {
        // Sept jours, mais du mardi au lundi : ce n'est pas la semaine calendaire du besoin.
        String json = reference()
                .replace("\"dateDebut\": \"2026-05-11\",\n      \"dateFin\": \"2026-05-17\"",
                        "\"dateDebut\": \"2026-05-12\",\n      \"dateFin\": \"2026-05-18\"");

        postSc06(json).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void creneauDuPlanningSansRessourceAffectee_estRefuse() throws Exception {
        // Un créneau de planning sans affectation deviendrait une variable de décision
        // silencieuse — exactement ce que SC-06 s'interdit.
        String json = reference().replace("\"ressourceAffecteeId\": \"SAL-2003\"", "\"lieu\": \"HOPITAL-NORD\"");

        postSc06(json).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void besoinPortantUneActiviteInconnue_estRefuse() throws Exception {
        // Une activité absente du référentiel ne pèse sur aucune règle : le besoin serait
        // couvert sans jamais avoir été évalué.
        String json = reference().replace(
                "\"codeActiviteId\": \"ACT-SOIN\",\n          \"lieu\": \"HOPITAL-NORD\",\n          \"posteComptable\": \"PC-SOINS\"",
                "\"codeActiviteId\": \"ACT-FANTOME\",\n          \"lieu\": \"HOPITAL-NORD\",\n          \"posteComptable\": \"PC-SOINS\"");

        postSc06(json).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void creneauDuPlanningReferencantUneRessourceInconnue_estRefuse() throws Exception {
        String json = reference().replace("\"ressourceAffecteeId\": \"SAL-2003\"",
                "\"ressourceAffecteeId\": \"SAL-INEXISTANT\"");

        postSc06(json).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void mauvaisScenarioType_estRefuse() throws Exception {
        String json = reference().replace("\"scenarioType\": \"SC-06\"", "\"scenarioType\": \"SC-03\"");

        postSc06(json).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void besoinAbsent_estRefuse() throws Exception {
        String json = reference().replaceFirst("\"scenarioParameters\"", "\"scenarioParametersIgnore\"");

        postSc06(json).andExpect(status().isBadRequest());
    }

    @Test
    void requestIdAbsent_estRefuse() throws Exception {
        // Convention partagée avec SC-03 : requestId et metadata sont obligatoires.
        String json = reference().replace("\"requestId\": \"REQ-SC06-REF-001\",", "");

        postSc06(json).andExpect(status().isBadRequest());
    }

    @Test
    void datasetSansSalarie_estRefuse() throws Exception {
        // Sans candidat, la question n'a pas de réponse.
        String json = reference().replaceAll("(?s)\"salaries\": \\[.*?\\],\n      \"postesVirtuels\"",
                "\"salaries\": [],\n      \"postesVirtuels\"");

        postSc06(json).andExpect(status().isUnprocessableEntity());
    }

    // =========================================================
    // Helpers
    // =========================================================

    private String reference() throws Exception {
        return Objects.requireNonNull(Files.readString(REFERENCE));
    }

    private org.springframework.test.web.servlet.ResultActions postSc06(String json) throws Exception {
        return mockMvc.perform(post("/scenarios/sc06/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(json));
    }
}
