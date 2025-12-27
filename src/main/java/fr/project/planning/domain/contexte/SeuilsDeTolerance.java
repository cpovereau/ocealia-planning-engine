package fr.project.planning.domain.contexte;

import java.io.Serializable;
import java.util.Objects;

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

    /**
     * Nome de nuit maximale par salarié
     * dans une solution globale.
     */
    private int maxNuitsConsecutives;


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

    public int getMaxNuitsConsecutives() {
        return maxNuitsConsecutives;
    }
}
