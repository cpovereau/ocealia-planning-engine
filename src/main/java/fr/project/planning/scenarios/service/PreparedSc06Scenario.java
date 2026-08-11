package fr.project.planning.scenarios.service;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scenarios.dto.ScenarioAlertDTO;
import fr.project.planning.solution.PlanningProblem;

import java.time.LocalDate;
import java.util.List;

/**
 * PreparedSc06Scenario — tout ce dont l'énumération a besoin, une fois les données validées.
 *
 * @param problem            problème complet : planning existant figé + créneaux du besoin
 * @param creneauxBesoin     créneaux à couvrir, dans l'ordre de la requête. Ce sont les seules
 *                           entités non figées du problème, donc les seules variables de décision
 * @param salaries           salariés réels du dataset, candidats potentiels
 * @param indisponibilites   indisponibilités, utilisées par le filtre d'éligibilité
 * @param referentiel        référentiel d'activités, utilisé pour les volumes horaires
 * @param dateBesoin         jour du besoin, commun à tous ses créneaux
 * @param lundiDeLaSemaine   lundi de la semaine du besoin, borne de calcul hebdomadaire
 * @param scenarioType       type de scénario, restitué tel quel
 * @param alerts             [S8.4] ce que la préparation a décidé à la place de l'appelant. SC-06
 *                           refuse plutôt qu'il n'ignore : ses garde-fous lèvent des exceptions.
 *                           Le cadre réglementaire est le seul endroit où il substitue en silence
 *                           — et ne le fait donc plus
 */
public record PreparedSc06Scenario(
        PlanningProblem problem,
        List<Creneau> creneauxBesoin,
        List<SalarieReel> salaries,
        List<Indisponibilite> indisponibilites,
        ReferentielComptabiliteActivite referentiel,
        LocalDate dateBesoin,
        LocalDate lundiDeLaSemaine,
        String scenarioType,
        List<ScenarioAlertDTO> alerts
) {
    public PreparedSc06Scenario {
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }
}
