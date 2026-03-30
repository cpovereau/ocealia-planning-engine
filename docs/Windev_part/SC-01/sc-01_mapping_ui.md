# SC-01 — Mapping UI ↔ JSON

## Contexte

| UI            | JSON                              | Type  | Obligatoire |
| ------------- | --------------------------------- | ----- | ----------- |
| Type scénario | scenarioType                      | caché | Oui         |
| Date début    | planningContext.horizon.dateDebut | date  | Oui         |
| Date fin      | planningContext.horizon.dateFin   | date  | Oui         |
| Stratégie     | planningContext.strategieScoring  | liste | Oui         |

---

## Paramètres SC-01

| UI                    | JSON                                   | Type        | Obligatoire |
| --------------------- | -------------------------------------- | ----------- | ----------- |
| Ressource             | scenarioParameters.resourceRef.id      | texte       | Oui         |
| Amplitude journalière | scenarioParameters.dailyAmplitudeHours | nombre      | Oui         |
| Heure début           | scenarioParameters.shiftStart          | heure       | Oui         |
| Jours travaillés      | scenarioParameters.workedDays          | liste       | Oui         |
| Pause début           | scenarioParameters.lunchBreak.start    | heure       | Non         |
| Pause fin             | scenarioParameters.lunchBreak.end      | heure       | Non         |
| Jours fériés          | scenarioParameters.holidayDates        | liste dates | Non         |

---

## Dataset (partiel)

### Référentiel

| UI            | JSON                                      | Type  | Obligatoire |
| ------------- | ----------------------------------------- | ----- | ----------- |
| Code activité | referentiels.activites[].codeActiviteId   | texte | Non         |
| Compte charge | referentiels.activites[].compteDansCharge | bool  | Non         |

---

## Important — comportement SC-01

```text
dataSet.creneaux est ignoré
```

```text
les créneaux sont générés par le moteur
```

---

## Synthèse UI

* peu de champs
* logique paramétrique
* pas de saisie de créneaux

👉 Interface très légère comparée à SC-03
