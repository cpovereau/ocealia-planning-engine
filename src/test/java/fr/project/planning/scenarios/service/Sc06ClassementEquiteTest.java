package fr.project.planning.scenarios.service;

import fr.project.planning.scenarios.dto.NatureCandidat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Sc06ClassementEquiteTest — lot L4 du chantier équité (V1, comparateur pur).
 *
 * <h3>Ce que ces tests fixent</h3>
 * <p>L'ordre des paliers <strong>est</strong> l'arbitrage. Le déplacer d'un rang change qui le
 * moteur recommande, sans changer une seule valeur mesurée — et rien ne le signalerait. Chaque
 * test ci-dessous isole un palier en rendant tous les précédents ex æquo, et vérifie qu'il tranche
 * bien dans le sens voulu par le métier.</p>
 *
 * <h3>Les deux placements qui ne vont pas de soi</h3>
 * <ul>
 *   <li><strong>L'écart au contrat passe devant le score SOFT.</strong> Derrière, il n'aurait
 *       quasiment jamais servi : le score départage presque toujours.</li>
 *   <li><strong>Les jours consécutifs passent devant l'écart.</strong> L'aptitude prime sur le
 *       partage — un écart favorable ne doit pas emporter un sixième jour d'affilée.</li>
 * </ul>
 */
class Sc06ClassementEquiteTest {

    // ---------------------------------------------------------
    // Palier 5 — l'aptitude
    // ---------------------------------------------------------

    @Test
    @DisplayName("À égalité par ailleurs, celui dont la série est la plus courte passe devant")
    void laSerieLaPlusCourtePasseDevant() {
        assertEquals(List.of("courte", "longue"), classer(
                candidat("longue", 5, -40.0, 0, 0),
                candidat("courte", 2, -40.0, 0, 0)));
    }

    @Test
    @DisplayName("L'aptitude prime sur le partage : la série l'emporte sur un écart plus favorable")
    void laSerieLemporteSurUnEcartPlusFavorable() {
        // Celui qui enchaîne cinq jours est pourtant le moins servi — de loin. Le faire revenir
        // n'est pas un arbitrage qu'un écart favorable doit pouvoir emporter.
        assertEquals(List.of("repose", "enchaine"), classer(
                candidat("enchaine", 5, -70.0, 0, 0),
                candidat("repose", 2, -10.0, 0, 0)));
    }

    // ---------------------------------------------------------
    // Palier 6 — l'équité
    // ---------------------------------------------------------

    @Test
    @DisplayName("À série égale, le moins servi passe devant — et l'écart est signé")
    void leMoinsServiPasseDevant() {
        assertEquals(List.of("sousCharge", "equilibre", "surCharge"), classer(
                candidat("surCharge", 2, 12.0, 0, 0),
                candidat("equilibre", 2, 0.0, 0, 0),
                candidat("sousCharge", 2, -25.0, 0, 0)));
    }

    @Test
    @DisplayName("L'écart passe devant le score SOFT — sinon il n'aurait jamais servi")
    void lEcartPasseDevantLeScoreSoft() {
        // Le score SOFT désigne l'autre, et de loin. Placé derrière lui, l'écart au contrat
        // n'aurait quasiment jamais départagé quoi que ce soit : le score tranche presque
        // toujours le premier.
        assertEquals(List.of("moinsServi", "meilleurScore"), classer(
                candidat("meilleurScore", 2, -5.0, 0, 0),
                candidat("moinsServi", 2, -30.0, -5_000, 0)));
    }

    @Test
    @DisplayName("Sans contrat déclaré, le candidat est classé en dernier de ce palier")
    void sansContrat_leCandidatEstClasseEnDernier() {
        // Pas de valeur inventée, et pas de bénéfice du doute non plus : à information égale par
        // ailleurs, on préfère la personne dont on peut mesurer l'impact.
        assertEquals(List.of("mesurable", "inconnu"), classer(
                candidat("inconnu", 2, null, 0, 0),
                candidat("mesurable", 2, 15.0, 0, 0)));
    }

    // ---------------------------------------------------------
    // Palier 8 — le confort
    // ---------------------------------------------------------

    @Test
    @DisplayName("Dernier départage : l'amplitude après affectation, la plus faible d'abord")
    void lAmplitudeApresDepartageEnDernier() {
        assertEquals(List.of("courteJournee", "longueJournee"), classer(
                candidat("longueJournee", 2, -20.0, 0, 13 * 60),
                candidat("courteJournee", 2, -20.0, 0, 9 * 60)));
    }

    // ---------------------------------------------------------
    // Les paliers antérieurs gardent la main
    // ---------------------------------------------------------

    @Test
    @DisplayName("Les paliers antérieurs ne se laissent pas emporter par l'équité")
    void lesPaliersAnterieursGardentLaMain() {
        // Un candidat non conforme reste dernier, aussi favorable que soient sa série et son
        // écart : on ne recommande jamais l'illégal en tête.
        assertEquals(List.of("conforme", "nonConforme"), classer(
                nomme("nonConforme", false, true, 0, 1, -80.0),
                nomme("conforme", true, true, 0, 4, 10.0)));
    }

    @Test
    @DisplayName("Rappeler quelqu'un sur son repos reste pire qu'un écart défavorable")
    void lePalierDuRappelResteDevant() {
        assertEquals(List.of("enPoste", "rappele"), classer(
                nomme("rappele", true, true, 1, 1, -80.0),
                nomme("enPoste", true, true, 0, 4, 10.0)));
    }

    // =========================================================
    // Helpers — le nom du candidat voyage dans la clé des affectations
    // =========================================================

    private static List<String> classer(Candidat... candidats) {
        List<Candidat> liste = new ArrayList<>(List.of(candidats));
        liste.sort(Sc06CandidatEnumerationService.COMPARATEUR);
        return liste.stream().map(Sc06ClassementEquiteTest::nomDe).toList();
    }

    private static String nomDe(Candidat candidat) {
        return candidat.affectations().keySet().iterator().next();
    }

    private static Candidat candidat(String nom, int joursConsecutifs, Double ecartContratPourcent,
                                     int softScore, int amplitudeApresMinutes) {
        return new Candidat(NatureCandidat.MONO_RESSOURCE, Collections.singletonMap(nom, null), true, true, 0,
                joursConsecutifs, ecartContratPourcent, softScore, amplitudeApresMinutes, List.of());
    }

    private static Candidat nomme(String nom, boolean conforme, boolean couvertureComplete,
                                  int rappelees, int joursConsecutifs, Double ecart) {
        return new Candidat(NatureCandidat.MONO_RESSOURCE, Collections.singletonMap(nom, null), conforme,
                couvertureComplete, rappelees, joursConsecutifs, ecart, 0, 0, List.of());
    }
}
