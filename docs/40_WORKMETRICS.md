# 📊 WorkMetrics — Définition partagée

Ce document définit **WorkMetrics**, l’ensemble des indicateurs dérivés utilisés par le moteur
pour **évaluer** une solution (scoring), sans jamais devenir des décisions.

---

## 1. Rôle

* **Nature** : agrégats dérivés des affectations
* **Usage** : 
  - explicabilité
  - scoring (uniquement pour certains compteurs de pénibilité)
  - restitution / reporting (compteurs de travail agrégés)

Les compteurs de “travail” (ex. heures travaillées par jour/semaine/mois, par lieu, par activité) sont strictement destinés à la restitution et ne participent pas à l’arbitrage.

> Les WorkMetrics sont calculées après résolution et ne participent pas à l’évaluation du planning.

-- 

## 1.1 Dépendance au référentiel métier

Les métriques de travail ne déduisent jamais les effets métier
directement à partir des créneaux.

Toute interprétation (dette repos, charge, criticité)
passe par `ReferentielComptabiliteActivite`
et ses `ComptabiliteActivite`.

---

### 1.1.1 Identification d’activité (contrat d’entrée)

Les créneaux transportent **deux informations distinctes** sur l’activité :

- `codeActiviteId` : **identifiant d’activité du logiciel de planning** (ex. `10101`, `101478`).  
  C’est la **clé de jointure** utilisée pour retrouver la `ComptabiliteActivite` dans le `ReferentielComptabiliteActivite`.
- `activite` : **libellé** (affichage / lisibilité), non utilisé comme clé.

En intégration, le logiciel de planning est responsable de fournir un référentiel dont les clés correspondent à `codeActiviteId`.
Le moteur ne déduit pas ces correspondances à partir du libellé.

### 1.1.2 Champs déductibles vs champs optionnels

Certaines propriétés de `ComptabiliteActivite` peuvent être **déduites automatiquement** côté logiciel de planning (selon sous-type d’activité) :
- `compteDansCharge`
- `genereDetteRepos` (ex. sous-type « Récupérable »)

D’autres restent des **choix client** et peuvent être absentes dans de nombreux scénarios :
- `estServiceCritique`
- `prioritaireSurConfort`

Dans ce cas, la valeur par défaut doit être considérée comme `false` (absence de sur-critère).

### 1.1.3 Mode dev (Option B) : référentiel absent ou incomplet

En phase de développement, le moteur **tolère** un référentiel absent/incomplet :
- si l’activité n’est pas trouvée dans le référentiel, le créneau est traité comme **neutre** (aucun compteur),
- un diagnostic de synthèse est affiché (compteurs : hors horizon / sans ressource / activité inconnue).

⚠️ En production, l’intégration doit viser un référentiel complet pour éviter des métriques à zéro « silencieuses ».

### 1.2 Clarification à renforcer

WorkMetrics sont des constats post-résolution.
Elles ne participent :
- ni à la faisabilité,
- ni aux décisions,
- ni à l’interdiction d’une solution.

Elles décrivent ce que la solution produit, indépendamment du fait qu’elle soit légale, acceptable ou non du point de vue métier.

---

### 1.3 Alignement HARD / SOFT

❌ Aucune métrique ne correspond directement à une contrainte HARD
✅ Une contrainte HARD peut expliquer a posteriori une valeur de métrique (ex. séquence observée)
❌ Une métrique ne déclenche jamais une exclusion

Exemple :
maxNuitsConsecutivesObservees = 6
→ ce n’est pas la métrique qui invalide la solution,
→ c’est la contrainte HARD NuitsConsecutivesMax qui l’interdit.

---

### 1.4. Définition du repos hebdomadaire
- RH = repos hebdomadaire du samedi (nature REPOS)
- RHD = repos hebdomadaire du dimanche (nature REPOS)

Ces codes représentent un repos attendu, pas du travail.

**[S7.9b] Mise en œuvre.** L'identifiant d'activité correspondant change d'un client à l'autre :
il est déclaré au contrat, dans `dataSet.referentiels.codeActiviteReposHebdomadaire` et
`codeActiviteReposHebdomadaireDimanche`. Le moteur ne connaît aucun code en dur.

Un créneau porteur de ce code, avec son `ressourceAffecteeId`, dit **quel jour ce salarié-là se
repose** — n'importe quel jour de la semaine. Il est retiré du problème et restitué tel quel :
un repos n'est ni un besoin à pourvoir, ni de la charge.

