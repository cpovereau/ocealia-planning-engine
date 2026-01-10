package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ReposHebdomadaireGlissant {

    public static Constraint reposHebdoGlissant(ConstraintFactory factory) {

        return factory
            .forEach(SalarieReel.class)
            .join(
                factory.forEach(Creneau.class),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )
            // groupBy salarié -> liste de tous ses créneaux
            .groupBy(
                (s, c) -> s,
                ConstraintCollectors.toList((s, c) -> c)
            )
            .join(factory.forEach(PlanningContext.class))
            .filter((salarie, creneaux, context) -> {
                int fenetre = context.getSeuilsDeTolerance().getReposHebdoFenetreJours();          // ex 7
                int minOff  = context.getSeuilsDeTolerance().getReposHebdoMinJoursOffDansFenetre(); // ex 1
                return violeReposHebdoGlissant(creneaux, fenetre, minOff);
            })
            .penalize("Repos hebdomadaire glissant non respecté", HardSoftScore.ONE_HARD);
    }

    private static boolean violeReposHebdoGlissant(List<Creneau> creneaux, int fenetreJours, int minJoursOff) {
        if (fenetreJours <= 0) return false;

        // jours travaillés = jours ayant au moins un créneau
        Set<LocalDate> joursTravailles = creneaux.stream()
            .map(Creneau::getDate)
            .collect(Collectors.toSet());

        if (joursTravailles.isEmpty()) return false;

        LocalDate min = Collections.min(joursTravailles);
        LocalDate max = Collections.max(joursTravailles);

        // on scanne toutes les fenêtres [d ; d+fenetreJours-1]
        for (LocalDate d = min; !d.isAfter(max); d = d.plusDays(1)) {
            int worked = 0;
            for (int i = 0; i < fenetreJours; i++) {
                if (joursTravailles.contains(d.plusDays(i))) worked++;
            }
            int off = fenetreJours - worked;
            if (off < minJoursOff) {
                return true; // violation
            }
        }
        return false;
    }
}
