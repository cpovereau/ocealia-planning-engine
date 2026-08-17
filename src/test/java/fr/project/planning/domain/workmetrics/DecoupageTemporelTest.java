package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.workmetrics.TrancheTemporelle.Granularite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DecoupageTemporelTest — lot O1 de SC-04.
 *
 * <p>Deux propriétés à ne pas perdre : la semaine commence le <strong>lundi</strong>, comme partout
 * ailleurs dans le moteur ; et les bords sont <strong>tronqués</strong>, jamais étendus — le moteur
 * ne juge pas hors de ce qu'il a reçu.</p>
 */
class DecoupageTemporelTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);

    @Test
    @DisplayName("Une semaine pleine donne une semaine, un mois partiel, et la période")
    void uneSemainePleine() {
        List<TrancheTemporelle> tranches = decouper(LUNDI, LUNDI.plusDays(6));

        assertEquals(3, tranches.size());
        assertEquals(Granularite.SEMAINE, tranches.get(0).granularite());
        assertEquals(Granularite.MOIS, tranches.get(1).granularite());
        assertEquals(Granularite.PERIODE, tranches.get(2).granularite());
    }

    @Test
    @DisplayName("La semaine commence le lundi, comme partout ailleurs dans le moteur")
    void laSemaineCommenceLeLundi() {
        // Départ un mercredi : la première tranche s'arrête au dimanche, pas sept jours plus tard.
        LocalDate mercredi = LUNDI.plusDays(2);
        List<TrancheTemporelle> semaines = semaines(mercredi, mercredi.plusDays(13));

        assertEquals(mercredi, semaines.get(0).debut());
        assertEquals(LUNDI.plusDays(6), semaines.get(0).fin(), "Le dimanche ferme la semaine.");
        assertEquals(LUNDI.plusDays(7), semaines.get(1).debut(), "La suivante repart un lundi.");
    }

    @Test
    @DisplayName("Les bords sont tronqués, jamais étendus")
    void lesBordsSontTronques() {
        LocalDate mercredi = LUNDI.plusDays(2);
        LocalDate fin = LUNDI.plusDays(9); // un mercredi, deux semaines plus loin
        List<TrancheTemporelle> semaines = semaines(mercredi, fin);

        assertEquals(2, semaines.size());
        assertEquals(fin, semaines.get(1).fin(),
                "La dernière semaine s'arrête à l'horizon, pas au dimanche suivant.");
        assertTrue(semaines.get(0).partielle());
        assertTrue(semaines.get(1).partielle());
    }

    @Test
    @DisplayName("Une semaine entière ne se déclare pas partielle")
    void uneSemaineEntiereNEstPasPartielle() {
        assertFalse(semaines(LUNDI, LUNDI.plusDays(6)).get(0).partielle());
    }

    @Test
    @DisplayName("Un horizon à cheval sur deux mois donne deux tranches mensuelles tronquées")
    void unHorizonAChevalSurDeuxMois() {
        List<TrancheTemporelle> mois = mois(LocalDate.of(2026, 5, 20), LocalDate.of(2026, 6, 10));

        assertEquals(2, mois.size());
        assertEquals(LocalDate.of(2026, 5, 31), mois.get(0).fin(), "Mai s'arrête au 31.");
        assertEquals(LocalDate.of(2026, 6, 1), mois.get(1).debut());
        assertEquals(LocalDate.of(2026, 6, 10), mois.get(1).fin(), "Juin s'arrête à l'horizon.");
        assertTrue(mois.get(0).partielle());
        assertTrue(mois.get(1).partielle());
    }

    @Test
    @DisplayName("Un mois entier ne se déclare pas partiel")
    void unMoisEntierNEstPasPartiel() {
        List<TrancheTemporelle> mois = mois(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertEquals(1, mois.size());
        assertFalse(mois.get(0).partielle());
    }

    @Test
    @DisplayName("Les tranches couvrent l'horizon sans trou ni recouvrement, à chaque granularité")
    void lesTranchesCouvrentLHorizonSansTrouNiRecouvrement() {
        LocalDate debut = LocalDate.of(2026, 4, 15);
        LocalDate fin = LocalDate.of(2026, 7, 3);

        for (Granularite granularite : List.of(Granularite.SEMAINE, Granularite.MOIS)) {
            List<TrancheTemporelle> tranches = decouper(debut, fin).stream()
                    .filter(t -> t.granularite() == granularite)
                    .toList();

            assertEquals(debut, tranches.get(0).debut(), granularite + " : premier jour");
            assertEquals(fin, tranches.get(tranches.size() - 1).fin(), granularite + " : dernier jour");

            for (int i = 1; i < tranches.size(); i++) {
                assertEquals(tranches.get(i - 1).fin().plusDays(1), tranches.get(i).debut(),
                        granularite + " : la tranche " + i + " reprend le lendemain de la précédente,"
                                + " sans trou ni recouvrement.");
            }
        }
    }

    @Test
    @DisplayName("Un seul jour reste un seul jour, aux trois granularités")
    void unSeulJourResteUnSeulJour() {
        List<TrancheTemporelle> tranches = decouper(LUNDI, LUNDI);

        assertEquals(3, tranches.size());
        for (TrancheTemporelle tranche : tranches) {
            assertEquals(LUNDI, tranche.debut());
            assertEquals(LUNDI, tranche.fin());
        }
    }

    @Test
    @DisplayName("Sans horizon, aucune tranche — et non une tranche vide")
    void sansHorizonAucuneTranche() {
        // HorizonTemporel garantit lui-même ses deux bornes : il n'y a que son absence à traiter.
        assertTrue(DecoupageTemporel.decouper(null).isEmpty());
    }

    // =====================================================================

    private static List<TrancheTemporelle> decouper(LocalDate debut, LocalDate fin) {
        return DecoupageTemporel.decouper(new HorizonTemporel(debut, fin));
    }

    private static List<TrancheTemporelle> semaines(LocalDate debut, LocalDate fin) {
        return decouper(debut, fin).stream()
                .filter(t -> t.granularite() == Granularite.SEMAINE).toList();
    }

    private static List<TrancheTemporelle> mois(LocalDate debut, LocalDate fin) {
        return decouper(debut, fin).stream()
                .filter(t -> t.granularite() == Granularite.MOIS).toList();
    }
}
