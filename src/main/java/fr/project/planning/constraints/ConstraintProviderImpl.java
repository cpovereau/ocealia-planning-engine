package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.DureeMaximaleLegaleParSalarie;
import fr.project.planning.constraints.physiques.LimitePhysique;
import fr.project.planning.constraints.metier.CreneauNonAffecte;
import fr.project.planning.constraints.metier.AffectationPosteVirtuel;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import fr.project.planning.constraints.metier.CreneauDeNuit;
import fr.project.planning.constraints.metier.CreneauJourFerie;  

/**
 * ConstraintProviderImpl
 *
 * Point d'entrée unique de toutes les contraintes du moteur.
 * L'ordre de déclaration reflète la hiérarchie métier :
 * - physiques
 * - légales
 * - métier
 * - service
 * - personnelles
 */
public class ConstraintProviderImpl implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {

        return new Constraint[] {

            /* =========================
               Contraintes physiques
               ========================= */

            LimitePhysique.pasDeChevauchement(factory),
            LimitePhysique.dureeMaxCreneau(factory),
            LimitePhysique.cumulJournalierMax(factory),

            /* =========================
               Contraintes légales (HARD)
               ========================= */

            DureeMaximaleLegaleParSalarie
                    .dureeMaximaleLegaleParSalarie(factory),

            /* =========================
               Contraintes métier
               ========================= */

            CreneauNonAffecte.creneauNonAffecte(factory),
            AffectationPosteVirtuel.affectationPosteVirtuel(factory),
            CreneauDeNuit.creneauDeNuit(factory),
            CreneauJourFerie.creneauJourFerie(factory),


            /* =========================
               Contraintes de service
               ========================= */

            // à venir : préférences

            /* =========================
               Contraintes personnelles
               ========================= */

            // à venir : préférences
        };
    }
}
