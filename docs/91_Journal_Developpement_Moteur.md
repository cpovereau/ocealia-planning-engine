# 📚 Journal de développement — Moteur de planification

Ce document constitue **l’historique chronologique du développement du moteur de planification**.

Contrairement au document `90_SUIVI_DEVELOPPEMENT_MOTEUR`, qui décrit l’état actuel du moteur, ce journal conserve :

* les étapes de développement
* les stabilisations techniques
* les validations importantes
* les décisions prises pendant l’implémentation

L’objectif est de **garder la mémoire technique du projet**.

---

# 2026

## Mars 2026 — Stabilisation de l’interface moteur et explicabilité

Travaux réalisés :

### 2026-03-12 — Stabilisation de l’interface WinDev / moteur

- création du test de désérialisation JSON
- stabilisation du dataset SC-01
- clarification du contrat d’entrée

---


### Stabilisation de la restitution du solveur (V1)

La structure de la réponse moteur a été stabilisée, avec
l’introduction des blocs principaux (planning, métriques,
résumé de solution, score et diagnostics).

Le détail du contrat est documenté dans
`50_interface_windev_moteur.md`.

Fonctionnalités associées :

* planning détaillé par créneau
* ressource affectée par créneau
* gestion explicite des créneaux non couverts (`A_AFFECTER`)
* résumé de solution
* métriques par ressource et globales

---

### Explicabilité du solveur V2

Une refonte de l’explicabilité du score a été réalisée.

Dans ce cadre, la gestion des unités a été centralisée
autour de PenaliteKey, afin de supprimer les logiques
de résolution dispersées.

Ajout d’une nouvelle section dans la réponse moteur :

```
diagnostics.assignmentDiagnostics
```

Ce bloc expose :
* les créneaux non couverts
* les raisons de non‑affectation
* les messages diagnostiques

Diagnostics implémentés :
* UNCOVERED
* NO_RESOURCE_ASSIGNED
* IMPOSSIBLE_TO_ASSIGN

---

### Branchement complet du solveur OptaPlanner

Validation du pipeline complet de résolution :

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

Résultats observés lors des tests :

* score initial : `0hard / -40000soft`
* score final : `0hard / 0soft`

Les affectations sont confirmées via les logs :

```
creneau → ressource
```

La solution est récupérée via :

```
solved.solution().getCreneaux()
```

---

### Évolution du DataSet d’entrée

Objectif :

Aligner progressivement le contrat d’entrée du moteur avec les structures du logiciel de planning.

Évolutions introduites :

#### Axes organisationnels

* direction
* service
* lieu
* poste comptable

#### Données portées par les ressources

* contrat de travail
* contraintes réglementaires

#### Structuration des besoins

* groupeBesoinId
* blocJourId
* ordreDansBloc
* estSegmentDePause

#### Infrastructure de test

Création d’un dataset de référence technique permettant de tester l’alimentation WebDev → moteur.

Stratégie de migration :

1. évolution compatible du schéma V1
2. adaptation du DatasetBuilder
3. introduction progressive d’une structure V2

---

### Exposition complète des WorkMetrics

Les métriques calculées par le moteur sont désormais exposées dans l’API scénario.

Les compteurs sont stabilisés côté API.

---

### Mise en place des contrôles combinatoires

Premières briques implémentées :

* repos hebdomadaire
* dimanches maximum
* séquences observées

Les métriques correspondantes sont calculées côté moteur.

---

## 10 janvier 2026 — Stabilisation du socle de tests WorkMetrics V2

Travaux réalisés :

### Finalisation du modèle `PlanningContext`

Évolutions :

* ajout explicite du type de résolution
* ajout des hypothèses d’historique
* constructeurs normés (complet / socle test)

---

### Mise en place des factories de test

Création de :

```
TestPlanningContextFactory
```

Séparation claire des factories de test :

* `TestRessourceFactory`
* `TestReferentielFactory`

Ces factories permettent de produire des contextes de test **normatifs et reproductibles**.

---

### Alignement des tests WorkMetrics

Les tests ont été alignés sur le modèle réel du moteur.

Éléments introduits :

* utilisation du référentiel `ComptabiliteActivite`
* référentiels minimaux mais valides

---

### Mise en place des tests WorkMetrics V2

Deux cas de test ont été introduits :

1. activité générant une dette de repos
2. activité sans dette de repos

Le socle de tests V2 est désormais considéré comme **stable**.

---

### Règles fonctionnelles validées pour WorkMetrics V2

Ces règles constituent le périmètre fonctionnel validé.

