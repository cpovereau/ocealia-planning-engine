# Suivi stabilisation SC-01

> Document opérationnel dérivé de l'audit `92_audit_scenario_sc-01.md`
> Date de création : 2026-03-27

---

## Tableau de bord

| Phase | Intitulé | Statut | Tâches | Date |
|-------|----------|--------|--------|------|
| A | Transparence et guards | **Terminée** | A1 ✓, A2 ✓, A3 ✓, A4 ✓, A5 ✓ | 2026-03-27 |
| B | Injection du référentiel | **Terminée** | B1 ✓, B2 ✓, B3 ✓, B4 ✓ | 2026-03-27 |
| C | Alignement architecture | **Terminée** | C1 ✓, C2 ✓, C3 évalué | 2026-03-30 |
| D | Génération isolée et convergence dataset-driven | **Terminée** | D1 ✓, D2 ✓, D3 N/A, D4 ✓, D5 ✓ | 2026-03-30 |

---

## Phase A — Transparence et guards ✓ TERMINÉE

> Complétée le **2026-03-27** — `BUILD SUCCESSFUL` — aucune régression

**Objectif :** rendre SC-01 transparent sur ses limitations et robuste aux entrées mal formées.
Zéro risque de régression — aucune logique fonctionnelle modifiée.

### Tâches

| ID | Description | Statut | Fichier(s) modifié(s) |
|----|-------------|--------|----------------------|
| A1 | `log.warn` si `dataSet.creneaux` est non vide | ✓ | `ScenarioSc01PreparationService` |
| A2 | `log.warn` si `dataSet.referentiels` est non vide | ✓ | `ScenarioSc01PreparationService` |
| A3 | `log.warn` si `ContraintesReglementairesDTO` est fourni | ✓ | `ScenarioSc01PreparationService` |
| A4 | Guards d'entrée dans `ScenarioSc01PreparationService` | ✓ | `ScenarioSc01PreparationService` |
| A5 | Guards complémentaires `Sc01ScenarioParametersDTO` | ✓ | Couverts par A4 (pattern service, cohérent avec SC-03) |

**Guards implémentés (A4) :**

| Guard | Exception levée |
|-------|----------------|
| `planningContext` null | `IllegalArgumentException` |
| `planningContext.horizon` null | `IllegalArgumentException` |
| `dateDebut` null | `IllegalArgumentException` |
| `dateFin` null | `IllegalArgumentException` |
| `dateDebut > dateFin` | `IllegalArgumentException` |
| `resourceRef` null | `IllegalArgumentException` |
| `resourceRef.id` blank | `IllegalArgumentException` |
| `dailyAmplitudeHours <= 0` | `IllegalArgumentException` |
| `shiftStart` null | `IllegalArgumentException` |

**Messages de log implémentés (A1/A2/A3) :**

```
[SC-01] dataSet.creneaux contient X éléments — ignorés (SC-01 génère ses créneaux via le builder)
[SC-01] dataSet.referentiels fourni mais ignoré (référentiel hardcodé utilisé)
[SC-01] contraintesReglementaires fournies mais ignorées (RegulatoryParameters.neutre utilisé)
```

### Tests Phase A

| Fichier | Tests | Résultat |
|---------|-------|----------|
| `ScenarioSc01PreparationServicePhaseATest` *(nouveau)* | 12 tests unitaires (9 guards + 2 warnings + 1 non-régression) | ✓ tous verts |
| `ScenarioControllerValidationTest` *(mis à jour)* | 6 tests d'intégration | ✓ tous verts |

**Ajustements sur `ScenarioControllerValidationTest` :**

| Test | Avant Phase A | Après Phase A |
|------|--------------|---------------|
| `should_raise_null_pointer_if_resource_reference_missing` | Attendait `NullPointerException` | Renommé `should_raise_illegal_argument_if_resource_reference_missing` — attend `IllegalArgumentException` (guard A4) |
| `should_raise_null_pointer_if_dataset_missing` | JSON sans `dailyAmplitudeHours` → guard A4 intervenait avant le NPE | JSON enrichi avec `dailyAmplitudeHours` et `shiftStart` — NPE sur dataset absent conservé (guard dataset hors périmètre Phase A) |

