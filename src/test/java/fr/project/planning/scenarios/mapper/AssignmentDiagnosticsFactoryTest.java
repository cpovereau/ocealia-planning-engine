package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.PrioriteCreneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scenarios.dto.AssignmentDiagnosticDTO;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AssignmentDiagnosticsFactoryTest — Phase 8.
 *
 * Vérifie le diagnostic enrichi pour les créneaux non couverts sur jours fériés.
 * JOUR_FERIE_NON_COUVERT doit être produit en lieu et place de NO_RESOURCE_ASSIGNED
 * ou NO_COMPATIBLE_RESOURCE quand le créneau est un jour férié.
 */
class AssignmentDiagnosticsFactoryTest {

    private static final Set<String> PAS_DE_POSTE_VIRTUEL = Set.of();

    // ----------------------------------------------------------
    // 1. Créneau férié non couvert (solveur a trouvé d'autres affectations)
    //    → UNCOVERED / JOUR_FERIE_NON_COUVERT
    // ----------------------------------------------------------

    @Test
    void creneauFerie_nonCouvert_avecAutresAffectations_doitProduire_JOUR_FERIE_NON_COUVERT() {
        // Un créneau affecté à un salarié réel (pour hasRealAssignment = true)
        SalarieReel salarieAffecte = salarie("SAL-A");
        Creneau creneauCouvert = creneau("C-COUVERT", LocalDate.of(2026, 5, 12), false, salarieAffecte);

        // Le créneau férié non couvert
        Creneau creneauFerieNonCouvert = creneau("C-FERIE-NC", LocalDate.of(2026, 5, 13), true, RessourceNonAffectee.INSTANCE);

        List<AssignmentDiagnosticDTO> diagnostics = AssignmentDiagnosticsFactory.build(
                List.of(creneauCouvert, creneauFerieNonCouvert),
                PAS_DE_POSTE_VIRTUEL
        );

        AssignmentDiagnosticDTO diag = diagnostics.stream()
                .filter(d -> "C-FERIE-NC".equals(d.getCreneauId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Diagnostic attendu absent"));

        assertEquals("UNCOVERED", diag.getStatus());
        assertEquals("JOUR_FERIE_NON_COUVERT", diag.getReasonCode());
        assertTrue(diag.getMessage().contains("férié"), "Le message doit mentionner 'férié'");
    }

    // ----------------------------------------------------------
    // 2. Créneau ordinaire non couvert (avec autres affectations)
    //    → UNCOVERED / NO_RESOURCE_ASSIGNED (comportement inchangé)
    // ----------------------------------------------------------

    @Test
    void creneauOrdinaire_nonCouvert_avecAutresAffectations_doitConserver_NO_RESOURCE_ASSIGNED() {
        SalarieReel salarieAffecte = salarie("SAL-B");
        Creneau creneauCouvert = creneau("C-COUVERT-2", LocalDate.of(2026, 5, 12), false, salarieAffecte);
        Creneau creneauNonCouvert = creneau("C-NC-ORDINAIRE", LocalDate.of(2026, 5, 14), false, RessourceNonAffectee.INSTANCE);

        List<AssignmentDiagnosticDTO> diagnostics = AssignmentDiagnosticsFactory.build(
                List.of(creneauCouvert, creneauNonCouvert),
                PAS_DE_POSTE_VIRTUEL
        );

        AssignmentDiagnosticDTO diag = diagnostics.stream()
                .filter(d -> "C-NC-ORDINAIRE".equals(d.getCreneauId()))
                .findFirst()
                .orElseThrow();

        assertEquals("UNCOVERED", diag.getStatus());
        assertEquals("NO_RESOURCE_ASSIGNED", diag.getReasonCode());
    }

    // ----------------------------------------------------------
    // 3. Créneau férié impossible à affecter (aucune affectation réelle)
    //    → IMPOSSIBLE_TO_ASSIGN / JOUR_FERIE_NON_COUVERT
    // ----------------------------------------------------------

    @Test
    void creneauFerie_impossible_sansAucuneAffectation_doitProduire_JOUR_FERIE_NON_COUVERT() {
        Creneau creneauFerie = creneau("C-FERIE-IMP", LocalDate.of(2026, 5, 13), true, RessourceNonAffectee.INSTANCE);

        List<AssignmentDiagnosticDTO> diagnostics = AssignmentDiagnosticsFactory.build(
                List.of(creneauFerie),
                PAS_DE_POSTE_VIRTUEL
        );

        assertEquals(1, diagnostics.size());
        AssignmentDiagnosticDTO diag = diagnostics.get(0);

        assertEquals("IMPOSSIBLE_TO_ASSIGN", diag.getStatus());
        assertEquals("JOUR_FERIE_NON_COUVERT", diag.getReasonCode());
    }

    // ----------------------------------------------------------
    // 4. Créneau ordinaire impossible à affecter
    //    → IMPOSSIBLE_TO_ASSIGN / NO_COMPATIBLE_RESOURCE (inchangé)
    // ----------------------------------------------------------

    @Test
    void creneauOrdinaire_impossible_doitConserver_NO_COMPATIBLE_RESOURCE() {
        Creneau creneauImpossible = creneau("C-IMP-ORDINAIRE", LocalDate.of(2026, 5, 14), false, RessourceNonAffectee.INSTANCE);

        List<AssignmentDiagnosticDTO> diagnostics = AssignmentDiagnosticsFactory.build(
                List.of(creneauImpossible),
                PAS_DE_POSTE_VIRTUEL
        );

        assertEquals(1, diagnostics.size());
        assertEquals("NO_COMPATIBLE_RESOURCE", diagnostics.get(0).getReasonCode());
    }

    // ----------------------------------------------------------
    // 5. [Lot S8.4] Le férié se lit au calendrier réglementaire
    //
    //    Les libellés de diagnostic étaient le dernier endroit du moteur à interroger le drapeau
    //    isJourFerie du créneau, quand la contrainte HARD et la valorisation lisaient déjà le
    //    calendrier. Un appelant qui déclare `regulatoryParameters.joursFeries` sans marquer ses
    //    créneaux perdait l'explication au moment où elle comptait le plus.
    // ----------------------------------------------------------

    @Test
    void dateFerieeAuCalendrier_sansDrapeau_doitProduire_JOUR_FERIE_NON_COUVERT() {
        LocalDate ferie = LocalDate.of(2026, 5, 13);
        Creneau creneauCouvert = creneau("C-COUVERT", LocalDate.of(2026, 5, 12), false, salarie("SAL-A"));
        Creneau nonCouvert = creneau("C-NC", ferie, false, RessourceNonAffectee.INSTANCE);

        List<AssignmentDiagnosticDTO> diagnostics = AssignmentDiagnosticsFactory.build(
                List.of(creneauCouvert, nonCouvert),
                PAS_DE_POSTE_VIRTUEL,
                new RegulatoryParameters(LocalTime.of(22, 0), LocalTime.of(6, 0), List.of(ferie))
        );

        AssignmentDiagnosticDTO diag = diagnostics.stream()
                .filter(d -> "C-NC".equals(d.getCreneauId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Diagnostic attendu absent"));

        assertEquals("JOUR_FERIE_NON_COUVERT", diag.getReasonCode(),
                "Le calendrier déclaré fait foi, même sans drapeau sur le créneau.");
    }

    @Test
    void drapeauSurLeCreneau_maisDateAbsenteDuCalendrier_doitProduire_NO_RESOURCE_ASSIGNED() {
        // Réciproque : dès qu'un calendrier est connu, c'est lui qui répond — même lecture que
        // JourFerieRefuse, sans quoi le diagnostic contredirait la contrainte.
        Creneau creneauCouvert = creneau("C-COUVERT", LocalDate.of(2026, 5, 12), false, salarie("SAL-A"));
        Creneau nonCouvert = creneau("C-NC", LocalDate.of(2026, 5, 13), true, RessourceNonAffectee.INSTANCE);

        List<AssignmentDiagnosticDTO> diagnostics = AssignmentDiagnosticsFactory.build(
                List.of(creneauCouvert, nonCouvert),
                PAS_DE_POSTE_VIRTUEL,
                RegulatoryParameters.neutre()
        );

        AssignmentDiagnosticDTO diag = diagnostics.stream()
                .filter(d -> "C-NC".equals(d.getCreneauId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Diagnostic attendu absent"));

        assertEquals("NO_RESOURCE_ASSIGNED", diag.getReasonCode());
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    private SalarieReel salarie(String id) {
        return new SalarieReel(id, null, "CDI", Set.of(), Set.of(), Set.of());
    }

    private Creneau creneau(String id, LocalDate date, boolean jourFerie, fr.project.planning.domain.ressource.Ressource ressource) {
        Creneau c = new Creneau(
                id, date, LocalTime.of(8, 0), LocalTime.of(16, 0), 480,
                "SITE-A", null, "travail", "PC-001",
                PrioriteCreneau.NORMALE, TypeCreneau.GENERE, TypePlageHoraire.JOUR,
                jourFerie, jourFerie ? QualificationJour.FERIE : QualificationJour.OUVRE
        );
        c.setRessourceAffectee(ressource);
        return c;
    }
}
