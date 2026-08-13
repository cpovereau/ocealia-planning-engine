package fr.project.planning.equite.calibration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.project.planning.domain.contexte.CoefficientsPenibilite;
import fr.project.planning.domain.workmetrics.EcartAuContrat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HarnaisDeCalibrationTest — lot L3 du chantier équité (V5, bout-en-bout).
 *
 * <h3>Ce que ce test garde</h3>
 * <p>Un harnais de calibration ne vaut que par sa fidélité : s'il pondère à sa façon, il calibre
 * quelque chose que le moteur ne calcule pas, et <em>personne ne le verra</em> — le résultat aura
 * exactement la même allure. Deux propriétés sont donc verrouillées ici.</p>
 *
 * <p><strong>La fidélité.</strong> Reconstituée depuis la réponse publiée, puis repondérée par le
 * harnais, la charge doit redonner très exactement les {@code heuresPonderees} et le
 * {@code ecartContratPourcent} que le moteur a produits. La vérification se fait sur une
 * <strong>seule</strong> réponse, à dessein : la résolution se termine sur un budget de temps, et
 * comparer deux résolutions ferait dépendre le test d'une stabilité que le moteur ne promet pas.</p>
 *
 * <p><strong>La licence de rejouer.</strong> Le harnais évalue autant d'échelles qu'on veut à partir
 * d'une seule résolution. Cela n'est légitime que tant que les coefficients ne participent pas au
 * score — sinon changer l'échelle changerait le planning, et rejouer la pondération seule serait
 * faux. Le lot L5 introduira cette contrainte SOFT : ce test est ce qui le signalera.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class HarnaisDeCalibrationTest {

    private static final Path FIXTURES = Path.of("src/test/resources/scenarios/sc03");
    /*
     * Extension .markdown et non .md : CorpusDocumentaireTest tient tout nom en .md pour la
     * citation d'un document du projet et exige qu'il existe. Ce rapport est un artefact de build,
     * pas un document du corpus — il n'a ni index ni durée de vie.
     */
    private static final Path RAPPORT = Path.of("build/rapports/calibration-penibilite.markdown");

    /** Une échelle quelconque mais décroissante le long de la dominance : elle doit être admise. */
    private static final CoefficientsPenibilite ECHELLE_ESSAI =
            new CoefficientsPenibilite(2.2, 1.6, 1.3);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    // ---------------------------------------------------------
    // 1. La fidélité au moteur
    // ---------------------------------------------------------

    @Test
    @DisplayName("Repondérée par le harnais, la charge publiée redonne la mesure du moteur")
    void leHarnaisReproduitExactementLaMesureDuMoteur() throws Exception {
        JsonNode requete = avecEchelle(lire("sc03_migration_reference.json"), ECHELLE_ESSAI);
        JsonNode reponse = solve(requete);

        CasDeCalibration cas = LectureCasDeCalibration.depuis("fidelite", requete, reponse);
        assertTrue(!cas.comparables().isEmpty(),
                "Sans contrat déclaré, ce test ne prouverait rien.");

        for (JsonNode metrique : reponse.get("workMetrics").get("byRessource")) {
            String ressourceId = metrique.get("resourceId").asText();
            ChargeObservee charge = cas.charges().stream()
                    .filter(c -> c.ressourceId().equals(ressourceId))
                    .findFirst().orElseThrow();

            if (!charge.estComparable()) {
                continue;
            }

            double minutesHarnais = charge.minutesPondereesPar(ECHELLE_ESSAI);

            assertEquals(metrique.get("heuresPonderees").asDouble(), minutesHarnais / 60.0, 0.02,
                    ressourceId + " : le harnais et le moteur ne pondèrent pas pareil. Toute "
                            + "calibration conduite avec cet écart porterait sur une mesure que "
                            + "le moteur ne produit pas.");

            assertEquals(metrique.get("ecartContratPourcent").asDouble(),
                    EcartAuContrat.ecartPourcent(minutesHarnais, charge.minutesAttendues()), 0.05,
                    ressourceId + " : l'écart au contrat diverge, donc le classement peut "
                            + "diverger — et c'est le classement qu'on calibre.");
        }
    }

    @Test
    @DisplayName("La répartition reconstituée depuis la réponse rend bien toutes les heures")
    void laRepartitionReconstituee_rendToutesLesHeures() throws Exception {
        JsonNode requete = lire("sc03_migration_reference.json");
        JsonNode reponse = solve(requete);

        CasDeCalibration cas = LectureCasDeCalibration.depuis("reconstitution", requete, reponse);

        for (JsonNode metrique : reponse.get("workMetrics").get("byRessource")) {
            ChargeObservee charge = cas.charges().stream()
                    .filter(c -> c.ressourceId().equals(metrique.get("resourceId").asText()))
                    .findFirst().orElseThrow();

            // Toute minute est quelque part, et nulle part deux fois : c'est ce qui autorise à
            // repondérer sans revenir aux créneaux.
            assertEquals(metrique.get("heuresTravaillees").asDouble() * 60.0,
                    charge.minutesTravaillees(), 1.0,
                    charge.ressourceId() + " : la somme des catégories ne fait pas le total. "
                            + "Le contrat publié ne suffirait alors plus à reconstituer la charge.");
        }
    }

    @Test
    @DisplayName("Le cas muet est reconnu comme tel plutôt que lu comme une indifférence")
    void unCasSansPenibilite_estReconnuMuet() throws Exception {
        // sc06 et sc02 ont leurs propres fixtures ; celle-ci ne porte ni nuit ni dimanche ni férié.
        JsonNode requete = lire("sc03_repos_non_rattache.json");
        CasDeCalibration cas =
                LectureCasDeCalibration.depuis("muet", requete, solve(requete));

        assertTrue(cas.estMuet(),
                "Aucune échelle ne changerait ce classement. Le harnais doit le dire, sinon le "
                        + "lecteur conclurait que les coefficients n'ont pas d'effet.");
    }

    // ---------------------------------------------------------
    // 2. La licence de rejouer sans résoudre à nouveau
    // ---------------------------------------------------------

    @Test
    @DisplayName("Aucune contrainte ne lit les coefficients — c'est ce qui autorise à rejouer")
    void aucuneContrainteNeLitLesCoefficients() {
        List<String> lecteurs = new ArrayList<>();

        for (Path racine : List.of(Path.of("src/main/java/fr/project/planning/constraints"),
                Path.of("src/main/java/fr/project/planning/score"))) {
            try (Stream<Path> fichiers = Files.walk(racine)) {
                fichiers.filter(p -> p.getFileName().toString().endsWith(".java"))
                        .filter(p -> lire(p).contains("CoefficientsPenibilite"))
                        .forEach(p -> lecteurs.add(p.toString()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        assertTrue(lecteurs.isEmpty(),
                "Le harnais évalue toutes les échelles à partir d'une seule résolution : la "
                        + "répartition par dominance ne dépend pas des coefficients, seule leur "
                        + "pondération en dépend.\nDès qu'une contrainte lit les coefficients — le "
                        + "lot L5 le fera — changer l'échelle change le planning, et rejouer la "
                        + "seule pondération devient faux. Le harnais doit alors résoudre par "
                        + "échelle.\nContraintes concernées :\n  - "
                        + String.join("\n  - ", lecteurs));
    }

    // ---------------------------------------------------------
    // 3. Le rapport
    // ---------------------------------------------------------

    @Test
    @DisplayName("Le rapport de calibration est produit sur les jeux du dépôt")
    void leRapportEstProduitSurLesJeuxDuDepot() throws Exception {
        List<CasDeCalibration> cas = new ArrayList<>();

        for (Path fixture : fixtures()) {
            JsonNode requete = lire(fixture.getFileName().toString());
            cas.add(LectureCasDeCalibration.depuis(
                    fixture.getFileName().toString(), requete, solve(requete)));
        }

        String rapport = RapportCalibration.de(cas, CoefficientsPenibilite.neutres());

        Files.createDirectories(RAPPORT.getParent());
        Files.writeString(RAPPORT, rapport, StandardCharsets.UTF_8);

        assertTrue(rapport.contains("## Cas : sc03_migration_reference.json"),
                "Le rapport doit couvrir les jeux du dépôt, sans quoi il ne prouve rien de "
                        + "l'instrument.");

        // Ce que le dépôt contient ne calibre rien, et le rapport doit le dire plutôt que de
        // laisser croire l'inverse. C'est la conclusion du lot, pas un aveu.
        assertTrue(cas.stream().anyMatch(CasDeCalibration::estMuet),
                "Les jeux du dépôt sont des cas de démonstration, pas des plannings réels : "
                        + "au moins l'un d'eux est muet, et le rapport le signale.");
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static List<Path> fixtures() {
        try (Stream<Path> s = Files.list(FIXTURES)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JsonNode lire(String fixture) throws IOException {
        return objectMapper.readTree(Files.readString(FIXTURES.resolve(fixture)));
    }

    private static String lire(Path fichier) {
        try {
            return Files.readString(fichier, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JsonNode avecEchelle(JsonNode requete, CoefficientsPenibilite echelle) {
        ObjectNode coefficients = objectMapper.createObjectNode();
        coefficients.put("nuit", echelle.getNuit());
        coefficients.put("dimanche", echelle.getDimanche());
        coefficients.put("ferie", echelle.getFerie());
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