### Critères de validation — Phase A

| Critère | Condition de succès | Résultat |
|---------|---------------------|----------|
| Logs présents | `referentiels` non vide → `warn` dans les logs | ✓ |
| Logs présents | `creneaux` non vide → `warn` dans les logs | ✓ |
| Guards actifs | `planningContext.horizon` absent → `IllegalArgumentException` | ✓ |
| Guards actifs | `dateDebut > dateFin` → `IllegalArgumentException` | ✓ |
| Pas de régression | `sc01_dataset_reference.json` toujours vert | ✓ |
| Build | `BUILD SUCCESSFUL` | ✓ |

---

## Phase B — Injection du référentiel ✓ TERMINÉE

> Complétée le **2026-03-27** — `BUILD SUCCESSFUL` — aucune régression

**Objectif :** remplacer le référentiel "travail" hardcodé par le référentiel fourni dans `dataSet.referentiels`.
C'est la correction structurelle principale — les WorkMetrics deviennent cohérents avec le contrat réel.
Un créneau ne doit jamais être injecté dans le solveur sans codeActiviteId valide.

### Tâches

| ID | Description | Statut | Fichier(s) modifié(s) |
|----|-------------|--------|----------------------|
| B1 | Remplacer la construction manuelle du référentiel par `resourceMapper.toReferentiel(referentiels)` | ✓ | `ScenarioSc01PreparationService` |
| B2 | Fallback référentiel minimal "travail" + `log.warn` si `referentiels` absent ou vide | ✓ | `ScenarioSc01PreparationService` |
| B3 | Corriger le builder : `codeActiviteId = "travail"` explicitement sur les créneaux générés | ✓ | `ScenarioDatasetBuilderSc01` |
| B4 | Ajouter un bloc `referentiels` dans `sc01_dataset_reference.json` avec l'activité "travail" | ✓ | `sc01_dataset_reference.json` |

**Analyse B3 — état avant / après :**

| Aspect | Avant Phase B | Après Phase B |
|--------|--------------|---------------|
| Constructeur `Creneau` utilisé | 13 params → `codeActiviteId = null`, `activite = "travail"` | 14 params → `codeActiviteId = "travail"`, `activite = "travail"` |
| Lookup WorkMetrics | Fallback sur `activite` (implicite) | Lookup direct sur `codeActiviteId` (explicite) |

**Logique du fallback B2 :**

La méthode privée `buildReferentielSc01(ReferentielsDTO)` dans `ScenarioSc01PreparationService` :
- Si `referentiels` non null et non vide → `resourceMapper.toReferentiel(referentiels)` (B1)
- Sinon → `log.warn` + référentiel minimal `{ "travail" → compteDansCharge=true, genereDetteRepos=false }` (B2)

**Suppression du warning A2 :**

Le warning `[SC-01] dataSet.referentiels fourni mais ignoré` a été supprimé : le référentiel est désormais utilisé.

**Message de fallback B2 :**

```
[SC-01] dataSet.referentiels absent ou vide — fallback sur un référentiel minimal compatible SC-01
```

### Tests Phase B

| Fichier | Tests | Résultat |
|---------|-------|----------|
| `ScenarioSc01PreparationServicePhaseBTest` *(nouveau)* | 5 tests unitaires (B1 × 1, B2 × 2, B3 × 1, cohérence B1+B3 × 1) | ✓ tous verts |
| `ScenarioSc01PreparationServicePhaseATest` *(inchangé)* | 12 tests unitaires Phase A | ✓ tous verts |
| `ScenarioControllerValidationTest` *(inchangé)* | 6 tests d'intégration | ✓ tous verts |

### Critères de validation — Phase B

