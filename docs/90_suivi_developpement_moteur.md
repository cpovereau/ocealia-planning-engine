# 📍 Suivi de développement — Moteur de planification

Ce document constitue **le tableau de bord réel du projet moteur de planification**.

Il répond à la question :

> Où en est réellement le moteur aujourd’hui et que reste-t-il à construire ?

Ce document est volontairement :

* factuel
* aligné sur l’implémentation réelle
* orienté pilotage du projet

Il ne redéfinit aucune règle métier et ne décrit que des **capacités implémentées ou manquantes**.

---

# 🧭 Rôle du document

Ce document :

* complète le **référentiel métier** (qui explique le pourquoi)
* complète les documents techniques (qui expliquent le comment)
* sert de **support de pilotage du développement du moteur**

Il doit être mis à jour **à chaque évolution structurante du moteur**.

---

# 📌 État & Cap (source de vérité)

## A. Livré (fait et prouvé)

### WorkMetrics V3

Implémentation complète des métriques RH suivantes :

* calcul des pénibilités en minutes
* détection des nuits
* détection des jours fériés
* dominance jour / nuit
* séquences observées

Preuves :

```
.\\gradlew test
```

Fichiers principaux impliqués :

* TimeBreakdownCalculator
* PenibilitesLegalesMinutes
* ScoreUtils
* RegulatoryParameters

---

### Branchement du solveur OptaPlanner

Le solveur est désormais intégré dans le pipeline complet du moteur.

Pipeline actuel :

```
ScenarioController
    ↓
PlanningRequest
    ↓
PlanningService
    ↓
SolverLauncher
    ↓
OptaPlanner
```

Preuves d'exécution :

* score observé en exécution
* affectations confirmées dans les logs
* récupération de la solution via `solved.solution().getCreneaux()`

---

### Restitution solveur V1 stabilisée

Le moteur restitue désormais une réponse complète via `ScenarioResponseDTO`.

Blocs principaux exposés :

```
planning
workMetrics
solutionSummary
solverResult
scoreBreakdown
```

Fonctionnalités incluses :
* mapping de la solution via `ScenarioResponseMapper`
* planning détaillé par créneau
* gestion explicite des créneaux non couverts (`A_AFFECTER`)
* résumé de solution
* métriques par ressource et globales
* score explicable

La structure de réponse est stabilisée et exploitable côté API,
avec un niveau d’information suffisant pour :
- analyser un planning,
- comprendre le score,
- identifier les créneaux non couverts.

---

### Explicabilité du solveur V2

Ajout d’une couche d’explicabilité avancée.

Évolutions :

* `PenaliteKey` devient la référence pour l’unité des pénalités
* centralisation de la construction des `ScoreBreakdownItemDTO`
* suppression des résolutions d’unité par `switch`

Ajout du bloc :

```
diagnostics.assignmentDiagnostics
```

Ce bloc expose :

* créneaux non couverts
* raison de non affectation
* message diagnostique

Diagnostics implémentés :

* UNCOVERED
* NO_RESOURCE_ASSIGNED
* IMPOSSIBLE_TO_ASSIGN

---

### Contraintes métier SOFT

Contraintes actuellement implémentées :

| Règle                          | Statut     |
| ------------------------------ | ---------- |
| Créneau non couvert            | Implémenté |
| Pénalisation poste virtuel     | Implémenté |
| Travail sur repos hebdomadaire | Implémenté |

Le calcul des pénibilités repose désormais sur une approche unifiée.

Voir :
- 40_WORKMETRICS.md
- 40_STRATEGIE_DE_SCORING.md

---

### Paramétrage du scoring

Le scoring repose désormais sur :

| Élément           | Rôle                        |
| ----------------- | --------------------------- |
| SeuilsDeTolerance | bornes métier               |
| Penalites         | clés métier du score        |
| ScoreWeights      | pondérations stabilisées    |
| ScoreUtils        | construction du score       |
| StrategieScoring  | contexte d’analyse du score |

Le paramétrage du scoring est désormais centralisé et stable, permettant d’ajuster les pondérations sans modifier les contraintes.

---

## 🧩 Cartographie des contraintes et des métriques associées

> 📌 Cette section constitue la **vue de synthèse centrale** entre :
>
> * les contraintes du moteur (HARD / SOFT)
> * les métriques observables (WorkMetrics)
> * les éléments d’explicabilité (diagnostics, score)
>
> Elle permet de répondre à la question :
> **"Qu’est-ce qui est contrôlé, observable et implémenté dans le moteur ?"**

---

### 🔎 Principe de lecture

