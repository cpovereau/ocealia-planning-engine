package fr.project.planning.constraints.metier;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.ToleranceEquite;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.workmetrics.EcartAuContrat;
import fr.project.planning.scoring.PenaliteKey;
import fr.project.planning.time.RepartitionPenibilites;
import fr.project.planning.time.TimeBreakdownCalculator;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;
import org.optaplanner.core.api.score.stream.uni.UniConstraintStream;

/**
 * EquiteChargeAuContrat — la première règle du moteur qui compare deux personnes (équité, lot L5).
 *
 * <h3>Ce qu'elle répare</h3>
 * <p>Toutes les autres contraintes vérifient une personne contre <em>sa</em> borne. Conséquence
 * relevée au cadrage : <strong>un planning où l'un fait 48 h et l'autre 25 h obtient exactement le
 * même score qu'un planning à 35 h chacun.</strong> Rien, dans le score, ne rendait un déséquilibre
 * coûteux.</p>
 *
 * <h3>La grandeur pesée</h3>
 * <p>L'écart <strong>signé</strong> au volume contractuel, mesuré sur les heures
 * <strong>pondérées</strong> par la pénibilité — on ne juge l'équité qu'à pénibilité équivalente —
 * et rapporté au contrat de chacun, jamais à la moyenne du groupe : un salarié à 50 % y serait
 * perpétuellement « sous la moyenne », et le moteur passerait son temps à vouloir le charger.</p>
 *
 * <p>La pénalité porte sur la <strong>valeur absolue</strong> de l'écart au-delà de la tolérance.
 * La sous-charge coûte donc autant que la surcharge, et c'est le point : sans cela le moteur
 * éviterait de surcharger sans jamais rééquilibrer — l'équité ne se produirait pas, elle serait
 * seulement moins violée.</p>
 *
 * <h3>Elle ne se déclenche que si une tolérance est déclarée</h3>
 * <p>Aucun {@link ToleranceEquite} au problème et la jointure ne produit rien : le score est
 * <strong>exactement</strong> celui d'avant ce lot. C'est le cas de toutes les demandes à ce jour,
 * et c'est délibéré — les coefficients qui pondèrent cet écart ne sont pas encore calibrés.</p>
 *
 * <h3>Deux volets, comme les heures minimum hebdomadaires</h3>
 * <p>Un salarié sans aucun créneau n'apparaît dans aucune jointure, donc dans aucun total : son
 * écart de −100 % resterait invisible, et le moteur n'aurait aucune raison de lui donner du
 * travail — alors que c'est exactement la personne que l'équité désigne. Le second volet le
 * rattrape. Même découpage que {@code HeuresMinimumParSemaine}, et pour la même raison.</p>
 *
 * <h3>Aucune borne du cadre général ne bouge</h3>
 * <p>Un écart d'équité favorable n'autorise jamais à dépasser un plafond réglementaire ; l'inverse
 * non plus — un salarié au plafond n'est pas « servi », il est empêché. Cette règle est SOFT et le
 * reste : elle départage, elle n'interdit pas.</p>
 */
public final class EquiteChargeAuContrat {

    private static final TimeBreakdownCalculator DECOUPAGE = new TimeBreakdownCalculator();

    /**
     * Pénalité par point de pourcentage d'écart au-delà de la tolérance.
     *
     * <p><strong>Calibrée pour départager sans jamais interdire</strong>, comme celle de
     * {@code SurchargeAcceptable} et pour la même raison : une pénalité d'équité trop lourde
     * rendrait un créneau laissé à pourvoir <em>moins cher</em> que sa couverture, et la règle se
     * comporterait en interdit — l'inverse de ce qui est arbitré.</p>
     *
     * <p>À 10 points l'unité, un salarié à +30 % avec une tolérance de 10 coûte 200 : il faut un
     * écart de cent points — un salarié entièrement inoccupé — pour approcher le coût d'un créneau
     * non couvert en {@code ANALYSE_RH} (1 000). L'équité départage donc deux plannings également
     * couverts, sans jamais dissuader de couvrir.</p>
     *
     * <p>⚠️ <strong>Valeur provisoire.</strong> Elle pèse une mesure dont l'échelle — les
     * coefficients de pénibilité — n'est pas calibrée. Elle relève du même protocole :
     * {@code 92_CALIBRATION_PENIBILITE.md}.</p>
     */
    private static final int PENALITE_PAR_POINT = 10;

    /** Écart d'un salarié qui ne travaille rien : le plus défavorable possible. */
    private static final double ECART_SANS_AUCUNE_CHARGE = -100.0;

    private EquiteChargeAuContrat() {
    }

    // =====================================================================
    // Volet 1 — le salarié travaille, et son écart dépasse la tolérance
    // =====================================================================

    public static Constraint equiteChargeAuContrat(ConstraintFactory factory) {
        return factory
                .forEach(SalarieReel.class)
                .filter(EquiteChargeAuContrat::contratMesurable)

                .join(creneauxTravailles(factory),
                        Joiners.equal(SalarieReel::getId, c -> c.getRessourceAffectee().getId()))
                .join(PlanningContext.class)
                .join(RegulatoryParameters.class)
                .filter((salarie, creneau, contexte, reglementaire) ->
                        !contexte.getToleranceEquite().estVide()
                                && contexte.getHorizonTemporel().contient(creneau.getDate()))

                // (salarié, contexte) → centièmes de minute pondérée
                .groupBy(
                        (salarie, creneau, contexte, reglementaire) -> salarie,
                        (salarie, creneau, contexte, reglementaire) -> contexte,
                        ConstraintCollectors.sumLong(
                                (salarie, creneau, contexte, reglementaire) ->
                                        centiemesPonderes(creneau, contexte, reglementaire)))

                .filter((salarie, contexte, centiemes) ->
                        pointsExcedentaires(salarie, contexte, centiemes) > 0)
                .penalize(HardSoftScore.ONE_SOFT,
                        (salarie, contexte, centiemes) ->
                                PENALITE_PAR_POINT * pointsExcedentaires(salarie, contexte, centiemes))
                .asConstraint(PenaliteKey.METIER_SOFT_EQUITE_ECART_CONTRAT.name());
    }

