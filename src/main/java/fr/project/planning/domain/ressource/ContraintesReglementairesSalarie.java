package fr.project.planning.domain.ressource;

/**
 * ContraintesReglementairesSalarie — objet valeur domaine.
 *
 * Représente les 8 contraintes réglementaires individuelles d'un salarié.
 * Transportées depuis WinDev en Phase 1, mappées vers le domaine en Phase 3.
 * Exploitées par le solveur à partir de Phase 4 (règles d'incompatibilité).
 */
public class ContraintesReglementairesSalarie {

    private final Double heuresMinimumParJour;
    private final Double heuresMaximumParJour;
    private final Double amplitudeJournaliereMaximum;
    private final Double reposQuotidienMinimum;
    private final Double heuresMinimumParSemaine;
    private final Double heuresMaximumParSemaine;
    private final Integer nuitsMaximumParSemaine;
    private final Integer joursConsecutifsMaximum;

    public ContraintesReglementairesSalarie(
            Double heuresMinimumParJour,
            Double heuresMaximumParJour,
            Double amplitudeJournaliereMaximum,
            Double reposQuotidienMinimum,
            Double heuresMinimumParSemaine,
            Double heuresMaximumParSemaine,
            Integer nuitsMaximumParSemaine,
            Integer joursConsecutifsMaximum
    ) {
        this.heuresMinimumParJour = heuresMinimumParJour;
        this.heuresMaximumParJour = heuresMaximumParJour;
        this.amplitudeJournaliereMaximum = amplitudeJournaliereMaximum;
        this.reposQuotidienMinimum = reposQuotidienMinimum;
        this.heuresMinimumParSemaine = heuresMinimumParSemaine;
        this.heuresMaximumParSemaine = heuresMaximumParSemaine;
        this.nuitsMaximumParSemaine = nuitsMaximumParSemaine;
        this.joursConsecutifsMaximum = joursConsecutifsMaximum;
    }

    public Double getHeuresMinimumParJour() { return heuresMinimumParJour; }
    public Double getHeuresMaximumParJour() { return heuresMaximumParJour; }
    public Double getAmplitudeJournaliereMaximum() { return amplitudeJournaliereMaximum; }
    public Double getReposQuotidienMinimum() { return reposQuotidienMinimum; }
    public Double getHeuresMinimumParSemaine() { return heuresMinimumParSemaine; }
    public Double getHeuresMaximumParSemaine() { return heuresMaximumParSemaine; }
    public Integer getNuitsMaximumParSemaine() { return nuitsMaximumParSemaine; }
    public Integer getJoursConsecutifsMaximum() { return joursConsecutifsMaximum; }
}