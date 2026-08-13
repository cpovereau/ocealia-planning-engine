package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;

import java.util.List;
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
 * @param alerts               alertes du socle, suivies de celles propres à SC-05
 */
public record PreparedSc05Scenario(
        PreparedDatasetScenario base,
        PlanningRequest planningRequest,
        Set<String> ressourcesAutorisees,
        Set<String> creneauxLiberes,
        Set<String> creneauxTenusParUnTiers,
        List<ScenarioAlertDTO> alerts) {

    public PreparedSc05Scenario {
        ressourcesAutorisees = ressourcesAutorisees == null ? Set.of() : Set.copyOf(ressourcesAutorisees);
        creneauxLiberes = creneauxLiberes == null ? Set.of() : Set.copyOf(creneauxLiberes);
        creneauxTenusParUnTiers =
                creneauxTenusParUnTiers == null ? Set.of() : Set.copyOf(creneauxTenusParUnTiers);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }
}
