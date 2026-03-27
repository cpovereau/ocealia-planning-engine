package fr.project.planning.scenarios.dto.input;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * CreneauInputDTO — DTO de transport pour un créneau entrant (WinDev → moteur).
 *
 * Distinct de CreneauPlanningDTO qui est un DTO de sortie (réponse API).
 *
 * Phase 5 : groupeBesoinId, blocJourId, ordreDansBloc, estSegmentDePause exploités.
 *
 * [Phase 10C] @JsonIgnoreProperties retiré — contrat strict.
 * Champs IGNORÉS supprimés : axesOrganisationnels, priorite, type.
 * Champ ⚠️ DÉPRÉCIÉ conservé : activite (fallback codeActiviteId).
 *
 * Créneau traversant minuit : date = jour de début, heureFin < heureDebut.
 */
public class CreneauInputDTO {

    private String id;
    private LocalDate date;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureDebut;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureFin;

    private String lieu;
    private String codeActiviteId;
    private String activite;        // ⚠️ DÉPRÉCIÉ — fallback si codeActiviteId absent
    private String posteComptable;

    private Boolean isJourFerie;
    private Boolean segmentNuit;

    // [Phase 5] structuration des besoins
    private String groupeBesoinId;
    private String blocJourId;
    private Integer ordreDansBloc;
    private Boolean estSegmentDePause;

    // =========================
    // Getters / Setters
    // =========================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getCodeActiviteId() { return codeActiviteId; }
    public void setCodeActiviteId(String codeActiviteId) { this.codeActiviteId = codeActiviteId; }

    public String getActivite() { return activite; }
    public void setActivite(String activite) { this.activite = activite; }

    public String getPosteComptable() { return posteComptable; }
    public void setPosteComptable(String posteComptable) { this.posteComptable = posteComptable; }

    public Boolean getIsJourFerie() { return isJourFerie; }
    public void setIsJourFerie(Boolean isJourFerie) { this.isJourFerie = isJourFerie; }

    public Boolean getSegmentNuit() { return segmentNuit; }
    public void setSegmentNuit(Boolean segmentNuit) { this.segmentNuit = segmentNuit; }

    public String getGroupeBesoinId() { return groupeBesoinId; }
    public void setGroupeBesoinId(String groupeBesoinId) { this.groupeBesoinId = groupeBesoinId; }

    public String getBlocJourId() { return blocJourId; }
    public void setBlocJourId(String blocJourId) { this.blocJourId = blocJourId; }

    public Integer getOrdreDansBloc() { return ordreDansBloc; }
    public void setOrdreDansBloc(Integer ordreDansBloc) { this.ordreDansBloc = ordreDansBloc; }

    public Boolean getEstSegmentDePause() { return estSegmentDePause; }
    public void setEstSegmentDePause(Boolean estSegmentDePause) { this.estSegmentDePause = estSegmentDePause; }
}
