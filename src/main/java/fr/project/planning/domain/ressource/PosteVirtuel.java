package fr.project.planning.domain.ressource;

import java.util.Set;

/**
 * PosteVirtuel
 *
 * Ressource représentant une capacité de travail potentielle
 * ou révélée par la résolution.
 */
public class PosteVirtuel extends Ressource {

    private TypePosteVirtuel type;

    /**
     * Capacité cible exprimée en minutes ou heures
     * sur la période de résolution.
     */
    private int capaciteCible;

    private Set<String> activitesAutorisees;
    private Set<String> lieuxAutorises;
    private Set<String> postesComptablesCompatibles;

    public PosteVirtuel(
            String id,
            TypePosteVirtuel type,
            int capaciteCible,
            Set<String> activitesAutorisees,
            Set<String> lieuxAutorises,
            Set<String> postesComptablesCompatibles
    ) {
        super(id);
        this.type = type;
        this.capaciteCible = capaciteCible;
        this.activitesAutorisees = activitesAutorisees;
        this.lieuxAutorises = lieuxAutorises;
        this.postesComptablesCompatibles = postesComptablesCompatibles;
    }

    public TypePosteVirtuel getType() {
        return type;
    }

    public int getCapaciteCible() {
        return capaciteCible;
    }

    public Set<String> getActivitesAutorisees() {
        return activitesAutorisees;
    }

    public Set<String> getLieuxAutorises() {
        return lieuxAutorises;
    }

    public Set<String> getPostesComptablesCompatibles() {
        return postesComptablesCompatibles;
    }
}
