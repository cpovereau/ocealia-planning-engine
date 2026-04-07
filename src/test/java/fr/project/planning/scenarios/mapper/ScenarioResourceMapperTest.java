package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.PosteVirtuel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.ressource.TypePosteVirtuel;
import fr.project.planning.scenarios.dto.DataSetDTO;
import fr.project.planning.scenarios.dto.RessourcesDTO;
import fr.project.planning.scenarios.dto.input.ContraintesReglementairesDTO;
import fr.project.planning.scenarios.dto.input.IndisponibiliteItemDTO;
import fr.project.planning.scenarios.dto.input.IndisponibilitesDTO;
import fr.project.planning.scenarios.dto.input.PosteVirtuelInputDTO;
import fr.project.planning.scenarios.dto.input.SalarieInputDTO;
import fr.project.planning.scenarios.dto.request.ResourceKind;
import fr.project.planning.scenarios.dto.request.ResourceRefDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScenarioResourceMapperTest
 *
 * Tests unitaires du ScenarioResourceMapper.
 * Valide le mapping DTO → domaine pour toutes les situations Phase 1-3.
 */
class ScenarioResourceMapperTest {

    private final ScenarioResourceMapper mapper = new ScenarioResourceMapper();

    // -------------------------------------------------------
    // toSalarieReel — champs de base (Phase 1)
    // -------------------------------------------------------

    @Test
    void toSalarieReel_champDeBase_doitMapperIdEtStatut() {
        SalarieInputDTO dto = new SalarieInputDTO();
        dto.setId("1041");
        dto.setStatut("CDI");

        SalarieReel result = mapper.toSalarieReel(dto);

        assertEquals("1041", result.getId());
        assertEquals("CDI", result.getStatut());
        assertNull(result.getProfilContractuel()); // absent du contrat WinDev
    }

    @Test
    void toSalarieReel_sansChamps_doitRetournerSetsVides() {
        SalarieInputDTO dto = new SalarieInputDTO();
        dto.setId("1041");

        SalarieReel result = mapper.toSalarieReel(dto);

        assertNotNull(result.getSitesAutorises());
        assertTrue(result.getSitesAutorises().isEmpty());
        assertNotNull(result.getActivitesCompatibles());
        assertTrue(result.getActivitesCompatibles().isEmpty());
        assertNotNull(result.getPostesComptablesCompatibles());
        assertTrue(result.getPostesComptablesCompatibles().isEmpty());
    }

    @Test
    void toSalarieReel_avecSites_doitMapper() {
        SalarieInputDTO dto = new SalarieInputDTO();
        dto.setId("1041");
        dto.setSitesAutorises(Set.of("HOPITAL-NORD"));
        dto.setActivitesCompatibles(Set.of("ACT-SOIN"));
        dto.setPostesComptablesCompatibles(Set.of("PC-SOINS"));

        SalarieReel result = mapper.toSalarieReel(dto);

        assertTrue(result.getSitesAutorises().contains("HOPITAL-NORD"));
        assertTrue(result.getActivitesCompatibles().contains("ACT-SOIN"));
        assertTrue(result.getPostesComptablesCompatibles().contains("PC-SOINS"));
    }

    // -------------------------------------------------------
    // toSalarieReel — champs Phase 3 (nuit/férié)
    // -------------------------------------------------------

    @Test
    void toSalarieReel_avecChampsNuitFerie_doitMapper() {
        SalarieInputDTO dto = new SalarieInputDTO();
        dto.setId("SAL-2002");
        dto.setTravailDeNuit("occasionnel");
        dto.setHeureDebutNuit(LocalTime.of(22, 0));
        dto.setHeureFinNuit(LocalTime.of(6, 0));
        dto.setTravailleJourFerie(true);

        SalarieReel result = mapper.toSalarieReel(dto);

        assertEquals("occasionnel", result.getTravailDeNuit());
        assertEquals(LocalTime.of(22, 0), result.getHeureDebutNuit());
        assertEquals(LocalTime.of(6, 0), result.getHeureFinNuit());
        assertTrue(result.getTravailleJourFerie());
    }

