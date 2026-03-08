package fr.project.planning.scenarios.dto;

public class IgnoredCreneauxDTO {

    private int horsHorizon;
    private int sansRessource;
    private int activiteInconnue;

    public IgnoredCreneauxDTO() {
    }

    public IgnoredCreneauxDTO(int horsHorizon, int sansRessource, int activiteInconnue) {
        this.horsHorizon = horsHorizon;
        this.sansRessource = sansRessource;
        this.activiteInconnue = activiteInconnue;
    }

    public int getHorsHorizon() {
        return horsHorizon;
    }

    public void setHorsHorizon(int horsHorizon) {
        this.horsHorizon = horsHorizon;
    }

    public int getSansRessource() {
        return sansRessource;
    }

    public void setSansRessource(int sansRessource) {
        this.sansRessource = sansRessource;
    }

    public int getActiviteInconnue() {
        return activiteInconnue;
    }

    public void setActiviteInconnue(int activiteInconnue) {
        this.activiteInconnue = activiteInconnue;
    }
}