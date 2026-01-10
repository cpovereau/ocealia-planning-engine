package fr.project.planning.constraints.metier;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.solution.PlanningProblem;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

/**
 * DetteReposSurReposHebdomadaire
 *
 * Contrainte métier SOFT.
 *
 * Pénalise un créneau placé sur un repos hebdomadaire
 * lorsque l'activité associée génère de la dette de repos.
 *
 * - La situation est autorisée
 * - Elle est pénalisée pour encourager une autre répartition
 * - Le poids dépend du contexte de résolution
 */
public class DetteReposSurReposHebdomadaire {

    public static Constraint penaliser(ConstraintFactory factory) {

    return factory
        // 1) Tous les créneaux
        .forEach(Creneau.class)

        // 2) Placés sur un repos hebdomadaire
        .filter(creneau ->
            creneau.getQualificationJour() == QualificationJour.RHD
        )

        // 3) Jointure avec la solution (PlanningProblem)
        .join(factory.forEach(PlanningProblem.class))

        // 4) Filtre métier : activité générant de la dette
        .filter((creneau, problem) -> {

            ReferentielComptabiliteActivite referentiel =
                problem.getReferentielComptabiliteActivite();

            ComptabiliteActivite comptabilite =
                referentiel.getByCode(creneau.getActivite());

            return comptabilite != null && comptabilite.isGenereDetteRepos();
        })

        // 5) Pénalité SOFT
        .penalize(
            "Dette de repos sur repos hebdomadaire",
            HardSoftScore.ONE_SOFT,
            (creneau, problem) -> {

                int base =
                    problem.getPlanningContext()
                           .getPenalites()
                           .getDetteReposSurReposHebdomadaire();

                return base * creneau.getDuree();
            }
        );
}

}
