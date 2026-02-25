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
import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;

import java.util.Set;
import java.util.TreeSet;
import java.time.LocalDate;
import java.util.List;

public class NuitsConsecutivesMax {

    public static Constraint maxNuitsConsecutives(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels
            .forEach(SalarieReel.class)

            // 2) Jointure STRUCTURELLE avec les créneaux (null-safe)
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // 3) Jointure avec le référentiel d’activité (travail réel)
            .join(factory.forEach(ReferentielComptabiliteActivite.class))

            // 4) Filtre LOGIQUE : uniquement les nuits travaillées
        .filter((salarie, creneau, ref) -> {
            if (creneau.getTypePlageHoraire() != TypePlageHoraire.NUIT) {
                return false;
            }
            ComptabiliteActivite ca = ref.getByCode(creneau.getActivite());
            return ca != null && ca.isCompteDansCharge();
            })

            // 5) Groupement par salarié (liste des créneaux de nuit travaillée)
            .groupBy(
                (salarie, creneau, ref) -> salarie,
                ConstraintCollectors.toList((s, c, ref) -> c)
            
            )
            // 6) Jointure avec le contexte (pour le seuil)
            .join(factory.forEach(PlanningContext.class))

            // 7) Violation HARD
            .filter((salarie, nuits, context) ->
                depasseMaxNuitsConsecutives(
                    nuits,
                    context.getHorizonTemporel(),
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
        HorizonTemporel horizon,
        int maxAutorise
    ) {
        if (nuits.isEmpty()) return false;

        // 1) Dates distinctes, triées, bornées à l’horizon
        Set<LocalDate> datesTriees = new TreeSet<>();
        for (Creneau n : nuits) {
            LocalDate d = n.getDate();
            if (horizon.contient(d)) {
                datesTriees.add(d);
            }
        }

        if (datesTriees.isEmpty()) return false;

        // 2) Calcul de la plus longue séquence consécutive
        int consecutives = 1;
        LocalDate precedente = null;

        for (LocalDate courante : datesTriees) {
            if (precedente != null && courante.equals(precedente.plusDays(1))) {
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
