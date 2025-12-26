package fr.project.planning.domain.ressource;

import java.io.Serializable;

/**
 * Ressource
 *
 * Abstraction d'une capacité de travail mobilisable.
 * Peut être :
 * - un salarié réel
 * - un poste virtuel
 * - un état spécial (ex : A_AFFECTER)
 */
public abstract class Ressource implements Serializable {

    private String id;

    protected Ressource() {
        // pour sérialisation / frameworks
    }

    protected Ressource(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
