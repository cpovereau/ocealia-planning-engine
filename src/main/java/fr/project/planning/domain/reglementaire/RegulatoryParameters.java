package fr.project.planning.domain.reglementaire;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public final class RegulatoryParameters {

    private final LocalTime heureDebutNuit;
    private final LocalTime heureFinNuit;
    private final List<LocalDate> joursFeries;

    public RegulatoryParameters(
            LocalTime heureDebutNuit,
            LocalTime heureFinNuit,
            List<LocalDate> joursFeries
    ) {
        this.heureDebutNuit = Objects.requireNonNull(heureDebutNuit);
        this.heureFinNuit = Objects.requireNonNull(heureFinNuit);
        this.joursFeries = List.copyOf(Objects.requireNonNull(joursFeries));
    }

    // Fournit des paramètres réglementaires neutres (ex: pour les tests unitaires)
    public static RegulatoryParameters neutre() {
    return new RegulatoryParameters(
        LocalTime.of(22, 0),
        LocalTime.of(6, 0),
        List.of()
    );
    }

    public LocalTime getHeureDebutNuit() {
        return heureDebutNuit;
    }

    public LocalTime getHeureFinNuit() {
        return heureFinNuit;
    }

    public List<LocalDate> getJoursFeries() {
        return joursFeries;
    }

    public boolean estJourFerie(LocalDate date) {
        return joursFeries.contains(date);
    }
}