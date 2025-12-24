package com.example.planning.solver;

import com.example.planning.domain.AffectationEtat;
import com.example.planning.domain.Creneau;
import com.example.planning.domain.PosteVirtuel;
import com.example.planning.domain.SalarieReel;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.*;

public class PlanningConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[]{
                // unSalarieNePeutAvoirQuUnCreneau(factory),
                penaliserCreneauNonAffecte(factory),
                penaliserPosteVirtuel(factory),
                penaliserPreferencePersonnelle(factory)
        };
    }

    /**
     * CONTRAINTE HARD (pédagogique)
     * Un salarié réel ne peut pas être affecté à plus d’un créneau.
     */
    private Constraint unSalarieNePeutAvoirQuUnCreneau(ConstraintFactory factory) {
        return factory.forEach(Creneau.class)
                // on ne garde que les créneaux affectés à un salarié réel
                .filter(c -> c.getRessourceAffectee() instanceof SalarieReel)
                // on regroupe par salarié
                .groupBy(
                        c -> (SalarieReel) c.getRessourceAffectee(),
                        ConstraintCollectors.count()
                )
                // plus d’un créneau = violation
                .filter((salarie, count) -> count > 1)
                .penalize(
                        "Un salarié ne peut avoir qu'un créneau",
                        HardSoftScore.ONE_HARD
                );
    }

    /**
     * Créneau non affecté = très mauvais
     */
    private Constraint penaliserCreneauNonAffecte(ConstraintFactory factory) {
        return factory.forEach(Creneau.class)
                .filter(c -> c.getRessourceAffectee() == AffectationEtat.A_AFFECTER)
                .penalize(
                        "Créneau non affecté",
                        HardSoftScore.ofSoft(10)
                );
    }

    /**
    * Poste virtuel = mauvais, mais moins
    */
    private Constraint penaliserPosteVirtuel(ConstraintFactory factory) {
        return factory.forEach(Creneau.class)
                .filter(c -> c.getRessourceAffectee() instanceof PosteVirtuel)
                .penalize(
                        "Poste virtuel utilisé",
                        HardSoftScore.ofSoft(1)
                );
    }

    /**
    * Préférence personnelle violée = pénalité légère
    */
    private Constraint penaliserPreferencePersonnelle(ConstraintFactory factory) {
        return factory.forEach(Creneau.class)
                .filter(c ->
                        c.getRessourceNonPreferee() != null &&
                        c.getRessourceNonPreferee().equals(c.getRessourceAffectee())
                )
                .penalize(
                        "Préférence personnelle violée",
                        HardSoftScore.ofSoft(2)
                );
    }

}

