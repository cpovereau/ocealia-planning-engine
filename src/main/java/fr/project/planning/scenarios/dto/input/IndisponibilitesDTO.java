package fr.project.planning.scenarios.dto.input;

import java.util.List;

/**
 * IndisponibilitesDTO — Phase 1 transport uniquement.
 * Bloc indisponibilites du dataSet.
 * Non exploité par le builder avant Phase 3.
 *
 * [Phase 10B] @JsonIgnoreProperties retiré : bloc stabilisé à 1 champ connu (items).
 * Tout champ inconnu est désormais une erreur de contrat (faute de frappe côté client).
 */
public class IndisponibilitesDTO {

    private List<IndisponibiliteItemDTO> items;

    public List<IndisponibiliteItemDTO> getItems() { return items; }
    public void setItems(List<IndisponibiliteItemDTO> items) { this.items = items; }
}
