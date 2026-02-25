package fr.project.planning.scenarios.dto;

import java.time.LocalDate;
import java.util.List;

public class JourPlanningDTO {

    private LocalDate date;
    private List<CreneauPlanningDTO> creneaux;

    public JourPlanningDTO() {}

    public JourPlanningDTO(LocalDate date, List<CreneauPlanningDTO> creneaux) {
        this.date = date;
        this.creneaux = creneaux;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public List<CreneauPlanningDTO> getCreneaux() { return creneaux; }
    public void setCreneaux(List<CreneauPlanningDTO> creneaux) { this.creneaux = creneaux; }
}

