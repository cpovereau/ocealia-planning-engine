package fr.project.planning.fixtures;

import fr.project.planning.domain.ressource.*;

import java.util.Set;

public final class TestRessourceFactory {

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
                Set.of("SITE"),
                Set.of("ACTIVITE"),
                Set.of("PC")
        );
    }

    /* =========================
       Postes virtuels
       ========================= */

    public static PosteVirtuel posteVirtuelStandard(String id) {
        return new PosteVirtuel(
                id,
                TypePosteVirtuel.POTENTIEL,
                480,                 // capacité "journée"
                Set.of("ACTIVITE"),
                Set.of("SITE"),
                Set.of("PC")
        );
    }
}
