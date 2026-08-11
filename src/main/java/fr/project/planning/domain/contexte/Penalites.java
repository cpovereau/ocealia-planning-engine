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
     *Pénalité associée au nombre de nuits consécutives
     */
    private final int approcheMaxNuitsConsecutives;

    /**
     * Pénalité associée au dépassement du nombre de dimanches travaillés.
     */
    private final int depassementMaxDimanchesTravailles;

    /**
     * Pénalité associée au dépassement du nombre de jours consécutifs travaillés.
     */
    private final int depassementMaxJoursConsecutifs;

    /**
     * Pénalité associée à une alternance jour/nuit sur jours consécutifs (R5/R6).
     */
    private final int penaliteAlternanceJourNuit;

    /**
     * Pénalité par minute de dépassement de l'amplitude journalière maximale (R10).
     */
    private final int penaliteAmplitude;

    /**
     * Pénalité, par créneau, d'une nuit confiée à un salarié qui n'en fait pas (lot S8.2).
     *
     * <p>Forfaitaire — la clé {@code METIER_SOFT_NUIT_SALARIE_NON_NUIT} déclare l'unité
     * {@code OCCURRENCE}. Ce poids manquait : la contrainte pénalisait un point nu, seule du
     * paquet métier à ne pas passer par {@code Penalites}. Le lot S8.1, qui a rendu la plage de
     * nuit individuelle effective, l'a rendu déterminant — un veilleur déclarant une plage plus
     * large coûte davantage de minutes pénibles qu'un salarié non-nuit, et un point forfaitaire
     * ne compensait pas l'écart.</p>
     */
    private final int nuitSalarieNonNuit;

    /** Valeur retenue à défaut : couvre largement l'écart maximal de plage (~360 points). */
    private static final int NUIT_SALARIE_NON_NUIT_PAR_DEFAUT = 2_000;

    /** Constructeur historique — applique le poids par défaut de {@link #nuitSalarieNonNuit}. */
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
            int depassementMaxDimanchesTravailles,
            int depassementMaxJoursConsecutifs,
            int penaliteAlternanceJourNuit,
            int penaliteAmplitude
    ) {
        this(violationPhysique, violationLegale, violationMetier, violationService,
                violationPersonnelle, affectationPosteVirtuel, nonAffectation,
                detteReposSurReposHebdomadaire, approcheMaxNuitsConsecutives,
                depassementMaxDimanchesTravailles, depassementMaxJoursConsecutifs,
                penaliteAlternanceJourNuit, penaliteAmplitude,
                NUIT_SALARIE_NON_NUIT_PAR_DEFAUT);
    }

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
            int depassementMaxDimanchesTravailles,
            int depassementMaxJoursConsecutifs,
            int penaliteAlternanceJourNuit,
            int penaliteAmplitude,
            int nuitSalarieNonNuit
    ) {
        this.nuitSalarieNonNuit = nuitSalarieNonNuit;
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
        this.depassementMaxJoursConsecutifs = depassementMaxJoursConsecutifs;
        this.penaliteAlternanceJourNuit = penaliteAlternanceJourNuit;
        this.penaliteAmplitude = penaliteAmplitude;
    }

    public int getNuitSalarieNonNuit() {
        return nuitSalarieNonNuit;
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

    public int getDepassementMaxJoursConsecutifs() {
        return depassementMaxJoursConsecutifs;
    }

    public int getPenaliteAlternanceJourNuit() {
        return penaliteAlternanceJourNuit;
    }

    public int getPenaliteAmplitude() {
        return penaliteAmplitude;
    }
}
