package fr.project.planning.scenarios.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Set;

/**
 * PosteVirtuelInputDTO — DTO de transport pour un poste virtuel.
 *
 * Remplace l'usage direct de PosteVirtuel dans RessourcesDTO.
 * Champs alignés sur le JSON WinDev (type en String pour éviter le couplage domaine).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PosteVirtuelInputDTO {

    private String id;
    private String type;
    private int capaciteCible;
    private Set<String> activitesAutorisees;
    private Set<String> lieuxAutorises;
    private Set<String> postesComptablesCompatibles;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getCapaciteCible() { return capaciteCible; }
    public void setCapaciteCible(int capaciteCible) { this.capaciteCible = capaciteCible; }

    public Set<String> getActivitesAutorisees() { return activitesAutorisees; }
    public void setActivitesAutorisees(Set<String> activitesAutorisees) { this.activitesAutorisees = activitesAutorisees; }

    public Set<String> getLieuxAutorises() { return lieuxAutorises; }
    public void setLieuxAutorises(Set<String> lieuxAutorises) { this.lieuxAutorises = lieuxAutorises; }

    public Set<String> getPostesComptablesCompatibles() { return postesComptablesCompatibles; }
    public void setPostesComptablesCompatibles(Set<String> postesComptablesCompatibles) { this.postesComptablesCompatibles = postesComptablesCompatibles; }
}
