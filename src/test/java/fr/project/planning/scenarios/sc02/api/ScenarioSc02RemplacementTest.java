package fr.project.planning.scenarios.sc02.api;

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
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioSc02RemplacementTest — lot S1 de SC-02 (V5, bout-en-bout).
 *
 * <h3>La situation jouée</h3>
 * <p>Marie est absente les 12 et 13 mai. Elle avait trois créneaux dans la semaine :</p>
 * <ul>
 *   <li><strong>mardi 12</strong> — pendant l'absence. Paul et Sophie travaillent déjà à cette
 *       heure-là : personne ne peut le reprendre, il part à pourvoir ;</li>
 *   <li><strong>mercredi 13</strong> — pendant l'absence. Sophie est occupée, Paul est libre :
 *       il le reprend ;</li>
 *   <li><strong>vendredi 15</strong> — <em>hors</em> absence. Il reste à Marie, épinglé : le
 *       scénario ne libère que ce que l'absence recouvre.</li>
 * </ul>
 *
 * <p>Le mardi tombe à pourvoir à cause d'un <strong>chevauchement physique</strong>, contrainte
 * HARD effectivement appliquée — et non à cause de l'activité, que le moteur n'oppose encore à
 * personne (rang 8 du backlog, cf. {@code 90_SUIVI_DEVELOPPEMENT_MOTEUR.md}). Le jeu d'essai
 * s'appuie délibérément sur une règle en vigueur, pas sur une règle attendue.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioSc02RemplacementTest {

    private static final String REFERENCE = "src/test/resources/scenarios/sc02/sc02_reference.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Seuls les créneaux que l'absence recouvre sont rendus au solveur")
    void absence_neLibereQueLesCreneauxQuElleRecouvre() throws Exception {
        JsonNode remplacement = objectMapper.readTree(postSc02()).get("remplacement");

        assertNotNull(remplacement, "SC-02 doit porter son bloc propre.");
        assertEquals("SAL-MARIE", remplacement.get("salarieAbsentId").asText());
        assertEquals(2, remplacement.get("creneauxLiberes").asInt(),
                "Le créneau du vendredi est hors absence : il n'avait pas à être libéré.");

        boolean vendrediPresent = false;
        for (JsonNode detail : remplacement.get("details")) {
            vendrediPresent |= "PLN-MARIE-VEN".equals(detail.get("creneauId").asText());
        }
        assertFalse(vendrediPresent, "Le créneau hors absence ne figure pas au remplacement.");
    }

    @Test
    @DisplayName("Le planning des présents n'est pas remanié, et celui de l'absent hors absence non plus")
    void existant_resteEnPlace() throws Exception {
        JsonNode planning = objectMapper.readTree(postSc02()).get("planning");

        assertEquals("SAL-PAUL", ressourceDe(planning, "PLN-PAUL-MAR"));
        assertEquals("SAL-SOPHIE", ressourceDe(planning, "PLN-SOPHIE-MAR"));
        assertEquals("SAL-SOPHIE", ressourceDe(planning, "PLN-SOPHIE-MER"));
        assertEquals("SAL-MARIE", ressourceDe(planning, "PLN-MARIE-VEN"),
                "Marie n'est absente que deux jours : son vendredi lui reste.");
    }

    @Test
    @DisplayName("Un remplaçant disponible reprend le créneau entier")
    void creneauCouvrable_estRepris() throws Exception {
        JsonNode reponse = objectMapper.readTree(postSc02());
        JsonNode mercredi = detail(reponse.get("remplacement"), "PLN-MARIE-MER");

        assertEquals("SALARIE", mercredi.get("nature").asText());
        assertEquals("SAL-PAUL", mercredi.get("ressourceApresId").asText(),
                "Sophie est occupée ce jour-là : Paul est le seul disponible.");
        assertEquals("SAL-MARIE", mercredi.get("ressourceAvantId").asText());
        assertEquals(480, mercredi.get("dureeMinutes").asInt());
        assertEquals("SAL-PAUL", ressourceDe(reponse.get("planning"), "PLN-MARIE-MER"));
    }

    @Test
    @DisplayName("Ce que personne ne peut couvrir devient des heures à pourvoir — jamais zéro solution")
    void creneauSansRepreneur_devientDesHeuresAPourvoir() throws Exception {
        // [Lot S2] Le mardi n'est plus insoluble d'un bloc : il est coupé à 09:00, l'heure où
        // Sophie prend son service, et elle en couvre la première heure. Restent sept heures.
        // Avant le découpage, les huit heures partaient à pourvoir d'un seul tenant.
        JsonNode reponse = objectMapper.readTree(postSc02());
        JsonNode remplacement = reponse.get("remplacement");
        JsonNode reste = detail(remplacement, "PLN-MARIE-MAR#S2");

        assertEquals("NON_COUVERT", reste.get("nature").asText());
        assertFalse(reste.has("ressourceApresId"),
                "Personne ne le reprend : la clé est omise plutôt que rendue vide.");
        assertEquals("PLN-MARIE-MAR", reste.get("creneauOrigineId").asText(),
                "Un morceau se rattache à son origine sans qu'on ait à analyser son identifiant.");
        assertEquals(1, remplacement.get("creneauxRepris").asInt(),
                "Seul le mercredi est repris en entier ; le mardi ne l'est qu'à moitié.");
        assertEquals(7.0, remplacement.get("heuresAPourvoir").asDouble());

        // Le scénario aboutit malgré tout : il rend visible l'impossible, il ne refuse pas.
        assertEquals("SOLVED", reponse.get("solverResult").get("status").asText());
    }

    @Test
    @DisplayName("Les heures restées à pourvoir sont signalées, pas seulement totalisées")
    void heuresAPourvoir_produisentUneAlerte() throws Exception {
        JsonNode alerts = objectMapper.readTree(postSc02()).get("diagnostics").get("alerts");

        boolean signalee = false;
        for (JsonNode alerte : alerts) {
            if ("HEURES_RESTANT_A_POURVOIR".equals(alerte.get("code").asText())) {
                signalee = true;
                assertEquals("WARNING", alerte.get("severity").asText());
                assertTrue(alerte.get("message").asText().contains("SAL-MARIE"),
                        "Le message doit nommer l'absent dont les heures restent en suspens.");
            }
        }
        assertTrue(signalee, "L'appelant doit l'apprendre, pas le déduire d'un total.");
    }

    @Test
    @DisplayName("Le poste virtuel ne s'invite pas : non demandé, les heures reviennent sans ressource")
    void posteVirtuelNonDemande_laisseLesHeuresSansRessource() throws Exception {
        JsonNode remplacement = objectMapper.readTree(postSc02()).get("remplacement");

        for (JsonNode detail : remplacement.get("details")) {
            assertFalse("POSTE_VIRTUEL".equals(detail.get("nature").asText()),
                    "posteVirtuelAutorise est absent de la demande, donc refusé.");
        }
    }

    // =========================================================
    // Helpers
    // =========================================================

    private String postSc02() throws Exception {
        return mockMvc.perform(post("/scenarios/sc02/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(lire()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Ressource affectée à un créneau dans le planning restitué, jour par jour. */
    private static String ressourceDe(JsonNode planning, String creneauId) {
        for (JsonNode jour : planning.get("jours")) {
            for (JsonNode creneau : jour.get("creneaux")) {
                if (creneauId.equals(creneau.get("id").asText())) {
                    return creneau.get("ressourceAffecteeId").asText();
                }
            }
        }
        throw new AssertionError("Créneau absent du planning restitué : " + creneauId);
    }

    private static JsonNode detail(JsonNode remplacement, String creneauId) {
        for (JsonNode detail : remplacement.get("details")) {
            if (creneauId.equals(detail.get("creneauId").asText())) {
                return detail;
            }
        }
        throw new AssertionError("Créneau absent du bloc remplacement : " + creneauId);
    }

    private static String lire() throws Exception {
        return Objects.requireNonNull(Files.readString(Path.of(REFERENCE)));
    }
}
