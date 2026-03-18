package fr.project.planning.scenarios.dto.input;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalTime;
import java.util.Set;

/**
 * SalarieInputDTO — DTO de transport pour un salarié réel.
 *
 * Remplace l'usage direct de SalarieReel dans RessourcesDTO.
 * Accepte les anciens noms de champs (activitesAutorisees, lieuxAutorises)
 * pour la rétrocompatibilité avec les JSON SC-01 existants.
 *
 * Phase 1 : tous les champs sont transportés mais ceux marqués [Phase 3]
 * ne sont pas encore mappés par le builder.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalarieInputDTO {

    private String id;
    private String statut;

    @JsonAlias("lieuxAutorises")
    private Set<String> sitesAutorises;

    @JsonAlias("activitesAutorisees")
    private Set<String> activitesCompatibles;

    private Set<String> postesComptablesCompatibles;

    // [Phase 3] axes organisationnels
    private AxesOrganisationnelsDTO axesOrganisationnels;

    // [Phase 3] contrat de travail
    private ContratTravailDTO contratTravail;

    // [Phase 3] contraintes réglementaires individuelles (8 champs)
    private ContraintesReglementairesDTO contraintesReglementaires;

    // [Phase 3] statut de travail de nuit : null | "permanent" | "occasionnel"
    private String travailDeNuit;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureDebutNuit;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureFinNuit;

    // [Phase 3] autorisation de travailler les jours fériés
    private Boolean travailleJourFerie;

    // =========================
    // Getters / Setters
    // =========================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Set<String> getSitesAutorises() { return sitesAutorises; }
    public void setSitesAutorises(Set<String> sitesAutorises) { this.sitesAutorises = sitesAutorises; }

    public Set<String> getActivitesCompatibles() { return activitesCompatibles; }
    public void setActivitesCompatibles(Set<String> activitesCompatibles) { this.activitesCompatibles = activitesCompatibles; }

    public Set<String> getPostesComptablesCompatibles() { return postesComptablesCompatibles; }
    public void setPostesComptablesCompatibles(Set<String> postesComptablesCompatibles) { this.postesComptablesCompatibles = postesComptablesCompatibles; }

    public AxesOrganisationnelsDTO getAxesOrganisationnels() { return axesOrganisationnels; }
    public void setAxesOrganisationnels(AxesOrganisationnelsDTO axesOrganisationnels) { this.axesOrganisationnels = axesOrganisationnels; }

    public ContratTravailDTO getContratTravail() { return contratTravail; }
    public void setContratTravail(ContratTravailDTO contratTravail) { this.contratTravail = contratTravail; }

    public ContraintesReglementairesDTO getContraintesReglementaires() { return contraintesReglementaires; }
    public void setContraintesReglementaires(ContraintesReglementairesDTO contraintesReglementaires) { this.contraintesReglementaires = contraintesReglementaires; }

    public String getTravailDeNuit() { return travailDeNuit; }
    public void setTravailDeNuit(String travailDeNuit) { this.travailDeNuit = travailDeNuit; }

    public LocalTime getHeureDebutNuit() { return heureDebutNuit; }
    public void setHeureDebutNuit(LocalTime heureDebutNuit) { this.heureDebutNuit = heureDebutNuit; }

    public LocalTime getHeureFinNuit() { return heureFinNuit; }
    public void setHeureFinNuit(LocalTime heureFinNuit) { this.heureFinNuit = heureFinNuit; }

    public Boolean getTravailleJourFerie() { return travailleJourFerie; }
    public void setTravailleJourFerie(Boolean travailleJourFerie) { this.travailleJourFerie = travailleJourFerie; }
}