* Une **contrainte** agit dans le solveur (validation ou pénalisation)
* Une **métrique** décrit le résultat après résolution
* Un **diagnostic** explique certains cas particuliers

👉 Une métrique n’active jamais une contrainte.
👉 Une contrainte peut être expliquée par une métrique.

---

### 📊 Tableau de cartographie

| Contrainte                            | Type | Implémentée  | Métrique associée             | Exposée en WorkMetrics | Diagnostic associé                        | ScoreBreakdown | Source documentaire         |
| ------------------------------------- | ---- | ------------ | ----------------------------- | ---------------------- | ----------------------------------------- | -------------- | --------------------------- |
| Créneau non couvert                   | SOFT | ✅            | nbCreneauxNonAffectes         | Oui                    | UNCOVERED / NO_RESOURCE_ASSIGNED          | Oui            | 50_ScenarioResponseContract |
| Pénalisation poste virtuel            | SOFT | ✅            | –                             | Non                    | VIRTUAL_ASSIGNED / POSTE_VIRTUEL_ASSIGNED | Oui            | 50_ScenarioResponseContract |
| Travail sur repos hebdomadaire (R8)   | SOFT | ✅            | heuresReposHebdoTravaille     | Oui                    | Non                                       | Oui            | 40_WORKMETRICS              |
| Repos hebdomadaire min glissant (R7)  | HARD | ✅            | –                             | Non                    | Non                                       | Non            | 40_REGLES_COMBINATOIRES     |
| Nuits consécutives max (R3)           | HARD | ⏳ / partiel  | maxNuitsConsecutivesObservees | Oui                    | Non                                       | Non            | 40_REGLES_COMBINATOIRES     |
| Jours consécutifs max (R1)            | SOFT | ❌            | maxJoursConsecutifsObservees  | Oui                    | Non                                       | Non            | 40_REGLES_COMBINATOIRES     |
| Dimanches maximum (R9)                | SOFT | ⏳            | nbDimanchesTravailles         | Oui                    | Non                                       | Non            | 40_REGLES_COMBINATOIRES     |
| Pénibilité nuit                       | SOFT | ✅            | heuresNuit                    | Oui                    | Non                                       | Oui            | 40_WORKMETRICS              |
| Pénibilité jours fériés               | SOFT | ✅            | heuresJourFerie               | Oui                    | Non                                       | Oui            | 40_WORKMETRICS              |
| Nuit affectée à salarié non-nuit      | SOFT | ✅            | nbCreneauxNuitNonNuit         | Oui                    | Non                                       | Oui            | 40_WORKMETRICS              |
| Jour férié refusé (travailleJourFerie)| HARD | ✅            | –                             | Non                    | JOUR_FERIE_NON_COUVERT                    | Non            | 50_ScenarioResponseContract |
| Indisponibilité salarié               | HARD | ✅            | –                             | Non                    | Non                                       | Non            | 90_plan_migration §Phase 4  |

---

### 🧠 Lecture architecturale

Cette cartographie matérialise la chaîne suivante :

```text
Contrainte → Score → Métrique → Diagnostic
```

Elle permet :

* d’identifier les écarts entre implémentation et cible
* de vérifier la cohérence entre scoring et restitution
* de prioriser les développements futurs

---

### 📌 Règles d’évolution

* Toute nouvelle contrainte doit être ajoutée à ce tableau
* Toute nouvelle métrique doit être reliée à une contrainte ou explicitement marquée comme "descriptive uniquement"
* Le statut "Implémentée" doit refléter l’état réel du code (tests + exécution)

---

### 📍 Position recommandée dans le document 90

👉 À placer **juste après la section "État & Cap" (A / B / C)**

Raison :

* elle sert de **pont entre l’état réel et la vision fonctionnelle**
* elle structure la lecture avant les détails techniques
* elle évite que le lecteur navigue entre plusieurs documents pour comprendre le moteur

---

### ⚠️ Important

Cette section ne remplace pas :

* `40_REGLES_COMBINATOIRES.md` (définition des règles)
* `40_WORKMETRICS.md` (définition des métriques)

Elle joue un rôle de **cartographie transverse uniquement**.

---

## Tableau de suivi des WorkMetrics

Ce tableau constitue la source de vérité sur l'état d'implémentation des WorkMetrics.
La définition fonctionnelle de chaque domaine est dans `40_WORKMETRICS.md`.

