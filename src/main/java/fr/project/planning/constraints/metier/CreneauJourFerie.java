package fr.project.planning.constraints.metier;

import fr.project.planning.domain.creneau.Creneau;
import fr.project.planning.domain.creneau.QualificationJour;
import fr.project.planning.domain.ressource.RessourceNonAffectee;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;


public class CreneauJourFerie {
    private CreneauJourFerie() {
        // Utility class
    }   

    public static Constraint creneauJourFerie(ConstraintFactory factory) {

    return factory
        .forEach(Creneau.class)

        // Créneaux réellement affectés (null-safe)
        .filter(creneau ->
            creneau.getRessourceAffectee() != null
            && !(creneau.getRessourceAffectee() instanceof RessourceNonAffectee)
        )

        // Créneaux qualifiés jour férié
        .filter(creneau ->
            creneau.getQualificationJour() == QualificationJour.FERIE
        )

        // Jointure référentiel : travail réel uniquement
        .join(factory.forEach(ReferentielComptabiliteActivite.class))
        .filter((creneau, ref) -> {
            ComptabiliteActivite ca = ref.getByCode(creneau.getActivite());
            return ca != null && ca.isCompteDansCharge();
        })

        // Pénalisation en minutes
        .penalizeLong(
            HardSoftScore.ONE_SOFT,
            (creneau, ref) -> (long) creneau.getDuree()
        )

        .asConstraint("Travail jour férié (minutes)");

    }
}
