package fr.project.planning.constraints.metier;

import fr.project.planning.domain.contexte.SeuilsSurcharge;
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
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * SurchargeAcceptable — contraintes SOFT, lot S3 de SC-02.
 *
 * <p>Le seuil de surcharge dit jusqu'où l'encadrement accepte d'aller pour couvrir une absence.
 * Arbitrage métier du 11/08 : <strong>traitement identique au plafond de dimanches</strong> — le
 * dépassement est signalé et pesé, jamais éliminatoire. Une borne de confort n'est pas une borne
 * de légalité ; celles-là restent HARD et gardent leur rôle.</p>
 *
 * <p>Deux règles, parce que la surcharge s'exprime de deux façons : <strong>en heures par
 * jour</strong> quand elle porte sur une journée, <strong>en heures par semaine</strong> quand la
 * semaine est également touchée. Un remplacement peut franchir l'une sans l'autre — une longue
 * journée dans une semaine creuse, une semaine chargée faite de journées ordinaires — et
 * l'encadrement ne juge pas les deux de la même manière.</p>
 *
 * <h3>Elles ne se déclenchent que si un seuil est déclaré</h3>
 * <p>Aucun {@link SeuilsSurcharge} au problème — donc tous les scénarios sauf SC-02, et SC-02
 * lui-même quand la demande n'en porte pas — et la jointure ne produit rien. Une borne absente
 * n'est pas une borne à zéro.</p>
 *
 * <h3>Périmètre du calcul</h3>
 * <p>Identique aux contraintes individuelles : segments de pause exclus, activités dont
 * {@code compteDansCharge = false} exclues. Le total porte sur <strong>tout ce que le salarié
 * travaille</strong> — son planning existant compris — et non sur le seul remplacement : c'est
 * bien sa charge du jour que le seuil borne, pas ce qu'on vient d'y ajouter.</p>
 */
public final class SurchargeAcceptable {

    /**
     * Pénalité par minute au-delà du seuil.
     *
     * <p><strong>Calibrée pour rester non éliminatoire</strong>, ce qui est la difficulté de
     * cette contrainte. Ne pas couvrir un créneau coûte 2 000 points forfaitaires
     * ({@code Penalites.nonAffectation}) ; une pénalité de surcharge trop lourde rendrait donc
     * l'abandon <em>moins cher</em> que la couverture, et le seuil de confort se comporterait en
     * interdit — l'inverse exact de l'arbitrage rendu.</p>
     *
     * <p>À 5 points la minute, une heure de dépassement coûte 300 : il faut plus de six heures
     * d'excédent pour égaler un créneau laissé à pourvoir. Le seuil départage donc deux
     * remplaçants possibles sans jamais dissuader de remplacer.</p>
     */
    private static final int PENALITE_PAR_MINUTE = 5;

    private SurchargeAcceptable() {
    }

    public static Constraint surchargeJournaliere(ConstraintFactory factory) {
        return factory
                .forEach(SalarieReel.class)
                .join(creneauxTravailles(factory),
                        Joiners.equal(SalarieReel::getId, c -> c.getRessourceAffectee().getId()))
                .groupBy(
                        (salarie, creneau) -> salarie,
                        (salarie, creneau) -> creneau.getDate(),
                        ConstraintCollectors.sum((salarie, creneau) -> creneau.getDuree()))
                .join(SeuilsSurcharge.class)
                .filter((salarie, jour, minutes, seuils) ->
                        excedent(minutes, seuils.minutesParJour()) > 0)
                .penalize(HardSoftScore.ONE_SOFT,
                        (salarie, jour, minutes, seuils) ->
                                PENALITE_PAR_MINUTE * excedent(minutes, seuils.minutesParJour()))
                .asConstraint(PenaliteKey.METIER_SOFT_SURCHARGE_JOUR.name());
    }

    public static Constraint surchargeHebdomadaire(ConstraintFactory factory) {
        return factory
                .forEach(SalarieReel.class)
                .join(creneauxTravailles(factory),
                        Joiners.equal(SalarieReel::getId, c -> c.getRessourceAffectee().getId()))
                .groupBy(
                        (salarie, creneau) -> salarie,
                        (salarie, creneau) -> lundiDeLaSemaine(creneau.getDate()),
                        ConstraintCollectors.sum((salarie, creneau) -> creneau.getDuree()))
                .join(SeuilsSurcharge.class)
                .filter((salarie, lundi, minutes, seuils) ->
                        excedent(minutes, seuils.minutesParSemaine()) > 0)
                .penalize(HardSoftScore.ONE_SOFT,
                        (salarie, lundi, minutes, seuils) ->
                                PENALITE_PAR_MINUTE * excedent(minutes, seuils.minutesParSemaine()))
                .asConstraint(PenaliteKey.METIER_SOFT_SURCHARGE_SEMAINE.name());
    }

    /** Créneaux affectés qui comptent dans la charge — même périmètre que les bornes individuelles. */
    private static org.optaplanner.core.api.score.stream.uni.UniConstraintStream<Creneau>
            creneauxTravailles(ConstraintFactory factory) {
        return factory.forEach(Creneau.class)
                .filter(c -> c.getRessourceAffectee() != null)
                .filter(c -> !Boolean.TRUE.equals(c.getEstSegmentDePause()))
                .ifExists(ReferentielComptabiliteActivite.class,
                        Joiners.filtering((creneau, ref) -> {
                            ComptabiliteActivite ca = ref.getByCode(creneau.getCodeActiviteEffectif());
                            return ca != null && ca.isCompteDansCharge();
                        }));
    }

    /** Minutes au-delà du seuil, ou 0 — y compris lorsque le seuil n'est pas déclaré. */
    private static int excedent(int minutes, Integer seuilMinutes) {
        if (seuilMinutes == null) {
            return 0;
        }
        return Math.max(0, minutes - seuilMinutes);
    }

    /**
     * Semaine calendaire lundi → dimanche, comme partout ailleurs dans le moteur.
     *
     * <p>Public parce que la restitution s'en sert aussi : la réponse doit regrouper les semaines
     * exactement comme la contrainte les regroupe, faute de quoi elle afficherait un total que le
     * score ne connaît pas.</p>
     */
    public static LocalDate lundiDeLaSemaine(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
