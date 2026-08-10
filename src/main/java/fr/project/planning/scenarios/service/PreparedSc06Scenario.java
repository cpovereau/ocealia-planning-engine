package fr.project.planning.scenarios.service;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.SalarieReel;
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
 */
public record PreparedSc06Scenario(
        PlanningProblem problem,
        List<Creneau> creneauxBesoin,
        List<SalarieReel> salaries,
        List<Indisponibilite> indisponibilites,
        ReferentielComptabiliteActivite referentiel,
        LocalDate dateBesoin,
        LocalDate lundiDeLaSemaine,
        String scenarioType
) {
}
