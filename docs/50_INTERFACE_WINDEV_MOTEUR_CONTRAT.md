# 50 — Contrat d'API WinDev ↔ Moteur (vue générale)

Ce document décrit le **contrat d'appel entre WinDev et le moteur de planification** :
structure globale de la requête et de la réponse, règles générales, normalisation des champs,
politique sur les champs inconnus, garanties du contrat et jalons d'évolution.

Pour le détail champ par champ, voir `50_INTERFACE_WINDEV_MOTEUR_CONTRAT_DETAIL.md`.
Pour les exemples JSON complets, voir `50_INTERFACE_WINDEV_MOTEUR_EXEMPLES.md`.

---

## 1. Endpoint principal

```text
POST /scenarios/sc01/solve/file
```

Ce endpoint permet d'exécuter un scénario SC-01.

Format HTTP :

```text
Content-Type: application/json
Accept:       application/json
```

Le moteur est utilisé comme un **service de planification stateless** : il n'a pas d'état entre deux appels.

Évolutivité — les scénarios futurs exposeront leurs propres endpoints :

```text
POST /scenarios/sc02/solve
POST /scenarios/sc03/solve
...
```

Chaque scénario possèdera ses propres paramètres, règles de scoring et structures de réponse.

---

## 2. Structure de la requête

Structure globale :

```json
{
  "scenarioType": "SC-01",
  "planningContext": { ... },
  "scenarioParameters": { ... },
  "dataSet": { ... }
}
```

Description des blocs :

| Bloc                 | Requis | Description                                        |
| -------------------- | ------ | -------------------------------------------------- |
| `scenarioType`       | Oui    | Identifiant du scénario — seule valeur : `"SC-01"` |
| `planningContext`    | Oui    | Horizon temporel et stratégie de scoring           |
| `scenarioParameters` | Oui    | Paramètres métier spécifiques au scénario          |
| `dataSet`            | Oui    | Ressources et créneaux disponibles                 |

### 2.1 `planningContext`

Contient :
- `horizon.dateDebut` / `horizon.dateFin` — fenêtre de résolution (format ISO-8601 `YYYY-MM-DD`)
- `strategieScoring` — stratégie de pondération du score

Valeurs de `strategieScoring` :

| Valeur        | Comportement                                                         |
| ------------- | -------------------------------------------------------------------- |
| `EXPLOITATION`| Coeff ×5 sur les créneaux non couverts — pénalise le manque de couverture |
| `ANALYSE_RH`  | Coeff ×2 — vision équilibrée pour analyse des charges                |
| `AUDIT`       | Coeff ×3 — focus sur la traçabilité et l'explicabilité               |

Défaut : `EXPLOITATION` si absent.

### 2.2 `scenarioParameters` (SC-01)

Contient les paramètres propres au scénario SC-01 :
- `resourceRef` — référence à la ressource principale (salarié ou poste virtuel)
- `dailyAmplitudeHours` — amplitude journalière pause incluse, en heures
- `shiftStart` / `shiftEndAlert` — horaires de poste
- `lunchBreak` — pause déjeuner optionnelle
- `workedDays` — jours travaillés (voir §3 — normalisation)
- `holidayDates` — jours fériés exclus du planning

### 2.3 `dataSet`

Contient les ressources et créneaux :
- `ressources.salaries` — liste des salariés disponibles
- `ressources.postesVirtuels` — postes virtuels (optionnel)
- `creneaux` — champ toléré mais **ignoré** en DataSet V1

### 2.4 Contenu cible du dataset (évolution)

Le contrat d'entrée est en cours d'enrichissement pour supporter les scénarios avancés.
La cible prévoit de transmettre :

**Organisation :**
```text
direction / service / lieu / poste comptable
```

**Ressources (par salarié) :**
```text
id / contrat / contraintes réglementaires / axes organisationnels / activités autorisées
```

**Planning existant :**
```text
créneaux existants / créneaux imposés / créneaux libres
```

**Besoins de couverture :**
```text
besoins par lieu / besoins par activité / besoins par période
```

Le plan de migration détaillé est dans `90_PLAN_MIGRATION_TEMPORAIRE_WINDEV_VERS_MOTEUR.md`.

---

## 3. Structure de la réponse

Structure globale :

