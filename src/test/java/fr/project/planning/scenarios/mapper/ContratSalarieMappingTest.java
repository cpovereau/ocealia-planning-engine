package fr.project.planning.scenarios.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import fr.project.planning.domain.ressource.ContratSalarie;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scenarios.dto.input.ContratSalarieDTO;
import fr.project.planning.scenarios.dto.input.SalarieInputDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ContratSalarieMappingTest — lot S2
 *
 * Valide le bloc `contrat` du salarié : désérialisation, mapping vers le domaine,
 * valeurs par défaut et distinction entre valeur absente et valeur transmise.
 *
 * Le bloc est transporté et mappé, jamais exploité par une contrainte à ce stade.
 */
class ContratSalarieMappingTest {

    private final ScenarioResourceMapper mapper = new ScenarioResourceMapper();

    /** ObjectMapper strict — les annotations des DTO gouvernent la tolérance. */
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    // =========================================================
    // Mapping DTO → domaine
    // =========================================================

    @Test
    void contratComplet_doitMapperLesQuatreChamps() {
        SalarieInputDTO dto = salarieAvecContrat(7.0, 35.0, 5, false);

        ContratSalarie contrat = mapper.toSalarieReel(dto).getContrat();

        assertNotNull(contrat);
        assertEquals(7.0, contrat.getHeuresMoyennesParJour());
        assertEquals(35.0, contrat.getHeuresHebdomadairesHabituelles());
        assertEquals(5, contrat.getJoursTravaillesParSemaine());
        assertEquals(Boolean.FALSE, contrat.getEstAnnualise());
    }

    @Test
    void contratAbsent_doitLaisserLeContratNull() {
        SalarieInputDTO dto = new SalarieInputDTO();
        dto.setId("SAL-S2-02");

        SalarieReel salarie = mapper.toSalarieReel(dto);

        assertNull(salarie.getContrat(),
                "Un contrat absent ne doit pas être fabriqué : l'absence reste distinguable.");
    }

    @Test
    void contratVide_doitMapperUnObjetAuxChampsNuls() {
        // Bloc présent mais sans aucun champ : le moteur doit distinguer ce cas
        // de l'absence totale de bloc.
        SalarieInputDTO dto = new SalarieInputDTO();
        dto.setId("SAL-S2-03");
        dto.setContrat(new ContratSalarieDTO());

        ContratSalarie contrat = mapper.toSalarieReel(dto).getContrat();

        assertNotNull(contrat);
        assertNull(contrat.getHeuresMoyennesParJour());
        assertNull(contrat.getHeuresHebdomadairesHabituelles());
        assertNull(contrat.getJoursTravaillesParSemaine());
        assertNull(contrat.getEstAnnualise());
    }

    // =========================================================
    // Valeurs effectives
    // =========================================================

    @Test
    void joursTravaillesParSemaineAbsent_effectifVautCinq() {
        SalarieInputDTO dto = salarieAvecContrat(7.0, 35.0, null, null);

        ContratSalarie contrat = mapper.toSalarieReel(dto).getContrat();

        assertNull(contrat.getJoursTravaillesParSemaine(),
                "La valeur brute reste nulle : l'absence doit rester lisible.");
        assertEquals(ContratSalarie.JOURS_TRAVAILLES_PAR_SEMAINE_DEFAUT,
                contrat.joursTravaillesParSemaineEffectif());
        assertEquals(5, contrat.joursTravaillesParSemaineEffectif());
    }

    @Test
    void joursTravaillesParSemaineRenseigne_effectifVautLaValeurTransmise() {
        SalarieInputDTO dto = salarieAvecContrat(8.0, 32.0, 4, null);

        ContratSalarie contrat = mapper.toSalarieReel(dto).getContrat();

        assertEquals(4, contrat.getJoursTravaillesParSemaine());
        assertEquals(4, contrat.joursTravaillesParSemaineEffectif());
    }

    @Test
    void estAnnualiseAbsent_doitEtreLuCommeNonAnnualise() {
        // Un vide ne suppose jamais que la chose est possible.
        SalarieInputDTO dto = salarieAvecContrat(7.0, 35.0, 5, null);

        ContratSalarie contrat = mapper.toSalarieReel(dto).getContrat();

        assertNull(contrat.getEstAnnualise());
        assertFalse(contrat.estAnnualise());
    }

    @Test
    void estAnnualiseVrai_doitEtreLuCommeAnnualise() {
        SalarieInputDTO dto = salarieAvecContrat(7.0, 35.0, 5, true);

        ContratSalarie contrat = mapper.toSalarieReel(dto).getContrat();

        assertTrue(contrat.estAnnualise());
    }

    // =========================================================
    // Désérialisation JSON
    // =========================================================

    @Test
    void jsonComplet_doitDeserializerLeBlocContrat() throws Exception {
        String json = """
                {
                  "id": "SAL-2002",
                  "statut": "CDI",
                  "contrat": {
                    "heuresMoyennesParJour": 7.0,
                    "heuresHebdomadairesHabituelles": 35.0,
                    "joursTravaillesParSemaine": 5,
                    "estAnnualise": true
                  }
                }
                """;

        SalarieInputDTO dto = objectMapper.readValue(json, SalarieInputDTO.class);

        assertNotNull(dto.getContrat());
        assertEquals(7.0, dto.getContrat().getHeuresMoyennesParJour());
        assertEquals(35.0, dto.getContrat().getHeuresHebdomadairesHabituelles());
        assertEquals(5, dto.getContrat().getJoursTravaillesParSemaine());
        assertEquals(Boolean.TRUE, dto.getContrat().getEstAnnualise());
    }

    @Test
    void jsonSansBlocContrat_doitResterValide() throws Exception {
        // Non-régression : les salariés SC-01 et SC-03 actuels n'envoient pas ce bloc.
        String json = """
                {
                  "id": "SAL-2001",
                  "statut": "CDI",
                  "sitesAutorises": ["HOPITAL-NORD"],
                  "activitesCompatibles": ["ACT-SOIN"],
                  "travailleJourFerie": false
                }
                """;

        SalarieInputDTO dto = objectMapper.readValue(json, SalarieInputDTO.class);

        assertNull(dto.getContrat());
        assertNull(mapper.toSalarieReel(dto).getContrat());
    }

    @Test
    void jsonAvecChampInconnuDansContrat_doitEchouer() {
        // ContratSalarieDTO est strict : pas de @JsonIgnoreProperties.
        // Un champ inconnu dans ce bloc est une erreur de contrat, pas un oubli toléré.
        String json = """
                {
                  "id": "SAL-2003",
                  "contrat": {
                    "heuresMoyennesParJour": 7.0,
                    "dateDebutContrat": "2020-01-01"
                  }
                }
                """;

        assertThrows(UnrecognizedPropertyException.class,
                () -> objectMapper.readValue(json, SalarieInputDTO.class),
                "Les dates de contrat sont hors périmètre : leur envoi doit être signalé.");
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static SalarieInputDTO salarieAvecContrat(Double heuresJour, Double heuresSemaine,
                                                      Integer joursSemaine, Boolean annualise) {
        ContratSalarieDTO contrat = new ContratSalarieDTO();
        contrat.setHeuresMoyennesParJour(heuresJour);
        contrat.setHeuresHebdomadairesHabituelles(heuresSemaine);
        contrat.setJoursTravaillesParSemaine(joursSemaine);
        contrat.setEstAnnualise(annualise);

        SalarieInputDTO dto = new SalarieInputDTO();
        dto.setId("SAL-S2-01");
        dto.setContrat(contrat);
        return dto;
    }
}
