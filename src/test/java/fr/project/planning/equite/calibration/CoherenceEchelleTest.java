package fr.project.planning.equite.calibration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.project.planning.domain.contexte.CoefficientsPenibilite;
import fr.project.planning.domain.contexte.DominancePenibilites;
import fr.project.planning.score.ScoreWeights;
import fr.project.planning.scoring.PenaliteKey;
import fr.project.planning.scoring.PenibiliteType;
import fr.project.planning.scoring.StrategieScoring;
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
 * CoherenceEchelleTest — lot L3 du chantier équité.
 *
 * <h3>Les deux règles qui se rencontrent ici</h3>
 * <p>La <strong>dominance</strong> décide à quelle catégorie appartient une minute qui en cumule
 * plusieurs ; les <strong>coefficients</strong> décident ce que cette catégorie pèse. Rien
 * n'oblige les deux à s'accorder, et le cadrage §4.2 le signalait comme le risque du chantier :
 * <em>ne pas créer une seconde échelle qui contredise la première</em>.</p>
 *
 * <p>Quand elles se contredisent, le résultat n'est pas faux — il est absurde. Une minute qui cumule
 * deux pénibilités pèse alors <strong>moins</strong> qu'une minute qui n'en porte qu'une : cumuler
 * devient un avantage. C'est le genre de défaut qui ne se voit pas dans un chiffre, seulement dans
 * un classement que personne ne comprend.</p>
 *
 * <p>D'où la condition que toute calibration doit respecter : <strong>les coefficients décroissent
 * le long de l'ordre de dominance</strong>. Le moteur ne refuse pas pour autant — l'échelle décrit
 * quelque chose, elle se contredit seulement elle-même — il la produit et dit ce qu'elle vaut.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class CoherenceEchelleTest {

    private static final Path REFERENCE =
            Path.of("src/test/resources/scenarios/sc03/sc03_migration_reference.json");

    /** L'ordre appliqué à toutes les demandes : aucun scénario n'en transmet d'autre. */
    private static final DominancePenibilites DOMINANCE = DominancePenibilites.parDefaut();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    // ---------------------------------------------------------
    // La condition
    // ---------------------------------------------------------

    @Test
    @DisplayName("Une échelle décroissante le long de la dominance est cohérente")
    void uneEchelleDecroissante_estCoherente() {
        assertTrue(new CoefficientsPenibilite(2.0, 1.5, 1.2).inversionsSelon(DOMINANCE).isEmpty());
        assertTrue(CoefficientsPenibilite.neutres().inversionsSelon(DOMINANCE).isEmpty(),
                "L'échelle neutre est plate : elle ne peut contredire aucun ordre.");
    }

    @Test
    @DisplayName("Un férié plus lourd qu'un dimanche rend le cumul des deux avantageux")
    void unFeriePlusLourdQuUnDimanche_rendLeCumulAvantageux() {
        // Une minute travaillée un dimanche férié est attribuée à DIMANCHE par la dominance, et
        // pèserait 1,2 — quand la même minute un férié ordinaire pèserait 1,8.
        List<CoefficientsPenibilite.Inversion> inversions =
                new CoefficientsPenibilite(2.0, 1.2, 1.8).inversionsSelon(DOMINANCE);

        assertEquals(1, inversions.size());
        assertEquals(PenibiliteType.DIMANCHE, inversions.get(0).dominante());
        assertEquals(PenibiliteType.FERIE, inversions.get(0).absorbee());
    }

    @Test
    @DisplayName("Une nuit plus légère que le reste contredit la dominance deux fois")
    void uneNuitPlusLegereQueLeReste_contreditLaDominanceDeuxFois() {
        // La nuit domine tout : si elle pèse le moins, elle absorbe les deux autres en les allégeant.
        List<CoefficientsPenibilite.Inversion> inversions =
                new CoefficientsPenibilite(1.0, 1.5, 1.2).inversionsSelon(DOMINANCE);

        assertEquals(2, inversions.size());
        assertTrue(inversions.stream().allMatch(i -> i.dominante() == PenibiliteType.NUIT));
    }

    // ---------------------------------------------------------
    // L'autre échelle, celle que le score porte déjà
    // ---------------------------------------------------------

    @Test
    @DisplayName("Le score paie le moins cher la pénibilité qui domine — l'ordre sert les deux lectures")
    void leScorePaieLeMoinsCherLaPenibiliteQuiDomine() {
        /*
         * [Équité L3] Le cadrage §4.2 met en garde : le moteur porte déjà une échelle de
         * pénibilité, et l'équité ne doit pas en créer une seconde qui la contredise.
         *
         * Cette échelle-là est dans ScoreWeights, à la minute : en EXPLOITATION, nuit 3,
         * dimanche 4, férié 5. Elle classe donc la nuit comme la MOINS coûteuse — l'inverse de
         * l'ordre de pénibilité du métier. Les deux tiennent ensemble parce qu'elles lisent la
         * même liste de dominance dans des sens opposés : le score y prend « la situation la plus
         * favorable au salarié » (40_STRATEGIE_DE_SCORING.md §4.1.1), l'équité y prend la
         * pénibilité la plus lourde.
         *
         * L'accord est donc une coïncidence entretenue, pas une propriété. Réordonner la liste
         * pour servir l'une casserait l'autre, en silence. Ce test est ce qui le dira.
         */
        ScoreWeights poids = new ScoreWeights();
        List<String> ecarts = new ArrayList<>();

        for (StrategieScoring strategie : StrategieScoring.values()) {
            List<PenibiliteType> ordre = DOMINANCE.getOrdreDominance();
            for (int rang = 1; rang < ordre.size(); rang++) {
                int dominant = poids.getWeight(strategie, cle(ordre.get(rang - 1)));
                int suivant = poids.getWeight(strategie, cle(ordre.get(rang)));
                if (dominant > suivant) {
                    ecarts.add(strategie + " : " + ordre.get(rang - 1) + " coûte " + dominant
                            + " et domine " + ordre.get(rang) + " qui coûte " + suivant);
                }
            }
        }

        assertTrue(ecarts.isEmpty(),
                "La dominance attribue une minute cumulée à la première catégorie de l'ordre. "
                        + "Pour le score, cela ne reste « le plus favorable au salarié » que si "
                        + "cette catégorie est la moins chère.\nSi cet ordre change, il change "
                        + "aussi pour la mesure d'équité, qui le lit à l'envers — et l'une des "
                        + "deux lectures devient fausse sans que rien ne l'indique.\n  - "
                        + String.join("\n  - ", ecarts));
    }

    private static PenaliteKey cle(PenibiliteType type) {
        return switch (type) {
            case NUIT -> PenaliteKey.LEGAL_SOFT_TRAVAIL_NUIT_MINUTES;
            case DIMANCHE -> PenaliteKey.LEGAL_SOFT_TRAVAIL_DIMANCHE_MINUTES;
            case FERIE -> PenaliteKey.LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES;
        };
    }

    // ---------------------------------------------------------
    // Ce que le moteur en dit
    // ---------------------------------------------------------

    @Test
    @DisplayName("Une échelle incohérente est signalée, et la réponse est produite quand même")
    void uneEchelleIncoherente_estSignaleeSansEtreRefusee() throws Exception {
        JsonNode reponse = solve(avecCoefficients(2.0, 1.2, 1.8));

        JsonNode alerte = alerte(reponse, "COEFFICIENTS_PENIBILITE_INCOHERENTS");
        assertTrue(alerte != null,
                "Le défaut ne se voit dans aucun chiffre : sans alerte, il se lirait comme un "
                        + "classement surprenant, jamais comme une erreur d'échelle.");
        assertEquals("WARNING", alerte.get("severity").asText(),
                "L'échelle décrit quelque chose, elle se contredit seulement elle-même. Le moteur "
                        + "rend visible, il ne refuse pas.");
        assertTrue(alerte.get("message").asText().contains("DIMANCHE"),
                "Le message doit nommer le couple fautif : « incohérent » ne se corrige pas.");
    }

    @Test
    @DisplayName("Une échelle cohérente ne déclenche rien")
    void uneEchelleCoherente_neDeclencheRien() throws Exception {
        assertTrue(alerte(solve(avecCoefficients(2.0, 1.5, 1.2)),
                        "COEFFICIENTS_PENIBILITE_INCOHERENTS") == null,
                "Une alerte qui se lève sur des configurations valides use le canal qui devra "
                        + "porter les vraies.");
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static JsonNode alerte(JsonNode reponse, String code) {
        for (JsonNode alerte : reponse.get("diagnostics").get("alerts")) {
            if (code.equals(alerte.get("code").asText())) {
                return alerte;
            }
        }
        return null;
    }

    private JsonNode avecCoefficients(double nuit, double dimanche, double ferie) throws Exception {
        ObjectNode requete = (ObjectNode) objectMapper.readTree(Files.readString(REFERENCE));
        ObjectNode coefficients = objectMapper.createObjectNode();
        coefficients.put("nuit", nuit);
        coefficients.put("dimanche", dimanche);
        coefficients.put("ferie", ferie);
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