```json
{
  "scenarioType": "SC-01",
  "solverResult": { ... },
  "planning": { ... },
  "workMetrics": { ... },
  "solutionSummary": { ... },
  "diagnostics": { ... }
}
```

Description des blocs :

| Bloc             | Toujours présent | Description                                     |
| ---------------- | ---------------- | ----------------------------------------------- |
| `scenarioType`   | Oui              | Echo du type de scénario traité                 |
| `solverResult`   | Oui              | Statut du solveur, score, et scoreBreakdown      |
| `planning`       | Oui              | Planning généré par le moteur                   |
| `workMetrics`    | Oui              | Métriques de travail calculées après résolution  |
| `solutionSummary`| Oui              | Résumé synthétique chiffré                      |
| `diagnostics`    | Oui              | Alertes et diagnostics d'affectation            |

Le contrat de sortie fonctionnel complet est documenté dans `50_SCENARIO_RESPONSE_CONTRACT.md`.
Le détail champ par champ de la réponse est dans `50_INTERFACE_WINDEV_MOTEUR_CONTRAT_DETAIL.md`.

---

## 4. Normalisation des champs

### 4.1 Champ `workedDays` (SC-01)

Le champ `workedDays` doit utiliser le format **Java `DayOfWeek` complet** :

```text
MONDAY  TUESDAY  WEDNESDAY  THURSDAY  FRIDAY  SATURDAY  SUNDAY
```

Les formats abrégés sont **rejetés** :

```text
MON  TUE  WED  THU  FRI  SAT  SUN   ← INTERDIT
```

Cette règle est normative pour garantir la cohérence entre le contrat fonctionnel,
les DTO Java, les exemples JSON et l'implémentation côté WinDev.

---

## 5. Politique sur les champs inconnus

### Décision provisoire (phase actuelle)

Les champs inconnus du JSON d'entrée sont **tolérés** à la désérialisation.

Cette tolérance est volontaire et transitoire. Elle permet de ne pas bloquer l'intégration WinDev
pendant la phase de construction et d'alignement du contrat d'entrée.

### Justification

Le contrat d'entrée n'est pas encore complètement stabilisé :
- certains DTO ont été introduits progressivement,
- la séparation entre transport API et modèle domaine n'est pas encore finalisée,
- la structure JSON documentée et la structure testée ne sont pas encore parfaitement alignées.

### Cible

Lorsque le contrat sera stabilisé et les DTO figés :
- rejet explicite de tout champ inconnu,
- protection forte du contrat d'interface,
- détection immédiate des écarts entre WinDev et le moteur.

| Phase                         | Comportement                    |
| ----------------------------- | ------------------------------- |
| Phase actuelle (migration)    | Champs inconnus tolérés         |
| Phase de stabilisation cible  | Champs inconnus rejetés         |

---

## 6. Garanties du contrat

L'API garantit les propriétés suivantes :

- réponse JSON valide,
- présence systématique des blocs principaux (`planning`, `workMetrics`, `solutionSummary`, `solverResult`, `diagnostics`),
- score explicable via `scoreBreakdown`,
- déterminisme du résultat observable pour une même requête.

Ces garanties sont validées automatiquement par la batterie de tests du moteur
(voir `50_INTERFACE_WINDEV_MOTEUR_TESTS.md`).

---

## 7. Flux d'intégration côté WinDev

Le flux typique d'utilisation est :

```text
1. Construction du JSON scénario
2. POST /scenarios/sc01/solve/file
3. Attente de la réponse JSON
4. Extraction du planning, des métriques et du score
```

---

## 8. Jalons d'évolution de l'interface

| Jalon | Objectif                                         | Statut   |
| ----- | ------------------------------------------------ | -------- |
| J1    | Stabilisation du contrat minimal SC-01           | Terminé  |
| J2    | DatasetBuilder robuste (DTO → PlanningProblem)   | Terminé  |
| J3    | Extension du dataset (axes, contrats, réglem.)   | Terminé  |
| J4    | Gestion des besoins (groupeBesoinId, blocJourId) | Terminé  |
| J5    | Scénarios avancés SC-02, SC-03, SC-04            | En cours |

Le suivi d'avancement détaillé est dans `90_SUIVI_DEVELOPPEMENT_MOTEUR.md`.

---

**Fin du document**
