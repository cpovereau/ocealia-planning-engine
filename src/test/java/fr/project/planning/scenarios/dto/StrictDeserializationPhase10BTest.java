package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import fr.project.planning.scenarios.dto.input.IndisponibilitesDTO;
import fr.project.planning.scenarios.dto.input.ReferentielsDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StrictDeserializationPhase10BTest — Phase 10B
 *
 * Valide que les DTO durcis en Phase 10B rejettent les champs inconnus.
 *
 * DTOs durcis :
 * - IndisponibilitesDTO : @JsonIgnoreProperties retiré — 1 seul champ connu (items)
 * - ReferentielsDTO     : @JsonIgnoreProperties retiré — 1 seul champ connu (activites)
 *
 * Les tests utilisent un ObjectMapper strict (défaut, sans FAIL_ON_UNKNOWN_PROPERTIES=false)
 * pour prouver le nouveau comportement contractuel.
 *
 * DTOs laissés tolérants (non testés ici) :
 * - Sc03ScenarioRequestDTO, Sc03ScenarioParametersDTO, DataSetDTO, PosteVirtuelInputDTO
 * - SalarieInputDTO, CreneauInputDTO (réservés Phase 10C)
 */
class StrictDeserializationPhase10BTest {

    /**
     * ObjectMapper strict — comportement par défaut Jackson.
     * Ne configure PAS FAIL_ON_UNKNOWN_PROPERTIES=false :
     * les annotations @JsonIgnoreProperties sur les classes gouvernent le comportement.
     * Un DTO sans @JsonIgnoreProperties(ignoreUnknown=true) échoue sur un champ inconnu.
     */
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // =========================================================
    // IndisponibilitesDTO — durcissement Phase 10B
    // =========================================================

    @Test
    void indisponibilitesDTO_jsonValide_doitDeserializerCorrectement() throws Exception {
        // JSON valide avec les seuls champs connus → pas de régression
        String json = "{\"items\": ["
                + "{\"ressourceId\": \"SAL-001\", \"dateDebut\": \"2026-05-01\","
                + "  \"dateFin\": \"2026-05-03\", \"motif\": \"CONGE_POSE\"}"
                + "]}";

        IndisponibilitesDTO result = objectMapper.readValue(json, IndisponibilitesDTO.class);

        assertNotNull(result.getItems());
        assertEquals(1, result.getItems().size());
        assertEquals("SAL-001", result.getItems().get(0).getRessourceId());
        assertEquals("CONGE_POSE", result.getItems().get(0).getMotif());
    }

    @Test
    void indisponibilitesDTO_champInconnu_doitEchouerExplicitement() {
        // [Phase 10B] Champ inconnu → UnrecognizedPropertyException
        // Avant : @JsonIgnoreProperties(ignoreUnknown=true) absorbait silencieusement
        // Après : rejet explicite — une faute de frappe ("item" au lieu de "items") est détectée
        String json = "{\"items\": [], \"champInconnu\": \"valeur_piege\"}";

        assertThrows(UnrecognizedPropertyException.class,
                () -> objectMapper.readValue(json, IndisponibilitesDTO.class));
    }

    @Test
    void indisponibilitesDTO_itemsNull_doitDeserializerSansErreur() throws Exception {
        // items absent (null JSON) → acceptable, le mapper le reçoit comme null
        String json = "{}";

        IndisponibilitesDTO result = objectMapper.readValue(json, IndisponibilitesDTO.class);

        assertNull(result.getItems());
    }

    // =========================================================
    // ReferentielsDTO — durcissement Phase 10B
    // =========================================================

    @Test
    void referentielsDTO_jsonValide_doitDeserializerCorrectement() throws Exception {
        // JSON valide avec les seuls champs connus → pas de régression
        String json = "{\"activites\": ["
                + "{\"codeActiviteId\": \"ACT-001\", \"libelle\": \"Soins\","
                + "  \"compteDansCharge\": true, \"genereDetteRepos\": false,"
                + "  \"estServiceCritique\": false}"
                + "]}";

        ReferentielsDTO result = objectMapper.readValue(json, ReferentielsDTO.class);

        assertNotNull(result.getActivites());
        assertEquals(1, result.getActivites().size());
        assertEquals("ACT-001", result.getActivites().get(0).getCodeActiviteId());
        assertTrue(result.getActivites().get(0).getCompteDansCharge());
    }

    @Test
    void referentielsDTO_champInconnu_doitEchouerExplicitement() {
        // [Phase 10B] Champ inconnu → UnrecognizedPropertyException
        // Avant : @JsonIgnoreProperties(ignoreUnknown=true) absorbait silencieusement
        // Après : rejet explicite — ex. "autresReferentiels" ou "postesComptables" non déclarés sont détectés
        String json = "{\"activites\": [], \"autresReferentiels\": \"valeur_piege\"}";

        assertThrows(UnrecognizedPropertyException.class,
                () -> objectMapper.readValue(json, ReferentielsDTO.class));
    }

    @Test
    void referentielsDTO_activitesVides_doitDeserializerSansErreur() throws Exception {
        // Bloc referentiels avec liste vide → acceptable
        String json = "{\"activites\": []}";

        ReferentielsDTO result = objectMapper.readValue(json, ReferentielsDTO.class);

        assertNotNull(result.getActivites());
        assertTrue(result.getActivites().isEmpty());
    }
}