À défaut de marqueur, le repli s'applique **par salarié et par semaine** : samedi vaut RH et
dimanche vaut RHD. Une semaine sans marqueur retombe sur le repli même si le salarié en déclare
ailleurs — une semaine oubliée ne doit jamais devenir silencieusement travaillable.

### 1.5. Définitions dérivées
- Dimanche travaillé
Dimanche travaillé Un dimanche travaillé est un dimanche calendaire (DayOfWeek.SUNDAY) comportant au moins un créneau dont l’activité compte dans la charge.

- Repos hebdomadaire travaillé
**[S7.9c]** Minutes de créneaux dont l'activité compte dans la charge positionnées un **dimanche
calendaire** (`DayOfWeek.SUNDAY`). Le samedi n'y entre plus, et le calendrier de repos individuel
non plus : c'est un indicateur d'observation, qui doit rester comparable entre salariés et entre
clients y compris quand aucun repos n'est déclaré. Un dimanche est un fait de calendrier ; le
repos d'une personne est une déclaration. Même maille que `nbDimanchesTravailles` — l'un compte
les heures, l'autre les jours.

- Dette de repos hebdomadaire (`nbCreneauxReposHebdoDetteRepos`)
**[S7.9c]** Jours où le salarié a travaillé **son** jour de repos, au sens du calendrier
individuel, avec une activité ouvrant une dette compensatoire (`genereDetteRepos`). Contrepartie
observée de la contrainte `DetteReposSurReposHebdomadaire` : les deux lisent le même calendrier,
sans quoi la métrique annoncerait une règle que le score n'applique pas. Ce repos peut tomber
n'importe quel jour de la semaine, d'où la divergence assumée avec l'indicateur ci-dessus.

## 2. Calcul des pénibilités temporelles

Les pénibilités liées au temps de travail (nuit, dimanche, jour férié) sont calculées à partir de l’intersection réelle des créneaux avec les intervalles réglementaires.

Le calcul est basé sur l’intersection réelle des créneaux avec les intervalles réglementaires (nuit, dimanche, jour férié).

> Ces calculs d’intersection utilisent `heureDebut`/`heureFin` — c’est un usage technique légitime.
> Ils sont distincts de `heuresTravaillees` (§4.2), qui utilise exclusivement `Creneau.duree`.
> Voir la règle de source de vérité dans `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`.

Pour chaque créneau, le moteur calcule :

| Métrique	                     | Description                                |
| ------------------------------ | ------------------------------------------ |
| `minutesTravaillees`	         | durée totale du créneau (via intersection) |
| `minutesNuit`	                 | minutes situées dans l’intervalle de nuit  |
| `minutesDimanche`	             | minutes situées un dimanche                |
| `minutesFerie`                 | minutes situées un jour férié              |
| `minutesNuitEtDimanche`	       | intersection nuit + dimanche               |
| `minutesNuitEtFerie`	         | intersection nuit + férié                  |
| `minutesDimancheEtFerie`	     | intersection dimanche + férié              |
| `minutesNuitEtDimancheEtFerie` | triple intersection                        |

Les volumes calculés par TimeBreakdownCalculator constituent
des **mesures élémentaires de pénibilité temporelle**.

Ces mesures sont utilisées :
- par certaines contraintes pour le calcul du score
- par le calcul des WorkMetrics pour produire des indicateurs descriptifs après résolution.

Ces volumes ne sont **pas des WorkMetrics en eux-mêmes**.

Ils représentent uniquement des **primitives de mesure**
à partir desquelles les contraintes et les WorkMetrics peuvent effectuer leurs calculs.

### Principe de dominance

Les intersections multiples ne produisent pas de double pénalité.

Un ordre de dominance paramétrable est appliqué : NUIT > DIMANCHE > FERIE (par défaut)

Les minutes appartenant à plusieurs catégories sont attribuées à la pénibilité dominante.

---

### Séquences observées

Les métriques `maxJoursConsecutifsObservees` et `maxNuitsConsecutivesObservees` sont calculées par `WorkMetricsCalculator` après résolution.

Leur définition détaillée est en section 5.1.

---

## 3. Portée temporelle

Chaque instance de WorkMetrics est **liée à :**

