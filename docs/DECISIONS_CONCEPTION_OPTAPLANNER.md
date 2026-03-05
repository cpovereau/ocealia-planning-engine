# Décisions de conception – Moteur de planification (OptaPlanner)

---

## 1. Objectif du moteur de planification

### Objectif principal

Le moteur de planification a pour objectif de **proposer une affectation cohérente de créneaux de travail à des ressources**, en tenant compte de contraintes multiples, parfois contradictoires.

Il doit :

* produire **une solution explicable**, pas seulement optimale mathématiquement ;
* accepter l’existence de **situations imparfaites** ;
* mettre en évidence les **manques de ressources** ou les **violations nécessaires**.

### Hors périmètre volontaire (à ce stade)

* calculs horaires précis (heures, durées, pauses) ;
* dates calendaires ;
* gestion fine des contrats ;
* performance et optimisation à grande échelle.

Ces éléments seront introduits **après stabilisation du modèle conceptuel**.

---

## 2. Référentiel d’activités – responsabilité et gouvernance

Le moteur de planification ne construit pas le référentiel métier des activités.

Le référentiel (ComptabiliteActivite) est fourni par le logiciel de planning amont.

### Principes

- `codeActiviteId` = idActivitePlanning (clé technique).
- `activite` = libellé d’affichage uniquement.

### Champs déductibles (non paramétrés dans le moteur)

Les propriétés suivantes sont déterminées automatiquement à partir du sous-type d’activité du logiciel amont :

- `compteDansCharge`
- `genereDetteRepos`

Le moteur ne décide pas ces valeurs.

### Champs paramétrables (choix client)

Les propriétés suivantes relèvent d’un choix métier client et peuvent être absentes :

- `estServiceCritique`
- `prioritaireSurConfort`

En l’absence de configuration, ces champs sont considérés à `false`.

### Mode développement

En environnement de développement :
- une activité inconnue du référentiel est tolérée,
- le créneau est considéré neutre,
- un diagnostic peut être émis.

En environnement cible (à définir V3) :
- un seuil ou un échec explicite pourra être activé.

---

## 3. Modèle conceptuel stabilisé

### Entités principales

* **Créneau**

  * Unité élémentaire de travail à affecter.
  * Porte la variable de décision `ressourceAffectee`.

* **Ressource** (concept abstrait)

  * **Salarié réel** : personne existante.
  * **Poste virtuel** : besoin non couvert / potentiel de recrutement.
  * **État A_AFFECTER** : absence d’affectation explicite (pas de `null`).

* **PlanningSolution**

  * Contient la liste des créneaux et des ressources.
  * Porte le score OptaPlanner.

* **PlanningContext** (prévu)

  * Objet de contexte décrivant l’objectif de la demande.
  * Permettra de configurer les poids et l’activation des contraintes.
  
* **Paramètres conventionnels**
  
  * RegulatoryParameters est porté par le contexte.
  * les règles de conversion (heures → repos / majorations) sont fournies par le métier (WebDev / paramétrage), le moteur ne fait que scorer / arbitrer.

---

## 4. Typologie des contraintes (décision structurante)

Les contraintes sont classées selon **leur nature métier**, indépendamment de leur implémentation technique.

### Catégories retenues

1. **Contraintes physiques**

   * Limites impossibles à dépasser (ex. 24h dans une journée).

2. **Contraintes légales**

   * Droit du travail, temps de repos, durées maximales.

3. **Contraintes métier**

   * Organisation, fonctionnement interne, règles de service.

4. **Contraintes personnelles**

   * Préférences individuelles, souhaits, confort.

👉 Cette classification est **invariante** et sert de base à toute évolution future.


### Seuils de tolérance

Certaines contraintes se basent sur des seuils de tolérance.
Les seuils de tolérance sont fournis par le logiciel de planning via PlanningContext. Le moteur ne définit pas de valeurs par défaut implicites : toute valeur absente ou incohérente doit être rejetée (ou remplacée par un défaut explicite documenté).

---

## 5. Invariant fondamental — Définition du travail

Cette définition constitue un invariant d’architecture. Toute règle, contrainte, métrique ou évolution future doit s’y conformer.

### 1. Définition du travail (règle unique)

Un créneau est considéré comme travaillé si et seulement si son activité compte dans la charge (compteDansCharge = true dans le référentiel d’activité).

Conséquences :
- Nature TRAVAIL → compteDansCharge = true
- Nature REPOS (RH, RHD) → false
- Nature FERIE (JF substitutif) → false
- Nature RECUP → false
- Nature NON_TRAVAILLE → false

