package fr.project.planning.constraints.metier;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;

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

        // 2) Placés sur un repos hebdomadaire (samedi ou dimanche)
        .filter(creneau ->
            creneau.getQualificationJour() == QualificationJour.RH
            || creneau.getQualificationJour() == QualificationJour.RHD
        )

        // 3) Jointure avec les facts (singleton)
            .join(factory.forEach(PlanningContext.class))
            .join(factory.forEach(ReferentielComptabiliteActivite.class))

        // 4) Filtre métier : dans l’horizon + activité générant de la dette
        .filter((creneau, context, referentiel) -> {

            // borne temporelle (cohérence avec le reste du moteur)
            if (!context.getHorizonTemporel().contient(creneau.getDate())) {
                return false;
            }

            // priorité à l'id activité planning, fallback sur l'ancien champ
            String codeActivite = (creneau.getCodeActiviteId() != null && !creneau.getCodeActiviteId().isBlank())
                    ? creneau.getCodeActiviteId()
                    : creneau.getActivite();

            ComptabiliteActivite comptabilite =
                referentiel.getByCode(codeActivite);

            return comptabilite != null
                && comptabilite.isCompteDansCharge()
                && comptabilite.isGenereDetteRepos();
        })

        // 5) Pénalité SOFT
        .penalize(
            "Dette de repos sur repos hebdomadaire",
            HardSoftScore.ONE_SOFT,
            (creneau, context, referentiel) -> {

                int base =
                    context.getPenalites()
                           .getDetteReposSurReposHebdomadaire();

                return base * creneau.getDuree();
            }
        );
    }
}