| Domaine                              | Livré | Où (code)                                           | Où (tests)         | Doc                  |
| ------------------------------------ | ----- | --------------------------------------------------- | ------------------ | -------------------- |
| Mesures temporelles (nuit/dim/férié) | ✅    | TimeBreakdownCalculator + PenibilitesLegalesMinutes | tests existants    | 40_WORKMETRICS §2    |
| WorkMetrics de restitution           | ⏳    | WorkMetricsCalculator                               | scénarios          | 40_WORKMETRICS §4-5  |
| Dominance                            | ✅    | ScoreUtils                                          | ScoreDominanceTest | 40_WORKMETRICS §2    |
| Séquences (contraintes)              | ✅    | ReposHebdomadaireMin/Glissant                       | tests contraintes  | 40_REGLES_COMBINATOIRES |
| Séquences (WorkMetrics observées)    | ✅    | WorkMetricsCalculator                               | scénario 1         | 40_WORKMETRICS §5.1  |
| Équité (WorkMetrics) — V3-C          | ❌    | –                                                   | –                  | 40_WORKMETRICS §5.2  |
| Référentiel contractuel — V4         | ❌    | –                                                   | –                  | 40_WORKMETRICS §5.3  |
| Dettes & coûts abstraits — V5        | ❌    | –                                                   | –                  | 40_WORKMETRICS §5.4  |

---

## Détail des WorkMetrics implémentées et validées (V2)

Les règles suivantes sont implémentées et validées par les tests automatisés.

### Métriques calculées

**Travail total**
- Somme des durées de tous les créneaux valides (`compteDansCharge = true`).

**Travail de nuit**
- Somme des minutes dans la plage réglementaire de nuit, calculées par intersection via `TimeBreakdownCalculator`.

**Travail les jours fériés**
- Somme des minutes sur un jour férié, calculées par intersection via `TimeBreakdownCalculator`.

**Repos hebdomadaire travaillé**
- Un créneau `RH` ou `RHD` est un repos hebdomadaire non travaillé.
- La durée du créneau est ajoutée aux minutes de repos hebdomadaire travaillé.
- La dette de repos est pilotée par le `ReferentielComptabiliteActivite`, comptabilisée par jour distinct.

**Dimanches travaillés**
- Un créneau sur un dimanche calendaire dont l'activité compte dans la charge.
- Comptage par date distincte — plusieurs créneaux le même jour = un seul dimanche travaillé.

---

# B. En cours (fait partiellement)

## Évolution du DataSet d’entrée

Objectif :

Aligner le contrat d’entrée du moteur avec les structures réelles du logiciel de planning.

Évolutions introduites :

### Axes organisationnels

* direction
* service
* lieu
* poste comptable

### Données portées par la ressource

* contrat de travail
* contraintes réglementaires

### Structuration des besoins

* groupeBesoinId
* blocJourId
* ordreDansBloc
* estSegmentDePause

### Infrastructure de test

Introduction d’un **dataset de référence technique** permettant de tester l’alimentation WebDev → moteur.

Stratégie de migration :

1. évolution compatible du schéma V1
2. adaptation du DatasetBuilder
3. introduction progressive d’une structure V2

---

### Contrôles combinatoires

Premières briques implémentées :

* repos hebdomadaire
* dimanches maximum
* séquences observées

Les métriques correspondantes sont désormais calculées côté moteur.

---

### Exposition des WorkMetrics

Les métriques calculées sont désormais exposées dans l’API scénario.

---

# C. Prochains jalons

## WorkMetrics Équité

Ajout d’indicateurs d’équité :

* écart par rapport à la moyenne
* dispersion de charge

---

## Compléter le DataSet amont

Ajout progressif des éléments nécessaires à la résolution :

* groupes de besoins (`groupeBesoinId`)
* blocs journaliers (`blocJourId`)
* ordre des créneaux dans un bloc
* identification des segments de pause

Ajouts prévus :

* entrepôt des activités
* paramètres réglementaires des ressources

---

## Stabilisation du contrat d’entrée

Objectif :

Éviter le faux sentiment de couverture fonctionnelle pendant l’enrichissement progressif du dataset.

Chaque champ doit être qualifié selon son niveau réel d’exploitation.

---

# 1️⃣ Capacités partiellement implémentées

| Sujet                   | Limitation actuelle                         |
| ----------------------- | ------------------------------------------- |
| Explicabilité détaillée | lecture pédagogique du score encore absente |

Des logs de diagnostic existent déjà :

* ScoreExplanation
* WorkMetrics calculées après résolution

---

# 2️⃣ Capacités identifiées mais non implémentées

## Contraintes combinatoires avancées

| Sujet                        | Priorité |
| ---------------------------- | -------- |
| Jours consécutifs travaillés | Haute    |
| Alternance jour / nuit       | Haute    |
| Amplitude journalière        | Moyenne  |

---

## WorkMetrics futures

