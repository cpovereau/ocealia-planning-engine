# 📊 WorkMetrics — Définition partagée

Ce document définit **WorkMetrics**, l’ensemble des indicateurs dérivés utilisés par le moteur
pour **évaluer** une solution (scoring), sans jamais devenir des décisions.

---

## 1. Rôle et statut

* **Statut** : `ProblemFact` (lu par le solveur, jamais modifié par lui)
* **Nature** : agrégats dérivés des affectations
* **Usage exclusif** : scoring et explicabilité

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

## 2. Portée temporelle

Chaque instance de WorkMetrics est **liée à :**

* une **ressource** (salarié réel ou ressource virtuelle agrégée),
* une **période** (issue du `PlanningContext`),
* un **type de résolution** (planning global, cycle, remplacement).

---

## 3. Champs retenus (socle)

### 3.1 Identification

| Champ            | Type | Description                                         |
| ---------------- | ---- | --------------------------------------------------- |
| `resourceId`     | UUID | Salarié réel ou identifiant agrégé                  |
| `periodeDebut`   | Date | Début de période                                    |
| `periodeFin`     | Date | Fin de période                                      |
| `resolutionType` | Enum | PLANNING_GLOBAL / CYCLE / REMPLACEMENT / PROJECTION |

---

### 3.2 Charges horaires

| Champ                       | Type    | Description                           | Implémenté |
| --------------------------- | ------- | ------------------------------------- | -----------|
| `heuresTravaillees`         | Decimal | Total heures affectées sur la période |      V1    |
| `heuresNuit`                | Decimal | Heures en plage de nuit               |      V1    |
| `heuresJourFerie`           | Decimal | Heures sur jours fériés               |      V1    |
| `heuresReposHebdoTravaille` | Decimal | Travail sur repos hebdomadaire        |      V1    |

---

### 3.3 Indicateurs liés au référentiel contractuel (cible)

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

### 3.4 Dettes et coûts (cible future)

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

### 3.4 État d’implémentation validé (V2)

Cette section décrit **exclusivement** les règles actuellement
implémentées et validées par les tests automatisés.

Elle ne remet pas en cause la définition générale de WorkMetrics.

#### 📊 Métriques calculées

##### Travail total

- Somme des durées de tous les créneaux valides.

#####  Travail de nuit

- Somme des durées des créneaux de type `NUIT`.

#####  Travail les jours fériés

- Somme des durées des créneaux qualifiés `FERIE`.

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

## 4. WorkMetrics à concevoir (roadmap)

Cette section décrit les métriques prévues, classées par **ordre logique d’introduction**.
Chaque groupe dépend explicitement de briques préalables du moteur
(contraintes, scoring, référentiels).

Aucune de ces métriques n’est interprétative : elles restent descriptives.

---

### 4.1 Séquences de travail (V3 – priorité haute)

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

Ces métriques servent exclusivement à l’explicabilité et à la comparaison de solutions,
et ne constituent jamais des seuils d’invalidation.

---

### 4.2 Répartition et équité (V3 – après stabilisation scoring)

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

### 4.3 Référentiel contractuel (V4 – spécifique contexte français)

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

### 4.4 Dettes et coûts abstraits (V5 – cible long terme)

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

## 5. Champs explicitement exclus

WorkMetrics **n’incluent pas** :

* rémunérations,
* primes,
* calculs de paie,
* valorisation financière réelle,
* gestion contractuelle fine.

Ces éléments relèvent du métier, hors moteur.

---

## 6. Calcul et mise à jour (principe)

Les WorkMetrics sont calculés à partir d’un planning résolu,
dans une phase dédiée de post-traitement.
Les contraintes n’écrivent pas les WorkMetrics.

---

## 7. Invariants

* Aucun champ de WorkMetrics n’est une décision
* Toute dette générée doit être traçable à des affectations
* Les indicateurs sont bornés à l’horizon transmis
* Le score doit pouvoir expliquer chaque champ
* Un créneau associé à une activité absente du référentiel est ignoré
  par l’ensemble des WorkMetrics.


---

## 8. Lien documentaire

WorkMetrics est référencé par :

* `UML_Optaplanner.md`
* `STRATEGIE_SCORING.md`
* `HORIZON_TEMPOREL_ET_REGLEMENTAIRE.md`

Il constitue la **référence unique** pour les indicateurs du moteur.
