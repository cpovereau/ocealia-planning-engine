package fr.project.planning.constraints.metier;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

/**
 * NuitSalarieNonNuit — Phase 8.
 *
 * Pénalise l'affectation d'un créneau de nuit (TypePlageHoraire.NUIT)
 * à un salarié qui n'est pas déclaré travailleur de nuit
 * (travailDeNuit = null ou valeur inconnue).
 *
 * Cette contrainte est SOFT : elle oriente le solveur
 * vers les salariés adaptés aux créneaux de nuit
 * sans interdire formellement l'affectation.
 *
 * Utilise SalarieReel.estTravailleurDeNuit() introduit en Phase 6.
 * Champ source : travailDeNuit (transporté Phase 1, mappé Phase 3).
 *
 * Impact SC-01 : SOFT uniquement — score hard inchangé.
 */
public class NuitSalarieNonNuit {

    private NuitSalarieNonNuit() {}

    public static Constraint nuitSalarieNonNuit(ConstraintFactory factory) {
        return factory
                .forEach(Creneau.class)
                .filter(c -> c.getRessourceAffectee() instanceof SalarieReel)
                .filter(c -> c.getTypePlageHoraire() == TypePlageHoraire.NUIT)
                .filter(c -> !((SalarieReel) c.getRessourceAffectee()).estTravailleurDeNuit())
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint(PenaliteKey.METIER_SOFT_NUIT_SALARIE_NON_NUIT.name());
    }
}
