package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * MotifArbitrageDTO — raison attachée à la répartition rendue par SC-05.
 *
 * <p>Même forme que {@link MotifCandidatDTO} et que {@code ScenarioAlertDTO} — {@code code},
 * {@code severite}, {@code message} — afin que le client applique la même règle de lecture
 * partout : il filtre sur la sévérité, pas sur le code.</p>
 *
 * <p>Un motif éliminatoire entraîne {@code arbitrage.acceptable = false}. La répartition reste
 * restituée : <em>le moteur ne refuse pas, il rend visible l'impossible</em>.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MotifArbitrageDTO {

    private String code;
    private String severite;
    private String message;

    public MotifArbitrageDTO() {
    }

    public MotifArbitrageDTO(String code, String severite, String message) {
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
