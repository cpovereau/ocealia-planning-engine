package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Phase 5 — ScenarioCreneauMapper
 *
 * Vérifie :
 * - mapping des champs de base (id, date, heures, lieu, activité)
 * - mapping des 4 champs de structuration (groupeBesoinId, blocJourId, ordreDansBloc, estSegmentDePause)
 * - détection segment de pause
 * - détection segment de nuit (TypePlageHoraire)
 * - détection jour férié
 * - calcul de durée (dont traversée minuit)
 * - mapping de liste avec conservation de l'ordre
 * - null-safety sur les champs optionnels
 */
class ScenarioCreneauMapperTest {

    private final ScenarioCreneauMapper mapper = new ScenarioCreneauMapper();

    private static final LocalDate DATE = LocalDate.of(2026, 3, 16);

    // ----------------------------------------------------------
    // Test 1 : mapping des champs de base
    // ----------------------------------------------------------

    @Test
    void toCreneau_mappeFieldsDeBase() {
        CreneauInputDTO dto = new CreneauInputDTO();
        dto.setId("CRE-001");
        dto.setDate(DATE);
        dto.setHeureDebut(LocalTime.of(8, 0));
        dto.setHeureFin(LocalTime.of(16, 0));
        dto.setLieu("LIEU-A");
        dto.setCodeActiviteId("ACT-01");
        dto.setActivite("Soins");
        dto.setPosteComptable("PC-01");

        Creneau creneau = mapper.toCreneau(dto);

        assertEquals("CRE-001", creneau.getId());
        assertEquals(DATE, creneau.getDate());
        assertEquals(LocalTime.of(8, 0), creneau.getHeureDebut());
        assertEquals(LocalTime.of(16, 0), creneau.getHeureFin());
        assertEquals("LIEU-A", creneau.getLieu());
        assertEquals("ACT-01", creneau.getCodeActiviteId());
        assertEquals("Soins", creneau.getActivite());
        assertEquals("PC-01", creneau.getPosteComptable());
        assertEquals(480, creneau.getDuree());
    }

    // ----------------------------------------------------------
    // Test 2 : mapping des 4 champs de structuration Phase 5
    // ----------------------------------------------------------

    @Test
    void toCreneau_mappeChampsDeSructurationPhase5() {
        CreneauInputDTO dto = creneauBaseDto("CRE-L-01", LocalTime.of(7, 0), LocalTime.of(15, 0));
        dto.setGroupeBesoinId("BESOIN-SEMAINE-01");
        dto.setBlocJourId("BLOC-LUNDI-MATIN");
        dto.setOrdreDansBloc(1);
        dto.setEstSegmentDePause(false);

        Creneau creneau = mapper.toCreneau(dto);

        assertEquals("BESOIN-SEMAINE-01", creneau.getGroupeBesoinId());
        assertEquals("BLOC-LUNDI-MATIN", creneau.getBlocJourId());
        assertEquals(1, creneau.getOrdreDansBloc());
        assertFalse(Boolean.TRUE.equals(creneau.getEstSegmentDePause()));
    }

    // ----------------------------------------------------------
    // Test 3 : estSegmentDePause = true est correctement transporté
    // ----------------------------------------------------------

    @Test
    void toCreneau_estSegmentDePause_true_estBienTransporte() {
        CreneauInputDTO dto = creneauBaseDto("CRE-PAUSE-01", LocalTime.of(12, 0), LocalTime.of(13, 0));
        dto.setGroupeBesoinId("BESOIN-SEMAINE-01");
        dto.setBlocJourId("BLOC-LUNDI-MIDI");
        dto.setOrdreDansBloc(2);
        dto.setEstSegmentDePause(true);

        Creneau creneau = mapper.toCreneau(dto);

        assertTrue(Boolean.TRUE.equals(creneau.getEstSegmentDePause()));
    }

    // ----------------------------------------------------------
    // Test 4 : segmentNuit = true → TypePlageHoraire.NUIT
    // ----------------------------------------------------------

    @Test
    void toCreneau_segmentNuit_setTypeNuit() {
        CreneauInputDTO dto = creneauBaseDto("CRE-VN-01", LocalTime.of(22, 0), LocalTime.of(6, 0));
        dto.setSegmentNuit(true);
        dto.setGroupeBesoinId("BESOIN-NUIT-01");

        Creneau creneau = mapper.toCreneau(dto);

        assertEquals(TypePlageHoraire.NUIT, creneau.getTypePlageHoraire());
        assertEquals("BESOIN-NUIT-01", creneau.getGroupeBesoinId());
    }

    // ----------------------------------------------------------
    // Test 5 : traversée minuit — durée calculée correctement
    // ----------------------------------------------------------

