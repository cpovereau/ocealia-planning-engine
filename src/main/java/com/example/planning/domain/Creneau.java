package com.example.planning.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class Creneau {

    private String id;

    @PlanningVariable(valueRangeProviderRefs = "ressources")
    private Ressource ressourceAffectee;

    public Creneau() {
        // requis par OptaPlanner
    }

    public Creneau(String id) {
        this.id = id;
        this.ressourceAffectee = AffectationEtat.A_AFFECTER;
    }

    public String getId() {
        return id;
    }

    public Ressource getRessourceAffectee() {
        return ressourceAffectee;
    }

    public void setRessourceAffectee(Ressource ressourceAffectee) {
        this.ressourceAffectee = ressourceAffectee;
    }

    private Ressource ressourceNonPreferee;

    public Ressource getRessourceNonPreferee() {
    return ressourceNonPreferee;
    }

    public void setRessourceNonPreferee(Ressource ressourceNonPreferee) {
        this.ressourceNonPreferee = ressourceNonPreferee;
    }

}
