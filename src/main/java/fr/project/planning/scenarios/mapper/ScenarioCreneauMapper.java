package fr.project.planning.scenarios.mapper;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.scenarios.dto.input.CreneauInputDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     *
     * <p>Les créneaux produits sont des <strong>variables de décision</strong> : aucun n'est
     * affecté ni figé, quelle que soit la valeur de {@code ressourceAffecteeId}. C'est le
     * comportement attendu de SC-01 et SC-03.</p>
     */
    public List<Creneau> toCreneaux(List<CreneauInputDTO> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .map(this::toCreneau)
                .toList();
    }

    /**
     * Convertit une liste de {@link CreneauInputDTO} en créneaux <strong>figés</strong> sur la
     * ressource que chacun déclare — lot S1.
     *
     * <p>Destiné aux scénarios qui reçoivent un planning existant comme fait acquis : le
     * solveur voit ces créneaux, les contraintes les évaluent, mais aucune décision ne peut
     * les modifier. Voir {@code 92_cadrage_scenario_sc-06.md} §4.2.</p>
     *
     * <p>Un créneau sans {@code ressourceAffecteeId} n'est <strong>pas</strong> figé : il reste
     * une variable de décision. Le signalement de ce cas relève du scénario appelant, seul à
     * savoir s'il constitue une anomalie.</p>
     *
     * @param dtos            créneaux du planning existant
     * @param ressourcesParId index des ressources du problème, par identifiant. Les instances
     *                        doivent être <strong>celles du value range</strong> : une copie
     *                        distincte sortirait du domaine de la variable de décision.
     * @return créneaux figés, dans l'ordre de la liste source
     * @throws IllegalArgumentException si un créneau référence une ressource absente de l'index
     */
    public List<Creneau> toCreneauxFiges(List<CreneauInputDTO> dtos,
                                         Map<String, Ressource> ressourcesParId) {
        if (dtos == null) return List.of();
        Objects.requireNonNull(ressourcesParId, "ressourcesParId");

        List<Creneau> creneaux = new ArrayList<>(dtos.size());
        for (CreneauInputDTO dto : dtos) {
            Creneau creneau = toCreneau(dto);

            String ressourceId = dto.getRessourceAffecteeId();
            if (ressourceId == null || ressourceId.isBlank()) {
                log.warn("[ScenarioCreneauMapper] créneau id='{}' : ressourceAffecteeId absent — "
                        + "créneau non figé, il reste une variable de décision", dto.getId());
                creneaux.add(creneau);
                continue;
            }

            Ressource ressource = ressourcesParId.get(ressourceId);
            if (ressource == null) {
                throw new IllegalArgumentException(
                        "Créneau id='" + dto.getId() + "' : ressource affectée introuvable dans le dataset : "
                                + ressourceId);
            }

            creneau.figerSur(ressource);
            creneaux.add(creneau);
        }
        return creneaux;
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
