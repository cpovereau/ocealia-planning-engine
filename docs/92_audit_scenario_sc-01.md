# Audit SC-01 — Rapport complet

> ⚠️ **Rapport d'audit daté (2026-03-27) — largement traité depuis. Ne pas lire comme l'état du code.**
> Ce document a servi de base au chantier de stabilisation SC-01, dont les phases A→D sont
> **terminées le 2026-03-30**. L'état à jour est dans `92_suivi_stabilisation_sc-01.md`.
>
> | Problème de l'audit | État réel | Tâche |
> |---|---|---|
> | C1 — référentiel `"travail"` hardcodé, `dataSet.referentiels` ignoré | Corrigé — bloc lu, référentiel interne réduit à un fallback loggé | B1, B2 |
> | C2 — `dataSet.creneaux` ignoré sans avertissement | Corrigé — `log.warn` explicite | A1 |
> | C3 — aucun partitioning pré-résolution | Corrigé — `computeIgnoredCreneaux()` | C1 |
> | C4 — `IgnoredCreneauxDTO(0,0,0)` statique | Corrigé — calculé dynamiquement | C1 |
> | I1 — absence de guards d'entrée | Corrigé — guards horizon, dates, `resourceRef`, `shiftStart` | A4, A5 |
> | I2 — référentiel ignoré sans log | Sans objet — le référentiel n'est plus ignoré | A2, B1 |
> | A4 — pas de log sur les comportements ignorés | Corrigé | A1, A2, A3 |
> | I3 — `contraintesReglementaires` absorbées, `RegulatoryParameters.neutre()` | **Partiel** — `log.warn` ajouté (A3), exploitation toujours incomplète | A3 |
> | I4 — DTO de requête sans base commune | **Toujours ouvert** — tâche C3 du chantier, reportée | C3 |
>
> ⚠️ Attention au double jeu de numéros : `C1…C4` ci-dessus sont les **problèmes de cet audit**,
> `A1…D5` sont les **tâches du chantier** (`92_suivi_stabilisation_sc-01.md`). Les passages devenus
> faux sont barrés et annotés en place.

## Synthèse exécutive

~~SC-01 est **fonctionnel mais structurellement non conforme** à l'architecture stabilisée (Phase 9).~~
Les écarts ne sont pas cosmétiques : plusieurs concernent le pipeline de données fondamental,
rendant SC-01 impossible à faire évoluer proprement sans corrections ciblées.
SC-01 viole le contrat d’entrée Phase 9 en acceptant des données sans les exploiter ni les signaler.

---

## 1. Vue d'ensemble des flux

| Axe | SC-01 | SC-03 |
|-----|-------|-------|
| Source des créneaux | Générés par le builder (paramètres utilisateur) | Fournis dans `dataSet.creneaux` |
| Référentiel activités | ~~**Hardcodé** (`"travail"`)~~ → Fourni dans `dataSet.referentiels`, fallback `"travail"` si absent (✅ B1/B2) | Fourni dans `dataSet.referentiels` |
| Ressource cible | **1 seule** (via `resourceRef`) | Toutes les ressources du dataset |
| Partitioning pré-résolution | Aucun | Activité inconnue + hors-horizon |
| Validations d'entrée | ~~Minimales (type scénario uniquement)~~ → Guards horizon, dates, `resourceRef`, `dailyAmplitudeHours`, `shiftStart` (✅ A4/A5) | Guards complets (horizon, dates, créneaux, référentiels) |
| `IgnoredCreneauxDTO` | ~~Hardcodé `(0, 0, 0)`~~ → Calculé dynamiquement (✅ C1) | Calculé dynamiquement |

---

## 2. Écarts par axe

### 2.1 Contrat d'entrée

| Champ du contrat Phase 9 | Comportement SC-01 | Verdict |
|--------------------------|-------------------|---------|
| `planningContext.horizon` | Utilisé (dateDebut, dateFin) | OK |
| `planningContext.strategieScoring` | Utilisé | OK |
| `dataSet.creneaux` | **Ignoré** — builder génère les siens | ÉCART MAJEUR |
| `dataSet.ressources.salaries` | Utilisé pour résoudre `resourceRef` + value range | OK partiel |
| `dataSet.ressources.postesVirtuels` | Utilisé pour résoudre `resourceRef` + value range | OK partiel |
| `dataSet.referentiels` | ~~**Ignoré** — référentiel hardcodé~~ → **Utilisé** (fallback `"travail"` + warn si absent) | ✅ Corrigé (B1, B2) |
| `dataSet.indisponibilites` | Utilisé (mappé et transmis) | OK |
| `scenarioParameters.resourceRef` | Utilisé | OK |
| `scenarioParameters.dailyAmplitudeHours` | Utilisé | OK |
| `scenarioParameters.shiftStart` | Utilisé | OK |
| `scenarioParameters.shiftEndAlert` | Utilisé | OK |
| `scenarioParameters.lunchBreak` | Utilisé | OK |
| `scenarioParameters.workedDays` | Utilisé | OK |
| `scenarioParameters.holidayDates` | Utilisé | OK |

