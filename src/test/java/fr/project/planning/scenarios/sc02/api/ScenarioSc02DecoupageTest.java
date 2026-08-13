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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioSc02DecoupageTest — lot S2 de SC-02 (V5, bout-en-bout).
 *
 * <h3>Les deux cas de l'arbitrage métier, joués tels quels</h3>
 * <p>Le créneau à remplacer est un <strong>13h30–16h00</strong>, les deux jours.</p>
 *
 * <ul>
 *   <li><strong>Mardi</strong> — M. X prend son service à 15h30. On peut lui confier 13h30–15h30 ;
 *       les 30 minutes restantes ne sont pas couvertes et partent à pourvoir. C'est le cas que
 *       l'arbitrage décrit mot pour mot.</li>
 *   <li><strong>Mercredi</strong> — M. Y commence à 13h45. On ne lui propose <em>pas</em>
 *       13h30–13h45 : quinze minutes ne se confient pas. Le créneau entier part à pourvoir.</li>
 * </ul>
 *
 * <p>Sur chaque jour, l'autre salarié est occupé toute la journée : le cas est donc isolé, et
 * l'issue ne dépend que de la frontière de disponibilité étudiée.</p>
 *
 * <p>Noter que <strong>13h45 n'appartient à aucune grille de 30 minutes</strong>. C'est ce qui
 * disqualifie l'idée d'un pas fixe : une coupe n'a de sens que là où la disponibilité de quelqu'un
 * change.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioSc02DecoupageTest {

    private static final String DECOUPAGE = "src/test/resources/scenarios/sc02/sc02_decoupage.json";
    private static final String REFERENCE = "src/test/resources/scenarios/sc02/sc02_reference.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Un remplaçant qui prend son service à 15h30 couvre 13h30–15h30, le reste part à pourvoir")
    void frontiereDeDisponibilite_produitUneCouverturePartielle() throws Exception {
        JsonNode remplacement = objectMapper.readTree(postSc02(DECOUPAGE)).get("remplacement");

        JsonNode couvert = detail(remplacement, "BESOIN-MARDI#S1");
        assertEquals("SALARIE", couvert.get("nature").asText());
        assertEquals("SAL-X", couvert.get("ressourceApresId").asText());
        assertEquals("13:30", couvert.get("heureDebut").asText());
        assertEquals("15:30", couvert.get("heureFin").asText());
        assertEquals(120, couvert.get("dureeMinutes").asInt());

        JsonNode reste = detail(remplacement, "BESOIN-MARDI#S2");
        assertEquals("NON_COUVERT", reste.get("nature").asText());
        assertEquals("15:30", reste.get("heureDebut").asText());
        assertEquals(30, reste.get("dureeMinutes").asInt(),
                "Un reliquat n'est soumis à aucun minimum : il part à pourvoir tel qu'il est.");
    }

    @Test
    @DisplayName("Quinze minutes ne se confient pas : le seuil porte sur le bloc, pas sur le reliquat")
    void blocTropCourt_nEstJamaisConfie() throws Exception {
        JsonNode remplacement = objectMapper.readTree(postSc02(DECOUPAGE)).get("remplacement");

        // M. Y est libre de 13h30 à 13h45, et lui seul. Quinze minutes : on ne les lui donne pas.
        for (JsonNode detail : remplacement.get("details")) {
            String besoin = detail.has("creneauOrigineId")
                    ? detail.get("creneauOrigineId").asText()
                    : detail.get("creneauId").asText();
            if (!"BESOIN-MERCREDI".equals(besoin)) {
                continue;
            }
            assertEquals("NON_COUVERT", detail.get("nature").asText(),
                    "Aucun morceau du mercredi ne doit être confié à qui que ce soit.");
        }

        // Les deux morceaux non couverts sont contigus : ils reviennent en un seul créneau,
        // sous l'identifiant d'origine.
        JsonNode mercredi = detail(remplacement, "BESOIN-MERCREDI");
        assertEquals(150, mercredi.get("dureeMinutes").asInt());
        assertFalse(mercredi.has("creneauOrigineId"),
                "Reconstitué en entier, le créneau n'est plus un morceau de quoi que ce soit.");
    }

    @Test
    @DisplayName("Le total des heures à pourvoir agrège les deux jours")
    void heuresAPourvoir_agregeLesDeuxJours() throws Exception {
        JsonNode remplacement = objectMapper.readTree(postSc02(DECOUPAGE)).get("remplacement");

        assertEquals(2, remplacement.get("creneauxLiberes").asInt(),
                "Deux créneaux d'origine libérés — le découpage ne gonfle pas ce compteur.");
        assertEquals(0, remplacement.get("creneauxRepris").asInt(),
                "Aucun n'est repris en entier : le mardi ne l'est qu'à moitié.");
        assertEquals(3.0, remplacement.get("heuresAPourvoir").asDouble(),
                "30 minutes le mardi, 2 h 30 le mercredi.");
    }

    @Test
    @DisplayName("Le découpage est signalé : la réponse peut contenir plus de créneaux que la demande")
    void decoupage_estSignale() throws Exception {
        JsonNode alerts = objectMapper.readTree(postSc02(DECOUPAGE)).get("diagnostics").get("alerts");

        boolean signale = false;
        for (JsonNode alerte : alerts) {
            if ("CRENEAUX_DECOUPES".equals(alerte.get("code").asText())) {
                signale = true;
                assertEquals("INFO", alerte.get("severity").asText(),
                        "Le découpage est le mécanisme prévu, pas une anomalie.");
            }
        }
        assertTrue(signale);
    }

    @Test
    @DisplayName("Un créneau repris en entier par une seule personne ressort entier, sous son identifiant")
    void creneauNonFragmente_gardeSonIdentite() throws Exception {
        // Jeu de référence : le mercredi est coupé à 09:00 par le service de Sophie, mais Paul est
        // libre toute la journée. Le lui confier entièrement coûte moins cher que de partager —
        // c'est la contrainte de cohésion qui l'impose — et la recombinaison efface la coupe.
        JsonNode reponse = objectMapper.readTree(postSc02(REFERENCE));
        JsonNode mercredi = detail(reponse.get("remplacement"), "PLN-MARIE-MER");

        assertEquals("SALARIE", mercredi.get("nature").asText());
        assertEquals("SAL-PAUL", mercredi.get("ressourceApresId").asText());
        assertEquals(480, mercredi.get("dureeMinutes").asInt(),
                "Les deux morceaux sont revenus à Paul : le créneau est reconstitué en entier.");
        assertFalse(mercredi.has("creneauOrigineId"));
    }

    // =========================================================
    // Helpers
    // =========================================================

    private String postSc02(String chemin) throws Exception {
        return mockMvc.perform(post("/scenarios/sc02/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(Files.readString(Path.of(chemin)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static JsonNode detail(JsonNode remplacement, String creneauId) {
        for (JsonNode detail : remplacement.get("details")) {
            if (creneauId.equals(detail.get("creneauId").asText())) {
                return detail;
            }
        }
        throw new AssertionError("Créneau absent du bloc remplacement : " + creneauId);
    }
}
