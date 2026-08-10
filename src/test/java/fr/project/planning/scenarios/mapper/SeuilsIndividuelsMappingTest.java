package fr.project.planning.scenarios.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.scenarios.dto.input.ContraintesReglementairesDTO;
import fr.project.planning.scenarios.dto.input.SalarieInputDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SeuilsIndividuelsMappingTest — lot S7.0
 *
 * <p>Valide le rapatriement au salarié des cinq seuils qui vivaient dans
 * {@code SeuilsDeTolerance} : transport JSON, mapping vers le domaine, et règle
 * d'activation {@code seuilActif}.</p>
 *
 * <p>Aucune contrainte ne lit encore ces champs. Ce lot est volontairement neutre sur le
 * score : c'est le point de contrôle qui permettra d'imputer sans ambiguïté chaque
 * variation de score aux lots S7.1 à S7.6, qui rebrancheront les contraintes une par une.</p>
 */
class SeuilsIndividuelsMappingTest {

    private final ScenarioResourceMapper mapper = new ScenarioResourceMapper();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // =========================================================
    // Règle d'activation
    // =========================================================

    @Test
    void seuilAbsent_estInactif() {
        assertFalse(ContraintesReglementairesSalarie.seuilActif(null));
    }

    @Test
    void seuilNul_estInactif() {
        // Le contrat demande d'omettre le champ. Un 0 reçu est ambigu ; le moteur retient
        // la lecture sûre plutôt que de rendre toute affectation fautive.
        assertFalse(ContraintesReglementairesSalarie.seuilActif(0));
        assertFalse(ContraintesReglementairesSalarie.seuilActif(0.0d));
    }

    @Test
    void seuilNegatif_estInactif() {
        assertFalse(ContraintesReglementairesSalarie.seuilActif(-1));
    }

    @Test
    void seuilPositif_estActif() {
        assertTrue(ContraintesReglementairesSalarie.seuilActif(1));
        assertTrue(ContraintesReglementairesSalarie.seuilActif(0.5d));
    }

    // =========================================================
    // Mapping DTO → domaine
    // =========================================================

    @Test
    void lesCinqSeuilsRenseignes_doiventAtteindreLeDomaine() {
        SalarieInputDTO dto = salarieAvecSeuils(3, 2, 2, 7, 1);

        ContraintesReglementairesSalarie c =
                mapper.toSalarieReel(dto).getContraintesReglementaires();

        assertNotNull(c);
        assertEquals(3, c.getNuitsConsecutivesMaximum());
        assertEquals(2, c.getJoursReposMinimumApresNuits());
        assertEquals(2, c.getDimanchesTravaillesMaximum());
        assertEquals(7, c.getReposHebdomadaireFenetreJours());
        assertEquals(1, c.getReposHebdomadaireJoursOffMinimum());
    }

    @Test
    void seuilsAbsents_doiventResterNuls() {
        // Non-régression : aucun payload existant ne porte ces cinq champs.
        // Ils doivent traverser le mapping sans être fabriqués.
        SalarieInputDTO dto = new SalarieInputDTO();
        dto.setId("SAL-S70-02");
        dto.setContraintesReglementaires(new ContraintesReglementairesDTO());

        ContraintesReglementairesSalarie c =
                mapper.toSalarieReel(dto).getContraintesReglementaires();

        assertNotNull(c);
        assertNull(c.getNuitsConsecutivesMaximum());
        assertNull(c.getJoursReposMinimumApresNuits());
        assertNull(c.getDimanchesTravaillesMaximum());
        assertNull(c.getReposHebdomadaireFenetreJours());
        assertNull(c.getReposHebdomadaireJoursOffMinimum());
    }

    @Test
    void constructeurHistorique_doitLaisserLesCinqSeuilsInactifs() {
        // Les jeux de test antérieurs à S7 continuent d'utiliser la signature à 8 arguments.
        ContraintesReglementairesSalarie c = new ContraintesReglementairesSalarie(
                null, 10.0, 13.0, 11.0, null, 48.0, null, 6);

        assertEquals(6, c.getJoursConsecutifsMaximum());
        assertNull(c.getNuitsConsecutivesMaximum());
        assertNull(c.getDimanchesTravaillesMaximum());
        assertFalse(ContraintesReglementairesSalarie.seuilActif(c.getNuitsConsecutivesMaximum()));
    }

    // =========================================================
    // Désérialisation JSON
    // =========================================================

    @Test
    void jsonAvecLesCinqSeuils_doitSeDeserializer() {
        String json = """
                {
                  "id": "SAL-2001",
                  "contraintesReglementaires": {
                    "heuresMaximumParSemaine": 48.0,
                    "nuitsConsecutivesMaximum": 3,
                    "joursReposMinimumApresNuits": 2,
                    "dimanchesTravaillesMaximum": 2,
                    "reposHebdomadaireFenetreJours": 7,
                    "reposHebdomadaireJoursOffMinimum": 1
                  }
                }
                """;

        SalarieInputDTO dto = assertLit(json);
        ContraintesReglementairesDTO c = dto.getContraintesReglementaires();

        assertEquals(48.0, c.getHeuresMaximumParSemaine());
        assertEquals(3, c.getNuitsConsecutivesMaximum());
        assertEquals(2, c.getJoursReposMinimumApresNuits());
        assertEquals(2, c.getDimanchesTravaillesMaximum());
        assertEquals(7, c.getReposHebdomadaireFenetreJours());
        assertEquals(1, c.getReposHebdomadaireJoursOffMinimum());
    }

    @Test
    void jsonSansLesNouveauxSeuils_doitResterValide() {
        // Les payloads SC-01, SC-03 et SC-06 en production ne portent que les huit champs
        // historiques : l'ajout doit être strictement additif.
        String json = """
                {
                  "id": "SAL-2001",
                  "contraintesReglementaires": {
                    "heuresMinimumParJour": 3.0,
                    "heuresMaximumParSemaine": 48.0,
                    "joursConsecutifsMaximum": 6
                  }
                }
                """;

        SalarieInputDTO dto = assertLit(json);

        assertEquals(6, dto.getContraintesReglementaires().getJoursConsecutifsMaximum());
        assertNull(dto.getContraintesReglementaires().getNuitsConsecutivesMaximum());
    }

    // =========================================================
    // Helpers
    // =========================================================

    private SalarieInputDTO assertLit(String json) {
        try {
            return objectMapper.readValue(json, SalarieInputDTO.class);
        } catch (Exception e) {
            throw new AssertionError("Désérialisation refusée : " + e.getMessage(), e);
        }
    }

    private static SalarieInputDTO salarieAvecSeuils(Integer nuitsConsecutives,
                                                     Integer reposApresNuits,
                                                     Integer dimanches,
                                                     Integer fenetre,
                                                     Integer joursOff) {
        ContraintesReglementairesDTO c = new ContraintesReglementairesDTO();
        c.setNuitsConsecutivesMaximum(nuitsConsecutives);
        c.setJoursReposMinimumApresNuits(reposApresNuits);
        c.setDimanchesTravaillesMaximum(dimanches);
        c.setReposHebdomadaireFenetreJours(fenetre);
        c.setReposHebdomadaireJoursOffMinimum(joursOff);

        SalarieInputDTO dto = new SalarieInputDTO();
        dto.setId("SAL-S70-01");
        dto.setContraintesReglementaires(c);
        return dto;
    }
}
