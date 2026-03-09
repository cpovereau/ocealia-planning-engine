package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.scenarios.dto.AssignmentDiagnosticDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class AssignmentDiagnosticsFactory {

    private AssignmentDiagnosticsFactory() {
    }

    public static List<AssignmentDiagnosticDTO> build(
            List<Creneau> creneauxResolus,
            Set<String> posteVirtuelIds
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
                String status;
                String reason;
                String message;

                if (!hasRealAssignment) {
                    status = "IMPOSSIBLE_TO_ASSIGN";
                    reason = "NO_COMPATIBLE_RESOURCE";
                    message = "Aucune ressource compatible avec ce créneau";
                } else {
                    status = "UNCOVERED";
                    reason = "NO_RESOURCE_ASSIGNED";
                    message = "Créneau non couvert par le solveur";
                }

                diagnostics.add(new AssignmentDiagnosticDTO(
                        creneau.getId(),
                        creneau.getDate() != null ? creneau.getDate().toString() : null,
                        creneau.getHeureDebut() != null ? creneau.getHeureDebut().toString() : null,
                        creneau.getHeureFin() != null ? creneau.getHeureFin().toString() : null,
                        creneau.getActivite() != null ? creneau.getActivite() : null,
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
                        creneau.getActivite() != null ? creneau.getActivite() : null,
                        "VIRTUAL_ASSIGNED",
                        "POSTE_VIRTUEL_ASSIGNED",
                        "Créneau affecté à un poste virtuel"
                ));
            }
        }

        return diagnostics;
    }
}