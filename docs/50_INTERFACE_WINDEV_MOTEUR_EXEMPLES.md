# 50 — Exemples d'utilisation — Interface WinDev ↔ Moteur

Ce document fournit des **exemples pratiques** destinés aux équipes intégrant le moteur
depuis WinDev ou toute application externe.

Pour la description des champs, voir `50_INTERFACE_WINDEV_MOTEUR_CONTRAT_DETAIL.md`.
Pour les règles générales du contrat, voir `50_INTERFACE_WINDEV_MOTEUR_CONTRAT.md`.

---

## 1. Exemple d'appel HTTP (curl)

Exécution d'un scénario SC-01 via l'API :

```bash
curl -X POST "http://localhost:8082/scenarios/sc01/solve/file" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d @sc01_dataset_reference.json
```

- Le moteur écoute sur `localhost:8082` en développement local.
- La requête contient un scénario complet au format `ScenarioRequestDTO`.
- Le fichier JSON de référence est : `src/test/resources/scenarios/sc01/sc01_dataset_reference.json`

---

## 2. Exemple de requête JSON (SC-01)

```json
{
  "scenarioType": "SC-01",
  "planningContext": {
    "horizon": {
      "dateDebut": "2026-05-11",
      "dateFin": "2026-05-17"
    },
    "strategieScoring": "EXPLOITATION"
  },
  "scenarioParameters": {
    "resourceRef": {
      "kind": "SALARIE",
      "id": "1041"
    },
    "dailyAmplitudeHours": 8.0,
    "shiftStart": "08:00",
    "shiftEndAlert": "17:00",
    "workedDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
  },
  "dataSet": {
    "ressources": {
      "salaries": [],
      "postesVirtuels": []
    },
    "creneaux": []
  }
}
```

---

## 3. Exemple de réponse JSON simplifiée

```json
{
  "planning": {
    "jours": []
  },
  "workMetrics": {
    "ressources": []
  },
  "solutionSummary": {
    "affectations": 0
  },
  "solverResult": {
    "status": "SOLVED",
    "score": {
      "hard": 0,
      "soft": -12
    }
  },
  "scoreBreakdown": [
    {
      "penaliteKey": "METIER_SOFT_CRENEAU_NON_COUVERT",
      "unit": "MINUTE_PONDEREE",
      "quantity": 30,
      "weightedImpact": -30
    }
  ]
}
```

---

## 4. Exemple de réponse JSON complète (structure réelle)

```json
{
  "scenarioType": "SC-01",
  "solverResult": {
    "status": "SOLVED",
    "score": {
      "hard": 0,
      "soft": -120
    },
    "scoreBreakdown": [
      {
        "penaliteKey": "METIER_SOFT_CRENEAU_NON_COUVERT",
        "unit": "MINUTE_PONDEREE",
        "quantity": 480.0,
        "weightedImpact": -120
      }
    ]
  },
  "planning": {
    "idSalarie": "1041",
    "jours": []
  },
  "workMetrics": {
    "byRessource": [
      {
        "resourceId": "1041",
        "periodeDebut": "2026-05-11",
        "periodeFin": "2026-05-17",
        "heuresTravaillees": 32.0,
        "heuresNuit": 0.0,
        "heuresJourFerie": 0.0,
        "heuresReposHebdoTravaille": 0.0,
        "nbDimanchesTravailles": 1,
        "maxJoursConsecutifsObservees": 5,
        "maxNuitsConsecutivesObservees": 0
      }
    ],
    "global": {
      "nbCreneaux": 5,
      "nbCreneauxNonAffectes": 0,
      "heuresTravailleesTotales": 32.0,
      "heuresNuitTotales": 0.0,
      "heuresJourFerieTotales": 0.0
    }
  },
  "solutionSummary": {
    "nbCreneaux": 5,
    "nbCreneauxAffectes": 5,
    "nbCreneauxNonAffectes": 0,
    "nbRessourcesMobilisees": 1,
    "heuresTravailleesTotales": 32.0
  },
  "diagnostics": {
    "alerts": [],
    "assignmentDiagnostics": [],
    "ignoredCreneaux": {
      "horsHorizon": 0,
      "sansRessource": 0,
      "activiteInconnue": 0
    }
  }
}
```

---

## 5. Lecture de la réponse

| Bloc              | Ce qu'on y lit                                                          |
| ----------------- | ----------------------------------------------------------------------- |
| `solverResult`    | Statut (`SOLVED`), score HARD/SOFT, et détail des pénalités             |
| `planning`        | Planning affecté par le solveur, créneau par créneau                   |
| `workMetrics`     | Métriques RH par salarié et agrégats globaux                           |
| `solutionSummary` | Vue chiffrée synthétique (nb créneaux, ressources mobilisées, heures)  |
| `diagnostics`     | Alertes du builder et diagnostics d'affectation post-résolution         |

Points d'attention :

- Un créneau non affecté apparaît avec la valeur `"A_AFFECTER"` (jamais `null`).
- Un `weightedImpact` à 0 dans `scoreBreakdown` signifie que la contrainte a été mesurée mais que son poids est nul dans la stratégie active.
- La pseudo-ressource `A_AFFECTER` n'apparaît pas dans `workMetrics.byRessource`.

---

## 6. Flux d'intégration côté client

```text
1. Construire le JSON du scénario
2. POST /scenarios/sc01/solve/file
3. Lire la réponse JSON
4. Extraire le planning depuis "planning"
5. Extraire les métriques depuis "workMetrics"
6. Lire le score depuis "solverResult.score"
7. Analyser les pénalités via "solverResult.scoreBreakdown"
```

Le moteur fonctionne comme un **service de calcul de planification stateless** :
il ne conserve aucun état entre deux appels.

---

**Fin du document**
