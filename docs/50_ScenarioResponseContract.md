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

La réponse du moteur est structurée en blocs fonctionnels indépendants — cinq communs à tous les
scénarios, un sixième propre à SC-06.

| Bloc              | Rôle                                                                   |
| ----------------- | ---------------------------------------------------------------------- |
| `solverResult`    | Explique comment le solveur évalue la solution                         |
| `planning`        | Contient les décisions produites par le moteur                         |
| `workMetrics`     | Décrit les conséquences du planning sur les ressources                 |
| `solutionSummary` | Fournit une lecture synthétique et pilotable de la solution produite   |
| `diagnostics`     | Fournit des informations techniques utiles pour l'analyse et le debug  |
| `candidats`       | **SC-06 uniquement** — classe les manières de couvrir un besoin        |

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
| `candidats`       | tableau| **SC-06 seul**   | Solutions classées — **clé absente ailleurs**, voir §6 |

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
          "id": "CRE-L-01",
          "lieu": "HOPITAL-NORD",
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

### 2.1 Champs d'un créneau du planning

| Champ                 | Type   | Toujours présent | Notes                                                              |
| --------------------- | ------ | :--------------: | ------------------------------------------------------------------ |
| `id`                  | string | Oui              | Identifiant du créneau, restitué **à l'identique** de l'entrée      |
| `lieu`                | string | Non              | Lieu reçu en entrée ; `null` si le créneau n'en portait pas         |
| `activite`            | string | Oui              | Code activité — règle de restitution en §4.5                        |
| `heureDebut`          | string | Oui              | `HH:mm`                                                             |
| `heureFin`            | string | Oui              | `HH:mm`                                                             |
| `duree`               | string | Oui              | `HH:MM`, issue de `Creneau.duree`                                   |
| `ressourceAffecteeId` | string | Oui              | `"A_AFFECTER"` si aucune ressource réelle n'est affectée            |

> **Champ `id` — clé de réintégration** : le moteur restitue l'identifiant reçu sans le modifier
> ni l'interpréter. C'est ce champ qui permet à WinDev de rattacher chaque créneau du planning à
> sa ligne d'origine. Pour les créneaux que le moteur génère lui-même (SC-01), l'identifiant porte
> le préfixe `SC01-` et ne correspond à aucune ligne en base : WinDev reste seul décideur de la
> clé primaire attribuée à la création. Le moteur ne fabrique jamais d'`Id_Journee`. Décision
> documentée dans `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`, convention d'entrée dans
> `50_ScenarioContract.md` §3.5.

> **Champ `lieu`** : restitué tel que reçu, sans normalisation ni résolution de libellé. Un créneau
> généré par le moteur (SC-01) sort avec `lieu: null` — le moteur n'attribue pas de lieu. Le champ
> est donc facultatif en sortie, jamais absent de la structure.

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
| `ACTIVITY_CODE_DEFAULTED` | `WARNING` | Aucun `codeActiviteId` déclaré en SC-01 : code historique `travail` appliqué — **sans `date`** |

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

## 6. `candidats` — Classement des solutions (SC-06 uniquement)

Ce bloc est **propre à SC-06**. La clé est **absente** — et non vide — pour SC-01 et SC-03 :
une réponse sans classement ne doit pas laisser croire à une capacité inexistante.

Il porte **au plus trois solutions classées**, de la plus favorable à la moins favorable. Une
liste plus courte n'est pas une anomalie : elle signifie qu'il n'existe pas davantage de manières
distinctes de couvrir ce besoin.

> **Un candidat est une solution, pas une personne** : l'affectation complète des créneaux du
> besoin. Quand une seule personne couvre tout — le cas préféré — la distinction s'efface, mais
> elle devient nécessaire dès qu'un besoin se répartit entre plusieurs personnes.

### 6.1 Structure d'un candidat

| Champ | Type | Description |
|---|---|---|
| `rang` | integer | 1 = la solution la plus favorable |
| `conforme` | boolean | `false` si une règle éliminatoire est violée — voir `motifs` |
| `couvertureComplete` | boolean | `false` si une partie du besoin reste sur `A_AFFECTER` |
| `nature` | string | `MONO_RESSOURCE` \| `COMPOSEE` \| `RESSOURCE_A_POURVOIR` |
| `affectations[]` | tableau | **qui** — une entrée par créneau du besoin |
| `impacts[]` | tableau | **à quel prix** — une entrée par ressource réelle mobilisée |
| `motifs[]` | tableau | **pourquoi ce rang** — même forme que `diagnostics.alerts[]` |

