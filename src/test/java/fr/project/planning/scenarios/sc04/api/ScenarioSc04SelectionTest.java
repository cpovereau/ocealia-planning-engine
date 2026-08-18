package fr.project.planning.scenarios.sc04.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioSc04SelectionTest — lot O5 : la liste explicite de créneaux ajustables.
 *
 * <h3>Ce que ces tests pinent, et qu'aucun autre ne pine</h3>
 * <p>La <strong>conjonction</strong>. Un moteur qui lirait l'union au lieu de l'intersection
 * passerait la plupart des tests de SC-04 sans broncher : il rendrait un planning optimisé, avec
 * ses séries, ses motifs et son schéma conforme. Il aurait simplement réécrit le passé. C'est
 * pourquoi {@code laSelectionNAtteintPasLePasse} vaut plus que tous les autres réunis.</p>
 *
 * <h3>Le cas de base</h3>
 * <p>Celui de {@code ScenarioSc04OptimisationTest} — deux semaines, pivot au lundi 18, seconde
 * semaine entièrement sur SAL-A. Dix créneaux ajustables sans sélection ; la liste sert à en
 * retirer.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioSc04SelectionTest {

    private static final Path REFERENCE = Path.of("src/test/resources/scenarios/sc04/sc04_reference.json");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("La sélection restreint l'après-pivot : ce qui n'est pas désigné ne bouge pas")
    void laSelectionRestreintLApresPivot() throws Exception {
        JsonNode reponse = solve(avecSelection(
                "FUTUR-MATIN-0", "FUTUR-MATIN-1", "FUTUR-MATIN-2", "FUTUR-MATIN-3", "FUTUR-MATIN-4"));

        assertEquals(5, reponse.get("optimisation").get("creneauxAjustables").asInt(),
                "Cinq désignés sur dix créneaux postérieurs au pivot.");
        assertEquals(15, reponse.get("optimisation").get("creneauxFiges").asInt(),
                "Les dix du passé, plus les cinq après-midi que la liste ne désigne pas.");

        for (JsonNode creneau : tousLesCreneaux(reponse)) {
            if (creneau.get("id").asText().startsWith("FUTUR-APREM")) {
                assertEquals("SAL-A", creneau.get("ressourceAffecteeId").asText(),
                        "Non désigné, donc figé : " + creneau.get("id").asText()
                                + " devait rester à SAL-A.");
            }
        }
    }

    @Test
    @DisplayName("La sélection n'atteint pas le passé : intersection, jamais union")
    void laSelectionNAtteintPasLePasse() throws Exception {
        // PASSE-MATIN-0 est désigné ET antérieur au pivot. Sous l'union il redeviendrait
        // ajustable ; sous l'intersection il reste figé, et c'est tout le sujet de l'arbitrage.
        JsonNode reponse = solve(avecSelection("PASSE-MATIN-0", "FUTUR-MATIN-0"));

        assertEquals(1, reponse.get("optimisation").get("creneauxAjustables").asInt(),
                "Un seul des deux désignés est postérieur au pivot.");
        for (JsonNode creneau : tousLesCreneaux(reponse)) {
            if ("PASSE-MATIN-0".equals(creneau.get("id").asText())) {
                assertEquals("SAL-A", creneau.get("ressourceAffecteeId").asText(),
                        "Le passé ne se réécrit pas, même désigné nommément.");
            }
        }
        assertTrue(aLAlerte(reponse, "CRENEAU_AJUSTABLE_ANTERIEUR_AU_PIVOT"),
                "Une demande sans effet doit se voir : c'est le plus souvent le pivot qui est mal "
                        + "placé, et l'appelant doit pouvoir le comprendre.");
    }

    @Test
    @DisplayName("Sélection absente : SC-04 est exactement ce qu'il était avant le lot O5")
    void laSelectionAbsenteNeChangeRien() throws Exception {
        JsonNode reponse = solve(lire());

        assertEquals(10, reponse.get("optimisation").get("creneauxAjustables").asInt());
        assertEquals(10, reponse.get("optimisation").get("creneauxFiges").asInt());
        assertFalse(aLAlerte(reponse, "AUCUN_CRENEAU_AJUSTABLE"));
    }

    @Test
    @DisplayName("Sélection vide : plus rien n'est ajustable — absent et vide ne disent pas la même chose")
    void laSelectionVideGeleTout() throws Exception {
        JsonNode reponse = solve(avecSelection());

        assertEquals(0, reponse.get("optimisation").get("creneauxAjustables").asInt(),
                "Transmettre une liste vide, c'est demander de ne rien rouvrir. L'omettre aurait "
                        + "rouvert tout l'après-pivot : le champ n'a donc pas de @NotEmpty.");
        assertEquals(0, reponse.get("optimisation").get("creneauxDeplaces").asInt());
        assertTrue(aLAlerte(reponse, "AUCUN_CRENEAU_AJUSTABLE"),
                "Le moteur ne refuse pas : il rend visible que la demande n'a pas d'objet.");
    }

    @Test
    @DisplayName("Un identifiant inconnu est nommé, pas ignoré")
    void unIdentifiantInconnuEstNomme() throws Exception {
        JsonNode reponse = solve(avecSelection("FUTUR-MATIN-0", "CRE-QUI-NEXISTE-PAS"));

        assertEquals(1, reponse.get("optimisation").get("creneauxAjustables").asInt());
        assertTrue(aLAlerte(reponse, "CRENEAU_AJUSTABLE_INTROUVABLE"),
                "Une sélection dont la moitié ne correspond à rien produirait une optimisation "
                        + "bien plus étroite que demandée, sans que personne ne voie pourquoi.");
    }

    @Test
    @DisplayName("Un besoin futur que la liste laisse gelé est signalé — il sort du décompte")
    void unTrouFuturNonDesigneEstSignale() throws Exception {
        // Ce créneau du soir n'est couvert par personne et n'est pas désigné : il reste découvert,
        // et n'apparaît pas dans creneauxNonCouverts, qui ne compte que les ajustables.
        ObjectNode requete = (ObjectNode) avecSelection("FUTUR-MATIN-0");
        ajouterCreneau(requete, "FUTUR-SOIR-0", "2026-05-18", "18:00", "20:00", null);

        JsonNode reponse = solve(requete);

        assertEquals(0, reponse.get("optimisation").get("creneauxNonCouverts").asInt(),
                "Le trou est hors du décompte : c'est précisément ce que l'alerte doit rattraper.");
        assertTrue(aLAlerte(reponse, "CRENEAU_FUTUR_NON_COUVERT_GELE"),
                "Ne pas lister, c'est renoncer à couvrir — une décision recevable, qui doit se "
                        + "lire dans la réponse et non se découvrir sur le terrain.");
    }

    @Test
    @DisplayName("Un mois exactement est accepté")
    void unMoisExactementEstAccepte() throws Exception {
        // Premier désigné postérieur au pivot : 2026-05-18. Dernier jour désignable : 2026-06-17.
        ObjectNode requete = surUnHorizonLarge();
        ajouterCreneau(requete, "LOIN", "2026-06-17", "08:00", "12:00", "SAL-A");
        selectionner(requete, "FUTUR-MATIN-0", "LOIN");

        JsonNode reponse = solve(requete);
        assertEquals(2, reponse.get("optimisation").get("creneauxAjustables").asInt());
    }

    @Test
    @DisplayName("Un jour de plus est refusé : la zone remaniable est bornée à un mois glissant")
    void unJourDePlusEstRefuse() throws Exception {
        ObjectNode requete = surUnHorizonLarge();
        ajouterCreneau(requete, "LOIN", "2026-06-18", "08:00", "12:00", "SAL-A");
        selectionner(requete, "FUTUR-MATIN-0", "LOIN");

        // Une liste hors borne n'est pas un planning impossible : c'est une requête mal formée.
        // Tronquer en silence rendrait un résultat partiel que l'appelant lirait comme complet.
        refuse(requete);
    }

    @Test
    @DisplayName("Un créneau désigné hors horizon ne gonfle pas l'amplitude")
    void unCreneauHorsHorizonNeGonflePasLAmplitude() throws Exception {
        // Ce qui est borné est la zone effectivement remaniable, non la liste transmise : un
        // dataset de trois mois avec un horizon de deux semaines ne doit pas se voir refuser une
        // demande que le moteur aurait de toute façon restreinte à deux semaines.
        ObjectNode requete = (ObjectNode) lire();
        ajouterCreneau(requete, "HORS-HORIZON", "2026-07-01", "08:00", "12:00", "SAL-A");
        selectionner(requete, "FUTUR-MATIN-0", "HORS-HORIZON");

        JsonNode reponse = solve(requete);
        assertEquals(1, reponse.get("optimisation").get("creneauxAjustables").asInt());
        assertTrue(aLAlerte(reponse, "CRENEAU_AJUSTABLE_INTROUVABLE"),
                "Écarté avant résolution, donc jamais rencontré : la sélection doit le dire.");
    }

    @Test
    @DisplayName("Le bloc optimisation reste conforme au schéma publié, sélection comprise")
    void leBlocResteConformeAuSchemaPublie() throws Exception {
        SchemaPublie schema = SchemaPublie.charger();

        List<String> ecarts = schema.confronter(
                solve(avecSelection("FUTUR-MATIN-0", "FUTUR-MATIN-1")).get("optimisation"),
                "Optimisation", "optimisation (avec sélection)");

        assertTrue(ecarts.isEmpty(),
                "Le bloc produit et le schéma publié ont divergé :\n  - "
                        + String.join("\n  - ", ecarts));
    }

    // =====================================================================
    // Montage
    // =====================================================================

    private JsonNode lire() throws Exception {
        return objectMapper.readTree(Files.readString(REFERENCE, StandardCharsets.UTF_8));
    }

    private JsonNode avecSelection(String... ids) throws Exception {
        ObjectNode requete = (ObjectNode) lire();
        selectionner(requete, ids);
        return requete;
    }

    private static void selectionner(ObjectNode requete, String... ids) {
        ObjectNode parametres = (ObjectNode) requete.get("scenarioParameters");
        ArrayNode selection = parametres.putArray("creneauxAjustables");
        for (String id : ids) {
            selection.add(id);
        }
    }

    /** L'horizon élargi à juillet, pour que la borne du mois ait de quoi être dépassée. */
    private ObjectNode surUnHorizonLarge() throws Exception {
        ObjectNode requete = (ObjectNode) lire();
        ((ObjectNode) requete.get("planningContext").get("horizon"))
                .put("dateFin", "2026-07-31");
        return requete;
    }

    private static void ajouterCreneau(ObjectNode requete, String id, String date,
                                       String debut, String fin, String ressourceId) {
        ObjectNode creneau = ((ArrayNode) requete.get("dataSet").get("creneaux")).addObject();
        creneau.put("id", id);
        creneau.put("date", date);
        creneau.put("heureDebut", debut);
        creneau.put("heureFin", fin);
        creneau.put("lieu", "HOPITAL-NORD");
        creneau.put("codeActiviteId", "ACT-SOIN");
        if (ressourceId != null) {
            creneau.put("ressourceAffecteeId", ressourceId);
        }
    }

    private JsonNode solve(JsonNode requete) throws Exception {
        String corps = mockMvc.perform(post("/scenarios/sc04/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode reponse = objectMapper.readTree(corps);
        assertNotNull(reponse.get("optimisation"), "SC-04 doit porter son bloc optimisation.");
        return reponse;
    }

    /**
     * 422, non 400 : le JSON est bien formé et la {@code datePivot} présente. Ce qui ne passe pas
     * est la <em>demande</em>, pas sa syntaxe — c'est la convention du moteur, portée par
     * {@code GlobalExceptionHandler}, et le contrat publié l'annonce déjà pour SC-04.
     */
    private void refuse(JsonNode requete) throws Exception {
        mockMvc.perform(post("/scenarios/sc04/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isUnprocessableEntity());
    }

    private static Iterable<JsonNode> tousLesCreneaux(JsonNode reponse) {
        ArrayNode tous = new ObjectMapper().createArrayNode();
        for (JsonNode jour : reponse.get("planning").get("jours")) {
            for (JsonNode creneau : jour.get("creneaux")) {
                tous.add(creneau);
            }
        }
        return tous;
    }

    private static boolean aLAlerte(JsonNode reponse, String code) {
        for (JsonNode alerte : reponse.get("diagnostics").get("alerts")) {
            if (code.equals(alerte.get("code").asText())) {
                return true;
            }
        }
        return false;
    }
}
