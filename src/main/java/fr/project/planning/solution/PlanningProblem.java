package fr.project.planning.solution;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.repos.ReposHebdomadaire;
import fr.project.planning.domain.ressource.Indisponibilite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.domain.workmetrics.WorkMetrics;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.solution.ProblemFactProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import fr.project.planning.domain.reglementaire.RegulatoryParameters;

import java.util.List;
import java.util.ArrayList;

/**
 * PlanningProblem
 *
 * État global du problème manipulé par le solveur.
 * Contient les faits, les décisions et le score.
 */
@PlanningSolution
public class PlanningProblem {

    

    /* =========================
       Faits immuables
       ========================= */

    /**
     * Contexte de résolution (intention métier, stratégie de scoring, horizon).
     * Unique et partagé par toutes les contraintes.
     */
    @ProblemFactProperty
    private PlanningContext planningContext;
    
    /**
     * Paramètres réglementaires (durées maximales, repos obligatoires, etc.).
     * Unique et partagé par toutes les contraintes.
     */
    @ProblemFactProperty
    private RegulatoryParameters regulatoryParameters;

    /**
     * Ressources disponibles (salariés réels, postes virtuels, non affecté).
     */
    @ProblemFactCollectionProperty
    private List<Ressource> ressources;

    @ValueRangeProvider(id = "ressourceRange")
    public List<Ressource> getRessourceRange() {
        return ressources;
    }

    /* =========================
       Faits immuables fournis par les tests
       ========================= */
    @ProblemFactCollectionProperty
    public List<WorkMetrics> getWorkMetrics() {
        return (workMetrics != null) ? workMetrics : List.of();
    }

    public void setWorkMetrics(List<WorkMetrics> workMetrics) {
        this.workMetrics = (workMetrics != null) ? workMetrics : new ArrayList<>();
    }

    public RegulatoryParameters getRegulatoryParameters() {
        return regulatoryParameters;
    }

    public void setRegulatoryParameters(RegulatoryParameters regulatoryParameters) {
        this.regulatoryParameters = regulatoryParameters;
    }

    /**
    * Référentiel métier de comptabilisation des activités.
    * Fourni par l'appelant, lu par les contraintes.
    */
    @ProblemFactProperty
    private ReferentielComptabiliteActivite referentielComptabiliteActivite;

    /* =========================
       Entités de décision
       ========================= */

    /**
     * Indisponibilités des salariés.
     * Phase 4 : exploitées par la contrainte IndisponibiliteSalarie (HARD).
     */
    @ProblemFactCollectionProperty
    private List<Indisponibilite> indisponibilites = List.of();

    /**
     * Jours de repos hebdomadaire, un par salarié et par jour concerné (lot S7.9b).
     *
     * <p>Faits et non entités : le repos d'une personne n'est pas un besoin à pourvoir. Le
     * calendrier est construit à la préparation — repos déclarés par l'appelant, complétés
     * semaine par semaine par le repli samedi/dimanche — et le solveur ne peut pas le modifier.</p>
     */
    @ProblemFactCollectionProperty
    private List<ReposHebdomadaire> reposHebdomadaires = List.of();

    /**
     * Créneaux à affecter.
     */
    @PlanningEntityCollectionProperty
    private List<Creneau> creneaux;

    /* =========================
        Faits calculés : WorkMetrics
        ========================= */
    
    private List<WorkMetrics> workMetrics = new ArrayList<>();


    /* =========================
       Score
       ========================= */

    @PlanningScore
    private HardSoftScore score;

    /* =========================
       Constructeurs
       ========================= */

    public PlanningProblem() {
        // requis par OptaPlanner
    }

    public PlanningProblem(
            PlanningContext planningContext,
            RegulatoryParameters regulatoryParameters,
            ReferentielComptabiliteActivite referentielComptabiliteActivite,
            List<Ressource> ressources,
            List<Creneau> creneaux,
            List<Indisponibilite> indisponibilites
    ) {
        this.planningContext = planningContext;
        this.regulatoryParameters = regulatoryParameters;
        this.referentielComptabiliteActivite = referentielComptabiliteActivite;
        this.ressources = ressources;
        this.creneaux = creneaux;
        this.indisponibilites = indisponibilites != null ? indisponibilites : List.of();
    }

    public PlanningProblem(
            PlanningContext planningContext,
            RegulatoryParameters regulatoryParameters,
            ReferentielComptabiliteActivite referentielComptabiliteActivite,
            List<Ressource> ressources,
            List<Creneau> creneaux
    ) {
        this(planningContext, regulatoryParameters, referentielComptabiliteActivite, ressources, creneaux, List.of());
    }

    /* =========================
       Getters / setters
       ========================= */

    public PlanningContext getPlanningContext() {
        return planningContext;
    }

    public ReferentielComptabiliteActivite getReferentielComptabiliteActivite() {
    return referentielComptabiliteActivite;
    }

    public void setReferentielComptabiliteActivite(
        ReferentielComptabiliteActivite referentielComptabiliteActivite
    ) {
    this.referentielComptabiliteActivite = referentielComptabiliteActivite;
    }

    public void setPlanningContext(PlanningContext planningContext) {
        this.planningContext = planningContext;
    }

    public List<Ressource> getRessources() {
        return ressources;
    }

    public void setRessources(List<Ressource> ressources) {
        this.ressources = ressources;
    }

    public List<Creneau> getCreneaux() {
        return creneaux;
    }

    public void setCreneaux(List<Creneau> creneaux) {
        this.creneaux = creneaux;
    }

    public List<Indisponibilite> getIndisponibilites() {
        return indisponibilites;
    }

    public void setIndisponibilites(List<Indisponibilite> indisponibilites) {
        this.indisponibilites = indisponibilites != null ? indisponibilites : List.of();
    }

    public List<ReposHebdomadaire> getReposHebdomadaires() {
        return reposHebdomadaires;
    }

    public void setReposHebdomadaires(List<ReposHebdomadaire> reposHebdomadaires) {
        this.reposHebdomadaires = reposHebdomadaires != null ? reposHebdomadaires : List.of();
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
    
}