### 6.2 Ordre de classement

Paliers lexicographiques : chacun ne départage que les ex æquo du précédent.

| Rang | Palier |
|---|---|
| 1 | **Conformité** — aucune règle éliminatoire violée |
| 2 | **Couverture complète** avant couverture partielle |
| 3 | **Une seule personne** avant plusieurs ; **salarié réel** avant poste virtuel |
| 4 | **Personne déjà en poste ce jour-là** avant personne rappelée sur son repos |
| 5 | **Score SOFT** du moteur |
| 6 | **Charge** rapportée au volume hebdomadaire habituel |

Le choix d'un ordre lexicographique plutôt que d'un score unique est délibéré : les pondérations
du moteur ont été calibrées pour optimiser un planning, pas pour choisir une personne. Ici, chaque
rang se lit ligne à ligne.

> **Palier 6, donnée absente** : un salarié dont le contrat ne déclare pas
> `heuresHebdomadairesHabituelles` est classé en dernier de ce palier. À égalité par ailleurs, le
> moteur préfère la personne dont il peut mesurer l'impact — et rend ainsi visible un défaut
> d'intégration au lieu de l'absorber.

### 6.3 `affectations[]`

| Champ | Type | Description |
|---|---|---|
| `creneauId` | string | Identifiant du créneau de besoin, restitué à l'identique |
| `ressourceId` | string | `A_AFFECTER` si aucune ressource ne le prend en charge |
| `activite` | string | Code activité |
| `lieu` | string | Lieu reçu en entrée |
| `heureDebut` / `heureFin` | string | `HH:mm` |

### 6.4 `impacts[]`

Une entrée par ressource **réelle** mobilisée. Un poste virtuel n'y figure pas : il ne porte ni
contrat ni contraintes individuelles. Une solution `RESSOURCE_A_POURVOIR` a donc des impacts
**vides** — ce n'est pas une omission.

| Champ | Type | Description |
|---|---|---|
| `ressourceId` | string | Ressource concernée |
| `amplitudeJournaliere` | objet | Du début du premier créneau à la fin du dernier, le jour du besoin |
| `heuresJour` | objet | Heures travaillées le jour du besoin |
| `heuresSemaine` | objet | Heures travaillées sur la semaine lundi → dimanche |
| `heuresHabituellesSemaine` | double | Volume habituel déclaré au contrat ; `null` si absent |

Chaque mesure porte `avant`, `apres`, `delta`, `plafond` et `depassement`, en **heures décimales**
— même unité que `workMetrics.byRessource`.

`plafond` vaut `null` quand la limite individuelle n'est pas transmise, et `depassement` reste
alors `false` : **une limite absente n'est pas une limite à zéro**.

> ⚠️ **Un dépassement signalé n'est pas une règle appliquée.** Ce bloc décrit des conséquences ;
> il ne préjuge pas de ce que le moteur sanctionne. `heuresJour` est ainsi mesuré et son plafond
> restitué, alors qu'aucune contrainte ne lit encore `heuresMaximumParJour`. L'écart est rendu
> visible plutôt que masqué — il ne doit pas être lu comme une garantie.

### 6.5 `motifs[]`

Même forme que `diagnostics.alerts[]` : `code`, `severite`, `message`. **Le client filtre sur la
sévérité, pas sur le code.**

| Code | Sévérité | Éliminatoire |
|---|---|:---:|
| `REPOS_QUOTIDIEN_INSUFFISANT` | ERROR | ✅ |
| `HEURES_HEBDO_DEPASSEES` | ERROR | ✅ |
| `JOUR_FERIE_NON_AUTORISE` | ERROR | ✅ |
| `INDISPONIBILITE` | ERROR | ✅ |
| `CHEVAUCHEMENT` | ERROR | ✅ |
| `NUITS_CONSECUTIVES_DEPASSEES` | ERROR | ✅ |
| `REPOS_APRES_NUITS_INSUFFISANT` | ERROR | ✅ |
| `REPOS_HEBDOMADAIRE_GLISSANT_INSUFFISANT` | ERROR | ✅ |
| `AMPLITUDE_DEPASSEE` | WARNING | — |
| `JOURS_CONSECUTIFS_DEPASSES` | WARNING | — |
| `DIMANCHES_TRAVAILLES_DEPASSES` | WARNING | — |
| `NUIT_SALARIE_NON_NUIT` | WARNING | — |
| `RAPPEL_SUR_REPOS` | INFO | — |
| `BESOIN_PARTIELLEMENT_COUVERT` | WARNING | — |
| `AUCUNE_RESSOURCE_ELIGIBLE` | ERROR | — |

