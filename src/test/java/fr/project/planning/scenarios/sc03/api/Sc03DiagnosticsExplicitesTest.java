package fr.project.planning.scenarios.sc03.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sc03DiagnosticsExplicitesTest — lot S8.4
 *
 * <p>Deux promesses du contrat de réponse n'étaient pas tenues en SC-03 :</p>
 * <ul>
 *   <li>{@code diagnostics.alerts} valait {@code List.of()} en dur. Tout ce que la préparation
 *       constatait — y compris les décisions prises <strong>à la place</strong> de l'appelant —
 *       partait dans un {@code log.warn} qu'il ne lit jamais ;</li>
 *   <li>{@code diagnostics.ignoredCreneaux} ne restituait que trois entiers. Un appelant qui
 *       transmettait quatre-vingts créneaux et en retrouvait soixante-dix-sept savait qu'il en
 *       manquait trois, sans pouvoir dire lesquels.</li>
 * </ul>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class Sc03DiagnosticsExplicitesTest {

    @Autowired
    private MockMvc mockMvc;

    // =========================================================
    // Rang 6 — dire quels créneaux, et s'ils sont écartés
    // =========================================================

    @Test
    @DisplayName("Un créneau hors horizon est nommé, et déclaré écarté")
    void horsHorizon_estNommeEtDeclareEcarte() throws Exception {
        postSc03("sc03_hors_horizon")
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.horsHorizon").value(1))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details.length()").value(1))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[0].creneauId").value("CRE-HH-01"))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[0].date").value("2026-05-25"))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[0].motif").value("HORS_HORIZON"))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[0].exclu").value(true))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[0].message",
                        containsString("2026-05-11")));
    }

    @Test
    @DisplayName("Un créneau à l'activité inconnue est nommé, avec le code en cause")
    void activiteInconnue_estNommeeAvecLeCodeEnCause() throws Exception {
        postSc03("sc03_activite_inconnue")
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.activiteInconnue").value(1))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[0].creneauId").value("CRE-AI-02"))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[0].motif").value("ACTIVITE_INCONNUE"))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[0].exclu").value(true))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[0].message",
                        containsString("ACT-INCONNU")));
    }

    @Test
    @DisplayName("Un créneau sans ressource compatible est signalé, mais déclaré conservé")
    void aucuneRessourceDansDataset_estSignaleMaisConserve() throws Exception {
        // Le nom du bloc laisse croire que tout ce qu'il compte est perdu. Ce compteur-là ne
        // retire rien : le créneau part au solveur, qui rendra visible l'impossible.
        postSc03("sc03_aucune_ressource")
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.aucuneRessourceDansDataset").value(1))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[0].creneauId").value("CRE-AR-02"))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[0].motif")
                        .value("AUCUNE_RESSOURCE_DANS_DATASET"))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[0].exclu").value(false))
                // Et il est bien là dans le planning restitué.
                .andExpect(jsonPath("$.planning.jours[?(@.date=='2026-05-13')].creneaux[0].id")
                        .value("CRE-AR-02"));
    }

    @Test
    @DisplayName("Un repos impossible à rattacher est nommé — il manquera au planning rechargé")
    void reposNonRattache_estNommeCarIlManqueraAuPlanning() throws Exception {
        postSc03("sc03_repos_non_rattache")
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details.length()").value(2))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[?(@.creneauId=='REPOS-ORPHELIN')].motif")
                        .value("MARQUEUR_REPOS_NON_RATTACHE"))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[?(@.creneauId=='REPOS-ORPHELIN')].exclu")
                        .value(true))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details[?(@.creneauId=='REPOS-INCONNU')].message",
                        org.hamcrest.Matchers.hasItem(containsString("SAL-FANTOME"))))
                // Ces deux motifs n'alimentent aucun des trois compteurs historiques.
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.horsHorizon").value(0))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.activiteInconnue").value(0));
    }

    // =========================================================
    // Rang 5 — dire ce qui a été décidé à la place de l'appelant
    // =========================================================

    @Test
    @DisplayName("Une plage de nuit inexploitable est signalée, pas seulement journalisée")
    void plageDeNuitIncomplete_produitUneAlerte() throws Exception {
        // Substituer la plage par défaut à celle qu'il a déclarée déplace le score : le taire
        // n'était pas tenable.
        postSc03("sc03_reglementaire_degrade")
                .andExpect(jsonPath("$.diagnostics.alerts[?(@.code=='PLAGE_NUIT_PAR_DEFAUT')].severity")
                        .value("WARNING"))
                .andExpect(jsonPath("$.diagnostics.alerts[?(@.code=='PLAGE_NUIT_PAR_DEFAUT')].message",
                        org.hamcrest.Matchers.hasItem(containsString("22:00"))));
    }

    @Test
    @DisplayName("Un calendrier de fériés qui écrase le dataset est signalé, avec les dates perdues")
    void calendrierFeriesDivergent_produitUneAlerte() throws Exception {
        postSc03("sc03_reglementaire_degrade")
                .andExpect(jsonPath("$.diagnostics.alerts[?(@.code=='CALENDRIER_FERIES_DIVERGENT')].severity")
                        .value("WARNING"))
                .andExpect(jsonPath("$.diagnostics.alerts[?(@.code=='CALENDRIER_FERIES_DIVERGENT')].message",
                        org.hamcrest.Matchers.hasItem(containsString("2026-05-13"))));
    }

    @Test
    @DisplayName("Des créneaux écartés produisent une alerte qui renvoie au détail")
    void creneauxEcartes_produisentUneAlerteQuiRenvoieAuDetail() throws Exception {
        postSc03("sc03_hors_horizon")
                .andExpect(jsonPath("$.diagnostics.alerts[?(@.code=='CRENEAUX_ECARTES_AVANT_SOLVEUR')].severity")
                        .value("WARNING"))
                .andExpect(jsonPath("$.diagnostics.alerts[?(@.code=='CRENEAUX_ECARTES_AVANT_SOLVEUR')].message",
                        org.hamcrest.Matchers.hasItem(containsString("ignoredCreneaux.details"))));
    }

    // =========================================================
    // Non-régression — le silence reste possible
    // =========================================================

    @Test
    @DisplayName("Le jeu de référence reste sans alerte et sans détail")
    void jeuDeReference_resteSilencieux() throws Exception {
        // Une réponse propre doit le rester : ces deux canaux ne servent qu'à dire ce qui cloche.
        postSc03("sc03_migration_reference")
                .andExpect(jsonPath("$.diagnostics.alerts.length()").value(0))
                .andExpect(jsonPath("$.diagnostics.ignoredCreneaux.details.length()").value(0))
                .andExpect(jsonPath("$.solverResult.score.soft").value(-67440));
    }

    // =========================================================
    // Helper
    // =========================================================

    private org.springframework.test.web.servlet.ResultActions postSc03(String fixture) throws Exception {
        String json = Files.readString(
                Path.of("src/test/resources/scenarios/sc03/" + fixture + ".json"));
        return mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(Objects.requireNonNull(json)))
                .andExpect(status().isOk());
    }
}
