package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

/**
 * ScenarioCreneauMapper
 *
 * Convertit les {@link CreneauInputDTO} (couche transport WinDev) en objets domaine {@link Creneau}.
 *
 * Phase 5 : mapping des champs de structuration des besoins
 *   (groupeBesoinId, blocJourId, ordreDansBloc, estSegmentDePause).
 *
 * Ne modifie pas le solveur ni le scoring.
 * Ne remplace pas le builder SC-01 qui génère ses propres créneaux.
 */
@Service
public class ScenarioCreneauMapper {

    private static final Logger log = LoggerFactory.getLogger(ScenarioCreneauMapper.class);

    /**
     * Convertit un {@link CreneauInputDTO} en {@link Creneau} domaine.
     *
     * Règles appliquées :
     * - {@code duree} calculée (gestion traversée minuit) ;
     * - {@code typePlageHoraire} déduit de {@code segmentNuit} ;
     * - {@code jourFerie} déduit de {@code isJourFerie} ;
     * - {@code qualificationJour} = OUVRE par défaut (non computable depuis le DTO seul) ;
     * - {@code type} = IMPOSE (créneau issu du métier WinDev) ;
     * - les 4 champs de structuration Phase 5 sont portés via setters.
     */
    public Creneau toCreneau(CreneauInputDTO dto) {
        int duree = calculerDureeMinutes(dto.getHeureDebut(), dto.getHeureFin());
        if (duree <= 0) {
            log.warn("[ScenarioCreneauMapper] duree calculée invalide ({} min) pour le créneau {} — heureDebut={} heureFin={}",
                    duree, dto.getId(), dto.getHeureDebut(), dto.getHeureFin());
        }

        TypePlageHoraire typePlageHoraire = Boolean.TRUE.equals(dto.getSegmentNuit())
                ? TypePlageHoraire.NUIT
                : TypePlageHoraire.JOUR;

        boolean jourFerie = Boolean.TRUE.equals(dto.getIsJourFerie());

        Creneau creneau = new Creneau(
                dto.getId(),
                dto.getDate(),
                dto.getHeureDebut(),
                dto.getHeureFin(),
                duree,
                dto.getLieu(),
                dto.getCodeActiviteId(),
                dto.getActivite(),
                dto.getPosteComptable(),
                null,                       // PrioriteCreneau — non exploité Phase 5
                TypeCreneau.IMPOSE,
                typePlageHoraire,
                jourFerie,
                QualificationJour.OUVRE     // non computable depuis le DTO seul
        );

        // Phase 5 — structuration des besoins
        creneau.setGroupeBesoinId(dto.getGroupeBesoinId());
        creneau.setBlocJourId(dto.getBlocJourId());
        creneau.setOrdreDansBloc(dto.getOrdreDansBloc());
        creneau.setEstSegmentDePause(dto.getEstSegmentDePause());

        return creneau;
    }

    /**
     * Convertit une liste de {@link CreneauInputDTO} en liste de {@link Creneau}.
     * L'ordre de la liste source est conservé.
     */
    public List<Creneau> toCreneaux(List<CreneauInputDTO> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .map(this::toCreneau)
                .toList();
    }

    // =========================
    // Utilitaires privés
    // =========================

    /**
     * Calcule la durée en minutes entre heureDebut et heureFin.
     * Gère la traversée minuit (heureFin < heureDebut).
     */
    private int calculerDureeMinutes(LocalTime heureDebut, LocalTime heureFin) {
        int debut = heureDebut.getHour() * 60 + heureDebut.getMinute();
        int fin   = heureFin.getHour()   * 60 + heureFin.getMinute();
        if (fin > debut) {
            return fin - debut;
        }
        // traversée minuit
        return (24 * 60 - debut) + fin;
    }
}
