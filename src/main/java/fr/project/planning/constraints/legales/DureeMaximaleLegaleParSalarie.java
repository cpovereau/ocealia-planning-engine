package fr.project.planning.constraints.legales;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.ressource.SalarieReel;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;

public class DureeMaximaleLegaleParSalarie {
    private DureeMaximaleLegaleParSalarie() {
    // Utility class
    }

    /**
     * Durée maximale légale absolue sur la période de résolution. POINT DE BLOCAGE FORT pour les scénarios de test qui génèrent des durées totales très élevées.
     * (exemple : 13h = 780 minutes)
     *
     * 👉 À externaliser plus tard si besoin.
     */
    private static final int DUREE_MAX_LEGALE = 780;

    public static Constraint dureeMaximaleLegaleParSalarie(ConstraintFactory factory) {

        return factory
  // 1️⃣ On part des créneaux
        .forEach(Creneau.class)

        // 2️⃣ On ne garde que ceux affectés
        .filter(c -> c.getRessourceAffectee() != null)
        .filter(c -> c.getRessourceAffectee() instanceof SalarieReel)

        // 3️⃣ On joint le référentiel (problem fact)
        .join(factory.forEach(ReferentielComptabiliteActivite.class))

        // 4️⃣ On ne garde que le TRAVAIL (au sens "compte dans la charge")
        .filter((creneau, ref) -> {
            ComptabiliteActivite ca = ref.getByCode(creneau.getActivite());
            return ca != null && ca.isCompteDansCharge();
        })

        // 5️⃣ Agrégation par salarié (si vous voulez strictement Salarié réel)
        .groupBy(
            (creneau, ref) -> (SalarieReel) creneau.getRessourceAffectee(),
            ConstraintCollectors.sum((creneau, ref) -> creneau.getDuree())
        )

        // 6️⃣ Détection du dépassement légal
        .filter((salarie, dureeTotale) -> dureeTotale > DUREE_MAX_LEGALE)

        // 7️⃣ Pénalité HARD
        .penalize(
            HardSoftScore.ONE_HARD,
            (salarie, dureeTotale) -> dureeTotale - DUREE_MAX_LEGALE
        )
        .asConstraint("Durée totale par salarié > maximum légal");
    }
}
