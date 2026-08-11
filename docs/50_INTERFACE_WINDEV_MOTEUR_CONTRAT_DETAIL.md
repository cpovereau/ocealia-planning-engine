# 50 — Contrat d'API WinDev ↔ Moteur (détail de la requête)

Ce document est la **référence normative champ par champ de la requête** envoyée par WinDev
au moteur de planification pour le scénario SC-01.

Il complète `50_INTERFACE_WINDEV_MOTEUR_CONTRAT.md` (structure globale et règles générales).

Pour la description de la réponse, voir `50_SCENARIO_RESPONSE_CONTRACT.md`.
Pour les exemples JSON complets, voir `50_INTERFACE_WINDEV_MOTEUR_EXEMPLES.md`.

---

## 1. Niveau racine

| Champ                | Type   | Requis | Valeurs acceptées | Notes                               |
| -------------------- | ------ | :----: | ----------------- | ----------------------------------- |
| `scenarioType`       | string | Oui    | `"SC-01"`         | Seule valeur supportée actuellement |
| `planningContext`    | objet  | Oui    | voir §2           |                                     |
| `scenarioParameters` | objet  | Oui    | voir §3           |                                     |
| `dataSet`            | objet  | Oui    | voir §4           |                                     |

---

## 2. `planningContext`

| Champ               | Type   | Requis | Valeurs acceptées                          | Notes                             |
| ------------------- | ------ | :----: | ------------------------------------------ | --------------------------------- |
| `horizon`           | objet  | Oui    | voir ci-dessous                            |                                   |
| `horizon.dateDebut` | string | Oui    | date ISO-8601 `YYYY-MM-DD`                 | doit être ≤ `dateFin`             |
| `horizon.dateFin`   | string | Oui    | date ISO-8601 `YYYY-MM-DD`                 | doit être ≥ `dateDebut`           |
| `strategieScoring`  | string | Non    | `EXPLOITATION` \| `ANALYSE_RH` \| `AUDIT` | Défaut : `EXPLOITATION` si absent |

Sémantique des stratégies :

| Valeur        | Comportement                                                              |
| ------------- | ------------------------------------------------------------------------- |
| `EXPLOITATION`| Coeff ×5 sur les créneaux non couverts — pénalise le manque de couverture |
| `ANALYSE_RH`  | Coeff ×2 — vision équilibrée pour analyse des charges                     |
| `AUDIT`       | Coeff ×3 — focus sur la traçabilité et l'explicabilité                    |

---

## 3. `scenarioParameters` (SC-01)

| Champ                 | Type            | Requis                      | Format / Valeurs                 | Notes                                                   |
| --------------------- | --------------- | :-------------------------: | -------------------------------- | ------------------------------------------------------- |
| `resourceRef`         | objet           | Oui                         | voir ci-dessous                  | Ressource principale du scénario                        |
| `resourceRef.kind`    | string          | Oui                         | `SALARIE` \| `POSTE_VIRTUEL`     |                                                         |
| `resourceRef.id`      | string          | Oui                         | identifiant de la ressource      | Doit exister dans `dataSet.ressources`                  |
| `dailyAmplitudeHours` | number (double) | Oui                         | ex : `8.0`                       | Amplitude journalière **pause incluse**, en heures      |
| `shiftStart`          | string          | Oui                         | format `HH:mm` — ex : `"08:00"` | Heure de début de poste                                 |
| `shiftEndAlert`       | string          | Non                         | format `HH:mm` — ex : `"17:00"` | Heure de fin d'alerte (déclenche `SHIFT_END_EXCEEDED`)  |
| `lunchBreak`          | objet           | Non                         | voir ci-dessous                  | Pause déjeuner optionnelle                              |
| `lunchBreak.start`    | string          | Oui si `lunchBreak` présent | `HH:mm` — ex : `"12:00"`        |                                                         |
| `lunchBreak.end`      | string          | Oui si `lunchBreak` présent | `HH:mm` — ex : `"13:00"`        |                                                         |
| `workedDays`          | tableau string  | Oui                         | valeurs Java `DayOfWeek` long    | Voir règle de normalisation — `50_INTERFACE_WINDEV_MOTEUR_CONTRAT.md` §4.1 |
| `holidayDates`        | tableau string  | Non                         | dates ISO-8601 `YYYY-MM-DD`      | Jours fériés exclus du planning                         |
| `codeActiviteId`      | string          | Non *(transitoire)*         | code du référentiel client       | Code porté par les créneaux générés. Doit figurer dans `dataSet.referentiels.activites`. Absent → repli `travail` + alerte `ACTIVITY_CODE_DEFAULTED`. **Deviendra obligatoire** |

Valeurs valides pour `workedDays` :

```text
MONDAY  TUESDAY  WEDNESDAY  THURSDAY  FRIDAY  SATURDAY  SUNDAY
```

Les formats abrégés (`MON`, `TUE`, etc.) sont **rejetés**.

---

## 4. `dataSet`

| Champ                       | Type    | Requis | Notes                                                               |
| --------------------------- | ------- | :----: | ------------------------------------------------------------------- |
| `ressources`                | objet   | Oui    |                                                                     |
| `ressources.salaries`       | tableau | Oui    | Liste des salariés disponibles                                      |
| `ressources.postesVirtuels` | tableau | Non    | Postes virtuels (capacité de remplacement)                          |
| `creneaux`                  | tableau | Non    | **Toléré mais ignoré en SC-01** — le moteur génère ses propres créneaux |

