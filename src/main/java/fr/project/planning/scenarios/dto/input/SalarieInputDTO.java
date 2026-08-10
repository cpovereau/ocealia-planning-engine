package fr.project.planning.scenarios.dto.input;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalTime;
import java.util.Set;

/**
 * SalarieInputDTO — DTO de transport pour un salarié réel.
 *
 * Accepte les anciens noms de champs (activitesAutorisees, lieuxAutorises)
 * pour la rétrocompatibilité avec les JSON SC-01 existants.
 *
 * [Lot S2] Ajout du bloc `contrat` (descriptif). À ne pas confondre avec l'ancien
 * `contratTravail` supprimé en Phase 10C : celui-ci portait des champs jamais exploités
 * et une structure différente. Le nouveau bloc est arbitré au §4.6 du cadrage SC-06.
 *
 * [Phase 10C] Champs IGNORÉS supprimés : axesOrganisationnels, contratTravail.
 * @JsonIgnoreProperties conservé : SC-01 envoie type:"SALARIE" et capaciteCible
 * dans ses objets salarié — ces champs inconnus doivent être absorbés silencieusement.
 * @JsonAlias conservés : SC-01 utilise activitesAutorisees et lieuxAutorises.
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

    // contraintes réglementaires individuelles (8 champs) — prescriptif
    private ContraintesReglementairesDTO contraintesReglementaires;

    // [Lot S2] contrat de travail — descriptif, transporté et mappé, non exploité
    private ContratSalarieDTO contrat;

    // statut de travail de nuit : null | "permanent" | "occasionnel"
    private String travailDeNuit;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureDebutNuit;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureFinNuit;

    // autorisation de travailler les jours fériés
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

    public ContraintesReglementairesDTO getContraintesReglementaires() { return contraintesReglementaires; }
    public void setContraintesReglementaires(ContraintesReglementairesDTO contraintesReglementaires) { this.contraintesReglementaires = contraintesReglementaires; }

    public ContratSalarieDTO getContrat() { return contrat; }
    public void setContrat(ContratSalarieDTO contrat) { this.contrat = contrat; }

    public String getTravailDeNuit() { return travailDeNuit; }
    public void setTravailDeNuit(String travailDeNuit) { this.travailDeNuit = travailDeNuit; }

    public LocalTime getHeureDebutNuit() { return heureDebutNuit; }
    public void setHeureDebutNuit(LocalTime heureDebutNuit) { this.heureDebutNuit = heureDebutNuit; }

    public LocalTime getHeureFinNuit() { return heureFinNuit; }
    public void setHeureFinNuit(LocalTime heureFinNuit) { this.heureFinNuit = heureFinNuit; }

    public Boolean getTravailleJourFerie() { return travailleJourFerie; }
    public void setTravailleJourFerie(Boolean travailleJourFerie) { this.travailleJourFerie = travailleJourFerie; }
}
