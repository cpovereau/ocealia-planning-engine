# 50 — Contrat de sortie du moteur (ScenarioResponseContract)

Ce document est la **référence normative de la réponse** produite par le moteur de planification.

Il décrit :
- le rôle fonctionnel de chaque bloc de la réponse,
- la structure et les types de chaque champ,
- les points d'attention contractuels.

Pour les exemples JSON complets, voir `50_interface_windev_moteur_exemples.md`.
Pour la description de la requête champ par champ, voir `50_interface_windev_moteur_contrat_detail.md`.

---

## Principe de conception

La réponse du moteur est structurée en cinq blocs fonctionnels indépendants.

| Bloc              | Rôle                                                                   |
| ----------------- | ---------------------------------------------------------------------- |
| `solverResult`    | Explique comment le solveur évalue la solution                         |
| `planning`        | Contient les décisions produites par le moteur                         |
| `workMetrics`     | Décrit les conséquences du planning sur les ressources                 |
| `solutionSummary` | Fournit une lecture synthétique et pilotable de la solution produite   |
| `diagnostics`     | Fournit des informations techniques utiles pour l'analyse et le debug  |

Cette séparation garantit que :
- le moteur reste un moteur d'optimisation,
- l'analyse RH reste une couche aval.

---

## Structure racine

| Champ             | Type   | Toujours présent | Description                                      |
| ----------------- | ------ | :--------------: | ------------------------------------------------ |
| `scenarioType`    | string | Oui              | Echo du type de scénario traité (`"SC-01"`)      |
| `solverResult`    | objet  | Oui              | Statut et score du solveur                       |
| `planning`        | objet  | Oui              | Planning généré par le moteur                    |
| `workMetrics`     | objet  | Oui              | Métriques de travail calculées après résolution  |
| `solutionSummary` | objet  | Oui              | Résumé synthétique chiffré                       |
| `diagnostics`     | objet  | Oui              | Alertes et diagnostics d'affectation             |

---

## 1. `solverResult` — Évaluation du solveur

Ce bloc expose le résultat du solveur OptaPlanner : statut de résolution, score final, et détail des pénalités.

| Champ            | Type    | Description                                                               |
| ---------------- | ------- | ------------------------------------------------------------------------- |
| `status`         | string  | Statut du solveur (`"SOLVED"` si résolution complète)                     |
| `score.hard`     | integer | Composante HARD du score (0 = aucune violation de contrainte impérative)  |
| `score.soft`     | integer | Composante SOFT du score (≤ 0 — plus proche de 0 = meilleure solution)    |
| `scoreBreakdown` | tableau | Détail des pénalités — voir §1.1                                          |

Une solution valide doit toujours avoir : `hard = 0`.

Exemple :

```json
"solverResult": {
  "status": "SOLVED",
  "score": {
    "hard": 0,
    "soft": -120
  }
}
```

### 1.1 `scoreBreakdown` — détail des pénalités

Le bloc `scoreBreakdown` expose les contributions des pénalités au score soft.

| Champ            | Type    | Description                                                             |
| ---------------- | ------- | ----------------------------------------------------------------------- |
| `penaliteKey`    | string  | Identifiant de la contrainte (ex : `"METIER_SOFT_CRENEAU_NON_COUVERT"`) |
| `unit`           | string  | Unité de mesure — voir enum `ScoreBreakdownUnit` ci-dessous             |
| `quantity`       | double  | Volume mesuré selon l'unité                                             |
| `weightedImpact` | integer | Impact pondéré sur le score (toujours ≤ 0)                              |

L'unité est portée par `penaliteKey` pour garantir une restitution stable et cohérente.
La décision architecturale et la définition complète de l'enum `ScoreBreakdownUnit`
(`OCCURRENCE`, `MINUTE_PONDEREE`, `JOUR`, `UNKNOWN`) sont dans
`20_DECISIONS_CONCEPTION_OPTAPLANNER.md §9 bis`.

Règle de lecture :

```text
weightedImpact = quantity × poids(penaliteKey, strategieScoring)
```

Un `weightedImpact` nul signifie que la contrainte a été mesurée mais que son poids est 0 dans la stratégie active (informatif seulement).

Exemple :

```json
{
  "penaliteKey": "METIER_SOFT_CRENEAU_NON_COUVERT",
  "unit": "OCCURRENCE",
  "quantity": 4.0,
  "weightedImpact": -40000
}
```

---

## 2. `planning` — Solution produite

Ce bloc contient le planning résultant de la résolution : quelle ressource est affectée à quel créneau.

