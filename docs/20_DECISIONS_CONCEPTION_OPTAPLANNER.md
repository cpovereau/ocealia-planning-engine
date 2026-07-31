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

## Décision — Distinction créneaux ignorés / non affectés

Un créneau ignoré est un créneau exclu du périmètre de résolution avant construction du PlanningProblem.

Il :
- n’est pas transmis au solveur,
- n’est pas scoré,
- n’apparaît pas dans le planning.

Un créneau non affecté est un créneau inclus dans le PlanningProblem mais auquel aucune ressource réelle n’a pu être assignée.

Il :
- est présent dans le solveur,
- est représenté par la pseudo-ressource "A_AFFECTER",
- est comptabilisé dans le score et dans les métriques.

Règle fondamentale :

→ Ignoré = hors solveur  
→ Non affecté = dans solveur mais non couvert

---

## Décision — Identité des créneaux et clé de réintégration WinDev

`Id_Journee` est la **clé primaire d'une ligne de la base WinDev**. Ce n'est pas un identifiant
métier : c'est une clé technique appartenant au logiciel de planning, que le moteur reçoit,
transporte et rend — sans jamais en être propriétaire.

Trois règles en découlent.

**1. Le champ `id` d'un créneau est une chaîne opaque.**

Le moteur le transporte et le restitue à l'identique. Il ne l'interprète jamais, ne le découpe
jamais, et ne déduit aucun comportement de sa forme. Aucune contrainte ni aucun calcul de
métrique ne doit lire le *contenu* de cette chaîne.

**2. Le moteur ne fabrique jamais d'`Id_Journee`.**

Il ne produit d'identifiant que pour les créneaux qu'il génère lui-même, sous son propre préfixe
(`SC01-<date>-<séquence>`). Un identifiant produit par le moteur ne désigne aucune ligne en
base : il ne vaut que le temps de la résolution et de sa restitution.

**3. L'unicité est une exigence technique, pas un confort de lecture.**

`Creneau.id` porte l'annotation `@PlanningId`. OptaPlanner exige une valeur non nulle et unique
sur l'ensemble des créneaux d'un scénario, toutes origines confondues. Un créneau sans
identifiant n'est donc pas un défaut de restitution : c'est un défaut de résolution.

### Conséquence — la convention de préfixe appartient à WinDev

WinDev renseigne systématiquement `id`, y compris pour les besoins qui ne correspondent à
aucune ligne enregistrée, sous un préfixe distinct. Le moteur y gagne l'identifiant non nul
qu'exige `@PlanningId` ; WinDev conserve la capacité de reconnaître ses propres clés au retour
et de décider seul entre mise à jour et création.

| Forme de l'`id`          | Origine                      | Correspond à une ligne en base |
| ------------------------ | ---------------------------- | :----------------------------: |
| `Id_Journee`             | ligne de planning existante  | oui                            |
| `BES-00X`                | besoin déclaré par WinDev    | non                            |
| `SC01-<date>-<séquence>` | créneau généré par le moteur | non                            |

Ce tableau décrit une convention **lue par WinDev seul**. Le moteur n'en connaît pas
l'existence : pour lui, les trois formes sont des chaînes strictement équivalentes.

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

Cette définition constitue un **invariant d’architecture**.
Toute règle, contrainte, métrique ou évolution future doit s’y conformer.
Elle fait autorité sur toute autre formulation présente dans la documentation.

### 5.1 Définition canonique (règle unique)

> **Un créneau est considéré comme travaillé si et seulement si
> son activité compte dans la charge (`compteDansCharge = true`
> dans le référentiel d’activité).**

Par extension :

- Une **journée** est considérée comme travaillée si et seulement si elle comporte
  au moins un créneau dont l’activité a `compteDansCharge = true`.
- Une **nuit** est considérée comme travaillée si et seulement si elle comporte
  au moins un créneau en plage horaire NUIT dont l’activité a `compteDansCharge = true`.

### 5.2 Mappings Nature → compteDansCharge

| Nature                  | compteDansCharge |
| ----------------------- | :--------------: |
| `TRAVAIL`               | `true`           |
| `REPOS` (RH, RHD)       | `false`          |
| `FERIE` (JF substitutif)| `false`          |
| `RECUP`                 | `false`          |
| `NON_TRAVAILLE`         | `false`          |

### 5.3 Ce que le moteur ne déduit jamais

