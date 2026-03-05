# 📊 WorkMetrics — Définition partagée

Ce document définit **WorkMetrics**, l’ensemble des indicateurs dérivés utilisés par le moteur
pour **évaluer** une solution (scoring), sans jamais devenir des décisions.

---

## 0. Tableau de suivi

|Domaine                            |	Livré  | Où (code)                     |	Où (tests)        |	Doc                   |
| --------------------------------- |------- | ----------------------------- | ------------------ |---------------------- |
| Pénibilités minutes	              | 	✅   | TimeBreakdownCalculator       | tests existants    |	section 2            |
|                                   |        |   + PenibilitesLegalesMinutes |	                  |                       |
| Dominance	                        | 	✅   | ScoreUtils	                  | ScoreDominanceTest |	section 2            |
| Séquences (contraintes)           | 	✅   | ReposHebdomadaireMin/Glissant	| tests contraintes  |	REGLES_COMBINATOIRES |
| Séquences (WorkMetrics observées) | 	✅   | WorkMetricsCalculator	        | scénario 1         |  section 5.1          |
| Équité (WorkMetrics)              | 	❌   | –	                            | –	                 |  section 5.2          |
| Contractuel                       | 	❌   | –	                            | –	                 |  section 5.3          |
| Dettes & coûts abstraits          | 	❌   | –	                            | –	                 |  section 5.4          |

---

## 1. Rôle et statut

* **Statut** : `ProblemFact` (lu par le solveur, jamais modifié par lui)
* **Nature** : agrégats dérivés des affectations
* **Usage** : 
  - explicabilité
  - scoring (uniquement pour certains compteurs de pénibilité)
  - restitution / reporting (compteurs de travail agrégés)

Les compteurs de “travail” (ex. heures travaillées par jour/semaine/mois, par lieu, par activité) sont strictement destinés à la restitution et ne participent pas à l’arbitrage.

> WorkMetrics rendent visibles les **conséquences** des décisions
> (coûts, dettes, charges), pas les décisions elles‑mêmes.

> Les WorkMetrics sont calculées **après la résolution complète du planning**
> et ne sont jamais modifiées pendant l’exécution du solveur. 

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

## 2. Calcul des pénibilités temporelles

Les pénibilités liées au temps de travail (nuit, dimanche, jour férié) sont calculées à partir de l’intersection réelle des créneaux avec les intervalles réglementaires.

Le calcul est réalisé par : `TimeBreakdownCalculator`

Pour chaque créneau, le moteur calcule :

| Métrique	                     | Description                                |
| ------------------------------ | ------------------------------------------ |
| `minutesTravaillees`	         | durée totale du créneau                    |
| `minutesNuit`	                 | minutes situées dans l’intervalle de nuit  |
| `minutesDimanche`	             | minutes situées un dimanche                |
| `minutesFerie`                 | minutes situées un jour férié              |
| `minutesNuitEtDimanche`	       | intersection nuit + dimanche               |
| `minutesNuitEtFerie`	         | intersection nuit + férié                  |
| `minutesDimancheEtFerie`	     | intersection dimanche + férié              |
| `minutesNuitEtDimancheEtFerie` | triple intersection                        |

Les volumes calculés par TimeBreakdownCalculator constituent
des primitives utilisées :

- par les contraintes pour le scoring
- par certaines WorkMetrics pour l’explicabilité

Ces volumes ne sont pas des WorkMetrics en eux-mêmes.

Elles ne sont jamais utilisées directement pour modifier le planning.

### Principe de dominance

Les intersections multiples ne produisent pas de double pénalité.

Un ordre de dominance paramétrable est appliqué : NUIT > DIMANCHE > FERIE (par défaut)

Les minutes appartenant à plusieurs catégories sont attribuées à la pénibilité dominante.

---

### Séquences observées (V3)

Les séquences de travail sont désormais calculées à partir des créneaux affectés :

- `seqJours` : nombre de jours travaillés consécutifs
- `seqNuits` : nombre de nuits consécutives

Ces métriques sont calculées par `WorkMetricsCalculator`
après résolution du solveur.

