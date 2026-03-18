package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.scenarios.builder.ScenarioDatasetBuilderSc01;

import java.util.Set;

/**
 * PreparedSc01Scenario — objet de transport interne.
 *
 * Contient toutes les données préparées par ScenarioSc01PreparationService
 * avant l'appel au solveur. Évite de repasser par le DTO dans la couche exécution.
 */
public record PreparedSc01Scenario(
        PlanningRequest planningRequest,
        ScenarioDatasetBuilderSc01.BuildResult buildResult,
        String scenarioType,
        String resourceId,
        Set<String> posteVirtuelIds
) {}