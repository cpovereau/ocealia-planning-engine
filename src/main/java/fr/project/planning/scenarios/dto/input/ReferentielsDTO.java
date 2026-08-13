package fr.project.planning.scenarios.dto.input;

import java.util.List;

/**
 * ReferentielsDTO — Phase 1 transport uniquement.
 * Bloc referentiels du dataSet (activités, etc.).
 * Non exploité par le builder avant Phase 3.
 *
 * Bloc stabilisé à 1 champ connu (activites) : tout champ inconnu est une erreur de contrat,
 * typiquement une faute de frappe côté client. Un nouveau référentiel doit être déclaré ici avant
 * de pouvoir être transmis.
 *
 * ⚠️ [Rang 11] Annoncé strict depuis la phase 10B au motif du retrait de @JsonIgnoreProperties.
 * Le motif était faux — Spring Boot désactive FAIL_ON_UNKNOWN_PROPERTIES — et le bloc ignorait
 * en silence. La strictness est effective depuis que le contrat d'entrée l'active.
 *
 * [Lot S7.9b] Deux champs ajoutés : les identifiants d'activité qui désignent un repos
 * hebdomadaire. Ils vivent ici parce que le repos hebdomadaire est qualifié par le référentiel
 * d'activités, et non par un drapeau porté par le créneau.
 */
public class ReferentielsDTO {

    private List<ReferentielActiviteDTO> activites;

    /**
     * Identifiant d'activité désignant un repos hebdomadaire (RH).
     *
     * <p>Cet identifiant <strong>change d'un client à l'autre</strong> : le moteur n'en connaît
     * aucun en dur. Non déclaré, aucun créneau n'est reconnu comme marqueur de repos et le
     * calendrier retombe entièrement sur le repli samedi/dimanche.</p>
     */
    private String codeActiviteReposHebdomadaire;

    /** Identifiant d'activité désignant un repos hebdomadaire dominical (RHD). */
    private String codeActiviteReposHebdomadaireDimanche;

    public List<ReferentielActiviteDTO> getActivites() { return activites; }
    public void setActivites(List<ReferentielActiviteDTO> activites) { this.activites = activites; }

    public String getCodeActiviteReposHebdomadaire() { return codeActiviteReposHebdomadaire; }
    public void setCodeActiviteReposHebdomadaire(String codeActiviteReposHebdomadaire) {
        this.codeActiviteReposHebdomadaire = codeActiviteReposHebdomadaire;
    }

    public String getCodeActiviteReposHebdomadaireDimanche() { return codeActiviteReposHebdomadaireDimanche; }
    public void setCodeActiviteReposHebdomadaireDimanche(String codeActiviteReposHebdomadaireDimanche) {
        this.codeActiviteReposHebdomadaireDimanche = codeActiviteReposHebdomadaireDimanche;
    }
}
