package fr.project.planning.scenarios.dto;

/**
 * Pourquoi un créneau figure dans {@code diagnostics.ignoredCreneaux.details}.
 *
 * <p>Le motif dit <strong>ce qui a été constaté</strong>, jamais ce qui en a été fait : le même
 * constat n'a pas les mêmes suites selon le scénario. Une activité inconnue exclut le créneau en
 * SC-03, qui partitionne réellement, et ne l'exclut pas en SC-01, qui mesure sans écarter. C'est
 * {@link CreneauIgnoreDTO#exclu()} qui tranche, cas par cas.</p>
 */
public enum MotifCreneauIgnore {

    /** Date hors de l'horizon [dateDebut, dateFin]. */
    HORS_HORIZON,

    /** Code activité absent du référentiel d'activités. */
    ACTIVITE_INCONNUE,

    /**
     * Aucune ressource du dataset ne déclare l'activité de ce créneau.
     *
     * <p>Constat structurel qui porte sur le dataset entier : il ne dit pas qu'une affectation
     * donnée est incompatible, seulement que personne ne déclare cette activité.</p>
     */
    AUCUNE_RESSOURCE_DANS_DATASET,

    /**
     * Marqueur de repos hebdomadaire impossible à rattacher — {@code ressourceAffecteeId} absent,
     * ou désignant une ressource hors dataset.
     *
     * <p>Écarté, et donc <strong>absent de la réponse</strong> : l'appelant recharge le planning
     * qu'on lui rend, et ce repos y fera un trou. C'est précisément pourquoi il doit l'apprendre
     * autrement que par les journaux du serveur.</p>
     */
    MARQUEUR_REPOS_NON_RATTACHE
}
