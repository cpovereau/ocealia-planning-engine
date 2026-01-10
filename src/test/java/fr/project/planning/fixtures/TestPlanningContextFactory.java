package fr.project.planning.fixtures;

import fr.project.planning.domain.contexte.*;

import java.time.LocalDate;

/**
 * TestPlanningContextFactory
 *
 * Factory normée de PlanningContext pour les tests V2 / V3 / V4.
 *
 * Règles :
 * - crée uniquement des PlanningContext
 * - ne connaît ni ressources, ni créneaux
 * - n'active / ne désactive aucune contrainte
 * - ne contient aucune logique métier
 */
public final class TestPlanningContextFactory {

    private TestPlanningContextFactory() {
        // utilitaire statique
    }

    // ============================================================
    // Méthodes nommées (lecture claire des intentions de test)
    // ============================================================

    /**
     * Contexte neutre standard :
     * - planning global
     * - historique neutre
     * - scoring d'exploitation
     */
    public static PlanningContext contexteNeutre(LocalDate debut, LocalDate fin) {
        return build(
                debut,
                fin,
                ResolutionType.PLANNING_GLOBAL,
                HypotheseHistorique.NEUTRE,
                StrategieScoring.EXPLOITATION,
                ObjectifResolution.EQUILIBRER
        );
    }

    /**
     * Contexte de cycle de travail.
     */
    public static PlanningContext contexteCycle(LocalDate debut, LocalDate fin) {
        return build(
                debut,
                fin,
                ResolutionType.CYCLE,
                HypotheseHistorique.NEUTRE,
                StrategieScoring.EXPLOITATION,
                ObjectifResolution.EQUILIBRER
        );
    }

    /**
     * Contexte de remplacement ponctuel.
     */
    public static PlanningContext contexteRemplacement(LocalDate debut, LocalDate fin) {
        return build(
                debut,
                fin,
                ResolutionType.REMPLACEMENT,
                HypotheseHistorique.NEUTRE,
                StrategieScoring.EXPLOITATION,
                ObjectifResolution.EQUILIBRER
        );
    }

    /**
     * Contexte de projection / simulation :
     * - passé ignoré
     * - scoring d'audit
     */
    public static PlanningContext contexteProjection(LocalDate debut, LocalDate fin) {
        return build(
                debut,
                fin,
                ResolutionType.PROJECTION,
                HypotheseHistorique.IGNORER_ETAT_PASSE,
                StrategieScoring.AUDIT,
                ObjectifResolution.ANALYSER_LE_MANQUE
        );
    }

    // ============================================================
    // Point d'entrée générique (unique endroit où on instancie)
    // ============================================================

    public static PlanningContext build(
            LocalDate debut,
            LocalDate fin,
            ResolutionType resolutionType,
            HypotheseHistorique hypotheseHistorique,
            StrategieScoring strategieScoring,
            ObjectifResolution objectifResolution
    ) {
        return new PlanningContext(
                objectifResolution,
                strategieScoring,
                debut,
                fin,
                resolutionType,
                hypotheseHistorique
        );
    }
}
