package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.solution.PlanningProblem;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    // Diagnostics (mode dev - A changer lors du passage en prod)
    int nbCreneauxSansRessource = 0;
    int nbCreneauxHorsHorizon = 0;
    int nbCreneauxActiviteInconnue = 0;

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
            nbCreneauxSansRessource++; // contournement temporaire pour diagnostic (mode dev)
            continue;
        }

        String ressourceId = r.getId();

        // 0) On “déclare” l’existence du salarié dès qu’il est rencontré
        ressourceParId.putIfAbsent(ressourceId, r);
        metricsParRessourceId.computeIfAbsent(ressourceId, id -> new WorkMetrics(id));

        // 1) Filtre horizon : ignore le créneau pour les compteurs
        if (!horizon.contient(c.getDate())) {
            nbCreneauxHorsHorizon++; // contournement temporaire pour diagnostic (mode dev)
            continue;
        }

        // 2) Filtre activité connue : si inconnue => créneau neutre V2 (aucun compteur)
        // Priorité à l'id (stable) ; fallback sur le libellé/code historique
        String codeActivite = (c.getCodeActiviteId() != null && !c.getCodeActiviteId().isBlank())
            ? c.getCodeActiviteId()
            : c.getActivite();

        ComptabiliteActivite ca = (ref != null) ? ref.getByCode(codeActivite) : null;
        if (ca == null) {
            nbCreneauxActiviteInconnue++;
            continue; // créneau neutre si référentiel absent/incomplet ou activité non mappée
        }

        WorkMetrics wm = metricsParRessourceId.get(ressourceId);
        int minutes = c.getDuree();
        QualificationJour qj = c.getQualificationJour();

        // -----------------------------
        // 1) Travail total
        // -----------------------------
        if (ca.isCompteDansCharge()) {
            wm.addTravail(minutes);
        }

        // -----------------------------
        // 2) Nuit
        // -----------------------------
        if (c.getTypePlageHoraire() == TypePlageHoraire.NUIT && ca.isCompteDansCharge()) {
            wm.addNuit(minutes);
        }

        // -----------------------------
        // 3) Jour férié
        // -----------------------------
        if (qj == QualificationJour.FERIE && ca.isCompteDansCharge()) {
            wm.addJourFerie(minutes);
        }

        // -----------------------------
        // 4) Repos hebdomadaire travaillé / dette
        // -----------------------------
        DayOfWeek dow = c.getDate().getDayOfWeek();
        boolean estWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);

        if (estWeekend && ca.isCompteDansCharge()) {
            wm.addReposHebdoTravaille(minutes);

        // Dette = par jour distinct + pilotée par référentiel
        if (ca.isGenereDetteRepos()) {
            detteReposParRessourceId
                .computeIfAbsent(ressourceId, x -> new HashSet<>())
                .add(c.getDate().toString());
        }
        }

        // -----------------------------
        // 5) Dimanches travaillés (V2 corrigée)
        // Dimanche travaillé = un créneau un dimanche calendaire dont l’activité compte dans la charge
        // -----------------------------
        if (c.getDate().getDayOfWeek() == DayOfWeek.SUNDAY && ca.isCompteDansCharge()) {
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

    // Diagnostics (mode dev - A changer lors du passage en prod)    
    if (nbCreneauxSansRessource > 0 || nbCreneauxHorsHorizon > 0 || nbCreneauxActiviteInconnue > 0) {
        System.out.println("[WorkMetricsCalculator] Diagnostics (dev/Option B) : "
            + "sansRessource=" + nbCreneauxSansRessource
            + ", horsHorizon=" + nbCreneauxHorsHorizon
            + ", activiteInconnueOuReferentielManquant=" + nbCreneauxActiviteInconnue);
    }

    return result;
}

}


