package fr.project.planning.constraints.legales;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

public class DureeMaximaleLegaleParSalarie {

    /**
     * Durée maximale légale absolue sur la période de résolution.
     * (exemple : 13h = 780 minutes)
     *
     * 👉 À externaliser plus tard si besoin.
     */
    private static final int DUREE_MAX_LEGALE = 780;

    public static Constraint dureeMaximaleLegaleParSalarie(ConstraintFactory factory) {

        return factory
            // 1️⃣ On part des créneaux
            .forEach(Creneau.class)

            // 2️⃣ On ne garde que ceux affectés à un salarié réel
            .filter(creneau ->
                creneau.getRessourceAffectee() instanceof SalarieReel
            )

            // 3️⃣ Agrégation par salarié
            .groupBy(
                creneau -> (SalarieReel) creneau.getRessourceAffectee(),
                ConstraintCollectors.sum(Creneau::getDuree)
            )

            // 4️⃣ Détection du dépassement légal
            .filter((salarie, dureeTotale) ->
                dureeTotale > DUREE_MAX_LEGALE
            )

            // 5️⃣ Pénalité HARD
            .penalize(
                "Dépassement durée maximale légale par salarié",
                HardSoftScore.ONE_HARD,
                (salarie, dureeTotale) ->
                    dureeTotale - DUREE_MAX_LEGALE
            );
    }
}
