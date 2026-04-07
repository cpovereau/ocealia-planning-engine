package fr.project.planning.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Phase 4.2 — Tests unitaires du GlobalExceptionHandler.
 *
 * Vérifie le mapping exception → code HTTP + format de réponse.
 * Pas de contexte Spring nécessaire : instanciation directe du handler.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleMalformedJson_retourne_400_MALFORMED_JSON() {
        var ex = mock(HttpMessageNotReadableException.class);
        ResponseEntity<ErrorResponseDTO> resp = handler.handleMalformedJson(ex);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        ErrorResponseDTO body = resp.getBody();
        assertNotNull(body);
        assertEquals("MALFORMED_JSON", body.getError().getCode());
        assertEquals(List.of(), body.getError().getDetails());
    }

    @Test
    void handleBusinessError_retourne_422_BUSINESS_ERROR_avec_message() {
        var ex = new IllegalArgumentException("dataSet est requis.");
        ResponseEntity<ErrorResponseDTO> resp = handler.handleBusinessError(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        ErrorResponseDTO body = resp.getBody();
        assertNotNull(body);
        assertEquals("BUSINESS_ERROR", body.getError().getCode());
        assertEquals("dataSet est requis.", body.getError().getMessage());
        assertEquals(List.of(), body.getError().getDetails());
    }

    @Test
    void handleGenericException_retourne_500_INTERNAL_ERROR_sans_fuite() {
        var ex = new RuntimeException("détail technique confidentiel");
        ResponseEntity<ErrorResponseDTO> resp = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        ErrorResponseDTO body = resp.getBody();
        assertNotNull(body);
        assertEquals("INTERNAL_ERROR", body.getError().getCode());
        // le message brut de l'exception ne doit pas être exposé
        assertNotEquals("détail technique confidentiel", body.getError().getMessage());
        assertEquals(List.of(), body.getError().getDetails());
    }
}