**Champs ignorés silencieusement :** ~~`dataSet.creneaux`, `dataSet.referentiels`~~ → aucun : `dataSet.creneaux` reste ignoré mais avec `log.warn` (A1), `dataSet.referentiels` est désormais lu (B1)

**Champs implicites / reconstruits :** créneaux (générés par le builder) ~~, référentiel (hardcodé `"travail"`)~~

---

### 2.2 DTOs

| DTO | SC-01 | SC-03 | Écart |
|-----|-------|-------|-------|
| Requête racine | `ScenarioRequestDTO` | `Sc03ScenarioRequestDTO` | Deux classes sans base commune |
| Paramètres scénario | `Sc01ScenarioParametersDTO` | `Sc03ScenarioParametersDTO` | Normal (sémantique différente) |
| Dataset | `DataSetDTO` | `DataSetDTO` | Partagé — OK |
| `SalarieInputDTO` | Utilisé | Utilisé | Partagé — OK |
| `CreneauInputDTO` | Non utilisé en entrée | Utilisé | Divergence |
| `ReferentielActiviteDTO` | Non lu | Lu et mappé | Divergence |
| `ContraintesReglementairesDTO` | Mappé mais `RegulatoryParameters.neutre()` | Idem | Limitation commune |
| `IgnoredCreneauxDTO` en sortie | `(0, 0, 0)` statique | Calculé | ÉCART |

**Champs `SalarieInputDTO` non exploités par SC-01 :**

- `contraintesReglementaires` : mappé en domaine mais rendu ineffectif par `RegulatoryParameters.neutre()`
- La compatibilité historique (`@JsonAlias` / `@JsonIgnoreProperties`) est correctement gérée

---

### 2.3 Dataset

| Question | Réponse SC-01 |
|----------|---------------|
| Dataset entièrement fourni par l'entrée ? | **Non** — `dataSet.creneaux` est vide, les créneaux sont construits par le builder |
| Reconstruit partiellement dans le code ? | **Oui** — créneaux ET référentiel sont construits en dehors du dataset |
| `dataSet.referentiels` exploité ? | **Non** — ignoré sans log ni warning |
| `dataSet.creneaux` exploité ? | **Non** — ignoré sans log ni warning |

---

### 2.4 Builder

Logiques **spécifiques et exclusives à SC-01** dans `ScenarioDatasetBuilderSc01` :

| Logique | Description | Problème |
|---------|-------------|----------|
| Qualification RH/RHD | ~~2 jours non cochés → RH + RHD, 1 non coché → RHD~~ → Dimanche non travaillé = `RHD`, samedi = `RH`, jours restants = `NON_TRAVAILLE` ; `RH` reporté sur le 1er jour non coché si le week-end est travaillé (✅ 2026-07-30) | ~~Règle métier hardcodée sans paramétrage~~ → Règle adossée au week-end, sans seuil arbitraire |
| Génération créneaux par amplitude | Calcul matin/après-midi depuis `shiftStart`, `dailyAmplitudeHours`, `lunchBreak` | Logique non partagée avec SC-03 |
| IDs créneaux `SC01-YYYY-MM-DD-NNN` | Format propriétaire | Inconsistant avec SC-03 |
| Alertes SHIFT / LUNCH / REST | 4 types d'alertes générées côté builder, chacune portant une `severity` `INFO`/`WARNING`/`ERROR` (✅ 2026-07-30) | Spécifique SC-01 |

**Comportements hardcodés :**

- ~~`codeActiviteId` des créneaux générés : absent ou non renseigné depuis un référentiel (le builder ne lit aucun référentiel)~~ — ✅ Corrigé (B3) : le builder renseigne `codeActiviteId = "travail"`, clé de lookup du référentiel injecté
- `isJourFerie` calculé depuis `holidayDates` en entrée — acceptable mais non issu du dataset

---

### 2.5 Référentiel — ✅ CORRIGÉ (Phase B, 2026-03-30)

