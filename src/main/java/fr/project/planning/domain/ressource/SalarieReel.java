package fr.project.planning.domain.ressource;

import java.time.LocalTime;
import java.util.Set;

/**
 * SalarieReel
 *
 * Ressource représentant une personne existante.
 * Immuable pendant la résolution OptaPlanner.
 *
 * Champs Phase 3 (travailDeNuit, heureDebutNuit, heureFinNuit, travailleJourFerie,
 * contraintesReglementaires) : configurés par ScenarioResourceMapper avant la résolution,
 * non modifiés pendant la résolution. Exploités par le solveur à partir de Phase 4.
 */
public class SalarieReel extends Ressource {

    private String profilContractuel;
    private String statut;

    private Set<String> sitesAutorises;
    private Set<String> activitesCompatibles;
    private Set<String> postesComptablesCompatibles;

    // Phase 3 — champs RH/nuit/férié
    private String travailDeNuit;
    private LocalTime heureDebutNuit;
    private LocalTime heureFinNuit;
    private Boolean travailleJourFerie;

    // Phase 3 — contraintes réglementaires individuelles
    private ContraintesReglementairesSalarie contraintesReglementaires;

    public SalarieReel(
            String id,
            String profilContractuel,
            String statut,
            Set<String> sitesAutorises,
            Set<String> activitesCompatibles,
            Set<String> postesComptablesCompatibles
    ) {
        super(id);
        this.profilContractuel = profilContractuel;
        this.statut = statut;
        this.sitesAutorises = sitesAutorises;
        this.activitesCompatibles = activitesCompatibles;
        this.postesComptablesCompatibles = postesComptablesCompatibles;
    }

    // =========================
    // Getters existants
    // =========================

    public String getProfilContractuel() { return profilContractuel; }
    public String getStatut() { return statut; }
    public Set<String> getSitesAutorises() { return sitesAutorises; }
    public Set<String> getActivitesCompatibles() { return activitesCompatibles; }
    public Set<String> getPostesComptablesCompatibles() { return postesComptablesCompatibles; }

    // =========================
    // Getters/Setters Phase 3
    // =========================

    public String getTravailDeNuit() { return travailDeNuit; }
    public void setTravailDeNuit(String travailDeNuit) { this.travailDeNuit = travailDeNuit; }

    public LocalTime getHeureDebutNuit() { return heureDebutNuit; }
    public void setHeureDebutNuit(LocalTime heureDebutNuit) { this.heureDebutNuit = heureDebutNuit; }

    public LocalTime getHeureFinNuit() { return heureFinNuit; }
    public void setHeureFinNuit(LocalTime heureFinNuit) { this.heureFinNuit = heureFinNuit; }

    public Boolean getTravailleJourFerie() { return travailleJourFerie; }
    public void setTravailleJourFerie(Boolean travailleJourFerie) { this.travailleJourFerie = travailleJourFerie; }

    public ContraintesReglementairesSalarie getContraintesReglementaires() { return contraintesReglementaires; }
    public void setContraintesReglementaires(ContraintesReglementairesSalarie contraintesReglementaires) {
        this.contraintesReglementaires = contraintesReglementaires;
    }
}