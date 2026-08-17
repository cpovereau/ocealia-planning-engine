package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.ressource.Indisponibilite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JoursDisponiblesTest — rang 14, le calcul pur.
 *
 * <p>La règle tient en une phrase : <strong>une absence déclarée n'est pas du temps disponible</strong>.
 * Ces tests fixent ce qui est déduit, ce qui ne l'est pas, et les trois cas de bord qui feraient
 * silencieusement mentir le compte — le chevauchement, le débordement, et l'absence d'autrui.</p>
 */
class JoursDisponiblesTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 5, 11);
    private static final HorizonTemporel DEUX_SEMAINES =
            new HorizonTemporel(LUNDI, LUNDI.plusDays(13));

    @Test
    @DisplayName("Sans indisponibilité, la fenêtre entière est disponible")
    void sansIndisponibiliteLaFenetreEntiereEstDisponible() {
        assertEquals(14, JoursDisponibles.pour("S1", DEUX_SEMAINES, List.of()));
        assertEquals(14, JoursDisponibles.pour("S1", DEUX_SEMAINES, null));
    }

    @Test
    @DisplayName("Une absence de trois jours en retire trois, bornes comprises")
    void uneAbsenceDeTroisJoursEnRetireTrois() {
        List<Indisponibilite> absences = List.of(
                new Indisponibilite("S1", LUNDI, LUNDI.plusDays(2), "CONGE"));

        assertEquals(11, JoursDisponibles.pour("S1", DEUX_SEMAINES, absences));
    }

    @Test
    @DisplayName("Deux absences qui se chevauchent ne comptent qu'une fois")
    void deuxAbsencesQuiSeChevauchentNeComptentQuUneFois() {
        // Union de 5 jours, non somme de 3 + 3 : sans quoi le compte descendrait à 8.
        List<Indisponibilite> absences = List.of(
                new Indisponibilite("S1", LUNDI, LUNDI.plusDays(2), "CONGE"),
                new Indisponibilite("S1", LUNDI.plusDays(2), LUNDI.plusDays(4), "MALADIE"));

        assertEquals(9, JoursDisponibles.pour("S1", DEUX_SEMAINES, absences));
    }

    @Test
    @DisplayName("Une absence qui déborde la fenêtre n'en retire que la partie visible")
    void uneAbsenceQuiDebordeNeRetireQueLaPartieVisible() {
        // Trois mois d'absence dont l'horizon ne voit que les deux premiers jours.
        List<Indisponibilite> absences = List.of(
                new Indisponibilite("S1", LUNDI.minusMonths(3), LUNDI.plusDays(1), "MALADIE"));

        assertEquals(12, JoursDisponibles.pour("S1", DEUX_SEMAINES, absences));
    }

    @Test
    @DisplayName("L'absence d'un autre salarié ne retire rien")
    void lAbsenceDUnAutreSalarieNeRetireRien() {
        List<Indisponibilite> absences = List.of(
                new Indisponibilite("S2", LUNDI, LUNDI.plusDays(6), "CONGE"));

        assertEquals(14, JoursDisponibles.pour("S1", DEUX_SEMAINES, absences));
    }

    @Test
    @DisplayName("Une absence couvrant toute la fenêtre ne laisse aucun jour disponible")
    void uneAbsenceCouvrantToutLaFenetreNeLaisseAucunJour() {
        List<Indisponibilite> absences = List.of(
                new Indisponibilite("S1", LUNDI.minusDays(5), LUNDI.plusDays(30), "MALADIE"));

        assertEquals(0, JoursDisponibles.pour("S1", DEUX_SEMAINES, absences));
    }

    @Test
    @DisplayName("Une indisponibilité aux dates incomplètes est ignorée plutôt que devinée")
    void uneIndisponibiliteAuxDatesIncompletesEstIgnoree() {
        List<Indisponibilite> absences = List.of(
                new Indisponibilite("S1", null, LUNDI.plusDays(2), "CONGE"),
                new Indisponibilite("S1", LUNDI, null, "CONGE"));

        assertEquals(14, JoursDisponibles.pour("S1", DEUX_SEMAINES, absences));
    }

    @Test
    @DisplayName("Sans identifiant de ressource, rien n'est déduit")
    void sansIdentifiantDeRessourceRienNEstDeduit() {
        List<Indisponibilite> absences = List.of(
                new Indisponibilite("S1", LUNDI, LUNDI.plusDays(6), "CONGE"));

        assertEquals(14, JoursDisponibles.pour(null, DEUX_SEMAINES, absences));
    }
}
