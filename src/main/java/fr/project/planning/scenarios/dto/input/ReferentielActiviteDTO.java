package fr.project.planning.scenarios.dto.input;

/**
 * ReferentielActiviteDTO — Phase 1 transport uniquement.
 * Une activité dans le bloc referentiels.activites.
 * Non exploitée par le builder avant Phase 3.
 */
public class ReferentielActiviteDTO {

    private String codeActiviteId;
    private String libelle;
    private Boolean compteDansCharge;
    private Boolean genereDetteRepos;
    private Boolean estServiceCritique;

    public String getCodeActiviteId() { return codeActiviteId; }
    public void setCodeActiviteId(String codeActiviteId) { this.codeActiviteId = codeActiviteId; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public Boolean getCompteDansCharge() { return compteDansCharge; }
    public void setCompteDansCharge(Boolean compteDansCharge) { this.compteDansCharge = compteDansCharge; }

    public Boolean getGenereDetteRepos() { return genereDetteRepos; }
    public void setGenereDetteRepos(Boolean genereDetteRepos) { this.genereDetteRepos = genereDetteRepos; }

    public Boolean getEstServiceCritique() { return estServiceCritique; }
    public void setEstServiceCritique(Boolean estServiceCritique) { this.estServiceCritique = estServiceCritique; }
}
