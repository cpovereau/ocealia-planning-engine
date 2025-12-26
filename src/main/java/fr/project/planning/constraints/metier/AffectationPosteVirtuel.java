package fr.project.planning.constraints.metier;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.StrategieScoring;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.PosteVirtuel;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

public class AffectationPosteVirtuel {

    public static Constraint affectationPosteVirtuel(ConstraintFactory factory) {

        return factory
            .forEach(Creneau.class)
            .filter(creneau ->
                creneau.getRessourceAffectee() instanceof PosteVirtuel
            )
            .join(factory.forEach(PlanningContext.class))
            .penalize(
                "Affectation à un poste virtuel",
                HardSoftScore.ONE_SOFT,
                (creneau, context) -> {

                    int base = context.getPenalites().getAffectationPosteVirtuel();

                    // Interdiction stricte
                    if (!context.getStrategieCouverture().isAutoriserPosteVirtuel()) {
                        return base * 1000;
                    }

                    // Ajustement selon la stratégie de scoring
                    return base * coefficient(context.getStrategieScoring());
                }
            );
    }

    private static int coefficient(StrategieScoring strategieScoring) {
        return switch (strategieScoring) {
            case EXPLOITATION -> 3; // on évite fortement
            case ANALYSE_RH  -> 1; // on observe
            case AUDIT       -> 2; // neutre mais visible
        };
    }
}
