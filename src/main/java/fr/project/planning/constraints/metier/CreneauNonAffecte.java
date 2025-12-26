package fr.project.planning.constraints.metier;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.StrategieScoring;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

public class CreneauNonAffecte {

    public static Constraint creneauNonAffecte(ConstraintFactory factory) {

        return factory
            .forEach(Creneau.class)
            .filter(creneau ->
                creneau.getRessourceAffectee() instanceof RessourceNonAffectee
            )
            .join(factory.forEach(PlanningContext.class))
            .penalize(
                "Créneau non affecté",
                HardSoftScore.ONE_SOFT,
                (creneau, context) -> {

                    int base = context.getPenalites().getNonAffectation();

                    // Interdiction stricte
                    if (!context.getStrategieCouverture().isAutoriserNonAffectation()) {
                        return base * 1000;
                    }

                    // Ajustement selon la stratégie de scoring
                    return base * coefficient(context.getStrategieScoring());
                }
            );
    }

    private static int coefficient(StrategieScoring strategieScoring) {
        return switch (strategieScoring) {
            case EXPLOITATION -> 5; // quasiment inacceptable
            case ANALYSE_RH  -> 2; // signal fort mais exploitable
            case AUDIT       -> 3; // risque à documenter
        };
    }
}
