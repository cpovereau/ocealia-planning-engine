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

### Itération 10 — Refactoring architectural (2026-03-18) — PlanningService solveur pur

- **Objectif** : séparer la responsabilité du solveur de la construction des DTO de restitution
- **Fichiers modifiés** :
  - `PlanningService` — suppression de `buildScoreBreakdown()` et `resolveQuantity()` ; retourne `PlanningResponse(solution, explanation)` ; aucune dépendance vers `scenarios.dto`
  - `PlanningResponse` — converti en `record` : `solution` + `explanation` brute ; suppression des champs `alerts` et `scoreBreakdown`
  - `ScoreBreakdownFactory` — ajout de la méthode statique `build(ScoreExplanation)` ; accueille la logique déplacée depuis `PlanningService`
  - `ScenarioSc01ExecutionService` — `solved.scoreBreakdown()` → `ScoreBreakdownFactory.build(solved.explanation())`
  - `ScenarioSc03ExecutionService` — idem
  - `PlanningServiceSolverStabilityTest` — `flattenBreakdown()` adapté au nouveau contrat ; assertion `alerts()` supprimée
  - `PlanningServiceScoreRegressionTest` — `response.scoreBreakdown()` → `ScoreBreakdownFactory.build(response.explanation())`
- **Résultat** : BUILD SUCCESSFUL (1m 3s, 0 échec)
- **Décision** : `ScoreBreakdownFactory` est le seul point de contact entre `ScoreExplanation` (domaine solveur) et `ScoreBreakdownItemDTO` (couche API) — frontière explicite et testable

### Itération 9 — Phase 9 (2026-03-18) — Consolidation pipeline SC-03 et diagnostics complets

- **Objectif** : pipeline end-to-end validé, `ignoredCreneaux` réels, null éliminé de la réponse
- **Fichiers modifiés** :
  - `PlanningService` — suppression du bloc debug force-1041 (36 lignes) — `solve()` propre
  - `ScenarioResponseMapper` — `toResponse()` accepte `IgnoredCreneauxDTO` en 11e arg ; null-safety `ressourceAffecteeId` → `"A_AFFECTER"` garanti
  - `PreparedSc03Scenario` — ajout 4e champ `IgnoredCreneauxDTO ignoredCreneaux`
  - `ScenarioSc03PreparationService` — calcul pré-résolution `horsHorizon` + `activiteInconnue` à partir des DTO bruts
  - `ScenarioSc03ExecutionService` — transmission `prepared.ignoredCreneaux()` au mapper
  - `ScenarioSc01ExecutionService` — ajout `new IgnoredCreneauxDTO(0, 0, 0)` comme 11e argument
- **Tests ajoutés** :
  - `Phase9IntegrationTest` (4 tests `@SpringBootTest` + MockMvc)
  - `sc03_hors_horizon.json` — dataset horsHorizon=1
  - `sc03_activite_inconnue.json` — dataset activiteInconnue=1
- **Résultat** : BUILD SUCCESSFUL (1m 25s, 0 échec)
- **Décision** : `ignoredCreneaux` est un diagnostic pré-résolution (calculé sur les DTO avant `planningService.solve()`) — jamais dérivé de la réponse solveur

### Itération 12 — `ignoredCreneaux.aucuneRessourceDansDataset` implémenté pour SC-03 (2026-03-18)

- **Objectif** : compléter le compteur contractuel `aucuneRessourceDansDataset` encore laissé à `0` en dur dans `ScenarioSc03PreparationService`
- **Règle retenue** : un créneau est compté si aucune ressource du dataset (salarié ou poste virtuel) ne peut potentiellement le couvrir au niveau structurel — sa `codeActiviteId` n'apparaît dans la liste d'activités d'aucune ressource. Une ressource avec une liste vide/nulle est considérée non contrainte (couvre toute activité). Contrôle strictement pré-résolution sur les DTO bruts.
- **Fichiers modifiés** :
  - `ScenarioSc03PreparationService` — calcul `aucuneRessourceDansDataset` + méthode privée `auMoinsUneRessourceCompatible()`