Dans le périmètre actuel, les caractéristiques temporelles des créneaux (date, heure de début, heure de fin, type) sont des **données d'entrée figées**. Le moteur optimise leur **affectation aux ressources**.

Certains champs structurels du dataset (`groupeBesoinId`, `blocJourId`, `ordreDansBloc`) permettent au moteur de raisonner sur des ensembles de créneaux liés, sans modifier directement les décisions du solveur.

Exemple :

```json
"planning": {
  "idSalarie": "1041",
  "jours": [
    {
      "date": "2026-02-23",
      "creneaux": [
        {
          "activite": "travail",
          "heureDebut": "08:00",
          "heureFin": "16:00",
          "duree": "08:00",
          "ressourceAffecteeId": "1041"
        }
      ]
    }
  ]
}
```

> **Créneau non affecté** : un créneau sans ressource est représenté par la valeur `"A_AFFECTER"` dans le champ `ressourceAffecteeId`, jamais par `null`. Il est comptabilisé dans `solutionSummary.nbCreneauxNonAffectes` et dans `scoreBreakdown` via `METIER_SOFT_CRENEAU_NON_COUVERT`. La pseudo-ressource `A_AFFECTER` n'apparaît pas dans `workMetrics.byRessource`. Décision documentée dans `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`.

> **Champ `duree` dans le planning** : la valeur exposée est issue de `Creneau.duree` (durée stockée, transmise par WinDev). Un recalcul d'affichage à partir de `heureDebut`/`heureFin` est toléré uniquement s'il est strictement cohérent avec la durée stockée. Décision documentée dans `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`.

---

## 3. `workMetrics` — Conséquences du planning

Les WorkMetrics décrivent les effets du planning sur chaque ressource. Elles sont calculées **après** la résolution.

Elles permettent :
- d'expliquer le score,
- d'analyser la charge de travail,
- de préparer les analyses RH futures.

Les WorkMetrics :
- ne modifient jamais le planning,
- ne déclenchent aucune contrainte,
- servent uniquement à décrire la solution produite.

### 3.1 Structure de `workMetrics`

| Champ         | Type    | Description                               |
| ------------- | ------- | ----------------------------------------- |
| `byRessource` | tableau | Métriques par ressource — voir §3.2       |
| `global`      | objet   | Agrégats globaux — voir §3.3              |

### 3.2 Métriques par ressource (`byRessource[]`)

| Champ                           | Type    | Description                                                      |
| ------------------------------- | ------- | ---------------------------------------------------------------- |
| `resourceId`                    | string  | Identifiant de la ressource                                      |
| `periodeDebut`                  | string  | Date début de la période analysée (ISO-8601)                     |
| `periodeFin`                    | string  | Date fin de la période analysée (ISO-8601)                       |
| `heuresTravaillees`             | double  | Heures travaillées totales (créneaux `compteDansCharge = true`)  |
| `heuresNuit`                    | double  | Heures en plage nocturne (défaut : 22h–06h)                      |
| `heuresJourFerie`               | double  | Heures travaillées sur jours fériés                              |
| `heuresReposHebdoTravaille`     | double  | Heures empiétant sur le repos hebdomadaire                       |
| `nbDimanchesTravailles`         | integer | Nombre de dimanches travaillés                                   |
| `maxJoursConsecutifsObservees`  | integer | Séquence max de jours consécutifs travaillés                     |
| `maxNuitsConsecutivesObservees` | integer | Séquence max de nuits consécutives                               |

Exemple :

```json
{
  "resourceId": "1041",
  "heuresTravaillees": 35.5,
  "heuresNuit": 6.0,
  "nbDimanchesTravailles": 1
}
```

### 3.3 Métriques globales (`global`)

| Champ                      | Type    | Description                                                                  |
| -------------------------- | ------- | ---------------------------------------------------------------------------- |
| `nbCreneaux`               | integer | Total créneaux dans le planning                                              |
| `nbCreneauxNonAffectes`    | integer | Total créneaux non couverts                                                  |
| `heuresTravailleesTotales` | double  | Total heures tous salariés — calculé à partir de `Creneau.duree` (durée stockée) |
| `heuresNuitTotales`        | double  | Total heures de nuit (intersection)                                          |
| `heuresJourFerieTotales`   | double  | Total heures jours fériés (intersection)                                     |

---

## 4. `diagnostics` — Informations techniques

Ce bloc contient des informations utiles pour comprendre l'exécution du moteur :
créneaux ignorés, activités inconnues, créneaux hors horizon, alertes de cohérence.

Particulièrement utile en phase d'intégration ou lors de l'analyse d'un scénario.

### 4.1 Structure de `diagnostics`

