package fr.project.planning.scenarios.service;

import fr.project.planning.api.PlanningRequest;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;

import java.util.List;
import java.util.Set;

/**
 * PreparedSc02Scenario — objet de transport interne pour SC-02.
 *
 * <p>Enveloppe la préparation commune aux scénarios dataset-driven et y ajoute ce que SC-02 est
 * seul à savoir : de quelle absence il s'agit, et quels créneaux elle a rendus au solveur.</p>
 *
 * @param base             préparation commune — problème, diagnostics, alertes du socle
 * @param planningRequest  problème effectivement soumis au solveur. Distinct de
 *                         {@code base.planningRequest()} lorsque le poste virtuel est refusé :
 *                         les ressources en sont alors retirées
 * @param salarieAbsentId  salarié dont l'absence motive le scénario
 * @param creneauxLiberes  identifiants des créneaux rendus au solveur. Le rapprochement avec la
 *                         solution se fait <strong>par identifiant</strong> : le solveur travaille
 *                         sur des clones, une comparaison par référence ne retrouverait rien
 * @param alerts           alertes du socle, suivies de celles propres à SC-02
 */
public record PreparedSc02Scenario(
        PreparedDatasetScenario base,
        PlanningRequest planningRequest,
        String salarieAbsentId,
        Set<String> creneauxLiberes,
        List<ScenarioAlertDTO> alerts) {

    public PreparedSc02Scenario {
        creneauxLiberes = creneauxLiberes == null ? Set.of() : Set.copyOf(creneauxLiberes);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }
}