- **Tests ajoutés / adaptés** :
  - `sc03_aucune_ressource.json` — dataset avec un créneau ACT-TECH non couvert par le salarié (activitesCompatibles: ["ACT-SOIN"] seulement)
  - `Phase9IntegrationTest` — nouveau test `sc03_aucuneRessourceCompatible_doitComptabiliserAucuneRessource()` ; assertions `aucuneRessourceDansDataset = 0` ajoutées aux 3 tests existants
- **Résultat** : BUILD SUCCESSFUL (1m 2s, 0 échec)
- **Décision** : `aucuneRessourceDansDataset` est calculé en préparation, avant le solveur — cohérent avec `horsHorizon` et `activiteInconnue`

---

### Itération 11 — Décision `Creneau.duree` source de vérité (2026-03-18)

- **Objectif** : aligner le code avec la décision architecturale — `Creneau.duree` est la source de vérité pour tous les agrégats et la restitution
- **Contexte** : `CreneauInputDTO` ne porte pas de champ `duree` ; `ScenarioCreneauMapper` le calcule depuis `heureDebut`/`heureFin`. `ScenarioResponseMapper.formatDuree()` recalculait indépendamment via `Duration.between()` — source de divergence potentielle.
- **Fichiers modifiés** :
  - `ScenarioResponseMapper` — `formatDuree()` remplacée : utilise directement `creneau.getDuree()` ; import `java.time.Duration` supprimé
  - `ScenarioCreneauMapper` — ajout d’un log `warn` si `duree <= 0` après calcul (vérification défensive)
- **Vérifications confirmées (non modifiées)** :
  - `TimeBreakdownCalculator` — `minutesTravaillees = creneau.getDuree()` ✅
  - `WorkMetricsCalculator` — passe par `TimeBreakdownCalculator` ✅
  - `solutionSummary.heuresTravailleesTotales` — `Creneau::getDuree` ✅
  - `workMetrics.global.heuresTravailleesTotales` — `Creneau::getDuree` ✅
- **Résultat** : BUILD SUCCESSFUL (1m 14s, 0 échec)
- **Décision** : `Creneau.duree` est l’unique référence pour les agrégats et la restitution — documenté dans `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`

---

### Itération 10 — Phase 10 (2026-03-19) — Contrainte SOFT "Jours consécutifs travaillés max"

- **Objectif** : implémenter la contrainte R1 "Jours consécutifs max" (SOFT), identifiée priorité haute, jusqu’ici absente du solveur malgré la métrique `maxJoursConsecutifsObservees` déjà observable
- **Seuil retenu** : `ContraintesReglementairesSalarie.joursConsecutifsMaximum` (par salarié, déjà mappé depuis WinDev Phase 3) — cohérent avec la granularité individuelle ; pas de seuil global dans `SeuilsDeTolerance`
- **Définition du jour travaillé** : date comportant au moins un créneau avec `compteDansCharge = true` — dédupliquée par date, bornée à l’horizon ; identique à la définition de `WorkMetricsCalculator`
- **Fichiers modifiés** :
  - `scoring/PenaliteKey.java` — ajout `LEGAL_SOFT_JOURS_CONSECUTIFS_MAX(JOUR)`
  - `domain/contexte/Penalites.java` — ajout champ + getter `depassementMaxJoursConsecutifs`, constructeur étendu à 11 args
  - `domain/contexte/PlanningContext.java` — `defaultPenalites()` : 11e arg = 5 000
  - `constraints/legales/JoursConsecutifsMax.java` — nouvelle contrainte SOFT
  - `constraints/ConstraintProviderImpl.java` — import + `JoursConsecutifsMax.maxJoursConsecutifs(factory)` dans la section légales SOFT
- **Tests mis à jour** :
  - `solver/StrategieScoringComparaisonTest.java` — 11e arg Penalites
  - `score/DominancePenibilitesTest.java` — 11e arg Penalites
