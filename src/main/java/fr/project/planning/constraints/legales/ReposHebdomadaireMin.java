package fr.project.planning.constraints.legales;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.contexte.HorizonTemporel;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReposHebdomadaireMin (HARD)
 *
 * Socle légal : éviter une semaine à 7/7.
 * Implémentation volontairement simple :
 * - Sur toute fenêtre glissante de 7 jours, il faut au moins 1 jour OFF
 * - "Jour travaillé" = au moins un créneau dont l’activité "compte dans la charge" (référentiel)
 *
 * Remarque :
 * - On ne se base PAS sur RH/RHD (qui sont des codes REPOS attendus, pas du travail),
 * - On se base sur la notion "compte dans la charge".
 */
public class ReposHebdomadaireMin {
    private ReposHebdomadaireMin() {
        // Utility class
    }

    private static final int FENETRE_JOURS = 7;
    private static final int MIN_JOURS_OFF = 1;

    public static Constraint reposHebdomadaireMin(ConstraintFactory factory) {

        return factory
            .forEach(SalarieReel.class)

            // Tous les créneaux affectés au salarié
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null),
                Joiners.equal(SalarieReel::getId, c -> c.getRessourceAffectee().getId())
            )

            // On groupe par salarié -> liste de créneaux
            .groupBy(
                (s, c) -> s,
                ConstraintCollectors.toList((s, c) -> c)
            )

            // On joint le contexte + référentiel
            .join(factory.forEach(PlanningContext.class))
            .join(factory.forEach(ReferentielComptabiliteActivite.class))

            // Violation HARD
            .filter((salarie, creneaux, context, ref) ->
                violeReposHebdoMin(creneaux, context.getHorizonTemporel(), ref)
            )

            .penalize(
                HardSoftScore.ONE_HARD
            )
            .asConstraint("Repos hebdomadaire insuffisant");
    }

    private static boolean violeReposHebdoMin(
        List<Creneau> creneaux,
        HorizonTemporel horizon,
        ReferentielComptabiliteActivite ref
    ) {
        if (creneaux == null || creneaux.isEmpty()) return false;

        // Jours "travaillés" au sens "compte dans la charge", et dans l'horizon
        Set<LocalDate> joursTravailles = creneaux.stream()
            .filter(c -> c.getDate() != null && horizon.contient(c.getDate()))
            .filter(c -> {
                ComptabiliteActivite ca = ref.getByCode(c.getActivite());
                return ca != null && ca.isCompteDansCharge();
            })
            .map(Creneau::getDate)
            .collect(Collectors.toSet());

        if (joursTravailles.isEmpty()) return false;

        LocalDate min = Collections.min(joursTravailles);
        LocalDate max = Collections.max(joursTravailles);

        // Fenêtre glissante 7 jours : off = 7 - worked
        for (LocalDate d = min; !d.isAfter(max); d = d.plusDays(1)) {
            int worked = 0;
            for (int i = 0; i < FENETRE_JOURS; i++) {
                if (joursTravailles.contains(d.plusDays(i))) {
                    worked++;
                }
            }
            int off = FENETRE_JOURS - worked;
            if (off < MIN_JOURS_OFF) {
                return true;
            }
        }
        return false;
    }
}