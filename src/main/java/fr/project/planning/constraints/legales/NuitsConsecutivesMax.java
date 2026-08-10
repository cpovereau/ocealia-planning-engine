package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;

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

/**
 * NuitsConsecutivesMax — contrainte HARD (R3)
 *
 * <p>Interdit qu'un salarié enchaîne plus de nuits travaillées que son contrat ne l'autorise.
 * Le comptage porte sur des <strong>dates calendaires distinctes et consécutives</strong> : deux
 * créneaux de nuit la même date ne comptent que pour une nuit.</p>
 *
 * <h3>Lot S7.2 — remise en service</h3>
 * <p>Cette contrainte était <strong>dormante</strong>, et pour deux raisons cumulées :</p>
 * <ul>
 *   <li>elle lisait l'activité via {@code getActivite()}, le champ déprécié, que les clients
 *       conformes au contrat n'envoient plus — aucun match n'était produit ;</li>
 *   <li>son seuil venait de {@code SeuilsDeTolerance.maxNuitsConsecutives}, jamais alimenté,
 *       donc nul, et <strong>sans garde</strong> : réparer le seul repli d'activité aurait rendu
 *       toute deuxième nuit consécutive immédiatement illégale.</li>
 * </ul>
 *
 * <p>L'activité est désormais lue via {@link Creneau#getCodeActiviteEffectif()} et le plafond sur
 * le salarié — {@code contraintesReglementaires.nuitsConsecutivesMaximum}.</p>
 *
 * <h3>Activation</h3>
 * <p>Inactive si le plafond est absent ou nul, cf.
 * {@link ContraintesReglementairesSalarie#seuilActif(Number)}. Le filtre est appliqué en tête de
 * flux : un salarié sans plafond de nuits consécutives ne produit aucun tuple.</p>
 *
 * <h3>Périmètre</h3>
 * <p>Seules les nuits comprises dans l'horizon de résolution sont comptées. Une séquence entamée
 * avant l'horizon n'est donc vue qu'à partir de son entrée dans la fenêtre : la règle mesure ce
 * qu'elle reçoit, comme les autres contraintes individuelles.</p>
 */
public class NuitsConsecutivesMax {
    private NuitsConsecutivesMax() {
    // Utility class
    }

    public static Constraint maxNuitsConsecutives(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels dont le plafond de nuits consécutives est renseigné
            .forEach(SalarieReel.class)
            .filter(NuitsConsecutivesMax::plafondRenseigne)

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
            ComptabiliteActivite ca = ref.getByCode(creneau.getCodeActiviteEffectif());
            return ca != null && ca.isCompteDansCharge();
            })

            // 5) Groupement par salarié (liste des créneaux de nuit travaillée)
            .groupBy(
                (salarie, creneau, ref) -> salarie,
                ConstraintCollectors.toList((s, c, ref) -> c)

            )
            // 6) Jointure avec le contexte (pour l'horizon)
            .join(factory.forEach(PlanningContext.class))

            // 7) Violation HARD
            .filter((salarie, nuits, context) ->
                depasseMaxNuitsConsecutives(
                    nuits,
                    context.getHorizonTemporel(),
                    plafond(salarie)
                )
        )

            .penalize(
                HardSoftScore.ONE_HARD
            )
            .asConstraint(PenaliteKey.LEGAL_HARD_NUITS_CONSECUTIVES_MAX.name());
    }

    private static boolean plafondRenseigne(SalarieReel salarie) {
        return ContraintesReglementairesSalarie.seuilActif(
                salarie.contraintesOuAucune().getNuitsConsecutivesMaximum());
    }

    /** N'est appelé qu'après {@link #plafondRenseigne(SalarieReel)} : la valeur est non nulle. */
    private static int plafond(SalarieReel salarie) {
        return salarie.contraintesOuAucune().getNuitsConsecutivesMaximum();
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
