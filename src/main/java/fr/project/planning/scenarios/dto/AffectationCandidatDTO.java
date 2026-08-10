package fr.project.planning.scenarios.dto;

/**
 * AffectationCandidatDTO — un créneau du besoin et la ressource proposée pour le servir.
 *
 * <p>C'est la réponse attendue de SC-06 : une identité, l'activité à servir, le lieu.
 * Les caractéristiques du créneau sont restituées telles que reçues.</p>
 */
public class AffectationCandidatDTO {

    private String creneauId;
    private String ressourceId;
    private String activite;
    private String lieu;
    private String heureDebut;
    private String heureFin;

    public AffectationCandidatDTO() {
    }

    public AffectationCandidatDTO(String creneauId, String ressourceId, String activite,
                                  String lieu, String heureDebut, String heureFin) {
        this.creneauId = creneauId;
        this.ressourceId = ressourceId;
        this.activite = activite;
        this.lieu = lieu;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
    }

    public String getCreneauId() { return creneauId; }
    public void setCreneauId(String creneauId) { this.creneauId = creneauId; }

    public String getRessourceId() { return ressourceId; }
    public void setRessourceId(String ressourceId) { this.ressourceId = ressourceId; }

    public String getActivite() { return activite; }
    public void setActivite(String activite) { this.activite = activite; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getHeureDebut() { return heureDebut; }
    public void setHeureDebut(String heureDebut) { this.heureDebut = heureDebut; }

    public String getHeureFin() { return heureFin; }
    public void setHeureFin(String heureFin) { this.heureFin = heureFin; }
}
