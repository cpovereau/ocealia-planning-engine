package fr.project.planning.scenarios.dto.input;

/**
 * ContratSalarieDTO — lot S2, bloc {@code contrat} du salarié.
 *
 * <p>Décrit ce que le salarié <strong>fait normalement</strong>. À ne pas confondre avec
 * {@link ContraintesReglementairesDTO}, qui décrit ce à quoi il <strong>est soumis</strong> :
 * le premier est descriptif, le second prescriptif. Distinction arbitrée au §6.6 de
 * {@code 92_cadrage_donnees_amont_scenarios.md}.</p>
 *
 * <h3>Champs volontairement absents</h3>
 * <ul>
 *   <li><strong>Dates de début et de fin de contrat</strong> — écartées. Le filtrage des salariés
 *       hors contrat à la date visée relève de WinDev, en amont du moteur.</li>
 *   <li><strong>{@code travailDeNuit} et {@code travailleJourFerie}</strong> — maintenus à la
 *       racine de {@link SalarieInputDTO}, où ils sont déjà exploités par deux contraintes.
 *       Les déplacer serait une rupture de contrat pour SC-03 sans contrepartie.</li>
 * </ul>
 *
 * <p>Contrat strict : aucun {@code @JsonIgnoreProperties}, conformément au durcissement
 * Phase 10B. Un champ inconnu dans ce bloc est une erreur, pas un oubli toléré.</p>
 *
 * <p>Lot S2 : transport et mapping uniquement. Aucune contrainte ne lit ce bloc.
 * Voir {@code 92_cadrage_scenario_sc-06.md} §4.6.</p>
 */
public class ContratSalarieDTO {

    /** Durée journalière travaillée moyenne, en heures. */
    private Double heuresMoyennesParJour;

    /** Durée hebdomadaire habituelle, en heures. */
    private Double heuresHebdomadairesHabituelles;

    /** Nombre de jours travaillés par semaine. Défaut 5 appliqué par le domaine si absent. */
    private Integer joursTravaillesParSemaine;

    /**
     * Salarié annualisé.
     *
     * <p>Transporté, <strong>non exploité</strong>. Cible : un dépassement hebdomadaire est moins
     * grave pour un salarié annualisé, le critère se déplaçant vers le cumul d'heures excédentaires
     * sur la période d'annualisation. Deux données manquent encore au contrat pour cela — les bornes
     * de la période et le cumul à date. Voir {@code 92_cadrage_scenario_sc-06.md} §10.1.</p>
     */
    private Boolean estAnnualise;

    // =========================
    // Getters / Setters
    // =========================

    public Double getHeuresMoyennesParJour() { return heuresMoyennesParJour; }
    public void setHeuresMoyennesParJour(Double heuresMoyennesParJour) { this.heuresMoyennesParJour = heuresMoyennesParJour; }

    public Double getHeuresHebdomadairesHabituelles() { return heuresHebdomadairesHabituelles; }
    public void setHeuresHebdomadairesHabituelles(Double heuresHebdomadairesHabituelles) { this.heuresHebdomadairesHabituelles = heuresHebdomadairesHabituelles; }

    public Integer getJoursTravaillesParSemaine() { return joursTravaillesParSemaine; }
    public void setJoursTravaillesParSemaine(Integer joursTravaillesParSemaine) { this.joursTravaillesParSemaine = joursTravaillesParSemaine; }

    public Boolean getEstAnnualise() { return estAnnualise; }
    public void setEstAnnualise(Boolean estAnnualise) { this.estAnnualise = estAnnualise; }
}