Elles sont actuellement utilisées pour :
- explicabilité
- validation des scénarios
- préparation des futurs WorkMetrics d’équité

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

| Champ                       | Type    | Description                           | Implémenté |
| --------------------------- | ------- | ------------------------------------- | -----------|
| `heuresTravaillees`         | Decimal | Total heures affectées sur la période |      V1    |
| `heuresNuit`                | Decimal | Heures en plage de nuit               |      V1    |
| `heuresJourFerie`           | Decimal | Heures sur jours fériés               |      V1    |
| `heuresReposHebdoTravaille` | Decimal | Travail sur repos hebdomadaire        |      V1    |

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

### 4.5 État d’implémentation validé (V2)

Cette section décrit **exclusivement** les règles actuellement
implémentées et validées par les tests automatisés.

Elle ne remet pas en cause la définition générale de WorkMetrics.

#### 📊 Métriques calculées

##### Travail total

- Somme des durées de tous les créneaux valides.

#####  Travail de nuit

- Somme des minutes appartenant à la plage réglementaire de nuit,
calculées par intersection temporelle via `TimeBreakdownCalculator`.

#####  Travail les jours fériés

- Somme des minutes appartenant à un jour férié,
calculées par intersection temporelle via `TimeBreakdownCalculator`.

---

#### 🛌 Repos hebdomadaire travaillé

##### Définition

Un créneau qualifié `RH` ou `RHD` est considéré comme un **repos hebdomadaire non travaillé**.

##### Calcul

- La durée du créneau est ajoutée aux minutes de repos hebdomadaire travaillé.
- Une **dette de repos hebdomadaire** peut être générée selon l’activité.

##### Dette de repos

- La génération de dette est pilotée par le `ReferentielComptabiliteActivite`.
- La dette est comptabilisée **par jour distinct**, indépendamment du nombre de créneaux sur la journée.

---

#### 📆 Dimanches travaillés (V2)

- Un dimanche travaillé correspond à un créneau enregistré sur un dimanche calendaire dont l’activité compte dans la charge.
- Le comptage est effectué **par date distincte**.
- Plusieurs créneaux travaillés le même jour ne génèrent **qu’un seul dimanche travaillé**.

---

## Principes de conception des WorkMetrics

Cette section décrit les principes de conception des WorkMetrics
utilisées par le moteur de planification.

L’état d’avancement de leur implémentation est suivi dans le tableau
récapitulatif en début de document, qui constitue la source de vérité.

Chaque groupe dépend explicitement de briques préalables du moteur
(contraintes, scoring, référentiels).

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

#### 5.1.1 Principe de calcul sans découpage de créneaux

Le moteur ne découpe jamais les créneaux.
Lorsque qu’un créneau chevauche une frontière (plage de nuit, changement de jour, dimanche, férié…), les volumes sont calculés par intersection temporelle afin d’obtenir des minutes partielles.

Ces minutes partielles sont ensuite utilisées :
- par les contraintes (mesure),
- et par les WorkMetrics (constats).

Les volumes partiels utilisés par les contraintes et ceux utilisés par WorkMetrics doivent être strictement identiques (même algorithme d’intersection).

---

#### 5.1.2 Volumes d’intersection

Pour permettre un scoring maîtrisé en cas de chevauchements, le moteur calcule également :
- minutesNuitEtDimanche
- minutesNuitEtFerie

Ces volumes sont des mesures neutres issues de la même primitive d’intersection temporelle.

Ils servent :
- au scoring,
- à l’explicabilité.

L’ordre de dominance appliqué au scoring est fourni par le PlanningContext ; les volumes d’intersection restent disponibles à des fins d’explicabilité, indépendamment de l’ordre choisi.

---

#### 5.1.3 Définition canonique du travail

Un créneau contribue aux WorkMetrics si et seulement si son activité est considérée comme du travail au sens du moteur (compteDansCharge=true via référentiel).
Toute minute issue d’un calcul d’intersection est ignorée si l’activité ne compte pas dans la charge.

Une journée ou une nuit est considérée comme travaillée si et seulement si :
- le créneau associé possède une activité dont
- compteDansCharge = true dans le référentiel d’activité.