Le moteur ne déduit jamais la notion de travail :
- du code d’activité brut,
- du type de créneau,
- d’un qualifiant calendaire,
- de la valeur du champ `Nature` utilisée directement.

Il s’appuie **uniquement** sur `compteDansCharge` tel que fourni par le référentiel d’activité.

### 5.4 Principe structurant

- Les **contraintes** mesurent.
- Les **ScoreWeights** pondèrent.
- Les **WorkMetrics** constatent post-résolution.

Aucune métrique ne redéfinit la notion de travail.

### 5.5 Règle de cohérence transversale

- Les contraintes n’utilisent jamais directement `Nature` pour déterminer le travail.
- Les WorkMetrics n’interprètent jamais `QualificationJour` comme du travail.
- Toute évolution V3 ou ultérieure devra s’appuyer exclusivement sur la définition §5.1 ci-dessus.

Cette définition est la référence pour :
- les contraintes légales,
- les métriques WorkMetrics (toutes versions),
- et la restitution planning.

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
- centraliser la logique de restitution (planning, métriques, diagnostics).

Chaîne de transformation :

```
Solveur interne → PlanningSolution → ScenarioResponseMapper → ScenarioResponseDTO (contrat API)
```

### Décision — PlanningService comme solveur pur

`PlanningService` est un service à responsabilité unique : recevoir un `PlanningRequest`, résoudre le `PlanningProblem`, retourner les données brutes du solveur.

`PlanningResponse` contient uniquement :
- la `PlanningSolution` résolue
- le `ScoreExplanation` brut produit par `SolutionManager`

`PlanningService` ne construit aucun DTO et n’a aucune dépendance vers le package `scenarios.dto`.

### Décision — Construction du scoreBreakdown dans ScoreBreakdownFactory (couche scénarios)

La transformation `ScoreExplanation → List<ScoreBreakdownItemDTO>` est réalisée par `ScoreBreakdownFactory.build()`, dans la couche scénarios.

Cette factory est appelée par les `ExecutionService` (SC-01, SC-03), qui transmettent ensuite la liste construite au `ScenarioResponseMapper`.

Ce découpage garantit que :
- `PlanningService` ne connaît aucun DTO de restitution ;
- la logique de transformation `explanation → breakdown` est centralisée et testable indépendamment ;
- le `ScenarioResponseMapper` reçoit des données déjà transformées, sans accès direct à `ScoreExplanation`.

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
- produisent une **mesure neutre** exprimée selon l’unité portée par `PenaliteKey` ;
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

### Décision — Distinction entre nuit réglementaire globale et nuit portée par salarié

Le moteur distingue explicitement deux notions différentes :

#### 1. Nuit réglementaire globale
La nuit utilisée pour les calculs de pénibilité, de scoring et de WorkMetrics temporelles
est définie globalement par les `RegulatoryParameters` / le `PlanningContext`.

Cette plage constitue la **source de vérité commune** pour :
- le calcul des minutes de nuit,
- les intersections temporelles,
- les pénalités légales associées,
- les métriques agrégées du moteur.

#### 2. Nuit portée par salarié
Les champs portés par la ressource (`travailDeNuit`, `heureDebutNuit`, `heureFinNuit`)
décrivent une **qualification propre au salarié** :
- statut RH ou conventionnel,
- profil de travail de nuit,
- compatibilité ou traitement spécifique.

Ils ne remplacent pas la nuit réglementaire globale.
Ils la complètent pour des usages ciblés :
- contraintes spécifiques au salarié,
- pénalités différenciées,
- diagnostics ou métriques RH.

#### Règle fondamentale
La mesure temporelle de la nuit reste globale et commune.
La qualification “nuit” portée par un salarié reste locale à la ressource.

En conséquence :
- `RegulatoryParameters` demeure la source de vérité pour mesurer la nuit ;
- les champs salarié ne redéfinissent pas les volumes temporels globaux ;
- toute logique spécifique au salarié doit s’appuyer sur cette séparation.

---

### Décision — Explicabilité du score

Le moteur doit produire un score explicable permettant d’analyser
les contributions des différentes pénalités.

Cette explicabilité repose sur :
- l’identification explicite de chaque pénalité ;
- l’association à une unité de mesure cohérente (voir enum `ScoreBreakdownUnit` ci-dessous) ;
- la possibilité d’agréger et de comparer les contributions.

