package fr.project.planning.api;

import java.util.List;

public class ErrorResponseDTO {

    private final ErrorBody error;

    private ErrorResponseDTO(ErrorBody error) {
        this.error = error;
    }

    public ErrorBody getError() { return error; }

    public static ErrorResponseDTO of(String code, String message, List<ErrorDetailDTO> details) {
        return new ErrorResponseDTO(new ErrorBody(code, message, details));
    }

    public static class ErrorBody {
        private final String code;
        private final String message;
        private final List<ErrorDetailDTO> details;

        ErrorBody(String code, String message, List<ErrorDetailDTO> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
        public List<ErrorDetailDTO> getDetails() { return details; }
    }
}
