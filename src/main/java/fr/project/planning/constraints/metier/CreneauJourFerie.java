package fr.project.planning.constraints.metier;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.StrategieScoring;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

public class CreneauJourFerie {

    public static Constraint creneauJourFerie(ConstraintFactory factory) {

        return factory
            .forEach(Creneau.class)

            // Travail réel un jour férié
            .filter(creneau ->
                creneau.isJourFerie()
                && !(creneau.getRessourceAffectee() instanceof RessourceNonAffectee)
            )

            .join(factory.forEach(PlanningContext.class))

            .penalize(
                "Créneau travaillé un jour férié",
                HardSoftScore.ONE_SOFT,
                (creneau, context) -> {

                    int base = context.getPenalites().getViolationLegale();

                    return base * coefficient(context.getStrategieScoring());
                }
            );
    }

    private static int coefficient(StrategieScoring strategieScoring) {
        return switch (strategieScoring) {
            case EXPLOITATION -> 3; // à éviter en production
            case ANALYSE_RH  -> 1; // signal RH
            case AUDIT       -> 2; // signal réglementaire
        };
    }
}