> **Portée de cette note** : `dataSet.creneaux` est bien mappé par le moteur — c'est l'entrée
> principale de SC-03. C'est SC-01 qui l'ignore, parce qu'il produit lui-même les créneaux à
> partir de `scenarioParameters`. Les créneaux ainsi générés portent un identifiant sous préfixe
> `SC01-<date>-<séquence>` : il ne désigne aucune ligne de la base WinDev.
> Voir `50_SCENARIO_CONTRACT.md` §3.5 — *Identité des créneaux*.

### 4.1 Structure d'un salarié (`ressources.salaries[]`)

| Champ                         | Type           | Requis | Notes                                    |
| ----------------------------- | -------------- | :----: | ---------------------------------------- |
| `id`                          | string         | Oui    | Identifiant unique                       |
| `type`                        | string         | Non    | `"SALARIE"` (informatif)                 |
| `capaciteCible`               | integer        | Non    | Capacité contractuelle cible en minutes  |
| `activitesAutorisees`         | tableau string | Non    | Codes activité autorisés                 |
| `lieuxAutorises`              | tableau string | Non    | Codes lieu autorisés                     |
| `postesComptablesCompatibles` | tableau string | Non    | Codes poste comptable compatibles        |

### 4.2 Structure d'un poste virtuel (`ressources.postesVirtuels[]`)

| Champ                         | Type           | Requis | Notes                                                |
| ----------------------------- | -------------- | :----: | ---------------------------------------------------- |
| `id`                          | string         | Oui    | Identifiant unique                                   |
| `type`                        | string         | Non    | `"POTENTIEL"` ou autre valeur de `TypePosteVirtuel`  |
| `capaciteCible`               | integer        | Non    | En minutes                                           |
| `activitesAutorisees`         | tableau string | Non    |                                                      |
| `lieuxAutorises`              | tableau string | Non    |                                                      |
| `postesComptablesCompatibles` | tableau string | Non    |                                                      |

> **Note architecturale** : actuellement, `ressources.salaries` et `ressources.postesVirtuels`
> sont désérialisés directement vers les classes domaine `SalarieReel` et `PosteVirtuel`.
> C'est le couplage identifié dans `50_INTERFACE_WINDEV_MOTEUR.md` §5.
> Des DTO de transport dédiés seront introduits lors d'une prochaine phase.

---

## 5. Bloc `contraintesReglementaires` par salarié

Ce bloc est **défini dans le schéma JSON** (`50_ScenarioContract.schema.json`) et
**prévu dans le plan de migration** (Phase 1 : transport, Phase 3 : utilisation par le builder).

Il n'est **pas encore transporté ni utilisé par le moteur** à ce stade.

### 5.1 Structure cible

| Champ                         | Type    | Unité  | Description                                                                                       |
| ----------------------------- | ------- | ------ | ------------------------------------------------------------------------------------------------- |
| `heuresMinimumParJour`        | number  | heures | Durée minimale de travail effectif par jour travaillé                                             |
| `heuresMaximumParJour`        | number  | heures | Durée maximale de travail effectif par jour (hors pause)                                          |
| `amplitudeJournaliereMaximum` | number  | heures | Amplitude maximale entre la première heure de début et la dernière heure de fin dans une journée  |
| `reposQuotidienMinimum`       | number  | heures | Durée minimale de repos obligatoire entre deux journées de travail                                |
| `heuresMinimumParSemaine`     | number  | heures | Volume horaire minimum attendu sur une semaine calendaire                                         |
| `heuresMaximumParSemaine`     | number  | heures | Volume horaire maximum autorisé sur une semaine calendaire                                        |
| `nuitsMaximumParSemaine`      | integer | nuits  | Nombre maximal de nuits travaillées sur une semaine calendaire                                    |
| `joursConsecutifsMaximum`     | integer | jours  | Nombre maximal de jours consécutifs travaillés (toutes activités confondues)                      |

### 5.2 État d'implémentation

| Contrainte                    | Schéma JSON | `SalarieReel` | Contrainte solver |
| ----------------------------- | :---------: | :-----------: | :---------------: |
| `heuresMinimumParJour`        | ✓           | ✗             | ✗                 |
| `heuresMaximumParJour`        | ✓           | ✗             | ✗                 |
| `amplitudeJournaliereMaximum` | ✓           | ✗             | ✗                 |
| `reposQuotidienMinimum`       | ✓           | ✗             | ✗                 |
| `heuresMinimumParSemaine`     | ✓           | ✗             | ✗                 |
| `heuresMaximumParSemaine`     | ✓           | ✗             | ✗                 |
| `nuitsMaximumParSemaine`      | ✓           | ✗             | ✗ (*)             |
| `joursConsecutifsMaximum`     | ✓           | ✗             | ✗                 |

(*) Deux règles distinctes portent sur les nuits et ne doivent pas être confondues :
`nuitsMaximumParSemaine` borne un **volume** sur la semaine calendaire, tandis que
`nuitsConsecutivesMaximum` borne un **enchaînement**. Trois nuits lundi, mercredi et vendredi ne
violent aucun enchaînement mais peuvent dépasser un volume de deux. Les deux sont désormais des
seuils **par salarié**, portés par `contraintesReglementaires` et exploités depuis les lots S7.2
et S7.7. Le seuil global `maxNuitsConsecutives` de `SeuilsDeTolerance` mentionné ici auparavant
n'existe plus : jamais alimenté, il a été retiré au lot S7.8.

### 5.3 Prochaines étapes

Selon `90_PLAN_MIGRATION_TEMPORAIRE_WINDEV_VERS_MOTEUR.md` :

1. **Phase 1** — Transporter le JSON sans l'exploiter (aucun impact sur le solveur)
2. **Phase 3** — Mapper vers un objet `ContraintesReglementairesSalarie` sur `SalarieReel`
3. **Phase 4+** — Activer les contraintes solver correspondantes une par une

---

**Fin du document**
