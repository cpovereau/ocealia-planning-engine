package fr.project.planning.scenarios.dto.input;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Set;

/**
 * PosteVirtuelInputDTO — DTO de transport pour un poste virtuel.
 *
 * Remplace l'usage direct de PosteVirtuel dans RessourcesDTO.
 * Champs alignés sur le JSON WinDev (type en String pour éviter le couplage domaine).
 *
 * [Phase 4.3] Renommage des champs Java pour homogénéité avec SalarieInputDTO :
 *   activitesAutorisees → activitesCompatibles (@JsonAlias préserve la rétrocompat JSON)
 *   lieuxAutorises      → sitesAutorises       (@JsonAlias préserve la rétrocompat JSON)
 * Le contrat JSON WinDev (clés activitesAutorisees / lieuxAutorises) reste inchangé.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PosteVirtuelInputDTO {

    private String id;
    private String type;
    private int capaciteCible;

    @JsonAlias("activitesAutorisees")
    private Set<String> activitesCompatibles;

    @JsonAlias("lieuxAutorises")
    private Set<String> sitesAutorises;

    private Set<String> postesComptablesCompatibles;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getCapaciteCible() { return capaciteCible; }
    public void setCapaciteCible(int capaciteCible) { this.capaciteCible = capaciteCible; }

    public Set<String> getActivitesCompatibles() { return activitesCompatibles; }
    public void setActivitesCompatibles(Set<String> activitesCompatibles) { this.activitesCompatibles = activitesCompatibles; }

    public Set<String> getSitesAutorises() { return sitesAutorises; }
    public void setSitesAutorises(Set<String> sitesAutorises) { this.sitesAutorises = sitesAutorises; }

    public Set<String> getPostesComptablesCompatibles() { return postesComptablesCompatibles; }
    public void setPostesComptablesCompatibles(Set<String> postesComptablesCompatibles) { this.postesComptablesCompatibles = postesComptablesCompatibles; }
}
