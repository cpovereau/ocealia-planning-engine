package fr.project.planning.solution;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
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

import java.util.List;

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
     * Ressources disponibles (salariés réels, postes virtuels, non affecté).
     */
    @ProblemFactCollectionProperty
    private List<Ressource> ressources;

    @ValueRangeProvider(id = "ressourceRange")
    public List<Ressource> getRessourceRange() {
        return ressources;
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
     * Créneaux à affecter.
     */
    
    @PlanningEntityCollectionProperty
    private List<Creneau> creneaux;

    /* =========================
        Faits calculés : WorkMetrics
        ========================= */
    
    @ProblemFactCollectionProperty
    private List<WorkMetrics> workMetrics;


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
            ReferentielComptabiliteActivite referentielComptabiliteActivite,
            List<Ressource> ressources,
            List<Creneau> creneaux
    ) {
        this.planningContext = planningContext;
        this.referentielComptabiliteActivite = referentielComptabiliteActivite;
        this.ressources = ressources;
        this.creneaux = creneaux;
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

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
