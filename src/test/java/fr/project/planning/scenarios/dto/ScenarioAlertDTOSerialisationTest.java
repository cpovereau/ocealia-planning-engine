package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sérialisation de ScenarioAlertDTO.
 *
 * `date` est optionnel dans les contrats de sortie. Un `null` sérialisé échouerait la
 * validation `type: string` — que le champ soit ou non dans `required`. La clé doit
 * donc être **omise**, pas mise à null.
 */
class ScenarioAlertDTOSerialisationTest {

    /**
     * Reproduit la configuration auto-appliquée par Spring Boot : sans
     * `WRITE_DATES_AS_TIMESTAMPS` désactivé, un `LocalDate` sortirait en `[2026,7,27]`
     * au lieu de `"2026-07-27"`.
     */
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * [T-SER-01] Date absente → la clé disparaît du JSON.
     */
    @Test
    void date_nulle_est_omise_et_non_serialisee_a_null() throws Exception {
        String json = mapper.writeValueAsString(
                new ScenarioAlertDTO("UNKNOWN_ACTIVITY", "ERROR", null, "Activité absente du référentiel")
        );

        assertFalse(json.contains("\"date\""),
                "La clé date doit être absente, pas nulle : " + json);
        assertFalse(json.contains("null"), "Aucun null attendu : " + json);
        assertTrue(json.contains("\"code\":\"UNKNOWN_ACTIVITY\""), json);
        assertTrue(json.contains("\"severity\":\"ERROR\""), json);
    }

    /**
     * [T-SER-02] Date présente → sérialisée en ISO-8601, sans régression de format.
     */
    @Test
    void date_presente_est_serialisee_en_iso() throws Exception {
        String json = mapper.writeValueAsString(new ScenarioAlertDTO(
                "TOO_MANY_NON_WORKED_DAYS", "INFO", LocalDate.of(2026, 7, 27), "Horaire réduit"
        ));

        assertTrue(json.contains("\"date\":\"2026-07-27\""),
                "Date attendue en ISO-8601 : " + json);
    }
}
