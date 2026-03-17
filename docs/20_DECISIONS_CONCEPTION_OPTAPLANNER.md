# Décisions de conception – Moteur de planification (OptaPlanner)

---

## 1. Objectif du moteur de planification

### Objectif principal

Le moteur de planification a pour objectif de **proposer une affectation cohérente de créneaux de travail à des ressources**, en tenant compte de contraintes multiples, parfois contradictoires.

Les caractéristiques temporelles des créneaux (date, heure de début, heure de fin) sont définies en amont par le scénario ou le logiciel de planning.

Le moteur décide uniquement quelle ressource couvre quel créneau, en respectant les contraintes réglementaires, organisationnelles et métier.

Il doit :
* produire **une solution explicable**, pas seulement optimale mathématiquement ;
* accepter l’existence de **situations imparfaites** ;
* mettre en évidence les **manques de ressources** ou les **violations nécessaires**.

## Traitement des besoins

Les besoins de présence continus ou discontinus sont modélisés sous forme de créneaux élémentaires à couvrir.
Une journée de présence peut ainsi être représentée par plusieurs créneaux liés logiquement entre eux (ex. matin / après-midi).
Le moteur continue alors d’optimiser uniquement l’affectation des ressources, tandis que les contraintes évaluent la cohérence d’ensemble de la journée (continuité, amplitude, fragmentation, etc.).

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

---

## 3. Modèle conceptuel stabilisé

### Entités principales

* **Créneau**
  * Unité élémentaire de travail à affecter.
  * Porte la variable de décision `ressourceAffectee`.

* **Ressource** (concept abstrait)
  * **Salarié réel** : personne existante.
  * **Poste virtuel** : besoin non couvert / potentiel de recrutement.
  * **État `A_AFFECTER`** : absence d’affectation explicite (pas de `null`).

* **PlanningSolution**
  * Contient la liste des créneaux et des ressources.
  * Porte le score OptaPlanner.

* **PlanningContext**
  * Objet de contexte décrivant le cadre de résolution.
  * Porte notamment les paramètres nécessaires à l’interprétation correcte d’un scénario.

* **Paramètres conventionnels**
  * `RegulatoryParameters` est porté par le contexte ou injecté comme fait immuable selon l’architecture retenue.
  * Les règles de conversion métier (heures → repos / majorations) sont fournies par l’amont ; le moteur score et arbitre, mais ne produit pas la règle conventionnelle.

### Décision — Représentation des créneaux non affectés

Un créneau non couvert est représenté explicitement par la pseudo-ressource `A_AFFECTER`.

`ressourceAffectee` n’est jamais `null` : l’absence d’affectation est toujours matérialisée par un objet explicite.

Conséquences :
- la valeur `"A_AFFECTER"` apparaît dans le planning retourné par l’API ;
- les créneaux non couverts sont comptabilisés dans `solutionSummary.nbCreneauxNonAffectes` et dans le `scoreBreakdown` via la pénalité `METIER_SOFT_CRENEAU_NON_COUVERT` ;
- les métriques RH (`workMetrics.byRessource`) n’incluent pas `A_AFFECTER`.

Cet invariant est également listé en §11. La forme contractuelle de cette représentation est décrite dans `50_interface_windev_moteur.md`.

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

### 2. Principe structurant V2
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

#### Décision — Nature des WorkMetrics

Les WorkMetrics ne sont jamais des ProblemFacts du solveur.

Elles ne participent pas à la résolution et ne sont pas utilisées comme variables ou faits dans l’optimisation.

Elles sont exclusivement produites après résolution dans une logique de restitution et d’explicabilité.

### 3. Décision — Gestion des chevauchements temporels sans découpage de créneaux

Le moteur ne découpe jamais un créneau pour matérialiser des sous-segments calendaires ou réglementaires.

Lorsqu’un créneau chevauche plusieurs catégories temporelles (nuit, dimanche, férié, etc.), les volumes utiles sont calculés par intersection temporelle à partir du créneau d’origine.

Cette décision garantit :
- la stabilité du modèle solveur ;
- la stabilité de l’API ;
- la cohérence entre mesure, scoring et explicabilité.

Les règles détaillées de calcul et de dominance sont documentées dans les documents de la série 40.

**Conséquences**