👉 Le moteur ne déduit jamais le travail à partir du code d’activité brut.
👉 Il s’appuie uniquement sur le référentiel.

### 2. Définition du repos hebdomadaire
- RH = repos hebdomadaire du samedi (nature REPOS)
- RHD = repos hebdomadaire du dimanche (nature REPOS)

Ces codes représentent un repos attendu, pas du travail.

### 3. Définitions dérivées
- Dimanche travaillé
Dimanche travaillé Un dimanche travaillé est un dimanche calendaire (DayOfWeek.SUNDAY) comportant au moins un créneau dont l’activité compte dans la charge.

- Repos hebdomadaire travaillé
Minutes de créneaux dont l’activité compte dans la charge positionnées un samedi (Saturday) ou un dimanche (Sunday).

### 4. Principe structurant V2
- Les contraintes mesurent.
- Les ScoreWeights pondèrent.
- Les WorkMetrics constatent post-résolution.

Aucune métrique ne redéfinit la notion de travail.

📌 Cette définition devient la référence pour :
- les contraintes légales,
- les métriques V2,
- les futures métriques V3,
- et la restitution planning.

🔒 Règle de cohérence transversale
- Les contraintes n’utilisent jamais directement Nature pour déterminer le travail.
- Les WorkMetrics n’interprètent jamais QualificationJour comme du travail.
- Toute évolution V3 ou ultérieure devra s’appuyer exclusivement sur la définition canonique ci-dessus.

### 5. Décision — Gestion des chevauchements temporels sans découpage de créneaux (V3)

**Constat**

Dans les domaines comportant des horaires “frontières” (veille de nuit, astreintes, interventions), un même créneau peut chevaucher :
- la plage de nuit (ex. 22:00–06:00),
- un changement de jour calendaire (samedi → dimanche),
- un jour férié,
- un repos hebdomadaire attendu (RH/RHD selon la modélisation amont).

Dans ces cas, une qualification globale du créneau (ex. TypePlageHoraire = NUIT) est insuffisante :
- elle ne permet pas de comptabiliser les minutes de nuit de manière précise (ex. 18:00–23:00 contient 60 minutes de nuit),
- elle ne permet pas de distinguer “minutes sur dimanche” dans un créneau samedi 22:00 → dimanche 06:00.

**Décision**

Le moteur ne découpe jamais un créneau pour matérialiser des sous-segments (nuit/jour, samedi/dimanche, férié/non férié, etc.).
À la place, il calcule des volumes partiels de façon déterministe, par intersection temporelle entre :
- l’intervalle réel du créneau,
- et les fenêtres temporelles réglementaires/calendaires (plages de nuit, jours fériés, jour “dimanche”, etc.).

Le créneau reste un objet unique :
- stable pour le solveur,
- stable pour l’API,
- stable pour la restitution UI.

**Conséquences**

1. Aucune reconstitution n’est nécessaire en sortie : on ne fragmente rien.
2. Les contraintes consomment des mesures (minutes) plutôt qu’une qualification globale.
3. Les WorkMetrics consomment les mêmes volumes, en post-résolution, pour garantir la cohérence “mesure ↔ constats”.
4. Les attributs de type “qualifiant” (ex. TypePlageHoraire) peuvent subsister comme indicateurs, mais ne constituent pas une source de vérité suffisante en présence de chevauchements.

**Règle de cohérence**

Toute pénalité exprimée “en minutes” (nuit, dimanche, férié, etc.) doit être dérivée de ces volumes partiels calculés par intersection temporelle, et non d’une qualification globale.

**Dominance sur chevauchements (anti double-pondération)**

Une minute peut appartenir à plusieurs catégories (ex. nuit + dimanche).
Les volumes partiels sont calculés séparément à des fins de mesure et d’explicabilité.

Cependant, le scoring ne doit pas appliquer une “double peine” par défaut.

- Décision retenue :
  - le système calcule explicitement des volumes d’intersection :
  - minutesNuitEtDimanche
  - minutesNuitEtFerie
  - (optionnel) minutesDimancheEtFerie
- et le scoring applique une dominance (via PenaliteKey / ScoreWeights) de façon à ce que :
  - une minute chevauchante soit pénalisée selon une règle unique maîtrisée,
  - plutôt que par addition naïve de toutes les pénalités.