    @Test
    void toCreneau_traverseeMinuit_dureeCorrecte() {
        CreneauInputDTO dto = creneauBaseDto("CRE-NUIT", LocalTime.of(22, 0), LocalTime.of(6, 0));
        dto.setSegmentNuit(true);

        Creneau creneau = mapper.toCreneau(dto);

        assertEquals(480, creneau.getDuree()); // 22h00 → 06h00 = 8h = 480 min
    }

    // ----------------------------------------------------------
    // Test 6 : isJourFerie = true → jourFerie = true sur le domaine
    // ----------------------------------------------------------

    @Test
    void toCreneau_jourFerie_estBienTransporte() {
        CreneauInputDTO dto = creneauBaseDto("CRE-ME-01", LocalTime.of(7, 0), LocalTime.of(15, 0));
        dto.setIsJourFerie(true);
        dto.setGroupeBesoinId("BESOIN-FERIE-01");

        Creneau creneau = mapper.toCreneau(dto);

        assertTrue(creneau.isJourFerie());
    }

    // ----------------------------------------------------------
    // Test 7 : toCreneaux conserve l'ordre et mappe tous les éléments
    // ----------------------------------------------------------

    @Test
    void toCreneaux_conserveOrdreEtMappeElements() {
        CreneauInputDTO dto1 = creneauBaseDto("CRE-001", LocalTime.of(7, 0), LocalTime.of(12, 0));
        dto1.setBlocJourId("BLOC-A");
        dto1.setOrdreDansBloc(1);

        CreneauInputDTO dto2 = creneauBaseDto("CRE-002", LocalTime.of(13, 0), LocalTime.of(17, 0));
        dto2.setBlocJourId("BLOC-A");
        dto2.setOrdreDansBloc(2);

        CreneauInputDTO dto3 = creneauBaseDto("CRE-003", LocalTime.of(22, 0), LocalTime.of(6, 0));
        dto3.setBlocJourId("BLOC-B");
        dto3.setOrdreDansBloc(1);

        List<Creneau> creneaux = mapper.toCreneaux(List.of(dto1, dto2, dto3));

        assertEquals(3, creneaux.size());
        assertEquals("CRE-001", creneaux.get(0).getId());
        assertEquals(1, creneaux.get(0).getOrdreDansBloc());
        assertEquals("CRE-002", creneaux.get(1).getId());
        assertEquals(2, creneaux.get(1).getOrdreDansBloc());
        assertEquals("CRE-003", creneaux.get(2).getId());
        assertEquals("BLOC-B", creneaux.get(2).getBlocJourId());
    }

    // ----------------------------------------------------------
    // Test 8 : champs Phase 5 null si absents du DTO
    // ----------------------------------------------------------

    @Test
    void toCreneau_champsPhase5NullSiAbsents() {
        CreneauInputDTO dto = creneauBaseDto("CRE-MINIMAL", LocalTime.of(9, 0), LocalTime.of(17, 0));
        // groupeBesoinId, blocJourId, ordreDansBloc, estSegmentDePause non renseignés

        Creneau creneau = mapper.toCreneau(dto);

        assertNull(creneau.getGroupeBesoinId());
        assertNull(creneau.getBlocJourId());
        assertNull(creneau.getOrdreDansBloc());
        assertNull(creneau.getEstSegmentDePause());
    }

    // ----------------------------------------------------------
    // Test 9 : champ 'type' du DTO toujours ignoré — TypeCreneau.IMPOSE appliqué
    // [Phase 1 visibilité — T-07]
    // ----------------------------------------------------------

    @Test
    void toCreneau_typeIgnore_imoseApplique_quelqueSoitLaValeurDTO() {
        CreneauInputDTO dto = creneauBaseDto("CRE-TYPE-01", LocalTime.of(8, 0), LocalTime.of(16, 0));
        dto.setType("URGENT");

        Creneau creneau = mapper.toCreneau(dto);

        assertEquals(TypeCreneau.IMPOSE, creneau.getType(),
                "TypeCreneau.IMPOSE doit être appliqué quelle que soit la valeur du champ type du DTO");
    }

    @Test
    void toCreneau_typeNull_imoseApplique() {
        CreneauInputDTO dto = creneauBaseDto("CRE-TYPE-02", LocalTime.of(8, 0), LocalTime.of(16, 0));
        // type non renseigné

        Creneau creneau = mapper.toCreneau(dto);

        assertEquals(TypeCreneau.IMPOSE, creneau.getType());
    }

    // ----------------------------------------------------------
    // Utilitaire
    // ----------------------------------------------------------

    private CreneauInputDTO creneauBaseDto(String id, LocalTime debut, LocalTime fin) {
        CreneauInputDTO dto = new CreneauInputDTO();
        dto.setId(id);
        dto.setDate(DATE);
        dto.setHeureDebut(debut);
        dto.setHeureFin(fin);
        dto.setLieu("LIEU-TEST");
        dto.setCodeActiviteId("ACT-TEST");
        dto.setActivite("Activité test");
        return dto;
    }
}
