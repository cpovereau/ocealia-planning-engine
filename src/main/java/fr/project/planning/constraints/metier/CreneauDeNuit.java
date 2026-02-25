package fr.project.planning.constraints.metier;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

public class CreneauDeNuit {

    public static Constraint creneauDeNuit(ConstraintFactory factory) {

    return factory
        .forEach(Creneau.class)

            // créneaux réellement affectés (null-safe)
            .filter(c ->
                c.getRessourceAffectee() != null
                && !(c.getRessourceAffectee() instanceof RessourceNonAffectee)
            )

            // nuit uniquement
            .filter(c -> c.getTypePlageHoraire() == TypePlageHoraire.NUIT)

            // référentiel : travail réel uniquement
            .join(factory.forEach(ReferentielComptabiliteActivite.class))
            .filter((c, ref) -> {
                ComptabiliteActivite ca = ref.getByCode(c.getActivite());
                return ca != null && ca.isCompteDansCharge();
            })

        .penalizeLong(
            HardSoftScore.ONE_SOFT,
            (c, ref) -> (long) c.getDuree()
        )

        .asConstraint("Travail de nuit (minutes)");

    }
}
