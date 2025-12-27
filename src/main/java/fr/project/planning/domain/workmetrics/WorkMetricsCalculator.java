package fr.project.planning.domain.workmetrics;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.Ressource;
import fr.project.planning.solution.PlanningProblem;

import java.util.HashMap;
import java.util.Map;
import java.time.DayOfWeek;


public class WorkMetricsCalculator {

    public Map<String, WorkMetrics> compute(PlanningProblem solution) {
        Map<String, WorkMetrics> map = new HashMap<>();

        ReferentielComptabiliteActivite ref = solution.getReferentielComptabiliteActivite();

        for (Creneau c : solution.getCreneaux()) {
            Ressource r = c.getRessourceAffectee();
            if (r == null) continue; // ou ressource "NON_AFFECTE" selon votre modèle

            WorkMetrics wm = map.computeIfAbsent(r.getId(), WorkMetrics::new);

            int minutes = c.getDuree();

            // 1) Travail total (V1 : on compte tout créneau affecté)
            wm.addTravail(minutes);

            // 2) Nuit
            if (c.getTypePlageHoraire() == TypePlageHoraire.NUIT) {
                wm.addNuit(minutes);
            }

            // 3) Jour férié
            if (c.isJourFerie() || c.getQualificationJour() == QualificationJour.FERIE) {
                wm.addJourFerie(minutes);
            }

            // 4) Repos hebdo travaillé
            if (c.getQualificationJour() == QualificationJour.REPOS_HEBDOMADAIRE) {
                wm.addReposHebdoTravaille(minutes);

                // 5) Compteur : cas “dette repos sur repos hebdo” (aligné avec votre contrainte)
                ComptabiliteActivite ca = ref.getByCode(c.getActivite());
                if (ca != null && ca.isGenereDetteRepos()) {
                    wm.incReposHebdoDetteRepos();
                }

                // 6) Dimanche travaillé (V2)
                if (c.getDate().getDayOfWeek() == DayOfWeek.SUNDAY) {
                wm.incDimancheTravaille();
                }
            }
        }

        return map;
    }
}
