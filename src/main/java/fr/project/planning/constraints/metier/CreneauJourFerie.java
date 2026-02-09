package fr.project.planning.constraints.metier;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

public class CreneauJourFerie {

    public static Constraint creneauJourFerie(ConstraintFactory factory) {

    return factory
        .forEach(Creneau.class)

        // Créneaux réellement affectés
        .filter(creneau ->
            !(creneau.getRessourceAffectee() instanceof RessourceNonAffectee)
        )

        // Créneaux qualifiés jour férié
        .filter(creneau ->
            creneau.getQualificationJour() == QualificationJour.FERIE
        )

        // Pénalisation en minutes
        .penalizeLong(
            HardSoftScore.ONE_SOFT,
            creneau -> (long) creneau.getDuree()
        )

        .asConstraint("Travail jour férié (minutes)");
    }

}
