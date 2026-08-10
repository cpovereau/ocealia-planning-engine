package fr.project.planning.scenarios.dto;

import java.util.List;

/**
 * CandidatDTO — une manière de couvrir le besoin, et son rang (SC-06, lot S4).
 *
 * <p>Un candidat n'est pas une personne mais une <strong>solution</strong> : l'affectation
 * complète des créneaux du besoin. Lorsqu'une seule personne couvre tout — le cas préféré — la
 * distinction s'efface, mais elle est nécessaire dès qu'un besoin se répartit entre plusieurs
 * personnes.</p>
 *
 * <p>Le bloc {@code impacts} viendra au lot S5 : conséquences chiffrées sur l'amplitude
 * journalière et les volumes horaires de chaque ressource mobilisée.</p>
 */
public class CandidatDTO {

    /** 1 = la solution la plus favorable. */
    private int rang;

    /** {@code false} si la solution viole une règle éliminatoire — voir {@code motifs}. */
    private boolean conforme;

    /** {@code false} si une partie du besoin reste non couverte. */
    private boolean couvertureComplete;

    /** MONO_RESSOURCE, COMPOSEE ou RESSOURCE_A_POURVOIR. */
    private String nature;

    /** Une entrée par créneau du besoin. */
    private List<AffectationCandidatDTO> affectations;

    /** Raisons expliquant le rang et la conformité. Jamais nul, éventuellement vide. */
    private List<MotifCandidatDTO> motifs;

    public CandidatDTO() {
    }

    public CandidatDTO(int rang, boolean conforme, boolean couvertureComplete, String nature,
                       List<AffectationCandidatDTO> affectations, List<MotifCandidatDTO> motifs) {
        this.rang = rang;
        this.conforme = conforme;
        this.couvertureComplete = couvertureComplete;
        this.nature = nature;
        this.affectations = affectations;
        this.motifs = motifs;
    }

    public int getRang() { return rang; }
    public void setRang(int rang) { this.rang = rang; }

    public boolean isConforme() { return conforme; }
    public void setConforme(boolean conforme) { this.conforme = conforme; }

    public boolean isCouvertureComplete() { return couvertureComplete; }
    public void setCouvertureComplete(boolean couvertureComplete) { this.couvertureComplete = couvertureComplete; }

    public String getNature() { return nature; }
    public void setNature(String nature) { this.nature = nature; }

    public List<AffectationCandidatDTO> getAffectations() { return affectations; }
    public void setAffectations(List<AffectationCandidatDTO> affectations) { this.affectations = affectations; }

    public List<MotifCandidatDTO> getMotifs() { return motifs; }
    public void setMotifs(List<MotifCandidatDTO> motifs) { this.motifs = motifs; }
}