    // =====================================================================
    // Volet 2 — le salarié ne travaille rien du tout
    // =====================================================================

    public static Constraint equiteSalarieSansAffectation(ConstraintFactory factory) {
        return factory
                .forEach(SalarieReel.class)
                .filter(EquiteChargeAuContrat::contratMesurable)

                // Le référentiel est joint d'abord : sans lui, « ne travaille rien » se lirait
                // « n'a aucun créneau », et un planning entier d'activités hors charge — une
                // semaine de formation — échapperait aux deux volets à la fois.
                .join(ReferentielComptabiliteActivite.class)
                .ifNotExists(Creneau.class,
                        Joiners.equal((salarie, referentiel) -> salarie.getId(),
                                EquiteChargeAuContrat::ressourceAffecteeId),
                        Joiners.filtering((salarie, referentiel, creneau) ->
                                porteDuTravail(creneau) && compteDansCharge(referentiel, creneau)))

                .join(PlanningContext.class)
                .filter((salarie, referentiel, contexte) -> contexte.getToleranceEquite()
                        .pointsExcedentaires(ECART_SANS_AUCUNE_CHARGE) > 0)
                .penalize(HardSoftScore.ONE_SOFT,
                        (salarie, referentiel, contexte) -> PENALITE_PAR_POINT
                                * contexte.getToleranceEquite()
                                        .pointsExcedentaires(ECART_SANS_AUCUNE_CHARGE))
                .asConstraint(PenaliteKey.METIER_SOFT_EQUITE_SANS_AFFECTATION.name());
    }

    // =====================================================================
    // Calculs
    // =====================================================================

    /**
     * Points de pourcentage d'écart au-delà de la tolérance.
     *
     * <p>L'écart passe par {@link EcartAuContrat}, comme la mesure restituée et comme le classement
     * de SC-06 : le score, la réponse et le rang disent la même chose, ou l'un contredirait
     * l'autre.</p>
     */
    private static int pointsExcedentaires(SalarieReel salarie, PlanningContext contexte,
                                           long centiemesPonderes) {
        Double attendues = EcartAuContrat.minutesAttendues(
                salarie.getContrat(), contexte.getHorizonTemporel());

        Double ecart = EcartAuContrat.ecartPourcent(centiemesPonderes / 100.0, attendues);
        return ecart == null ? 0 : contexte.getToleranceEquite().pointsExcedentaires(ecart);
    }

    /**
     * Minutes pondérées d'un créneau, en centièmes — les collecteurs comptent des entiers.
     *
     * <p>Répartition et pondération viennent des mêmes classes que {@code heuresPonderees} : deux
     * implémentations de la même règle auraient fini par diverger.</p>
     */
    private static long centiemesPonderes(Creneau creneau, PlanningContext contexte,
                                          RegulatoryParameters reglementaire) {
        return Math.round(100.0 * RepartitionPenibilites
                .de(DECOUPAGE.compute(creneau, reglementaire, true),
                        contexte.getDominancePenibilites())
                .minutesPondereesPar(contexte.getCoefficientsPenibilite()));
    }

    /** Sans volume contractuel déclaré, rien n'est comparable — et rien n'est pesé. */
    private static boolean contratMesurable(SalarieReel salarie) {
        return salarie.getContrat() != null
                && salarie.getContrat().getHeuresHebdomadairesHabituelles() != null
                && salarie.getContrat().getHeuresHebdomadairesHabituelles() > 0;
    }

    /**
     * Créneaux affectés qui comptent dans la charge — même périmètre que les bornes individuelles.
     * Segments de pause exclus, activités dont {@code compteDansCharge = false} exclues.
     */
    private static UniConstraintStream<Creneau> creneauxTravailles(ConstraintFactory factory) {
        return factory.forEach(Creneau.class)
                .filter(c -> c.getRessourceAffectee() instanceof SalarieReel)
                .filter(c -> !Boolean.TRUE.equals(c.getEstSegmentDePause()))
                .ifExists(ReferentielComptabiliteActivite.class,
                        Joiners.filtering((creneau, referentiel) ->
                                compteDansCharge(referentiel, creneau)));
    }

    /** Un segment de pause n'est pas du travail — même périmètre que les bornes individuelles. */
    private static boolean porteDuTravail(Creneau creneau) {
        return !Boolean.TRUE.equals(creneau.getEstSegmentDePause());
    }

    private static String ressourceAffecteeId(Creneau creneau) {
        return creneau.getRessourceAffectee() == null ? null : creneau.getRessourceAffectee().getId();
    }

    private static boolean compteDansCharge(ReferentielComptabiliteActivite referentiel,
                                            Creneau creneau) {
        ComptabiliteActivite activite = referentiel.getByCode(creneau.getCodeActiviteEffectif());
        return activite != null && activite.isCompteDansCharge();
    }
}
