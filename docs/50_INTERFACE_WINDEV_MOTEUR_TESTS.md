# 50 — Validation automatisée de l'interface moteur

Ce document décrit la **batterie de tests automatisés** qui garantit la stabilité de l'interface
entre WinDev et le moteur de planification.

Ces tests sont la sécurité principale avant toute évolution du moteur.
La stratégie de test générale du moteur est documentée dans `60_TESTING_STRATEGY_ENGINE.md`.

---

## 1. Objectif et périmètre

La validation automatisée garantit :

- la validité et la stabilité de l'API REST pour WinDev,
- que les scénarios sont exécutables de bout en bout,
- que le solveur reste déterministe,
- que le score et son explicabilité restent cohérents.

Exécution :

```bash
.\gradlew test
```

---

## 2. Organisation des classes de test

```text
src/test/java
│
├─ scenarios/
│   └─ api/
│       ├─ ScenarioControllerRuntimeTest
│       ├─ ScenarioControllerValidationTest
│       └─ ScenarioControllerSolverStabilityTest
│
├─ solver/
│   ├─ PlanningServiceSolverStabilityTest
│   └─ PlanningServiceScoreRegressionTest
│
└─ fixtures/
    └─ TestPlanningRequestFactory
```

---

## 3. Tests d'exécution du scénario

**Classe :** `ScenarioControllerRuntimeTest`

**Objectif :** Vérifier qu'un scénario SC-01 complet peut être exécuté via l'API HTTP du moteur.

**Flux testé :**

```text
POST /scenarios/sc01/solve
```

**Entrée :**

```text
src/test/resources/scenarios/sc01/sc01_dataset_reference.json
```

**Vérifications :**

- réponse HTTP 200
- réponse JSON valide
- présence des blocs : `planning`, `workMetrics`, `solutionSummary`, `solverResult`, `scoreBreakdown`

**Ce test garantit que :**

- le contrat JSON entre WinDev et le moteur est valide,
- le solveur peut traiter un scénario complet.

---

## 4. Tests de robustesse de l'API

**Classe :** `ScenarioControllerValidationTest`

**Objectif :** Vérifier que l'API réagit correctement aux erreurs d'entrée.

**Cas testés :**

| Cas                                 | Résultat attendu |
| ----------------------------------- | ---------------- |
| scénario invalide                   | erreur           |
| dataset manquant                    | exception        |
| `resourceRef` manquant              | exception        |
| ressource inconnue dans le dataset  | erreur           |
| horizon invalide                    | erreur           |

**Ce test garantit que l'interface moteur :**

- détecte les erreurs de contrat,
- ne produit pas de planification incohérente à partir d'une entrée invalide.

---

## 5. Test de stabilité du solveur

**Classe :** `PlanningServiceSolverStabilityTest`

**Objectif :** Vérifier que le solveur produit un résultat observable stable pour une même requête.

**Procédure :**

1. Génération d'un `PlanningRequest` via `TestPlanningRequestFactory`
2. Exécution du solveur deux fois : `PlanningService.solve(request)`
3. Comparaison du score final, des affectations et du `scoreBreakdown`

**Ce test garantit que :**

- le solveur n'introduit pas d'aléa observable,
- les résultats sont reproductibles pour une même entrée.

---

## 6. Test de cohérence du score

**Classe :** `PlanningServiceScoreRegressionTest`

**Objectif :** Garantir que la structure du score et de son explicabilité reste stable.

**Vérifications :**

- score final présent,
- `scoreBreakdown` non vide,
- chaque item du breakdown possède : `penaliteKey`, `unit`, `quantity`, `weightedImpact`,
- `weightedImpact` ≤ 0,
- score soft ≤ 0.

**Ce test protège le contrat d'explicabilité du moteur.**

---

## 7. Factory de génération de scénarios de test

**Classe :** `TestPlanningRequestFactory`

**Rôle :** Créer des scénarios de test cohérents pour les tests unitaires, indépendamment du JSON.

**Elle génère :**

```text
PlanningContext
RegulatoryParameters
ReferentielComptabiliteActivite
Ressources
Créneaux
```

**Avantages :**

- tests indépendants du JSON d'entrée,
- scénarios reproductibles,
- simplification des tests unitaires du solveur.

---

## 8. Garanties apportées par la batterie

| Garantie                          | Tests couvrants                                                      |
| --------------------------------- | -------------------------------------------------------------------- |
| API REST stable pour WinDev       | `ScenarioControllerRuntimeTest`, `ScenarioControllerValidationTest`  |
| Scénario exécutable de bout en bout | `ScenarioControllerRuntimeTest`                                    |
| Solveur déterministe              | `PlanningServiceSolverStabilityTest`                                 |
| Score et explicabilité cohérents  | `PlanningServiceScoreRegressionTest`                                 |
| Détection des erreurs de contrat  | `ScenarioControllerValidationTest`                                   |

---

**Fin du document**
