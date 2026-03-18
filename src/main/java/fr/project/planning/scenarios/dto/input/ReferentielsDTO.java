package fr.project.planning.scenarios.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * ReferentielsDTO — Phase 1 transport uniquement.
 * Bloc referentiels du dataSet (activités, etc.).
 * Non exploité par le builder avant Phase 3.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReferentielsDTO {

    private List<ReferentielActiviteDTO> activites;

    public List<ReferentielActiviteDTO> getActivites() { return activites; }
    public void setActivites(List<ReferentielActiviteDTO> activites) { this.activites = activites; }
}