* une **ressource** (salarié réel ou ressource virtuelle agrégée),
* une **période** (issue du `PlanningContext`),
* un **type de résolution** (planning global, cycle, remplacement).

---

## 4. Champs retenus (V3-A socle)

### 4.1 Identification

| Champ            | Type | Description                                         |
| ---------------- | ---- | --------------------------------------------------- |
| `resourceId`     | UUID | Salarié réel ou identifiant agrégé                  |
| `periodeDebut`   | Date | Début de période                                    |
| `periodeFin`     | Date | Fin de période                                      |
| `resolutionType` | Enum | PLANNING_GLOBAL / CYCLE / REMPLACEMENT / PROJECTION |

---

### 4.2 Charges horaires

| Champ                       | Type    | Description                                                                | Implémenté |
| --------------------------- | ------- | -------------------------------------------------------------------------- | -----------|
| `heuresTravaillees`         | Decimal | Total heures affectées — calculé à partir de `Creneau.duree` (durée stockée) |      V1    |
| `heuresNuit`                | Decimal | Heures en plage de nuit (intersection `heureDebut`/`heureFin` × plage nuit) |      V1    |
| `heuresJourFerie`           | Decimal | Heures sur jours fériés (intersection). **[S7.9]** Valait 0.0 pour tous jusqu'à ce lot : le calendrier de `RegulatoryParameters` n'était jamais alimenté. Il l'est désormais depuis `holidayDates` (SC-01) ou `isJourFerie` (SC-03, SC-06). |      V1    |
| `heuresReposHebdoTravaille` | Decimal | Travail sur repos hebdomadaire                                             |      V1    |

---

### 4.3 Indicateurs liés au référentiel contractuel (cible)

| Champ                            | Type    | Description                                                  | Implémenté |
| -------------------------------- | ------- | ------------------------------------------------------------ | ---------- |
| `nbDimanchesTravailles`          | Integer | Nombre de dimanches calendaires travaillés                   |     V2     |
| `nbCreneauxReposHebdoDetteRepos` | Integer | Nombre de créneaux sur repos hebdomadaire générant une dette |     V2     |

⚠️ Cette section décrit des métriques cibles.
Elles ne sont pas implémentées à ce stade du moteur et
nécessitent une définition préalable du temps contractuel côté métier.

| `heuresSupplementaires`          | Decimal | Heures au‑delà du contrat                                    |            |
| `heuresComplementaires`          | Decimal | Heures complémentaires (temps partiel)                       |            |
| `depassementContingentHS`        | Decimal | Heures au‑delà du contingent                                 |            |

---

### 4.4 Dettes et coûts (cible future)

Ces métriques ne seront introduites qu’après stabilisation :
– des WorkMetrics de base
– de la stratégie de scoring
– et de l’analyse métier aval.

| Champ                    | Type    | Description                         |
| ------------------------ | ------- | ----------------------------------- |
| `detteReposCompensateur` | Decimal | Heures de repos à récupérer         |
| `detteReposNuit`         | Decimal | Part liée au travail de nuit        |
| `detteReposFerie`        | Decimal | Part liée aux jours fériés          |
| `coutDirect`             | Decimal | Coût payé (abstrait, non financier) |
| `coutIndirect`           | Decimal | Coût différé (repos, fatigue)       |

---

## Principes de conception des WorkMetrics

Cette section décrit les principes de conception des WorkMetrics
utilisées par le moteur de planification.

L’état d’avancement de leur implémentation est suivi dans le tableau récapitulatif en début de document, qui constitue la source de vérité.

Chaque groupe dépend explicitement de briques préalables du moteur (contraintes, scoring, référentiels).

Aucune de ces métriques n’est interprétative : elles restent descriptives.

---

### 5.1 Séquences observées (V3-B – priorité haute)

Ces métriques accompagnent directement les contraintes combinatoires légales
(nuits consécutives, jours consécutifs).

| Champ                              | Type    | Description                                                |
| ---------------------------------- | ------- | ---------------------------------------------------------- |
| `maxNuitsConsecutivesObservees`    | Integer | Longueur maximale observée de nuits consécutives           |
| `maxJoursConsecutifsObservees`     | Integer | Longueur maximale observée de jours travaillés consécutifs |

**Pré-requis :**
- contraintes combinatoires HARD/ SOFT stabilisées
- horizon temporel cohérent

