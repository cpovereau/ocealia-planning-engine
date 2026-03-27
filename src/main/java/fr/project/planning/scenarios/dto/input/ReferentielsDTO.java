package fr.project.planning.scenarios.dto.input;

import java.util.List;

/**
 * ReferentielsDTO — Phase 1 transport uniquement.
 * Bloc referentiels du dataSet (activités, etc.).
 * Non exploité par le builder avant Phase 3.
 *
 * [Phase 10B] @JsonIgnoreProperties retiré : bloc stabilisé à 1 champ connu (activites).
 * Tout champ inconnu est désormais une erreur de contrat (faute de frappe côté client).
 * Si de nouveaux référentiels sont ajoutés en Phase 10C, le champ doit être déclaré ici d'abord.
 */
public class ReferentielsDTO {

    private List<ReferentielActiviteDTO> activites;

    public List<ReferentielActiviteDTO> getActivites() { return activites; }
    public void setActivites(List<ReferentielActiviteDTO> activites) { this.activites = activites; }
}
