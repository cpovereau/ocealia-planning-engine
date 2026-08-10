package fr.project.planning.scenarios.sc06.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScenarioSc06ImpactsTest — lot S5
 *
 * <p>Vérifie le bloc {@code candidats[].impacts[]} sur le jeu de référence, dont toutes les
 * valeurs sont calculables à la main.</p>
 *
 * <p>Besoin : mercredi 13 mai, 14:00 → 22:00, soit 8 h.</p>
 *
 * <pre>
 * SAL-2001  lundi 8 h · mardi 8 h · mercredi 08:00-12:00 (4 h)   → semaine 20 h
 * SAL-2002  lundi 8 h · mardi 8 h                                 → semaine 16 h
 * SAL-2003  jeudi 04:00-12:00 (8 h)                               → semaine  8 h
 * </pre>
 *
 * <p>Le cas le plus parlant est celui du rang 1 : son amplitude du mercredi passe de 4 h à 14 h,
 * puisque sa journée s'étirerait de 08:00 à 22:00. C'est exactement l'information attendue —
 * « l'impact sur l'amplitude journalière pour la personne retenue ».</p>
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class ScenarioSc06ImpactsTest {

    private static final Path REFERENCE = Path.of("src/test/resources/scenarios/sc06/sc06_reference.json");
    private static final Path REPLI = Path.of("src/test/resources/scenarios/sc06/sc06_repli.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rangUn_amplitudeJournaliereEtirree_de4hA14h() throws Exception {
        postSc06(REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidats[0].impacts.length()").value(1))
                .andExpect(jsonPath("$.candidats[0].impacts[0].ressourceId").value("SAL-2001"))

                // Journée du mercredi : 08:00-12:00 seul, puis 08:00 → 22:00 avec le besoin
                .andExpect(jsonPath("$.candidats[0].impacts[0].amplitudeJournaliere.avant").value(4.0))
                .andExpect(jsonPath("$.candidats[0].impacts[0].amplitudeJournaliere.apres").value(14.0))
                .andExpect(jsonPath("$.candidats[0].impacts[0].amplitudeJournaliere.delta").value(10.0))
                .andExpect(jsonPath("$.candidats[0].impacts[0].amplitudeJournaliere.plafond").value(14.0))
                .andExpect(jsonPath("$.candidats[0].impacts[0].amplitudeJournaliere.depassement").value(false))

                // Heures travaillées du jour : 4 h + 8 h
                .andExpect(jsonPath("$.candidats[0].impacts[0].heuresJour.avant").value(4.0))
                .andExpect(jsonPath("$.candidats[0].impacts[0].heuresJour.apres").value(12.0))

                // Heures de la semaine : 20 h + 8 h
                .andExpect(jsonPath("$.candidats[0].impacts[0].heuresSemaine.avant").value(20.0))
                .andExpect(jsonPath("$.candidats[0].impacts[0].heuresSemaine.apres").value(28.0))
                .andExpect(jsonPath("$.candidats[0].impacts[0].heuresSemaine.delta").value(8.0))
                .andExpect(jsonPath("$.candidats[0].impacts[0].heuresSemaine.plafond").value(44.0))
                .andExpect(jsonPath("$.candidats[0].impacts[0].heuresSemaine.depassement").value(false))

                .andExpect(jsonPath("$.candidats[0].impacts[0].heuresHabituellesSemaine").value(35.0));
    }

    @Test
    void plafondNonDeclare_resteNul_etNeDeclencheAucunDepassement() throws Exception {
        // heuresMaximumParJour n'est pas transmis dans le jeu de référence.
        // Une limite absente n'est pas une limite à zéro : plafond null, depassement false.
        postSc06(REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidats[0].impacts[0].heuresJour.plafond").doesNotExist())
                .andExpect(jsonPath("$.candidats[0].impacts[0].heuresJour.depassement").value(false));
    }

    @Test
    void rangDeux_journeeEntierementNouvelle() throws Exception {
        // SAL-2002 ne travaille pas le mercredi : tout part de zéro ce jour-là,
        // mais sa semaine est déjà à 16 h.
        postSc06(REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidats[1].impacts[0].ressourceId").value("SAL-2002"))
                .andExpect(jsonPath("$.candidats[1].impacts[0].amplitudeJournaliere.avant").value(0.0))
                .andExpect(jsonPath("$.candidats[1].impacts[0].amplitudeJournaliere.apres").value(8.0))
                .andExpect(jsonPath("$.candidats[1].impacts[0].heuresJour.avant").value(0.0))
                .andExpect(jsonPath("$.candidats[1].impacts[0].heuresJour.apres").value(8.0))
                .andExpect(jsonPath("$.candidats[1].impacts[0].heuresSemaine.avant").value(16.0))
                .andExpect(jsonPath("$.candidats[1].impacts[0].heuresSemaine.apres").value(24.0));
    }

    @Test
    void candidatNonConforme_porteAussiSesImpacts() throws Exception {
        // Une solution écartée doit rester lisible : c'est ce qui permet de comprendre
        // pourquoi elle a été écartée plutôt que de la voir disparaître.
        postSc06(REFERENCE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidats[2].conforme").value(false))
                .andExpect(jsonPath("$.candidats[2].impacts[0].ressourceId").value("SAL-2003"))
                .andExpect(jsonPath("$.candidats[2].impacts[0].heuresSemaine.avant").value(8.0))
                .andExpect(jsonPath("$.candidats[2].impacts[0].heuresSemaine.apres").value(16.0));
    }

    @Test
    void solutionSurPosteVirtuel_naAucunImpact() throws Exception {
        // Un poste virtuel ne porte ni contrat ni contraintes individuelles :
        // mesurer son amplitude n'aurait aucun sens. Le tableau vide est la bonne réponse.
        postSc06(REPLI)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidats[0].nature").value("RESSOURCE_A_POURVOIR"))
                .andExpect(jsonPath("$.candidats[0].impacts").isArray())
                .andExpect(jsonPath("$.candidats[0].impacts.length()").value(0));
    }

    @Test
    void solutionComposee_porteUnImpactParPersonneMobilisee() throws Exception {
        postSc06(Path.of("src/test/resources/scenarios/sc06/sc06_composee.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidats[0].nature").value("COMPOSEE"))
                .andExpect(jsonPath("$.candidats[0].impacts.length()").value(2));
    }

    // =========================================================
    // Helpers
    // =========================================================

    private ResultActions postSc06(Path fixture) throws Exception {
        return mockMvc.perform(post("/scenarios/sc06/solve")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(Objects.requireNonNull(Files.readString(fixture))));
    }
}