| Champ                   | Type    | Description                                            |
| ----------------------- | ------- | ------------------------------------------------------ |
| `alerts`                | tableau | Alertes pré-résolution générées par le builder         |
| `assignmentDiagnostics` | tableau | Diagnostics post-résolution sur les affectations       |
| `ignoredCreneaux`       | objet   | Compteurs de créneaux ignorés lors de la construction  |

### 4.2 Alertes (`alerts[]`)

| Champ      | Type   | Description                                                          |
| ---------- | ------ | -------------------------------------------------------------------- |
| `code`     | string | Code d'alerte — ex : `"SHIFT_END_EXCEEDED"`                          |
| `severity` | string | `INFO` \| `WARNING` \| `ERROR` — optionnel, absent = lire `WARNING`   |
| `date`     | string | Date concernée (ISO-8601) — **optionnel, la clé est omise si absente** |
| `message`  | string | Message lisible                                                      |

`date` est omis — et non sérialisé à `null` — lorsque l'alerte ne porte pas sur un jour
mais sur le dataset ou la configuration. Le client doit traiter l'absence de la clé,
pas une valeur nulle.

Codes émis et gravité associée :

| Code | Sévérité | Signification |
|------|----------|---------------|
| `SHIFT_END_EXCEEDED` | `WARNING` | Fin de poste au-delà de la borne d'alerte |
| `LUNCH_BREAK_OUTSIDE_AMPLITUDE` | `WARNING` | Pause midi incohérente : un seul créneau généré |
| `INSUFFICIENT_WEEKLY_REST` | `ERROR` | Aucun jour de repos hebdomadaire configurable |
| `TOO_MANY_NON_WORKED_DAYS` | `INFO` | Jours non travaillés au-delà du repos hebdomadaire |
| `UNKNOWN_ACTIVITY` | `ERROR` | Code activité des créneaux générés absent du référentiel injecté — **sans `date`** |

**Une alerte `INFO` n'est pas une anomalie.** Elle décrit une configuration atypique mais
valide — un temps partiel à 4 jours travaillés relève de ce cas — et ne doit pas être
restituée à l'utilisateur comme un défaut. Le client filtre sur `severity`, pas sur `code`.

Les alertes portant sur la configuration hebdomadaire (`INSUFFICIENT_WEEKLY_REST`,
`TOO_MANY_NON_WORKED_DAYS`) sont émises **une seule fois** par réponse, ancrées sur le
début de l'horizon, quel que soit le nombre de semaines couvertes.

### 4.3 Diagnostics d'affectation (`assignmentDiagnostics[]`)

Les `assignmentDiagnostics` sont produits dans la couche de restitution API (`ScenarioResponseMapper`),
indépendamment du solveur et du builder.

| Champ        | Type   | Description                           |
| ------------ | ------ | ------------------------------------- |
| `creneauId`  | string | Identifiant du créneau concerné       |
| `date`       | string | Date du créneau (ISO-8601)            |
| `heureDebut` | string | Heure de début                        |
| `heureFin`   | string | Heure de fin                          |
| `activite`   | string | Code activité — `codeActiviteId` du créneau, avec repli sur le libellé `activite` s'il est absent (voir §4.5) |
| `status`     | string | Statut d'affectation — voir ci-dessous |
| `reasonCode` | string | Code raison — voir ci-dessous         |
| `message`    | string | Message explicatif                    |

Valeurs de `status` / `reasonCode` implémentées :

| `status`              | `reasonCode`              | Signification                              |
| --------------------- | ------------------------- | ------------------------------------------ |
| `UNCOVERED`           | `NO_RESOURCE_ASSIGNED`    | Aucune ressource affectée par le solveur   |
| `VIRTUAL_ASSIGNED`    | `POSTE_VIRTUEL_ASSIGNED`  | Affecté à un poste virtuel                 |
| `IMPOSSIBLE_TO_ASSIGN`| `NO_COMPATIBLE_RESOURCE`  | Aucune ressource compatible disponible     |

Exemple :

```json
{
  "creneauId": "SC01-2026-02-22-001",
  "date": "2026-02-22",
  "heureDebut": "22:00",
  "heureFin": "06:00",
  "activite": "travail",
  "status": "UNCOVERED",
  "reasonCode": "NO_RESOURCE_ASSIGNED",
  "message": "Créneau non couvert par le solveur"
}
```

### 4.4 Créneaux ignorés (`ignoredCreneaux`)

Ces compteurs sont calculés **en pré-résolution**, dans la couche de préparation du scénario (`ScenarioSc03PreparationService`), à partir des DTO bruts reçus de WinDev — avant tout appel au solveur.

