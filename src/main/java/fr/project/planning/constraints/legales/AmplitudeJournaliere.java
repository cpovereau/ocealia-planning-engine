package fr.project.planning.constraints.legales;

import fr.project.planning.domain.contexte.PlanningContext;
import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;

import java.util.List;

/**
 * AmplitudeJournaliere — contrainte SOFT (R10)
 *
 * Pénalise le dépassement de l'amplitude journalière maximale d'un salarié.
 *
 * Amplitude journalière = max(heureFin) - min(heureDebut) des créneaux compteDansCharge = true,
 * exprimée en minutes. Les créneaux à cheval sur minuit sont correctement gérés
 * (heureFin ajustée de +24h si heureFin < heureDebut).
 *
 * Seuil : ContraintesReglementairesSalarie.amplitudeJournaliereMaximum (en heures).
 * Activation : {@link ContraintesReglementairesSalarie#borneRenseignee(Number)}, source unique
 * de la règle depuis le lot S8.3.
 *
 * Pénalité : Penalites.penaliteAmplitude × minutes de dépassement, par jour en dépassement.
 */
public class AmplitudeJournaliere {

    private AmplitudeJournaliere() {
        // Utility class
    }

    public static Constraint amplitudeJournaliere(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels avec un seuil d'amplitude configuré
            //    [S8.3] Règle d'activation commune — cf. ContraintesReglementairesSalarie.
            .forEach(SalarieReel.class)
            .filter(s -> ContraintesReglementairesSalarie.borneRenseignee(
                    s.contraintesOuAucune().getAmplitudeJournaliereMaximum()))

            // 2) Jointure avec les créneaux affectés à ce salarié, pauses exclues
            // [Phase 8] estSegmentDePause = true → exclu du calcul d'amplitude (segment non productif)
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
            //    ifExists évite le cross join : vérifie l'existence sans créer de produit cartésien
            // [Phase 10A] Fallback codeActiviteId → activite (cohérence avec le reste du moteur)
            .ifExists(
                ReferentielComptabiliteActivite.class,
                Joiners.filtering((salarie, creneau, ref) -> {
                    ComptabiliteActivite ca = ref.getByCode(creneau.getCodeActiviteEffectif());
                    return ca != null && ca.isCompteDansCharge();
                })
            )

            // 4) Groupement par (salarié, date) → liste des créneaux de la journée
            //    Supprime le regroupement manuel par date de l'ancienne version
            .groupBy(
                (salarie, creneau) -> salarie,
                (salarie, creneau) -> creneau.getDate(),
                ConstraintCollectors.toList((s, c) -> c)
            )

            // 5) Jointure avec le contexte (valeur de pénalité)
            .join(factory.forEach(PlanningContext.class))

            // 6) Pénalité SOFT proportionnelle à l'excédent en minutes pour cette journée
            .penalize(
                HardSoftScore.ONE_SOFT,
                (salarie, date, creneaux, context) -> {
                    int seuilMinutes = (int)(
                        salarie.contraintesOuAucune().getAmplitudeJournaliereMaximum() * 60
                    );
                    int excedent = calculerAmplitudeMinutes(creneaux) - seuilMinutes;
                    return excedent > 0
                        ? context.getPenalites().getPenaliteAmplitude() * excedent
                        : 0;
                }
            )
            .asConstraint(PenaliteKey.PHYSIQUE_SOFT_AMPLITUDE_JOURNALIERE.name());
    }

    /**
     * Calcule l'amplitude en minutes pour un ensemble de créneaux d'un même jour.
     *
     * amplitude = max(effectiveFin) - min(heureDebut)
     *
     * Les créneaux à cheval sur minuit (heureFin < heureDebut) sont corrigés :
     * effectiveFin = heureFinMinutes + 1440.
     */
    public static int calculerAmplitudeMinutes(List<Creneau> creneaux) {
        if (creneaux.isEmpty()) return 0;

        int minDebut = Integer.MAX_VALUE;
        int maxFin = Integer.MIN_VALUE;

        for (Creneau c : creneaux) {
            int debut = c.getHeureDebut().toSecondOfDay() / 60;
            int fin   = c.getHeureFin().toSecondOfDay() / 60;
            if (fin < debut) {
                fin += 1440; // créneau à cheval sur minuit
            }
            if (debut < minDebut) minDebut = debut;
            if (fin   > maxFin)   maxFin   = fin;
        }

        return maxFin - minDebut;
    }
}