Une analyse d’impact V2 est requise pour aligner toutes les contraintes en minutes sur ce mécanisme.

**Statut / périmètre**

Décision structurante V3.
Une analyse d’impact sur V2 sera menée afin :
- d’identifier les écarts de mesure introduits,
- d’adapter les tests,
- de préserver les invariants de dominance et de pondération.

**Principe**
Le moteur ne découpe jamais les créneaux. Les volumes (nuit, dimanche, férié) sont calculés par intersection temporelle à la minute.

**Primitive**
minutesIntersect(A, B) renvoie la durée d’intersection de deux intervalles [start, end).

**Volumes**
Pour tout créneau travaillé (compteDansCharge=true), le moteur calcule :
- minutesNuit
- minutesDimanche
- minutesFerie

**Chevauchements**
Une minute peut appartenir à plusieurs catégories (ex. nuit et dimanche). Les volumes sont indépendants et explicatifs.
Les règles de non-double-pondération relèvent du scoring (ScoreWeights / PenaliteKey), pas de la mesure.

---

### 6. Séparation “mesure de restitution” vs “mesure d’arbitrage” (V3)

Le moteur calcule deux familles de compteurs :

**A. Compteurs de restitution (hors scoring)**
Ils servent uniquement à :
- restituer un planning lisible,
- produire des tableaux de charge (jour/hebdo/mois),
- alimenter l’analyse aval.

Ils ne sont jamais utilisés pour l’arbitrage du solveur.

Exemples :
- minutes/heures travaillées par jour calendaire,
- agrégations hebdomadaires / mensuelles,
- agrégations par lieu, activité, poste comptable.

**B. Compteurs d’arbitrage (scoring)**
Ils servent à mesurer les pénibilités / coûts d’organisation utilisés dans le score.

En V2/V3, l’arbitrage repose exclusivement sur :
- minutes de nuit
- minutes de dimanche
- minutes de jour férié

Ces compteurs sont calculés par intersection temporelle (sans découpage de créneaux).

---

## 6. Règles fondamentales sur les contraintes

### Contraintes HARD

* Ne doivent jamais être violées.
* Structurent l’espace de recherche.
* Une solution qui les viole est **interdite**.

### Contraintes SOFT

* Peuvent être violées.
* Sont hiérarchisées par des poids.
* Servent à arbitrer entre plusieurs solutions imparfaites.

### Décision clé

> Les contraintes **ne dépendent pas des scénarios**, mais du **contexte de résolution**.

Les scénarios ont servi à **révéler** les contraintes, pas à les figer.

---

## 7. Rôle réel des scénarios de test (1 à 5)

Les scénarios sont des **tests de capacité du moteur**, pas une configuration définitive.

| Scénario | Rôle               | Capacité validée                                |
| -------- | ------------------ | ----------------------------------------------- |
| 1        | Découverte         | Affecter quand tout est possible                |
| 2        | Structuration      | Dire « impossible » sans tricher                |
| 3        | Aide à la décision | Modéliser un manque (poste virtuel)             |
| 4        | Scoring            | Arbitrer entre plusieurs solutions imparfaites  |
| 5        | Réalisme           | Violer une préférence personnelle si nécessaire |

### Décision explicite

> Les scénarios **ne sont pas tous compatibles simultanément**.
> Ils ont servi à comprendre et classifier les règles.

### Décision – Alignement strict des fixtures sur le modèle métier

Les fixtures de test doivent construire des objets valides au sens du domaine.
Aucun constructeur ou raccourci ne doit être ajouté au modèle pour faciliter les tests.

---

8. Décision — Dominance configurable “plus favorable au salarié” (V3)

**Objectif**

En cas de chevauchement de catégories temporelles (ex. nuit + dimanche, nuit + férié), le moteur applique une dominance afin d’éviter la double-pondération d’une même minute.

**Principe directeur**

La dominance doit être la plus favorable au salarié, mais cette notion dépend potentiellement :
- du secteur,
- de la convention,
- ou des règles internes du client.

**Décision retenue**

L’ordre de dominance n’est pas figé dans le moteur.
Il est fourni par l’appelant via le PlanningContext / paramètres réglementaires.

**Conséquences**

- Les volumes bruts (minutesNuit, minutesDimanche, minutesFerie, intersections) sont toujours calculés pour l’explicabilité.
- Le scoring choisit la pénalité applicable selon l’ordre de dominance transmis.
- Toute absence de paramètre doit conduire à un ordre par défaut explicite et documenté (jamais implicite silencieux).

