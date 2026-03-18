package fr.project.planning.constraints.metier;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;

/**
 * IndisponibiliteSalarie
 *
 * Contrainte HARD Phase 4.
 *
 * Un salarié ne peut pas être affecté sur un créneau dont la date
 * est comprise dans l'une de ses périodes d'indisponibilité.
 *
 * La jointure porte sur l'identifiant de la ressource (ressourceId),
 * le filtre post-jointure vérifie l'appartenance à l'intervalle [dateDebut, dateFin].
 */
public class IndisponibiliteSalarie {

    private IndisponibiliteSalarie() {}

    public static Constraint indisponibiliteSalarie(ConstraintFactory factory) {
        return factory
                .forEach(Creneau.class)
                .filter(c -> c.getRessourceAffectee() instanceof SalarieReel)
                .join(factory.forEach(Indisponibilite.class),
                        Joiners.equal(
                                c -> c.getRessourceAffectee().getId(),
                                Indisponibilite::getRessourceId
                        )
                )
                .filter((c, indispo) ->
                        !c.getDate().isBefore(indispo.getDateDebut()) &&
                        !c.getDate().isAfter(indispo.getDateFin())
                )
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint(PenaliteKey.METIER_HARD_INDISPONIBILITE.name());
    }
}
