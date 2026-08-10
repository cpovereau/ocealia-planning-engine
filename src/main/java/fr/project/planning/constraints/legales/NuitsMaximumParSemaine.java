package fr.project.planning.constraints.legales;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
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

/**
 * NuitsMaximumParSemaine — contrainte SOFT (lot S7.7)
 *
 * <p>Plafonne le nombre de nuits travaillées par semaine calendaire. Le comptage porte sur des
 * <strong>dates distinctes</strong> : deux créneaux de nuit la même date valent une nuit.</p>
 *
 * <h3>À ne pas confondre avec {@link NuitsConsecutivesMax}</h3>
 * <p>R3 borne l'<em>enchaînement</em> — combien de nuits d'affilée. Cette règle borne le
 * <em>volume</em> — combien de nuits sur la semaine, consécutives ou non. Trois nuits lundi,
 * mercredi et vendredi ne violent aucun enchaînement mais peuvent dépasser un volume de deux.</p>
 *
 * <h3>Pourquoi SOFT</h3>
 * <p>Alignement sur la doctrine posée au lot S3 : les bornes individuelles hebdomadaires
 * introduites pour SC-06 restent SOFT dans le solveur, et deviennent éliminatoires au
 * <em>classement</em> SC-06 par leur motif. Ce sont deux niveaux distincts — le solveur cherche
 * la meilleure solution possible, SC-06 refuse de recommander une solution illégale.
 * {@link fr.project.planning.constraints.metier.NuitSalarieNonNuit}, qui traite le signal voisin
 * {@code travailDeNuit}, est SOFT pour la même raison.</p>
 *
 * <h3>Lecture du zéro</h3>
 * <p>{@code nuitsMaximumParSemaine: 0} interdit toute nuit — c'est le cas de SAL-2001 dans le jeu
 * de référence SC-03, et l'exemple qui a motivé l'arbitrage du lot S7.7a.</p>
 *
 * <h3>Pénalité</h3>
 * <p>{@code PENALITE_NUITS_MAX_PAR_SEMAINE} × nuits en excédent, par semaine en dépassement.</p>
 */
public class NuitsMaximumParSemaine {

    private NuitsMaximumParSemaine() {
        // Utility class
    }

    /**
     * Pénalité par nuit excédentaire.
     *
     * <p>Une unité de dépassement vaut ici une nuit entière, pas une minute : le coefficient est
     * donc d'un tout autre ordre de grandeur que les pénalités exprimées à la minute.
     * Aligné sur {@code Penalites.depassementMaxDimanchesTravailles} (5 000), qui compte lui
     * aussi des journées.</p>
     */
    private static final int PENALITE_NUITS_MAX_PAR_SEMAINE = 5_000;

    public static Constraint nuitsMaximumParSemaine(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels dont le plafond hebdomadaire de nuits est renseigné
            .forEach(SalarieReel.class)
            .filter(NuitsMaximumParSemaine::plafondRenseigne)

            // 2) Jointure avec les créneaux de nuit affectés à ce salarié
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null
                            && c.getTypePlageHoraire() == TypePlageHoraire.NUIT),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // 3) Filtre : uniquement les nuits travaillées (compteDansCharge = true)
            .ifExists(
                ReferentielComptabiliteActivite.class,
                Joiners.filtering((salarie, creneau, ref) -> {
                    ComptabiliteActivite ca = ref.getByCode(creneau.getCodeActiviteEffectif());
                    return ca != null && ca.isCompteDansCharge();
                })
            )

            // 4) Groupement par (salarié, semaine) → nombre de nuits distinctes
            .groupBy(
                (salarie, creneau) -> salarie,
                (salarie, creneau) -> HeuresMaximumParSemaine.lundiDeLaSemaine(creneau.getDate()),
                ConstraintCollectors.countDistinct((salarie, creneau) -> creneau.getDate())
            )

            // 5) Seuls les dépassements produisent un match
            .filter((salarie, lundi, nbNuits) -> nbNuits > plafond(salarie))

            // 6) Pénalité SOFT proportionnelle à l'excédent
            .penalize(
                HardSoftScore.ONE_SOFT,
                (salarie, lundi, nbNuits) ->
                        PENALITE_NUITS_MAX_PAR_SEMAINE * (nbNuits - plafond(salarie))
            )
            .asConstraint(PenaliteKey.LEGAL_SOFT_NUITS_MAX_PAR_SEMAINE.name());
    }

    private static boolean plafondRenseigne(SalarieReel salarie) {
        return ContraintesReglementairesSalarie.borneRenseignee(
                salarie.contraintesOuAucune().getNuitsMaximumParSemaine());
    }

    /** N'est appelé qu'après {@link #plafondRenseigne(SalarieReel)} : la valeur est non nulle. */
    private static int plafond(SalarieReel salarie) {
        return salarie.contraintesOuAucune().getNuitsMaximumParSemaine();
    }
}
