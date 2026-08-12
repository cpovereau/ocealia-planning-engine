package fr.project.planning.scenarios.alerte;

/**
 * Vocabulaire des alertes de pré-résolution, commun aux trois scénarios.
 *
 * <h3>[Lot S8.4] Pourquoi ce type a quitté le builder SC-01</h3>
 * <p>Il y vivait comme enum imbriqué, parce que SC-01 était seul à produire des alertes : SC-03 et
 * SC-06 passaient {@code List.of()} en dur. Tout ce qu'ils détectaient en préparation partait dans
 * un {@code log.warn} que l'appelant ne lit jamais — y compris les décisions prises
 * <strong>à sa place</strong>, comme la plage de nuit par défaut appliquée quand la sienne est
 * inexploitable.</p>
 *
 * <h3>Ce que cette liste couvre, et ce qu'elle ne couvre pas</h3>
 * <p>Une alerte porte sur la <strong>configuration</strong> ou sur le <strong>dataset pris dans
 * son ensemble</strong> : ce que le moteur a décidé sans qu'on le lui demande, ou ce qui rend le
 * résultat sujet à caution. Le détail créneau par créneau des exclusions relève de
 * {@code diagnostics.ignoredCreneaux.details}, pour que la même information ne soit pas restituée
 * deux fois sous deux formes.</p>
 */
public enum AlertCode {

    // ---- SC-01 — génération des créneaux ----

    /** Fin de poste au-delà de la borne d'alerte déclarée. */
    SHIFT_END_EXCEEDED,

    /** Pause midi incohérente avec l'amplitude : un seul créneau généré. */
    LUNCH_BREAK_OUTSIDE_AMPLITUDE,

    /** Aucun jour de repos hebdomadaire configurable à partir de {@code workedDays}. */
    INSUFFICIENT_WEEKLY_REST,

    /** Jours non travaillés au-delà du repos hebdomadaire — configuration atypique, pas un défaut. */
    TOO_MANY_NON_WORKED_DAYS,

    /** Code activité des créneaux générés absent du référentiel injecté. */
    UNKNOWN_ACTIVITY,

    /** Aucun {@code codeActiviteId} déclaré : code historique appliqué par défaut. */
    ACTIVITY_CODE_DEFAULTED,

    // ---- [S8.4] Cadre réglementaire — les trois scénarios ----

    /**
     * La plage de nuit déclarée est inexploitable — une seule borne, ou deux bornes identiques —
     * et la plage légale par défaut lui a été substituée.
     *
     * <p>Décision prise à la place de l'appelant, et qui déplace le score : les minutes de nuit
     * sont comptées sur une plage qu'il n'a pas choisie.</p>
     */
    PLAGE_NUIT_PAR_DEFAUT,

    /**
     * Le calendrier de jours fériés déclaré au contrat fait autorité et diverge de ce que les
     * créneaux du dataset déclarent : des dates marquées fériées en amont ne le sont pas au
     * calendrier retenu.
     */
    CALENDRIER_FERIES_DIVERGENT,

    // ---- [S8.4] Dataset — les trois scénarios ----

    /**
     * Des créneaux ont été écartés avant le solveur. Le détail est dans
     * {@code diagnostics.ignoredCreneaux.details}.
     */
    CRENEAUX_ECARTES_AVANT_SOLVEUR,

    /** Aucun créneau n'a survécu aux partitions : la résolution porte sur un problème vide. */
    AUCUN_CRENEAU_A_RESOUDRE,

    /**
     * Ni salarié ni poste virtuel au dataset : tous les créneaux reviendront non affectés,
     * quel que soit le reste de la demande.
     */
    AUCUNE_RESSOURCE_DANS_DATASET,

    /** Une ressource du dataset n'a pas d'identifiant : son comportement solveur n'est pas garanti. */
    RESSOURCE_SANS_ID,

    // ---- [S1 de SC-02] Remplacement d'un salarié absent ----

    /**
     * {@code salarieAbsentId} ne désigne aucun salarié du dataset. Le scénario s'exécute — il ne
     * refuse pas — mais aucun créneau n'est libéré : il ne peut rien remplacer.
     */
    SALARIE_ABSENT_INTROUVABLE,

    /**
     * Le salarié désigné absent ne porte aucune indisponibilité dans le dataset. Le moteur ne sait
     * donc pas <em>quand</em> il l'est, et ne libère rien.
     *
     * <p>C'est un vide, pas une permission : le scénario ne suppose pas une absence sur tout
     * l'horizon.</p>
     */
    AUCUNE_ABSENCE_DECLAREE,

    /** L'absence est déclarée mais ne recouvre aucun créneau du salarié : il n'y a rien à remplacer. */
    AUCUN_CRENEAU_A_REMPLACER,

    /**
     * Le poste virtuel n'est pas autorisé par la demande, mais le planning existant en utilise un.
     * Il est conservé — l'existant ne se réécrit pas — et reste donc une couverture possible.
     */
    POSTE_VIRTUEL_REFUSE_MAIS_PRESENT,

    /** Tout ou partie des heures libérées n'a trouvé aucun salarié : elles restent à pourvoir. */
    HEURES_RESTANT_A_POURVOIR
}
