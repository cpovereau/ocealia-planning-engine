package fr.project.planning.constraints.metier;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.reglementaire.CalendrierJoursFeries;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;

/**
 * JourFerieRefuse — contrainte HARD (Phase 4)
 *
 * <p>Un salarié dont {@code travailleJourFerie = false} ne peut pas être affecté sur un créneau
 * tombant un jour férié. La contrainte reste silencieuse si le champ est {@code null} : une
 * absence d'information n'est pas une interdiction.</p>
 *
 * <h3>[Lot S8.3] Le calendrier réglementaire est la seule source</h3>
 * <p>La règle lisait le drapeau {@code isJourFerie} porté par chaque créneau, quand la
 * valorisation ({@code TimeBreakdownCalculator}) lisait déjà le calendrier de
 * {@link RegulatoryParameters}. Deux sources répondaient donc à la même question, et le lot S8.0 —
 * qui a ouvert {@code planningContext.regulatoryParameters} au contrat — avait creusé l'écart :
 * un appelant déclarant son calendrier de fériés voyait ses minutes valorisées comme fériées,
 * mais un salarié refusant le travail férié pouvait y être affecté sans le moindre point HARD.</p>
 *
 * <p>Les deux consommateurs interrogent désormais le même calendrier. Celui-ci est arbitré en un
 * point unique, {@code ScenarioRegulatoryParametersMapper} : déclaré au contrat s'il l'est, déduit
 * des drapeaux {@code isJourFerie} sinon. Le drapeau du créneau n'est donc pas perdu — il devient
 * une <em>source</em> du calendrier au lieu d'être une seconde vérité concurrente.</p>
 *
 * <h3>Conséquence sur la maille</h3>
 * <p>Le férié devient une propriété de la <strong>date</strong>. Sur une date où un seul créneau
 * portait le drapeau, tous les créneaux de la journée sont désormais concernés — une déclaration
 * partielle en amont ne produit plus une interdiction partielle. C'est la même règle que
 * {@code CalendrierJoursFeries} applique déjà à la valorisation depuis S7.9a.</p>
 *
 * <h3>Créneaux traversant minuit</h3>
 * <p>Un créneau 22:00–06:00 chevauche deux jours civils. Il est refusé si <em>l'un des deux</em>
 * est férié, dès lors qu'il y travaille réellement — même lecture que la valorisation, qui compte
 * séparément les minutes de chaque jour.</p>
 */
public class JourFerieRefuse {

    private JourFerieRefuse() {}

    public static Constraint jourFerieRefuse(ConstraintFactory factory) {
        return factory
                .forEach(Creneau.class)
                .filter(c -> c.getRessourceAffectee() instanceof SalarieReel)
                .filter(c -> Boolean.FALSE.equals(
                        ((SalarieReel) c.getRessourceAffectee()).getTravailleJourFerie()))
                .join(factory.forEach(RegulatoryParameters.class))
                .filter(CalendrierJoursFeries::toucheUnJourFerie)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint(PenaliteKey.METIER_HARD_JOUR_FERIE_REFUSE.name());
    }
}
