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
    HEURES_RESTANT_A_POURVOIR,

    /**
     * Des créneaux libérés ont été découpés, faute d'un remplaçant disponible sur toute leur durée.
     *
     * <p>Informative et non fautive : c'est le mécanisme prévu. Elle est signalée parce que la
     * réponse peut alors contenir <strong>plus de créneaux que la demande</strong>, et que
     * l'appelant doit le savoir sans avoir à le déduire.</p>
     */
    CRENEAUX_DECOUPES,

    /**
     * Un remplaçant dépasse le seuil de surcharge que la demande avait déclaré (lot S3).
     *
     * <p>Signalée, jamais éliminatoire : une borne de confort n'est pas une borne de légalité. Le
     * moteur pèse le dépassement dans le score et le dit ; l'arbitrage reste à l'encadrement.</p>
     */
    SURCHARGE_ACCEPTABLE_DEPASSEE,

    /**
     * Le planning transmis fait déjà travailler un salarié le jour de son repos dominical
     * (lot L0 du chantier équité).
     *
     * <p>Le moteur <strong>interdit</strong> de créer cette situation, mais il n'a pas défait
     * celle-ci : le créneau est épinglé, c'est un fait d'entrée. Le pénaliser rendrait le problème
     * insoluble pour une faute que le solveur n'a pas commise.</p>
     *
     * <p>Il ne la tait pas pour autant. L'appelant doit savoir que sa demande contient ce que le
     * moteur refuserait de produire — sans quoi il lirait un score parfait sur un planning qui ne
     * l'est pas.</p>
     */
    REPOS_DOMINICAL_TRAVAILLE,

    /**
     * Un bloc {@code coefficientsPenibilite} est transmis, mais aucun coefficient ne pondère quoi
     * que ce soit (lot L1 du chantier équité).
     *
     * <p>Quelqu'un a cru configurer une échelle, et n'en a configuré aucune : les heures pondérées
     * valent les heures brutes, et la mesure compare des durées là où l'appelant croit comparer
     * des pénibilités.</p>
     *
     * <p><strong>L'absence complète du bloc ne déclenche rien</strong>, elle. C'est le cas de
     * toutes les demandes tant que les coefficients ne sont pas calibrés, et une alerte sur cent
     * pour cent des réponses n'est pas un signal. L'information est portée là où elle se vérifie :
     * {@code heuresPonderees} vaut alors exactement {@code heuresTravaillees}.</p>
     *
     * <p>{@code INFO} et non {@code WARNING} : c'est une configuration valide, pas une anomalie.</p>
     */
    COEFFICIENTS_PENIBILITE_SANS_EFFET,

    /**
     * L'échelle de coefficients contredit l'ordre de dominance (lot L3 du chantier équité).
     *
     * <p>Deux règles se rencontrent, et elles ne viennent pas du même endroit : la dominance décide
     * à quelle catégorie appartient une minute qui en cumule plusieurs, les coefficients décident ce
     * que cette catégorie pèse. Quand la catégorie retenue pèse moins qu'une de celles qu'elle
     * absorbe, une minute qui cumule deux pénibilités pèse <em>moins</em> qu'une minute qui n'en
     * porte qu'une : <strong>le cumul devient un avantage</strong>.</p>
     *
     * <p>{@code WARNING} et non un refus : l'échelle décrit quelque chose, elle se contredit
     * seulement elle-même. Le moteur produit la mesure et dit ce qu'elle vaut — c'est à l'appelant
     * de trancher entre changer ses coefficients et changer son ordre de dominance.</p>
     */
    COEFFICIENTS_PENIBILITE_INCOHERENTS,

    // ---- [A1 de SC-05] Arbitrage de répartition entre salariés ----

    /**
     * Un créneau du périmètre à arbitrer est tenu par quelqu'un qui n'est pas de l'arbitrage
     * (arbitrage métier du 13/08, §5.2 du cadrage SC-05).
     *
     * <p>Ni refus, ni reprise : le créneau est <strong>épinglé</strong> et l'arbitrage porte sur ce
     * qui reste. Le tiers n'a rien demandé ; on ne lui retire pas son travail pour équilibrer deux
     * autres personnes — <em>l'existant ne se réécrit pas</em>.</p>
     *
     * <p>L'appelant doit néanmoins l'apprendre : il a désigné ce créneau, et il ne bougera pas.
     * Sans cette alerte, il lirait une répartition qui l'ignore sans savoir pourquoi.</p>
     */
    CRENEAU_ARBITRE_TENU_PAR_UN_TIERS,

    /**
     * Un salarié entre lesquels arbitrer n'est pas dans le dataset.
     *
     * <p>Même lecture que {@link #SALARIE_ABSENT_INTROUVABLE} pour SC-02 : le scénario s'exécute —
     * il ne refuse pas — mais l'arbitrage se fait alors entre moins de personnes que demandé, et
     * les créneaux du périmètre ne peuvent revenir qu'aux autres, ou rester à pourvoir.</p>
     */
    SALARIE_ARBITRE_INTROUVABLE,

    /**
     * Le périmètre désigne des créneaux qui ne sont pas dans le dataset, ou qui en ont été écartés
     * avant le solveur.
     *
     * <p>Le périmètre est transmis et jamais déduit (§5.1) : le moteur ne peut donc pas deviner ce
     * que l'appelant visait. L'arbitrage porte sur les créneaux effectivement présents, et cette
     * alerte nomme ceux qui manquent — un périmètre partiellement inconnu rendrait sinon
     * l'arbitrage plus étroit que demandé, sans que personne ne le voie.</p>
     */
    CRENEAU_ARBITRE_INTROUVABLE,

    /**
     * L'écart au contrat d'un salarié arbitré reste au-delà de la tolérance déclarée, une fois
     * l'arbitrage fait (lot A3 de SC-05, §7 du cadrage).
     *
     * <p>L'arbitrage a fait ce qu'il pouvait, et ce n'était pas assez : il n'y avait pas, dans le
     * périmètre remis en jeu, de quoi ramener tout le monde dans la marge. <strong>Ce n'est pas un
     * défaut du moteur</strong> — c'est un constat sur le périmètre, et l'élargir est une décision
     * d'encadrement, pas une décision de moteur.</p>
     *
     * <p>Ne peut pas être levée sans tolérance déclarée : une borne absente n'est pas une borne à
     * zéro, et le moteur ne juge pas inéquitable un écart que personne ne lui a dit de juger.</p>
     */
    INEQUITE_RESIDUELLE,

    /**
     * La date pivot de SC-04 est postérieure à tous les créneaux transmis (lot O3, §5.5).
     *
     * <p>Rien n'est ajustable : l'optimisation ne peut rien changer, et le planning est rendu tel
     * quel avec ses indicateurs. <strong>Le moteur ne refuse pas</strong> — il rend visible que la
     * demande, telle qu'elle est formulée, n'a pas d'objet.</p>
     */
    AUCUN_CRENEAU_AJUSTABLE,

    /**
     * La date pivot de SC-04 est antérieure à tous les créneaux transmis (lot O3, §5.5).
     *
     * <p>Le planning est rouvert en entier. C'est légitime et le moteur l'exécute, mais SC-04
     * promet d'améliorer un planning existant <strong>sans le reconstruire</strong> : un pivot
     * placé hors de la période produit exactement le remaniement que le scénario existe pour
     * éviter, et l'appelant a rarement voulu cela.</p>
     */
    PLANNING_ENTIEREMENT_REOUVERT,

    /**
     * {@code scenarioParameters.creneauxAjustables} désigne des créneaux absents du dataset, ou qui
     * en ont été écartés avant le solveur (lot O5, §5.5).
     *
     * <p>Jumelle de {@link #CRENEAU_ARBITRE_INTROUVABLE} pour SC-04, et pour la même raison : la
     * sélection est transmise, jamais déduite. Sans cette alerte, une liste dont la moitié des
     * identifiants ne correspond à rien produirait une optimisation bien plus étroite que demandée,
     * sans que personne ne voie pourquoi.</p>
     */
    CRENEAU_AJUSTABLE_INTROUVABLE,

    /**
     * {@code scenarioParameters.creneauxAjustables} désigne des créneaux antérieurs à la date
     * pivot (lot O5, §5.5).
     *
     * <p>La conjonction des deux champs est une <strong>intersection</strong> : la liste restreint
     * l'après-pivot, elle ne rouvre pas le passé. Ces identifiants sont donc sans effet. Le moteur
     * ne les refuse pas — désigner un créneau passé est une erreur de bonne foi, souvent le signe
     * d'un pivot mal placé — mais il ne les applique pas en silence.</p>
     */
    CRENEAU_AJUSTABLE_ANTERIEUR_AU_PIVOT,

    /**
     * Des créneaux postérieurs au pivot, que personne ne couvre, sont gelés parce que la sélection
     * ne les désigne pas (lot O5, §5.5).
     *
     * <p>C'est la seule régression que la liste explicite peut introduire, et elle est sournoise :
     * toute la restitution de SC-04 se compte sur les créneaux ajustables, donc ces besoins
     * <strong>sortent du décompte {@code creneauxNonCouverts}</strong>. Le trou reste dans le
     * planning ; le compte cesse de le voir.</p>
     *
     * <p>Ne pas lister, ici, c'est renoncer à couvrir. C'est une décision recevable — l'appelant
     * sait peut-être que ces besoins sont pourvus ailleurs — mais elle doit se lire dans la
     * réponse, et non se découvrir sur le terrain.</p>
     */
    CRENEAU_FUTUR_NON_COUVERT_GELE
}
