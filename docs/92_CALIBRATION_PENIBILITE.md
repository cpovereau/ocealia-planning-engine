# 92 — Calibration des coefficients de pénibilité

> **Statut** : protocole d'ingénierie, livré au **lot L3** du chantier équité (2026-08-13).
> Cadrage : `92_CADRAGE_WORKMETRICS_EQUITE.md`. Ce document décrit l'instrument et la façon de s'en
> servir. **Il ne propose aucune valeur** — c'est précisément ce que l'instrument existe pour ne pas
> faire.

---

## 1. Ce que la calibration décide

L'ordre de pénibilité est un **rang** ; l'addition exige une **échelle**. Savoir qu'une heure de
nuit est plus pénible qu'une heure de bureau ne dit pas si elle en vaut 1,5 ou 3 — et ce choix-là
décide qui, de Paul ou de Sophie, sera jugé le plus sollicité. Aucune valeur *a priori* n'est
défendable, d'où l'arbitrage §4.2 du cadrage : **les coefficients se calibrent, ils ne se
décrètent pas.**

Sans instrument, « calibrer » veut dire deviner deux fois : une fois la valeur, une fois la
justification. Le harnais existe pour éviter cela.

### 1.1 Il ne répond pas à la question — il la rend répondable

> Le harnais ne dit pas ce que vaut une heure de nuit.
> Il dit : **sur ce planning, l'ordre entre ces deux personnes change à 1,375.**

C'est un déplacement de question, et c'est tout l'intérêt. Personne ne sait répondre à « combien
vaut une heure de nuit ». Un responsable d'exploitation sait répondre à « entre Paul, qui a fait
deux nuits, et Sophie, qui a fait un dimanche et un férié, lequel a été le plus sollicité ? ».
La valeur du coefficient se déduit de la réponse, au lieu de la précéder.

---

## 2. Comment s'en servir

| | |
|---|---|
| Le harnais | `src/test/java/fr/project/planning/equite/calibration/` |
| Le lancer | `gradlew test --tests "*HarnaisDeCalibrationTest*"` |
| Le rapport | `build/rapports/calibration-penibilite.markdown`, réécrit à chaque exécution |

Le harnais est **du côté des tests, et non du moteur**. La calibration est une activité d'équipe
moteur menée sur des exports réels, pas un service rendu à l'appelant : rien ne justifierait
d'embarquer dans le livrable un code qu'aucun runtime n'appelle.

### 2.1 Calibrer sur des cas réels

1. Déposer les demandes réelles — des exports WinDev — dans `src/test/resources/scenarios/sc03/`.
2. Lancer le harnais. Il résout chaque cas, en lit la restitution, et produit le rapport.
3. Lire les **points de bascule** de chaque coefficient.
4. Pour chaque bascule, poser au métier la question qu'elle formule : *lequel des deux vous paraît
   le plus sollicité ?*
5. La réponse situe le coefficient d'un côté ou de l'autre de la bascule. Plusieurs cas resserrent
   l'intervalle ; c'est ce resserrement qui est la calibration.

Il n'y a pas de valeur au bout de l'étape 3. Il y en a une au bout de l'étape 5, et elle est
défendable parce qu'elle est **dérivée d'arbitrages réels** plutôt que choisie.

### 2.2 Ce qu'un cas doit réunir pour servir

| Condition | Sinon |
|---|---|
| Au moins **deux personnes comparables** | L'équité est une question sur un groupe. Le rapport le signale : `PERSONNE_A_COMPARER` |
| Des **contrats déclarés** (`contrat.heuresHebdomadairesHabituelles`) | Rien n'est comparable, et le moteur ne restitue aucun écart |
| Des **pénibilités contrastées** | Toutes les échelles donnent le même classement. Le rapport le signale : `AUCUNE_PENIBILITE` |
| Une **fenêtre large** | Un écart au contrat lu sur trois jours n'est pas faux, il est faible |

> ⚠️ **Un cas muet n'est pas un cas neutre.** Une absence de bascule ne veut jamais dire que les
> coefficients sont indifférents ; elle veut dire que ce cas-là n'en dit rien. Le rapport distingue
> les deux mutismes parce qu'ils ne se corrigent pas de la même façon : l'un demande d'élargir le
> jeu transmis, l'autre de choisir une période qui contienne des nuits et des dimanches.

### 2.3 Ce que les jeux du dépôt permettent — c'est-à-dire rien

