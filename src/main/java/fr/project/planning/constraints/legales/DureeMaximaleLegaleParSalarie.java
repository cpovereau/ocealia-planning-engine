package fr.project.planning.constraints.legales;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.ContraintesReglementairesSalarie;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;

/**
 * DureeMaximaleLegaleParSalarie — contrainte HARD
 *
 * <p>Plafonne la <strong>durée travaillée d'une journée</strong> : somme des créneaux qui comptent
 * dans la charge, pour un salarié et une date.</p>
 *
 * <h3>Lot S7.6 — correction de maille, pas seulement remise en service</h3>
 * <p>Cette contrainte était dormante comme les cinq autres — activité lue via le champ déprécié.
 * Mais elle portait un second défaut, d'une autre nature : son {@code groupBy} agrégeait sur
 * <strong>tout l'horizon</strong> et comparait ce cumul à une constante de 780 minutes, qui est
 * une valeur <strong>journalière</strong> (13 h). Elle mesurait une période et la comparait à un
 * jour. Réveillée en l'état, deux journées ordinaires auraient suffi à la violer.</p>
 *
 * <p>Trois corrections : agrégation par {@code (salarié, date)}, plafond lu sur le salarié via
 * {@code contraintesReglementaires.heuresMaximumParJour}, et suppression de la constante globale.
 * La clé de pénalité a été renommée en conséquence — voir
 * {@link PenaliteKey#LEGAL_HARD_DUREE_MAX_PAR_JOUR}.</p>
 *
 * <h3>Durée travaillée, à ne pas confondre avec amplitude</h3>
 * <p>{@code heuresMaximumParJour} plafonne le <strong>temps de travail</strong> de la journée.
 * {@code amplitudeJournaliereMaximum}, appliqué par {@link AmplitudeJournaliere}, borne l'écart
 * entre la première et la dernière heure, <strong>pauses comprises</strong>. Une journée
 * 08:00–12:00 puis 18:00–22:00 totalise 8 h travaillées pour 14 h d'amplitude : les deux règles
 * mesurent des choses différentes et se complètent.</p>
 *
 * <h3>Activation</h3>
 * <p>Inactive tant que le plafond n'est pas transmis, cf.
 * {@link ContraintesReglementairesSalarie#borneRenseignee(Number)}. Un plafond à <strong>0</strong>
 * est appliqué à la lettre : toute minute travaillée devient excédentaire.
 * Le plancher <em>physique</em>
 * — 24 h cumulées sur une journée, 12 h pour un créneau — reste assuré en toute circonstance par
 * {@code LimitePhysique}, qui ne dépend d'aucun seuil transmis.</p>
 */
public class DureeMaximaleLegaleParSalarie {
    private DureeMaximaleLegaleParSalarie() {
    // Utility class
    }

    public static Constraint dureeMaximaleLegaleParSalarie(ConstraintFactory factory) {

        return factory
        // 1️⃣ Salariés réels dont le plafond journalier est renseigné
        .forEach(SalarieReel.class)
        .filter(DureeMaximaleLegaleParSalarie::plafondRenseigne)

        // 2️⃣ Jointure avec les créneaux affectés à ce salarié
        .join(
            factory.forEach(Creneau.class)
                .filter(c -> c.getRessourceAffectee() != null),
            Joiners.equal(
                SalarieReel::getId,
                c -> c.getRessourceAffectee().getId()
            )
        )

        // 3️⃣ On ne garde que le TRAVAIL (au sens "compte dans la charge")
        .ifExists(
            ReferentielComptabiliteActivite.class,
            Joiners.filtering((salarie, creneau, ref) -> {
                ComptabiliteActivite ca = ref.getByCode(creneau.getCodeActiviteEffectif());
                return ca != null && ca.isCompteDansCharge();
            })
        )

        // 4️⃣ Agrégation par salarié ET PAR JOUR — c'est la maille corrigée au lot S7.6
        .groupBy(
            (salarie, creneau) -> salarie,
            (salarie, creneau) -> creneau.getDate(),
            ConstraintCollectors.sum((salarie, creneau) -> creneau.getDuree())
        )

        // 5️⃣ Détection du dépassement
        .filter((salarie, date, dureeJour) -> dureeJour > plafondMinutes(salarie))

        // 6️⃣ Pénalité HARD proportionnelle aux minutes excédentaires
        .penalize(
            HardSoftScore.ONE_HARD,
            (salarie, date, dureeJour) -> dureeJour - plafondMinutes(salarie)
        )
        .asConstraint(PenaliteKey.LEGAL_HARD_DUREE_MAX_PAR_JOUR.name());
    }

    private static boolean plafondRenseigne(SalarieReel salarie) {
        return ContraintesReglementairesSalarie.borneRenseignee(
                salarie.contraintesOuAucune().getHeuresMaximumParJour());
    }

    /** N'est appelé qu'après {@link #plafondRenseigne(SalarieReel)} : la valeur est non nulle. */
    private static int plafondMinutes(SalarieReel salarie) {
        return (int) (salarie.contraintesOuAucune().getHeuresMaximumParJour() * 60);
    }
}
