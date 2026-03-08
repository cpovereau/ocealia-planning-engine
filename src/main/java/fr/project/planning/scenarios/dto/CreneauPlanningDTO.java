package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

public class CreneauPlanningDTO {

    private String activite;     // ex: "travail"

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureDebut;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureFin;

    private String duree;        // "HH:MM"
    private CreneauNature nature;

    private String ressourceAffecteeId;

    public CreneauPlanningDTO() {}

    public CreneauPlanningDTO(
            String activite,
            LocalTime heureDebut,
            LocalTime heureFin,
            String duree,
            CreneauNature nature,
            String ressourceAffecteeId
    ) {
        this.activite = activite;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.duree = duree;
        this.nature = nature;
        this.ressourceAffecteeId = ressourceAffecteeId;
    }

    public String getActivite() {
        return activite;
    }

    public void setActivite(String activite) {
        this.activite = activite;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
    }

    public String getDuree() {
        return duree;
    }

    public void setDuree(String duree) {
        this.duree = duree;
    }

    public CreneauNature getNature() {
        return nature;
    }

    public void setNature(CreneauNature nature) {
        this.nature = nature;
    }

    public String getRessourceAffecteeId() {
        return ressourceAffecteeId;
    }

    public void setRessourceAffecteeId(String ressourceAffecteeId) {
        this.ressourceAffecteeId = ressourceAffecteeId;
    }
}