package fr.project.planning.constraints.physiques;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;

public class LimitePhysique {
    private LimitePhysique() {
        // Utility class
    }

    /**
     * 1️⃣ Un salarié réel ne peut pas avoir deux créneaux qui se chevauchent.
     */
    public static Constraint pasDeChevauchement(ConstraintFactory factory) {

    return factory
        .forEachUniquePair(
            Creneau.class,
            Joiners.equal(Creneau::getDate),
            Joiners.equal(Creneau::getRessourceAffectee)
        )
        .filter((c1, c2) ->
            c1.getRessourceAffectee() != null
            && c1.getRessourceAffectee() instanceof SalarieReel
            && chevaucheStrict(c1, c2)
        )
        .penalize(HardSoftScore.ONE_HARD)
        .asConstraint("Chevauchement de créneaux (physique)");
    }    

    /**
     * 2️⃣ Un créneau ne peut pas dépasser 12h (720 minutes).
     */
    public static Constraint dureeMaxCreneau(ConstraintFactory factory) {

        return factory
            .forEach(Creneau.class)
            .filter(creneau -> creneau.getDuree() > 720)
            .penalize(
                HardSoftScore.ONE_HARD,
                creneau -> creneau.getDuree() - 720
            )
            .asConstraint("Durée de créneau > 12h (physique)");
    }

    /**
     * 3️⃣ Un salarié réel ne peut pas cumuler plus de 24h sur une journée.
     */
    public static Constraint cumulJournalierMax(ConstraintFactory factory) {

        return factory
            .forEach(Creneau.class)
            .filter(creneau ->
                creneau.getRessourceAffectee() != null
                && creneau.getRessourceAffectee() instanceof SalarieReel
        )
            .groupBy(
                creneau -> (SalarieReel) creneau.getRessourceAffectee(),
                Creneau::getDate,
                ConstraintCollectors.sum(Creneau::getDuree)
            )
            .filter((salarie, date, dureeTotale) -> dureeTotale > 1440)
            .penalize(
                HardSoftScore.ONE_HARD,
                (salarie, date, dureeTotale) -> dureeTotale - 1440
            )
            .asConstraint("Cumul journalier > 24h (physique)");
    }

    /* =========================
       Méthodes utilitaires
       ========================= */

    private static boolean chevaucheStrict(Creneau c1, Creneau c2) {
        return c1.getHeureDebut().isBefore(c2.getHeureFin())
            && c2.getHeureDebut().isBefore(c1.getHeureFin());
    }
}
