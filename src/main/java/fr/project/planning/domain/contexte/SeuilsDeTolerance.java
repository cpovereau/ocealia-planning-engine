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
    // Seuils en cours de rapatriement vers le salarié (lot S7)
    //
    // Ces cinq champs n'ont jamais été alimentés : aucun constructeur ne
    // les prend en argument et defaultSeuilsDeTolerance() n'y touche pas,
    // ils valent donc 0 en production. Ils relèvent du contrat de la
    // personne, pas du contexte de calcul, et migrent vers
    // ContraintesReglementairesSalarie, un lot par contrainte.
    // Chaque champ disparaît quand sa contrainte a basculé.
    // ================================================================

    /**
     * @deprecated Lot S7 — remplacé par
     *             {@code ContraintesReglementairesSalarie.nuitsConsecutivesMaximum}.
     */
    @Deprecated(forRemoval = true)
    private int maxNuitsConsecutives;

    /**
     * @deprecated Lot S7 — remplacé par
     *             {@code ContraintesReglementairesSalarie.joursReposMinimumApresNuits}.
     */
    @Deprecated(forRemoval = true)
    private int reposApresNuitsEnJours;

    /**
     * @deprecated Lot S7 — remplacé par
     *             {@code ContraintesReglementairesSalarie.reposHebdomadaireFenetreJours}.
     */
    @Deprecated(forRemoval = true)
    private int reposHebdoFenetreJours;

    /**
     * @deprecated Lot S7 — remplacé par
     *             {@code ContraintesReglementairesSalarie.reposHebdomadaireJoursOffMinimum}.
     */
    @Deprecated(forRemoval = true)
    private int reposHebdoMinJoursOffDansFenetre;

    /**
     * @deprecated Lot S7 — remplacé par
     *             {@code ContraintesReglementairesSalarie.dimanchesTravaillesMaximum}.
     */
    @Deprecated(forRemoval = true)
    private int maxDimanchesTravailles;



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

    /** @deprecated Lot S7 — voir {@link #maxNuitsConsecutives}. */
    @Deprecated(forRemoval = true)
    public int getMaxNuitsConsecutives() {
        return maxNuitsConsecutives;
    }

    /** @deprecated Lot S7 — voir {@link #reposApresNuitsEnJours}. */
    @Deprecated(forRemoval = true)
    public int getReposApresNuitsEnJours() {
        return reposApresNuitsEnJours;
    }

    /** @deprecated Lot S7 — voir {@link #reposHebdoFenetreJours}. */
    @Deprecated(forRemoval = true)
    public int getReposHebdoFenetreJours() {
        return reposHebdoFenetreJours;
    }

    /** @deprecated Lot S7 — voir {@link #reposHebdoMinJoursOffDansFenetre}. */
    @Deprecated(forRemoval = true)
    public int getReposHebdoMinJoursOffDansFenetre() {
        return reposHebdoMinJoursOffDansFenetre;
    }

    /** @deprecated Lot S7 — voir {@link #maxDimanchesTravailles}. */
    @Deprecated(forRemoval = true)
    public int getMaxDimanchesTravailles() {
        return maxDimanchesTravailles;
    }

    /** @deprecated Lot S7 — voir {@link #maxDimanchesTravailles}. */
    @Deprecated(forRemoval = true)
    public void setMaxDimanchesTravailles(int maxDimanchesTravailles) {
        this.maxDimanchesTravailles = maxDimanchesTravailles;
    }
}
