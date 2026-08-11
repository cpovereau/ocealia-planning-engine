package fr.project.planning.domain.reglementaire;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
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
 * CalendrierJoursFeriesTest — lot S7.9
 *
 * <p>Jusqu'à ce lot, {@code RegulatoryParameters.neutre()} était employé par les trois
 * scénarios : la liste des jours fériés était vide, donc {@code minutesFerie} valait 0 pour tout
 * client, la pénalité férié ne se déclenchait jamais et {@code heuresJourFerie} restait à 0.0
 * dans toutes les réponses. Ces tests couvrent la reconstitution du calendrier.</p>
 */
class CalendrierJoursFeriesTest {

    /** Lundi 11 mai 2026. */
    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);

    @Nested
    @DisplayName("Reconstitution depuis les créneaux (SC-03, SC-06)")
    class Reconstitution {

        @Test
        void aucunCreneauFerie_calendrierVide() {
            Set<LocalDate> dates = CalendrierJoursFeries.declaresParLesCreneaux(
                    List.of(creneau("C-1", LUNDI, false), creneau("C-2", LUNDI.plusDays(1), false)));

            assertTrue(dates.isEmpty());
        }

        @Test
        void unCreneauFerie_saDateEstRetenue() {
            Set<LocalDate> dates = CalendrierJoursFeries.declaresParLesCreneaux(
                    List.of(creneau("C-1", LUNDI, false), creneau("C-2", LUNDI.plusDays(2), true)));

            assertEquals(Set.of(LUNDI.plusDays(2)), dates);
        }

        @Test
        void declarationPartielle_unSeulCreneauSuffitAQualifierLaJournee() {
            // Le férié est une propriété de la date, pas du créneau : s'il est férié pour l'un,
            // il l'est pour tous. Un planning composé de plusieurs sources amont peut ne marquer
            // qu'une partie des créneaux du jour — la journée entière est qualifiée quand même.
            Set<LocalDate> dates = CalendrierJoursFeries.declaresParLesCreneaux(List.of(
                    creneau("C-1", LUNDI, false),
                    creneau("C-2", LUNDI, true),
                    creneau("C-3", LUNDI, false)));

            assertEquals(Set.of(LUNDI), dates);
        }

        @Test
        void plusieursDates_toutesRetenuesSansDoublon() {
            Set<LocalDate> dates = CalendrierJoursFeries.declaresParLesCreneaux(List.of(
                    creneau("C-1", LUNDI, true),
                    creneau("C-2", LUNDI, true),
                    creneau("C-3", LUNDI.plusDays(3), true)));

            assertEquals(Set.of(LUNDI, LUNDI.plusDays(3)), dates);
        }

        @Test
        void creneauSansDate_ignoreSansLeverDException() {
            Set<LocalDate> dates = CalendrierJoursFeries.declaresParLesCreneaux(
                    List.of(creneau("C-1", null, true), creneau("C-2", LUNDI, true)));

            assertEquals(Set.of(LUNDI), dates);
        }

        @Test
        void listeNulle_calendrierVide() {
            assertTrue(CalendrierJoursFeries.declaresParLesCreneaux(null).isEmpty());
        }
    }

    @Nested
    @DisplayName("Paramètres réglementaires assortis du calendrier")
    class Parametres {

        @Test
        void calendrierAlimente_lesDatesSontReconnues() {
            RegulatoryParameters rp = RegulatoryParameters.avecJoursFeries(
                    Set.of(LUNDI, LUNDI.plusDays(2)));

            assertTrue(rp.estJourFerie(LUNDI));
            assertTrue(rp.estJourFerie(LUNDI.plusDays(2)));
            assertFalse(rp.estJourFerie(LUNDI.plusDays(1)));
        }

        @Test
        void plageDeNuitInchangee_seulLeCalendrierEstAjoute() {
            // La plage de nuit reste celle de neutre() : ce lot ne traite que le férié.
            RegulatoryParameters rp = RegulatoryParameters.avecJoursFeries(Set.of(LUNDI));

            assertEquals(LocalTime.of(22, 0), rp.getHeureDebutNuit());
            assertEquals(LocalTime.of(6, 0), rp.getHeureFinNuit());
        }

        @Test
        void calendrierNul_equivautAAucunFerie() {
            RegulatoryParameters rp = RegulatoryParameters.avecJoursFeries(null);

            assertTrue(rp.getJoursFeries().isEmpty());
            assertFalse(rp.estJourFerie(LUNDI));
        }

        @Test
        void neutre_neQualifieAucuneDate() {
            // Le comportement historique, conservé pour les tests unitaires : c'est lui qui
            // rendait la valorisation du férié inopérante lorsqu'il servait en production.
            assertFalse(RegulatoryParameters.neutre().estJourFerie(LUNDI));
        }
    }

    // ---------------------------------------------------------

    private static Creneau creneau(String id, LocalDate date, boolean jourFerie) {
        return new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                "SITE-A", "ACT-SOIN", null, "PC-001",
                PrioriteCreneau.NORMALE, TypeCreneau.IMPOSE, TypePlageHoraire.JOUR,
                jourFerie, QualificationJour.OUVRE);
    }
}
