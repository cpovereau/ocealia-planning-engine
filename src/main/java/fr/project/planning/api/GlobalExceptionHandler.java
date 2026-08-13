package fr.project.planning.api;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 4.2 — Gestion d'erreur HTTP unifiée.
 *
 * Toutes les erreurs API sont normalisées dans le format :
 *   { "error": { "code": "...", "message": "...", "details": [...] } }
 *
 * Codes utilisés :
 *   INVALID_REQUEST   — Bean Validation (400)
 *   UNKNOWN_FIELD     — Champ inconnu dans le contrat d'entrée (400) — rang 11
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
     * JSON non parseable, type incompatible, ou champ inconnu.
     *
     * <p>[Rang 11] Un champ inconnu n'est pas un JSON mal formé — la syntaxe est parfaite, c'est le
     * contrat qui ne le connaît pas. Le distinguer par un code propre évite que l'appelant parte
     * chercher une erreur de syntaxe qui n'existe pas, et lui rend ce dont il a réellement besoin :
     * <strong>où</strong> se trouve le champ, et <strong>quels noms</strong> sont acceptés à cet
     * endroit.</p>
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleMalformedJson(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof UnrecognizedPropertyException champInconnu) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponseDTO.of("UNKNOWN_FIELD",
                            "Champ inconnu du contrat d'entrée : '"
                                    + cheminDe(champInconnu) + "'",
                            List.of(new ErrorDetailDTO(cheminDe(champInconnu),
                                    "Champ non reconnu. Champs acceptés à ce niveau : "
                                            + champsAcceptes(champInconnu)))));
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDTO.of("MALFORMED_JSON", "JSON invalide ou mal formé", List.of()));
    }

    /**
     * Chemin JSON complet du champ fautif — {@code dataSet.creneaux[0].priorite}.
     *
     * <p>Le seul nom du champ ne suffirait pas : dans une requête qui porte quatre-vingts créneaux,
     * savoir <em>lequel</em> le porte est ce qui distingue un diagnostic d'une devinette.</p>
     */
    static String cheminDe(UnrecognizedPropertyException ex) {
        StringBuilder chemin = new StringBuilder();
        for (JsonMappingException.Reference reference : ex.getPath()) {
            if (reference.getIndex() >= 0) {
                chemin.append('[').append(reference.getIndex()).append(']');
            } else {
                if (chemin.length() > 0) {
                    chemin.append('.');
                }
                chemin.append(reference.getFieldName());
            }
        }
        return chemin.toString();
    }

    /** Noms que le contrat accepte à cet endroit, triés — la réponse à « alors quoi ? ». */
    private static String champsAcceptes(UnrecognizedPropertyException ex) {
        Collection<Object> connus = ex.getKnownPropertyIds();
        if (connus == null || connus.isEmpty()) {
            return "(aucun)";
        }
        return connus.stream().map(String::valueOf).sorted().collect(Collectors.joining(", "));
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
