package fr.project.planning.domain.contexte;

import java.io.Serializable;

/**
 * Penalites
 *
 * Définit la sévérité relative des violations.
 * Ces valeurs sont utilisées comme poids dans les contraintes OptaPlanner.
 *
 * Elles n'activent ni ne désactivent jamais une règle :
 * elles expriment uniquement ce qui est "pire que quoi".
 */
public final class Penalites implements Serializable {

    /**
     * Pénalité associée à une violation physique.
     * Toujours considérée comme bloquante (HARD).
     */
    private final int violationPhysique;

    /**
     * Pénalité associée à une violation légale.
     * Peut être HARD ou SOFT selon le contexte.
     */
    private final int violationLegale;

    /**
     * Pénalité associée à une violation métier.
     */
    private final int violationMetier;

    /**
     * Pénalité associée à une violation de service
     * (continuité, couverture minimale, etc.).
     */
    private final int violationService;

    /**
     * Pénalité associée à une préférence personnelle non respectée.
     */
    private final int violationPersonnelle;

    /**
     * Pénalité associée à l'utilisation d'un poste virtuel.
     */
    private final int affectationPosteVirtuel;

    /**
     * Pénalité associée à un créneau non affecté.
     */
    private final int nonAffectation;

    /**
     * Pénalité associée à la dette de repos sur repos hebdomadaire.
     */
    private final int detteReposSurReposHebdomadaire;

    /** 
    / *Pénalité associée au nombre de nuits consécutives
     */
    private int approcheMaxNuitsConsecutives;

    /**
     * Pénalité associée au dépassement du nombre de dimanches travaillés.
     */
    private int depassementMaxDimanchesTravailles;

    public Penalites(
            int violationPhysique,
            int violationLegale,
            int violationMetier,
            int violationService,
            int violationPersonnelle,
            int affectationPosteVirtuel,
            int nonAffectation,
            int detteReposSurReposHebdomadaire,
            int approcheMaxNuitsConsecutives,
            int depassementMaxDimanchesTravailles
    ) {
        this.violationPhysique = violationPhysique;
        this.violationLegale = violationLegale;
        this.violationMetier = violationMetier;
        this.violationService = violationService;
        this.violationPersonnelle = violationPersonnelle;
        this.affectationPosteVirtuel = affectationPosteVirtuel;
        this.nonAffectation = nonAffectation;
        this.detteReposSurReposHebdomadaire = detteReposSurReposHebdomadaire;
        this.approcheMaxNuitsConsecutives = approcheMaxNuitsConsecutives;
        this.depassementMaxDimanchesTravailles = depassementMaxDimanchesTravailles;
    }

    public int getViolationPhysique() {
        return violationPhysique;
    }

    public int getViolationLegale() {
        return violationLegale;
    }

    public int getViolationMetier() {
        return violationMetier;
    }

    public int getViolationService() {
        return violationService;
    }

    public int getViolationPersonnelle() {
        return violationPersonnelle;
    }

    public int getAffectationPosteVirtuel() {
        return affectationPosteVirtuel;
    }

    public int getNonAffectation() {
        return nonAffectation;
    }

    public int getDetteReposSurReposHebdomadaire() {
        return detteReposSurReposHebdomadaire;
    }

    public int getApprocheMaxNuitsConsecutives() {
    return approcheMaxNuitsConsecutives;
    }

    public int getDepassementMaxDimanchesTravailles() {
    return depassementMaxDimanchesTravailles;
    }
}