Principes :

* WorkMetrics est un calcul **post‑résolution**
* il est **indépendant d’OptaPlanner**
* un WorkMetrics existe pour chaque ressource

Les créneaux sont ignorés lorsqu’ils sont :

* hors horizon temporel
* associés à une activité absente du référentiel

Les métriques sont isolées par ressource.

---

### Règles de calcul validées

Repos hebdomadaire travaillé :

* comptabilisé pour les jours RH et RHD
* génère une dette de repos par jour distinct

Dimanches travaillés :

* comptés par date distincte
* indépendamment du nombre de créneaux

---

## Mars 2026 — Migration du contrat d’entrée WinDev → moteur (Phases 0 à 8)

Les itérations ci-dessous retracent les travaux de migration du dataset d’entrée.
La stratégie complète est documentée dans `90_plan_migration_temporaire_windev_vers_moteur.md`.

### Itération 1 — Phase 1 (2026-03-13) — Stabiliser la couche transport

- **Objectif** : permettre au moteur de recevoir les nouveaux champs sans les exploiter
- **Fichiers modifiés** :
  - `scenarios/dto/DataSetDTO.java` — ajout creneaux, referentiels, indisponibilites
  - `scenarios/dto/RessourcesDTO.java` — remplacement SalarieReel/PosteVirtuel par SalarieInputDTO/PosteVirtuelInputDTO
  - `scenarios/dto/input/` — 10 nouveaux DTOs (SalarieInputDTO, PosteVirtuelInputDTO, CreneauInputDTO, AxesOrganisationnelsDTO, ContratTravailDTO, ContraintesReglementairesDTO, ReferentielActiviteDTO, ReferentielsDTO, IndisponibiliteItemDTO, IndisponibilitesDTO)
  - `scenarios/builder/ScenarioDatasetBuilderSc01.java` — resolveResource() adapté à SalarieInputDTO
  - `api/ScenarioController.java` — méthodes toDomain() ajoutées, value range mis à jour
- **Tests ajoutés** : `DataSetDeserializationPhase1Test` (3 tests)
- **Résultat** : BUILD SUCCESSFUL (39s, 0 échec) — régression : aucune
- **Décision** : couplage DTO/domaine résolu dans ScenarioController en attendant le mapper Phase 3

### Itération 2 — Phase 2 (2026-03-14) — Dataset technique de référence

- **Objectif** : formaliser le dataset technique SC-03 comme base canonique de migration
- **Fichiers modifiés** :
  - `src/test/resources/scenarios/sc03/sc03_migration_reference.json` — couvre tous les cas
  - `src/test/java/.../scenarios/sc03/Sc03DatasetIntegrityTest.java` — 14 tests
- **Résultat** : BUILD SUCCESSFUL (47s, 0 échec)
- **Décision** : le dataset SC-03 est validé comme référence de migration

### Itération 3 — Phase 3 (2026-03-14) — Refactoring architectural + mapping

- **Objectif** : refactoring SC-01 en 3 services + mapping complet Phase 3
- **Fichiers modifiés** :
  - nouveaux domaine : `ContraintesReglementairesSalarie`, `Indisponibilite`
  - modifié domaine : `SalarieReel` (5 champs Phase 3), `PlanningRequest` (6-arg ctor)
  - nouveau mapper : `ScenarioResourceMapper` (@Service)
  - nouveaux services : `PreparedSc01Scenario`, `ScenarioSc01PreparationService`, `ScenarioSc01ExecutionService`
  - allégé : `ScenarioController` (1 dépendance, 3 endpoints)
- **Tests ajoutés** : `ScenarioResourceMapperTest` (14 tests unitaires)
- **Résultat** : BUILD SUCCESSFUL (38s, 0 échec)
- **Décision** : `ScenarioResourceMapper` est le point d’entrée canonique pour tous les mappings DTO→domaine

### Itération 4 — Phase 4 (2026-03-14) — Incompatibilités structurelles HARD

- **Objectif** : exploiter les champs mappés en Phase 3 dans des règles simples
- **Fichiers modifiés** :
  - `PlanningProblem` — `@ProblemFactCollectionProperty List<Indisponibilite>`
  - `PenaliteKey` — 2 nouvelles clés HARD : `METIER_HARD_JOUR_FERIE_REFUSE`, `METIER_HARD_INDISPONIBILITE`
  - nouvelles contraintes : `JourFerieRefuse`, `IndisponibiliteSalarie`
  - `build.gradle` — ajout `optaplanner-test`
