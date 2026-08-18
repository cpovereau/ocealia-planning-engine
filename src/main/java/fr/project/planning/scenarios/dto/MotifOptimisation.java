package fr.project.planning.scenarios.dto;

/**
 * MotifOptimisation — ce qui disqualifie le planning rendu par SC-04 (lot O2).
 *
 * <p>Même lecture que {@link MotifArbitrage} pour SC-05, et pour la même raison : <em>le moteur ne
 * refuse pas, il rend visible l'impossible</em>. Le planning optimisé est toujours restitué, et ces
 * motifs disent ce qui, le cas échéant, le disqualifie.</p>
 *
 * <h3>Ce que SC-04 ajoute : la régression</h3>
 * <p>Le contrat annonce « gains / régressions explicitées ». Un scénario qui rouvre une période
 * entière peut améliorer le total en dégradant quelqu'un — c'est même la façon la plus ordinaire
 * d'optimiser un planning, et celle qui le fait rejeter. {@link #REGRESSION_INDIVIDUELLE} nomme ce
 * cas au lieu de le laisser découvrir.</p>
 *
 * <h3>⚠️ « Acceptable » veut dire : au regard de ce que le moteur sait</h3>
 * <p>Et le moteur ignore les <strong>préférences individuelles</strong> — rang 10, en attente du
 * Produit. Sur un scénario qui remanie une période entière, cette ignorance pèse davantage que
 * partout ailleurs : l'absence de motif ne vaut pas accord des intéressés.</p>
 */
public enum MotifOptimisation {

    // ---- Éliminatoires ----

    /**
     * Le planning optimisé viole au moins une contrainte HARD.
     *
     * <p>Il est rendu malgré tout — c'est le moins mauvais que le solveur ait trouvé, et le taire
     * laisserait l'appelant sans rien. Le détail est dans {@code solverResult.scoreBreakdown}.</p>
     */
    PLANNING_NON_CONFORME(MotifArbitrage.Severite.ERROR, true,
            "Le planning optimisé viole au moins une contrainte impérative — voir scoreBreakdown"),

    /**
     * Au moins un salarié sort de l'optimisation <strong>plus loin de son contrat</strong> qu'il
     * n'y entrait, sur la période.
     *
     * <p>C'est la régression que le contrat demande d'expliciter. Optimiser un total en dégradant
     * une personne est la manière la plus ordinaire de produire un planning que personne
     * n'acceptera, et le moteur n'a aucun moyen de savoir si ce sacrifice est consenti.</p>
     *
     * <p>Mesurée sur la tranche {@code PERIODE} et en valeur absolue de l'écart : quelqu'un qui
     * passe de −20 % à −25 % régresse autant que celui qui passe de +20 % à +25 %.</p>
     */
    REGRESSION_INDIVIDUELLE(MotifArbitrage.Severite.WARNING, true,
            "Au moins un salarié sort plus loin de son contrat qu'il n'y entrait"),

    /**
     * Un créneau que quelqu'un assurait ne trouve plus personne.
     *
     * <p>À distinguer d'un créneau qui n'était déjà à personne : celui-là n'a rien perdu. Ici, du
     * travail assuré a disparu du planning — ce qu'une optimisation n'est jamais censée produire.</p>
     */
    CRENEAU_PERDU(MotifArbitrage.Severite.ERROR, true,
            "Un créneau que quelqu'un assurait ne trouve plus personne"),

    /**
     * L'écart au contrat d'au moins un salarié reste au-delà de la tolérance déclarée.
     *
     * <p>Sans tolérance déclarée, ce motif ne peut pas être levé : <em>une borne absente n'est pas
     * une borne à zéro</em>, et le moteur ne juge pas inéquitable un écart que personne ne lui a
     * dit de juger.</p>
     */
    INEQUITE_RESIDUELLE(MotifArbitrage.Severite.WARNING, true,
            "L'écart au contrat reste au-delà de la tolérance déclarée après optimisation"),

    // ---- Signalements non éliminatoires ----

    /**
     * Aucun créneau n'a changé de main.
     *
     * <p>Le planning transmis était déjà le meilleur que le moteur sache produire à partir du
     * pivot. Sans ce signalement, l'appelant recevrait son propre planning et chercherait ce qui a
     * échoué.</p>
     */
    OPTIMISATION_SANS_EFFET(MotifArbitrage.Severite.INFO, false,
            "Aucun créneau n'a changé de main : le planning transmis n'a pas pu être amélioré");

    private final String severite;
    private final boolean eliminatoire;
    private final String message;

    MotifOptimisation(String severite, boolean eliminatoire, String message) {
        this.severite = severite;
        this.eliminatoire = eliminatoire;
        this.message = message;
    }

    public String getSeverite() {
        return severite;
    }

    public boolean isEliminatoire() {
        return eliminatoire;
    }

    public String getMessage() {
        return message;
    }

    /** Même forme que les motifs de SC-05 : le client filtre sur la sévérité, pas sur le code. */
    public MotifArbitrageDTO toDto() {
        return new MotifArbitrageDTO(name(), severite, message);
    }
}
