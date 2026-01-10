package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.SalarieReel;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class ReposObligatoireApresNuits {

    public static Constraint reposObligatoireApresNuits(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels
            .forEach(SalarieReel.class)

            // 2) Jointure avec TOUS les créneaux du salarié
            .join(
                factory.forEach(Creneau.class),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // 3) Groupement par salarié
            .groupBy(
                (s, c) -> s,
                ConstraintCollectors.toList((s, c) -> c)
            )

            // 4) Jointure avec le contexte (seuils)
            .join(factory.forEach(PlanningContext.class))

            // 5) Violation HARD
            .filter((salarie, creneaux, context) ->
                violeReposApresNuits(
                    creneaux,
                    context.getSeuilsDeTolerance().getReposApresNuitsEnJours()
                )
            )

            .penalize(
                "Repos obligatoire non respecté après nuits",
                HardSoftScore.ONE_HARD
            );
    }

    /**
     * Vérifie si le repos obligatoire après une séquence de nuits
     * n'est pas respecté.
     */
    private static boolean violeReposApresNuits(
            List<Creneau> creneaux,
            int reposExige
    ) {
        if (creneaux.isEmpty() || reposExige <= 0) return false;

        // Tri par date
        creneaux.sort(Comparator.comparing(Creneau::getDate));

        for (int i = 0; i < creneaux.size(); i++) {

            Creneau courant = creneaux.get(i);

            // On détecte une nuit
            if (courant.getTypePlageHoraire() != TypePlageHoraire.NUIT) {
                continue;
            }

            LocalDate finNuit = courant.getDate();

            // Est-ce la fin d'une séquence de nuits ?
            boolean finSequence =
                (i == creneaux.size() - 1) ||
                !creneaux.get(i + 1).getDate().equals(finNuit.plusDays(1)) ||
                creneaux.get(i + 1).getTypePlageHoraire() != TypePlageHoraire.NUIT;

            if (!finSequence) {
                continue;
            }

            // Fenêtre de repos obligatoire
            LocalDate debutRepos = finNuit.plusDays(1);
            LocalDate finRepos = finNuit.plusDays(reposExige);

            // Recherche d'une reprise pendant la fenêtre
            for (Creneau c : creneaux) {
                LocalDate d = c.getDate();

                if (!d.isBefore(debutRepos) && !d.isAfter(finRepos)) {
                    return true; // violation HARD
                }
            }
        }

        return false;
    }
}
