package fr.project.planning.domain.contexte;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * HorizonTemporel
 *
 * Définit la période couverte par la résolution.
 */
public final class HorizonTemporel implements Serializable {

    private final LocalDate dateDebut;
    private final LocalDate dateFin;

    public HorizonTemporel(LocalDate dateDebut, LocalDate dateFin) {
        this.dateDebut = Objects.requireNonNull(dateDebut);
        this.dateFin = Objects.requireNonNull(dateFin);

        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("dateFin must be >= dateDebut");
        }
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public boolean contient(LocalDate date) {
    Objects.requireNonNull(date, "date ne doit pas être null");
    return !date.isBefore(dateDebut) && !date.isAfter(dateFin);
    }

}
