package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;

import java.time.DayOfWeek;

/**
 * DimanchesTravaillesMax — contrainte SOFT (R9)
 *
 * Pénalise le dépassement du nombre maximal de dimanches travaillés
 * pour un salarié sur la période de résolution.
 *
 * Définition d'un dimanche travaillé (conforme à 40_WORKMETRICS et nbDimanchesTravailles) :
 *   dimanche calendaire (DayOfWeek.SUNDAY) comportant au moins un créneau
 *   dont l'activité compte dans la charge (compteDansCharge = true).
 *   Comptage par date distincte : plusieurs créneaux le même dimanche = 1 dimanche travaillé.
 *
 * Seuil : SeuilsDeTolerance.maxDimanchesTravailles (global, conventionnel / métier).
 *   Pas de seuil individuel dans ContraintesReglementairesSalarie pour ce cas.
 *
 * Pénalité : Penalites.depassementMaxDimanchesTravailles × dimanches en excédent.
 */
public class DimanchesTravaillesMax {

    private DimanchesTravaillesMax() {
        // Utility class
    }

    public static Constraint maxDimanchesTravailles(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels
            .forEach(SalarieReel.class)

            // 2) Jointure avec les créneaux du dimanche affectés à ce salarié
            //    Le filtre DayOfWeek.SUNDAY est appliqué à la source pour limiter le volume
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null
                            && c.getDate().getDayOfWeek() == DayOfWeek.SUNDAY),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // 3) Filtre : uniquement les créneaux travaillés (compteDansCharge = true)
            //    ifExists évite le cross join avec le référentiel
            .ifExists(
                ReferentielComptabiliteActivite.class,
                Joiners.filtering((salarie, creneau, ref) -> {
                    ComptabiliteActivite ca = ref.getByCode(creneau.getActivite());
                    return ca != null && ca.isCompteDansCharge();
                })
            )

            // 4) Groupement par salarié → nombre de dimanches distincts travaillés
            //    countDistinct(date) déduplique automatiquement les créneaux du même dimanche
            .groupBy(
                (salarie, creneau) -> salarie,
                ConstraintCollectors.countDistinct((salarie, creneau) -> creneau.getDate())
            )

            // 5) Jointure avec le contexte (seuil + valeur de pénalité)
            .join(factory.forEach(PlanningContext.class))

            // 6) Pénalité SOFT proportionnelle au dépassement
            .penalize(
                HardSoftScore.ONE_SOFT,
                (salarie, nbDimanches, context) -> {
                    int max = context.getSeuilsDeTolerance().getMaxDimanchesTravailles();
                    int excedent = Math.max(0, nbDimanches - max);
                    return context.getPenalites().getDepassementMaxDimanchesTravailles() * excedent;
                }
            )
            .asConstraint(PenaliteKey.LEGAL_SOFT_DIMANCHES_TRAVAILLES_MAX.name());
    }
}
