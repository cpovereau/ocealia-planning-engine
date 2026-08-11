package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.scenarios.dto.RegulatoryParametersDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ScenarioRegulatoryParametersMapperTest — lot S8.0
 *
 * <p>Point unique où se décide la précédence entre ce que l'appelant déclare et ce que le moteur
 * déduit. Ces règles sont les seules du lot qui puissent rendre un calcul faux sans rien casser,
 * d'où leur couverture au cas par cas.</p>
 */
class ScenarioRegulatoryParametersMapperTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final LocalTime DEFAUT_DEBUT = LocalTime.of(22, 0);
    private static final LocalTime DEFAUT_FIN = LocalTime.of(6, 0);

    private final ScenarioRegulatoryParametersMapper mapper =
            new ScenarioRegulatoryParametersMapper();

    // =====================================================================
    // Plage de nuit
    // =====================================================================

    @Nested
    @DisplayName("Plage de nuit")
    class PlageDeNuit {

        @Test
        void blocAbsent_plageLegaleParDefaut() {
            RegulatoryParameters rp = mapper.toRegulatoryParameters(null, List.of(), "SC-03");

            assertEquals(DEFAUT_DEBUT, rp.getHeureDebutNuit());
            assertEquals(DEFAUT_FIN, rp.getHeureFinNuit());
        }

        @Test
        void deuxBornesDeclarees_sontAppliquees() {
            // 21:00–06:00 : la plage du Code du travail, que le moteur ne savait pas exprimer.
            RegulatoryParameters rp = mapper.toRegulatoryParameters(
                    bloc(LocalTime.of(21, 0), LocalTime.of(6, 0), null), List.of(), "SC-03");

            assertEquals(LocalTime.of(21, 0), rp.getHeureDebutNuit());
            assertEquals(LocalTime.of(6, 0), rp.getHeureFinNuit());
        }

        @Test
        void uneSeuleBorne_estIgnoree_etLaPlageParDefautSApplique() {
            // Mélanger une borne déclarée et une borne par défaut produirait un intervalle
            // que personne n'a voulu : 21:00–06:00 alors que le client n'a dit que « 21:00 ».
            RegulatoryParameters debutSeul = mapper.toRegulatoryParameters(
                    bloc(LocalTime.of(21, 0), null, null), List.of(), "SC-03");
            RegulatoryParameters finSeule = mapper.toRegulatoryParameters(
                    bloc(null, LocalTime.of(7, 0), null), List.of(), "SC-03");

            assertEquals(DEFAUT_DEBUT, debutSeul.getHeureDebutNuit());
            assertEquals(DEFAUT_FIN, debutSeul.getHeureFinNuit());
            assertEquals(DEFAUT_DEBUT, finSeule.getHeureDebutNuit());
            assertEquals(DEFAUT_FIN, finSeule.getHeureFinNuit());
        }

        @Test
        void bornesIdentiques_plageVide_donneLaPlageParDefaut() {
            RegulatoryParameters rp = mapper.toRegulatoryParameters(
                    bloc(LocalTime.of(22, 0), LocalTime.of(22, 0), null), List.of(), "SC-03");

            assertEquals(DEFAUT_DEBUT, rp.getHeureDebutNuit());
            assertEquals(DEFAUT_FIN, rp.getHeureFinNuit());
        }
    }

    // =====================================================================
    // Jours fériés
    // =====================================================================

    @Nested
    @DisplayName("Jours fériés")
    class JoursFeries {

        @Test
        void calendrierAbsent_leMoteurDeduit() {
            RegulatoryParameters rp = mapper.toRegulatoryParameters(
                    bloc(null, null, null), Set.of(LUNDI), "SC-03");

            assertTrue(rp.estJourFerie(LUNDI));
        }

        @Test
        void blocEntierementAbsent_leMoteurDeduit() {
            RegulatoryParameters rp = mapper.toRegulatoryParameters(null, Set.of(LUNDI), "SC-03");

            assertTrue(rp.estJourFerie(LUNDI));
        }

        @Test
        void calendrierDeclare_faitAutorite() {
            RegulatoryParameters rp = mapper.toRegulatoryParameters(
                    bloc(null, null, List.of(LUNDI.plusDays(3))), Set.of(LUNDI), "SC-03");

            assertTrue(rp.estJourFerie(LUNDI.plusDays(3)));
            assertFalse(rp.estJourFerie(LUNDI),
                    "Le calendrier déclaré fait autorité : jamais de fusion silencieuse avec "
                            + "ce que le dataset déclare");
        }

        @Test
        void calendrierDeclareVide_ditQuIlNYADeFerie() {
            // Une liste vide est une déclaration, pas une absence : le moteur ne déduit plus.
            RegulatoryParameters rp = mapper.toRegulatoryParameters(
                    bloc(null, null, List.of()), Set.of(LUNDI), "SC-03");

            assertFalse(rp.estJourFerie(LUNDI));
            assertTrue(rp.getJoursFeries().isEmpty());
        }

        @Test
        void calendrierDeclare_dedoublonneEtTrie() {
            RegulatoryParameters rp = mapper.toRegulatoryParameters(
                    bloc(null, null, List.of(LUNDI.plusDays(3), LUNDI, LUNDI.plusDays(3))),
                    List.of(), "SC-03");

            assertEquals(List.of(LUNDI, LUNDI.plusDays(3)), rp.getJoursFeries());
        }

        @Test
        void datesNulles_sontIgnorees() {
            List<LocalDate> avecNull = new java.util.ArrayList<>();
            avecNull.add(LUNDI);
            avecNull.add(null);

            RegulatoryParameters rp = mapper.toRegulatoryParameters(
                    bloc(null, null, avecNull), List.of(), "SC-03");

            assertEquals(List.of(LUNDI), rp.getJoursFeries());
        }
    }

    // ---------------------------------------------------------

    private static RegulatoryParametersDTO bloc(LocalTime debut, LocalTime fin,
                                                List<LocalDate> joursFeries) {
        RegulatoryParametersDTO dto = new RegulatoryParametersDTO();
        dto.setHeureDebutNuit(debut);
        dto.setHeureFinNuit(fin);
        dto.setJoursFeries(joursFeries);
        return dto;
    }
}
