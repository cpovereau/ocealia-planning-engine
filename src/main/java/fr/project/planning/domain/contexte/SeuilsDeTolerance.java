package fr.project.planning.domain.contexte;

import java.io.Serializable;

/**
 * SeuilsDeTolerance
 *
 * Définit les limites d'acceptabilité globale d'une solution.
 * Ces seuils ne sont PAS des règles, mais des bornes métier
 * utilisées par les contraintes pour juger une situation.
 */
public final class SeuilsDeTolerance implements Serializable {

    /**
     * Charge maximale acceptable pour un salarié réel
     * sur la période de résolution (en minutes ou heures).
     */
    private final int surchargeMaxParSalarie;

    /**
     * Nombre maximal de violations légales tolérées
     * dans une solution globale.
     */
    private final int violationsLegalesMax;

    /**
     * Nombre maximal de violations métier tolérées
     * dans une solution globale.
     */
    private final int violationsMetierMax;

    // ================================================================
    // [S7.8] Cinq seuils réglementaires ont été retirés d'ici.
    //
    // maxNuitsConsecutives, reposApresNuitsEnJours, reposHebdoFenetreJours,
    // reposHebdoMinJoursOffDansFenetre et maxDimanchesTravailles relevaient du
    // contrat de la personne, pas du contexte de calcul. Aucun constructeur ne
    // les prenait en argument : ils valaient 0 en production, et les contraintes
    // qui les lisaient s'en trouvaient neutralisées sans que rien ne le signale.
    // Ils vivent désormais dans ContraintesReglementairesSalarie, alimentés par
    // le contrat d'entrée, un seuil par salarié (lots S7.0 à S7.7).
    //
    // Ce qui reste ici est bien global : des bornes d'acceptabilité de la
    // solution, pas des règles applicables à une personne.
    // ================================================================

    public SeuilsDeTolerance(
            int surchargeMaxParSalarie,
            int violationsLegalesMax,
            int violationsMetierMax
    ) {
        if (surchargeMaxParSalarie < 0) {
            throw new IllegalArgumentException("surchargeMaxParSalarie must be >= 0");
        }
        if (violationsLegalesMax < 0) {
            throw new IllegalArgumentException("violationsLegalesMax must be >= 0");
        }
        if (violationsMetierMax < 0) {
            throw new IllegalArgumentException("violationsMetierMax must be >= 0");
        }

        this.surchargeMaxParSalarie = surchargeMaxParSalarie;
        this.violationsLegalesMax = violationsLegalesMax;
        this.violationsMetierMax = violationsMetierMax;
    }

    public int getSurchargeMaxParSalarie() {
        return surchargeMaxParSalarie;
    }

    public int getViolationsLegalesMax() {
        return violationsLegalesMax;
    }

    public int getViolationsMetierMax() {
        return violationsMetierMax;
    }
}
