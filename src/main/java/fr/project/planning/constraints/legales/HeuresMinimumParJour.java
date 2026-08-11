package fr.project.planning.constraints.legales;

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

/**
 * HeuresMinimumParJour — contrainte SOFT (Phase 8)
 *
 * <p>Pénalise les journées où le temps de travail effectif d'un salarié
 * est inférieur à son minimum contractuel journalier configuré.</p>
 *
 * <h3>Définition retenue du "temps de travail effectif" (hypothèse Phase 8)</h3>
 * <p>Le calcul porte exclusivement sur les créneaux vérifiant <strong>les deux conditions suivantes</strong> :</p>
 * <ol>
 *   <li>{@code estSegmentDePause != true} — les pauses sont exclues du décompte ;</li>
 *   <li>activité avec {@code compteDansCharge = true} dans le référentiel — seuls les créneaux
 *       qui comptent dans la charge de travail sont pris en compte.</li>
 * </ol>
 * <p>Conséquences de cette hypothèse :</p>
 * <ul>
 *   <li>Un créneau de formation ({@code compteDansCharge = false}) n'entre pas dans le total,
 *       même s'il représente du temps passé au travail. Si le métier souhaite l'inclure,
 *       cette hypothèse devra être révisée.</li>
 *   <li>La contrainte ne s'active que pour les journées où le salarié a au moins un créneau
 *       de travail affecté (non-pause, compteDansCharge = true). Un jour sans affectation
 *       de travail réel ne produit aucune pénalité.</li>
 * </ul>
 *
 * <h3>Activation</h3>
 * <p>Inactive si {@code ContraintesReglementairesSalarie.heuresMinimumParJour == null}.</p>
 *
 * <h3>Pénalité</h3>
 * <p>{@code PENALITE_HEURES_MIN_PAR_JOUR} × minutes de déficit, par journée en déficit.
 * Coefficient aligné sur {@code penaliteAmplitude} (50) — même ordre de grandeur,
 * même unité (minutes de déviation par rapport à une borne journalière contractuelle).
 * À externaliser vers {@code Penalites} si un paramétrage indépendant est nécessaire.</p>
 */
public class HeuresMinimumParJour {

    private HeuresMinimumParJour() {
        // Utility class
    }

    /**
     * Pénalité par minute de déficit.
     * Alignée sur penaliteAmplitude (50) pour cohérence des ordres de grandeur.
     * À externaliser vers Penalites.penaliteHeuresMinParJour si besoin de paramétrage.
     */
    private static final int PENALITE_HEURES_MIN_PAR_JOUR = 50;

    public static Constraint heuresMinimumParJour(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels avec un seuil de minimum horaire journalier configuré
            .forEach(SalarieReel.class)
            .filter(s -> s.getContraintesReglementaires() != null
                    && s.getContraintesReglementaires().getHeuresMinimumParJour() != null)

            // 2) Jointure avec les créneaux affectés, pauses exclues
            // [Phase 8] estSegmentDePause = true → exclu du calcul des heures de travail
            .join(
                factory.forEach(Creneau.class)
                    .filter(c -> c.getRessourceAffectee() != null)
                    .filter(c -> !Boolean.TRUE.equals(c.getEstSegmentDePause())),
                Joiners.equal(
                    SalarieReel::getId,
                    c -> c.getRessourceAffectee().getId()
                )
            )

            // 3) Filtre : uniquement les créneaux travaillés (compteDansCharge = true)
            // [Phase 10A] Fallback codeActiviteId → activite (cohérence avec le reste du moteur)
            .ifExists(
                ReferentielComptabiliteActivite.class,
                Joiners.filtering((salarie, creneau, ref) -> {
                    ComptabiliteActivite ca = ref.getByCode(creneau.getCodeActiviteEffectif());
                    return ca != null && ca.isCompteDansCharge();
                })
            )

            // 4) Groupement par (salarié, date) → somme des durées de la journée
            .groupBy(
                (salarie, creneau) -> salarie,
                (salarie, creneau) -> creneau.getDate(),
                ConstraintCollectors.sum((salarie, creneau) -> creneau.getDuree())
            )

            // 5) Pénalité SOFT proportionnelle au déficit en minutes
            .penalize(
                HardSoftScore.ONE_SOFT,
                (salarie, date, totalMinutes) -> {
                    int minMinutes = (int)(
                        salarie.getContraintesReglementaires().getHeuresMinimumParJour() * 60
                    );
                    int deficit = minMinutes - totalMinutes;
                    return deficit > 0 ? PENALITE_HEURES_MIN_PAR_JOUR * deficit : 0;
                }
            )
            .asConstraint(PenaliteKey.LEGAL_SOFT_HEURES_MIN_PAR_JOUR.name());
    }
}
