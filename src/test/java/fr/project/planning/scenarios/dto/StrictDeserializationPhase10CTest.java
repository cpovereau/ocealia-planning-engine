package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StrictDeserializationPhase10CTest — Phase 10C
 *
 * Valide que CreneauInputDTO, durci en Phase 10C, rejette les champs inconnus.
 *
 * [Phase 10C] @JsonIgnoreProperties retiré de CreneauInputDTO.
 * Champs supprimés : type, priorite, axesOrganisationnels.
 * Champ conservé déprécié : activite (fallback codeActiviteId).
 *
 * Les tests utilisent un ObjectMapper strict (défaut, sans FAIL_ON_UNKNOWN_PROPERTIES=false)
 * pour prouver le nouveau comportement contractuel.
 *
 * DTOs laissés tolérants (non testés ici) :
 * - Sc03ScenarioRequestDTO, Sc03ScenarioParametersDTO, DataSetDTO, PosteVirtuelInputDTO
 * - SalarieInputDTO (tolérant délibéré — SC-01 envoie type et capaciteCible dans les salariés)
 */
class StrictDeserializationPhase10CTest {

    /**
     * ObjectMapper strict — comportement par défaut Jackson.
     * Ne configure PAS FAIL_ON_UNKNOWN_PROPERTIES=false :
     * les annotations @JsonIgnoreProperties sur les classes gouvernent le comportement.
     * CreneauInputDTO sans @JsonIgnoreProperties échoue sur tout champ inconnu.
     */
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // =========================================================
    // CreneauInputDTO — durcissement Phase 10C
    // =========================================================

    @Test
    void creneauInputDTO_jsonValide_doitDeserializerCorrectement() throws Exception {
        // JSON valide avec les seuls champs connus du contrat SC-03 → pas de régression
        String json = "{"
                + "\"id\": \"CRE-001\","
                + "\"date\": \"2026-05-11\","
                + "\"heureDebut\": \"07:00\","
                + "\"heureFin\": \"15:00\","
                + "\"lieu\": \"HOPITAL-NORD\","
                + "\"codeActiviteId\": \"ACT-SOIN\","
                + "\"activite\": \"Soins infirmiers\","
                + "\"posteComptable\": \"PC-SOINS\","
                + "\"isJourFerie\": false,"
                + "\"segmentNuit\": false,"
                + "\"groupeBesoinId\": \"BESOIN-01\","
                + "\"blocJourId\": \"BLOC-A\","
                + "\"ordreDansBloc\": 1,"
                + "\"estSegmentDePause\": false"
                + "}";

        CreneauInputDTO result = objectMapper.readValue(json, CreneauInputDTO.class);

        assertNotNull(result);
        assertEquals("CRE-001", result.getId());
        assertEquals("ACT-SOIN", result.getCodeActiviteId());
        assertEquals("Soins infirmiers", result.getActivite());
        assertFalse(result.getIsJourFerie());
        assertFalse(result.getSegmentNuit());
        assertEquals("BESOIN-01", result.getGroupeBesoinId());
        assertEquals(1, result.getOrdreDansBloc());
    }

    @Test
    void creneauInputDTO_champType_doitEchouerExplicitement() {
        // [Phase 10C] 'type' supprimé du DTO → champ inconnu rejeté
        // Avant : @JsonIgnoreProperties absorbait silencieusement "type": "GENERE"
        // Après : rejet explicite — tout envoi de 'type' par le client est une erreur de contrat
        String json = "{"
                + "\"id\": \"CRE-001\","
                + "\"heureDebut\": \"07:00\","
                + "\"heureFin\": \"15:00\","
                + "\"type\": \"GENERE\""
                + "}";

        assertThrows(UnrecognizedPropertyException.class,
                () -> objectMapper.readValue(json, CreneauInputDTO.class));
    }

    @Test
    void creneauInputDTO_champPriorite_doitEchouerExplicitement() {
        // [Phase 10C] 'priorite' supprimé du DTO → champ inconnu rejeté
        // Avant : Integer reçu, log.warn émis, null transmis au domaine
        // Après : rejet explicite — mismatch de type et sémantique non définie
        String json = "{"
                + "\"id\": \"CRE-001\","
                + "\"heureDebut\": \"07:00\","
                + "\"heureFin\": \"15:00\","
                + "\"priorite\": 2"
                + "}";

        assertThrows(UnrecognizedPropertyException.class,
                () -> objectMapper.readValue(json, CreneauInputDTO.class));
    }

    @Test
    void creneauInputDTO_champAxesOrganisationnels_doitEchouerExplicitement() {
        // [Phase 10C] 'axesOrganisationnels' supprimé du DTO → champ inconnu rejeté
        // Avant : objet reçu, log.warn émis, jamais passé au domaine
        // Après : rejet explicite — aucune contrainte organisationnelle, champ sans trajectoire
        String json = "{"
                + "\"id\": \"CRE-001\","
                + "\"heureDebut\": \"07:00\","
                + "\"heureFin\": \"15:00\","
                + "\"axesOrganisationnels\": {\"directionIds\": [\"DIR-001\"]}"
                + "}";

        assertThrows(UnrecognizedPropertyException.class,
                () -> objectMapper.readValue(json, CreneauInputDTO.class));
    }

    @Test
    void creneauInputDTO_champActiviteDeprecated_doitEncoreEtreAccepte() throws Exception {
        // [Phase 10C] 'activite' reste dans le DTO — champ ⚠️ DÉPRÉCIÉ conservé
        // Le fallback codeActiviteId → activite est encore actif
        String json = "{"
                + "\"id\": \"CRE-001\","
                + "\"heureDebut\": \"07:00\","
                + "\"heureFin\": \"15:00\","
                + "\"activite\": \"Soins infirmiers\""
                + "}";

        CreneauInputDTO result = objectMapper.readValue(json, CreneauInputDTO.class);

        assertEquals("Soins infirmiers", result.getActivite());
        assertNull(result.getCodeActiviteId());
    }
}
