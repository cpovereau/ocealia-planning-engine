# SC-06 — Notice d'intégration WinDev

> Désignation de la ressource la plus à même de couvrir un besoin.
> Contrat fonctionnel : `50_ScenarioContract.md` §4 · Réponse : `50_ScenarioResponseContract.md` §6
> Contrat machine : `50_openapi_windev_moteur_v_1.yaml` (schéma `Sc06ScenarioRequest`)

---

## 1. Ce que fait le moteur

Vous transmettez le **planning de la semaine** de vos ressources et un **besoin à couvrir** sur
une journée. Le moteur vous rend **trois manières de le couvrir, classées**, avec pour chacune :
qui, à quel prix, et pourquoi ce rang.

**Le moteur n'y réorganise rien.** Il insère dans le planning que vous lui donnez ; il ne
redistribue jamais l'existant.

---

## 2. Les deux canaux

| Canal | Adresse |
|---|---|
| HTTP | `POST /scenarios/sc06/solve` |
| FileAdapter | dépôt d'un fichier avec `"scenarioType": "SC-06"` |

Les deux produisent le même résultat, vérifié par test. Le FileAdapter est le canal de test
initial retenu.

**Une différence à connaître** : par la voie fichier, la validation déclarative des champs
obligatoires ne s'applique pas. Seuls jouent les garde-fous métier (§4). Un champ obligatoire
absent produira une erreur de préparation plutôt qu'une erreur de validation.

---

## 3. Ce que vous devez envoyer

### 3.1 Le principe à retenir

> **`dataSet.creneaux` = le passé, figé.
> `scenarioParameters.besoin` = la question posée.**

Le besoin ne se met **jamais** dans `dataSet.creneaux`. S'il y était, un `ressourceAffecteeId`
oublié sur une ligne de planning deviendrait silencieusement un besoin à couvrir.

### 3.2 Trois exigences

| # | Exigence | Pourquoi |
|---|---|---|
| 1 | `planningContext.horizon` = **la semaine pleine lundi → dimanche** contenant la date du besoin, pour **toutes** les ressources candidates | Une semaine tronquée sous-évalue le total hebdomadaire : le moteur ne détecterait aucun dépassement et déclarerait conformes des candidats qui ne le sont pas |
| 2 | **Tout créneau du `dataSet` porte son `ressourceAffecteeId`** | Un créneau de planning sans affectation deviendrait une variable de décision, ce que le scénario s'interdit |
| 3 | L'activité du besoin **figure au référentiel transmis** | Une activité inconnue ne pèse sur aucune règle : le besoin serait couvert sans avoir été évalué |

Le non-respect de l'une des trois produit une réponse **HTTP 422** avec un message explicite.

### 3.3 Convention sur les limites — important

> Pour **désactiver** une contrainte individuelle : **omettre le champ**.
> Pour **interdire complètement** quelque chose : **envoyer `0`**.

Le zéro garde son sens arithmétique. `nuitsMaximumParSemaine: 0` signifie « ce salarié ne
travaille aucune nuit », et c'est ainsi que le moteur l'applique. À l'inverse,
`heuresMinimumParSemaine: 0` n'exige rien — un minimum de zéro est toujours atteint.

Une valeur **négative** ne décrit aucune règle : elle est ignorée et signalée dans les journaux.

**Une seule exception**, `reposHebdomadaireFenetreJours` : une fenêtre est une *taille*, pas une
limite. « Au moins 2 jours off sur 0 jour » ne veut rien dire. Ce champ n'est pris en compte
qu'à partir de 1 jour.

> **Ce point a changé au lot S7.7.** La règle précédente traitait `0` comme une désactivation.
> Vos payloads SC-03 envoient `nuitsMaximumParSemaine: 0` pour signifier « aucune nuit » : c'est
> désormais ce que le moteur applique, et non plus l'inverse.

### 3.4 Valeurs à toujours transmettre

Dans le bloc `contrat` de chaque salarié :

| Champ | Conséquence si absent |
|---|---|
| `heuresHebdomadairesHabituelles` | Le salarié est **classé en dernier** au palier de départage : à égalité par ailleurs, le moteur préfère la personne dont il peut mesurer l'impact |
| `heuresMoyennesParJour` | Impact non mesurable sur ce champ |
| `joursTravaillesParSemaine` | Le moteur applique **5** par défaut |

---

## 4. Ce que vous recevez

Le bloc `candidats[]`, au plus trois solutions, de la plus favorable à la moins favorable.

Une liste plus courte **n'est pas une anomalie** : elle signifie qu'il n'existe pas davantage de
manières distinctes de couvrir ce besoin.

### 4.1 Lire un candidat

| Champ | Question à laquelle il répond |
|---|---|
| `affectations[]` | **Qui** — identité, activité, lieu |
| `impacts[]` | **À quel prix** — amplitude et volumes horaires, avant / après |
| `motifs[]` | **Pourquoi ce rang** |
| `conforme` | La solution viole-t-elle une règle éliminatoire ? |
| `nature` | Une personne, plusieurs, ou une ressource à pourvoir |

### 4.2 Deux règles de lecture

**Filtrez sur `severite`, pas sur `code`.** Les codes évolueront ; les trois sévérités
(`INFO`, `WARNING`, `ERROR`) sont stables.

**Une solution `conforme: false` est restituée, pas masquée.** Elle est classée en dernier. Le
moteur ne refuse pas : il rend visible l'impossible. C'est ce qui vous permet d'afficher
« aucune solution conforme, voici la moins mauvaise et pourquoi » plutôt qu'un écran vide.

### 4.3 Point de vigilance sur les impacts

⚠️ **Un `depassement: true` décrit une conséquence, il ne garantit pas que le moteur la
sanctionne.** N'en faites pas une règle de gestion sans vérifier l'état d'avancement dans
`90_SUIVI_DEVELOPPEMENT_MOTEUR.md`.

**Mise à jour — lot S7.6** : `heuresJour` faisait précisément exception, mesuré sans qu'aucune
contrainte ne l'applique. Ce n'est plus le cas : `heuresMaximumParJour` est désormais appliqué, et
un dépassement produit un motif éliminatoire `DUREE_JOURNALIERE_DEPASSEE`. Les trois mesures du
bloc `impacts[]` sont maintenant adossées à une contrainte.

---

## 5. Fichiers de cette notice

| Fichier | Rôle |
|---|---|
| `sc_06_reference.json` | Requête complète et valide, **identique au jeu de test du moteur** |
| `sc_06_fileadapter_input.json` | Même contenu, à déposer tel quel dans l'inbox du FileAdapter |

Ces deux fichiers sont la copie exacte de la fixture exécutée par la suite de tests : ce qu'ils
décrivent fonctionne réellement, ce n'est pas un exemple rédigé à la main.

### Ce que le jeu de référence illustre

Besoin : mercredi 13 mai, 14:00 → 22:00, activité `ACT-SOIN`.

| Salarié | Situation | Résultat attendu |
|---|---|---|
| SAL-2001 | travaille déjà le mercredi matin | **rang 1** — conforme, pas de rappel |
| SAL-2002 | libre ce jour-là | **rang 2** — conforme, mais rappelé sur son repos |
| SAL-2003 | reprend le jeudi à 04:00 | **rang 3** — `conforme: false`, repos quotidien insuffisant |
| SAL-2004 | ne pratique pas l'activité | absent du podium |
| SAL-2005 | en congé ce jour-là | absent du podium |

Pour le rang 1, l'amplitude du mercredi passe de **4 h à 14 h** : sa journée s'étirerait de 08:00
à 22:00.
