package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PreparedSc05Scenario — objet de transport interne pour SC-05.
 *
 * <p>Enveloppe la préparation commune aux scénarios dataset-driven et y ajoute ce que SC-05 est
 * seul à savoir : entre qui l'on arbitre, et quels créneaux du périmètre ont effectivement été
 * rendus au solveur.</p>
 *
 * @param base                 préparation commune — problème, diagnostics, alertes du socle
 * @param planningRequest      problème effectivement soumis au solveur, périmètre d'arbitrage
 *                             attaché
 * @param ressourcesAutorisees salariés entre lesquels arbitrer. Un <strong>ensemble</strong>, de
 *                             taille deux aujourd'hui : le « deux » ne vit que dans le contrat
 *                             d'entrée (§5.5)
 * @param creneauxLiberes      identifiants des besoins rendus au solveur. Le rapprochement avec la
 *                             solution se fait <strong>par identifiant</strong> : le solveur
 *                             travaille sur des clones
 * @param creneauxTenusParUnTiers identifiants des créneaux du périmètre épinglés parce que tenus
 *                             par quelqu'un qui n'est pas de l'arbitrage (§5.2)
 * @param perimetreRetenu      besoins du périmètre <strong>effectivement présents</strong> dans le
 *                             problème soumis. Distinct du périmètre demandé : celui-ci peut nommer
 *                             des créneaux absents du dataset ou écartés avant le solveur
 * @param ressourceAvantParCreneau qui tenait chaque créneau du périmètre dans le planning transmis.
 *                             Le solveur ne le saura plus une fois le créneau libéré : c'est ici
 *                             que l'état d'avant est conservé, et nulle part ailleurs
 * @param metriquesAvant       [A2] indicateurs de chaque ressource <strong>avant</strong>
 *                             arbitrage, par identifiant. Mesurés par le même calculateur que ceux
 *                             d'après, sans quoi la comparaison ferait passer une différence de
 *                             méthode pour un mouvement
 * @param alerts               alertes du socle, suivies de celles propres à SC-05
 */
public record PreparedSc05Scenario(
        PreparedDatasetScenario base,
        PlanningRequest planningRequest,
        Set<String> ressourcesAutorisees,
        Set<String> creneauxLiberes,
        Set<String> creneauxTenusParUnTiers,
        Set<String> perimetreRetenu,
        Map<String, String> ressourceAvantParCreneau,
        Map<String, WorkMetrics> metriquesAvant,
        List<ScenarioAlertDTO> alerts) {

    public PreparedSc05Scenario {
        ressourcesAutorisees = fige(ressourcesAutorisees);
        creneauxLiberes = fige(creneauxLiberes);
        creneauxTenusParUnTiers = fige(creneauxTenusParUnTiers);
        perimetreRetenu = fige(perimetreRetenu);
        ressourceAvantParCreneau = ressourceAvantParCreneau == null
                ? Map.of() : Map.copyOf(ressourceAvantParCreneau);
        metriquesAvant = metriquesAvant == null ? Map.of() : Map.copyOf(metriquesAvant);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    /**
     * Copie immuable qui <strong>conserve l'ordre d'insertion</strong>.
     *
     * <p>{@code Set.copyOf} ne le garantit pas — son ordre d'itération est explicitement non
     * spécifié. Ces ensembles finissent dans la réponse : {@code ressourcesAutorisees} devient
     * {@code arbitrage.ressourcesArbitrees} et commande l'ordre de {@code parSalarie},
     * {@code creneauxTenusParUnTiers} est énuméré dans le message d'alerte. L'appelant a nommé A
     * puis B ; il doit les relire dans cet ordre, et deux appels identiques ne doivent pas les
     * intervertir.</p>
     */
    private static Set<String> fige(Set<String> valeurs) {
        return valeurs == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(valeurs));
    }
}
