# 20 — Planning Context

Ce document décrit le rôle, le périmètre et les invariants du `PlanningContext` transmis au moteur de planification.

Il constitue la **référence documentaire unique** pour tout ce qui relève :
- du cadre temporel de résolution,
- du contexte réglementaire applicable,
- de la stratégie de scoring,
- et des hypothèses nécessaires à l'interprétation correcte d'un scénario.

---

## 1. Définition du contexte de résolution

Le `PlanningContext` est un **objet de contexte immuable** fourni au moteur par l'appelant.

Il ne représente ni :
- une décision du solveur,
- une donnée métier planifiée,
- ni un résultat calculé.

Son rôle est de porter le **cadre de jugement** dans lequel le moteur doit évaluer les affectations.

> Le moteur optimise des affectations.
> Le `PlanningContext` lui dit **dans quel cadre** ces affectations doivent être jugées.

### Principe fondamental

> **OptaPlanner ne choisit jamais l'horizon temporel ni aucun paramètre de cadre.**
> Il reçoit un contexte de résolution et juge les décisions à l'intérieur de ce cadre.

Le moteur n'interprète pas l'intention utilisateur : il évalue des décisions dans un cadre **explicite et imposé**.

### Ce que le PlanningContext n'est pas

Le `PlanningContext` n'est pas :
- un scénario métier,
- un référentiel d'activité,
- un objet de restitution,
- une variable de décision,
- ni un mécanisme de contournement des contraintes.

Il ne doit jamais contenir de logique du type :
- « désactiver telle contrainte pour ce scénario »
- « tolérer telle violation sans traçabilité »
- « reconstruire une donnée absente du dataset »

> Le `PlanningContext` cadre.
> Il ne triche jamais avec le moteur.

---

## 2. Horizon temporel

### 2.1 Responsabilité de l'appelant

Le cadre temporel est **construit et transmis par l'appelant** (WebDev / API) à partir de la demande utilisateur.

L'appelant est responsable de :
- l'interprétation métier de la demande (planification sur période, cycle, remplacement…),
- la construction du `PlanningContext` temporel,
- la transmission de ce contexte au moteur.

Le moteur ne recalcule, ne complète et n'interprète jamais ce cadre.

### 2.2 Fenêtre de résolution

Le contexte doit définir explicitement :
- `dateDebut`
- `dateFin`

Cette fenêtre :
- borne l'espace de décision du solveur,
- borne les WorkMetrics,
- borne la lecture des séquences,
- borne les diagnostics.

Le moteur n'invente jamais de date en dehors de cette fenêtre.

### 2.3 Horizons réglementaires par famille de règles

Pour chaque grande famille de règles, l'horizon de validité applicable est précisé explicitement :

| Famille de règles               | Horizon applicable                                     |
| ------------------------------- | ------------------------------------------------------ |
| Repos quotidien                 | Fenêtre de résolution (ou étendue explicitement)       |
| Repos hebdomadaire              | Fenêtre de résolution (ou étendue explicitement)       |
| Heures supplémentaires / comp.  | Peut dépasser partiellement la fenêtre de résolution   |
| Dette de repos compensateur     | Horizon de restitution distinct, déclaré explicitement |

Ces horizons peuvent coïncider avec la fenêtre de résolution ou la dépasser partiellement.
Dans tous les cas, ils sont **transmis explicitement** et non déduits par le moteur.

### 2.4 Invariants temporels

- Le cadre temporel est **toujours explicite** : aucune date implicite.
- Toute dette générée doit être **visible dans les résultats**.
- Le moteur **n'altère jamais le passé**.
- Aucune règle ne s'applique en dehors des horizons déclarés.
- Le moteur ne prolonge jamais l'analyse au-delà de la fenêtre transmise.

---

## 3. Cadre réglementaire

Le moteur a besoin de paramètres réglementaires explicites pour évaluer les affectations dans leur contexte légal.

### 3.1 Paramètres attendus

| Paramètre                       | Rôle                                                          |
| ------------------------------- | ------------------------------------------------------------- |
| Plage de nuit                   | Définit les heures constituant une nuit réglementaire         |
| Jours fériés                    | Liste des jours fériés applicables sur l'horizon              |
| Repos quotidien minimum         | Borne légale entre deux affectations consécutives             |
| Repos hebdomadaire minimum      | Borne légale sur la semaine                                   |
| Ordre de dominance pénibilités  | Priorité en cas de chevauchement (nuit > dimanche > férié)    |

### 3.2 Portage des paramètres

Selon l'architecture retenue, ces paramètres peuvent être :
- portés directement par le `PlanningContext`,
- ou référencés par lui puis injectés comme `ProblemFact` dans le monde solveur.

Dans tous les cas :
- leur origine doit être **traçable**,
- leur rôle doit être **explicite**,
- le moteur ne les invente jamais.

---

## 4. Paramètres de résolution

### 4.1 Type de résolution

Le contexte porte un type métier explicite, par exemple :
- `PLANNING_GLOBAL`
- `CYCLE`
- `REMPLACEMENT`
- `PROJECTION`