---

## 9. Décision d’architecture majeure

### Principe retenu

* Un **seul moteur** de résolution.
* Un **ensemble stable de contraintes**.
* Des **variations de comportement** pilotées par le contexte.

### Mise en œuvre prévue

* Introduction d’un `PlanningContext` fourni par l’appelant (WebDev).
* Utilisation de contraintes **configurables** (poids dynamiques).
* Aucun code spécifique par scénario.

---

## 9 bis. Décision de stabilisation du scoring (V2)

### Contexte

La phase V2 du moteur de planification vise à **stabiliser le scoring** afin de :
- rendre les arbitrages explicables ;
- dissocier clairement la **mesure des violations** de leur **pondération** ;
- préparer les évolutions futures (V3+) sans dette technique.

Cette stabilisation s’appuie sur les retours des scénarios de test et sur l’exploitation des indicateurs de type *WorkMetrics*.

---

### Principe fondamental retenu

> Les contraintes **mesurent des écarts ou des volumes**,  
> Le scoring **arbitre ces mesures** via des poids centralisés.
> Les WorkMetrics sont calculées post-résolution et ne participent jamais directement au calcul du score.

En conséquence :
- aucune contrainte ne porte de coefficient métier ;
- aucune contrainte ne dépend directement d’un scénario ;
- toute pondération est centralisée et explicite.

---

### Découpage des responsabilités

Le scoring est structuré selon trois niveaux clairement séparés :

#### 1. Contraintes (mesure)

Les contraintes :
- détectent une situation (ex. travail de nuit, créneau non couvert) ;
- produisent une **mesure neutre** :
  - en **minutes** (nuit, jour férié),
  - ou en **occurrence** (créneau non couvert, poste virtuel) ;
- sont identifiées par une clé métier (`PenaliteKey`).

Aucune logique de stratégie ou de priorité n’est portée par les contraintes.

---

#### 2. ScoreWeights (pondération)

La classe `ScoreWeights` :
- centralise les **poids par type de pénalité** (`PenaliteKey`) ;
- décline ces poids selon la **stratégie de scoring** (`StrategieScoring`, utilisée comme multiplicateur global) ;
- constitue la **seule source de vérité** pour l’arbitrage relatif des pénalités.

Les stratégies actuellement définies sont :
- `EXPLOITATION` (continuité de service),
- `ANALYSE_RH` (lecture RH et équité),
- `AUDIT` (signal réglementaire).

---

#### 3. ScoreUtils (construction du score)

`ScoreUtils` est le **point de passage unique** qui :
- combine une clé de pénalité, une mesure (volume) et une stratégie ;
- applique les poids définis dans `ScoreWeights` ;
- construit le score OptaPlanner (`HardSoftScore`).

Aucune règle métier n’est implémentée à ce niveau.

---

### Décision — Calcul des pénibilités légales par intersection temporelle

#### Contexte
Les premières versions du moteur distinguaient certains types de créneaux :
- créneau de nuit
- créneau de dimanche
- créneau de jour férié

Cette approche ne permettait pas de représenter correctement des situations réelles telles que :
- un créneau 18h–23h contenant une heure de nuit,
- un créneau samedi 22h → dimanche 06h combinant nuit et dimanche,
- un créneau 00h–02h un jour férié.

#### Décision
Le moteur adopte une approche par intersection temporelle.
Les pénibilités ne sont plus associées à un type de créneau mais à des minutes réellement travaillées dans des intervalles réglementaires.

Le calcul est réalisé par : `TimeBreakdownCalculator`

Ce calcul décompose chaque créneau en minutes appartenant aux catégories :
- minutes de nuit
- minutes de dimanche
- minutes de jour férié

ainsi que leurs intersections :
- nuit + dimanche
- nuit + férié
- dimanche + férié
- nuit + dimanche + férié

#### Conséquence

Les anciennes contraintes :
- CreneauDeNuit
- CreneauJourFerie
ne sont plus utilisées pour le scoring des pénibilités.

Elles sont remplacées par une contrainte unique : `PenibilitesLegalesMinutes`

---

### Décision — Dominance des pénibilités

Lorsque plusieurs pénibilités s’appliquent simultanément, une dominance configurable est utilisée.

Exemple : NUIT > DIMANCHE > FERIE

Les minutes appartenant à plusieurs catégories sont attribuées à la pénibilité dominante uniquement.

La logique est implémentée dans : `ScoreUtils.penalitesLegalesAvecDominance(...)`