| Champ                        | Type    | Implémenté | Description                                           |
| ---------------------------- | ------- | :--------: | ----------------------------------------------------- |
| `horsHorizon`                | integer | ✅ Phase 9  | Créneaux dont la date est hors de l'horizon [dateDebut, dateFin] |
| `activiteInconnue`           | integer | ✅ Phase 9  | Créneaux avec un `codeActiviteId` absent du référentiel d'activités |
| `aucuneRessourceDansDataset` | integer | ✅ Phase 12 | Créneaux dont aucune ressource du dataset ne déclare l'activité (contrôle structurel pré-résolution) |

> ~~Pour SC-01, ces trois compteurs valent toujours 0 : les créneaux sont générés programmatiquement par le builder, sans filtrage pré-résolution.~~
>
> **Mise à jour 2026-03-30 (chantier SC-01, tâche C1)** — SC-01 calcule lui aussi ses compteurs, via
> `ScenarioSc01PreparationService.computeIgnoredCreneaux()`, avec deux différences par rapport à SC-03 :
> - `activiteInconnue` peut être **> 0** si le bloc `referentiels` transmis ne déclare pas l'activité
>   `"travail"` utilisée par le builder ;
> - `aucuneRessourceDansDataset` vaut toujours 0 (la ressource cible est résolue en amont) et
>   `horsHorizon` vaut 0 par construction (les créneaux sont générés dans l'horizon) ;
> - en SC-01 ces compteurs sont **diagnostiques uniquement** : aucun créneau n'est exclu avant le
>   solveur, contrairement à SC-03 qui les partitionne réellement.

### 4.5 Restitution du code activité

Deux champs de sortie portent l'activité d'un créneau : `planning.jours[].creneaux[].activite` et
`assignmentDiagnostics[].activite`. Les deux appliquent la même règle, alignée sur celle utilisée
avant résolution pour la jointure référentiel :

| Entrée | Valeur restituée |
|---|---|
| `codeActiviteId` renseigné | `codeActiviteId` |
| `codeActiviteId` absent ou vide, `activite` renseigné | `activite` (libellé — champ déprécié) |
| les deux absents | `null` |

> **Correctif 2026-07-29** — la restitution lisait auparavant `activite` seul. Un client conforme
> n'envoyant que `codeActiviteId` — cas nominal SC-03 — recevait `"activite": null` en sortie.
> Implémenté par `Creneau.getCodeActiviteEffectif()`, utilisé par `ScenarioResponseMapper` et
> `AssignmentDiagnosticsFactory`.
>
> Le champ de sortie conserve le nom `activite` : aucun changement de structure pour WinDev.

---

## 5. `solutionSummary` — Lecture synthétique

Ce bloc fournit une lecture condensée et lisible du planning résolu.

Il permet :
- de résumer rapidement une solution,
- de comparer plusieurs résultats entre eux,
- d'exposer des indicateurs globaux compréhensibles sans lire le détail des WorkMetrics.

Il ne remplace ni le détail des WorkMetrics, ni l'évaluation du solveur, ni les diagnostics techniques.

| Champ                      | Type    | Description                                     |
| -------------------------- | ------- | ----------------------------------------------- |
| `nbCreneaux`               | integer | Nombre total de créneaux à planifier            |
| `nbCreneauxAffectes`       | integer | Créneaux assignés à une ressource réelle        |
| `nbCreneauxNonAffectes`    | integer | Créneaux restés sur `A_AFFECTER`                |
| `nbRessourcesMobilisees`   | integer | Nombre de ressources ayant au moins un créneau  |
| `heuresTravailleesTotales` | double  | Total des heures travaillées (tous salariés)    |

---

## 6. Points d'attention contractuels

**Séparation solveur / API** — la solution OptaPlanner est transformée en `ScenarioResponseDTO` par `ScenarioResponseMapper`. Cette couche garantit la stabilité du contrat API indépendamment des évolutions internes du solveur. Décision documentée dans `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`.

**Unité du `scoreBreakdown`** — l'unité de chaque ligne (`penaliteKey`, `unit`, `quantity`, `weightedImpact`) est déterminée par `PenaliteKey`. Décision documentée dans `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`.

**`workMetrics` et `A_AFFECTER`** — la pseudo-ressource `A_AFFECTER` n'apparaît jamais dans `workMetrics.byRessource`. Seules les ressources réelles y figurent.

---

## 7. Évolution prévue

Ce contrat pourra être enrichi progressivement avec :
- de nouvelles WorkMetrics (équité, écarts de charge, métriques contractuelles),
- des indicateurs d'analyse RH.

Ces évolutions n'impacteront pas la structure générale de la réponse.

---

**Fin du document**