Ce type :
- ne change jamais la nature des contraintes,
- n'influence pas directement le scoring,
- peut uniquement influencer la lecture des résultats ou leur interprétation côté métier ou UI.

### 4.2 Stratégie de scoring

Le `PlanningContext` porte la `StrategieScoring` utilisée pour lire les arbitrages du solveur.

Exemples :
- `EXPLOITATION`
- `ANALYSE_RH`
- `AUDIT`

Principe :
- la stratégie n'est pas une règle métier,
- elle ne réécrit pas les contraintes,
- elle fournit un **contexte de pondération** via `ScoreWeights`.

### 4.3 Seuils de tolérance

Le contexte porte les seuils nécessaires à l'évaluation des contraintes lorsque ces seuils ne relèvent pas d'un invariant figé du moteur.

Exemples :
- maximum de jours consécutifs,
- maximum de nuits consécutives,
- seuils d'alerte sur charge ou dette,
- limites propres à un cadre client.

Règles :
- aucun seuil implicite silencieux,
- toute valeur absente doit être rejetée ou remplacée par un défaut **explicite et documenté**,
- un seuil fourni par le contexte ne doit jamais contredire les invariants structurels du moteur.

---

## 5. Hypothèses de résolution

### 5.1 Hypothèses d'historique

Le contexte doit rendre explicite ce que le moteur sait du passé.

Exemples :
- historique neutre,
- compteurs initiaux fournis (heures déjà réalisées, dettes existantes),
- dette antérieure déjà connue,
- séquence de travail déjà entamée avant l'horizon.

Principe :
- le moteur ne reconstitue jamais l'historique,
- il exploite uniquement ce qui lui est transmis,
- toute hypothèse de neutralité doit être explicite.

### 5.2 Ce que le moteur fait — et ne fait pas

**Le moteur :**
- évalue des affectations dans la fenêtre fournie,
- calcule des indicateurs dérivés (charges, dettes, coûts),
- applique les contraintes dans les horizons définis.

**Le moteur ne :**
- choisit pas la période,
- n'interprète pas l'intention utilisateur,
- n'extrapole pas au-delà des horizons transmis,
- ne reconstitue pas les semaines antérieures,
- n'invente aucun paramètre absent du contexte.

---

## 6. Invariants de conception

Les invariants suivants s'appliquent au `PlanningContext`.

### 6.1 Explicite
Tout ce qui influence la lecture d'une solution doit être explicite dans le contexte.

### 6.2 Immuable
Le contexte n'est pas modifié pendant la résolution.

### 6.3 Traçable
Un résultat moteur doit toujours pouvoir être relié au contexte qui a servi à le produire.

### 6.4 Non décisionnel
Le contexte ne décide pas à la place du solveur.

### 6.5 Non compensatoire
Une information absente du dataset ne doit pas être "devinée" via le contexte.

### 6.6 Non extrapolant
Aucune règle ne s'applique en dehors des horizons déclarés dans le contexte.

---

## 7. Interaction avec les autres briques

Le `PlanningContext` est consommé ou utilisé par plusieurs composants.

| Composant                              | Rôle du PlanningContext                                              |
| -------------------------------------- | -------------------------------------------------------------------- |
| `ScenarioDatasetBuilder`               | cadre de construction du monde solveur                               |
| `PlanningProblem` / `PlanningSolution` | portage du contexte de résolution                                    |
| `ConstraintProvider`                   | lecture des seuils / stratégie / horizons                            |
| `WorkMetricsCalculator`                | bornage des calculs post-résolution                                  |
| `ScenarioResponseMapper`               | contextualisation et restitution (sans impact sur la logique métier) |

Le `PlanningContext` ne remplace aucun de ces composants.
Il fournit un cadre partagé entre eux.

---

## 8. Exemple de lecture correcte

Un scénario de type `REMPLACEMENT` sur 5 jours peut fournir :
- un horizon borné sur 5 jours,
- une stratégie `EXPLOITATION`,
- un historique neutre,
- une plage de nuit explicite,
- des seuils réglementaires standard.

Le moteur :
- ne déduit pas qu'il s'agit d'un remplacement à partir du dataset,
- ne prolonge pas l'analyse au-delà des 5 jours,
- ne reconstruit pas les semaines antérieures,
- applique les contraintes et le scoring à l'intérieur du cadre fourni.

---

## 9. Risques évités par ce design

Le `PlanningContext` existe pour éviter plusieurs dérives :
- contexte implicite variable selon les scénarios,
- règles cachées dans les builders,
- paramètres réglementaires diffus dans plusieurs couches,
- tests non reproductibles,
- résultats impossibles à expliquer a posteriori.

---

## 10. Lien avec les autres documents

Ce document complète directement :
- `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`
- `20_DATASET_BUILDER.md`
- `50_SCENARIO_CONTRACT.md`
- `60_TESTING_STRATEGY_ENGINE.md`

Il doit rester cohérent avec :
- le contrat d'entrée,
- le modèle de résolution,
- et les factories de test.

---

## 11. Statut du document

- Document de référence
- Normatif sur le rôle du contexte
- À maintenir à chaque évolution de `PlanningContext`

---

**Fin du document**
