package fr.project.planning.scenarios.dto.input;

import java.util.List;

/**
 * IndisponibilitesDTO — Phase 1 transport uniquement.
 * Bloc indisponibilites du dataSet.
 * Non exploité par le builder avant Phase 3.
 *
 * Bloc stabilisé à 1 champ connu (items) : tout champ inconnu est une erreur de contrat,
 * typiquement une faute de frappe côté client.
 *
 * ⚠️ [Rang 11] Annoncé strict depuis la phase 10B au motif du retrait de @JsonIgnoreProperties.
 * Le motif était faux — Spring Boot désactive FAIL_ON_UNKNOWN_PROPERTIES — et le bloc ignorait
 * en silence. La strictness est effective depuis que le contrat d'entrée l'active.
 */
public class IndisponibilitesDTO {

    private List<IndisponibiliteItemDTO> items;

    public List<IndisponibiliteItemDTO> getItems() { return items; }
    public void setItems(List<IndisponibiliteItemDTO> items) { this.items = items; }
}