Un motif n'est levé que si le candidat **aggrave** la situation de référence — le besoin non
couvert. Cette mesure relative est nécessaire : le planning existant étant figé, il peut porter
des violations préexistantes, qui ne doivent être imputées à aucun candidat.

> **Éliminatoire ≠ contrainte HARD.** Le caractère éliminatoire relève du classement SC-06, pas
> du solveur. `REPOS_QUOTIDIEN_INSUFFISANT` et `HEURES_HEBDO_DEPASSEES` reposent sur des
> contraintes SOFT ; elles disqualifient néanmoins un candidat, parce qu'aucune recommandation
> ne doit conduire à une situation illégale.

### 6.6 `planning` et `workMetrics` en SC-06

Ces deux blocs décrivent la **solution de rang 1, et elle seule** :

* `planning` ne porte que **les créneaux du besoin**, affectés selon le rang 1. Le planning
  existant n'est pas réémis — l'appelant le possède déjà, et le renvoyer pour toutes les
  ressources candidates alourdirait la réponse sans rien apprendre ;
* `workMetrics.byRessource` ne porte que **les ressources mobilisées** par le rang 1.

### 6.7 Exemple

```json
"candidats": [
  {
    "rang": 1,
    "conforme": true,
    "couvertureComplete": true,
    "nature": "MONO_RESSOURCE",
    "affectations": [
      {
        "creneauId": "BES-001",
        "ressourceId": "SAL-2001",
        "activite": "ACT-SOIN",
        "lieu": "HOPITAL-NORD",
        "heureDebut": "14:00",
        "heureFin": "22:00"
      }
    ],
    "impacts": [
      {
        "ressourceId": "SAL-2001",
        "amplitudeJournaliere": { "avant": 4.0,  "apres": 14.0, "delta": 10.0, "plafond": 14.0, "depassement": false },
        "heuresJour":           { "avant": 4.0,  "apres": 12.0, "delta": 8.0,  "plafond": null, "depassement": false },
        "heuresSemaine":        { "avant": 20.0, "apres": 28.0, "delta": 8.0,  "plafond": 44.0, "depassement": false },
        "heuresHabituellesSemaine": 35.0
      }
    ],
    "motifs": []
  }
]
```

Lecture : ce salarié travaille déjà de 08:00 à 12:00 ce jour-là. Lui confier le besoin étirerait
sa journée jusqu'à 22:00, soit une amplitude de 14 h — au plafond, sans le dépasser — et porterait
sa semaine de 20 h à 28 h.

---

## 7. Points d'attention contractuels

**Séparation solveur / API** — la solution OptaPlanner est transformée en `ScenarioResponseDTO` par `ScenarioResponseMapper`. Cette couche garantit la stabilité du contrat API indépendamment des évolutions internes du solveur. Décision documentée dans `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`.

**Unité du `scoreBreakdown`** — l'unité de chaque ligne (`penaliteKey`, `unit`, `quantity`, `weightedImpact`) est déterminée par `PenaliteKey`. Décision documentée dans `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`.

**`workMetrics` et `A_AFFECTER`** — la pseudo-ressource `A_AFFECTER` n'apparaît jamais dans `workMetrics.byRessource`. Seules les ressources réelles y figurent.

**Restitution des données d'identification** — toute donnée reçue permettant d'identifier ou de situer un créneau (`id`, `lieu`) est restituée à l'identique. Le moteur ne normalise pas ces valeurs et ne les remplace pas par une forme canonique.

---

## 8. Évolution prévue

Ce contrat pourra être enrichi progressivement avec :
- de nouvelles WorkMetrics (équité, écarts de charge, métriques contractuelles),
- des indicateurs d'analyse RH.

Ces évolutions n'impacteront pas la structure générale de la réponse.

---

**Fin du document**
