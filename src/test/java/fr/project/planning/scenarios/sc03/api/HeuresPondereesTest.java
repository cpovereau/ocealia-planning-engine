package fr.project.planning.scenarios.sc03.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.project.planning.docs.SchemaPublie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HeuresPondereesTest — lot L1 du chantier équité (V5, bout-en-bout).
 *
 * <h3>Ce que le lot introduit</h3>
 * <p>Une heure n'est pas une heure : <em>on ne juge l'équité qu'à pénibilité équivalente</em>.
 * {@code heuresPonderees} ramène la charge à une unité commune — l'heure ordinaire — en pondérant
 * chaque minute par le coefficient de <strong>sa seule</strong> catégorie de pénibilité.</p>
 *
 * <h3>Le piège que ces tests gardent</h3>
 * <p>Sans coefficients transmis, la mesure vaut exactement les heures brutes — le moteur n'invente
 * pas une échelle que seule la simulation peut établir. C'est le genre de valeur par défaut qui
 * laisse croire qu'un calcul a eu lieu, et deux tests le fixent : <strong>elle est silencieuse</strong>
 * quand rien n'est transmis, parce que c'est le cas de toutes les demandes jusqu'au lot L3, et
 * <strong>elle parle</strong> quand un bloc est transmis sans rien pondérer, parce que là quelqu'un
 * a cru configurer quelque chose.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class HeuresPondereesTest {

    private static final Path REFERENCE =
            Path.of("src/test/resources/scenarios/sc03/sc03_migration_reference.json");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Sans coefficients, la mesure pondérée vaut les heures brutes — en silence")
    void sansCoefficients_laMesureVautLesHeuresBrutes() throws Exception {
        JsonNode reponse = solve(lire());

        for (JsonNode metrique : reponse.get("workMetrics").get("byRessource")) {
            assertEquals(metrique.get("heuresTravaillees").asDouble(),
                    metrique.get("heuresPonderees").asDouble(), 0.001,
                    "Aucune pondération demandée : la mesure ne prétend rien de plus.");
        }

        assertTrue(!aLAlerte(reponse, "COEFFICIENTS_PENIBILITE_SANS_EFFET"),
                "C'est le cas de toutes les demandes tant que les coefficients ne sont pas "
                        + "calibrés : une alerte sur cent pour cent des réponses n'est pas un "
                        + "signal. L'égalité des deux mesures porte l'information.");
    }

    @Test
    @DisplayName("Un bloc de coefficients qui ne pondère rien est signalé : quelqu'un a cru configurer")
    void blocTransmisSansEffet_estSignale() throws Exception {
        JsonNode reponse = solve(avecCoefficients(1.0, 1.0, 1.0));

        assertTrue(aLAlerte(reponse, "COEFFICIENTS_PENIBILITE_SANS_EFFET"),
                "Transmettre un bloc neutre n'est pas la même chose que n'en transmettre aucun.");
    }

    @Test
    @DisplayName("Avec des coefficients, les heures pénibles pèsent plus — et seulement elles")
    void avecCoefficients_lesHeuresPeniblesPesentPlus() throws Exception {
        // Nuit à 2, dimanche et férié neutres : seules les minutes de nuit changent de poids.
        JsonNode reponse = solve(avecCoefficients(2.0, null, null));

        boolean auMoinsUnePondereeDifferente = false;
        for (JsonNode metrique : reponse.get("workMetrics").get("byRessource")) {
            double brutes = metrique.get("heuresTravaillees").asDouble();
            double ponderees = metrique.get("heuresPonderees").asDouble();
            double nuit = metrique.get("heuresNuit").asDouble();

            assertEquals(brutes + nuit, ponderees, 0.02,
                    metrique.get("resourceId").asText()
                            + " : une heure de nuit comptée deux fois, les autres une seule.");
            auMoinsUnePondereeDifferente |= ponderees > brutes;
        }
        assertTrue(auMoinsUnePondereeDifferente,
                "Le jeu d'essai doit contenir du travail de nuit, sinon ce test ne prouve rien.");

        assertTrue(!aLAlerte(reponse, "COEFFICIENTS_PENIBILITE_SANS_EFFET"),
                "Le bloc transmis pondère bien quelque chose : rien à signaler.");
    }

    @Test
    @DisplayName("Un coefficient négatif est refusé : une pénibilité ne s'allège pas")
    void coefficientNegatif_estRefuse() throws Exception {
        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(avecCoefficients(-1.0, null, null))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("L'écart au contrat est restitué, signé, avec la fenêtre qui lui sert de référence")
    void ecartAuContrat_estRestitueAvecSaFenetre() throws Exception {
        // [Équité L2] La mesure comparative : ce que chacun fait rapporté à ce qu'il doit.
        JsonNode requete = lire();
        JsonNode reponse = solve(requete);
        JsonNode horizon = requete.get("planningContext").get("horizon");
        long joursDeLHorizon = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.parse(horizon.get("dateDebut").asText()),
                java.time.LocalDate.parse(horizon.get("dateFin").asText())) + 1;

        /*
         * [Rang 14] Le dénominateur n'est pas l'horizon nu : c'est l'horizon moins les
         * indisponibilités déclarées de CHACUN. Le jeu d'essai pose un CONGE_POSE d'un seul jour,
         * sur SAL-2001 et lui seul — il en observe donc un de moins que les autres. Sans cette
         * déduction il paraîtrait sous son contrat, donc préférable, et le moteur lui rattraperait
         * son congé de part et d'autre.
         */
        JsonNode indisponibilites = requete.path("dataSet").path("indisponibilites").path("items");
        assertEquals(1, indisponibilites.size(),
                "Ce test suppose une indisponibilité unique dans le jeu d'essai : s'il en gagne "
                        + "une, l'attendu ci-dessous cesse d'être juste.");
        String salarieEnConge = indisponibilites.get(0).get("ressourceId").asText();
        assertEquals(indisponibilites.get(0).get("dateDebut").asText(),
                indisponibilites.get(0).get("dateFin").asText(),
                "Ce test suppose une absence d'un seul jour.");

        boolean auMoinsUnEcart = false;
        for (JsonNode metrique : reponse.get("workMetrics").get("byRessource")) {
            long joursAttendus = salarieEnConge.equals(metrique.get("resourceId").asText())
                    ? joursDeLHorizon - 1
                    : joursDeLHorizon;

            assertEquals(joursAttendus, metrique.get("joursObserves").asLong(),
                    "Le dénominateur est l'horizon déclaré où CE salarié était disponible, et "
                            + "l'appelant doit pouvoir le vérifier.");

            if (!metrique.get("ecartContratPourcent").isNull()) {
                auMoinsUnEcart = true;
                assertTrue(metrique.has("partNuits") && metrique.has("partDimanches")
                                && metrique.has("partFeries"),
                        "Les pénibilités se rapportent au contrat comme les heures.");
            }
        }
        assertTrue(auMoinsUnEcart,
                "Le jeu d'essai doit déclarer au moins un contrat, sinon rien n'est comparable.");
    }

    @Test
    @DisplayName("Sans volume contractuel, le moteur ne dit rien plutôt que de supposer")
    void sansContrat_lesMesuresComparativesSontNulles() throws Exception {
        ObjectNode requete = (ObjectNode) lire();
        for (JsonNode salarie : requete.get("dataSet").get("ressources").get("salaries")) {
            ((ObjectNode) salarie).remove("contrat");
        }

        for (JsonNode metrique : solve(requete).get("workMetrics").get("byRessource")) {
            assertTrue(metrique.get("ecartContratPourcent").isNull(),
                    "Rien n'est comparable sans référence : une valeur inventée serait pire "
                            + "qu'une absence de valeur.");
            assertTrue(metrique.get("partNuits").isNull());
        }
    }

    @Test
    @DisplayName("Le bloc workMetrics n'a ni champ non déclaré ni champ requis manquant")
    void workMetricsProduites_correspondentAuSchemaPublie() throws Exception {
        // Rien ne confrontait workMetrics au schéma publié, et il avait déjà dérivé :
        // nbCreneauxNuitNonNuit était restitué depuis la phase 8 sans y être déclaré. Avec
        // additionalProperties: false, un client qui validait sa réponse la voyait rejetée.
        SchemaPublie schema = SchemaPublie.charger();

        List<String> ecarts = new ArrayList<>(schema.confronter(
                solve(lire()).get("workMetrics"), "WorkMetrics", "workMetrics"));

        assertTrue(ecarts.isEmpty(),
                "Le schéma publié et la réponse produite divergent :\n  - "
                        + String.join("\n  - ", ecarts));
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static boolean aLAlerte(JsonNode reponse, String code) {
        for (JsonNode alerte : reponse.get("diagnostics").get("alerts")) {
            if (code.equals(alerte.get("code").asText())) {
                return true;
            }
        }
        return false;
    }

    private JsonNode lire() throws Exception {
        return objectMapper.readTree(Files.readString(REFERENCE));
    }

    private JsonNode avecCoefficients(Double nuit, Double dimanche, Double ferie) throws Exception {
        ObjectNode requete = (ObjectNode) lire();
        ObjectNode coefficients = objectMapper.createObjectNode();
        if (nuit != null) {
            coefficients.put("nuit", nuit);
        }
        if (dimanche != null) {
            coefficients.put("dimanche", dimanche);
        }
        if (ferie != null) {
            coefficients.put("ferie", ferie);
        }
        ((ObjectNode) requete.get("planningContext")).set("coefficientsPenibilite", coefficients);
        return requete;
    }

    private JsonNode solve(JsonNode requete) throws Exception {
        String reponse = mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(reponse);
    }
}