> Le constat ci-dessous décrit le code **avant** la Phase B. Aujourd'hui,
> `ScenarioSc01PreparationService.buildReferentielSc01()` appelle `resourceMapper.toReferentiel()`
> dès que `dataSet.referentiels` est présent et non vide ; le bloc `map.put("travail", ...)` n'est
> plus qu'un fallback assorti d'un `log.warn`. Les quatre conséquences listées ne s'appliquent donc
> plus qu'au cas où WinDev n'envoie aucun référentiel — cas encore réel côté SC-01 (RF2).

~~**C'est l'écart le plus grave.**~~

`ScenarioSc01PreparationService` construit manuellement le référentiel :

```java
map.put("travail", new ComptabiliteActivite(
    "travail", true, false, false, false,
    TypeImpactActivite.CHARGE_STANDARD
));
ReferentielComptabiliteActivite referentiel = new ReferentielComptabiliteActivite(map);
```

Conséquences directes :

- Si WinDev envoie `codeActiviteId = "ACT-SOIN"`, SC-01 ne connaît pas cette activité — *reste vrai si aucun référentiel n'est transmis ; le compteur `activiteInconnue` le signale désormais (C1)*
- ~~`dataSet.referentiels` est transmis dans le contrat mais n'est **jamais lu**~~ — ✅ lu depuis la Phase B
- `WorkMetricsCalculator` cherche l'activité dans le référentiel : si absent → activité neutre (pas de comptage charge, pas de dette repos)
- Les WorkMetrics sont calculés sur la base d'une activité fictive

---

### 2.6 Solveur et contraintes

SC-01 et SC-03 appellent le même `PlanningService.solve()` avec le même `PlanningRequest`.
Les contraintes OptaPlanner sont identiques. **Pas d'écart sur ce point.**

**Limitation commune (non spécifique à SC-01) :** les deux scénarios utilisent le constructeur
simplifié de `PlanningContext` avec des valeurs par défaut.

---

### 2.7 WorkMetrics / Scoring

| Point | SC-01 | SC-03 | Écart |
|-------|-------|-------|-------|
| Calculateur | `WorkMetricsCalculator` (partagé) | `WorkMetricsCalculator` (partagé) | OK |
| Référentiel utilisé pour le calcul | Hardcodé "travail" | Depuis le dataset | ÉCART |
| `minutesTravaillees` | Comptées si activité "travail" dans référentiel | Comptées si `compteDansCharge=true` | Divergence si codes différents |
| `IgnoredCreneauxDTO` | `(0,0,0)` statique | Calculé | ÉCART |

**Risque concret :** Si les créneaux générés par le builder n'ont pas `codeActiviteId = "travail"`,
le `WorkMetricsCalculator` applique le fallback neutre → `minutesTravaillees = 0` pour tous les créneaux.

---

### 2.8 Architecture

| Principe | SC-01 | SC-03 | Verdict |
|----------|-------|-------|---------|
| Séparation DTO / domaine | Partielle — référentiel construit directement en domaine | Complète via `resourceMapper.toReferentiel()` | ÉCART |
| Séparation solveur / API | OK | OK | OK |
| Usage du `PlanningContext` | Constructeur simplifié (défauts) | Idem | Limitation commune |
| Mapper pour le référentiel | Absent — logique inline dans `PreparationService` | `resourceMapper.toReferentiel()` | ÉCART |
| Validations d'entrée | 1 seule (type scénario) | Guards complets Phase 7 | ÉCART |
| Warnings sur données ignorées | Aucun | Complets | ÉCART |

---

## 3. Classification des problèmes

### Critiques — bloquants pour l'alignement

| # | Problème | Impact |
|---|----------|--------|
| C1 | ~~Référentiel "travail" hardcodé — `dataSet.referentiels` ignoré~~ | ✅ **Corrigé** (B1, B2) — bloc lu, référentiel interne réduit à un fallback loggé |
| C2 | ~~`dataSet.creneaux` ignoré sans avertissement~~ | ✅ **Corrigé** (A1) — `log.warn` émis avec le nombre de créneaux ignorés |
| C3 | Aucun partitioning pré-résolution | ✅ **Corrigé** (C1) — `computeIgnoredCreneaux()` mesure activité inconnue / hors-horizon / sans ressource |
| C4 | ~~`IgnoredCreneauxDTO(0,0,0)` statique~~ | ✅ **Corrigé** (C1) — calculé dynamiquement et propagé via `PreparedSc01Scenario` |

### Importants — risque de comportement incohérent

