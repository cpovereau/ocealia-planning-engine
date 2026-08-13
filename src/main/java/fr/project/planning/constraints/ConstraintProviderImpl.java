package fr.project.planning.constraints;

import fr.project.planning.constraints.legales.DureeMaximaleLegaleParSalarie;
import fr.project.planning.constraints.physiques.LimitePhysique;
import fr.project.planning.constraints.metier.AffectationPosteVirtuel;
import fr.project.planning.constraints.metier.CreneauNonAffecte;
import fr.project.planning.constraints.metier.BlocConfieTropCourt;
import fr.project.planning.constraints.metier.CohesionCreneauOrigine;
import fr.project.planning.constraints.metier.IndisponibiliteSalarie;
import fr.project.planning.constraints.metier.SurchargeAcceptable;
import fr.project.planning.constraints.metier.JourFerieRefuse;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import fr.project.planning.constraints.legales.NuitsConsecutivesMax;
import fr.project.planning.constraints.legales.PenibilitesLegalesMinutes;
import fr.project.planning.constraints.legales.ReposObligatoireApresNuits;
import fr.project.planning.constraints.legales.ReposHebdomadaireMin;
import fr.project.planning.constraints.legales.ReposHebdomadaireGlissant;
import fr.project.planning.constraints.legales.AlternanceJourNuit;
import fr.project.planning.constraints.legales.AmplitudeJournaliere;
import fr.project.planning.constraints.legales.DimanchesTravaillesMax;
import fr.project.planning.constraints.legales.HeuresMinimumParJour;
import fr.project.planning.constraints.legales.HeuresMaximumParSemaine;
import fr.project.planning.constraints.legales.HeuresMinimumParSemaine;
import fr.project.planning.constraints.legales.NuitsMaximumParSemaine;
import fr.project.planning.constraints.legales.JoursConsecutifsMax;
import fr.project.planning.constraints.legales.ReposQuotidienMinimum;

import fr.project.planning.constraints.metier.DetteReposSurReposHebdomadaire;
import fr.project.planning.constraints.metier.NuitSalarieNonNuit;
import fr.project.planning.constraints.metier.ReposDominicalInviolable;

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

            DureeMaximaleLegaleParSalarie.dureeMaximaleLegaleParSalarie(factory),
            NuitsConsecutivesMax.maxNuitsConsecutives(factory),
            ReposObligatoireApresNuits.reposObligatoireApresNuits(factory),
            ReposHebdomadaireMin.reposHebdomadaireMin(factory),

           /* =========================
               Contraintes légales (SOFT)
               ========================= */
            ReposHebdomadaireGlissant.reposHebdoGlissant(factory),
            DimanchesTravaillesMax.maxDimanchesTravailles(factory),
            JoursConsecutifsMax.maxJoursConsecutifs(factory),
            AlternanceJourNuit.alternanceJourNuit(factory),
            AmplitudeJournaliere.amplitudeJournaliere(factory),
            HeuresMinimumParJour.heuresMinimumParJour(factory),
            ReposQuotidienMinimum.reposQuotidienMinimum(factory),
            HeuresMaximumParSemaine.heuresMaximumParSemaine(factory),
            HeuresMinimumParSemaine.heuresMinimumParSemaine(factory),
            HeuresMinimumParSemaine.semaineSansAffectation(factory),
            NuitsMaximumParSemaine.nuitsMaximumParSemaine(factory),

            /* =========================
               Contraintes métier (HARD)
               ========================= */

            JourFerieRefuse.jourFerieRefuse(factory),
            IndisponibiliteSalarie.indisponibiliteSalarie(factory),
            BlocConfieTropCourt.blocConfieTropCourt(factory),
            ReposDominicalInviolable.reposDominicalInviolable(factory),

            /* =========================
               Contraintes métier (SOFT)
               ========================= */

            CreneauNonAffecte.creneauNonAffecte(factory),
            AffectationPosteVirtuel.affectationPosteVirtuel(factory),
            NuitSalarieNonNuit.nuitSalarieNonNuit(factory),
            CohesionCreneauOrigine.cohesionCreneauOrigine(factory),
            SurchargeAcceptable.surchargeJournaliere(factory),
            SurchargeAcceptable.surchargeHebdomadaire(factory),
            PenibilitesLegalesMinutes.penaliser(factory),
            DetteReposSurReposHebdomadaire.penaliser(factory),

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
