package fr.project.planning.fixtures;

import fr.project.planning.domain.reglementaire.RegulatoryParameters;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class TestRegulatoryParametersFactory {

    private TestRegulatoryParametersFactory() {}

    // Defaults stables (V3)
    public static final LocalTime DEFAULT_DEBUT_NUIT = LocalTime.of(22, 0);
    public static final LocalTime DEFAULT_FIN_NUIT = LocalTime.of(6, 0);
    public static final List<LocalDate> DEFAULT_JOURS_FERIES = List.of();

    /** Socle neutre canonique. */
    public static RegulatoryParameters neutre() {
        return new RegulatoryParameters(
                DEFAULT_DEBUT_NUIT,
                DEFAULT_FIN_NUIT,
                DEFAULT_JOURS_FERIES
        );
    }

    /** Variante : plage de nuit custom, pas de jours fériés. */
    public static RegulatoryParameters avecPlageNuit(LocalTime debut, LocalTime fin) {
        return new RegulatoryParameters(
                debut,
                fin,
                DEFAULT_JOURS_FERIES
        );
    }

    /** Variante : jours fériés explicitement fournis, plage de nuit par défaut. */
    public static RegulatoryParameters avecJoursFeries(List<LocalDate> joursFeries) {
        return new RegulatoryParameters(
                DEFAULT_DEBUT_NUIT,
                DEFAULT_FIN_NUIT,
                joursFeries
        );
    }

    /** Variante : paramètres explicitement fournis (complet = complet pour le modèle actuel). */
    public static RegulatoryParameters complet(LocalTime debutNuit, LocalTime finNuit, List<LocalDate> joursFeries) {
        return new RegulatoryParameters(
                debutNuit,
                finNuit,
                joursFeries
        );
    }
}