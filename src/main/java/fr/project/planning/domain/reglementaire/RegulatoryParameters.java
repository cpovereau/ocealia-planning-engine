package fr.project.planning.domain.reglementaire;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Collection;
import java.util.Objects;
import java.util.TreeSet;

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

    /**
     * Paramètres de référence assortis d'un calendrier de jours fériés (lot S7.9).
     *
     * <p>Conserve la plage de nuit 22:00–06:00 de {@link #neutre()} et n'ajoute que le
     * calendrier. Jusqu'à ce lot, les trois scénarios appelaient {@code neutre()} : la liste
     * était donc vide et <strong>aucune minute n'était jamais comptée comme fériée</strong>,
     * ni au score ni dans {@code workMetrics.heuresJourFerie}.</p>
     *
     * @param joursFeries dates fériées de la période ; {@code null} est traité comme vide
     */
    public static RegulatoryParameters avecJoursFeries(Collection<LocalDate> joursFeries) {
        return new RegulatoryParameters(
                LocalTime.of(22, 0),
                LocalTime.of(6, 0),
                joursFeries == null ? List.of() : List.copyOf(new TreeSet<>(joursFeries))
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