Les jeux d'essai du dépôt sont des cas de démonstration. Ils ont deux ou trois créneaux, un ou
deux salariés, et sont construits pour éprouver une règle précise. Le rapport les traverse et les
déclare muets ou quasi muets : **la calibration ne peut pas commencer avant que des plannings
réels ne soient versés.** C'est la conclusion du lot L3, pas une réserve sur son résultat.

---

## 3. La condition de cohérence

> **Les coefficients doivent décroître le long de l'ordre de dominance.**

Deux règles se rencontrent, et elles ne viennent pas du même endroit :

* la **dominance** décide à quelle catégorie appartient une minute qui en cumule plusieurs —
  `NUIT > DIMANCHE > FERIE` ;
* les **coefficients** décident ce que cette catégorie pèse.

Rien ne les oblige à s'accorder. Quand elles se contredisent, le résultat n'est pas faux, il est
absurde : avec `dimanche = 1,2` et `ferie = 2,0`, une minute travaillée un **dimanche férié** est
attribuée à DIMANCHE et pèse 1,2, quand la même minute un férié ordinaire pèse 2,0. **Cumuler deux
pénibilités allège l'heure au lieu de l'alourdir.**

Le moteur ne refuse pas pour autant — l'échelle décrit quelque chose, elle se contredit seulement
elle-même. Il lève `COEFFICIENTS_PENIBILITE_INCOHERENTS` en `WARNING`, en nommant le couple fautif,
et produit la mesure. C'est à l'appelant de trancher entre changer ses coefficients et changer son
ordre de dominance.

---

## 4. L'échelle que le moteur portait déjà

Le cadrage §4.2 mettait en garde : *ne pas créer une seconde échelle qui contredise la première.*
Confrontation faite, voici ce qu'elle donne.

### 4.1 Elle existe, et elle classe la nuit en dernier

`ScoreWeights` pondère chaque minute de pénibilité, après application de la dominance :

| Stratégie | NUIT | DIMANCHE | FERIE |
|---|---:|---:|---:|
| `EXPLOITATION` | 3 | 4 | 5 |
| `AUDIT` | 2 | 2 | 2 |
| `ANALYSE_RH` | 1 | 1 | 1 |

En `EXPLOITATION`, **une minute de nuit est la moins chère des trois** — l'inverse de l'ordre de
pénibilité du métier, pour lequel la nuit est ce qu'il y a de plus lourd.

### 4.2 Les deux tiennent ensemble, mais pour des raisons opposées

`40_STRATEGIE_DE_SCORING.md` §4.1.1 énonce le principe qui gouverne la dominance côté score : *la
pénalité retenue est celle correspondant à la situation la plus favorable au salarié.* Avec 3/4/5,
placer NUIT en tête revient bien à retenir la moins chère. Le score est cohérent.

L'équité lit exactement la même liste, et y prend la pénibilité **la plus lourde** — il le faut,
sinon le cumul devient un avantage (§3).

| | Ce que la dominance signifie | Condition de cohérence |
|---|---|---|
| **Score** | attribuer la minute à la catégorie la **moins pénalisée** | poids **croissants** le long de l'ordre |
| **Équité** | attribuer la minute à la pénibilité la **plus lourde** | coefficients **décroissants** le long de l'ordre |

Les deux lectures sont satisfaites aujourd'hui — mais seulement parce que les deux échelles sont
**inversées l'une par rapport à l'autre**. C'est une coïncidence entretenue, pas une propriété :
réordonner la liste de dominance pour servir l'une casserait l'autre, sans que rien ne le signale.
Depuis le lot L1, les deux partagent en outre la même implémentation (`RepartitionPenibilites`),
ce qui rend le couplage réel et non théorique.

`CoherenceEchelleTest` verrouille les deux moitiés de la condition. C'est ce test qui parlera le
jour où quelqu'un touchera à l'ordre.

### 4.3 Ce que le cadrage demandait de trancher

> *« Soit les coefficients d'équité s'y adossent, soit l'écart est documenté et voulu. »*

**S'y adosser est impossible.** Adopter 3 : 4 : 5 normalisé donnerait à la nuit le coefficient le
plus faible, ce qui contredit l'arbitrage métier et viole la condition de cohérence du §3.
L'échelle d'équité est donc **distincte et inversée** par rapport à celle des pénalités.

