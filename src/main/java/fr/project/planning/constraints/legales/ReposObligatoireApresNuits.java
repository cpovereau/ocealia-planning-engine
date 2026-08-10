package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.TypePlageHoraire;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;
import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;

import java.time.LocalDate;
import java.util.List;

/**
 * ReposObligatoireApresNuits — contrainte HARD (R4)
 *
 * <p>Impose un nombre minimal de jours calendaires sans travail après une <strong>séquence</strong>
 * de nuits. Une séquence se termine dès que la nuit suivante n'est pas au lendemain ; la fenêtre
 * de repos court alors de J+1 à J+n, et tout créneau de travail qui y tombe est une violation.</p>
 *
 * <h3>Lot S7.3 — remise en service</h3>
 * <p>Cette contrainte était éteinte <strong>deux fois</strong> : elle lisait l'activité via le
 * champ déprécié {@code getActivite()}, et son seuil venait de
 * {@code SeuilsDeTolerance.reposApresNuitsEnJours}, jamais alimenté donc nul — sa garde interne
 * {@code reposExige <= 0} la neutralisait par surcroît. Corriger le seul repli d'activité ne
 * l'aurait donc pas réveillée.</p>
 *
 * <p>L'activité est désormais lue via {@link Creneau#getCodeActiviteEffectif()} et le repos exigé
 * sur le salarié — {@code contraintesReglementaires.joursReposMinimumApresNuits}.</p>
 *
 * <h3>Activation</h3>
 * <p>Inactive si le repos exigé est absent ou nul, cf.
 * {@link ContraintesReglementairesSalarie#seuilActif(Number)}. Le filtre est appliqué en tête de
 * flux, ce qui rend inutile la garde interne d'origine.</p>
 *
 * <h3>À ne pas confondre avec le repos quotidien</h3>
 * <p>{@link ReposQuotidienMinimum} mesure des <em>heures</em> entre deux journées travaillées
 * successives. R4 exige des <em>journées entières</em> après une séquence de nuits : c'est une
 * récupération, pas une coupure. Les deux peuvent être satisfaites ou violées indépendamment.</p>
 */
public class ReposObligatoireApresNuits {
    private ReposObligatoireApresNuits() {
        // Utility class
    }

    public static Constraint reposObligatoireApresNuits(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels dont le repos après nuits est renseigné
            .forEach(SalarieReel.class)
            .filter(ReposObligatoireApresNuits::reposRenseigne)

            // 2) Jointure avec TOUS les créneaux du salarié
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // 3) Jointure avec le contexte (horizon)
            .join(factory.forEach(PlanningContext.class))

            // 4) Jointure référentiel d’activité (pour ne conserver que les créneaux de travail réel)
            .join(factory.forEach(ReferentielComptabiliteActivite.class))

            // 5) On ne conserve que les créneaux "travaillés" au sens canonique + bornés à l’horizon
            .filter((salarie, creneau, context, ref) -> {
                HorizonTemporel h = context.getHorizonTemporel();
                if (!h.contient(creneau.getDate())) return false;

                ComptabiliteActivite ca = ref.getByCode(creneau.getCodeActiviteEffectif());
                return ca != null && ca.isCompteDansCharge();
            })

            // 6) Groupement par salarié
            .groupBy(
                (s, c, context, ref) -> s,
                ConstraintCollectors.toList((s, c, context, ref) -> c)
            )

            // 7) Violation HARD
            .filter((salarie, creneauxTravail) ->
                violeReposApresNuits(creneauxTravail, reposExige(salarie))
            )

            .penalize(
                HardSoftScore.ONE_HARD
            )
            .asConstraint(PenaliteKey.LEGAL_HARD_REPOS_OBLIGATOIRE_APRES_NUITS.name());
    }

    private static boolean reposRenseigne(SalarieReel salarie) {
        return ContraintesReglementairesSalarie.seuilActif(
                salarie.contraintesOuAucune().getJoursReposMinimumApresNuits());
    }

    /** N'est appelé qu'après {@link #reposRenseigne(SalarieReel)} : la valeur est non nulle. */
    private static int reposExige(SalarieReel salarie) {
        return salarie.contraintesOuAucune().getJoursReposMinimumApresNuits();
    }

    /**
     * Vérifie si le repos obligatoire après une séquence de nuits n'est pas respecté.
     *
     * <p>La liste reçue n'est jamais triée sur place : elle provient d'un
     * {@code ConstraintCollectors.toList} dont OptaPlanner reste propriétaire, et la muter
     * reviendrait à modifier l'état interne du calcul de score. L'ordre nécessaire est obtenu
     * par un tri local sur les seules dates de nuit.</p>
     */
    private static boolean violeReposApresNuits(
        List<Creneau> creneauxTravail,
        int reposExige
    ) {
        if (creneauxTravail.isEmpty()) return false;

        // Dates de nuits travaillées, dédupliquées et triées
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
