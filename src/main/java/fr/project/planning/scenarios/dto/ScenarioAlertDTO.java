package fr.project.planning.scenarios.dto;

import java.time.LocalDate;

public class ScenarioAlertDTO {

    private String code;     // ex: SHIFT_END_EXCEEDED
    private LocalDate date;  // date concernée
    private String message;  // message lisible

    public ScenarioAlertDTO() {}

    public ScenarioAlertDTO(String code, LocalDate date, String message) {
        this.code = code;
        this.date = date;
        this.message = message;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}