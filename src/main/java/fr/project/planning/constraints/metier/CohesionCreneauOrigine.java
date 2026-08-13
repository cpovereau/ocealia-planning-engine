package fr.project.planning.constraints.metier;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

/**
 * CohesionCreneauOrigine — contrainte SOFT, lot S2 de SC-02.
 *
 * <p>Rien n'empêche, une fois un créneau découpé, que ses morceaux partent à quatre personnes
 * différentes. Le résultat serait <strong>conforme aux règles et inutilisable sur le terrain</strong> :
 * une journée de soins ne se passe pas de main en main toutes les heures.</p>
 *
 * <p>La pénalité porte sur les ressources <strong>en excédent de la première</strong> : couvrir
 * avec une seule personne ne coûte rien, avec deux coûte un cran, avec trois deux crans. Elle est
 * délibérément inférieure au coût d'un créneau non couvert — mieux vaut deux remplaçants que des
 * heures à pourvoir — et inférieure à celui d'un poste virtuel : deux personnes réelles valent
 * mieux qu'un poste fictif.</p>
 *
 * <p>Un morceau que personne ne prend n'est <strong>pas</strong> de la fragmentation : ne comptent
 * ici que les salariés réels. Laisser une part à pourvoir a déjà son propre coût, et le compter
 * deux fois pousserait le solveur à préférer un éparpillement à une couverture partielle.</p>
 */
public final class CohesionCreneauOrigine {

    private CohesionCreneauOrigine() {
    }

    public static Constraint cohesionCreneauOrigine(ConstraintFactory factory) {
        return factory
                .forEach(Creneau.class)
                .filter(c -> c.getCreneauOrigineId() != null
                        && c.getRessourceAffectee() instanceof SalarieReel)
                .groupBy(Creneau::getCreneauOrigineId,
                        ConstraintCollectors.countDistinct(Creneau::getRessourceAffectee))
                .filter((origine, ressourcesDistinctes) -> ressourcesDistinctes > 1)
                .join(factory.forEach(PlanningContext.class))
                .penalize(HardSoftScore.ONE_SOFT,
                        (origine, ressourcesDistinctes, contexte) ->
                                (ressourcesDistinctes - 1)
                                        * contexte.getPenalites().getFragmentationCreneauOrigine())
                .asConstraint(PenaliteKey.METIER_SOFT_FRAGMENTATION_CRENEAU_ORIGINE.name());
    }
}
