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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * HeuresMaximumParSemaine — contrainte SOFT (lot S3, scénario SC-06)
 *
 * <p>Pénalise le dépassement de la durée hebdomadaire maximale de travail d'un salarié.</p>
 *
 * <h3>Définition de la semaine</h3>
 * <p><strong>Lundi → dimanche calendaire</strong>, arbitrage tranché au §4.8 de
 * {@code 92_cadrage_scenario_sc-06.md}. Une semaine est identifiée par la date de son lundi.</p>
 *
 * <p>Un créneau est rattaché <strong>en entier</strong> à la semaine de sa {@code date}, y compris
 * lorsqu'il franchit minuit du dimanche au lundi. Sa durée n'est jamais scindée entre deux
 * semaines : {@code Creneau.duree} est une donnée d'entrée atomique, calculée en amont et jamais
 * recalculée par le moteur.</p>
 *
 * <h3>Complétude de la semaine</h3>
 * <p>La règle mesure ce qu'elle reçoit. Si l'appelant ne transmet qu'une fraction de semaine, le
 * total est mécaniquement sous-évalué et aucun dépassement n'est détecté. C'est la raison pour
 * laquelle SC-06 exige une semaine pleine pour toutes les ressources candidates, et le vérifie —
 * voir {@code 92_cadrage_scenario_sc-06.md} §4.8 et §5.3.</p>
 *
 * <h3>Périmètre du calcul</h3>
 * <p>Identique aux autres contraintes individuelles : segments de pause exclus, activités dont
 * {@code compteDansCharge = false} exclues.</p>
 *
 * <h3>Activation</h3>
 * <p>Inactive si {@code ContraintesReglementairesSalarie.heuresMaximumParSemaine == null}.
 * Une valeur {@code 0} activerait la contrainte avec un seuil nul — le contrat d'entrée impose
 * d'<strong>omettre le champ</strong> pour désactiver la règle, jamais d'envoyer 0.</p>
 *
 * <h3>Pénalité</h3>
 * <p>{@code PENALITE_HEURES_MAX_PAR_SEMAINE} × minutes de dépassement, par semaine en dépassement.</p>
 */
public class HeuresMaximumParSemaine {

    private HeuresMaximumParSemaine() {
        // Utility class
    }

    /**
     * Pénalité par minute de dépassement.
     *
     * <p>Fixée au double de {@code penaliteAmplitude} (50), au même niveau que
     * {@link ReposQuotidienMinimum} : dépassement d'une borne légale et non d'une borne de confort.</p>
     *
     * <p>Constante locale, comme dans {@link HeuresMinimumParJour}. À externaliser vers
     * {@code Penalites} si un paramétrage indépendant s'avère nécessaire (lot S7).</p>
     */
    private static final int PENALITE_HEURES_MAX_PAR_SEMAINE = 100;

    public static Constraint heuresMaximumParSemaine(ConstraintFactory factory) {

        return factory
            // 1) Salariés réels avec un maximum hebdomadaire configuré
            .forEach(SalarieReel.class)
            .filter(s -> s.getContraintesReglementaires() != null
                    && s.getContraintesReglementaires().getHeuresMaximumParSemaine() != null)

            // 2) Jointure avec les créneaux affectés à ce salarié, pauses exclues
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
            .ifExists(
                ReferentielComptabiliteActivite.class,
                Joiners.filtering((salarie, creneau, ref) -> {
                    String codeActivite = (creneau.getCodeActiviteId() != null && !creneau.getCodeActiviteId().isBlank())
                            ? creneau.getCodeActiviteId() : creneau.getActivite();
                    ComptabiliteActivite ca = ref.getByCode(codeActivite);
                    return ca != null && ca.isCompteDansCharge();
                })
            )

            // 4) Groupement par (salarié, semaine) → somme des durées de la semaine
            .groupBy(
                (salarie, creneau) -> salarie,
                (salarie, creneau) -> lundiDeLaSemaine(creneau.getDate()),
                ConstraintCollectors.sum((salarie, creneau) -> creneau.getDuree())
            )

            // 5) Pénalité SOFT proportionnelle à l'excédent en minutes pour cette semaine
            .penalize(
                HardSoftScore.ONE_SOFT,
                (salarie, lundi, totalMinutes) -> {
                    int maxMinutes = (int)(
                        salarie.getContraintesReglementaires().getHeuresMaximumParSemaine() * 60
                    );
                    int excedent = totalMinutes - maxMinutes;
                    return excedent > 0 ? PENALITE_HEURES_MAX_PAR_SEMAINE * excedent : 0;
                }
            )
            .asConstraint(PenaliteKey.LEGAL_SOFT_HEURES_MAX_PAR_SEMAINE.name());
    }

    /**
     * Retourne le lundi de la semaine calendaire contenant la date fournie.
     * Sert de clé de regroupement hebdomadaire.
     */
    static LocalDate lundiDeLaSemaine(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
