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
 * <p>Contrainte HARD Phase 4. Un salarié ne peut pas être affecté sur un créneau qui empiète sur
 * l'une de ses périodes d'indisponibilité.</p>
 *
 * <p>La jointure porte sur l'identifiant de la ressource ; le filtre post-jointure délègue à
 * {@link Creneau#chevauchePeriode} le soin de savoir ce qu'« empiéter » veut dire.</p>
 *
 * <p><strong>[Lot S0 de SC-02]</strong> Ce filtre comparait la seule {@code date} du créneau aux
 * bornes de l'absence. Un créneau du 3 mars 22:00–06:00 échappait donc à une absence déclarée le
 * 4 mars, alors qu'il fait travailler six heures pendant celle-ci — aucun point HARD n'était
 * produit. La règle est désormais celle de l'intervalle effectif, minuit franchi compris, comme
 * pour les quatre calculs réparés au lot S8.3.</p>
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
                        c.chevauchePeriode(indispo.getDateDebut(), indispo.getDateFin()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint(PenaliteKey.METIER_HARD_INDISPONIBILITE.name());
    }
}