- **Tests ajoutés** :
  - `constraints/Phase10ConstraintsTest.java` — 7 cas : seuil exact, dépassement ×1, dépassement ×2, créneaux multiples même jour, activité hors charge, salarié sans contrainte, non-régression SC-01
- **Résultat** : BUILD SUCCESSFUL (0 échec, suite complète)
- **Cohérence WorkMetrics** : `maxJoursConsecutifsObservees` reste descriptif (post-résolution) ; la contrainte est décisionnelle (pendant résolution) — les deux partagent la même définition du jour travaillé

### Itération 11 — Phase 11 (2026-03-19) — Contrainte SOFT "Alternance jour / nuit" (R5+R6)

- **Objectif** : implémenter les règles R5 et R6 du référentiel "Alternance jour/nuit" (SOFT), identifiées priorité haute dans `40_REGLES_COMBINATOIRES.md`, jusqu’ici absentes du solveur
- **Règle retenue** : deux jours calendaires consécutifs J et J+1, tous deux travaillés (`compteDansCharge = true`), avec changement de `TypePlageHoraire` — JOUR→NUIT ou NUIT→JOUR. Un jour est classé NUIT si au moins un créneau NUIT avec charge est présent ; JOUR sinon.
- **Simplification v1** : pénalité proportionnelle `base × nbAlternances`. La pondération croissante (1→3→5) documentée dans R5 est différée à une révision ultérieure.
- **Périmètre** : tous salariés — pas de seuil par salarié, la règle s’applique globalement.
- **Pénalité par défaut** : 3 000 (inférieure aux contraintes légales strictes, supérieure aux contraintes de confort)
- **Fichiers modifiés** :
  - `scoring/PenaliteKey.java` — ajout `LEGAL_SOFT_ALTERNANCE_JOUR_NUIT(OCCURRENCE)`
  - `domain/contexte/Penalites.java` — ajout champ + getter `penaliteAlternanceJourNuit`, constructeur étendu à 12 args
  - `domain/contexte/PlanningContext.java` — `defaultPenalites()` : 12e arg = 3 000
  - `constraints/legales/AlternanceJourNuit.java` — nouvelle contrainte SOFT
  - `constraints/ConstraintProviderImpl.java` — import + `AlternanceJourNuit.alternanceJourNuit(factory)` dans la section légales SOFT
- **Tests mis à jour** :
  - `solver/StrategieScoringComparaisonTest.java` — 12e arg Penalites
  - `score/DominancePenibilitesTest.java` — 12e arg Penalites
- **Tests ajoutés** :
  - `constraints/Phase11ConstraintsTest.java` — 7 cas : pas d’alternance, JOUR→NUIT, NUIT→JOUR, double alternance, jour mixte JOUR+NUIT classé NUIT, activité sans charge, gap d’un jour OFF
- **Cohérence WorkMetrics** : aucune métrique "alternance" n’existe côté WorkMetrics — la contrainte est purement décisionnelle. Si une métrique descriptive est ajoutée ultérieurement, elle devra utiliser la même définition (NUIT dominant, jours consécutifs, compteDansCharge).

---

### Itération 12 — Phase 12 (2026-03-19) — Contrainte SOFT "Amplitude journalière maximale" (R10)

- **Objectif** : implémenter la règle R10 "Amplitude journalière" (SOFT, priorité moyenne), identifiée dans `40_REGLES_COMBINATOIRES.md`, jusqu’ici absente du solveur
- **Définition** : amplitude = max(heureFin) - min(heureDebut) des créneaux `compteDansCharge = true` sur un jour donné. Les créneaux à cheval sur minuit sont corrigés (+1440 min). Amplitude ≠ somme des durées.
- **Seuil** : `ContraintesReglementairesSalarie.amplitudeJournaliereMaximum` (Double, heures → converti en minutes). Si null → contrainte non active pour ce salarié.
- **Pénalité par défaut** : 50 × minutes de dépassement (priorité moyenne, sous SOFT fort à 3 000–5 000)
- **Fichiers modifiés** :
  - `scoring/PenaliteKey.java` — ajout `PHYSIQUE_SOFT_AMPLITUDE_JOURNALIERE(MINUTE_PONDEREE)` (première clé PHYSIQUE SOFT)
  - `domain/contexte/Penalites.java` — ajout champ + getter `penaliteAmplitude`, constructeur étendu à 13 args
  - `domain/contexte/PlanningContext.java` — `defaultPenalites()` : 13e arg = 50
  - `constraints/legales/AmplitudeJournaliere.java` — nouvelle contrainte SOFT
  - `constraints/ConstraintProviderImpl.java` — import + `AmplitudeJournaliere.amplitudeJournaliere(factory)`
