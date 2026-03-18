package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
import fr.project.planning.scenarios.dto.input.IndisponibilitesDTO;
import fr.project.planning.scenarios.dto.input.ReferentielsDTO;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSetDTO {

    private RessourcesDTO ressources;

    /** Phase 1 : creneaux entrants (liste de besoins WinDev). Ignorés par le builder SC-01. */
    private List<CreneauInputDTO> creneaux;

    /** Phase 1 transport / Phase 3 builder. */
    private ReferentielsDTO referentiels;

    /** Phase 1 transport / Phase 3 builder. */
    private IndisponibilitesDTO indisponibilites;

    public RessourcesDTO getRessources() { return ressources; }
    public void setRessources(RessourcesDTO ressources) { this.ressources = ressources; }

    public List<CreneauInputDTO> getCreneaux() { return creneaux; }
    public void setCreneaux(List<CreneauInputDTO> creneaux) { this.creneaux = creneaux; }

    public ReferentielsDTO getReferentiels() { return referentiels; }
    public void setReferentiels(ReferentielsDTO referentiels) { this.referentiels = referentiels; }

    public IndisponibilitesDTO getIndisponibilites() { return indisponibilites; }
    public void setIndisponibilites(IndisponibilitesDTO indisponibilites) { this.indisponibilites = indisponibilites; }
}