**Objectif :**
- explicabilité du respect (ou non) des seuils
- comparaison de solutions

Ces métriques ont un rôle strictement descriptif :
- elles sont post-résolution ;
- elles n’interviennent pas dans la faisabilité ;
- elles ne déclenchent aucune contrainte HARD ;
- elles ne produisent aucune pénalité directe ;
- elles ne constituent jamais un seuil d’invalidation.

Elles servent exclusivement :
- à l’explicabilité,
- à la comparaison de solutions,
- à la préparation d’analyses aval (RH, audit).

---

#### 5.1.1 Définition canonique du travail

La définition canonique du travail est un invariant d’architecture défini dans :

> `20_DECISIONS_CONCEPTION_OPTAPLANNER.md §5`

Se référer à ce document pour la définition faisant autorité, les mappings `Nature → compteDansCharge`,
et la règle de cohérence transversale.

**Application aux WorkMetrics :**

Un créneau contribue aux WorkMetrics si et seulement si `compteDansCharge = true` dans le référentiel d’activité.
Toute minute issue d’un calcul d’intersection est ignorée si l’activité ne compte pas dans la charge.

> Les primitives de calcul (volumes d’intersection, dominance) sont définies en section 2 du présent document.
> L’ordre de dominance est fourni par le `PlanningContext`.

---

#### 5.1.2 maxJoursConsecutifsObservees

**Définition :**
Représente la longueur maximale d’une séquence de jours calendaires consécutifs travaillés pour une ressource donnée.

**Méthode de calcul :**

Pour chaque ressource :
1. Identifier les dates de l’horizon comportant au moins un créneau tel que :
    compteDansCharge = true
2. Dédupliquer par date (plusieurs créneaux le même jour comptent pour 1).
3. Trier les dates.
4. Calculer la plus longue suite de dates consécutives.

**Cas limites**
- Aucune journée travaillée → valeur = 0
- Créneaux hors horizon → ignorés
- Activité absente du référentiel → ignorée
- Plusieurs créneaux le même jour → 1 seul jour

---

### 5.1.3 maxNuitsConsecutivesObservees

#### Définition

Nombre maximal de **nuits travaillées consécutivement** par un salarié
sur l’horizon analysé.

Une nuit est considérée comme travaillée si **au moins un créneau affecté au salarié satisfait simultanément les deux conditions** :

- le créneau appartient à une **plage horaire de type NUIT** ;
- l’activité associée **compte dans la charge de travail**
  (`compteDansCharge = true` dans le référentiel d’activité).

L’indicateur est exprimé **en nombre de nuits calendaires consécutives**.

---

#### Méthode de calcul

1. Sélectionner les créneaux :
   - affectés au salarié,
   - appartenant à une plage horaire `NUIT`,
   - dont l’activité `compteDansCharge = true`.

2. Extraire la **date calendaire** de chaque créneau.

3. Construire l’ensemble des **dates distinctes triées**.

4. Parcourir ces dates pour déterminer la **plus longue séquence de jours consécutifs**.

5. La valeur maximale observée correspond à : `maxNuitsConsecutivesObservees`

#### Exemple

Créneaux de nuit observés :

| Date  | Nuit travaillée |
|-------|-----------------|
| 03/03 | oui             |
| 04/03 | oui             |
| 05/03 | oui             |
| 07/03 | oui             |

Séquences détectées :
03/03 → 04/03 → 05/03 = 3 nuits
07/03 = 1 nuit

#### Remarques

- L’indicateur est **indépendant du scoring**.
- Il sert :
  - à l’analyse RH,
  - au diagnostic de pénibilité,
  - à la vérification des contraintes réglementaires.

La contrainte correspondante dans le moteur (`NuitsConsecutivesMax`) compare cette valeur au seuil **du salarié**, `contraintesReglementaires.nuitsConsecutivesMaximum`, transporté par le contrat d'entrée. Elle lisait auparavant un seuil global dans le `PlanningContext` ; ce seuil n'était jamais alimenté et a été retiré au lot S7.8.

---

#### 5.1.4 Alignement HARD / SOFT

Ces métriques :
- peuvent expliquer a posteriori une violation de contrainte combinatoire ;
- ne remplacent jamais la contrainte ;
- ne déclenchent jamais d’exclusion.

Exemple : maxNuitsConsecutivesObservees = 6

