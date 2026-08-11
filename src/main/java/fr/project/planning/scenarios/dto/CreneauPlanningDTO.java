package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

/**
 * Créneau tel qu'il apparaît dans le planning restitué.
 *
 * <p>Les champs {@code id} et {@code lieu} sont des <b>données reçues restituées à
 * l'identique</b> : le moteur ne les interprète pas et ne les normalise pas. {@code id} est
 * la clé qui permet à l'appelant de rattacher chaque créneau du planning à sa ligne d'origine.
 *
 * <p>Pour un créneau généré par le moteur (SC-01), {@code id} porte le préfixe {@code SC01-}
 * et ne désigne aucune ligne en base, et {@code lieu} vaut {@code null}.
 *
 * @see <a href="file:../../../../../../../docs/50_SCENARIO_RESPONSE_CONTRACT.md">50_SCENARIO_RESPONSE_CONTRACT.md §2.1</a>
 */
public class CreneauPlanningDTO {

    /** Identifiant du créneau, restitué tel que reçu. Chaîne opaque. */
    private String id;

    /** Lieu reçu en entrée ; {@code null} si le créneau n'en portait pas. */
    private String lieu;

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
            String id,
            String lieu,
            String activite,
            LocalTime heureDebut,
            LocalTime heureFin,
            String duree,
            CreneauNature nature,
            String ressourceAffecteeId
    ) {
        this.id = id;
        this.lieu = lieu;
        this.activite = activite;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.duree = duree;
        this.nature = nature;
        this.ressourceAffecteeId = ressourceAffecteeId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
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