- **Tests mis à jour** :
  - `solver/StrategieScoringComparaisonTest.java` — 13e arg Penalites
  - `score/DominancePenibilitesTest.java` — 13e arg Penalites
- **Tests ajoutés** :
  - `constraints/Phase12ConstraintsTest.java` — 7 cas : sous seuil, dépassement simple, multi-créneaux avec pause (borne ≠ somme), seuil absent, activité sans charge, créneau unique, calcul cross-midnight

---

### Itération 13 — Phase 13 (2026-03-19) — Contrainte SOFT "Dimanches travaillés maximum" (R9)

- **Objectif** : finaliser la contrainte R9 "Dimanches maximum" (SOFT, priorité moyenne), qui existait en ébauche partielle avec deux cross joins et zéro test
- **Problèmes résolus** : double cross join (`.join(ref)` + `.join(PlanningContext)` après groupBy) → remplacé par `ifExists(ref)` + `.join(PlanningContext)` ; helper `compterDimanchesDistincts` supprimé ; import inutile `Stream/Collectors/List/Set` supprimé
- **Définition retenue** (cohérente avec `40_WORKMETRICS.md` et métrique `nbDimanchesTravailles`) : dimanche calendaire (`DayOfWeek.SUNDAY`) comportant au moins un créneau `compteDansCharge = true`. Comptage par **date distincte** via `countDistinct` natif OptaPlanner.
- **Seuil** : `SeuilsDeTolerance.maxDimanchesTravailles` (global, conventionnel/métier). Pas de champ par salarié dans `ContraintesReglementairesSalarie` pour ce cas.
- **Fichiers modifiés** :
  - `domain/contexte/SeuilsDeTolerance.java` — ajout setter `setMaxDimanchesTravailles(int)` (champ déjà mutable, setter manquant)
  - `constraints/legales/DimanchesTravaillesMax.java` — réécriture complète : `ifExists`, `countDistinct`, filtre dimanche à la source
- **Tests ajoutés** :
  - `constraints/Phase13ConstraintsTest.java` — 8 cas : aucun dimanche, sous seuil, seuil exact, dépassement ×1, dépassement ×2, multi-créneaux même dimanche → 1 seul, activité sans charge, jour non dimanche
- **Résultat** : BUILD SUCCESSFUL (0 échec, suite complète)
- **Cohérence WorkMetrics** : `nbDimanchesTravailles` reste descriptif post-résolution avec la même définition (DayOfWeek.SUNDAY + compteDansCharge + dates distinctes) — contrainte et métrique partagent la même réalité métier

---

## Juillet 2026 — Contrat de sortie et cohérence documentaire

### 2026-07-29 — Restitution du code activité corrigée + reprise des obsolescences documentaires

- **Constat** : `ScenarioResponseMapper` et `AssignmentDiagnosticsFactory` alimentaient le champ de sortie `activite` depuis `Creneau.getActivite()` (libellé déprécié) au lieu de `codeActiviteId` (clé stable du référentiel). Un client conforme n'envoyant que `codeActiviteId` — cas nominal SC-03 — recevait `"activite": null`. Reproduit sur un résultat réel du FileAdapter (`outbox/sc_03_fileadapter_input_RESULT.json`).
- **Correction** : ajout de `Creneau.getCodeActiviteEffectif()` — `codeActiviteId` prioritaire, repli sur `activite`, `null` si les deux sont absents ou vides. Même règle que celle appliquée avant résolution par `ScenarioSc03PreparationService`, donc la sortie expose désormais la clé qui a réellement servi au calcul.
- **Fichiers modifiés** :
  - `domain/creneau/Creneau.java` — nouvelle méthode `getCodeActiviteEffectif()`
  - `scenarios/mapper/ScenarioResponseMapper.java` — `toCreneauPlanningDTO()`
  - `scenarios/mapper/AssignmentDiagnosticsFactory.java` — 2 sites de construction du diagnostic