- **Tests ajoutés** : `Phase4ConstraintsTest` (7 tests via `ConstraintVerifier`)
- **Résultat** : BUILD SUCCESSFUL (37s, 0 échec)
- **Décision** : `ConstraintVerifier` adopté comme stratégie de test pour les contraintes isolées

### Itération 5 — Phase 5 (2026-03-14) — Structuration des besoins

- **Objectif** : mapper les 4 champs de structuration des besoins vers le domaine
- **Fichiers modifiés** :
  - `Creneau` — 4 nouveaux champs : `groupeBesoinId`, `blocJourId`, `ordreDansBloc`, `estSegmentDePause`
  - nouveau mapper : `ScenarioCreneauMapper` (@Service)
- **Tests ajoutés** : `ScenarioCreneauMapperTest` (8 tests unitaires)
- **Résultat** : BUILD SUCCESSFUL (50s, 0 échec)
- **Décision** : `ScenarioCreneauMapper` distinct de `ScenarioResourceMapper` (responsabilités séparées)

### Itération 6 — Phase 6 (2026-03-14) — Données nuit par salarié

- **Objectif** : rendre exploitables les données de nuit par salarié
- **Fichiers modifiés** :
  - `SalarieReel` — 3 méthodes utilitaires : `estTravailleurDeNuit()`, `heureDebutNuitEffective(fallback)`, `heureFinNuitEffective(fallback)`
- **Tests ajoutés** : `SalarieReelNuitTest` (11 tests unitaires)
- **Résultat** : BUILD SUCCESSFUL (40s, 0 échec)
- **Décision** : couche complémentaire au calcul global (`RegulatoryParameters`) — le calcul `TimeBreakdownCalculator` reste inchangé

### Itération 7 — Phase 7 (2026-03-14) — Ouverture SC-03

- **Objectif** : ouvrir le scénario SC-03 comme un vrai scénario supporté par l’API
- **Fichiers modifiés** :
  - nouveaux DTO : `Sc03ScenarioParametersDTO`, `Sc03ScenarioRequestDTO`
  - nouveau record : `PreparedSc03Scenario`
  - modifié mapper : `ScenarioResourceMapper` — ajout `toReferentiel(ReferentielsDTO)`
  - nouveaux services : `ScenarioSc03PreparationService`, `ScenarioSc03ExecutionService`
  - modifié controller : ajout `POST /sc03/solve`
- **Tests ajoutés** :
  - `ScenarioControllerSc03ValidationTest` (4 tests négatifs)
  - `ScenarioControllerSc03RuntimeTest` (1 test runtime, score hard=0)
- **Résultat** : BUILD SUCCESSFUL (41s, 0 échec)
- **Décision** : SC-01 conserve son référentiel hardcodé jusqu’en Phase 9

### Itération 8 — Phase 8 (2026-03-14) — Scoring, diagnostics et WorkMetrics enrichis

- **Objectif** : donner aux champs `travailDeNuit` et `travailleJourFerie` un impact visible
- **Fichiers modifiés** :
  - `PenaliteKey` — nouvelle clé `METIER_SOFT_NUIT_SALARIE_NON_NUIT`
  - nouvelle contrainte : `NuitSalarieNonNuit` (@SOFT)
  - `AssignmentDiagnosticsFactory` — reasonCode `JOUR_FERIE_NON_COUVERT`
  - `WorkMetrics` — ajout `nbCreneauxNuitNonNuit`
  - `WorkMetricsCalculator`, `WorkMetricsByRessourceDTO`, `ScenarioResponseMapper` — branchement
- **Tests ajoutés** :
  - `Phase8ConstraintsTest` (7 tests `ConstraintVerifier`)
  - `AssignmentDiagnosticsFactoryTest` (4 tests)
  - `WorkMetricsNuitNonNuitTest` (5 tests unitaires)
- **Résultat** : BUILD SUCCESSFUL (44s, 0 échec)
- **Décision** : `heureDebutNuit`/`heureFinNuit` non exploités solveur en Phase 8 — différé Phase 9+

---

## Conclusion de l’audit actuel du moteur

L’analyse globale du moteur montre que :

* l’architecture générale est cohérente
* la migration du dataset progresse conformément au plan
* les risques identifiés sont maîtrisables

Points de vigilance identifiés :

1. exploitation future des blocs de besoins
2. amélioration de la couche diagnostique
3. qualification métier des jours
4. nettoyage progressif du contrat d’entrée

Aucun de ces points ne nécessite de correction immédiate mais devra être suivi lors des phases suivantes du développement.
