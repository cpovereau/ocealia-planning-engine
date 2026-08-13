package fr.project.planning.scenarios.sc05;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.scenarios.dto.Sc05ScenarioRequestDTO;
import fr.project.planning.scenarios.service.PreparedSc05Scenario;
import fr.project.planning.scenarios.service.ScenarioSc05PreparationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ScenarioSc05PreparationServiceTest — lot A2, l'état du problème au sortir de la préparation.
 *
 * <h3>Pourquoi un test à ce niveau, et pas seulement bout-en-bout</h3>
 * <p>Le lot A2 reconstitue l'état d'<strong>avant</strong> en réaffectant les créneaux libérés le
 * temps de la mesure, puis défait cette réaffectation. Sur le jeu d'essai du projet, oublier de la
 * défaire ne change <em>aucune</em> réponse : le solveur retrouve la même répartition. Un test
 * bout-en-bout ne discriminerait donc pas cette ligne, et la croire couverte serait pire que de la
 * savoir découverte.</p>
 *
 * <p>Ce que la préparation doit garantir se constate ici, et nulle part ailleurs : le problème remis
 * au solveur est <strong>exactement</strong> celui que le lot A1 définit — mesurer ne le modifie
 * pas.</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
class ScenarioSc05PreparationServiceTest {

    private static final Path ARBITRAGE = Path.of("src/test/resources/scenarios/sc05/sc05_arbitrage.json");

    private static final String CRENEAU_ARBITRE = "PLN-ARB-V";
    private static final String CRENEAU_DU_TIERS = "PLN-ARB-TIERS";

    @Autowired
    private ScenarioSc05PreparationService preparationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Le créneau libéré part au solveur sans affectation : mesurer ne modifie pas le problème")
    void leCreneauLibere_partSansAffectation() throws Exception {
        PreparedSc05Scenario prepared = preparationService.prepare(lire());

        Creneau libere = creneau(prepared, CRENEAU_ARBITRE);
        assertNull(libere.getRessourceAffectee(),
                "La mesure de l'avant a réaffecté ce créneau et ne l'a pas défait : le solveur "
                        + "recevrait un point de départ qui dépend d'un calcul de restitution.");
        assertTrue(!libere.isFige(), "Un créneau remis en jeu n'est pas figé.");
    }

    @Test
    @DisplayName("Le créneau du tiers part figé sur lui, et l'état d'avant est retenu")
    void leCreneauDuTiers_partFigeSurLui() throws Exception {
        PreparedSc05Scenario prepared = preparationService.prepare(lire());

        Creneau tiers = creneau(prepared, CRENEAU_DU_TIERS);
        assertTrue(tiers.isFige());
        assertEquals("SAL-TIERS", tiers.getRessourceAffectee().getId());
        assertTrue(prepared.creneauxTenusParUnTiers().contains(CRENEAU_DU_TIERS));
    }

    @Test
    @DisplayName("L'état d'avant est conservé pour les deux créneaux du périmètre")
    void lEtatDAvant_estConserve() throws Exception {
        PreparedSc05Scenario prepared = preparationService.prepare(lire());

        assertEquals("SAL-A", prepared.ressourceAvantParCreneau().get(CRENEAU_ARBITRE),
                "Sans cette carte, le titulaire d'avant serait perdu au moment de le libérer.");
        assertEquals("SAL-TIERS", prepared.ressourceAvantParCreneau().get(CRENEAU_DU_TIERS));
    }

    @Test
    @DisplayName("Les métriques d'avant comptent le créneau libéré chez son titulaire d'origine")
    void lesMetriquesDAvant_comptentLeCreneauLibere() throws Exception {
        // 40 h et non 32 h : c'est ce qui distingue une mesure de l'avant d'une mesure du problème
        // tel qu'il part au solveur, où le créneau libéré n'est à personne.
        PreparedSc05Scenario prepared = preparationService.prepare(lire());

        assertNotNull(prepared.metriquesAvant().get("SAL-A"));
        assertEquals(40 * 60, prepared.metriquesAvant().get("SAL-A").getMinutesTravaillees());
        assertEquals(24 * 60, prepared.metriquesAvant().get("SAL-B").getMinutesTravaillees());
    }

    @Test
    @DisplayName("Hors du périmètre, l'état d'avant n'est pas retenu — il n'a pas à l'être")
    void horsDuPerimetre_lEtatDAvantNEstPasRetenu() throws Exception {
        // Ces créneaux sont épinglés : ils portent leur titulaire, et le dupliquer dans une carte
        // ouvrirait deux sources pour la même vérité.
        PreparedSc05Scenario prepared = preparationService.prepare(lire());

        assertNull(prepared.ressourceAvantParCreneau().get("PLN-A-L"));
        assertEquals("SAL-A", creneau(prepared, "PLN-A-L").getRessourceAffectee().getId());
    }

    private static Creneau creneau(PreparedSc05Scenario prepared, String id) {
        return prepared.planningRequest().creneaux().stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Créneau '" + id + "' absent du problème."));
    }

    private Sc05ScenarioRequestDTO lire() throws Exception {
        return objectMapper.readValue(Files.readString(ARBITRAGE), Sc05ScenarioRequestDTO.class);
    }
}