| Critère | Condition de succès | Résultat |
|---------|---------------------|----------|
| Référentiel injecté | `ScenarioSc01PreparationService` ne contient plus de construction inline hardcodée | ✓ |
| Référentiel injecté | `ScenarioSc01PreparationService` appelle `resourceMapper.toReferentiel()` | ✓ |
| Référentiel vide toléré | SC-01 répond sans exception si `dataSet.referentiels` est absent — avec un `warn` | ✓ |
| `codeActiviteId` explicite | Créneaux générés ont `codeActiviteId = "travail"` (non null) | ✓ |
| Cohérence référentiel / créneaux | Référentiel injecté couvre le `codeActiviteId` de tous les créneaux | ✓ |
| Dataset de test mis à jour | `sc01_dataset_reference.json` contient un bloc `referentiels` non vide | ✓ |
| Pas de régression | `BUILD SUCCESSFUL` — suite complète verte | ✓ |

---

## Phase C — Alignement architecture ✓ TERMINÉE

> Complétée le **2026-03-30** — `BUILD SUCCESSFUL` — aucune régression

**Objectif :** aligner SC-01 sur les conventions établies par SC-03 — sans changer le comportement fonctionnel.

### Tâches

| ID | Description | Statut | Fichier(s) modifié(s) |
|----|-------------|--------|----------------------|
| C1 | `IgnoredCreneauxDTO` calculé dynamiquement dans `ScenarioSc01PreparationService` ; passé dans `PreparedSc01Scenario` ; utilisé dans `ScenarioSc01ExecutionService` | ✓ | `ScenarioSc01PreparationService`, `PreparedSc01Scenario`, `ScenarioSc01ExecutionService` |
| C2 | Référentiel déjà correctement isolé via `buildReferentielSc01()` (Phase B) — pas de refactoring supplémentaire nécessaire | ✓ (rien à faire) | — |
| C3 | **Évaluation** : base commune `ScenarioRequestDTO` / `Sc03ScenarioRequestDTO` → **reportée** (voir conclusion) | Évalué | — |

**Logique C1 — `computeIgnoredCreneaux()` :**

Méthode privée ajoutée dans `ScenarioSc01PreparationService` :

