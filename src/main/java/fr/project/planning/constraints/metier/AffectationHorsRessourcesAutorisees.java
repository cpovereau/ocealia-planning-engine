package fr.project.planning.constraints.metier;

import fr.project.planning.domain.contexte.PerimetreArbitre;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;

/**
 * AffectationHorsRessourcesAutorisees — contrainte HARD, lot A0 de SC-05.
 *
 * <p>Un créneau remis en jeu par un arbitrage ne peut être confié qu'à l'une des ressources entre
 * lesquelles on arbitre. C'est la brique que le moteur n'avait pas (§4 du cadrage) : le domaine de
 * la variable de décision est global, et rien ne permettait jusqu'ici de le restreindre.</p>
 *
 * <h3>Pourquoi une contrainte, et non un périmètre réduit</h3>
 * <p>Ne transmettre que A et B comme ressources se passerait de toute contrainte nouvelle — et
 * serait trompeur. Sans le planning des autres, le moteur ne voit plus les créneaux qui bornent A
 * et B, et déclarerait conforme une répartition qui ne l'est pas. La contrainte HARD est la seule
 * forme sûre : elle borne l'affectation sans amputer la vue.</p>
 *
 * <h3>Elle ne rend jamais le problème insoluble</h3>
 * <p>Comme {@link BlocConfieTropCourt} et {@link ReposDominicalInviolable}, elle laisse toujours une
 * issue : le créneau part sur {@code A_AFFECTER} ou sur un poste virtuel, et devient des heures à
 * pourvoir. C'est l'invariant du moteur — <em>il ne refuse pas, il rend visible l'impossible</em>.
 * Seules les affectations à un <strong>salarié réel</strong> sont donc jugées, exactement comme
 * {@link IndisponibiliteSalarie} et {@link ReposDominicalInviolable}.</p>
 *
 * <h3>⚠️ L'existant épinglé n'est pas jugé — et c'est ce qui rend l'arbitrage §5.2 possible</h3>
 * <p>Un créneau du périmètre déjà tenu par un tiers est <strong>épinglé et signalé</strong>, jamais
 * repris : le tiers n'a rien demandé, on ne lui retire pas son travail pour équilibrer deux autres
 * personnes. Cet arbitrage n'est tenable que parce que cette contrainte ignore les créneaux figés.
 * Si elle les jugeait, épingler un créneau sur un tiers produirait un point HARD que le solveur ne
 * peut pas défaire, et « épingler et signaler » deviendrait « épingler et rendre insoluble ».</p>
 *
 * <h3>Le reste du planning reste libre</h3>
 * <p>SC-05 exige que le planning complet de la période soit transmis, faute de quoi les bornes
 * hebdomadaires seraient invérifiables. Seuls les créneaux <strong>du périmètre</strong> sont donc
 * bornés ; tout le reste — y compris les autres créneaux de A et de B — conserve le domaine global.
 * Sans cette restriction, transmettre le planning complet le rendrait entièrement fautif.</p>
 *
 * <h3>La jointure porte sur le besoin, pas sur le créneau</h3>
 * <p>L'appelant désigne le périmètre avec les identifiants qu'il a transmis. Si la préparation
 * découpe un créneau en segments, ceux-ci reçoivent des identifiants fabriqués et gardent celui de
 * leur origine — {@link Creneau#getIdBesoin()}. Joindre sur {@code getId()} ferait silencieusement
 * échapper tous les segments à l'arbitrage, c'est-à-dire ne borner plus rien du tout, sans qu'aucun
 * point HARD ne le signale.</p>
 *
 * <h3>Un ensemble, jamais un couple</h3>
 * <p>Aucune trace ici du « deux » de SC-05 : la contrainte demande à
 * {@link PerimetreArbitre#autorise} si la ressource est autorisée, et ne sait pas combien elles
 * sont. Voir §5.5 du cadrage — l'ouverture à N est attendue à brève échéance.</p>
 */
public final class AffectationHorsRessourcesAutorisees {

    private AffectationHorsRessourcesAutorisees() {
    }

    public static Constraint affectationHorsRessourcesAutorisees(ConstraintFactory factory) {
        return factory
                .forEach(Creneau.class)

                // 1) Confié à un salarié réel. Rester à pourvoir ou passer sur un poste virtuel
                //    demeure possible : c'est ce qui garantit que le problème reste soluble.
                .filter(creneau -> creneau.getRessourceAffectee() instanceof SalarieReel)

                // 2) Décidé par le solveur. Le créneau du périmètre tenu par un tiers est épinglé
                //    et signalé (§5.2) ; le juger le rendrait insoluble.
                .filter(creneau -> !creneau.isFige())

                // 3) Du périmètre remis en jeu — et de lui seul. Sur l'identifiant du besoin, pour
                //    que les segments d'un créneau arbitré le restent.
                .join(PerimetreArbitre.class,
                        Joiners.filtering((creneau, perimetre) ->
                                perimetre.contientLeBesoin(creneau.getIdBesoin())))

                // 4) À quelqu'un qui n'est pas de l'arbitrage.
                .filter((creneau, perimetre) ->
                        !perimetre.autorise(creneau.getRessourceAffectee().getId()))

                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint(PenaliteKey.METIER_HARD_RESSOURCE_NON_AUTORISEE.name());
    }
}
