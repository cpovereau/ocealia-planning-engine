package fr.project.planning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PlanningSolverApplicationTest — le moteur démarre pour de vrai.
 *
 * <h3>Ce que ce test couvre, et que les 844 autres ne couvraient pas</h3>
 * <p>Toute la suite s'exécute sur {@code TestSpringConfig}, une configuration de test qui scanne
 * {@code fr.project.planning}. Elle prouve que les composants du moteur fonctionnent ensemble ;
 * elle ne prouve pas que <strong>l'application livrée</strong> démarre, puisqu'elle ne passe pas
 * par son point d'entrée. Le seul test qui le faisait vivait dans {@code com.example} et était
 * <em>exclu de la suite</em> : il n'a jamais été joué.</p>
 *
 * <p>C'est précisément le trou qu'a révélé le déménagement du point d'entrée dans
 * {@code fr.project.planning} le 2026-08-18. Ce test le ferme : il démarre le contexte réel, celui
 * du {@code .jar}.</p>
 *
 * <h3>Deux précisions de montage</h3>
 * <p>{@code classes} est explicite parce qu'il y a désormais deux configurations candidates dans
 * ce package — l'application et {@code TestSpringConfig}. Et le répertoire d'écoute est coupé :
 * un test n'a pas à créer des dossiers de travail ni à ramasser ce qui traînerait dans une boîte
 * de dépôt.</p>
 */
@SpringBootTest(
        classes = PlanningSolverApplication.class,
        properties = {"file-adapter.enabled=false", "server.port=0"})
class PlanningSolverApplicationTest {

    @Autowired
    private ApplicationContext contexte;

    @Test
    @DisplayName("Le contexte de l'application livrée démarre")
    void leContexteDemarre() {
        assertNotNull(contexte);
    }

    @Test
    @DisplayName("Le scan par défaut atteint le moteur, sans scanBasePackages")
    void leScanParDefautAtteintLeMoteur() {
        // Le point d'entrée ne déclare plus de scanBasePackages : il n'en a plus besoin depuis
        // qu'il partage la racine du moteur. Si quelqu'un le redéplaçait ailleurs, le contexte
        // démarrerait quand même — vide — et c'est cette panne silencieuse que ce test attrape.
        assertTrue(contexte.containsBean("scenarioController"),
                "Les composants du moteur doivent être découverts par le scan par défaut.");
        assertNotNull(contexte.getBean(fr.project.planning.solver.SolverConfigFactory.class));
    }
}
