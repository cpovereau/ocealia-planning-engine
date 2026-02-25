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
import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ReposObligatoireApresNuits {

    public static Constraint reposObligatoireApresNuits(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels
            .forEach(SalarieReel.class)

            // 2) Jointure avec TOUS les créneaux du salarié
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // 3) Jointure avec le contexte (seuils)
            .join(factory.forEach(PlanningContext.class))

            // 4) Jointure référentiel d’activité (pour ne conserver que les créneaux de travail réel)
            .join(factory.forEach(ReferentielComptabiliteActivite.class))

            // 5) On ne conserve que les créneaux "travaillés" au sens canonique + bornés à l’horizon
            .filter((salarie, creneau, context, ref) -> {
                HorizonTemporel h = context.getHorizonTemporel();
                if (!h.contient(creneau.getDate())) return false;

                ComptabiliteActivite ca = ref.getByCode(creneau.getActivite());
                return ca != null && ca.isCompteDansCharge();
            })

            // 6) Groupement par salarié
            .groupBy(
                (s, c, context, ref) -> s,
                (s, c, context, ref) -> context,
                ConstraintCollectors.toList((s, c, context, ref) -> c)
            )

            // 7) Violation HARD
            .filter((salarie, context, creneauxTravail) ->
                violeReposApresNuits(
                    creneauxTravail,
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
        List<Creneau> creneauxTravail,
        int reposExige
    ) {
        if (creneauxTravail.isEmpty() || reposExige <= 0) return false;

        // Tri par date
        creneauxTravail.sort(Comparator.comparing(Creneau::getDate));

        // On ne garde que les dates de nuits travaillées (dédupliquées)
        List<LocalDate> datesNuits = creneauxTravail.stream()
            .filter(c -> c.getTypePlageHoraire() == TypePlageHoraire.NUIT)
            .map(Creneau::getDate)
            .distinct()
            .sorted()
            .toList();

        if (datesNuits.isEmpty()) return false;

        for (int i = 0; i < datesNuits.size(); i++) {

            LocalDate finNuit = datesNuits.get(i);

            // Fin de séquence si la prochaine nuit n'est pas J+1
            boolean finSequence =
                (i == datesNuits.size() - 1) ||
                !datesNuits.get(i + 1).equals(finNuit.plusDays(1));

            if (!finSequence) continue;

            // Fenêtre de repos obligatoire (jours calendaires)
            LocalDate debutRepos = finNuit.plusDays(1);
            LocalDate finRepos = finNuit.plusDays(reposExige);

            // Violation si reprise de travail (donc un créneau TRAVAIL) dans la fenêtre
            for (Creneau c : creneauxTravail) {
                LocalDate d = c.getDate();
                if (!d.isBefore(debutRepos) && !d.isAfter(finRepos)) {
                    return true;
                }
            }
        }

        return false;
    }
}