Ce n’est pas la métrique qui invalide la solution.
C’est la contrainte combinatoire correspondante (HARD ou SOFT) qui s’applique indépendamment.

---

### 5.1.5 Nature des WorkMetrics : individuelles vs comparatives

Les WorkMetrics peuvent être de deux natures différentes.

#### A. Métriques individuelles

La majorité des WorkMetrics décrivent l’activité d’une ressource indépendamment des autres.

Exemples :
- heuresTravaillees
- heuresNuit
- heuresJourFerie
- nbDimanchesTravailles
- maxJoursConsecutifsObservees
- maxNuitsConsecutivesObservees

Ces métriques sont calculées **ressource par ressource** à partir des créneaux affectés.

#### B. Métriques comparatives

Certaines métriques nécessitent une **comparaison entre ressources**.

Exemples :
- `ecartContratPourcent` — l'écart signé au contrat, voir §5.2

Ces métriques ne peuvent être calculées qu'après avoir calculé les métriques individuelles de toutes les ressources.

Elles doivent donc être produites dans **une seconde phase d’agrégation**, et non pendant le calcul des métriques individuelles.

#### Principe de calcul

Le calcul des WorkMetrics doit donc suivre deux étapes :
1. calcul des métriques individuelles par ressource ;
2. calcul éventuel de métriques comparatives sur l’ensemble
   des ressources.

Cette séparation garantit :
- la simplicité du calcul,
- la lisibilité de l’architecture,
- et la possibilité d’introduire des métriques d’équité sans complexifier les calculs existants.

---

### 5.2 Répartition et équité — livrée aux lots L1 et L2 du chantier équité

Ces métriques permettent une lecture **comparative**, sans décision.

> ⚠️ **Ce paragraphe annonçait un écart à la moyenne du groupe. L'arbitrage métier du 13/08/2026
> l'a écarté** : la référence est le **contrat de chacun**. Comparer à la moyenne collective en
> heures brutes mettrait un salarié à 50 % perpétuellement « sous la moyenne », et le moteur
> passerait son temps à vouloir le charger. `ecartChargeAvecMoyenne` et `ecartNuitsAvecMoyenne`
> **ne seront pas écrits** ; ils sont remplacés par les champs ci-dessous.
> Voir `92_CADRAGE_WORKMETRICS_EQUITE.md` §4.1 et §5.2.

| Champ | Type | Description |
| --- | --- | --- |
| `heuresPonderees` | Decimal | Charge ramenée à l'unité de l'heure ordinaire, chaque minute pondérée par le coefficient de **sa seule** catégorie de pénibilité — celle que la dominance retient |
| `ecartContratPourcent` | Decimal | Écart **signé** au volume contractuel attendu sur la fenêtre. Négatif : en dessous, ce qui doit rendre la personne **préférable** |
| `partNuits`, `partDimanches`, `partFeries` | Decimal | Part de chaque pénibilité rapportée à ce même volume — un mi-temps doit une part proportionnée à son contrat |
| `joursObserves` | Integer | Jours de l'horizon déclaré **où ce salarié était disponible** : le dénominateur, restitué pour que l'appelant sache sur quoi le moteur a jugé |

Ces mesures sont produites dans la **seconde phase d'agrégation** décrite au §5.1.5 : rapporter une
charge au contrat suppose que tout ait été compté. Elles valent `null` sans volume contractuel
déclaré — un poste virtuel est dans ce cas par nature.

> ⚠️ **Le dénominateur est propre à chacun — les absences en sont déduites (rang 14).** Le moteur ne
> place aucun créneau dans un congé : la contrainte HARD `METIER_HARD_INDISPONIBILITE` y veille
> depuis l'origine. Mais proratiser le contrat sur l'horizon **entier** ferait lire l'absence comme
> du *temps disponible non travaillé* : le salarié revenant de congé apparaîtrait sous son contrat,
> donc préférable, et le moteur lui rattraperait son absence de part et d'autre. **On n'optimise pas
> un planning en annulant les congés**, et compenser une absence est une manière de l'annuler.
>
> Seule l'indisponibilité **déclarée** est déduite — le bloc `indisponibilites`, source unique de
> l'absence au contrat d'entrée. Un jour sans créneau n'est pas une absence : c'est précisément ce
> que l'équité doit voir. Les périodes qui se chevauchent ne comptent qu'une fois, et une absence
> débordant l'horizon n'est déduite que sur sa partie visible.
>
> Un salarié absent **toute** la fenêtre a `joursObserves: 0` et `ecartContratPourcent: null` : il
> n'est pas à −100 %, il est **hors de comparaison**. La même primitive alimente la mesure restituée
> et le classement de SC-06 — le score, la réponse et le rang disent la même chose.

