package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.SalarieReel;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.Joiners;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class NuitsConsecutivesMax {

    public static Constraint maxNuitsConsecutives(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels
            .forEach(SalarieReel.class)

            // 2) Jointure STRUCTURELLE avec les créneaux
            .join(
                factory.forEach(Creneau.class),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // 3) Filtre LOGIQUE : uniquement les nuits
            .filter((salarie, creneau) ->
                creneau.getTypePlageHoraire() == TypePlageHoraire.NUIT
            )

            // 4) Groupement par salarié
            .groupBy(
                (salarie, creneau) -> salarie,
                ConstraintCollectors.toList((s, c) -> c)
            )

            // 5) Jointure avec le contexte (pour le seuil)
            .join(factory.forEach(PlanningContext.class))

            // 6) Violation HARD
            .filter((salarie, nuits, context) ->
                depasseMaxNuitsConsecutives(
                    nuits,
                    context.getSeuilsDeTolerance().getMaxNuitsConsecutives()
                )
            )

            .penalize(
                "Dépassement du nombre maximal de nuits consécutives",
                HardSoftScore.ONE_HARD
            );
    }

    private static boolean depasseMaxNuitsConsecutives(
            List<Creneau> nuits,
            int maxAutorise
    ) {
        if (nuits.isEmpty()) return false;

        nuits.sort(Comparator.comparing(Creneau::getDate));

        int consecutives = 1;
        LocalDate precedente = nuits.get(0).getDate();

        for (int i = 1; i < nuits.size(); i++) {
            LocalDate courante = nuits.get(i).getDate();

            if (courante.equals(precedente.plusDays(1))) {
                consecutives++;
                if (consecutives > maxAutorise) {
                    return true;
                }
            } else {
                consecutives = 1;
            }

            precedente = courante;
        }

        return false;
    }
}
