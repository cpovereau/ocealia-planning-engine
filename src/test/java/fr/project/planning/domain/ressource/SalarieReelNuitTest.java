package fr.project.planning.domain.ressource;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Phase 6 — méthodes utilitaires nuit de SalarieReel.
 *
 * Vérifie :
 * - estTravailleurDeNuit() pour les 3 valeurs possibles (permanent, occasionnel, null)
 * - heureDebutNuitEffective() : priorité salarié-spécifique puis fallback global
 * - heureFinNuitEffective() : priorité salarié-spécifique puis fallback global
 * - non-interférence : les méthodes utilitaires ne modifient pas l'état du salarié
 *
 * Ces méthodes préparent les diagnostics Phase 8 sans activer de règles solveur.
 * Le calcul global RegulatoryParameters reste inchangé.
 */
class SalarieReelNuitTest {

    private static final LocalTime FALLBACK_DEBUT = LocalTime.of(22, 0);
    private static final LocalTime FALLBACK_FIN   = LocalTime.of(6, 0);

    // ----------------------------------------------------------
    // estTravailleurDeNuit()
    // ----------------------------------------------------------

    @Test
    void estTravailleurDeNuit_permanent_retourneTrue() {
        SalarieReel salarie = salarie("SAL-001", "permanent", null, null);
        assertTrue(salarie.estTravailleurDeNuit());
    }

    @Test
    void estTravailleurDeNuit_occasionnel_retourneTrue() {
        SalarieReel salarie = salarie("SAL-002", "occasionnel", null, null);
        assertTrue(salarie.estTravailleurDeNuit());
    }

    @Test
    void estTravailleurDeNuit_null_retourneFalse() {
        SalarieReel salarie = salarie("SAL-003", null, null, null);
        assertFalse(salarie.estTravailleurDeNuit());
    }

    @Test
    void estTravailleurDeNuit_valeurInconnue_retourneFalse() {
        SalarieReel salarie = salarie("SAL-004", "diurne", null, null);
        assertFalse(salarie.estTravailleurDeNuit());
    }

    // ----------------------------------------------------------
    // heureDebutNuitEffective()
    // ----------------------------------------------------------

    @Test
    void heureDebutNuitEffective_avecHeureSalarie_retourneHeureSalarie() {
        LocalTime heureSpecifique = LocalTime.of(21, 0); // différente du fallback
        SalarieReel salarie = salarie("SAL-005", "permanent", heureSpecifique, LocalTime.of(5, 0));

        LocalTime effective = salarie.heureDebutNuitEffective(FALLBACK_DEBUT);

        assertEquals(heureSpecifique, effective);
    }

    @Test
    void heureDebutNuitEffective_sansHeureSalarie_retourneFallback() {
        SalarieReel salarie = salarie("SAL-006", null, null, null);

        LocalTime effective = salarie.heureDebutNuitEffective(FALLBACK_DEBUT);

        assertEquals(FALLBACK_DEBUT, effective);
    }

    // ----------------------------------------------------------
    // heureFinNuitEffective()
    // ----------------------------------------------------------

    @Test
    void heureFinNuitEffective_avecHeureSalarie_retourneHeureSalarie() {
        LocalTime heureSpecifique = LocalTime.of(5, 0); // différente du fallback
        SalarieReel salarie = salarie("SAL-007", "occasionnel", LocalTime.of(22, 0), heureSpecifique);

        LocalTime effective = salarie.heureFinNuitEffective(FALLBACK_FIN);

        assertEquals(heureSpecifique, effective);
    }

    @Test
    void heureFinNuitEffective_sansHeureSalarie_retourneFallback() {
        SalarieReel salarie = salarie("SAL-008", null, null, null);

        LocalTime effective = salarie.heureFinNuitEffective(FALLBACK_FIN);

        assertEquals(FALLBACK_FIN, effective);
    }

    // ----------------------------------------------------------
    // Cohérence : les deux méthodes ensemble
    // ----------------------------------------------------------

    @Test
    void heuresEffectives_salarieSansHeures_retournentFallbacksGlobaux() {
        // SAL-2001 dans le dataset SC-03 : pas de nuit, heures nulles
        SalarieReel salarie = salarie("SAL-2001", null, null, null);

        assertEquals(FALLBACK_DEBUT, salarie.heureDebutNuitEffective(FALLBACK_DEBUT));
        assertEquals(FALLBACK_FIN,   salarie.heureFinNuitEffective(FALLBACK_FIN));
    }

    @Test
    void heuresEffectives_salarieAvecHeures_retournentHeuresSalarieSpecifiques() {
        // SAL-2002 dans le dataset SC-03 : nuit occasionnel, 22:00-06:00
        LocalTime debutSpecifique = LocalTime.of(22, 0);
        LocalTime finSpecifique   = LocalTime.of(6, 0);
        SalarieReel salarie = salarie("SAL-2002", "occasionnel", debutSpecifique, finSpecifique);

        assertEquals(debutSpecifique, salarie.heureDebutNuitEffective(LocalTime.of(23, 0)));
        assertEquals(finSpecifique,   salarie.heureFinNuitEffective(LocalTime.of(7, 0)));
    }

    // ----------------------------------------------------------
    // Non-régression : les méthodes utilitaires ne modifient pas l'état
    // ----------------------------------------------------------

    @Test
    void methodesUtilitaires_neModifientPasEtat() {
        LocalTime debut = LocalTime.of(22, 0);
        LocalTime fin   = LocalTime.of(6, 0);
        SalarieReel salarie = salarie("SAL-009", "permanent", debut, fin);

        // appels multiples — l'état doit rester stable
        salarie.heureDebutNuitEffective(FALLBACK_DEBUT);
        salarie.heureFinNuitEffective(FALLBACK_FIN);
        salarie.estTravailleurDeNuit();

        assertEquals(debut, salarie.getHeureDebutNuit());
        assertEquals(fin,   salarie.getHeureFinNuit());
        assertEquals("permanent", salarie.getTravailDeNuit());
    }

    // ----------------------------------------------------------
    // Utilitaire
    // ----------------------------------------------------------

    private SalarieReel salarie(String id, String travailDeNuit,
                                LocalTime heureDebutNuit, LocalTime heureFinNuit) {
        SalarieReel s = new SalarieReel(id, null, "CDI", Set.of(), Set.of(), Set.of());
        s.setTravailDeNuit(travailDeNuit);
        s.setHeureDebutNuit(heureDebutNuit);
        s.setHeureFinNuit(heureFinNuit);
        return s;
    }
}
