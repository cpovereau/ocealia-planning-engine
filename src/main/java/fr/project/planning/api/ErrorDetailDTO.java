package fr.project.planning.api;

public class ErrorDetailDTO {

    private final String field;
    private final String message;

    public ErrorDetailDTO(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() { return field; }
    public String getMessage() { return message; }
}