Le moteur ne déduit jamais le travail :
- du code d’activité brut,
- du type de créneau,
- d’un qualifiant calendaire.

Cette règle est un invariant d’architecture : DECISIONS_CONCEPTION_OPTAPLANNER

---

#### 5.1.4 maxJoursConsecutifsObservees

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

#### 5.1.5 maxNuitsConsecutivesObservees

**Définition :**
Représente la longueur maximale d’une séquence de nuits travaillées consécutives pour une ressource donnée.

**Qualification d’une nuit travaillée**
Une date est considérée comme contenant une “nuit travaillée” si la ressource a, sur cette date, un volume :
- minutesNuit > 0
où minutesNuit est calculé par intersection temporelle entre :
- l’intervalle réel du créneau,
- et les fenêtres “plage de nuit” définies par les paramètres réglementaires.

Ce volume est pris en compte uniquement si :
- l’activité du créneau compteDansCharge = true (référentiel d’activité).

TypePlageHoraire et segmentNuit peuvent exister comme indicateurs d’entrée, mais ne sont pas des sources de vérité suffisantes en présence de chevauchements.

**Méthode de calcul**
Pour chaque ressource :
1. Identifier les dates comportant au moins un créneau :
- de type NUIT
- et comptant dans la charge.
2. Dédupliquer par date.
3. Trier les dates.
4. Calculer la plus longue suite consécutive.

**Cas limites**
- Aucune nuit travaillée → valeur = 0
- Plusieurs créneaux de nuit le même jour → 1 seule nuit
- Nuit non comptée dans la charge → ignorée

---

#### 5.1.6 Alignement HARD / SOFT

Ces métriques :
- peuvent expliquer a posteriori une violation de contrainte combinatoire ;
- ne remplacent jamais la contrainte ;
- ne déclenchent jamais d’exclusion.

Exemple : maxNuitsConsecutivesObservees = 6

Ce n’est pas la métrique qui invalide la solution.
C’est la contrainte combinatoire correspondante (HARD ou SOFT) qui s’applique indépendamment.

---

### 5.2 Répartition et équité (V3-C – après stabilisation scoring)

Ces métriques permettent une lecture **comparative**, sans décision.

| Champ                       | Type    | Description                                                      |
| --------------------------- | ------- | ---------------------------------------------------------------- |
| `ecartChargeAvecMoyenne`    | Decimal | Écart absolu entre la charge du salarié et la moyenne collective |
| `ecartNuitsAvecMoyenne`     | Integer | Écart du nombre de nuits travaillées par rapport à la moyenne    |

**Pré-requis :**
- WorkMetrics V1 et V2 stabilisées
- stratégie de scoring (`ScoreWeights`) en place

**Objectif :**
- préparation des contraintes SOFT d’équité
- aide à la lecture RH ultérieure

---

### 5.3 Référentiel contractuel (V4 – spécifique contexte français)

Ces métriques expriment un **écart relatif au temps contractuel de référence**,
sans interprétation juridique.

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

Ces métriques représentent des **coûts abstraits**, non financiers,
liés à la pénibilité et à la récupération.

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

## 9. Utilisation dans le solveur

Les WorkMetrics sont utilisées par le moteur de planification lors de l’évaluation du score OptaPlanner.

Le calcul des métriques est déclenché pendant l’évaluation des contraintes afin de mesurer l'impact d'une affectation de créneau sur :
- les pénibilités légales (minutes de nuit, minutes fériées, etc.)
- les dominances de pénibilité
- les futures métriques d'équité et de séquences.

Le solveur utilise ces métriques pour comparer les solutions candidates et améliorer le score global.

L'intégration effective dans le solveur a été validée lors du branchement du scénario SC-01 (mars 2026), avec une amélioration observée du score :
- 0hard/-40000soft → 0hard/0soft

Les WorkMetrics constituent ainsi la base analytique du scoring métier du moteur de planification.

---

## 10. Lien documentaire

WorkMetrics est référencé par :

* `UML_Optaplanner.md`
* `STRATEGIE_SCORING.md`
* `HORIZON_TEMPOREL_ET_REGLEMENTAIRE.md`

Il constitue la **référence unique** pour les indicateurs du moteur.
