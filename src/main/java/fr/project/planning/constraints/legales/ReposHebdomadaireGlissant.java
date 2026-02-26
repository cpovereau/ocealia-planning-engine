package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.*;
import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ReposHebdomadaireGlissant {
    private ReposHebdomadaireGlissant() {
        // Utility class
    }

    public static Constraint reposHebdoGlissant(ConstraintFactory factory) {

        return factory
            .forEach(SalarieReel.class)

            // Join créneaux (null-safe)
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // Join context (horizon + seuils)
            .join(factory.forEach(PlanningContext.class))

            // Join référentiel (travail réel)
            .join(factory.forEach(ReferentielComptabiliteActivite.class))

            // Filtre : horizon + activité qui compte dans la charge
            .filter((salarie, creneau, context, ref) -> {
                HorizonTemporel h = context.getHorizonTemporel();
                if (!h.contient(creneau.getDate())) return false;

                ComptabiliteActivite ca = ref.getByCode(creneau.getActivite());
                return ca != null && ca.isCompteDansCharge();
            })

            // Groupement : salarié + context + liste de créneaux TRAVAIL
            .groupBy(
                (s, c, context, ref) -> s,
                (s, c, context, ref) -> context,
                ConstraintCollectors.toList((s, c, context, ref) -> c)
            )

            // Violation HARD
            .filter((salarie, context, creneauxTravail) -> {
                int fenetre = context.getSeuilsDeTolerance().getReposHebdoFenetreJours();
                int minOff  = context.getSeuilsDeTolerance().getReposHebdoMinJoursOffDansFenetre();
                return violeReposHebdoGlissant(creneauxTravail, fenetre, minOff);
            })

            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("Repos hebdomadaire insuffisant");
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
