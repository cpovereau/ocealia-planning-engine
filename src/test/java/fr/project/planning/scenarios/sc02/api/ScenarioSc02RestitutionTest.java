package fr.project.planning.scenarios.sc02.api;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioSc02RestitutionTest — lot S4 de SC-02 (V5, bout-en-bout).
 *
 * <h3>Ce que le lot ajoute</h3>
 * <p>Jusqu'au lot S3, la réponse comptait des <strong>créneaux</strong> et ne chiffrait que les
 * heures restées à pourvoir. L'encadrement lisait « un créneau sur deux repris » sans savoir de
 * combien d'heures on parlait — un créneau de huit heures et un d'une heure pèsent pareil dans un
 * décompte d'objets, et pas du tout dans un décompte d'heures.</p>
 *
 * <p>Les deux jeux joués ne diffèrent que par un paramètre — {@code posteVirtuelAutorise} — et
 * c'est le point : les heures que personne de réel ne couvre sont les mêmes, seul change
 * l'endroit où elles sont garées.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioSc02RestitutionTest {

    private static final String REFERENCE = "src/test/resources/scenarios/sc02/sc02_reference.json";
    private static final String POSTE_VIRTUEL =
            "src/test/resources/scenarios/sc02/sc02_poste_virtuel.json";
    private static final String SURCHARGE = "src/test/resources/scenarios/sc02/sc02_surcharge.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("L'absence est chiffrée en heures, pas seulement en créneaux")
    void absence_estChiffreeEnHeures() throws Exception {
        // Marie perd deux journées de 8 h. Paul reprend le mercredi entier ; le mardi est coupé à
        // 09:00 et Sophie n'en couvre que la première heure.
        JsonNode remplacement = objectMapper.readTree(postSc02(REFERENCE)).get("remplacement");

        assertEquals(16.0, remplacement.get("heuresLiberees").asDouble(),
                "Deux journées de 8 h : voilà ce que l'absence a rendu au solveur.");
        assertEquals(9.0, remplacement.get("heuresReprises").asDouble(),
                "Le mercredi entier, plus la première heure du mardi.");
        assertEquals(7.0, remplacement.get("heuresNonCouvertes").asDouble());
        assertEquals(0.0, remplacement.get("heuresSurPosteVirtuel").asDouble(),
                "Le poste virtuel n'est pas demandé : il ne s'invite pas.");
    }

    @Test
    @DisplayName("Les volumes se recomposent — l'appelant peut vérifier le compte")
    void volumes_seRecomposent() throws Exception {
        for (String fixture : List.of(REFERENCE, POSTE_VIRTUEL, SURCHARGE)) {
            JsonNode r = objectMapper.readTree(postSc02(fixture)).get("remplacement");

            double reprises = r.get("heuresReprises").asDouble();
            double surPosteVirtuel = r.get("heuresSurPosteVirtuel").asDouble();
            double nonCouvertes = r.get("heuresNonCouvertes").asDouble();

            assertEquals(r.get("heuresLiberees").asDouble(),
                    reprises + surPosteVirtuel + nonCouvertes, 0.001,
                    fixture + " : ce qui a été libéré est repris, garé ou laissé — sans reste.");
            assertEquals(r.get("heuresAPourvoir").asDouble(),
                    surPosteVirtuel + nonCouvertes, 0.001,
                    fixture + " : le total à staffer compte le poste virtuel comme le vide.");
        }
    }

    @Test
    @DisplayName("Un créneau repris en partie n'est ni repris ni abandonné")
    void reprisePartielle_aSonPropreDecompte() throws Exception {
        JsonNode remplacement = objectMapper.readTree(postSc02(REFERENCE)).get("remplacement");

        assertEquals(2, remplacement.get("creneauxLiberes").asInt());
        assertEquals(1, remplacement.get("creneauxRepris").asInt(),
                "Le mercredi, et lui seul, est repris de bout en bout par un salarié réel.");
        assertEquals(1, remplacement.get("creneauxPartiellementRepris").asInt(),
                "Le mardi a trouvé quelqu'un pour une heure sur huit : le ranger parmi les "
                        + "créneaux sans repreneur ferait mentir le décompte.");
    }

    @Test
    @DisplayName("L'alerte ne compte plus une reprise partielle parmi les créneaux sans repreneur")
    void alerte_distingueLaReprisePartielleDeLAbandon() throws Exception {
        // Le message soustrayait les créneaux repris des créneaux libérés et annonçait le reste
        // comme n'ayant « trouvé aucun salarié ». Le mardi y figurait, alors que Sophie en couvre
        // une heure.
        String message = alerte(postSc02(REFERENCE), "HEURES_RESTANT_A_POURVOIR");

        assertTrue(message.contains("1 repris en entier"), message);
        assertTrue(message.contains("1 en partie"), message);
        assertTrue(message.contains("0 sans aucun repreneur"),
                "Les deux créneaux libérés ont trouvé quelqu'un, fût-ce pour une part : " + message);
    }

    @Test
    @DisplayName("Poste virtuel demandé : les heures y sont garées, et restent à pourvoir")
    void posteVirtuelDemande_gareLesHeuresSansLesCouvrir() throws Exception {
        // Même jeu que la référence, à un paramètre près. Les sept heures que personne ne peut
        // prendre ne changent pas de volume — elles changent d'endroit.
        JsonNode reponse = objectMapper.readTree(postSc02(POSTE_VIRTUEL));
        JsonNode remplacement = reponse.get("remplacement");

        assertEquals(7.0, remplacement.get("heuresSurPosteVirtuel").asDouble());
        assertEquals(0.0, remplacement.get("heuresNonCouvertes").asDouble());
        assertEquals(7.0, remplacement.get("heuresAPourvoir").asDouble(),
                "Garer des heures sur un poste fictif ne les couvre pas : la question de "
                        + "l'encadrement reste « combien me reste-t-il à staffer ».");

        JsonNode reste = detail(remplacement, "PLN-MARIE-MAR#S2");
        assertEquals("POSTE_VIRTUEL", reste.get("nature").asText());
        assertEquals("PV-SOINS", reste.get("ressourceApresId").asText(),
                "Le planning dit où elles sont ; le total dit combien il y en a.");
    }

    @Test
    @DisplayName("Les heures sont au même format que partout ailleurs dans la réponse")
    void heures_sontAuFormatHeuresMinutes() throws Exception {
        // Le bloc planning de la même réponse produit HH:mm ; details[] sortait en HH:mm:ss.
        // Deux formats pour la même grandeur dans un seul document obligeraient l'appelant à
        // savoir lequel s'applique où.
        JsonNode reponse = objectMapper.readTree(postSc02(REFERENCE));

        for (JsonNode detail : reponse.get("remplacement").get("details")) {
            assertEquals(5, detail.get("heureDebut").asText().length(), detail.toString());
            assertEquals(5, detail.get("heureFin").asText().length(), detail.toString());
        }

        JsonNode premierJour = reponse.get("planning").get("jours").get(0);
        assertEquals(5, premierJour.get("creneaux").get(0).get("heureDebut").asText().length(),
                "C'est le format du bloc planning qui fait référence ; details[] s'y aligne.");
    }

    @Test
    @DisplayName("Le bloc remplacement n'a ni champ non déclaré ni champ requis manquant")
    void remplacementProduit_correspondAuSchemaPublie() throws Exception {
        // 50_ScenarioResponse.schema.json est publié aux intégrateurs et porte
        // additionalProperties: false. Un champ ajouté ici et pas là-bas fait rejeter la réponse
        // chez un client qui la valide — c'est exactement ce qui était arrivé au lot S8.4.
        SchemaPublie schema = SchemaPublie.charger();

        List<String> ecarts = new ArrayList<>();
        for (String fixture : List.of(REFERENCE, POSTE_VIRTUEL, SURCHARGE)) {
            ecarts.addAll(schema.confronter(
                    objectMapper.readTree(postSc02(fixture)).get("remplacement"),
                    "Remplacement", fixture + " → remplacement"));
        }

        assertTrue(ecarts.isEmpty(),
                "Le schéma publié et la réponse produite divergent :\n  - "
                        + String.join("\n  - ", ecarts));
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static String alerte(String reponse, String code) throws Exception {
        for (JsonNode alerte : new ObjectMapper().readTree(reponse)
                .get("diagnostics").get("alerts")) {
            if (code.equals(alerte.get("code").asText())) {
                return alerte.get("message").asText();
            }
        }
        throw new AssertionError("Alerte absente de la réponse : " + code);
    }

    private static JsonNode detail(JsonNode remplacement, String creneauId) {
        for (JsonNode detail : remplacement.get("details")) {
            if (creneauId.equals(detail.get("creneauId").asText())) {
                return detail;
            }
        }
        throw new AssertionError("Créneau absent du bloc remplacement : " + creneauId);
    }

    private String postSc02(String chemin) throws Exception {
        return mockMvc.perform(post("/scenarios/sc02/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(Objects.requireNonNull(Files.readString(Path.of(chemin)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
