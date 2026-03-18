package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IgnoredCreneauxDTO {

    private int horsHorizon;
    private int aucuneRessourceDansDataset;
    private int activiteInconnue;

    public IgnoredCreneauxDTO() {
    }

    public IgnoredCreneauxDTO(int horsHorizon, int aucuneRessourceDansDataset, int activiteInconnue) {
        this.horsHorizon = horsHorizon;
        this.aucuneRessourceDansDataset = aucuneRessourceDansDataset;
        this.activiteInconnue = activiteInconnue;
    }

    public int getHorsHorizon() {
        return horsHorizon;
    }

    public void setHorsHorizon(int horsHorizon) {
        this.horsHorizon = horsHorizon;
    }

    @JsonProperty("aucuneRessourceDansDataset")
    public int getAucuneRessourceDansDataset() {
        return aucuneRessourceDansDataset;
    }

    @JsonProperty("aucuneRessourceDansDataset")
    @JsonAlias("sansRessource")
    public void setAucuneRessourceDansDataset(int aucuneRessourceDansDataset) {
        this.aucuneRessourceDansDataset = aucuneRessourceDansDataset;
    }

    public int getActiviteInconnue() {
        return activiteInconnue;
    }

    public void setActiviteInconnue(int activiteInconnue) {
        this.activiteInconnue = activiteInconnue;
    }
}