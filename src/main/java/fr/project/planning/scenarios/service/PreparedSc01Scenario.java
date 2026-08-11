package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.scenarios.builder.ScenarioDatasetBuilderSc01;
import fr.project.planning.scenarios.dto.IgnoredCreneauxDTO;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;

import java.util.List;
import java.util.Set;

/**
 * PreparedSc01Scenario — objet de transport interne.
 *
 * Contient toutes les données préparées par ScenarioSc01PreparationService
 * avant l'appel au solveur. Évite de repasser par le DTO dans la couche exécution.
 *
 * Phase C — ignoredCreneaux : compteurs diagnostiques calculés lors de la préparation.
 * Lot S8.4 — alerts : celles du builder et celles du cadre réglementaire, réunies. Le second
 * n'en produisait aucune et décidait pourtant, seul, de substituer la plage de nuit par défaut.
 */
public record PreparedSc01Scenario(
        PlanningRequest planningRequest,
        ScenarioDatasetBuilderSc01.BuildResult buildResult,
        String scenarioType,
        String resourceId,
        Set<String> posteVirtuelIds,
        IgnoredCreneauxDTO ignoredCreneaux,
        List<ScenarioAlertDTO> alerts
) {
    public PreparedSc01Scenario {
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }
}