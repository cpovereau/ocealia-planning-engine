package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
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
import java.util.Map;
import java.util.TreeMap;

/**
 * AlternanceJourNuit — contrainte SOFT (R5 + R6)
 *
 * Pénalise les alternances de rythme entre travail de jour et travail de nuit
 * sur des jours calendaires consécutifs.
 *
 * Définition d'une alternance :
 *   Deux jours consécutifs J et J+1, tous deux travaillés (compteDansCharge = true),
 *   où le type de plage change (JOUR → NUIT ou NUIT → JOUR).
 *
 * Type d'un jour :
 *   NUIT si le jour comporte au moins un créneau avec TypePlageHoraire.NUIT et compteDansCharge = true.
 *   JOUR sinon.
 *
 * Pénalité : Penalites.penaliteAlternanceJourNuit × nombre d'alternances détectées.
 *
 * Note v1 : pondération croissante (1 → 3 → 5) documentée dans R5 différée à une révision ultérieure.
 * Le schéma proportionnel retenu est suffisant pour guider le solveur vers des rythmes stables.
 */
public class AlternanceJourNuit {

    private AlternanceJourNuit() {
        // Utility class
    }

    public static Constraint alternanceJourNuit(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels
            .forEach(SalarieReel.class)

            // 2) Jointure avec les créneaux affectés à ce salarié
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // 3) Jointure avec le référentiel d'activité
            .join(factory.forEach(ReferentielComptabiliteActivite.class))

            // 4) Filtre : uniquement les créneaux travaillés (compteDansCharge = true)
            // [Phase 10A] Fallback codeActiviteId → activite (cohérence avec le reste du moteur)
            .filter((salarie, creneau, ref) -> {
                String codeActivite = (creneau.getCodeActiviteId() != null && !creneau.getCodeActiviteId().isBlank())
                        ? creneau.getCodeActiviteId() : creneau.getActivite();
                ComptabiliteActivite ca = ref.getByCode(codeActivite);
                return ca != null && ca.isCompteDansCharge();
            })

            // 5) Groupement par salarié
            .groupBy(
                (salarie, creneau, ref) -> salarie,
                ConstraintCollectors.toList((s, c, ref) -> c)
            )

            // 6) Jointure avec le contexte (horizon + valeur de pénalité)
            .join(factory.forEach(PlanningContext.class))

            // 7) Pénalité SOFT proportionnelle au nombre d'alternances
            .penalize(
                HardSoftScore.ONE_SOFT,
                (salarie, creneaux, context) -> {
                    int nb = compterAlternances(creneaux, context.getHorizonTemporel());
                    if (nb == 0) return 0;
                    return context.getPenalites().getPenaliteAlternanceJourNuit() * nb;
                }
            )
            .asConstraint(PenaliteKey.LEGAL_SOFT_ALTERNANCE_JOUR_NUIT.name());
    }

    /**
     * Compte le nombre d'alternances JOUR/NUIT sur jours consécutifs.
     *
     * Pour chaque date travaillée dans l'horizon :
     *   - type = NUIT si au moins un créneau TypePlageHoraire.NUIT présent sur cette date
     *   - type = JOUR sinon
     *
     * Une alternance = paire (J, J+1) consécutive avec changement de type.
     *
     * Appel depuis les tests en accès package.
     */
    static int compterAlternances(List<Creneau> creneaux, HorizonTemporel horizon) {
        if (creneaux.size() < 2) return 0;

        // date → true si NUIT (NUIT gagne sur JOUR pour la même date)
        Map<LocalDate, Boolean> dateEstNuit = new TreeMap<>();
        for (Creneau c : creneaux) {
            LocalDate d = c.getDate();
            if (!horizon.contient(d)) continue;
            if (c.getTypePlageHoraire() == TypePlageHoraire.NUIT) {
                dateEstNuit.put(d, true);
            } else {
                dateEstNuit.putIfAbsent(d, false);
            }
        }

        if (dateEstNuit.size() < 2) return 0;

        int alternances = 0;
        LocalDate precedente = null;
        boolean estNuitPrecedente = false; // valeur non significative tant que precedente == null

        for (Map.Entry<LocalDate, Boolean> entry : dateEstNuit.entrySet()) {
            LocalDate courante = entry.getKey();
            boolean estNuitCourante = entry.getValue();

            if (precedente != null
                    && courante.equals(precedente.plusDays(1))
                    && estNuitPrecedente != estNuitCourante) {
                alternances++;
            }

            precedente = courante;
            estNuitPrecedente = estNuitCourante;
        }

        return alternances;
    }
}