| # | Problème | Impact |
|---|----------|--------|
| I1 | ~~Absence de guards d'entrée (horizon null, dates incohérentes)~~ | ✅ **Corrigé** (A4, A5) — guards horizon, dates, `resourceRef`, `dailyAmplitudeHours`, `shiftStart` |
| I2 | ~~`dataSet.referentiels` ignoré sans log warn~~ | ✅ **Sans objet** (A2 puis B1) — le référentiel n'est plus court-circuité ; le warn ne subsiste que sur le fallback |
| I3 | `ContraintesReglementairesDTO` mappé mais `RegulatoryParameters.neutre()` | Les contraintes réglementaires fournies sont absorbées en silence et ignorées |
| I4 | DTOs de requête non alignés (pas de classe de base commune) | Duplication à maintenir, risque de divergence future |

### Améliorations — confort et maintenabilité

| # | Problème | Impact |
|---|----------|--------|
| A1 | Tests SC-01 avec `creneaux: []` ne testent pas le pipeline de mappage | Couverture incomplète |
| A2 | Format IDs créneaux `SC01-YYYY-MM-DD-NNN` propriétaire | Inconsistance mineure avec SC-03 |
| A3 | Alertes builder (SHIFT, LUNCH, REST) non testées en isolation | Confiance limitée dans ces règles |
| A4 | Pas de log sur comportements ignorés — asymétrie avec SC-03 | Comportement non traçable |

---

## 4. Recommandations

### R1 — Injecter le référentiel depuis le contrat (adresse C1, I2) — ✅ APPLIQUÉE (Phase B, 2026-03-30)

Remplacer la construction inline du référentiel hardcodé dans `ScenarioSc01PreparationService`
par un appel à `resourceMapper.toReferentiel()` en lisant `request.getDataSet().getReferentiels()`.

Si `referentiels` est absent ou vide : construire un référentiel neutre **avec un `log.warn` explicite**.

> Implémentée telle quelle dans `buildReferentielSc01()`, à une nuance près : le fallback n'est pas
> un référentiel *neutre* (map vide) mais un référentiel minimal `{"travail": ...}`, pour que les
> créneaux générés par le builder restent comptabilisés en l'absence de bloc `referentiels`.

### R2 — Avertir que `dataSet.creneaux` est ignoré (adresse C2)

Ajouter dans `ScenarioSc01PreparationService` :

```java
if (request.getDataSet().getCreneaux() != null &&
    !request.getDataSet().getCreneaux().isEmpty()) {
    log.warn("[SC-01] dataSet.creneaux contient {} éléments — ignorés " +
             "(SC-01 génère ses créneaux via le builder)",
        request.getDataSet().getCreneaux().size());
}
```

Cela documente le comportement sans casser SC-01.

### R3 — Aligner les validations d'entrée (adresse I1)

Ajouter les guards minimaux inspirés de SC-03 :

- `planningContext` non null
- `planningContext.horizon` non null
- `dateDebut` et `dateFin` non null
- `dateDebut` ≤ `dateFin`
- `scenarioParameters` non null
- `resourceRef` non null

### R4 — Corriger `IgnoredCreneauxDTO` (adresse C4)

Pour SC-01, le `(0,0,0)` statique est acceptable **à condition que R2 soit appliqué**
(warning explicite sur les créneaux ignorés). Le calcul dynamique sera pertinent
si SC-01 évolue pour accepter des créneaux depuis le dataset (Phase 4).

### R5 — Documenter `ContraintesReglementairesDTO` ignoré (adresse I3)

Ajouter un `log.warn` si des contraintes réglementaires sont fournies mais
que `RegulatoryParameters.neutre()` est utilisé, pour que le comportement soit traçable.

---

## 5. Plan de correction

### Phase 1 — Corrections immédiates (sans risque de régression)

| Étape | Action | Risque |
|-------|--------|--------|
| 1.1 | Ajouter warning `dataSet.creneaux` ignoré (R2) | Nul — ajout de log seulement |
| 1.2 | Ajouter warning `dataSet.referentiels` ignoré si non vide | Nul — ajout de log seulement |
| 1.3 | Ajouter warning `ContraintesReglementairesDTO` ignoré (R5) | Nul — ajout de log seulement |
| 1.4 | Ajouter guards d'entrée (R3) | Faible — rejette des entrées mal formées qui planteraient de toute façon |

**Livrable :** SC-01 transparent sur ses limitations, robuste aux entrées mal formées.

---

### Phase 2 — Injection du référentiel (correction structurelle principale)

| Étape | Action | Risque | Dépendance |
|-------|--------|--------|------------|
| 2.1 | Remplacer référentiel hardcodé par `resourceMapper.toReferentiel()` (R1) | Moyen — comportement WorkMetrics change si codes activité ≠ "travail" | Phase 1 terminée |
| 2.2 | Vérifier que les créneaux générés par le builder ont un `codeActiviteId` cohérent avec le référentiel fourni | Moyen | 2.1 |
| 2.3 | Mettre à jour `sc01_dataset_reference.json` avec un bloc `referentiels` non vide | Faible | 2.1 |
| 2.4 | Vérifier les WorkMetrics dans les tests après changement de référentiel | Faible | 2.1, 2.3 |