- **Tests ajoutés** : `ScenarioResponseActiviteRestitutionTest` — 5 cas (code seul, libellé seul, les deux, aucun, diagnostics)
- **Résultat** : BUILD SUCCESSFUL — 308 tests, 0 échec. Vérifié bout en bout sur `docs/Windev_part/exemples/sc_03_exemple_valide.json` : `"activite": "ACT-SOIN"` en planning comme en diagnostics.
- **Impact contrat** : aucun changement de structure — le champ garde son nom `activite`, seule sa valeur devient conforme à `50_ScenarioResponseContract.md` §4.5 (nouvelle section).
- **Reste à faire** (non traité ici) : écho du bloc `referentiels` en sortie et WorkMetrics par activité — l'« entrepôt des activités » de `90_suivi_developpement_moteur.md` n'est implémenté que côté entrée.

#### Obsolescences documentaires corrigées le même jour

Le chantier de stabilisation SC-01 (phases A→D, 2026-03-30) n'avait pas été répercuté dans les documents d'audit et de migration, qui décrivaient encore SC-01 comme ignorant `dataSet.referentiels` :

| Document | Correction |
|---|---|
| `92_audit_contrat_entree.md` | Bandeau d'obsolescence + §1.8, T-15, §3.2 et §4 annotés « ✅ Corrigé » |
| `92_audit_scenario_sc-01.md` | Bandeau récapitulant C1→C4, I1→I4 ; passages faux barrés ; R1 marquée appliquée |
| `90_plan_migration_temporaire_windev_vers_moteur.md` | Bloc `referentiels` : SC-01 n'est plus « hardcodé jusqu'en Phase 9 » |
| `50_ScenarioResponseContract.md` | §4.4 : les compteurs `ignoredCreneaux` ne sont plus « toujours 0 » en SC-01 |

Les entrées de journal antérieures ne sont pas modifiées : elles restent l'historique daté des décisions.

---

### 2026-07-30 — Qualification du repos hebdomadaire SC-01 + sévérité des alertes

- **Constat** : sur un export réel (`data/file-adapter/archive/2026/07/102100/`), un salarié à 4 jours travaillés (`MONDAY, TUESDAY, THURSDAY, FRIDAY`) déclenchait `TOO_MANY_NON_WORKED_DAYS` — restitué côté WinDev comme une anomalie. Le déclencheur était un seuil codé en dur, `nonWorked.size() > 2` : toute configuration à moins de 5 jours travaillés le franchissait, donc tout temps partiel.
- **Cause réelle** : le seuil ne mesurait pas un défaut métier mais la limite de `mapNonWorkedDayToQualification`, qui ne savait qualifier que 1 ou 2 jours de repos. Au-delà, les jours excédentaires tombaient dans un repli `RH`. Le tri par `DayOfWeek.getValue()` qualifiait donc **mercredi en `RH`** et samedi en `RHD` — alors que `DetteReposSurReposHebdomadaire` suppose explicitement « samedi ou dimanche ».
- **Correction (1) — qualification** : `qualifyNonWorkedDays()` remplace `mapNonWorkedDayToQualification()`. Le repos suit le week-end : dimanche non travaillé → `RHD`, samedi → `RH`, jours restants → `NON_TRAVAILLE` (horaire contractuel). Si le week-end est entièrement travaillé, le `RH` est reporté sur le premier jour non coché. Requalifier `RH` en `NON_TRAVAILLE` est sans effet sur la définition du travail : `20_DECISIONS_CONCEPTION_OPTAPLANNER.md` §5.2 range les deux dans la même famille non travaillée, et §5.5 interdit aux WorkMetrics d'interpréter `QualificationJour` comme du travail.
- **Correction (2) — alerte** : le seuil `> 2` disparaît. L'alerte est dérivée de la qualification et liste les jours hors repos hebdomadaire. `INSUFFICIENT_WEEKLY_REST` (0 jour non coché) reste la seule anomalie de repos. La qualification de la semaine type sort de la boucle hebdomadaire : les alertes de configuration ne sont plus dupliquées sur un horizon multi-semaines.
- **Correction (3) — sévérité** : `AlertSeverity { INFO, WARNING, ERROR }` portée par `ScenarioAlert` et `ScenarioAlertDTO`. `SHIFT_END_EXCEEDED` et `LUNCH_BREAK_OUTSIDE_AMPLITUDE` en `WARNING`, `INSUFFICIENT_WEEKLY_REST` en `ERROR`, `TOO_MANY_NON_WORKED_DAYS` en `INFO`. Sans ce champ, le client n'avait aucun moyen de distinguer une information d'un défaut.
- **Fichiers modifiés** :
  - `scenarios/builder/ScenarioDatasetBuilderSc01.java` — `qualifyNonWorkedDays()`, `emitWeeklyRestAlerts()`, enum `AlertSeverity`, record `ScenarioAlert`
  - `scenarios/dto/ScenarioAlertDTO.java` — champ `severity`
  - `scenarios/service/ScenarioSc01ExecutionService.java` — propagation de la sévérité
