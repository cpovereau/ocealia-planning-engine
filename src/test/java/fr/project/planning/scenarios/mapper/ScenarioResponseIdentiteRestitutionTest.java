package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import fr.project.planning.scenarios.dto.CreneauPlanningDTO;
import fr.project.planning.scenarios.dto.ScenarioResponseDTO;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Restitution de l'identifiant et du lieu d'un créneau.
 *
 * <p>Règle contractuelle (`50_ScenarioResponseContract.md` §2.1) : toute donnée reçue
 * permettant d'identifier ou de situer un créneau est restituée <b>à l'identique</b>.
 * L'{@code id} est la clé de réintégration côté appelant ; sans lui, le planning ne peut
 * être rattaché aux lignes d'entrée que par triangulation.
 *
 * <p>Invariant vérifié ici (`20_DECISIONS_CONCEPTION_OPTAPLANNER.md` — <i>Identité des
 * créneaux et clé de réintégration WinDev</i>) : l'{@code id} est une chaîne opaque, jamais
 * interprétée ni normalisée par le moteur, quelle que soit la convention de préfixe employée
 * par l'appelant.
 */
class ScenarioResponseIdentiteRestitutionTest {

    private static final LocalDate JOUR = LocalDate.of(2026, 7, 27);

    private final ScenarioCreneauMapper creneauMapper = new ScenarioCreneauMapper();
    private final ScenarioResponseMapper responseMapper = new ScenarioResponseMapper();

    // ----------------------------------------------------------
    // Aller-retour entrée → domaine → sortie
    // ----------------------------------------------------------

    @Test
    void creneauServi_doitRestituerSonIdentifiantEtSonLieu() {
        Creneau entrant = creneauMapper.toCreneau(creneauEntrant("12345", "HOPITAL-NORD"));

        CreneauPlanningDTO sortant = premierCreneau(toResponse(entrant));

        assertEquals("12345", sortant.getId());
        assertEquals("HOPITAL-NORD", sortant.getLieu());
    }

    @Test
    void besoinNonServi_doitRestituerSonIdentifiantDedie() {
        Creneau entrant = creneauMapper.toCreneau(creneauEntrant("BES-007", "HOPITAL-SUD"));

        CreneauPlanningDTO sortant = premierCreneau(toResponse(entrant));

        assertEquals("BES-007", sortant.getId());
        assertEquals("HOPITAL-SUD", sortant.getLieu());
    }

    // ----------------------------------------------------------
    // Opacité de l'identifiant
    // ----------------------------------------------------------

    @Test
    void identifiantGenereParLeMoteur_doitEtreRestitueTelQuel() {
        Creneau genere = creneauDomaine("SC01-2026-07-27-001", null);

        CreneauPlanningDTO sortant = premierCreneau(toResponse(genere));

        assertEquals("SC01-2026-07-27-001", sortant.getId());
    }

    @Test
    void identifiantDeFormeInattendue_doitEtreRestitueSansNormalisation() {
        Creneau exotique = creneauDomaine("  x/42#B  ", "SITE-A");

        CreneauPlanningDTO sortant = premierCreneau(toResponse(exotique));

        assertEquals("  x/42#B  ", sortant.getId());
    }

    // ----------------------------------------------------------
    // Absence de lieu
    // ----------------------------------------------------------

    @Test
    void creneauSansLieu_doitRestituerNullSansSubstitution() {
        Creneau sansLieu = creneauDomaine("SC01-2026-07-27-001", null);

        assertNull(premierCreneau(toResponse(sansLieu)).getLieu());
    }

    // ----------------------------------------------------------
    // helpers
    // ----------------------------------------------------------

    private ScenarioResponseDTO toResponse(Creneau creneau) {
        return responseMapper.toResponse(
                "SC-03",
                "SOLVED",
                0,
                0,
                List.of(),
                "SAL-2001",
                List.of(creneau),
                Map.of(),
                List.of(),
                Set.of(),
                null
        );
    }

    private CreneauPlanningDTO premierCreneau(ScenarioResponseDTO response) {
        return response.getPlanning().getJours().get(0).getCreneaux().get(0);
    }

    private CreneauInputDTO creneauEntrant(String id, String lieu) {
        CreneauInputDTO dto = new CreneauInputDTO();
        dto.setId(id);
        dto.setDate(JOUR);
        dto.setHeureDebut(LocalTime.of(7, 0));
        dto.setHeureFin(LocalTime.of(15, 0));
        dto.setLieu(lieu);
        dto.setCodeActiviteId("ACT-SOIN");
        return dto;
    }

    private Creneau creneauDomaine(String id, String lieu) {
        Creneau creneau = new Creneau(
                id,
                JOUR,
                LocalTime.of(7, 0),
                LocalTime.of(15, 0),
                480,
                lieu,
                "ACT-SOIN",
                null,
                null,
                null,
                TypeCreneau.GENERE,
                TypePlageHoraire.JOUR,
                false,
                QualificationJour.OUVRE
        );
        creneau.setRessourceAffectee(RessourceNonAffectee.INSTANCE);
        return creneau;
    }
}