**Livrable :** SC-01 utilise le référentiel métier réel. WorkMetrics cohérents.

**Point d'attention critique :** Le builder `ScenarioDatasetBuilderSc01` doit renseigner `codeActiviteId`
sur les créneaux qu'il génère, avec un code présent dans le référentiel fourni. Sans ça, tous les
créneaux générés seront traités comme "activité inconnue" par le `WorkMetricsCalculator`.

---

### Phase 3 — Alignement architecture (moyen terme)

| Étape | Action | Risque | Dépendance |
|-------|--------|--------|------------|
| 3.1 | Calculer `IgnoredCreneauxDTO` dynamiquement plutôt que statique (R4) | Faible | Phase 2 |
| 3.2 | Extraire la construction du référentiel vers `resourceMapper` (cohérence) | Faible | Phase 2 |
| 3.3 | Évaluer une base commune à `ScenarioRequestDTO` et `Sc03ScenarioRequestDTO` (I4) | Faible — refactoring | Phase 2 |

**Livrable :** SC-01 architecturalement aligné sur SC-03.

---

### Phase 4 — Migration créneaux vers dataset (long terme, décision stratégique)

Cette phase n'est recommandée qu'**après validation des phases 1 à 3**.

| Étape | Action | Risque | Dépendance |
|-------|--------|--------|------------|
| 4.1 | Décider si SC-01 reçoit des créneaux depuis `dataSet.creneaux` ou continue à les générer | Décision produit | Phase 3 |
| 4.2 | Si génération conservée : documenter explicitement dans le schéma JSON que `dataSet.creneaux` est ignoré | Faible | 4.1 |
| 4.3 | Si migration vers dataset : implémenter le partitioning (activité inconnue, hors-horizon) | Fort | 4.1 |

**Note :** La génération paramétrique de créneaux par SC-01 est une fonctionnalité métier légitime
(planification d'une semaine type). Elle n'a pas à disparaître. La question est de savoir si elle
coexiste ou remplace la logique dataset.

---

## 6. Tableau de synthèse

| # | Écart | Classe | Phase de correction |
|---|-------|--------|---------------------|
| C1 | Référentiel "travail" hardcodé | Critique | Phase 2 |
| C2 | `dataSet.creneaux` ignoré sans avertissement | Critique | Phase 1 |
| C3 | Pas de partitioning pré-résolution | Critique | Phase 4 |
| C4 | `IgnoredCreneauxDTO(0,0,0)` statique | Critique | Phase 3 |
| I1 | Absence de guards d'entrée | Important | Phase 1 |
| I2 | `dataSet.referentiels` ignoré sans log | Important | Phase 1 |
| I3 | `ContraintesReglementairesDTO` ignoré silencieusement | Important | Phase 1 |
| I4 | DTOs requête sans base commune | Important | Phase 3 |
| A1 | Tests incomplets (creneaux vides) | Amélioration | Phase 2 |
| A2 | IDs créneaux propriétaires | Amélioration | Phase 4 |
| A3 | Alertes builder non testées en isolation | Amélioration | Phase 3 |
| A4 | Pas de log sur comportements ignorés | Amélioration | Phase 1 |

---

## 7. Ce qui va bien dans SC-01

- **Mappers partagés :** `ScenarioResourceMapper`, `ScenarioCreneauMapper`, `ScenarioResponseMapper` sont utilisés correctement
- **Indisponibilités :** correctement mappées et transmises au `PlanningRequest`
- **Solveur identique :** `PlanningService.solve()` appelé de la même façon que SC-03
- **`WorkMetricsCalculator` partagé :** pas de duplication de logique de calcul
- **Alias historiques :** (`lieuxAutorises`, `activitesAutorisees`) gérés proprement au niveau DTO
- **Builder SC-01 :** les règles RH/RHD et les alertes (SHIFT, LUNCH, REST) sont une vraie valeur métier

---

## Conclusion

La priorité absolue est la **Phase 1** (logs et guards) : sans risque, apporte une transparence
immédiate sur les limitations de SC-01.

La **Phase 2** (injection du référentiel) est l'écart structurel le plus grave et doit suivre
immédiatement après.

La question de la migration des créneaux vers le dataset (Phase 4) est une décision stratégique
produit qui ne doit pas bloquer les phases 1 à 3.
