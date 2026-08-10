package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * MotifCandidatDTO — raison attachée à un candidat SC-06.
 *
 * <p>Reprend délibérément la forme de {@code ScenarioAlertDTO} — {@code code}, {@code severite},
 * {@code message} — afin que le client applique la même règle de lecture partout : il filtre sur
 * la sévérité, pas sur le code.</p>
 *
 * <p>Un motif {@code ERROR} sur une règle éliminatoire entraîne {@code conforme = false}. Les
 * motifs {@code WARNING} et {@code INFO} décrivent sans disqualifier.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MotifCandidatDTO {

    private String code;
    private String severite;
    private String message;

    public MotifCandidatDTO() {
    }

    public MotifCandidatDTO(String code, String severite, String message) {
        this.code = code;
        this.severite = severite;
        this.message = message;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getSeverite() { return severite; }
    public void setSeverite(String severite) { this.severite = severite; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
