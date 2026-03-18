package fr.project.planning.scenarios.dto;

import fr.project.planning.scenarios.dto.input.PosteVirtuelInputDTO;
import fr.project.planning.scenarios.dto.input.SalarieInputDTO;

import java.util.List;

public class RessourcesDTO {

    private List<SalarieInputDTO> salaries;
    private List<PosteVirtuelInputDTO> postesVirtuels;

    public List<SalarieInputDTO> getSalaries() { return salaries; }
    public void setSalaries(List<SalarieInputDTO> salaries) { this.salaries = salaries; }

    public List<PosteVirtuelInputDTO> getPostesVirtuels() { return postesVirtuels; }
    public void setPostesVirtuels(List<PosteVirtuelInputDTO> postesVirtuels) { this.postesVirtuels = postesVirtuels; }
}