    @Test
    void toSalarieReel_sansNuit_doitRetournerNullSurChampsNuit() {
        SalarieInputDTO dto = new SalarieInputDTO();
        dto.setId("SAL-2001");
        dto.setTravailDeNuit(null);
        dto.setTravailleJourFerie(false);

        SalarieReel result = mapper.toSalarieReel(dto);

        assertNull(result.getTravailDeNuit());
        assertNull(result.getHeureDebutNuit());
        assertNull(result.getHeureFinNuit());
        assertFalse(result.getTravailleJourFerie());
    }

    // -------------------------------------------------------
    // toSalarieReel — contraintesReglementaires (Phase 3)
    // -------------------------------------------------------

    @Test
    void toSalarieReel_avecContraintes_doitMapperLes8Champs() {
        ContraintesReglementairesDTO crd = new ContraintesReglementairesDTO();
        crd.setHeuresMinimumParJour(4.0);
        crd.setHeuresMaximumParJour(10.0);
        crd.setAmplitudeJournaliereMaximum(11.0);
        crd.setReposQuotidienMinimum(11.0);
        crd.setHeuresMinimumParSemaine(35.0);
        crd.setHeuresMaximumParSemaine(39.0);
        crd.setNuitsMaximumParSemaine(0);
        crd.setJoursConsecutifsMaximum(5);

        SalarieInputDTO dto = new SalarieInputDTO();
        dto.setId("SAL-2001");
        dto.setContraintesReglementaires(crd);

        SalarieReel result = mapper.toSalarieReel(dto);

        ContraintesReglementairesSalarie cr = result.getContraintesReglementaires();
        assertNotNull(cr);
        assertEquals(4.0, cr.getHeuresMinimumParJour());
        assertEquals(10.0, cr.getHeuresMaximumParJour());
        assertEquals(11.0, cr.getAmplitudeJournaliereMaximum());
        assertEquals(11.0, cr.getReposQuotidienMinimum());
        assertEquals(35.0, cr.getHeuresMinimumParSemaine());
        assertEquals(39.0, cr.getHeuresMaximumParSemaine());
        assertEquals(0, cr.getNuitsMaximumParSemaine());
        assertEquals(5, cr.getJoursConsecutifsMaximum());
    }

    @Test
    void toSalarieReel_sansContraintes_doitRetournerNull() {
        SalarieInputDTO dto = new SalarieInputDTO();
        dto.setId("1041");

        SalarieReel result = mapper.toSalarieReel(dto);

        assertNull(result.getContraintesReglementaires());
    }

    // -------------------------------------------------------
    // toPosteVirtuel
    // -------------------------------------------------------

    @Test
    void toPosteVirtuel_doitMapperTousLesChamps() {
        PosteVirtuelInputDTO dto = new PosteVirtuelInputDTO();
        dto.setId("PV-001");
        dto.setType("POTENTIEL");
        dto.setCapaciteCible(2400);
        dto.setActivitesCompatibles(Set.of("ACT-SOIN"));
        dto.setSitesAutorises(Set.of("HOPITAL-NORD"));
        dto.setPostesComptablesCompatibles(Set.of("PC-SOINS"));

        PosteVirtuel result = mapper.toPosteVirtuel(dto);

        assertEquals("PV-001", result.getId());
        assertEquals(TypePosteVirtuel.POTENTIEL, result.getType());
        assertEquals(2400, result.getCapaciteCible());
        assertTrue(result.getActivitesAutorisees().contains("ACT-SOIN"));
    }

    @Test
    void toPosteVirtuel_avecTypeInconnu_doitDefaulterSurPotentiel() {
        PosteVirtuelInputDTO dto = new PosteVirtuelInputDTO();
        dto.setId("PV-X");
        dto.setType("TYPE_INCONNU");

        PosteVirtuel result = mapper.toPosteVirtuel(dto);

        assertEquals(TypePosteVirtuel.POTENTIEL, result.getType());
    }

    // -------------------------------------------------------
    // toRessources
    // -------------------------------------------------------

