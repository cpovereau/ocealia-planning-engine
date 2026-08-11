package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * JoursConsecutifsMax — contrainte SOFT (R1)
 *
 * Pénalise le dépassement du nombre maximal de jours consécutifs travaillés
 * pour un salarié, tel que défini dans ses contraintes réglementaires individuelles.
 *
 * Définition d'un "jour travaillé" : date comportant au moins un créneau
 * avec compteDansCharge = true (cohérent avec WorkMetrics.maxJoursConsecutifsObservees).
 *
 * Seuil : ContraintesReglementairesSalarie.joursConsecutifsMaximum (par salarié).
 * Pénalité : Penalites.depassementMaxJoursConsecutifs × jours de dépassement.
 */
public class JoursConsecutifsMax {

    private JoursConsecutifsMax() {
        // Utility class
    }

    public static Constraint maxJoursConsecutifs(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels avec un seuil de jours consécutifs configuré
            .forEach(SalarieReel.class)
            .filter(s -> s.getContraintesReglementaires() != null
                    && s.getContraintesReglementaires().getJoursConsecutifsMaximum() != null)

            // 2) Jointure avec les créneaux affectés à ce salarié, pauses exclues
            // [Phase 8] estSegmentDePause = true → exclu du comptage des jours travaillés (segment non productif)
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null)
                    .filter(c -> !Boolean.TRUE.equals(c.getEstSegmentDePause())),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // 3) Jointure avec le référentiel d'activité
            .join(factory.forEach(ReferentielComptabiliteActivite.class))

            // 4) Filtre : uniquement les jours travaillés (compteDansCharge = true)
            .filter((salarie, creneau, ref) -> {
                ComptabiliteActivite ca = ref.getByCode(creneau.getCodeActiviteEffectif());
                return ca != null && ca.isCompteDansCharge();
            })

            // 5) Groupement par salarié
            .groupBy(
                (salarie, creneau, ref) -> salarie,
                ConstraintCollectors.toList((s, c, ref) -> c)
            )

            // 6) Jointure avec le contexte (horizon + valeur de pénalité)
            .join(factory.forEach(PlanningContext.class))

            // 7) Pénalité SOFT proportionnelle au dépassement
            .penalize(
                HardSoftScore.ONE_SOFT,
                (salarie, creneaux, context) -> {
                    int maxAutorise = salarie.getContraintesReglementaires().getJoursConsecutifsMaximum();
                    int plusLongue = plusLongueSequenceConsecutive(creneaux, context.getHorizonTemporel());
                    int excedent = Math.max(0, plusLongue - maxAutorise);
                    if (excedent == 0) return 0;
                    return context.getPenalites().getDepassementMaxJoursConsecutifs() * excedent;
                }
            )
            .asConstraint(PenaliteKey.LEGAL_SOFT_JOURS_CONSECUTIFS_MAX.name());
    }

    /**
     * Calcule la longueur de la plus longue suite de dates consécutives
     * parmi les créneaux fournis, bornée à l'horizon de résolution.
     *
     * Plusieurs créneaux le même jour comptent pour un seul jour travaillé.
     */
    static int plusLongueSequenceConsecutive(List<Creneau> creneaux, HorizonTemporel horizon) {
        if (creneaux.isEmpty()) return 0;

        Set<LocalDate> datesTriees = new TreeSet<>();
        for (Creneau c : creneaux) {
            LocalDate d = c.getDate();
            if (horizon.contient(d)) {
                datesTriees.add(d);
            }
        }

        if (datesTriees.isEmpty()) return 0;

        int maxConsecutives = 1;
        int courantes = 1;
        LocalDate precedente = null;

        for (LocalDate d : datesTriees) {
            if (precedente != null && d.equals(precedente.plusDays(1))) {
                courantes++;
                if (courantes > maxConsecutives) {
                    maxConsecutives = courantes;
                }
            } else {
                courantes = 1;
            }
            precedente = d;
        }

        return maxConsecutives;
    }
}
