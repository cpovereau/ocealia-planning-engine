package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;
import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DimanchesTravaillesMax {
    private DimanchesTravaillesMax() {
    // Utility class
    }

    public static Constraint maxDimanchesTravailles(ConstraintFactory factory) {

        return factory
            .forEach(SalarieReel.class)
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null),
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
            .join(factory.forEach(ReferentielComptabiliteActivite.class))

            .penalize(
                HardSoftScore.ONE_SOFT,
                (salarie, creneaux, context, ref) -> {

                int max = context.getSeuilsDeTolerance().getMaxDimanchesTravailles();
                int base = context.getPenalites().getDepassementMaxDimanchesTravailles();

                HorizonTemporel horizon = context.getHorizonTemporel();

                int dimanches = compterDimanchesDistincts(creneaux, horizon, ref);
                int excedent = Math.max(0, dimanches - max);

                return base * excedent;
            }
            )
            .asConstraint(PenaliteKey.LEGAL_SOFT_DIMANCHES_TRAVAILLES_MAX.name());
    }

    private static int compterDimanchesDistincts(
        List<Creneau> creneaux,
        HorizonTemporel horizon,
        ReferentielComptabiliteActivite ref
    ) {
        Set<LocalDate> dimanches = creneaux.stream()
            .filter(c -> horizon.contient(c.getDate()))
            .filter(c -> c.getDate().getDayOfWeek() == DayOfWeek.SUNDAY)
            .filter(c -> {
                ComptabiliteActivite ca = ref.getByCode(c.getActivite());
                return ca != null && ca.isCompteDansCharge();
            })
            .map(Creneau::getDate)
            .collect(Collectors.toSet());

        return dimanches.size();
    }

}