1. Aucune reconstitution n’est nécessaire en sortie : on ne fragmente rien.
2. Les contraintes consomment des mesures (minutes) plutôt qu’une qualification globale.
3. Les WorkMetrics consomment les mêmes volumes, en post-résolution, pour garantir la cohérence “mesure ↔ constats”.
4. Les attributs de type “qualifiant” (ex. TypePlageHoraire) peuvent subsister comme indicateurs, mais ne constituent pas une source de vérité suffisante en présence de chevauchements.

**Règle de cohérence**

Toute pénalité exprimée “en minutes” (nuit, dimanche, férié, etc.) doit être dérivée de ces volumes partiels calculés par intersection temporelle, et non d’une qualification globale.

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

### Décision — Séparation solveur / API via ScenarioResponseMapper

Le moteur sépare explicitement la solution interne du solveur (`PlanningProblem`, `PlanningSolution`) de la représentation exposée par l’API (`ScenarioResponseDTO`).

Cette séparation est assurée par le composant `ScenarioResponseMapper`.

Objectifs :
- éviter toute dépendance directe de l’API vis-à-vis d’OptaPlanner ;
- permettre l’évolution du modèle interne sans casser le contrat API ;
- centraliser la logique de restitution (planning, scoreBreakdown, métriques, diagnostics).

Chaîne de transformation :

```
Solveur interne → PlanningSolution → ScenarioResponseMapper → ScenarioResponseDTO (contrat API)
```

### Décision — Production des diagnostics d’affectation dans le mapper API

Les diagnostics d’affectation (`assignmentDiagnostics`) sont produits dans `ScenarioResponseMapper`, pas dans le builder amont, ni dans le solveur, ni dans les WorkMetrics.

Cette décision :
- préserve l’indépendance du solveur ;
- évite de polluer le modèle interne avec des objets d’explication API ;
- expose des diagnostics contextualisés par créneau sans dépendance directe à `ScoreExplanation`.

---

## 9 bis. Décision de stabilisation du scoring

> **Périmètre de ce document** : ce chapitre énonce les **décisions architecturales** prises pour structurer le scoring.
> La description fonctionnelle complète (comment fonctionne le scoring, quels volumes, quelle dominance) est dans `40_STRATEGIE_DE_SCORING.md` et `40_WORKMETRICS.md`.

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

### Décision — Explicabilité du score

Le moteur doit produire un score explicable permettant d’analyser
les contributions des différentes pénalités.

Cette explicabilité repose sur :
- l’identification explicite de chaque pénalité ;
- l’association à une unité de mesure cohérente ;
- la possibilité d’agréger et de comparer les contributions.

La forme contractuelle de restitution est décrite dans
`50_interface_windev_moteur.md`.

---

### Décision — `PenaliteKey` porte l’unité de restitution du breakdown

L’unité de chaque ligne de `scoreBreakdown` est rattachée directement à `PenaliteKey`.

Conséquences :
- suppression des `switch` de résolution d’unité dans la couche de restitution ;
- centralisation de la construction des items de breakdown ;
- stabilité accrue du contrat API lors de l’ajout de nouvelles pénalités.

Ce principe renforce le découpage : Contraintes → mesurent · `ScoreWeights` → pondèrent · `scoreBreakdown` → restitue.

---

### Décision — Explicabilité du score

Le moteur doit produire un score explicable, permettant d’analyser
les contributions des différentes pénalités.

La structure de restitution du score est définie dans le contrat API
(`ScenarioResponseDTO`) et documentée dans :

- 50_interface_windev_moteur.md

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

### Invariant d’extensibilité

Ce découpage garantit que :
- toute nouvelle pénalité peut être introduite sans refonte globale ;
- les poids peuvent être modifiés sans toucher aux contraintes ;
- une solution peut toujours être expliquée sous la forme :
  > (clé de pénalité, unité, volume, poids, stratégie, contribution au score).

Toute évolution future des contraintes, WorkMetrics ou stratégies devra respecter ce découpage des responsabilités.

---

## Intégration du solveur OptaPlanner

L’intégration du solveur OptaPlanner est réalisée dans le moteur.

Les détails d’implémentation, la chaîne d’appel, les validations d’exécution,
ainsi que le contrat API (`ScenarioResponseDTO`) sont documentés dans les documents :

- 50_interface_windev_moteur.md
- 90_suivi_developpement_moteur.md
- 91_Journal_Developpement_Moteur.md

Ce document ne décrit que les décisions de conception associées.

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
