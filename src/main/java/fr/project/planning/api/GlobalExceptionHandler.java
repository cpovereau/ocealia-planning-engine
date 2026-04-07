package fr.project.planning.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Phase 4.2 — Gestion d'erreur HTTP unifiée.
 *
 * Toutes les erreurs API sont normalisées dans le format :
 *   { "error": { "code": "...", "message": "...", "details": [...] } }
 *
 * Codes utilisés :
 *   INVALID_REQUEST   — Bean Validation (400)
 *   MALFORMED_JSON    — JSON non parseable (400)
 *   BUSINESS_ERROR    — Règle métier violée (422)
 *   INTERNAL_ERROR    — Erreur technique inattendue (500)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bean Validation : requestId absent, metadata.clientId vide, etc.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetailDTO> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorDetailDTO(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.of("INVALID_REQUEST", "Validation failed", details));
    }

    /**
     * JSON non parseable ou type incompatible.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleMalformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.of("MALFORMED_JSON", "JSON invalide ou mal formé", List.of()));
    }

    /**
     * Violation d'une règle métier (scenarioType incorrect, creneaux vides, etc.).
     *
     * TODO Phase 4.6 : si des exceptions métier spécifiques sont introduites
     *   (ex: ResourceNotFoundException, HorizonIncoherentException), les mapper ici
     *   avec des codes 422 dédiés plutôt que de réutiliser le message brut.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessError(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponseDTO.of("BUSINESS_ERROR", ex.getMessage(), List.of()));
    }

    /**
     * Filet de sécurité — toute exception non gérée explicitement.
     * Le message d'origine n'est pas exposé pour éviter les fuites d'information.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDTO.of("INTERNAL_ERROR", "Une erreur interne est survenue", List.of()));
    }
}
