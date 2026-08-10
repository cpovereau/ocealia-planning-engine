package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.HorizonTemporel;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * HeuresMinimumParSemaine — sous-emploi hebdomadaire, contrainte SOFT en deux volets (lot S7.7)
 *
 * <p>Pénalise les minutes qui manquent à un salarié pour atteindre son volume hebdomadaire
 * contractuel minimal. C'est la seule règle du chantier S7 qui pousse le solveur à
 * <em>donner</em> du travail plutôt qu'à en retirer.</p>
 *
 * <h3>Pourquoi deux volets</h3>
 * <p>Le regroupement par {@code (salarié, semaine)} ne produit de tuple que là où le salarié a au
 * moins un créneau. Pris seul, il produit exactement l'incitation inverse de celle recherchée :
 * ne rien confier à un salarié ne coûte rien, tandis que lui confier trop peu coûte le déficit
 * entier. Le solveur préfère alors ne pas l'employer — comportement effectivement observé sur le
 * jeu SC-03, où les six créneaux sont partis au poste virtuel.</p>
 *
 * <p>Le second volet, {@link #semaineSansAffectation(ConstraintFactory)}, énumère les semaines
 * complètes de l'horizon et pénalise celles où le salarié n'a rien. Le déficit y est maximal —
 * l'ordre des coûts redevient correct, et toute affectation améliore le score.</p>
 *
 * <h3>Seules les semaines complètes sont évaluées</h3>
 * <p>Un minimum a un mode de défaillance dangereux, inverse de celui d'un maximum : sur une
 * semaine tronquée le total est mécaniquement sous-évalué, et la règle signalerait un déficit qui
 * n'existe pas. Un maximum peut se permettre de mesurer ce qu'il reçoit — il sous-détecte, ce qui
 * est sans danger ; un minimum, non. Seules les semaines dont le lundi <strong>et</strong> le
 * dimanche tombent dans l'horizon sont donc jugées.</p>
 *
 * <h3>Définition de la semaine</h3>
 * <p><strong>Lundi → dimanche calendaire</strong>, comme {@link HeuresMaximumParSemaine}, dont
 * cette règle est le pendant.</p>
 *
 * <h3>Pénalité</h3>
 * <p>{@code PENALITE} × minutes manquantes. Coefficient aligné sur {@link HeuresMinimumParJour} :
 * même nature de déviation, par rapport à une borne contractuelle basse.</p>
 */
public class HeuresMinimumParSemaine {

    private HeuresMinimumParSemaine() {
        // Utility class
    }

    /** Pénalité par minute manquante — aligné sur {@code HeuresMinimumParJour}. */
    private static final int PENALITE = 50;

    // =====================================================================
    // Volet 1 — le salarié travaille cette semaine, mais pas assez
    // =====================================================================

    public static Constraint heuresMinimumParSemaine(ConstraintFactory factory) {

        return factory
            .forEach(SalarieReel.class)
            .filter(HeuresMinimumParSemaine::minimumRenseigne)

            // Créneaux affectés à ce salarié, pauses exclues
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null)
                    .filter(c -> !Boolean.TRUE.equals(c.getEstSegmentDePause())),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // Uniquement les créneaux travaillés (compteDansCharge = true)
            .ifExists(
                ReferentielComptabiliteActivite.class,
                Joiners.filtering((salarie, creneau, ref) -> compteDansCharge(ref, creneau))
            )

            // (salarié, semaine) → somme des durées
            .groupBy(
                (salarie, creneau) -> salarie,
                (salarie, creneau) -> HeuresMaximumParSemaine.lundiDeLaSemaine(creneau.getDate()),
                ConstraintCollectors.sum((salarie, creneau) -> creneau.getDuree())
            )

            // L'horizon dit quelles semaines sont complètes
            .join(factory.forEach(PlanningContext.class))
            .filter((salarie, lundi, totalMinutes, context) ->
                    semaineComplete(lundi, context.getHorizonTemporel()))

            // Seuls les déficits produisent un match
            .filter((salarie, lundi, totalMinutes, context) -> totalMinutes < minimumMinutes(salarie))

            .penalize(
                HardSoftScore.ONE_SOFT,
                (salarie, lundi, totalMinutes, context) ->
                        PENALITE * (minimumMinutes(salarie) - totalMinutes)
            )
            .asConstraint(PenaliteKey.LEGAL_SOFT_HEURES_MIN_PAR_SEMAINE.name());
    }

    // =====================================================================
    // Volet 2 — le salarié ne travaille pas du tout cette semaine
    // =====================================================================

    /**
     * Pénalise une semaine complète sans la moindre affectation, au déficit maximal.
     *
     * <p>Sans ce volet, l'absence totale d'affectation serait gratuite et le solveur en ferait sa
     * solution préférée. C'est le seul moyen, en Constraint Streams, de faire exister un tuple
     * pour un salarié qui n'a aucun créneau : les jointures étant internes, il faut énumérer les
     * semaines depuis l'horizon puis exclure celles qui sont pourvues.</p>
     *
     * <p><strong>Approximation assumée</strong> : le test d'existence ne consulte pas le
     * référentiel d'activités. Une semaine entièrement remplie d'activités hors charge — de la
     * formation, par exemple — est donc vue comme pourvue, et relève du volet 1 avec un total
     * nul. Le déficit reste signalé, sous l'autre clé.</p>
     */
    public static Constraint semaineSansAffectation(ConstraintFactory factory) {

        return factory
            .forEach(SalarieReel.class)
            .filter(HeuresMinimumParSemaine::minimumRenseigne)

            .join(factory.forEach(PlanningContext.class))
            .flattenLast(context -> semainesCompletes(context.getHorizonTemporel()))

            .ifNotExists(
                Creneau.class,
                Joiners.filtering((salarie, lundi, creneau) ->
                        creneau.getRessourceAffectee() != null
                                && salarie.getId().equals(creneau.getRessourceAffectee().getId())
                                && lundi.equals(HeuresMaximumParSemaine.lundiDeLaSemaine(creneau.getDate())))
            )

            .penalize(
                HardSoftScore.ONE_SOFT,
                (salarie, lundi) -> PENALITE * minimumMinutes(salarie)
            )
            .asConstraint(PenaliteKey.LEGAL_SOFT_SEMAINE_SANS_AFFECTATION.name());
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static boolean compteDansCharge(ReferentielComptabiliteActivite ref, Creneau creneau) {
        ComptabiliteActivite ca = ref.getByCode(creneau.getCodeActiviteEffectif());
        return ca != null && ca.isCompteDansCharge();
    }

    /** Lundis des semaines dont les sept jours tombent dans l'horizon. */
    static List<LocalDate> semainesCompletes(HorizonTemporel horizon) {
        List<LocalDate> lundis = new ArrayList<>();
        LocalDate lundi = HeuresMaximumParSemaine.lundiDeLaSemaine(horizon.getDateDebut());
        while (!lundi.isAfter(horizon.getDateFin())) {
            if (semaineComplete(lundi, horizon)) {
                lundis.add(lundi);
            }
            lundi = lundi.plusWeeks(1);
        }
        return lundis;
    }

    private static boolean semaineComplete(LocalDate lundi, HorizonTemporel horizon) {
        return horizon.contient(lundi) && horizon.contient(lundi.plusDays(6));
    }

    private static boolean minimumRenseigne(SalarieReel salarie) {
        return ContraintesReglementairesSalarie.borneRenseignee(
                salarie.contraintesOuAucune().getHeuresMinimumParSemaine());
    }

    /** N'est appelé qu'après {@link #minimumRenseigne(SalarieReel)} : la valeur est non nulle. */
    private static int minimumMinutes(SalarieReel salarie) {
        return (int) (salarie.contraintesOuAucune().getHeuresMinimumParSemaine() * 60);
    }
}
