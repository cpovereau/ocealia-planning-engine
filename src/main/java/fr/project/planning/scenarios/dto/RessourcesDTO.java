package fr.project.planning.scenarios.dto;

import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.ressource.PosteVirtuel;

import java.util.List;

public class RessourcesDTO {

    private List<SalarieReel> salaries;
    private List<PosteVirtuel> postesVirtuels;

    public List<SalarieReel> getSalaries() {
        return salaries;
    }

    public void setSalaries(List<SalarieReel> salaries) {
        this.salaries = salaries;
    }

    public List<PosteVirtuel> getPostesVirtuels() {
        return postesVirtuels;
    }

    public void setPostesVirtuels(List<PosteVirtuel> postesVirtuels) {
        this.postesVirtuels = postesVirtuels;
    }
}