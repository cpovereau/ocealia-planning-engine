package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.solution.PlanningProblem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkMetricsParTranche — la charge de chacun, semaine par semaine, mois par mois (lot O1 de SC-04).
 *
 * <h3>Le même calculateur, jamais un second</h3>
 * <p>Chaque tranche est mesurée en <strong>rejouant {@link WorkMetricsCalculator}</strong> sur une
 * vue du problème dont seul l'horizon est rétréci. Écrire un agrégateur qui additionnerait les
 * créneaux lui-même aurait été plus court et faux : la pondération par la pénibilité, la dominance,
 * le filtre {@code compteDansCharge} et la déduction des absences offrent quatre occasions de
 * diverger, et <strong>une divergence de méthode se lirait comme un mouvement du planning</strong>.
 * C'est la leçon du lot A2 de SC-05, et l'arbitrage §5.1 du cadrage SC-04 : <em>on ne compare pas
 * des torchons et des serviettes</em>.</p>
 *
 * <h3>Ce que chaque tranche mesure vraiment</h3>
 * <p>Tout suit la fenêtre, y compris ce qui s'en déduit : les jours disponibles d'une semaine où le
 * salarié a pris deux jours valent cinq, et son écart au contrat se proratise sur ces cinq jours.
 * Une semaine de congé complet ne le montre donc pas à −100 % — elle ne le compare à rien.</p>
 *
 * <h3>⚠️ Ce qui ne s'additionne pas d'une tranche à l'autre</h3>
 * <p>Les <strong>volumes</strong> se somment : les heures d'une période sont celles de ses semaines.
 * Les <strong>séries</strong> ne se somment pas et ne se maximisent pas non plus.
 * {@code maxJoursConsecutifsObservees} et {@code maxNuitsConsecutivesObservees} sont observés
 * <em>à l'intérieur</em> de la tranche : huit jours consécutifs à cheval sur deux semaines s'y
 * lisent 5 puis 3. <strong>Seule la tranche {@code PERIODE} porte la série réelle</strong>, et
 * c'est elle qu'il faut lire pour juger d'un enchaînement.</p>
 */
public final class WorkMetricsParTranche {

    private final WorkMetricsCalculator calculateur;

    public WorkMetricsParTranche() {
        this(new WorkMetricsCalculator());
    }

    public WorkMetricsParTranche(WorkMetricsCalculator calculateur) {
        this.calculateur = calculateur;
    }

    /**
     * La charge de chaque ressource, tranche par tranche.
     *
     * <p>L'ordre des tranches est celui de {@link DecoupageTemporel#decouper} — semaines, puis
     * mois, puis période — et la {@code Map} le conserve.</p>
     */
    public Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> calculer(PlanningProblem problem) {
        if (problem == null || problem.getPlanningContext() == null) {
            return Map.of();
        }
        List<TrancheTemporelle> tranches =
                DecoupageTemporel.decouper(problem.getPlanningContext().getHorizonTemporel());

        Map<TrancheTemporelle, Map<Ressource, WorkMetrics>> parTranche = new LinkedHashMap<>();
        for (TrancheTemporelle tranche : tranches) {
            parTranche.put(tranche, calculateur.compute(vueSur(problem, tranche)));
        }
        return parTranche;
    }

    /**
     * Le problème vu à travers la tranche — mêmes faits, fenêtre plus étroite.
     *
     * <p>Rien n'est copié ni filtré : le calculateur écarte lui-même les créneaux hors horizon, et
     * les jours disponibles se recalculent sur la nouvelle fenêtre parce que
     * {@code PlanningProblem} les dérive au lieu de les recevoir.</p>
     */
    private static PlanningProblem vueSur(PlanningProblem problem, TrancheTemporelle tranche) {
        PlanningProblem vue = new PlanningProblem(
                problem.getPlanningContext().surHorizon(tranche.horizon()),
                problem.getRegulatoryParameters(),
                problem.getReferentielComptabiliteActivite(),
                problem.getRessources(),
                problem.getCreneaux(),
                problem.getIndisponibilites());
        vue.setReposHebdomadaires(problem.getReposHebdomadaires());
        return vue;
    }
}