- **Tests ajoutés** : `ScenarioDatasetBuilderSc01WeeklyRestTest` — 7 cas (horaire réduit en `INFO`, repos week-end préservé, aucun créneau sur jour non coché, semaine standard sans alerte, absence de repos en `ERROR`, report du `RH` si week-end travaillé, non-duplication sur 3 semaines).
- **Résultat** : BUILD SUCCESSFUL — 315 tests, 0 échec. Planning généré strictement inchangé : les jours requalifiés `NON_TRAVAILLE` étaient déjà écartés par le filtre `workedDays`.
- **Impact contrat** : additif et rétrocompatible — `severity` est optionnel, absent il doit être lu comme `WARNING`. **`50_ScenarioResponse.schema.json` déclarait `additionalProperties: false` sur `Alert`** : sans mise à jour, toute réponse portant `severity` aurait échoué à la validation. Schéma corrigé.
- **Écarté volontairement** : rendre le seuil dépendant de la quotité de travail. Aucune donnée de quotité n'existe dans le contrat d'entrée (`quotite`, `tempsPartiel`, `dureeHebdo` : 0 occurrence dans le dépôt), et l'introduire violerait `20_dataset_builder.md` §6.4. À traiter comme un sujet de contrat d'entrée si WinDev transmet un jour la donnée.
- **Reste à faire** : le code `TOO_MANY_NON_WORKED_DAYS` conserve un nom alarmant malgré sa sévérité `INFO` — renommage non fait pour ne pas casser un filtrage sur chaîne côté WinDev. Côté client, le filtrage des `INFO` reste à implémenter, sans quoi le symptôme demeure visible.

#### Obsolescences documentaires corrigées le même jour

| Document | Correction |
|---|---|
| `50_ScenarioResponse.schema.json` | `Alert` : ajout de `severity` (bloquant — `additionalProperties: false`) |
| `50_ScenarioResponseContract.md` | §4.2 : champ `severity`, table des 4 codes et de leur gravité, règle « `INFO` ≠ anomalie », unicité des alertes de configuration |
| `50_openapi_windev_moteur_v_1.yaml` | Schéma `ScenarioAlert` : `severity`, 4 codes documentés au lieu de 2, sémantique de `date` précisée |
| `50_ScenarioTechnicalContract.md` | §7 : exemple de restitution complété avec `severity` |
| `20_dataset_builder.md` | §7.1 : table codes/sévérités et règle de qualification RH/RHD adossée au week-end |
| `92_audit_scenario_sc-01.md` | §2.4 : règle RH/RHD de l'audit barrée et annotée ; alertes annotées `severity` |

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
