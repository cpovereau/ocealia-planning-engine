package fr.project.planning.scenarios.dto;

import java.util.List;

public class ScenarioPlanningDTO {

    private String idSalarie;
    private List<JourPlanningDTO> jours;

    public ScenarioPlanningDTO() {}

    public ScenarioPlanningDTO(String idSalarie, List<JourPlanningDTO> jours) {
        this.idSalarie = idSalarie;
        this.jours = jours;
    }

    public String getIdSalarie() { return idSalarie; }
    public void setIdSalarie(String idSalarie) { this.idSalarie = idSalarie; }

    public List<JourPlanningDTO> getJours() { return jours; }
    public void setJours(List<JourPlanningDTO> jours) { this.jours = jours; }
}
