package fr.project.planning.time;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.fixtures.TestCreneauFactory;
import fr.project.planning.fixtures.TestRegulatoryParametersFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeBreakdownCalculatorTest {

    private final TimeBreakdownCalculator calculator = new TimeBreakdownCalculator();

    @Test
    void creneau_18_23_compte_1h_de_nuit_si_nuit_22_06() {
        // 2026-01-05 = lundi (peu importe ici, pas dimanche, pas férié)
        LocalDate date = LocalDate.of(2026, 1, 5);

        Creneau c = TestCreneauFactory.jour(date, 18, 0, 23, 0);
        RegulatoryParameters rp = TestRegulatoryParametersFactory.neutre();

        TimeBreakdown b = calculator.compute(c, rp, true);

        assertEquals(300, b.minutesTravaillees());
        assertEquals(60, b.minutesNuit());

        assertEquals(0, b.minutesDimanche());
        assertEquals(0, b.minutesFerie());

        assertEquals(0, b.minutesNuitEtDimanche());
        assertEquals(0, b.minutesNuitEtFerie());
        assertEquals(0, b.minutesDimancheEtFerie());
        assertEquals(0, b.minutesNuitEtDimancheEtFerie());
    }

    @Test
    void creneau_samedi_22_dimanche_06_compte_dimanche_et_intersection_nuit_dimanche() {
        // 2026-01-10 = samedi
        LocalDate samedi = LocalDate.of(2026, 1, 10);

        Creneau c = TestCreneauFactory.traverseMinuit(samedi, 22, 0, 6, 0);
        RegulatoryParameters rp = TestRegulatoryParametersFactory.neutre(); // nuit 22->06

        TimeBreakdown b = calculator.compute(c, rp, true);

        // total 8h
        assertEquals(480, b.minutesTravaillees());

        // tout le créneau est dans la plage de nuit 22->06
        assertEquals(480, b.minutesNuit());

        // la partie dimanche = 00:00 -> 06:00 = 6h
        assertEquals(360, b.minutesDimanche());

        // intersection nuit+dimanche = dimanche (00->06) car c’est aussi nuit
        assertEquals(360, b.minutesNuitEtDimanche());

        // pas férié
        assertEquals(0, b.minutesFerie());
        assertEquals(0, b.minutesNuitEtFerie());
        assertEquals(0, b.minutesDimancheEtFerie());
        assertEquals(0, b.minutesNuitEtDimancheEtFerie());
    }

    @Test
    void creneau_ferie_00_02_compte_nuit_et_ferie_et_intersection() {
        LocalDate ferien = LocalDate.of(2026, 1, 1); // date arbitraire pour le test

        Creneau c = TestCreneauFactory.jour(ferien, 0, 0, 2, 0);
        RegulatoryParameters rp = TestRegulatoryParametersFactory.avecJoursFeries(
                java.util.List.of(ferien)
        );

        TimeBreakdown b = calculator.compute(c, rp, true);

        assertEquals(120, b.minutesTravaillees());

        // 00:00-02:00 est dans la plage nuit 22:00-06:00
        assertEquals(120, b.minutesNuit());

        // jour férié par RegulatoryParameters (décision B)
        assertEquals(120, b.minutesFerie());

        // intersection complète
        assertEquals(120, b.minutesNuitEtFerie());

        // pas dimanche
        assertEquals(0, b.minutesDimanche());
        assertEquals(0, b.minutesNuitEtDimanche());
        assertEquals(0, b.minutesDimancheEtFerie());
        assertEquals(0, b.minutesNuitEtDimancheEtFerie());
    }

    @Test
    void creneau_samedi_22_dimanche_06_avec_dimanche_ferie_produit_triple_intersection() {
        LocalDate samedi = LocalDate.of(2026, 1, 10);       // samedi
        LocalDate dimanche = samedi.plusDays(1);            // dimanche 2026-01-11

        Creneau c = TestCreneauFactory.traverseMinuit(samedi, 22, 0, 6, 0);

        RegulatoryParameters rp = TestRegulatoryParametersFactory.avecJoursFeries(
                java.util.List.of(dimanche) // seul le dimanche est férié
        );

        TimeBreakdown b = calculator.compute(c, rp, true);

        // total 8h
        assertEquals(480, b.minutesTravaillees());

        // tout le créneau est dans la nuit 22->06
        assertEquals(480, b.minutesNuit());

        // dimanche = 00->06
        assertEquals(360, b.minutesDimanche());

        // férié uniquement sur dimanche
        assertEquals(360, b.minutesFerie());

        // doubles
        assertEquals(360, b.minutesNuitEtDimanche());
        assertEquals(360, b.minutesNuitEtFerie());
        assertEquals(360, b.minutesDimancheEtFerie());

        // triple
        assertEquals(360, b.minutesNuitEtDimancheEtFerie());
    }
}