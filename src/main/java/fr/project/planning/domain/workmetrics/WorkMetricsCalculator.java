package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.solution.PlanningProblem;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Calculateur des WorkMetrics (V2)
 *
 * - Calcul mécanique post-résolution
 * - Aucune règle juridique
 * - Aucune décision OptaPlanner
 */
public class WorkMetricsCalculator {

   public Map<Ressource, WorkMetrics> compute(PlanningProblem solution) {

    ReferentielComptabiliteActivite ref = solution.getReferentielComptabiliteActivite();
    HorizonTemporel horizon = solution.getPlanningContext().getHorizonTemporel();

    // Vérité métier (par id)
    Map<String, WorkMetrics> metricsParRessourceId = new HashMap<>();
    Map<String, Ressource> ressourceParId = new HashMap<>();

    // Comptages “par jour distinct”
    Map<String, Set<String>> rhdParRessourceId = new HashMap<>();
    Map<String, Set<String>> detteReposParRessourceId = new HashMap<>();

    // INITIALISATION : tous les salariés ont des WorkMetrics (même sans créneau)
    for (Ressource r : solution.getRessources()) {
        String id = r.getId();
        metricsParRessourceId.putIfAbsent(id, new WorkMetrics(id));
        ressourceParId.putIfAbsent(id, r);
    }

    for (Creneau c : solution.getCreneaux()) {

        Ressource r = c.getRessourceAffectee();
        if (r == null) {
            continue;
        }

        String ressourceId = r.getId();

        // 0) On “déclare” l’existence du salarié dès qu’il est rencontré
        ressourceParId.putIfAbsent(ressourceId, r);
        metricsParRessourceId.computeIfAbsent(ressourceId, id -> new WorkMetrics(id));

        // 1) Filtre horizon : ignore le créneau pour les compteurs
        if (!horizon.contient(c.getDate())) {
            continue;
        }

        // 2) Filtre activité connue : si inconnue => créneau neutre V2 (aucun compteur)
        // ⚠️ IMPORTANT : vérifiez que c.getActivite() est bien le code attendu par le référentiel
        ComptabiliteActivite ca = ref.getByCode(c.getActivite());
        boolean activiteConnue = (ca != null);
        if (!activiteConnue) {
            continue;
        }

        WorkMetrics wm = metricsParRessourceId.get(ressourceId);
        int minutes = c.getDuree();
        QualificationJour qj = c.getQualificationJour();

        // -----------------------------
        // 1) Travail total
        // -----------------------------
        wm.addTravail(minutes);

        // -----------------------------
        // 2) Nuit
        // -----------------------------
        if (c.getTypePlageHoraire() == TypePlageHoraire.NUIT) {
            wm.addNuit(minutes);
        }

        // -----------------------------
        // 3) Jour férié
        // -----------------------------
        if (qj == QualificationJour.FERIE) {
            wm.addJourFerie(minutes);
        }

        // -----------------------------
        // 4) Repos hebdomadaire travaillé / dette
        // -----------------------------
        if (qj == QualificationJour.RH || qj == QualificationJour.RHD) {

            wm.addReposHebdoTravaille(minutes);

            // Dette = par jour distinct + pilotée par référentiel
            if (ca.isGenereDetteRepos()) {
                detteReposParRessourceId
                        .computeIfAbsent(ressourceId, x -> new HashSet<>())
                        .add(c.getDate().toString());
            }
        }

        // -----------------------------
        // 5) Dimanches travaillés (V2)
        // -----------------------------
        if (qj == QualificationJour.RHD) {
            rhdParRessourceId
                    .computeIfAbsent(ressourceId, x -> new HashSet<>())
                    .add(c.getDate().toString());
        }
    }

    // Finalisation : dettes repos hebdo (par date distincte)
    for (Map.Entry<String, Set<String>> entry : detteReposParRessourceId.entrySet()) {
        WorkMetrics wm = metricsParRessourceId.get(entry.getKey());
        if (wm == null) continue;
        int count = entry.getValue().size();
        for (int i = 0; i < count; i++) {
            wm.incReposHebdoDetteRepos();
        }
    }

    // Finalisation : dimanches travaillés (par date distincte)
    for (Map.Entry<String, Set<String>> entry : rhdParRessourceId.entrySet()) {
        WorkMetrics wm = metricsParRessourceId.get(entry.getKey());
        if (wm == null) continue;
        int count = entry.getValue().size();
        for (int i = 0; i < count; i++) {
            wm.incDimancheTravaille();
        }
    }

    // Projection finale : clé Ressource
    Map<Ressource, WorkMetrics> result = new HashMap<>();
    for (Map.Entry<String, WorkMetrics> entry : metricsParRessourceId.entrySet()) {
        Ressource r = ressourceParId.get(entry.getKey());
        if (r != null) {
            result.put(r, entry.getValue());
        }
    }

    return result;
}

}


