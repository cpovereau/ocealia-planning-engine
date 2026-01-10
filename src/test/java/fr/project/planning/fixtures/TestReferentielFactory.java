package fr.project.planning.fixtures;

import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;

import java.util.Map;

import static fr.project.planning.domain.metier.ComptabiliteActivite.TypeImpactActivite;

/**
 * Fixtures de référentiels métier pour les tests WorkMetrics V2.
 */
public final class TestReferentielFactory {

    private TestReferentielFactory() {
    }

    /**
     * Référentiel minimal :
     * - une activité qui génère une dette de repos
     * - sans charge
     * - sans service critique
     * - sans priorité confort
     */
    public static ReferentielComptabiliteActivite referentielActiviteDetteRepos() {

        ComptabiliteActivite activiteDetteRepos =
                new ComptabiliteActivite(
                        "ACTIVITE",
                        false,                    // compteDansCharge
                        true,                     // genereDetteRepos
                        false,                    // estServiceCritique
                        false,                    // prioritaireSurConfort
                        TypeImpactActivite.DETTE_REPOS
                );

        return new ReferentielComptabiliteActivite(
                Map.of(
                        "ACTIVITE", activiteDetteRepos
                )
        );
    }
   
public static ReferentielComptabiliteActivite referentielActiviteSansDetteRepos() {
    /**
     * Référentiel minimal :
     * - une activité qui ne génère pas de dette de repos
     * - sans charge
     * - sans service critique
     * - sans priorité confort
     */
    ComptabiliteActivite activiteSansDette =
            new ComptabiliteActivite(
                    "ACTIVITE",
                    false,                    // compteDansCharge
                    false,                    // genereDetteRepos
                    false,                    // estServiceCritique
                    false,                    // prioritaireSurConfort
                    TypeImpactActivite.NEUTRE
            );

    return new ReferentielComptabiliteActivite(
            Map.of(
                    "ACTIVITE", activiteSansDette
            )
    );
}
}