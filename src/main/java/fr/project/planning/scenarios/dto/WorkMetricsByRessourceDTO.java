package fr.project.planning.scenarios.dto;

import java.time.LocalDate;

public class WorkMetricsByRessourceDTO {

    private String resourceId;
    private LocalDate periodeDebut;
    private LocalDate periodeFin;

    private double heuresTravaillees;
    private double heuresNuit;
    private double heuresJourFerie;
    private double heuresReposHebdoTravaille;

    private int nbDimanchesTravailles;
    private int maxJoursConsecutifsObservees;
    private int maxNuitsConsecutivesObservees;

    public WorkMetricsByRessourceDTO() {
    }

    public WorkMetricsByRessourceDTO(
            String resourceId,
            LocalDate periodeDebut,
            LocalDate periodeFin,
            double heuresTravaillees,
            double heuresNuit,
            double heuresJourFerie,
            double heuresReposHebdoTravaille,
            int nbDimanchesTravailles,
            int maxJoursConsecutifsObservees,
            int maxNuitsConsecutivesObservees
    ) {
        this.resourceId = resourceId;
        this.periodeDebut = periodeDebut;
        this.periodeFin = periodeFin;
        this.heuresTravaillees = heuresTravaillees;
        this.heuresNuit = heuresNuit;
        this.heuresJourFerie = heuresJourFerie;
        this.heuresReposHebdoTravaille = heuresReposHebdoTravaille;
        this.nbDimanchesTravailles = nbDimanchesTravailles;
        this.maxJoursConsecutifsObservees = maxJoursConsecutifsObservees;
        this.maxNuitsConsecutivesObservees = maxNuitsConsecutivesObservees;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public LocalDate getPeriodeDebut() {
        return periodeDebut;
    }

    public void setPeriodeDebut(LocalDate periodeDebut) {
        this.periodeDebut = periodeDebut;
    }

    public LocalDate getPeriodeFin() {
        return periodeFin;
    }

    public void setPeriodeFin(LocalDate periodeFin) {
        this.periodeFin = periodeFin;
    }

    public double getHeuresTravaillees() {
        return heuresTravaillees;
    }

    public void setHeuresTravaillees(double heuresTravaillees) {
        this.heuresTravaillees = heuresTravaillees;
    }

    public double getHeuresNuit() {
        return heuresNuit;
    }

    public void setHeuresNuit(double heuresNuit) {
        this.heuresNuit = heuresNuit;
    }

    public double getHeuresJourFerie() {
        return heuresJourFerie;
    }

    public void setHeuresJourFerie(double heuresJourFerie) {
        this.heuresJourFerie = heuresJourFerie;
    }

    public double getHeuresReposHebdoTravaille() {
        return heuresReposHebdoTravaille;
    }

    public void setHeuresReposHebdoTravaille(double heuresReposHebdoTravaille) {
        this.heuresReposHebdoTravaille = heuresReposHebdoTravaille;
    }

    public int getNbDimanchesTravailles() {
        return nbDimanchesTravailles;
    }

    public void setNbDimanchesTravailles(int nbDimanchesTravailles) {
        this.nbDimanchesTravailles = nbDimanchesTravailles;
    }

    public int getMaxJoursConsecutifsObservees() {
        return maxJoursConsecutifsObservees;
    }

    public void setMaxJoursConsecutifsObservees(int maxJoursConsecutifsObservees) {
        this.maxJoursConsecutifsObservees = maxJoursConsecutifsObservees;
    }

    public int getMaxNuitsConsecutivesObservees() {
        return maxNuitsConsecutivesObservees;
    }

    public void setMaxNuitsConsecutivesObservees(int maxNuitsConsecutivesObservees) {
        this.maxNuitsConsecutivesObservees = maxNuitsConsecutivesObservees;
    }
}