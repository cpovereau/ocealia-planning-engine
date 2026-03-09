package fr.project.planning.scenarios.dto;

public class AssignmentDiagnosticDTO {

    private String creneauId;
    private String date;
    private String heureDebut;
    private String heureFin;
    private String activite;
    private String status;
    private String reasonCode;
    private String message;

    public AssignmentDiagnosticDTO() {
    }

    public AssignmentDiagnosticDTO(
            String creneauId,
            String date,
            String heureDebut,
            String heureFin,
            String activite,
            String status,
            String reasonCode,
            String message
    ) {
        this.creneauId = creneauId;
        this.date = date;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.activite = activite;
        this.status = status;
        this.reasonCode = reasonCode;
        this.message = message;
    }

    public String getCreneauId() {
        return creneauId;
    }

    public void setCreneauId(String creneauId) {
        this.creneauId = creneauId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(String heureDebut) {
        this.heureDebut = heureDebut;
    }

    public String getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(String heureFin) {
        this.heureFin = heureFin;
    }

    public String getActivite() {
        return activite;
    }

    public void setActivite(String activite) {
        this.activite = activite;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}