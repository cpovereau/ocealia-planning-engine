package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DimanchesTravaillesMax {

    public static Constraint maxDimanchesTravailles(ConstraintFactory factory) {

        return factory
            .forEach(SalarieReel.class)
            .join(
                factory.forEach(Creneau.class),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )
            .groupBy(
                (s, c) -> s,
                ConstraintCollectors.toList((s, c) -> c)
            )
            .join(factory.forEach(PlanningContext.class))
            .penalize(
                "Dépassement du nombre maximal de dimanches travaillés",
                HardSoftScore.ONE_SOFT,
                (salarie, creneaux, context) -> {

                    int max = context.getSeuilsDeTolerance().getMaxDimanchesTravailles();
                    int base = context.getPenalites().getDepassementMaxDimanchesTravailles();

                    int dimanches = compterDimanchesDistincts(creneaux);
                    int excedent = Math.max(0, dimanches - max);

                    return base * excedent; // SOFT fort (base réglée côté métier)
                }
            );
    }

    private static int compterDimanchesDistincts(List<Creneau> creneaux) {
        Set<LocalDate> dimanches = creneaux.stream()
            .map(Creneau::getDate)
            .filter(d -> d.getDayOfWeek() == DayOfWeek.SUNDAY)
            .collect(Collectors.toSet());
        return dimanches.size();
    }
}
