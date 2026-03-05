package fr.project.planning.api;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;

import java.util.List;
import java.util.Objects;

public final class PlanningRequest {

    private final PlanningContext planningContext;
    private final RegulatoryParameters regulatoryParameters;
    private final ReferentielComptabiliteActivite referentielComptabiliteActivite;
    private final List<Ressource> ressources;
    private final List<Creneau> creneaux;

    public PlanningRequest(
            PlanningContext planningContext,
            RegulatoryParameters regulatoryParameters,
            ReferentielComptabiliteActivite referentielComptabiliteActivite,
            List<Ressource> ressources,
            List<Creneau> creneaux
    ) {
        this.planningContext = Objects.requireNonNull(planningContext);
        this.regulatoryParameters = Objects.requireNonNull(regulatoryParameters);
        this.referentielComptabiliteActivite = Objects.requireNonNull(referentielComptabiliteActivite);
        this.ressources = List.copyOf(Objects.requireNonNull(ressources));
        this.creneaux = List.copyOf(Objects.requireNonNull(creneaux));
    }

    public PlanningContext planningContext() {
        return planningContext;
    }

    public RegulatoryParameters regulatoryParameters() {
        return regulatoryParameters;
    }

    public ReferentielComptabiliteActivite referentielComptabiliteActivite() {
        return referentielComptabiliteActivite;
    }

    public List<Ressource> ressources() {
        return ressources;
    }

    public List<Creneau> creneaux() {
        return creneaux;
    }
}