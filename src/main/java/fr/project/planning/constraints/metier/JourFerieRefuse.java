package fr.project.planning.constraints.metier;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

/**
 * JourFerieRefuse
 *
 * Contrainte HARD Phase 4.
 *
 * Un salarié avec travailleJourFerie = false ne peut pas être affecté
 * sur un créneau marqué jour férié (isJourFerie = true).
 *
 * La contrainte est silencieuse si travailleJourFerie est null
 * (absence d'information = pas d'interdiction explicite).
 */
public class JourFerieRefuse {

    private JourFerieRefuse() {}

    public static Constraint jourFerieRefuse(ConstraintFactory factory) {
        return factory
                .forEach(Creneau.class)
                .filter(c -> c.getRessourceAffectee() instanceof SalarieReel)
                .filter(c -> Boolean.FALSE.equals(
                        ((SalarieReel) c.getRessourceAffectee()).getTravailleJourFerie()))
                .filter(Creneau::isJourFerie)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint(PenaliteKey.METIER_HARD_JOUR_FERIE_REFUSE.name());
    }
}
