package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

public class ScenarioAlertDTO {

    private String code;      // ex: SHIFT_END_EXCEEDED
    private String severity;  // INFO | WARNING | ERROR

    /**
     * Date concernée — optionnelle.
     *
     * Une alerte peut porter sur le dataset ou sur la configuration plutôt que sur un jour
     * (ex : activité absente du référentiel). Le champ est alors omis de la réponse plutôt
     * que sérialisé à null : `null` échouerait la validation `type: string` des contrats,
     * que le champ soit requis ou non.
     *
     * L'annotation est volontairement portée par le champ : une inclusion globale
     * `non_null` modifierait toute la réponse (ex : `creneaux[].nature`).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDate date;

    private String message;   // message lisible

    public ScenarioAlertDTO() {}

    public ScenarioAlertDTO(String code, String severity, LocalDate date, String message) {
        this.code = code;
        this.severity = severity;
        this.date = date;
        this.message = message;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}