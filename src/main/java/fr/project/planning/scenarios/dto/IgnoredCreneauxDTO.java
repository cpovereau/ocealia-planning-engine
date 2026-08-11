package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Compteurs de créneaux signalés en pré-résolution, et depuis le lot S8.4 leur détail.
 *
 * <p>Les trois compteurs sont conservés à l'identique : ils sont au contrat depuis la Phase 9 et
 * des clients s'en servent. {@link #getDetails()} les explique — il n'en change pas la valeur.</p>
 */
public class IgnoredCreneauxDTO {

    private int horsHorizon;
    private int aucuneRessourceDansDataset;
    private int activiteInconnue;

    /**
     * Quels créneaux, et pourquoi.
     *
     * <p>Toujours présent, éventuellement vide : un tableau vide dit « aucun créneau signalé », là
     * où une clé absente laisserait le client se demander si le moteur sait répondre.</p>
     */
    private List<CreneauIgnoreDTO> details = new ArrayList<>();

    public IgnoredCreneauxDTO() {
    }

    public IgnoredCreneauxDTO(int horsHorizon, int aucuneRessourceDansDataset, int activiteInconnue) {
        this(horsHorizon, aucuneRessourceDansDataset, activiteInconnue, List.of());
    }

    public IgnoredCreneauxDTO(int horsHorizon, int aucuneRessourceDansDataset, int activiteInconnue,
                              List<CreneauIgnoreDTO> details) {
        this.horsHorizon = horsHorizon;
        this.aucuneRessourceDansDataset = aucuneRessourceDansDataset;
        this.activiteInconnue = activiteInconnue;
        this.details = details == null ? new ArrayList<>() : new ArrayList<>(details);
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

    public List<CreneauIgnoreDTO> getDetails() {
        return details;
    }

    public void setDetails(List<CreneauIgnoreDTO> details) {
        this.details = details == null ? new ArrayList<>() : new ArrayList<>(details);
    }
}
