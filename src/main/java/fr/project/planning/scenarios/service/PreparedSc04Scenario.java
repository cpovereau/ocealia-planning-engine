package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.workmetrics.TrancheTemporelle;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PreparedSc04Scenario — objet de transport interne pour SC-04.
 *
 * <p>Enveloppe la préparation commune aux scénarios dataset-driven et y ajoute ce que SC-04 est
 * seul à savoir : où passe la frontière entre le passé et la suite, et à quoi ressemblait la
 * charge de chacun <strong>avant</strong> qu'on la rouvre — tranche par tranche.</p>
 *
 * @param base            préparation commune — problème, diagnostics, alertes du socle
 * @param planningRequest problème effectivement soumis au solveur
 * @param datePivot       premier jour ajustable. Ce qui précède est figé (§5.5)
 * @param creneauxFiges   identifiants des créneaux épinglés parce qu'antérieurs au pivot
 * @param creneauxAjustables identifiants des créneaux rendus au solveur
 * @param ressourceAvantParCreneau qui tenait chaque créneau ajustable dans le planning transmis.
 *                        Le solveur ne le saura plus une fois le créneau libéré : c'est ici que
 *                        l'état d'avant est conservé, et nulle part ailleurs
 * @param metriquesAvant  la charge de chacun avant optimisation, <strong>par tranche</strong> —
 *                        semaine, mois, période. Mesurée par le même calculateur que celle
 *                        d'après, sans quoi la comparaison ferait passer une différence de méthode
 *                        pour un mouvement du planning
 * @param alerts          alertes du socle, suivies de celles propres à SC-04
 */
public record PreparedSc04Scenario(
        PreparedDatasetScenario base,
        PlanningRequest planningRequest,
        LocalDate datePivot,
        Set<String> creneauxFiges,
        Set<String> creneauxAjustables,
        Map<String, String> ressourceAvantParCreneau,
        Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> metriquesAvant,
        List<ScenarioAlertDTO> alerts) {

    public PreparedSc04Scenario {
        creneauxFiges = fige(creneauxFiges);
        creneauxAjustables = fige(creneauxAjustables);
        ressourceAvantParCreneau = ressourceAvantParCreneau == null
                ? Map.of() : Map.copyOf(ressourceAvantParCreneau);
        metriquesAvant = metriquesAvant == null ? Map.of() : Collections.unmodifiableMap(metriquesAvant);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    /**
     * Copie immuable qui <strong>conserve l'ordre d'insertion</strong>.
     *
     * <p>{@code Set.copyOf} ne le garantit pas — son ordre d'itération est explicitement non
     * spécifié. Ces ensembles sont énumérés dans les messages d'alerte, et deux appels identiques
     * ne doivent pas les intervertir. Même raison qu'au lot A2 de SC-05, où l'ordre perdu avait
     * fait remonter les salariés arbitrés à l'envers de ce que l'appelant avait nommé.</p>
     */
    private static Set<String> fige(Set<String> valeurs) {
        return valeurs == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(valeurs));
    }

    /**
     * L'ordre des tranches, tel que le découpage l'a produit : semaines, puis mois, puis période.
     *
     * <p>{@code metriquesAvant} est une {@code LinkedHashMap} et le conserve ; la restitution s'y
     * appuie plutôt que de re-découper l'horizon une seconde fois.</p>
     */
    public List<TrancheTemporelle> tranches() {
        return List.copyOf(metriquesAvant.keySet());
    }
}