Cette méthode :
1. reconstruit les volumes exclusifs par inclusion/exclusion,
2. applique l’ordre de dominance,
3. applique les poids de scoring.

### Décision — Source réglementaire des jours fériés

Les jours fériés ne sont plus déduits des créneaux.

La source de vérité est : `RegulatoryParameters`

Ce composant contient :
- l’intervalle réglementaire de nuit
- la liste des jours fériés

Il est injecté comme ProblemFact dans : `PlanningProblem`

---

### Décision — Type d’impact utilisé par les contraintes

Le score utilisé par le moteur est : `HardSoftScore`

Ce score utilise un ScoreImpacter basé sur des entiers.

Par conséquent : `penalizeLong(...)` ne peut pas être utilisé.

Toutes les contraintes doivent utiliser : `penalize(...)` 
avec conversion explicite : `Math.toIntExact(...)`si nécessaire.

---

### Décisions structurantes associées

Les décisions suivantes sont **explicitement actées** :

- Les pénibilités **Nuit** et **Jour férié** sont comptabilisées en **minutes**.
- Les situations **Créneau non couvert** et **Poste virtuel** sont comptabilisées en **occurrence**.
- Les coefficients conditionnels par scénario ont été supprimés des contraintes.
- La stratégie de scoring est un **contexte de lecture**, pas un algorithme.

---

### Validation par tests (dominance)

Les arbitrages V2 sont validés par des **tests de dominance sans solver**, qui prouvent que :
- certaines pénalités dominent structurellement d’autres (ex. non couvert ≫ nuit) ;
- les seuils d’équivalence sont cohérents avec les poids définis ;
- un changement de stratégie modifie effectivement le score produit.

Ces tests constituent des **preuves d’invariants d’arbitrage**, et non des tests d’optimisation.

---

### Conséquences pour les évolutions futures

Ce découpage permet :
- d’introduire de nouvelles pénalités sans refonte globale ;
- de modifier les poids sans toucher aux contraintes ;
- d’expliquer une solution sous la forme :
  > (clé de pénalité, unité, volume, poids, stratégie, contribution au score).

Les évolutions V3 (équité, pénibilité par occurrence, préférences) s’appuieront sur ce socle sans en modifier les principes.

---

## 2026-03-05 — Exécution réelle du solveur OptaPlanner

### Décision

Le moteur de planification appelle désormais le solveur OptaPlanner en exécution réelle dans le scénario SC-01.

L’appel est effectué via la chaîne suivante :

ScenarioController  
→ PlanningRequest  
→ PlanningService  
→ SolverLauncher  
→ OptaPlanner  
→ PlanningProblem résolu

La solution retournée par le solveur est récupérée via : `solved.solution().getCreneaux()`

### Validation

Le fonctionnement du solveur et du scoring a été vérifié en exécution.
Des logs supplémentaires dans `ScenarioController` confirment :
- le score final calculé par OptaPlanner
- les affectations des créneaux aux ressources.

### Décision associée

La structuration complète de la réponse du solveur (score, diagnostics, affectations détaillées) n'est **pas exposée pour l’instant dans l’API**.

L’API continue de renvoyer un modèle métier (`ScenarioResponseDTO`) afin de :

- stabiliser d’abord le moteur de planification,
- concevoir ultérieurement un **contrat de sortie solveur propre et pérenne**.

Cette décision évite d'introduire une structure de réponse provisoire qui devrait être refactorisée plus tard.

---

## 10. Éléments volontairement différés

Les éléments suivants sont identifiés mais volontairement repoussés :

* gestion fine du temps (heures, chevauchements, pauses) ;
* Prise en compte décisionnelle des coûts (dette repos, heures sup) : oui, via indicateurs dérivés ; calcul exact potentiellement externalisé ;
* trajets et distances ;
* contrats de travail détaillés ;
* explication utilisateur détaillée (UI).

Ces sujets seront traités **après validation du socle conceptuel**.

---

## 11. Invariants à respecter pour la suite du projet

* Pas de `null` pour représenter une absence d’affectation.
* Toute règle doit être classable (physique / légale / métier / personnelle).
* Les arbitrages doivent être explicables.
* Les tests servent à verrouiller des capacités, pas à figer des implémentations.

---

## 12. Statut du document

* Document vivant.
* Toute remise en cause d’un invariant doit être **explicitement discutée**.
* Sert de référence pour les échanges futurs et le mapping WebDev → moteur.

---

**Fin du document**
