package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.reglementaire.CalendrierJoursFeries;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.scenarios.dto.AssignmentDiagnosticDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class AssignmentDiagnosticsFactory {

    private AssignmentDiagnosticsFactory() {
    }

    /** Sans cadre réglementaire : le férié retombe sur le drapeau porté par le créneau. */
    public static List<AssignmentDiagnosticDTO> build(
            List<Creneau> creneauxResolus,
            Set<String> posteVirtuelIds
    ) {
        return build(creneauxResolus, posteVirtuelIds, null);
    }

    /**
     * @param regulatoryParameters [S8.4] calendrier des fériés retenu pour la résolution.
     *        Les libellés de diagnostic étaient le dernier endroit à lire {@code isJourFerie} au
     *        lieu du calendrier : un créneau non couvert sur une date fériée déclarée au contrat,
     *        mais non marquée créneau par créneau, était restitué {@code NO_RESOURCE_ASSIGNED} —
     *        l'appelant perdait l'explication au moment où elle comptait le plus.
     *        {@code null} conserve l'ancienne lecture.
     */
    public static List<AssignmentDiagnosticDTO> build(
            List<Creneau> creneauxResolus,
            Set<String> posteVirtuelIds,
            RegulatoryParameters regulatoryParameters
    ) {
        List<AssignmentDiagnosticDTO> diagnostics = new ArrayList<>();

        boolean hasRealAssignment = creneauxResolus.stream()
                .map(Creneau::getRessourceAffectee)
                .filter(Objects::nonNull)
                .filter(r -> !(r instanceof RessourceNonAffectee))
                .anyMatch(r -> !posteVirtuelIds.contains(r.getId()));

        for (Creneau creneau : creneauxResolus) {
            if (creneau.getRessourceAffectee() == null) {
                continue;
            }

            if (creneau.getRessourceAffectee() instanceof RessourceNonAffectee) {
                boolean ferie = estFerie(creneau, regulatoryParameters);
                String status;
                String reason;
                String message;

                if (!hasRealAssignment) {
                    status = "IMPOSSIBLE_TO_ASSIGN";
                    reason = ferie
                            ? "JOUR_FERIE_NON_COUVERT"
                            : "NO_COMPATIBLE_RESOURCE";
                    message = ferie
                            ? "Créneau sur jour férié — aucune ressource compatible"
                            : "Aucune ressource compatible avec ce créneau";
                } else {
                    status = "UNCOVERED";
                    reason = ferie
                            ? "JOUR_FERIE_NON_COUVERT"
                            : "NO_RESOURCE_ASSIGNED";
                    message = ferie
                            ? "Créneau sur jour férié non couvert par le solveur"
                            : "Créneau non couvert par le solveur";
                }

                diagnostics.add(new AssignmentDiagnosticDTO(
                        creneau.getId(),
                        creneau.getDate() != null ? creneau.getDate().toString() : null,
                        creneau.getHeureDebut() != null ? creneau.getHeureDebut().toString() : null,
                        creneau.getHeureFin() != null ? creneau.getHeureFin().toString() : null,
                        creneau.getCodeActiviteEffectif(),
                        status,
                        reason,
                        message
                ));
                continue;
            }

            String ressourceId = creneau.getRessourceAffectee().getId();

            if (posteVirtuelIds.contains(ressourceId)) {
                diagnostics.add(new AssignmentDiagnosticDTO(
                        creneau.getId(),
                        creneau.getDate() != null ? creneau.getDate().toString() : null,
                        creneau.getHeureDebut() != null ? creneau.getHeureDebut().toString() : null,
                        creneau.getHeureFin() != null ? creneau.getHeureFin().toString() : null,
                        creneau.getCodeActiviteEffectif(),
                        "VIRTUAL_ASSIGNED",
                        "POSTE_VIRTUEL_ASSIGNED",
                        "Créneau affecté à un poste virtuel"
                ));
            }
        }

        return diagnostics;
    }

    /**
     * Même lecture du férié que la contrainte HARD et que la valorisation — le calendrier
     * réglementaire, et non le drapeau du créneau, dès que le calendrier est connu.
     */
    private static boolean estFerie(Creneau creneau, RegulatoryParameters regulatoryParameters) {
        if (regulatoryParameters == null || creneau.getDate() == null) {
            return creneau.isJourFerie();
        }
        return CalendrierJoursFeries.toucheUnJourFerie(creneau, regulatoryParameters);
    }
}