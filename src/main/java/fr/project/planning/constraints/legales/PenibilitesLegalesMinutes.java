package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.score.ScoreUtils;
import fr.project.planning.scoring.PenaliteKey;
import fr.project.planning.time.TimeBreakdown;
import fr.project.planning.time.TimeBreakdownCalculator;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

public final class PenibilitesLegalesMinutes {

    private static final TimeBreakdownCalculator CALCULATOR = new TimeBreakdownCalculator();

    private PenibilitesLegalesMinutes() {}

    public static Constraint penaliser(ConstraintFactory factory) {

        return factory
            .forEach(Creneau.class)

            .join(factory.forEach(PlanningContext.class))
            .join(factory.forEach(ReferentielComptabiliteActivite.class))
            .join(factory.forEach(RegulatoryParameters.class))

            .filter((creneau, context, referentiel, regulatory) -> {

                // Horizon temporel
                if (!context.getHorizonTemporel().contient(creneau.getDate())) {
                    return false;
                }

                ComptabiliteActivite comptabilite =
                    referentiel.getByCode(creneau.getCodeActiviteEffectif());

                return comptabilite != null
                        && comptabilite.isCompteDansCharge();
            })

            .penalize(
                HardSoftScore.ONE_SOFT,
                (creneau, context, referentiel, regulatory) -> {

                    TimeBreakdown b =
                        CALCULATOR.compute(creneau, regulatory, true);

                    HardSoftScore score =
                        ScoreUtils.penalitesLegalesAvecDominance(
                            context,
                            b.minutesNuit(),
                            b.minutesDimanche(),
                            b.minutesFerie(),
                            b.minutesNuitEtDimanche(),
                            b.minutesNuitEtFerie(),
                            b.minutesDimancheEtFerie(),
                            b.minutesNuitEtDimancheEtFerie()
                        );

                    // On retourne une pénalité positive (OptaPlanner multipliera par -1)
                    long penaliteLong = -score.softScore();
                    return Math.toIntExact(penaliteLong);
                }
            )
            .asConstraint(PenaliteKey.LEGAL_SOFT_PENIBILITES_LEGALES_MINUTES.name());
    }
}