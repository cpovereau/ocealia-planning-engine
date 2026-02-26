package fr.project.planning.scenarios.mapper;

import fr.project.planning.scenarios.dto.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypeCreneau;

public class ScenarioResponseMapper {

    public ScenarioResponseDTO toResponse(String salarieId, List<Creneau> creneauxSolution) {

        // 1) filtrer les créneaux affectés au salarié cible
        List<Creneau> creneauxDuSalarie = creneauxSolution.stream()
            .filter(c -> c.getRessourceAffectee() != null)
            .filter(c -> salarieId.equals(c.getRessourceAffectee().getId()))
            .toList();

        // 2) grouper par date + trier
        List<JourPlanningDTO> jours = creneauxDuSalarie.stream()
            .collect(Collectors.groupingBy(Creneau::getDate))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                LocalDate date = entry.getKey();
                List<CreneauPlanningDTO> creneaux = entry.getValue().stream()
                    .sorted(Comparator.comparing(Creneau::getHeureDebut))
                    .map(this::toCreneauPlanningDTO)
                    .toList();
                return new JourPlanningDTO(date, creneaux);
            })
            .toList();

        ScenarioPlanningDTO planning = new ScenarioPlanningDTO(salarieId, jours);
        return new ScenarioResponseDTO("SOLVED", planning, null);
    }

    private CreneauPlanningDTO toCreneauPlanningDTO(Creneau c) {

        LocalTime debut = c.getHeureDebut();
        LocalTime fin = c.getHeureFin();

        return new CreneauPlanningDTO(
            "travail",                 // V1
            debut,
            fin,
            formatDuree(debut, fin),
            computeNature(c)
        );
    }

    private String formatDuree(LocalTime debut, LocalTime fin) {
        long minutes = Duration.between(debut, fin).toMinutes();
        long h = minutes / 60;
        long m = minutes % 60;
        return String.format("%02d:%02d", h, m);
    }

    private CreneauNature computeNature(Creneau c) {

        // 1) IMPOSE prioritaire sur tout le reste
        if (c.getType() == TypeCreneau.IMPOSE) { 
            return CreneauNature.IMPOSE;
        }

        // 2) Qualification jour (si renseignée)
        QualificationJour q = c.getQualificationJour();
        if (q != null) {
            switch (q) {
                case FERIE:
                    return CreneauNature.FERIE;
                case RH, RHD:
                    return CreneauNature.REPOS;
                case OUVRE:
                default:
                    break;
            }
        }

        // 3) Fallback sur le booléen jour férié (si jamais qualificationJour n'est pas renseignée)
        if (c.isJourFerie()) {
            return CreneauNature.FERIE;
        }

        // 4) TRAVAIL par défaut
        return CreneauNature.TRAVAIL;

        // ABSENCE : à activer le jour où on ajoute un marqueur d'absence exploitable
    }
}

