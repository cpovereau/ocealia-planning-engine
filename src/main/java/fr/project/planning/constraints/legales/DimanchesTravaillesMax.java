package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
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
 * <p>Pénalise le dépassement du nombre maximal de dimanches travaillés par un salarié sur la
 * période de résolution.</p>
 *
 * <h3>Définition d'un dimanche travaillé</h3>
 * <p>Conforme à {@code 40_WORKMETRICS} et à {@code nbDimanchesTravailles} : dimanche calendaire
 * ({@link DayOfWeek#SUNDAY}) comportant au moins un créneau dont l'activité compte dans la
 * charge. Le comptage se fait par date distincte — plusieurs créneaux le même dimanche valent un
 * seul dimanche travaillé.</p>
 *
 * <h3>Lot S7.1 — remise en service</h3>
 * <p>Cette contrainte était <strong>dormante</strong>. Elle lisait l'activité via
 * {@code getActivite()}, le champ déprécié, que les clients conformes au contrat n'envoient plus.
 * Le référentiel ne trouvait rien, aucun match n'était produit — et son seuil était de surcroît
 * un seuil global jamais alimenté, donc nul. Deux corrections :</p>
 * <ul>
 *   <li>l'activité est lue via {@link Creneau#getCodeActiviteEffectif()}, qui privilégie
 *       {@code codeActiviteId} et retombe sur le champ historique ;</li>
 *   <li>le seuil est lu sur le salarié — {@code contraintesReglementaires.dimanchesTravaillesMaximum}
 *       — et non plus dans {@code SeuilsDeTolerance}.</li>
 * </ul>
 *
 * <h3>Activation</h3>
 * <p>Inactive tant que le plafond n'est pas transmis, cf.
 * {@link ContraintesReglementairesSalarie#borneRenseignee(Number)}. Un plafond à <strong>0</strong>
 * est en revanche appliqué à la lettre : aucun dimanche travaillé n'est alors autorisé. Le filtre
 * est appliqué en tête de flux — un salarié sans plafond ne produit aucun tuple.</p>
 *
 * <h3>Pénalité</h3>
 * <p>{@code Penalites.depassementMaxDimanchesTravailles} × dimanches en excédent. La valeur de la
 * pénalité reste globale : c'est un poids de scoring, pas une règle. Seul le seuil est individuel.</p>
 */
public class DimanchesTravaillesMax {

    private DimanchesTravaillesMax() {
        // Utility class
    }

    public static Constraint maxDimanchesTravailles(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels dont le plafond de dimanches est renseigné
            .forEach(SalarieReel.class)
            .filter(DimanchesTravaillesMax::plafondRenseigne)

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
                    ComptabiliteActivite ca = ref.getByCode(creneau.getCodeActiviteEffectif());
                    return ca != null && ca.isCompteDansCharge();
                })
            )

            // 4) Groupement par salarié → nombre de dimanches distincts travaillés
            //    countDistinct(date) déduplique automatiquement les créneaux du même dimanche
            .groupBy(
                (salarie, creneau) -> salarie,
                ConstraintCollectors.countDistinct((salarie, creneau) -> creneau.getDate())
            )

            // 5) Seuls les dépassements produisent un match : un salarié dans les clous
            //    n'apparaît pas au scoreBreakdown avec un impact nul
            .filter((salarie, nbDimanches) -> nbDimanches > plafond(salarie))

            // 6) Jointure avec le contexte, pour la valeur de la pénalité
            .join(factory.forEach(PlanningContext.class))

            // 7) Pénalité SOFT proportionnelle au dépassement
            .penalize(
                HardSoftScore.ONE_SOFT,
                (salarie, nbDimanches, context) ->
                        context.getPenalites().getDepassementMaxDimanchesTravailles()
                                * (nbDimanches - plafond(salarie))
            )
            .asConstraint(PenaliteKey.LEGAL_SOFT_DIMANCHES_TRAVAILLES_MAX.name());
    }

    private static boolean plafondRenseigne(SalarieReel salarie) {
        return ContraintesReglementairesSalarie.borneRenseignee(
                salarie.contraintesOuAucune().getDimanchesTravaillesMaximum());
    }

    /** N'est appelé qu'après {@link #plafondRenseigne(SalarieReel)} : la valeur est non nulle. */
    private static int plafond(SalarieReel salarie) {
        return salarie.contraintesOuAucune().getDimanchesTravaillesMaximum();
    }
}
