package fr.project.planning.constraints.metier;

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
        .filter(creneau ->
            creneau.getTypePlageHoraire() == TypePlageHoraire.NUIT
            && !(creneau.getRessourceAffectee() instanceof RessourceNonAffectee)
        )
        .penalizeLong(
            HardSoftScore.ONE_SOFT,
            creneau -> (long) creneau.getDuree()
        )
        .asConstraint("Travail de nuit (minutes)");
    }

}