**L'unité, et ce qu'elle suppose.** *On ne juge l'équité qu'à pénibilité équivalente* : huit heures
un mardi et huit heures un dimanche n'entrent pas dans la même addition sans coefficient. Ces
coefficients sont **transmis, jamais écrits en dur**, et se calibrent sur des plannings réels —
`92_CALIBRATION_PENIBILITE.md`. Absents, `heuresPonderees` vaut exactement `heuresTravaillees`.

**Mesurer n'est pas sanctionner.** Ces champs décrivent une répartition ; ils ne statuent pas
qu'elle est mauvaise. La pénalisation est une contrainte SOFT distincte, livrée séparément
(lot L5).

**Restitution :** `50_SCENARIO_RESPONSE_CONTRACT.md` §3.2 bis et §3.2 ter.

---

### 5.3 Référentiel contractuel (V4 – spécifique contexte français)

Ces métriques expriment un **écart relatif au temps contractuel de référence**, sans interprétation juridique.

| Champ                                 | Type    | Description                                                        |
| ------------------------------------- | ------- | ------------------------------------------------------------------ |
| `deltaMinutesParRapportAuContractuel` | Decimal | Écart entre minutes travaillées et temps contractuel de référence  |
| `ratioChargeContractuelle`            | Decimal | Rapport charge réelle / charge contractuelle                       |

**Pré-requis :**
- définition du temps contractuel côté métier (hors moteur)
- injection de cette information comme fait immuable

**Objectif :**
- rendre visibles les écarts
- préparer l’analyse métier (sans statuer sur la légalité)

---

### 5.4 Dettes et coûts abstraits (V5 – cible long terme)

Ces métriques représentent des **coûts abstraits**, non financiers, liés à la pénibilité et à la récupération.

| Champ                    | Type    | Description                  |
| ------------------------ | ------- | ---------------------------- |
| `detteReposCompensateur` | Decimal | Volume de repos à récupérer  |
| `detteReposNuit`         | Decimal | Part liée au travail de nuit |
| `detteReposFerie`        | Decimal | Part liée aux jours fériés   |

**Pré-requis :**
- WorkMetrics V3 complètes
- ScoreWeights stabilisés
- Analyse métier aval définie

**Objectif :**
- support à la restitution RH
- aide à la décision, hors moteur

---

## 6. Champs explicitement exclus

WorkMetrics **n’incluent pas** :

* rémunérations,
* primes,
* calculs de paie,
* valorisation financière réelle,
* gestion contractuelle fine.

Ces éléments relèvent du métier, hors moteur.

---

## 7. Calcul et mise à jour (principe)

Les WorkMetrics sont calculés à partir d’un planning résolu,
dans une phase dédiée de post-traitement.
Les contraintes n’écrivent pas les WorkMetrics.

---

## 8. Invariants

* Aucun champ de WorkMetrics n’est une décision
* Toute dette générée doit être traçable à des affectations
* Les indicateurs sont bornés à l’horizon transmis
* Le score doit pouvoir expliquer chaque champ
* Un créneau associé à une activité absente du référentiel est ignoré
  par l’ensemble des WorkMetrics.


---

## 9. Utilisation des mesures dans le scoring

Le moteur utilise des mesures élémentaires issues des créneaux pour évaluer certaines contraintes de pénibilité.

Ces mesures sont calculées à partir des affectations et représentent des volumes (minutes de nuit, dimanche, férié, etc.).

Elles servent exclusivement à alimenter le scoring.

Les WorkMetrics, en revanche, restent des agrégats descriptifs produits après résolution et ne participent pas à l’évaluation.

Cette séparation garantit l’absence de confusion entre :
- les éléments utilisés pour l’arbitrage,
- et les indicateurs utilisés pour la restitution.

---

## 10. Lien documentaire

WorkMetrics est référencé par :

* `30_UML_OPTAPLANNER.md`
* `40_STRATEGIE_DE_SCORING.md`
* `20_PLANNING_CONTEXT.md`

Il constitue la **référence unique** pour les indicateurs du moteur.
