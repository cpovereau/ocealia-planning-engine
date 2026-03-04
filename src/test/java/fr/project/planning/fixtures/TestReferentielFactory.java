package fr.project.planning.fixtures;

import fr.project.planning.domain.metier.ComptabiliteActivite;
import fr.project.planning.domain.metier.ReferentielComptabiliteActivite;
import fr.project.planning.domain.metier.ComptabiliteActivite.TypeImpactActivite;

import java.util.Map;

/**
 * Factory de référentiels pour les tests.
 *
 * Convention V2/V3 :
 * La clé du référentiel = idActivitePlanning (ex: "10101", "101478"...)
 * afin d’être cohérent avec le logiciel de planning.
 */
public final class TestReferentielFactory {

    private TestReferentielFactory() {
    }

    // ============================================================
    // Codes activité planning utilisés dans les tests
    // ============================================================

    public static final String ID_ACTIVITE_PLANNING_TRAVAIL = "10101";
    public static final String ID_ACTIVITE_PLANNING_SYNDICAT = "101478";
    public static final String ID_ACTIVITE_PLANNING_TRAVAIL_DETTE_REPOS = "19999";

    public static ReferentielComptabiliteActivite referentielActiviteDetteRepos() {
        return referentielWorkMetricsV2Minimal();
    }

    /**
     * Référentiel minimal neutre (utilisable dans d'autres tests).
     *
     * ⚠️ IMPORTANT :
     * - La clé doit rester un idActivitePlanning.
     * - Cette variante fournit une activité "travail" standard (compteDansCharge=true),
     *   sans dette de repos.
     */
    public static ReferentielComptabiliteActivite referentielActiviteSansDetteRepos() {

        ComptabiliteActivite travailStandard = new ComptabiliteActivite(
                ID_ACTIVITE_PLANNING_TRAVAIL,
                true,   // compteDansCharge
                false,  // genereDetteRepos
                false,  // estServiceCritique (choix client, défaut false)
                false,  // prioritaireSurConfort (choix client, défaut false)
                TypeImpactActivite.CHARGE_STANDARD
        );

        return new ReferentielComptabiliteActivite(
                Map.of(ID_ACTIVITE_PLANNING_TRAVAIL, travailStandard)
        );
    }

    /**
     * Référentiel minimal réaliste pour WorkMetrics V2 :
     *
     * 10101  : travail standard → compte dans la charge
     * 101478 : représentation syndicale → compte dans la charge
     * 19999  : travail générant dette repos → charge + dette repos
     *
     * estServiceCritique et prioritaireSurConfort restent false
     * (choix client facultatifs).
     */
    public static ReferentielComptabiliteActivite referentielWorkMetricsV2Minimal() {

        ComptabiliteActivite travailStandard =
                new ComptabiliteActivite(
                        ID_ACTIVITE_PLANNING_TRAVAIL,
                        true,   // compteDansCharge
                        false,  // genereDetteRepos
                        false,
                        false,
                        TypeImpactActivite.CHARGE_STANDARD
                );

        ComptabiliteActivite representationSyndicale =
                new ComptabiliteActivite(
                        ID_ACTIVITE_PLANNING_SYNDICAT,
                        true,   // compteDansCharge
                        false,  // genereDetteRepos
                        false,
                        false,
                        TypeImpactActivite.CHARGE_STANDARD
                );

        ComptabiliteActivite travailDetteRepos =
                new ComptabiliteActivite(
                        ID_ACTIVITE_PLANNING_TRAVAIL_DETTE_REPOS,
                        true,   // compteDansCharge
                        true,   // genereDetteRepos
                        false,
                        false,
                        TypeImpactActivite.DETTE_REPOS
                );

        return new ReferentielComptabiliteActivite(
                Map.of(
                        ID_ACTIVITE_PLANNING_TRAVAIL, travailStandard,
                        ID_ACTIVITE_PLANNING_SYNDICAT, representationSyndicale,
                        ID_ACTIVITE_PLANNING_TRAVAIL_DETTE_REPOS, travailDetteRepos
                )
        );
    }
}