| Compteur | Règle de calcul SC-01 |
|----------|-----------------------|
| `horsHorizon` | Créneaux dont `date < dateDebut` ou `date > dateFin` (théoriquement 0 — le builder respecte l'horizon) |
| `activiteInconnue` | Créneaux dont `codeActiviteId` (ou fallback `activite`) est absent du référentiel injecté |
| `aucuneRessourceDansDataset` | Toujours `0` — SC-01 a une ressource cible explicite via `resourceRef` |

`PreparedSc01Scenario` est enrichi du champ `IgnoredCreneauxDTO ignoredCreneaux`.
`ScenarioSc01ExecutionService` utilise `prepared.ignoredCreneaux()` au lieu de `new IgnoredCreneauxDTO(0,0,0)`.

**Conclusion C3 — Évaluation base commune DTOs :**

`ScenarioRequestDTO` (SC-01) et `Sc03ScenarioRequestDTO` (SC-03) partagent 3 champs communs :
`scenarioType`, `planningContext`, `dataSet`. La seule différence est le type de `scenarioParameters`.

**Verdict : à reporter.**
- Aucune logique partagée ne bénéficierait d'un type commun aujourd'hui (chaque service reçoit son type propre)
- Le risque Jackson sur la désérialisation d'un type polymorphe est réel et disproportionné au gain
- Pertinent uniquement si un middleware ou un contrôleur commun émerge — à décider en Phase D ou après

### Tests Phase C

| Fichier | Tests | Résultat |
|---------|-------|----------|
| `ScenarioSc01PreparationServicePhaseCTest` *(nouveau)* | 5 tests unitaires (C1a × 1, C1b × 1, C1c × 1, C1d × 1, nominal × 1) | ✓ tous verts |
| Suite complète | — | ✓ `BUILD SUCCESSFUL` |

### Critères de validation — Phase C

| Critère | Condition de succès | Résultat |
|---------|---------------------|----------|
| `IgnoredCreneauxDTO` dynamique | Réponse SC-01 reflète les vraies anomalies (plus de `(0,0,0)` statique) | ✓ |
| `activiteInconnue > 0` simulable | Test T-C1-03 : référentiel sans "travail" → compteur positif | ✓ |
| `aucuneRessourceDansDataset = 0` garanti | Test T-C1-01 | ✓ |
| `horsHorizon = 0` par construction | Test T-C1-04 | ✓ |
| Pas de régression | `sc01_dataset_reference.json` vert — suite complète verte | ✓ |
| Build | `BUILD SUCCESSFUL` | ✓ |

---

## Phase D — Génération isolée et convergence dataset-driven ✓ TERMINÉE

> Complétée le **2026-03-30** — `BUILD SUCCESSFUL` — aucune régression

**Objectif :** extraire la génération de créneaux SC-01 dans un service dédié, rendre l'architecture prête pour une convergence future vers le modèle dataset-driven, sans migrer SC-01 maintenant.

**Décision D1 :** **Option 1 retenue — génération conservée.** SC-01 reste paramétrique. La génération est isolée dans `CreneauGenerationService` pour préparer la migration future.

### Tâches

| ID | Description | Statut | Fichier(s) modifié(s) |
|----|-------------|--------|----------------------|
| D1 | Décision : génération conservée (Option 1) | ✓ | — |
| D2 | Extraction de la génération dans `CreneauGenerationService` | ✓ | `CreneauGenerationService` (nouveau), `ScenarioSc01PreparationService` |
| D3 | Option 2 (migration dataset) — non retenue | N/A | — |
| D4 | Section Phase D ajoutée à `90_plan_migration_temporaire_windev_vers_moteur.md` | ✓ | `90_plan_migration_temporaire_windev_vers_moteur.md` |
| D5 | Contrat SC-01 clarifié : `dataSet.creneaux` ignoré, créneaux générés par le moteur | ✓ | `90_plan_migration_temporaire_windev_vers_moteur.md` §D.2 |

### Architecture après Phase D

```text
SC-01 : scenarioParameters → CreneauGenerationService → créneaux → solveur
SC-03 : dataSet.creneaux → solveur
```

`CreneauGenerationService` isole la génération. Il peut à terme exposer une API retournant `List<CreneauInputDTO>` pour alimenter `dataSet.creneaux` (Option 2, non implémentée).

### Tests Phase D

| Fichier | Tests | Résultat |
|---------|-------|----------|
| `CreneauGenerationServiceTest` *(nouveau)* | 4 tests unitaires (génération, codeActiviteId, comptage créneaux, alertes) | ✓ tous verts |
| Tests A/B/C *(mis à jour)* | Constructeur étendu avec `CreneauGenerationService` | ✓ tous verts |
| Suite complète | — | ✓ `BUILD SUCCESSFUL` |

### Critères de validation — Phase D

| Critère | Condition de succès | Résultat |
|---------|---------------------|----------|
| Service isolé | `CreneauGenerationService` existe et encapsule le builder | ✓ |
| SC-01 adapté | `ScenarioSc01PreparationService` injecte et utilise le service | ✓ |
| Comportement identique | Aucun changement fonctionnel — même résultat de génération | ✓ |
| Contrat documenté | `dataSet.creneaux` ignoré documenté dans `90_plan_migration_temporaire_windev_vers_moteur.md` | ✓ |
| Build | `BUILD SUCCESSFUL` | ✓ |

---

## Risques

### Risques techniques

| # | Risque | Phase concernée | Probabilité | Impact | Statut | Mitigation |
|---|--------|----------------|-------------|--------|--------|------------|
| RT1 | Le builder génère des créneaux sans `codeActiviteId` → WorkMetrics à zéro après B1 | B | Haute | Critique | **Corrigé (B3)** | Builder utilise désormais le constructeur 14 params avec `codeActiviteId = "travail"` |
| RT2 | Le `codeActiviteId` utilisé par le builder diffère des codes du référentiel fourni par WinDev | B | Haute | Critique | **Documenté** | Code `"travail"` retenu — dataset de référence B4 et fallback B2 alignés |
| RT3 | Les guards Phase A rejettent des appels WinDev actuellement mal formés mais fonctionnels | A | Faible | Important | **Non déclenché** (Phase A terminée sans incident) | Surveiller au déploiement |
| RT4 | `resourceMapper.toReferentiel()` se comporte différemment de la construction manuelle actuelle | B | Moyenne | Important | Ouvert | Comparer les résultats sur le dataset de référence |

### Risques fonctionnels

| # | Risque | Phase concernée | Probabilité | Impact | Statut | Mitigation |
|---|--------|----------------|-------------|--------|--------|------------|
| RF1 | Le score OptaPlanner change après injection du référentiel réel (activités avec `genereDetteRepos=true`) | B | Moyenne | Important | **Surveillé** | Fallback B2 préserve le comportement historique ; delta à documenter si WinDev fournit un référentiel différent |
| RF2 | WinDev ne fournit pas `referentiels` dans ses appels SC-01 actuels → fallback nécessaire | B | Haute | Important | **Couvert (B2)** | Fallback minimal "travail" implémenté avec warn |
| RF3 | La génération RH/RHD du builder est incompatible avec un futur partitioning si Option 2 est choisie | D | Faible | Moyen | **Documenté** | Option 2 non retenue en Phase D — à reconsidérer si convergence dataset décidée |

---

## Récapitulatif des tâches

| ID | Description courte | Type | Priorité | Phase | Statut | Dépendances |
|----|--------------------|------|----------|-------|--------|-------------|
| A1 | Warn `dataSet.creneaux` ignoré | contrat | critique | A | ✓ | — |
| A2 | Warn `dataSet.referentiels` ignoré | contrat | critique | A | ✓ | — |
| A3 | Warn `ContraintesReglementairesDTO` ignoré | contrat | haute | A | ✓ | — |
| A4 | Guards d'entrée `ScenarioSc01PreparationService` | contrat | haute | A | ✓ | — |
| A5 | Guards complémentaires (couverts par A4) | contrat | haute | A | ✓ | A4 |
| B1 | Remplacer référentiel hardcodé par `resourceMapper.toReferentiel()` | contrat | critique | B | ✓ | Phase A ✓ |
| B2 | Fallback référentiel minimal "travail" + warn si `referentiels` absent | contrat | critique | B | ✓ | B1 |
| B3 | Corriger `codeActiviteId` explicite dans les créneaux du builder | builder | critique | B | ✓ | B1 |
| B4 | Mettre à jour `sc01_dataset_reference.json` avec `referentiels` | tests | haute | B | ✓ | B1, B3 |
| C1 | `IgnoredCreneauxDTO` dynamique | contrat | haute | C | ✓ | Phase B ✓ |
| C2 | Extraire construction référentiel hors `PreparationService` | architecture | moyenne | C | ✓ (Phase B) | Phase B ✓ |
| C3 | Base commune `ScenarioRequestDTO` / `Sc03ScenarioRequestDTO` | architecture | moyenne | C | Reporté | Phase B ✓ |
| D1 | Décision : génération conservée (Option 1) | architecture | haute | D | ✓ | Phase C |
| D2 | Extraction `CreneauGenerationService` + adaptation SC-01 | architecture | haute | D | ✓ | D1 |
| D3 | Option 2 (migration dataset) — non retenue | contrat | haute | D | N/A | D1 |
| D4 | Documentation Phase D dans plan migration | contrat | haute | D | ✓ | D1 |
| D5 | Clarification contrat SC-01 (`dataSet.creneaux` ignoré) | contrat | haute | D | ✓ | D1 |
