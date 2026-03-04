package fr.project.planning.fixtures;

import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class TestRegulatoryParametersFactory {

    private TestRegulatoryParametersFactory() {}

    public static RegulatoryParameters neutre() {
        return new RegulatoryParameters(
            LocalTime.of(22, 0),   // debut nuit
            LocalTime.of(6, 0),    // fin nuit
            List.of()              // jours fériés
        );
    }

    public static RegulatoryParameters avecPlageNuit(
            LocalTime debut,
            LocalTime fin
    ) {
        return new RegulatoryParameters(
            debut,
            fin,
            List.of()
        );
    }

    public static RegulatoryParameters avecJoursFeries(
            List<LocalDate> joursFeries
    ) {
        return new RegulatoryParameters(
            LocalTime.of(22, 0),
            LocalTime.of(6, 0),
            joursFeries
        );
    }

    public static RegulatoryParameters complet(
        LocalTime debutNuit,
        LocalTime finNuit,
        List<LocalDate> joursFeries
    ) {
    return new RegulatoryParameters(
            debutNuit,
            finNuit,
            joursFeries
    );
    }
}