La forme contractuelle de restitution est décrite dans `50_ScenarioResponseContract.md`.

---

### Décision — `PenaliteKey` porte l’unité de restitution du breakdown

L’unité de chaque ligne de `scoreBreakdown` est rattachée directement à `PenaliteKey`.

Conséquences :
- suppression des `switch` de résolution d’unité dans la couche de restitution ;
- centralisation de la construction des items de breakdown ;
- stabilité accrue du contrat API lors de l’ajout de nouvelles pénalités.

Ce principe renforce le découpage : Contraintes → mesurent · `ScoreWeights` → pondèrent · `scoreBreakdown` → restitue.

#### Valeurs de l’enum `ScoreBreakdownUnit`

Chaque `PenaliteKey` est associée à exactement une valeur de cet enum, définie à sa création.

| Valeur            | Signification                                              | Exemples de pénalité associée                  |
| ----------------- | ---------------------------------------------------------- | ---------------------------------------------- |
| `OCCURRENCE`      | Nombre d’occurrences d’un événement                        | Créneau non couvert, affectation sur poste virtuel |
| `MINUTE_PONDEREE` | Minutes réelles dans un intervalle réglementaire           | Nuit, dimanche, jour férié (calcul par intersection) |
| `JOUR`            | Nombre de jours calendaires                                | Jours consécutifs travaillés au-delà du seuil  |
| `UNKNOWN`         | Unité non déterminée — valeur de sécurité / fallback       | Pénalité mal configurée ou en cours de définition |

**Règle d’usage :** toute nouvelle `PenaliteKey` doit déclarer explicitement son unité.
`UNKNOWN` ne doit jamais apparaître en production ; sa présence dans `scoreBreakdown` signale un problème de configuration.

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

### Décision de conception — Source de vérité de la durée des créneaux

**Contexte**

Le modèle de données du moteur de planification contient :
* une **durée stockée** (`Creneau.duree`)
* des **heures de début / fin** (`heureDebut`, `heureFin`) permettant de recalculer une durée

Cette double information introduit une ambiguïté :
> Quelle est la valeur de référence utilisée par le moteur et par la restitution API ?

Cette décision vise à **trancher explicitement cette ambiguïté**.

---

#### Décision

> **La durée stockée (`Creneau.duree`) est la source de vérité pour toutes les restitutions métier et les agrégats de sortie.**
> **Toute restitution API doit être cohérente avec `Creneau.duree`**

---

#### Règles associées

1. Restitution API

Les champs suivants doivent utiliser **exclusivement la durée stockée** :

* `solutionSummary.heuresTravailleesTotales`
* `workMetrics.global.heuresTravailleesTotales`
* toute agrégation horaire exposée en sortie

👉 Aucune recomposition à partir des heures ne doit être utilisée pour ces valeurs.

---

2. Planning détaillé

On autorise un **recalcul à partir des heures** uniquement pour l’affichage à condition que ce recalcul soit strictement cohérent avec la durée stockée

---

1. Recalcul technique

Le recalcul de durée à partir de `heureDebut` / `heureFin` est autorisé uniquement pour :
* des contrôles de cohérence
* des traitements techniques explicitement documentés

Il ne doit **jamais devenir une source de vérité implicite**.

---

4. Gestion des incohérences

Toute divergence entre :
* durée stockée
* durée recalculée

est considérée comme :
> ❌ une anomalie de construction du dataset

Toute divergence doit être tracée dans les diagnostics de préparation

---

5. Règle d’architecture

* Le moteur **ne choisit jamais dynamiquement** entre durée stockée et durée recalculée
* Les composants doivent appliquer une règle explicite et unique

---

#### Justification

* La durée stockée est issue du système source (Windev)
* Elle représente la **réalité métier contractualisée**
* Elle garantit la **stabilité des résultats et des agrégats**

À l’inverse, une durée recalculée :

* dépend de conventions implicites (nuit, chevauchement, minuit)
* peut diverger silencieusement

---

#### Impacts

Cette décision implique :
* un alignement des mappers API
* une cohérence entre planning et agrégats
* une clarification des responsabilités dans le code

---

#### Conclusion

> La durée stockée devient la référence unique pour toute lecture métier.
> Le recalcul reste un outil technique, jamais une vérité implicite.


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
* L’`id` d’un créneau est opaque : transporté, restitué, jamais interprété.
* Le moteur ne fabrique jamais de clé appartenant à WinDev.
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
