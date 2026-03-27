# Suivi stabilisation SC-01

> Document opérationnel dérivé de l'audit `92_audit_scenario_sc-01.md`
> Date de création : 2026-03-27

---

## Tableau de bord

| Phase | Intitulé | Statut | Tâches | Date |
|-------|----------|--------|--------|------|
| A | Transparence et guards | **Terminée** | A1 ✓, A2 ✓, A3 ✓, A4 ✓, A5 ✓ | 2026-03-27 |
| B | Injection du référentiel | **Terminée** | B1 ✓, B2 ✓, B3 ✓, B4 ✓ | 2026-03-27 |
| C | Alignement architecture | À faire | C1, C2, C3 | — |
| D | Migration créneaux (décision) | En attente | D1, D2, D3 | — |

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

## Phase C — Alignement architecture

**Objectif :** aligner SC-01 sur les conventions établies par SC-03 — sans changer le comportement fonctionnel.

### Tâches

| ID | Description | Type | Priorité | Dépendances |
|----|-------------|------|----------|-------------|
| C1 | Calculer `IgnoredCreneauxDTO` dynamiquement dans SC-01 plutôt que retourner `(0, 0, 0)` statique | contrat | haute | Phase B |
| C2 | Extraire la logique de construction du référentiel hors de `ScenarioSc01PreparationService` si ce n'est pas déjà le cas via `resourceMapper` | architecture | moyenne | Phase B |
| C3 | Évaluer une base commune à `ScenarioRequestDTO` et `Sc03ScenarioRequestDTO` — interface ou classe abstraite | architecture | moyenne | Phase B |

**Détail C1 — compteurs `IgnoredCreneauxDTO` pour SC-01 :**

SC-01 génère ses créneaux via le builder (pas de partitioning externe). Les seuls compteurs
pertinents à calculer sont :

- `horsHorizon` : créneaux générés par le builder tombant hors horizon (cas théoriquement impossible, mais à défensiviser)
- `activiteInconnue` : créneaux générés avec un `codeActiviteId` absent du référentiel injecté (pertinent après Phase B)
- `aucuneRessourceDansDataset` : reste à `0` (SC-01 a toujours une ressource cible)

**Détail C3 :** ne pas forcer si l'effort dépasse le bénéfice — à valider avec l'équipe.

### Critères de validation — Phase C

| Critère | Condition de succès |
|---------|---------------------|
| `IgnoredCreneauxDTO` dynamique | La réponse SC-01 reflète les vraies anomalies détectées (pas `(0,0,0)` en dur) |
| Pas de régression | `sc01_dataset_reference.json` toujours vert |
| Build | `BUILD SUCCESSFUL` |

---

## Phase D — Migration créneaux vers dataset (décision stratégique)

**Objectif :** trancher si SC-01 doit à terme recevoir ses créneaux depuis `dataSet.creneaux`
ou conserver la génération paramétrique via le builder.

**Prérequis :** phases A, B, C terminées et validées.

### Tâches

| ID | Description | Type | Priorité | Dépendances |
|----|-------------|------|----------|-------------|
| D1 | Décider : génération conservée ou migration vers dataset | architecture | haute | Phase C |
| D2 | Si génération conservée : documenter explicitement dans `scenario_sc_01_schema.json` que `dataSet.creneaux` est ignoré | contrat | haute | D1 |
| D3 | Si migration vers dataset : implémenter le partitioning (activité inconnue + hors-horizon) sur le modèle SC-03 | contrat | haute | D1 |

**Note :** La génération paramétrique est une fonctionnalité métier légitime (semaine type).
Elle n'a pas à disparaître. L'enjeu est de documenter ou d'aligner, pas de supprimer.

### Critères de validation — Phase D

| Critère | Si génération conservée | Si migration dataset |
|---------|------------------------|---------------------|
| Schéma JSON | `dataSet.creneaux` marqué ignoré dans le schéma | `dataSet.creneaux` obligatoire et documenté |
| Comportement | Pas de changement fonctionnel | Partitioning activité + horizon actif |
| Tests | Pas de régression | Nouveau dataset de test avec créneaux explicites |
| Build | `BUILD SUCCESSFUL` | `BUILD SUCCESSFUL` |

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
| RF3 | La génération RH/RHD du builder est incompatible avec un futur partitioning si D3 est choisi | D | Faible | Moyen | Ouvert | Traiter en phase D uniquement |

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
| C1 | `IgnoredCreneauxDTO` dynamique | contrat | haute | C | À faire | Phase B ✓ |
| C2 | Extraire construction référentiel hors `PreparationService` | architecture | moyenne | C | À faire | Phase B ✓ |
| C3 | Base commune `ScenarioRequestDTO` / `Sc03ScenarioRequestDTO` | architecture | moyenne | C | À faire | Phase B ✓ |
| D1 | Décision : génération conservée ou migration dataset | architecture | haute | D | En attente | Phase C |
| D2 | Documenter `dataSet.creneaux` ignoré dans le schéma JSON | contrat | haute | D | En attente | D1 |
| D3 | Implémenter partitioning si migration dataset choisie | contrat | haute | D | En attente | D1 |
