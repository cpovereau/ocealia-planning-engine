package com.example.planning.solver;

import com.example.planning.domain.Creneau;
import com.example.planning.domain.Ressource;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.domain.solution.PlanningScore;

import java.util.List;

@org.optaplanner.core.api.domain.solution.PlanningSolution
public class PlanningSolution {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "ressources")
    private List<Ressource> ressources;

    @PlanningEntityCollectionProperty
    private List<Creneau> creneaux;

    @PlanningScore
    private HardSoftScore score;

    public PlanningSolution() {
    }

    public PlanningSolution(List<Ressource> ressources, List<Creneau> creneaux) {
        this.ressources = ressources;
        this.creneaux = creneaux;
    }

    public List<Ressource> getRessources() {
        return ressources;
    }

    public List<Creneau> getCreneaux() {
        return creneaux;
    }

    public HardSoftScore getScore() {
        return score;
    }
}
