package fr.project.planning.constraints.physiques;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import fr.project.planning.scoring.PenaliteKey;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.Joiners;

public class LimitePhysique {
    private LimitePhysique() {
        // Utility class
    }

    /**
     * 1️⃣ Un salarié réel ne peut pas avoir deux créneaux qui se chevauchent.
     *
     * <h3>[Lot S8.3] Le chevauchement se mesure sur des instants, pas sur des heures</h3>
     * <p>La règle comparait jusqu'ici des {@link java.time.LocalTime} nus, après avoir apparié
     * les créneaux <strong>de même date</strong>. Elle était aveugle des deux côtés de minuit :</p>
     * <ul>
     *   <li><em>dates différentes</em> — un créneau du 3 mars 22:00–06:00 se termine le 4 mars à
     *       06:00 ; un créneau du 4 mars 02:00–10:00 le recouvre de quatre heures. Les deux
     *       n'étaient jamais appariés, donc jamais comparés ;</li>
     *   <li><em>même date</em> — sur 22:00–06:00 et 23:00–23:30, le test
     *       {@code 23:00.isBefore(06:00)} est faux : deux créneaux réellement simultanés étaient
     *       déclarés disjoints.</li>
     * </ul>
     * <p>Un salarié pouvait donc être à deux endroits à la fois sans qu'aucun point HARD ne le
     * signale — sur les nuits, précisément là où l'erreur est la plus probable.</p>
     *
     * <p>L'appariement porte désormais sur la ressource et sur le recouvrement des intervalles
     * {@code [début, fin[} absolus, via {@link Creneau#getDebutEffectif()} et
     * {@link Creneau#getFinEffectif()}. {@link Joiners#overlapping} applique la même comparaison
     * stricte qu'auparavant : deux créneaux jointifs — 08:00–12:00 puis 12:00–16:00 — ne se
     * chevauchent pas.</p>
     */
    public static Constraint pasDeChevauchement(ConstraintFactory factory) {

    return factory
        .forEachUniquePair(
            Creneau.class,
            Joiners.equal(Creneau::getRessourceAffectee),
            Joiners.overlapping(Creneau::getDebutEffectif, Creneau::getFinEffectif)
        )
        .filter((c1, c2) -> c1.getRessourceAffectee() instanceof SalarieReel)
        .penalize(HardSoftScore.ONE_HARD)
        .asConstraint(PenaliteKey.PHYSIQUE_HARD_CHEVAUCHEMENT_CRENEAUX.name());
    }

    /**
     * 2️⃣ Un créneau ne peut pas dépasser 12h (720 minutes).
     */
    public static Constraint dureeMaxCreneau(ConstraintFactory factory) {

        return factory
            .forEach(Creneau.class)
            .filter(creneau -> creneau.getDuree() > 720)
            .penalize(
                HardSoftScore.ONE_HARD,
                creneau -> creneau.getDuree() - 720
            )
            .asConstraint(PenaliteKey.PHYSIQUE_HARD_DUREE_CRENEAU_MAX.name());
    }

    /**
     * 3️⃣ Un salarié réel ne peut pas cumuler plus de 24h sur une journée.
     */
    public static Constraint cumulJournalierMax(ConstraintFactory factory) {

        return factory
            .forEach(Creneau.class)
            .filter(creneau ->
                creneau.getRessourceAffectee() != null
                && creneau.getRessourceAffectee() instanceof SalarieReel
        )
            .groupBy(
                creneau -> (SalarieReel) creneau.getRessourceAffectee(),
                Creneau::getDate,
                ConstraintCollectors.sum(Creneau::getDuree)
            )
            .filter((salarie, date, dureeTotale) -> dureeTotale > 1440)
            .penalize(
                HardSoftScore.ONE_HARD,
                (salarie, date, dureeTotale) -> dureeTotale - 1440
            )
            .asConstraint(PenaliteKey.PHYSIQUE_HARD_CUMUL_JOURNALIER_MAX.name());
    }
}
