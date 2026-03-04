package fr.project.planning.fixtures;

import fr.project.planning.domain.ressource.*;

import java.util.Set;

public final class TestRessourceFactory {

    // Valeurs "canon" côté tests (doivent matcher les créneaux de test par défaut)
    public static final String SITE_CANON = "SITE";
    public static final String POSTE_COMPTABLE_CANON = "PC";

    private TestRessourceFactory() {
        // utilitaire
    }

    /* =========================
       Salariés
       ========================= */

    public static SalarieReel salarieStandard(String id) {
        return new SalarieReel(
                id,
                "CDI",
                "ACTIF",
                Set.of(SITE_CANON),
                // IMPORTANT : aligné sur le référentiel (clé = idActivitePlanning)
                Set.of(
                        TestReferentielFactory.ID_ACTIVITE_PLANNING_TRAVAIL,
                        TestReferentielFactory.ID_ACTIVITE_PLANNING_SYNDICAT,
                        TestReferentielFactory.ID_ACTIVITE_PLANNING_TRAVAIL_DETTE_REPOS
                ),
                Set.of(POSTE_COMPTABLE_CANON)
        );
    }

    /* =========================
       Postes virtuels
       ========================= */

    public static PosteVirtuel posteVirtuelStandard(String id) {
        return new PosteVirtuel(
                id,
                TypePosteVirtuel.POTENTIEL,
                480, // capacité "journée"
                // IMPORTANT : aligné sur le référentiel (clé = idActivitePlanning)
                Set.of(
                        TestReferentielFactory.ID_ACTIVITE_PLANNING_TRAVAIL,
                        TestReferentielFactory.ID_ACTIVITE_PLANNING_SYNDICAT,
                        TestReferentielFactory.ID_ACTIVITE_PLANNING_TRAVAIL_DETTE_REPOS
                ),
                Set.of(SITE_CANON),
                Set.of(POSTE_COMPTABLE_CANON)
        );
    }
}