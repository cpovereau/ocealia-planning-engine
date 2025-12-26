package fr.project.planning.domain.ressource;

import java.util.Set;

/**
 * SalarieReel
 *
 * Ressource représentant une personne existante.
 * Totalement immuable pendant la résolution.
 */
public class SalarieReel extends Ressource {

    private String profilContractuel;
    private String statut;

    private Set<String> sitesAutorises;
    private Set<String> activitesCompatibles;
    private Set<String> postesComptablesCompatibles;

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

    public String getProfilContractuel() {
        return profilContractuel;
    }

    public String getStatut() {
        return statut;
    }

    public Set<String> getSitesAutorises() {
        return sitesAutorises;
    }

    public Set<String> getActivitesCompatibles() {
        return activitesCompatibles;
    }

    public Set<String> getPostesComptablesCompatibles() {
        return postesComptablesCompatibles;
    }
}
