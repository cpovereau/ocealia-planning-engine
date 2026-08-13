package fr.project.planning.api;

import fr.project.planning.domain.contexte.PerimetreArbitre;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.SeuilsSurcharge;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;
import fr.project.planning.domain.repos.ReposHebdomadaire;
import fr.project.planning.domain.ressource.Indisponibilite;
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

    /**
     * Phase 3 : indisponibilités transportées et mappées.
     * Exploitées par le solveur à partir de Phase 4.
     */
    private final List<Indisponibilite> indisponibilites;

    /** Lot S7.9b : calendrier de repos hebdomadaire, un fait par salarié et par jour concerné. */
    private final List<ReposHebdomadaire> reposHebdomadaires;

    /**
     * Lot S3 de SC-02 : seuils de surcharge acceptables, propres à la demande.
     *
     * <p>{@code null} pour tout scénario qui n'en déclare pas — c'est-à-dire tous sauf SC-02
     * aujourd'hui. Aucune contrainte ne s'en saisit alors.</p>
     */
    private final SeuilsSurcharge seuilsSurcharge;

    /**
     * Lot A0 de SC-05 : périmètre remis en jeu par un arbitrage, et ressources autorisées.
     *
     * <p>{@code null} pour tout scénario qui n'arbitre rien — c'est-à-dire tous sauf SC-05.
     * {@code AffectationHorsRessourcesAutorisees} ne se déclenche alors jamais.</p>
     */
    private final PerimetreArbitre perimetreArbitre;

    /** Constructeur Phase 1/2 — sans indisponibilités (rétrocompatibilité). */
    public PlanningRequest(
            PlanningContext planningContext,
            RegulatoryParameters regulatoryParameters,
            ReferentielComptabiliteActivite referentielComptabiliteActivite,
            List<Ressource> ressources,
            List<Creneau> creneaux
    ) {
        this(planningContext, regulatoryParameters, referentielComptabiliteActivite,
                ressources, creneaux, List.of());
    }

    /** Constructeur Phase 3+ — avec indisponibilités, sans calendrier de repos. */
    public PlanningRequest(
            PlanningContext planningContext,
            RegulatoryParameters regulatoryParameters,
            ReferentielComptabiliteActivite referentielComptabiliteActivite,
            List<Ressource> ressources,
            List<Creneau> creneaux,
            List<Indisponibilite> indisponibilites
    ) {
        this(planningContext, regulatoryParameters, referentielComptabiliteActivite,
                ressources, creneaux, indisponibilites, List.of());
    }

    /** Constructeur lot S7.9b — avec le calendrier de repos hebdomadaire. */
    public PlanningRequest(
            PlanningContext planningContext,
            RegulatoryParameters regulatoryParameters,
            ReferentielComptabiliteActivite referentielComptabiliteActivite,
            List<Ressource> ressources,
            List<Creneau> creneaux,
            List<Indisponibilite> indisponibilites,
            List<ReposHebdomadaire> reposHebdomadaires
    ) {
        this(planningContext, regulatoryParameters, referentielComptabiliteActivite, ressources,
                creneaux, indisponibilites, reposHebdomadaires, null);
    }

    /** Constructeur lot S3 de SC-02 — avec les seuils de surcharge de la demande. */
    public PlanningRequest(
            PlanningContext planningContext,
            RegulatoryParameters regulatoryParameters,
            ReferentielComptabiliteActivite referentielComptabiliteActivite,
            List<Ressource> ressources,
            List<Creneau> creneaux,
            List<Indisponibilite> indisponibilites,
            List<ReposHebdomadaire> reposHebdomadaires,
            SeuilsSurcharge seuilsSurcharge
    ) {
        this(planningContext, regulatoryParameters, referentielComptabiliteActivite, ressources,
                creneaux, indisponibilites, reposHebdomadaires, seuilsSurcharge, null);
    }

    /** Constructeur lot A0 de SC-05 — avec le périmètre remis en jeu par un arbitrage. */
    public PlanningRequest(
            PlanningContext planningContext,
            RegulatoryParameters regulatoryParameters,
            ReferentielComptabiliteActivite referentielComptabiliteActivite,
            List<Ressource> ressources,
            List<Creneau> creneaux,
            List<Indisponibilite> indisponibilites,
            List<ReposHebdomadaire> reposHebdomadaires,
            SeuilsSurcharge seuilsSurcharge,
            PerimetreArbitre perimetreArbitre
    ) {
        this.seuilsSurcharge = seuilsSurcharge;
        this.perimetreArbitre = perimetreArbitre;
        this.planningContext = Objects.requireNonNull(planningContext);
        this.regulatoryParameters = Objects.requireNonNull(regulatoryParameters);
        this.referentielComptabiliteActivite = Objects.requireNonNull(referentielComptabiliteActivite);
        this.ressources = List.copyOf(Objects.requireNonNull(ressources));
        this.creneaux = List.copyOf(Objects.requireNonNull(creneaux));
        this.indisponibilites = List.copyOf(Objects.requireNonNull(indisponibilites));
        this.reposHebdomadaires = List.copyOf(Objects.requireNonNull(reposHebdomadaires));
    }

    public PlanningContext planningContext() { return planningContext; }
    public RegulatoryParameters regulatoryParameters() { return regulatoryParameters; }
    public ReferentielComptabiliteActivite referentielComptabiliteActivite() { return referentielComptabiliteActivite; }
    public List<Ressource> ressources() { return ressources; }
    public List<Creneau> creneaux() { return creneaux; }
    public List<Indisponibilite> indisponibilites() { return indisponibilites; }
    public List<ReposHebdomadaire> reposHebdomadaires() { return reposHebdomadaires; }
    public SeuilsSurcharge seuilsSurcharge() { return seuilsSurcharge; }
    public PerimetreArbitre perimetreArbitre() { return perimetreArbitre; }
}