> ✅ **Tranché par le métier le 2026-08-13.** Le score conserve sa lecture — *la situation la plus
> favorable au salarié*, donc la catégorie la moins chère — et **rien n'y est modifié**.
>
> Sur un même planning, le moteur pénalise donc le moins la nuit tout en la mesurant comme la plus
> lourde. Ce n'est pas une contradiction : les deux répondent à des questions différentes — *jusqu'où
> a-t-on le droit d'aller* contre *qui a été le plus sollicité*. L'écart est **voulu**, et
> `CoherenceEchelleTest` garde les deux moitiés de la condition qui le rend tenable.

---

## 5. Comment le harnais s'y prend

### 5.1 Une résolution suffit pour toutes les échelles

La répartition d'une minute entre catégories est une propriété du **planning**, pas du choix des
coefficients : la rejouer donnerait chaque fois le même résultat. Ce que l'échelle change, c'est
uniquement ce que ces minutes **pèsent**. Une seule résolution permet donc d'évaluer autant
d'échelles qu'on veut.

> ⚠️ **Cette licence a été mise à l'épreuve au lot L5, et elle tient — sous condition.** La
> contrainte SOFT d'équité lit les coefficients, ce qui a fait échouer le garde-fou posé ici,
> exactement comme il l'annonçait. Elle **ne pèse rien tant qu'aucune tolérance n'est transmise**,
> et une demande de calibration n'en transmet pas : on calibre la mesure, pas la sanction. Sur ces
> demandes-là, le planning ne dépend toujours pas de l'échelle.
>
> **Corollaire à tenir** : calibrer sur une demande portant `planningContext.equite` serait faux —
> le planning changerait avec l'échelle, et rejouer la seule pondération mesurerait un planning que
> le moteur n'aurait pas produit. `HarnaisDeCalibrationTest` garde la liste des contraintes
> autorisées à lire les coefficients ; toute nouvelle entrée doit être justifiée de la même façon.

### 5.2 Les bascules se calculent, elles ne se cherchent pas

Balayer une grille — 1,0 puis 1,5 puis 2,0 — ne dit jamais *où* l'ordre a changé, seulement qu'il a
changé entre deux essais. Or à répartition fixée, l'écart au contrat est **affine** en chaque
coefficient :

```text
écart(c) = (K + c × m) / A − 1
```

où `m` est le volume de la catégorie qui varie, `K` le reste pondéré, `A` le volume contractuel
attendu. Deux personnes forment donc deux droites, et leur intersection est une **valeur exacte** —
celle où le classement s'inverse. Deux droites parallèles ne se croisent jamais : ce
coefficient-là ne départage pas ces deux personnes, quelle que soit sa valeur, et c'est une réponse
au même titre.

### 5.3 Le harnais lit ce que le moteur publie

Il aurait pu appeler le calcul interne. Il passe par la réponse HTTP, ce qui vérifie au passage que
**ce que le moteur publie suffit à calibrer** : `partNuits`, `partDimanches`, `partFeries` et
`heuresTravaillees` redonnent la répartition, `contrat.heuresHebdomadairesHabituelles` et l'horizon
redonnent la référence. Si la restitution cessait de porter de quoi la reconstituer, la lecture
échouerait ici plutôt que de calibrer sur des chiffres que personne d'autre ne peut voir.

La pondération, elle, n'est pas réécrite : harnais et moteur appellent la **même** méthode. Un
harnais qui pondère à sa façon calibre quelque chose que le moteur ne calcule pas, et le résultat
en a exactement la même allure — c'est le genre d'écart qui ne se voit jamais.

---

## 6. Limites connues

| Limite | Portée |
|---|---|
| **Les jeux du dépôt ne calibrent rien** | Ce sont des cas de démonstration. Des exports réels sont nécessaires (§2.3) |
| **Une résolution par cas, pas par échelle** | Valable **si la demande ne porte pas `planningContext.equite`** (§5.1) |
| **Arrondi des parts au centième de pourcent** | La reconstitution redonne les minutes exactes sur une fenêtre de l'ordre de la semaine ; sur une fenêtre très longue, l'erreur grandirait |
| **La bascule dépend du cas** | Elle décrit ce planning-là. Un intervalle défendable demande plusieurs cas, pas un seul |
| **Le volontariat n'est pas connu** | Certains veulent ces heures. Le moteur ne le saura pas avant le rang 10 du backlog — voir §9.3 du cadrage |
