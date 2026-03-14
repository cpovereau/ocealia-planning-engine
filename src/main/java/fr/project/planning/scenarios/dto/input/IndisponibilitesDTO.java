package fr.project.planning.scenarios.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * IndisponibilitesDTO — Phase 1 transport uniquement.
 * Bloc indisponibilites du dataSet.
 * Non exploité par le builder avant Phase 3.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndisponibilitesDTO {

    private List<IndisponibiliteItemDTO> items;

    public List<IndisponibiliteItemDTO> getItems() { return items; }
    public void setItems(List<IndisponibiliteItemDTO> items) { this.items = items; }
}
