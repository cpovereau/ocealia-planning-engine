package fr.project.planning.scenarios.sc02.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioSc02EquiteTest — lot L5 du chantier équité (V5, bout-en-bout).
 *
 * <h3>Pourquoi SC-02, et pas SC-06</h3>
 * <p>SC-06 énumère et classe : ses paliers portent l'équité depuis le lot L4. <strong>SC-02 n'a
 * aucun comparateur</strong> — il libère les créneaux de l'absent, épingle le reste, et laisse le
 * solveur décider. C'est donc par le <em>score</em> que l'équité l'atteint, et c'est ici qu'on le
 * vérifie.</p>
 *
 * <h3>Le cas</h3>
 * <pre>
 * SAL-ABSENT   vendredi 8 h                                   → absent, son créneau est libéré
 * SAL-CHARGE   lundi · mardi · mercredi · jeudi, 8 h chacun    → 32 h dans la semaine
 * SAL-DISPO    lundi 8 h                                       → 8 h dans la semaine
 * </pre>
 *
 * <p>Les deux peuvent reprendre le vendredi, et <strong>rien d'autre ne les départage</strong> :
 * mêmes bornes, même activité, même lieu, aucune pénibilité en jeu. Avant ce lot, le moteur
 * choisissait donc l'un ou l'autre indifféremment — les deux plannings ont exactement le même
 * score. C'est le défaut que le cadrage relevait : <em>un planning où l'un fait 48 h et l'autre
 * 25 h obtient le même score qu'un planning à 35 h chacun</em>.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioSc02EquiteTest {

    private static final Path EQUITE = Path.of("src/test/resources/scenarios/sc02/sc02_equite.json");

    private static final String CRENEAU_LIBERE = "PLN-ABSENT-V";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Sans tolérance déclarée, l'équité ne pèse rien du tout")
    void sansTolerance_lEquiteNePeseRien() throws Exception {
        // Le test le plus important du lot. Le score doit rester exactement celui d'avant : la
        // contrainte est livrée avant que les coefficients qui la pondèrent ne soient calibrés,
        // et rien ne doit changer pour les appelants qui ne demandent rien.
        JsonNode breakdown = solve(lire()).get("solverResult").get("scoreBreakdown");

        assertTrue(impact(breakdown, "METIER_SOFT_EQUITE_ECART_CONTRAT") == null,
                "Une contrainte qui pèse sans qu'on l'ait demandée déplacerait tous les plannings "
                        + "déjà livrés, sans que personne ne l'ait vu venir.");
        assertTrue(impact(breakdown, "METIER_SOFT_EQUITE_SANS_AFFECTATION") == null);
    }

    @Test
    @DisplayName("Avec une tolérance, le créneau libéré revient au moins servi")
    void avecUneTolerance_leCreneauRevientAuMoinsServi() throws Exception {
        // Confier le vendredi à SAL-CHARGE le porterait à +14,29 % et laisserait SAL-DISPO à
        // −77,14 %. Le confier à SAL-DISPO ramène l'un à −8,57 % et l'autre à −54,29 % : c'est
        // la répartition la moins inéquitable, et le score doit maintenant le dire.
        assertEquals("SAL-DISPO", repreneurDuVendredi(solve(avecTolerance(10.0))),
                "Sans la contrainte, les deux affectations ont exactement le même score et le "
                        + "moteur choisit indifféremment.");
    }

    @Test
    @DisplayName("L'écart pèse le montant annoncé, et il est lisible au scoreBreakdown")
    void lEcartPeseLeMontantAnnonce() throws Exception {
        // SAL-CHARGE à 32 h : −8,57 %, dans la tolérance, rien à payer.
        // SAL-DISPO à 16 h : −54,29 %, soit 44 points au-delà des 10 tolérés, à 10 le point.
        // SAL-ABSENT ne travaille plus rien : −100 %, soit 90 points, au second volet.
        JsonNode breakdown = solve(avecTolerance(10.0)).get("solverResult").get("scoreBreakdown");

        assertEquals(440, impact(breakdown, "METIER_SOFT_EQUITE_ECART_CONTRAT"),
                "Sans cette vérification, rien ne prouverait que le montant est celui qu'on croit.");
        assertEquals(900, impact(breakdown, "METIER_SOFT_EQUITE_SANS_AFFECTATION"),
                "L'absent ne travaille plus rien : le second volet le voit, là où aucune jointure "
                        + "ne le verrait.");
    }

    @Test
    @DisplayName("Une tolérance large ne pèse plus rien : c'est l'encadrement qui décide du seuil")
    void uneToleranceLarge_nePesePlusRien() throws Exception {
        JsonNode breakdown = solve(avecTolerance(100.0)).get("solverResult").get("scoreBreakdown");

        assertTrue(impact(breakdown, "METIER_SOFT_EQUITE_ECART_CONTRAT") == null,
                "Tolérer cent points, c'est ne rien juger inéquitable — et le moteur ne doit pas "
                        + "en juger autrement.");
    }

    @Test
    @DisplayName("Une tolérance négative est refusée : une tolérance décrit une marge")
    void uneToleranceNegative_estRefusee() throws Exception {
        mockMvc.perform(post("/scenarios/sc02/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(avecTolerance(-5.0))))
                .andExpect(status().isUnprocessableEntity());
    }

    // =========================================================
    // Helpers
    // =========================================================

    /** Impact pondéré d'une clé, en valeur absolue, ou {@code null} si la clé est absente. */
    private static Integer impact(JsonNode breakdown, String penaliteKey) {
        for (JsonNode item : breakdown) {
            if (penaliteKey.equals(item.get("penaliteKey").asText())) {
                return Math.abs(item.get("weightedImpact").asInt());
            }
        }
        return null;
    }

    private static String repreneurDuVendredi(JsonNode reponse) {
        for (JsonNode detail : reponse.get("remplacement").get("details")) {
            if (CRENEAU_LIBERE.equals(detail.get("creneauId").asText())) {
                JsonNode repreneur = detail.get("ressourceApresId");
                return repreneur == null || repreneur.isNull() ? "PERSONNE" : repreneur.asText();
            }
        }
        throw new AssertionError("Le créneau libéré ne figure pas dans le remplacement.");
    }

    private JsonNode lire() throws Exception {
        return objectMapper.readTree(Files.readString(EQUITE));
    }

    private JsonNode avecTolerance(double ecartTolerePourcent) throws Exception {
        ObjectNode requete = (ObjectNode) lire();
        ObjectNode equite = objectMapper.createObjectNode();
        equite.put("ecartTolerePourcent", ecartTolerePourcent);
        ((ObjectNode) requete.get("planningContext")).set("equite", equite);
        return requete;
    }

    private JsonNode solve(JsonNode requete) throws Exception {
        String reponse = mockMvc.perform(post("/scenarios/sc02/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(reponse);
    }
}
