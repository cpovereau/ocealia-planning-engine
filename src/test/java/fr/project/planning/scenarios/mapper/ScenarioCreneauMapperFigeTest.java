package fr.project.planning.scenarios.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.fixtures.TestRessourceFactory;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ScenarioCreneauMapperFigeTest — lot S1
 *
 * Valide le mapping d'un planning existant transmis comme fait acquis :
 * résolution de {@code ressourceAffecteeId} et figement des créneaux.
 *
 * Couvre également la non-régression de {@code toCreneaux()}, qui doit continuer à produire
 * des variables de décision libres pour SC-01 et SC-03.
 */
class ScenarioCreneauMapperFigeTest {

    private final ScenarioCreneauMapper mapper = new ScenarioCreneauMapper();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private static final LocalDate JOUR = LocalDate.of(2026, 5, 11);

    // =========================================================
    // toCreneauxFiges
    // =========================================================

    @Test
    void creneauAvecRessourceAffectee_doitEtreFigeSurCetteRessource() {
        SalarieReel salarie = TestRessourceFactory.salarieStandard("SAL-2001");
        Map<String, Ressource> index = Map.of("SAL-2001", salarie);

        List<Creneau> creneaux = mapper.toCreneauxFiges(List.of(dto("CRE-01", "SAL-2001")), index);

        Creneau creneau = creneaux.get(0);
        assertTrue(creneau.isFige());
        assertSame(salarie, creneau.getRessourceAffectee(),
                "L'instance figée doit être celle du value range, pas une copie.");
    }

    @Test
    void creneauSansRessourceAffectee_resteUneVariableDeDecision() {
        Map<String, Ressource> index = Map.of("SAL-2001", TestRessourceFactory.salarieStandard("SAL-2001"));

        List<Creneau> creneaux = mapper.toCreneauxFiges(List.of(dto("CRE-02", null)), index);

        Creneau creneau = creneaux.get(0);
        assertFalse(creneau.isFige());
        assertNull(creneau.getRessourceAffectee());
    }

    @Test
    void creneauAvecRessourceAffecteeVide_resteUneVariableDeDecision() {
        Map<String, Ressource> index = Map.of("SAL-2001", TestRessourceFactory.salarieStandard("SAL-2001"));

        List<Creneau> creneaux = mapper.toCreneauxFiges(List.of(dto("CRE-03", "   ")), index);

        assertFalse(creneaux.get(0).isFige());
    }

    @Test
    void creneauReferencantUneRessourceInconnue_doitEchouer() {
        // Un planning qui référence une ressource absente du dataset est une incohérence
        // structurelle : la taire produirait un résultat faux plutôt qu'une erreur.
        Map<String, Ressource> index = Map.of("SAL-2001", TestRessourceFactory.salarieStandard("SAL-2001"));

        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> mapper.toCreneauxFiges(List.of(dto("CRE-04", "SAL-INCONNU")), index));

        assertTrue(erreur.getMessage().contains("CRE-04"), "Le message doit situer le créneau fautif.");
        assertTrue(erreur.getMessage().contains("SAL-INCONNU"), "Le message doit nommer la ressource absente.");
    }

    @Test
    void listeNulle_doitRetournerListeVide() {
        assertTrue(mapper.toCreneauxFiges(null, Map.of()).isEmpty());
    }

    @Test
    void ordreDeLaListeSource_doitEtreConserve() {
        SalarieReel s1 = TestRessourceFactory.salarieStandard("SAL-1");
        SalarieReel s2 = TestRessourceFactory.salarieStandard("SAL-2");
        Map<String, Ressource> index = Map.of("SAL-1", s1, "SAL-2", s2);

        List<Creneau> creneaux = mapper.toCreneauxFiges(
                List.of(dto("CRE-A", "SAL-1"), dto("CRE-B", "SAL-2"), dto("CRE-C", "SAL-1")), index);

        assertEquals(List.of("CRE-A", "CRE-B", "CRE-C"), creneaux.stream().map(Creneau::getId).toList());
    }

    @Test
    void champsMetierHabituels_restentMappes() {
        // Le figement ne doit rien retirer au mapping existant.
        SalarieReel salarie = TestRessourceFactory.salarieStandard("SAL-2001");
        CreneauInputDTO dto = dto("CRE-05", "SAL-2001");
        dto.setGroupeBesoinId("BESOIN-01");
        dto.setBlocJourId("BLOC-A");
        dto.setOrdreDansBloc(2);
        dto.setEstSegmentDePause(false);

        Creneau creneau = mapper.toCreneauxFiges(List.of(dto), Map.of("SAL-2001", salarie)).get(0);

        assertEquals("BESOIN-01", creneau.getGroupeBesoinId());
        assertEquals("BLOC-A", creneau.getBlocJourId());
        assertEquals(2, creneau.getOrdreDansBloc());
        assertEquals(480, creneau.getDuree());
        assertEquals("HOPITAL-NORD", creneau.getLieu());
    }

    // =========================================================
    // Non-régression SC-01 / SC-03
    // =========================================================

    @Test
    void toCreneaux_ignoreRessourceAffecteeId_etNeFigeRien() {
        // SC-01 et SC-03 passent par toCreneaux() : même si un ressourceAffecteeId traînait
        // dans leur dataset, aucun créneau ne doit être figé ni pré-affecté.
        List<Creneau> creneaux = mapper.toCreneaux(List.of(dto("CRE-06", "SAL-2001")));

        Creneau creneau = creneaux.get(0);
        assertFalse(creneau.isFige());
        assertNull(creneau.getRessourceAffectee());
    }

    // =========================================================
    // Désérialisation
    // =========================================================

    @Test
    void jsonAvecRessourceAffecteeId_doitDeserializer() throws Exception {
        String json = """
                {
                  "id": "1041-20260511-01",
                  "date": "2026-05-11",
                  "heureDebut": "07:00",
                  "heureFin": "15:00",
                  "lieu": "HOPITAL-NORD",
                  "codeActiviteId": "ACT-SOIN",
                  "ressourceAffecteeId": "SAL-2002"
                }
                """;

        CreneauInputDTO dto = objectMapper.readValue(json, CreneauInputDTO.class);

        assertEquals("SAL-2002", dto.getRessourceAffecteeId());
    }

    @Test
    void jsonSansRessourceAffecteeId_resteValide() throws Exception {
        // Non-régression : le contrat SC-03 actuel ne porte pas ce champ.
        String json = """
                {
                  "id": "CRE-L-01",
                  "date": "2026-05-11",
                  "heureDebut": "07:00",
                  "heureFin": "15:00",
                  "codeActiviteId": "ACT-SOIN"
                }
                """;

        CreneauInputDTO dto = objectMapper.readValue(json, CreneauInputDTO.class);

        assertNull(dto.getRessourceAffecteeId());
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static CreneauInputDTO dto(String id, String ressourceAffecteeId) {
        CreneauInputDTO dto = new CreneauInputDTO();
        dto.setId(id);
        dto.setDate(JOUR);
        dto.setHeureDebut(LocalTime.of(7, 0));
        dto.setHeureFin(LocalTime.of(15, 0));
        dto.setLieu("HOPITAL-NORD");
        dto.setCodeActiviteId("ACT-SOIN");
        dto.setRessourceAffecteeId(ressourceAffecteeId);
        return dto;
    }
}
