package fr.project.planning.domain.creneau;

import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CodeActiviteTest — lot S7.8
 *
 * <p>La règle de résolution du code activité était réimplémentée à l'identique sur onze sites.
 * Ce test couvre la règle unique, vérifie que les deux porteurs y délèguent, et interdit le
 * retour de la duplication.</p>
 */
class CodeActiviteTest {

    // =====================================================================
    // La règle
    // =====================================================================

    @Nested
    @DisplayName("Règle de résolution")
    class Regle {

        @Test
        void codeActiviteIdRenseigne_estRetenu() {
            assertEquals("ACT-SOIN", CodeActivite.effectif("ACT-SOIN", null));
        }

        @Test
        void codeActiviteIdPrimeSurActivite_memeEnCasDeDiscordance() {
            // Le contrat courant fait autorité ; le libellé hérité est ignoré, pas fusionné.
            assertEquals("ACT-SOIN", CodeActivite.effectif("ACT-SOIN", "ACT-ADMIN"));
        }

        @Test
        void codeActiviteIdAbsent_repliSurActivite() {
            assertEquals("ACT-ADMIN", CodeActivite.effectif(null, "ACT-ADMIN"));
        }

        @Test
        void codeActiviteIdBlanc_vautAbsence_etDeclencheLeRepli() {
            // Une chaîne blanche ne désigne aucune entrée du référentiel : la traiter comme
            // une valeur ferait échouer la jointure au lieu de déclencher le repli.
            assertEquals("ACT-ADMIN", CodeActivite.effectif("   ", "ACT-ADMIN"));
            assertEquals("ACT-ADMIN", CodeActivite.effectif("", "ACT-ADMIN"));
        }

        @Test
        void aucunDesDeuxChamps_donneNull() {
            assertNull(CodeActivite.effectif(null, null));
        }

        @Test
        void activiteBlanche_donneNull_etNonLaChaineVide() {
            // Le null de sortie dit « aucune activité exploitable ». Rendre "" obligerait
            // chaque appelant à refaire un isBlank(), et c'est exactement ce qu'on supprime.
            assertNull(CodeActivite.effectif(null, "  "));
            assertNull(CodeActivite.effectif("  ", "  "));
        }
    }

    // =====================================================================
    // Les deux porteurs délèguent
    // =====================================================================

    @Nested
    @DisplayName("Délégation des porteurs")
    class Delegation {

        @Test
        void creneau_delegueALaRegleCommune() {
            assertEquals("ACT-SOIN", creneau("ACT-SOIN", "ACT-ADMIN").getCodeActiviteEffectif());
            assertEquals("ACT-ADMIN", creneau(null, "ACT-ADMIN").getCodeActiviteEffectif());
            assertEquals("ACT-ADMIN", creneau("  ", "ACT-ADMIN").getCodeActiviteEffectif());
            assertNull(creneau(null, null).getCodeActiviteEffectif());
            assertNull(creneau("", "").getCodeActiviteEffectif());
        }

        @Test
        void creneauInputDto_delegueALaMemeRegle() {
            // Le DTO applique la règle dès la préparation, avant que l'entité n'existe.
            // Les deux porteurs doivent répondre la même chose sur les mêmes entrées.
            assertEquals("ACT-SOIN", dto("ACT-SOIN", "ACT-ADMIN").getCodeActiviteEffectif());
            assertEquals("ACT-ADMIN", dto(null, "ACT-ADMIN").getCodeActiviteEffectif());
            assertEquals("ACT-ADMIN", dto("  ", "ACT-ADMIN").getCodeActiviteEffectif());
            assertNull(dto(null, null).getCodeActiviteEffectif());
            assertNull(dto("", "").getCodeActiviteEffectif());
        }

        private Creneau creneau(String codeActiviteId, String activite) {
            return new Creneau(
                    "C-1", LocalDate.of(2026, 5, 11), LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                    "SITE-A", codeActiviteId, activite, "PC-001",
                    PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                    false, QualificationJour.OUVRE);
        }

        private CreneauInputDTO dto(String codeActiviteId, String activite) {
            CreneauInputDTO d = new CreneauInputDTO();
            d.setCodeActiviteId(codeActiviteId);
            d.setActivite(activite);
            return d;
        }
    }

    // =====================================================================
    // Garde contre le retour de la duplication
    // =====================================================================

    @Nested
    @DisplayName("Unicité de la règle dans le code de production")
    class Unicite {

        /**
         * Signature de la règle écrite en ligne : {@code getCodeActiviteId().isBlank()}.
         *
         * <p>{@link CodeActivite} teste ses paramètres, pas un getter — aucune classe n'est
         * donc légitimement autorisée à correspondre.</p>
         */
        private static final Pattern REGLE_EN_LIGNE =
                Pattern.compile("getCodeActiviteId\\(\\)\\s*\\.isBlank\\(\\)");

        @Test
        void aucuneClasseDeProductionNeReecritLaRegleEnLigne() throws IOException {
            List<Path> fautives;
            try (Stream<Path> sources = Files.walk(Path.of("src/main/java"))) {
                fautives = sources
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(CodeActiviteTest.Unicite::contientLaRegleEnLigne)
                        .toList();
            }

            assertTrue(fautives.isEmpty(),
                    "La règle de résolution du code activité doit vivre dans CodeActivite seule. "
                            + "Ces classes la réécrivent en ligne et divergeront en silence : " + fautives
                            + ". Utiliser getCodeActiviteEffectif() sur le créneau ou le DTO.");
        }

        private static boolean contientLaRegleEnLigne(Path source) {
            try {
                return REGLE_EN_LIGNE.matcher(Files.readString(source, StandardCharsets.UTF_8)).find();
            } catch (IOException e) {
                throw new IllegalStateException("Lecture impossible : " + source, e);
            }
        }
    }
}
