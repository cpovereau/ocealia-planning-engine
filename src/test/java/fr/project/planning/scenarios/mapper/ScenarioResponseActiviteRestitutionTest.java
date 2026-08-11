package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.scenarios.dto.AssignmentDiagnosticDTO;
import fr.project.planning.scenarios.dto.CreneauPlanningDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Restitution du code activité dans la réponse scénario.
 *
 * Règle contractuelle (§ `50_SCENARIO_RESPONSE_CONTRACT.md` — `planning.jours[].creneaux[].activite`
 * est décrit comme « Code activité ») : {@code codeActiviteId} en priorité, {@code activite}
 * (libellé déprécié) en repli.
 *
 * Avant correction, la restitution lisait directement {@code activite} : un client conforme
 * envoyant uniquement {@code codeActiviteId} — cas nominal SC-03 — recevait {@code null}.
 */
class ScenarioResponseActiviteRestitutionTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 7, 27);

    private final ScenarioResponseMapper mapper = new ScenarioResponseMapper();

    // ----------------------------------------------------------
    // planning.jours[].creneaux[].activite
    // ----------------------------------------------------------

    @Test
    void planning_codeActiviteIdSeul_doitEtreRestitue() {
        ScenarioResponseDTO response = toResponse(creneau("ACT-SOIN", null));

        assertEquals("ACT-SOIN", premierCreneau(response).getActivite());
    }

    @Test
    void planning_libelleSeul_doitEtreRestitueEnRepli() {
        ScenarioResponseDTO response = toResponse(creneau(null, "travail"));

        assertEquals("travail", premierCreneau(response).getActivite());
    }

    @Test
    void planning_lesDeuxRenseignes_doitPrivilegierLeCode() {
        ScenarioResponseDTO response = toResponse(creneau("ACT-SOIN", "Soins infirmiers"));

        assertEquals("ACT-SOIN", premierCreneau(response).getActivite());
    }

    @Test
    void planning_aucunRenseigne_doitRestituerNull() {
        ScenarioResponseDTO response = toResponse(creneau(null, "   "));

        assertNull(premierCreneau(response).getActivite());
    }

    // ----------------------------------------------------------
    // diagnostics.assignments[].activite
    // ----------------------------------------------------------

    @Test
    void diagnostics_codeActiviteIdSeul_doitEtreRestitue() {
        List<AssignmentDiagnosticDTO> diagnostics = AssignmentDiagnosticsFactory.build(
                List.of(creneau("ACT-SOIN", null)),
                Set.of()
        );

        assertEquals(1, diagnostics.size());
        assertEquals("ACT-SOIN", diagnostics.get(0).getActivite());
    }

    // ----------------------------------------------------------
    // helpers
    // ----------------------------------------------------------

    private ScenarioResponseDTO toResponse(Creneau creneau) {
        return mapper.toResponse(
                "SC-03",
                "SOLVED",
                0,
                0,
                List.of(),
                "SAL-2001",
                List.of(creneau),
                Map.of(),
                List.of(),
                Set.of(),
                null
        );
    }

    private CreneauPlanningDTO premierCreneau(ScenarioResponseDTO response) {
        return response.getPlanning().getJours().get(0).getCreneaux().get(0);
    }

    private Creneau creneau(String codeActiviteId, String activite) {
        Creneau creneau = new Creneau(
                "CRE-01",
                JOUR,
                LocalTime.of(7, 0),
                LocalTime.of(15, 0),
                480,
                "HOPITAL-NORD",
                codeActiviteId,
                activite,
                "PC-SOINS",
                null,
                TypeCreneau.IMPOSE,
                TypePlageHoraire.JOUR,
                false,
                QualificationJour.OUVRE
        );
        creneau.setRessourceAffectee(RessourceNonAffectee.INSTANCE);
        return creneau;
    }
}
