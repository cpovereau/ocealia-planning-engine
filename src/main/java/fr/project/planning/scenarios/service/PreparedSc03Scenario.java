package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.scenarios.dto.IgnoredCreneauxDTO;

import java.util.List;
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
        IgnoredCreneauxDTO ignoredCreneaux,
        List<Creneau> marqueursRepos
) {
    /**
     * [Lot S7.9b] {@code marqueursRepos} — les créneaux de repos hebdomadaire reçus en entrée.
     *
     * <p>Ils ne font pas partie du problème : le solveur ne les voit pas, ne les affecte pas et
     * ne les compte pas. Ils sont conservés pour être <strong>restitués</strong>, l'appelant
     * rechargeant la réponse pour réafficher son planning complet.</p>
     */
    public PreparedSc03Scenario {
        marqueursRepos = marqueursRepos == null ? List.of() : List.copyOf(marqueursRepos);
    }
}
