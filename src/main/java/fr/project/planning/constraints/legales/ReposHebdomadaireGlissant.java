package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.*;
import fr.project.planning.domain.contexte.HorizonTemporel;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReposHebdomadaireGlissant — contrainte HARD (R7, volet conventionnel)
 *
 * <p>Exige un nombre minimal de jours non travaillés dans toute fenêtre glissante de largeur
 * donnée. C'est le volet <strong>paramétrable</strong> du repos hebdomadaire ; le plancher légal
 * — au moins un jour off sur sept — est porté par {@link ReposHebdomadaireMin} et ne se
 * paramètre pas.</p>
 *
 * <h3>Lot S7.4 — remise en service</h3>
 * <p>Comme {@link ReposObligatoireApresNuits}, cette contrainte était éteinte deux fois : champ
 * d'activité déprécié, et seuils globaux jamais alimentés donc nuls, sa garde interne
 * {@code fenetreJours <= 0} la neutralisant par surcroît.</p>
 *
 * <h3>Une paire indissociable</h3>
 * <p>{@code reposHebdomadaireFenetreJours} et {@code reposHebdomadaireJoursOffMinimum} ne
 * décrivent une règle qu'<strong>ensemble</strong> : une fenêtre sans minimum de jours off
 * n'interdit rien, un minimum sans fenêtre ne s'applique nulle part. La contrainte exige donc
 * les deux pour s'activer. Une paire transmise à moitié la laisse inactive, et le mapper
 * l'annonce en WARN — c'est le seul cas du chantier où deux champs se conditionnent.</p>
 *
 * <h3>Périmètre du balayage</h3>
 * <p>Les fenêtres examinées démarrent sur les jours travaillés connus, du premier au dernier.
 * Une fenêtre qui s'achèverait sur le premier jour transmis n'est donc pas évaluée : la règle
 * mesure ce qu'elle reçoit, comme les autres contraintes individuelles. Transmettre une semaine
 * pleine est à la charge de l'appelant.</p>
 */
public class ReposHebdomadaireGlissant {
    private ReposHebdomadaireGlissant() {
        // Utility class
    }

    public static Constraint reposHebdoGlissant(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels dont la PAIRE fenêtre / jours off est complètement renseignée
            .forEach(SalarieReel.class)
            .filter(ReposHebdomadaireGlissant::paireRenseignee)

            // Join créneaux (null-safe)
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // Join context (horizon)
            .join(factory.forEach(PlanningContext.class))

            // Join référentiel (travail réel)
            .join(factory.forEach(ReferentielComptabiliteActivite.class))

            // Filtre : horizon + activité qui compte dans la charge
            .filter((salarie, creneau, context, ref) -> {
                HorizonTemporel h = context.getHorizonTemporel();
                if (!h.contient(creneau.getDate())) return false;

                ComptabiliteActivite ca = ref.getByCode(creneau.getCodeActiviteEffectif());
                return ca != null && ca.isCompteDansCharge();
            })

            // Groupement : salarié + liste de créneaux TRAVAIL
            .groupBy(
                (s, c, context, ref) -> s,
                ConstraintCollectors.toList((s, c, context, ref) -> c)
            )

            // Violation HARD
            .filter((salarie, creneauxTravail) -> violeReposHebdoGlissant(
                    creneauxTravail,
                    fenetreJours(salarie),
                    joursOffMinimum(salarie)))

            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint(PenaliteKey.LEGAL_HARD_REPOS_HEBDOMADAIRE_GLISSANT.name());
        }

    /** Les deux seuils, et pas un seul : voir « Une paire indissociable » ci-dessus. */
    private static boolean paireRenseignee(SalarieReel salarie) {
        ContraintesReglementairesSalarie c = salarie.contraintesOuAucune();
        return ContraintesReglementairesSalarie.largeurRenseignee(c.getReposHebdomadaireFenetreJours())
                && ContraintesReglementairesSalarie.borneRenseignee(c.getReposHebdomadaireJoursOffMinimum());
    }

    /** N'est appelé qu'après {@link #paireRenseignee(SalarieReel)} : la valeur est non nulle. */
    private static int fenetreJours(SalarieReel salarie) {
        return salarie.contraintesOuAucune().getReposHebdomadaireFenetreJours();
    }

    /** N'est appelé qu'après {@link #paireRenseignee(SalarieReel)} : la valeur est non nulle. */
    private static int joursOffMinimum(SalarieReel salarie) {
        return salarie.contraintesOuAucune().getReposHebdomadaireJoursOffMinimum();
    }

    private static boolean violeReposHebdoGlissant(List<Creneau> creneaux, int fenetreJours, int minJoursOff) {
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