Les métriques futures sont documentées dans `40_WORKMETRICS.md`.

### V4 — Référentiel contractuel (§5.3)

Ces métriques expriment l'écart entre la charge planifiée et le temps contractuel de référence du salarié.

| Champ                                 | Type    | Description                                                       |
| ------------------------------------- | ------- | ----------------------------------------------------------------- |
| `deltaMinutesParRapportAuContractuel` | Decimal | Écart entre minutes travaillées et temps contractuel de référence |
| `ratioChargeContractuelle`            | Decimal | Rapport charge réelle / charge contractuelle                      |

**Pré-requis :**
- définition du temps contractuel côté métier (hors moteur)
- injection de cette information comme fait immuable dans le dataset d'entrée

**Objectif :** rendre visibles les écarts charge/contrat sans statuer sur la légalité.

---

### V5 — Dettes et coûts abstraits (§5.4)

Ces métriques représentent des coûts abstraits (non financiers) liés à la pénibilité et à la récupération obligatoire.

| Champ                    | Type    | Description                   |
| ------------------------ | ------- | ----------------------------- |
| `detteReposCompensateur` | Decimal | Volume de repos à récupérer   |
| `detteReposNuit`         | Decimal | Part liée au travail de nuit  |
| `detteReposFerie`        | Decimal | Part liée aux jours fériés    |

**Pré-requis :**
- WorkMetrics V3 complètes (séquences + équité stabilisées)
- ScoreWeights stabilisés
- Analyse métier aval définie

**Objectif :** support à la restitution RH et aide à la décision, hors moteur.

---

## Analyse métier aval

Certaines analyses resteront hors moteur :

| Sujet                         | Statut      |
| ----------------------------- | ----------- |
| Construction SurchargeSalarie | Hors moteur |
| Aide à la décision RH         | Hors moteur |

---

## Évolution prévue de la stratégie de scoring

La stratégie de scoring évolue par paliers, en cohérence avec les WorkMetrics.
La définition fonctionnelle du scoring est dans `40_STRATEGIE_DE_SCORING.md`.

| Phase            | Contraintes                    | WorkMetrics                    | ScoreWeights         |
| ---------------- | ------------------------------ | ------------------------------ | -------------------- |
| Actuelle         | HARD stabilisées, SOFT basiques| restitution partielle          | défini, usage limité |
| Suivante         | SOFT combinatoires, équité     | V3 équité                      | maîtrisé             |
| Ultérieure       | contraintes contractuelles     | équité, contractuel, dettes    | scénarios comparatifs|

---

## Feuille de route — Interface WinDev / moteur

Suivi des phases de stabilisation du contrat d'entrée.
Le contrat détaillé champ par champ est dans `50_interface_windev_moteur_contrat_detail.md`.

| Phase | Objectif                                             | Statut      |
| ----- | ---------------------------------------------------- | ----------- |
| 0     | Gel du point de départ / socle tests                 | ✅ Terminé  |
| 1     | Transport des nouveaux champs sans exploitation      | ✅ Terminé  |
| 2     | Dataset technique de référence SC-03                 | ✅ Terminé  |
| 3     | Branchement builder (mapping domaine)                | ✅ Terminé  |
| 4     | Incompatibilités structurelles (JourFerié, Indisp.)  | ✅ Terminé  |
| 5     | Structuration des besoins et blocs journaliers       | ✅ Terminé  |
| 6     | Données nuit par salarié                             | ✅ Terminé  |
| 7     | Ouverture SC-03 côté API                             | ✅ Terminé  |
| 8     | Scoring, WorkMetrics, diagnostics enrichis           | ✅ Terminé  |
| 9     | Consolidation pipeline SC-03 et diagnostics complets | ✅ Terminé  |

---

## Intégration du solveur OptaPlanner

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

### Représentation des créneaux non couverts

La représentation des créneaux non affectés est implémentée
et exposée via l’API.

Voir :
- 20_DECISIONS_CONCEPTION_OPTAPLANNER.md
- 60_interface_windev_moteur_plan_documentaire.md

### Restitution du solveur

La restitution du solveur est désormais implémentée
et expose les blocs principaux du contrat API.

Voir :
- 60_interface_windev_moteur_plan_documentaire.md

---

# 3️⃣ Ordre logique recommandé pour la suite

1. amélioration de l’explicabilité du score
2. nettoyage technique OptaPlanner
3. extension WorkMetrics
4. extension du dataset

---

# 🧠 Principe de lecture

Une capacité n’est considérée comme acquise que lorsqu’elle est :

* implémentée
* testée
* observée en exécution.

Ce document doit rester **la photographie fidèle du moteur à un instant donné**.