    @Test
    void toRessources_doitInclureNonAffectee() {
        SalarieInputDTO sal = new SalarieInputDTO();
        sal.setId("1041");
        PosteVirtuelInputDTO pv = new PosteVirtuelInputDTO();
        pv.setId("PV-001");

        RessourcesDTO ressourcesDTO = new RessourcesDTO();
        ressourcesDTO.setSalaries(List.of(sal));
        ressourcesDTO.setPostesVirtuels(List.of(pv));

        DataSetDTO dataSet = new DataSetDTO();
        dataSet.setRessources(ressourcesDTO);

        List<Ressource> result = mapper.toRessources(dataSet);

        assertEquals(3, result.size()); // 1 salarié + 1 PV + 1 NonAffectee
        assertTrue(result.stream().anyMatch(r -> "1041".equals(r.getId())));
        assertTrue(result.stream().anyMatch(r -> "PV-001".equals(r.getId())));
        assertTrue(result.stream().anyMatch(r -> r instanceof RessourceNonAffectee));
    }

    // -------------------------------------------------------
    // resolveResource
    // -------------------------------------------------------

    @Test
    void resolveResource_salarie_doitRetournerLeBonSalarie() {
        SalarieInputDTO sal = new SalarieInputDTO();
        sal.setId("1041");
        sal.setStatut("CDI");

        RessourcesDTO ressourcesDTO = new RessourcesDTO();
        ressourcesDTO.setSalaries(List.of(sal));
        ressourcesDTO.setPostesVirtuels(List.of());

        DataSetDTO dataSet = new DataSetDTO();
        dataSet.setRessources(ressourcesDTO);

        ResourceRefDTO ref = new ResourceRefDTO();
        ref.setId("1041");
        ref.setKind(ResourceKind.SALARIE);

        Ressource result = mapper.resolveResource(dataSet, ref);

        assertEquals("1041", result.getId());
        assertInstanceOf(SalarieReel.class, result);
    }

    @Test
    void resolveResource_introuvable_doitLeverIllegalArgument() {
        RessourcesDTO ressourcesDTO = new RessourcesDTO();
        ressourcesDTO.setSalaries(List.of());
        ressourcesDTO.setPostesVirtuels(List.of());

        DataSetDTO dataSet = new DataSetDTO();
        dataSet.setRessources(ressourcesDTO);

        ResourceRefDTO ref = new ResourceRefDTO();
        ref.setId("9999");
        ref.setKind(ResourceKind.SALARIE);

        assertThrows(IllegalArgumentException.class,
                () -> mapper.resolveResource(dataSet, ref));
    }

    // -------------------------------------------------------
    // toIndisponibilites
    // -------------------------------------------------------

    @Test
    void toIndisponibilites_doitMapperLesItems() {
        IndisponibiliteItemDTO item = new IndisponibiliteItemDTO();
        item.setRessourceId("SAL-2001");
        item.setDateDebut(LocalDate.of(2026, 5, 13));
        item.setDateFin(LocalDate.of(2026, 5, 13));
        item.setMotif("CONGE_POSE");

        IndisponibilitesDTO dto = new IndisponibilitesDTO();
        dto.setItems(List.of(item));

        List<Indisponibilite> result = mapper.toIndisponibilites(dto);

        assertEquals(1, result.size());
        assertEquals("SAL-2001", result.get(0).getRessourceId());
        assertEquals(LocalDate.of(2026, 5, 13), result.get(0).getDateDebut());
        assertEquals("CONGE_POSE", result.get(0).getMotif());
    }

    @Test
    void toIndisponibilites_null_doitRetournerListeVide() {
        assertTrue(mapper.toIndisponibilites(null).isEmpty());
    }

    @Test
    void toIndisponibilites_itemsNull_doitRetournerListeVide() {
        IndisponibilitesDTO dto = new IndisponibilitesDTO();
        dto.setItems(null);
        assertTrue(mapper.toIndisponibilites(dto).isEmpty());
    }

    // -------------------------------------------------------
    // toPosteVirtuel — signal type inconnu Phase 10A
    // -------------------------------------------------------

    @Test
    void toPosteVirtuel_avecTypeInconnu_doitDefaulterSurPotentielSansException() {
        // [Phase 10A] type inconnu → fallback POTENTIEL avec log.warn — aucune exception levée
        PosteVirtuelInputDTO dto = new PosteVirtuelInputDTO();
        dto.setId("PV-P10A-01");
        dto.setType("TYPE_INCONNU_P10A");

        PosteVirtuel result = mapper.toPosteVirtuel(dto);

        assertEquals("PV-P10A-01", result.getId());
        assertEquals(TypePosteVirtuel.POTENTIEL, result.getType());
    }
}