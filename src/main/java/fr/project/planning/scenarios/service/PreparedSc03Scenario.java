package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.scenarios.dto.IgnoredCreneauxDTO;

import java.util.Set;

/**
 * PreparedSc03Scenario — objet de transport interne pour SC-03.
 *
 * Contient les données préparées par ScenarioSc03PreparationService
 * avant l'appel au solveur.
 *
 * Différences vs PreparedSc01Scenario :
 * - pas de BuildResult (les créneaux viennent de dataSet.creneaux, pas du builder SC-01)
 * - pas de resourceId unique (scénario multi-ressources)
 *
 * Phase 9 — ignoredCreneaux : compteurs pré-résolution produits par la couche de préparation.
 */
public record PreparedSc03Scenario(
        PlanningRequest planningRequest,
        String scenarioType,
        Set<String> posteVirtuelIds,
        IgnoredCreneauxDTO ignoredCreneaux
) {}
