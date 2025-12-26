package fr.project.planning.constraints.metier;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.StrategieScoring;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

public class CreneauDeNuit {

    public static Constraint creneauDeNuit(ConstraintFactory factory) {

        return factory
            .forEach(Creneau.class)

            // On ne juge que les créneaux réellement affectés
            .filter(creneau ->
                creneau.getTypePlageHoraire() == TypePlageHoraire.NUIT
                && !(creneau.getRessourceAffectee() instanceof RessourceNonAffectee)
            )

            .join(factory.forEach(PlanningContext.class))

            .penalize(
                "Créneau de nuit",
                HardSoftScore.ONE_SOFT,
                (creneau, context) -> {

                    int base = context.getPenalites().getViolationService();

                    return base * coefficient(context.getStrategieScoring());
                }
            );
    }

    private static int coefficient(StrategieScoring strategieScoring) {
        return switch (strategieScoring) {
            case EXPLOITATION -> 3; // on évite
            case ANALYSE_RH  -> 1; // on observe
            case AUDIT       -> 2; // on signale
        };
    }
}
