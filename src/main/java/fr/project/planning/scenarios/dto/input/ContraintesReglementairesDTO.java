package fr.project.planning.scenarios.dto.input;

/**
 * ContraintesReglementairesDTO — Phase 1 transport uniquement.
 *
 * Transporte les 8 contraintes réglementaires individuelles d'un salarié
 * transmises par WinDev. Non exploitées par le builder avant Phase 3.
 *
 * Correspondance avec le schéma (ScenarioContract.schema.json §salarie.contraintesReglementaires) :
 *   heuresMinimumParJour        → borne basse journalière (h)
 *   heuresMaximumParJour        → borne haute journalière (h)
 *   amplitudeJournaliereMaximum → amplitude max (début 1er → fin dernier créneau) (h)
 *   reposQuotidienMinimum       → repos entre deux journées consécutives (h)
 *   heuresMinimumParSemaine     → borne basse hebdomadaire (h)
 *   heuresMaximumParSemaine     → borne haute hebdomadaire (h)
 *   nuitsMaximumParSemaine      → nuits autorisées par semaine
 *   joursConsecutifsMaximum     → jours consécutifs travaillés max
 */
public class ContraintesReglementairesDTO {

    private Double heuresMinimumParJour;
    private Double heuresMaximumParJour;
    private Double amplitudeJournaliereMaximum;
    private Double reposQuotidienMinimum;
    private Double heuresMinimumParSemaine;
    private Double heuresMaximumParSemaine;
    private Integer nuitsMaximumParSemaine;
    private Integer joursConsecutifsMaximum;

    public Double getHeuresMinimumParJour() { return heuresMinimumParJour; }
    public void setHeuresMinimumParJour(Double heuresMinimumParJour) { this.heuresMinimumParJour = heuresMinimumParJour; }

    public Double getHeuresMaximumParJour() { return heuresMaximumParJour; }
    public void setHeuresMaximumParJour(Double heuresMaximumParJour) { this.heuresMaximumParJour = heuresMaximumParJour; }

    public Double getAmplitudeJournaliereMaximum() { return amplitudeJournaliereMaximum; }
    public void setAmplitudeJournaliereMaximum(Double amplitudeJournaliereMaximum) { this.amplitudeJournaliereMaximum = amplitudeJournaliereMaximum; }

    public Double getReposQuotidienMinimum() { return reposQuotidienMinimum; }
    public void setReposQuotidienMinimum(Double reposQuotidienMinimum) { this.reposQuotidienMinimum = reposQuotidienMinimum; }

    public Double getHeuresMinimumParSemaine() { return heuresMinimumParSemaine; }
    public void setHeuresMinimumParSemaine(Double heuresMinimumParSemaine) { this.heuresMinimumParSemaine = heuresMinimumParSemaine; }

    public Double getHeuresMaximumParSemaine() { return heuresMaximumParSemaine; }
    public void setHeuresMaximumParSemaine(Double heuresMaximumParSemaine) { this.heuresMaximumParSemaine = heuresMaximumParSemaine; }

    public Integer getNuitsMaximumParSemaine() { return nuitsMaximumParSemaine; }
    public void setNuitsMaximumParSemaine(Integer nuitsMaximumParSemaine) { this.nuitsMaximumParSemaine = nuitsMaximumParSemaine; }

    public Integer getJoursConsecutifsMaximum() { return joursConsecutifsMaximum; }
    public void setJoursConsecutifsMaximum(Integer joursConsecutifsMaximum) { this.joursConsecutifsMaximum = joursConsecutifsMaximum; }
}
