package fr.project.planning.domain.metier;

public class ComptabiliteActivite {

    public enum TypeImpactActivite {
        NEUTRE,
        CHARGE_STANDARD,
        CHARGE_ELEVEE,
        DETTE_REPOS,
        SERVICE_CRITIQUE
    }

    private final String codeActivite;
    private final boolean compteDansCharge;
    private final boolean genereDetteRepos;
    private final boolean estServiceCritique;
    private final boolean prioritaireSurConfort;
    private final TypeImpactActivite typeImpact;

    public ComptabiliteActivite(
            String codeActivite,
            boolean compteDansCharge,
            boolean genereDetteRepos,
            boolean estServiceCritique,
            boolean prioritaireSurConfort,
            TypeImpactActivite typeImpact
    ) {
        this.codeActivite = codeActivite;
        this.compteDansCharge = compteDansCharge;
        this.genereDetteRepos = genereDetteRepos;
        this.estServiceCritique = estServiceCritique;
        this.prioritaireSurConfort = prioritaireSurConfort;
        this.typeImpact = typeImpact;
    }

    // Getters uniquement
    
    public String getCodeActivite() {
        return codeActivite;
    }

    public boolean isCompteDansCharge() {
        return compteDansCharge;
    }

    public boolean isGenereDetteRepos() {
        return genereDetteRepos;
    }

    public boolean isEstServiceCritique() {
        return estServiceCritique;
    }

    public boolean isPrioritaireSurConfort() {
        return prioritaireSurConfort;
    }

    public TypeImpactActivite getTypeImpact() {
        return typeImpact;
    }
}
