package fr.project.planning.scenarios.dto;

public class SolutionSummaryDTO {

    private int nbCreneaux;
    private int nbCreneauxAffectes;
    private int nbCreneauxNonAffectes;
    private int nbRessourcesMobilisees;
    private double heuresTravailleesTotales;

    public SolutionSummaryDTO() {}

    public int getNbCreneaux() {
        return nbCreneaux;
    }

    public void setNbCreneaux(int nbCreneaux) {
        this.nbCreneaux = nbCreneaux;
    }

    public int getNbCreneauxAffectes() {
        return nbCreneauxAffectes;
    }

    public void setNbCreneauxAffectes(int nbCreneauxAffectes) {
        this.nbCreneauxAffectes = nbCreneauxAffectes;
    }

    public int getNbCreneauxNonAffectes() {
        return nbCreneauxNonAffectes;
    }

    public void setNbCreneauxNonAffectes(int nbCreneauxNonAffectes) {
        this.nbCreneauxNonAffectes = nbCreneauxNonAffectes;
    }

    public int getNbRessourcesMobilisees() {
        return nbRessourcesMobilisees;
    }

    public void setNbRessourcesMobilisees(int nbRessourcesMobilisees) {
        this.nbRessourcesMobilisees = nbRessourcesMobilisees;
    }

    public double getHeuresTravailleesTotales() {
        return heuresTravailleesTotales;
    }

    public void setHeuresTravailleesTotales(double heuresTravailleesTotales) {
        this.heuresTravailleesTotales = heuresTravailleesTotales;
    }
}