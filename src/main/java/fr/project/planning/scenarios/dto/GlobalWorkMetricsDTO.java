package fr.project.planning.scenarios.dto;

public class GlobalWorkMetricsDTO {

    private int nbCreneaux;
    private int nbCreneauxNonAffectes;
    private double heuresTravailleesTotales;
    private double heuresNuitTotales;
    private double heuresJourFerieTotales;

    public GlobalWorkMetricsDTO() {
    }

    public GlobalWorkMetricsDTO(
            int nbCreneaux,
            int nbCreneauxNonAffectes,
            double heuresTravailleesTotales,
            double heuresNuitTotales,
            double heuresJourFerieTotales
    ) {
        this.nbCreneaux = nbCreneaux;
        this.nbCreneauxNonAffectes = nbCreneauxNonAffectes;
        this.heuresTravailleesTotales = heuresTravailleesTotales;
        this.heuresNuitTotales = heuresNuitTotales;
        this.heuresJourFerieTotales = heuresJourFerieTotales;
    }

    public int getNbCreneaux() {
        return nbCreneaux;
    }

    public void setNbCreneaux(int nbCreneaux) {
        this.nbCreneaux = nbCreneaux;
    }

    public int getNbCreneauxNonAffectes() {
        return nbCreneauxNonAffectes;
    }

    public void setNbCreneauxNonAffectes(int nbCreneauxNonAffectes) {
        this.nbCreneauxNonAffectes = nbCreneauxNonAffectes;
    }

    public double getHeuresTravailleesTotales() {
        return heuresTravailleesTotales;
    }

    public void setHeuresTravailleesTotales(double heuresTravailleesTotales) {
        this.heuresTravailleesTotales = heuresTravailleesTotales;
    }

    public double getHeuresNuitTotales() {
        return heuresNuitTotales;
    }

    public void setHeuresNuitTotales(double heuresNuitTotales) {
        this.heuresNuitTotales = heuresNuitTotales;
    }

    public double getHeuresJourFerieTotales() {
        return heuresJourFerieTotales;
    }

    public void setHeuresJourFerieTotales(double heuresJourFerieTotales) {
        this.heuresJourFerieTotales = heuresJourFerieTotales;
    }
}