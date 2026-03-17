# 🧭 PLANNING_CONTEXT.md

Ce document décrit le rôle, le périmètre et les invariants du `PlanningContext` transmis au moteur de planification.

Il constitue la **référence documentaire unique** pour tout ce qui relève :
- du cadre temporel de résolution,
- du contexte réglementaire applicable,
- de la stratégie de scoring,
- et des hypothèses nécessaires à l’interprétation correcte d’un scénario.

---

## 1. Rôle du PlanningContext

Le `PlanningContext` est un **objet de contexte immuable** fourni au moteur par l’appelant.

Il ne représente ni :
- une décision du solveur,
- une donnée métier planifiée,
- ni un résultat calculé.

Son rôle est de porter le **cadre de jugement** dans lequel le moteur doit évaluer les affectations.

> Le moteur optimise des affectations.
> Le `PlanningContext` lui dit **dans quel cadre** ces affectations doivent être jugées.

---

## 2. Responsabilité du PlanningContext

Le `PlanningContext` porte explicitement les informations qui ne doivent jamais être déduites implicitement par le moteur.

Il regroupe notamment :
- l’**horizon temporel**,
- le **type de résolution**,
- la **stratégie de scoring**,
- les **seuils de tolérance**,
- les **hypothèses d’historique**,
- et, selon le modèle retenu, les **paramètres réglementaires** ou la manière d’y accéder.

Le moteur consomme ce contexte mais ne l’interprète pas au-delà de ce qui est explicitement transmis.

---

## 3. Ce que le PlanningContext n’est pas

Le `PlanningContext` n’est pas :
- un scénario métier,
- un référentiel d’activité,
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

## 4. Contenu minimal attendu

## 4.1 Horizon temporel

Le contexte doit définir explicitement :
- `dateDebut`
- `dateFin`

Cet horizon :
- borne la résolution,
- borne les WorkMetrics,
- borne la lecture des séquences,
- borne les diagnostics.

Le moteur n’invente jamais de date en dehors de cette fenêtre.

---

## 4.2 Type de résolution

Le contexte doit porter un type métier explicite, par exemple :
- `PLANNING_GLOBAL`
- `CYCLE`
- `REMPLACEMENT`
- `PROJECTION`

Ce type :
- ne change jamais la nature des contraintes,
- n’influence pas directement le scoring,
- peut uniquement influencer la lecture des résultats
  ou la manière dont ils sont interprétés côté métier ou UI.

---

## 4.3 Stratégie de scoring

Le `PlanningContext` porte la `StrategieScoring` utilisée pour lire les arbitrages du solveur.

Exemples :
- `EXPLOITATION`
- `ANALYSE_RH`
- `AUDIT`

Principe :
- la stratégie n’est pas une règle métier,
- elle ne réécrit pas les contraintes,
- elle fournit un **contexte de pondération** via `ScoreWeights`.

---

## 4.4 Seuils de tolérance

Le contexte porte les seuils nécessaires à l’évaluation des contraintes lorsque ces seuils ne relèvent pas d’un invariant figé du moteur.

Exemples :
- maximum de jours consécutifs,
- maximum de nuits consécutives,
- seuils d’alerte sur charge ou dette,
- limites propres à un cadre client.

Règles :
- aucun seuil implicite silencieux,
- toute valeur absente doit être rejetée ou remplacée par un défaut **explicite et documenté**,
- un seuil fourni par le contexte ne doit jamais contredire les invariants structurels du moteur.

---

## 4.5 Hypothèses d’historique

Le contexte doit rendre explicite ce que le moteur sait du passé.

Exemples :
- historique neutre,
- compteurs initiaux fournis,
- dette antérieure déjà connue,
- séquence de travail déjà entamée avant l’horizon.

Principe :
- le moteur ne reconstitue jamais l’historique,
- il exploite uniquement ce qui lui est transmis,
- toute hypothèse de neutralité doit être explicite.

---

## 4.6 Paramètres réglementaires

Le moteur a besoin de paramètres réglementaires explicites pour :
- la plage de nuit,
- les jours fériés,
- certaines bornes légales,
- l’ordre de dominance applicable en cas de chevauchement de pénibilités.

Selon l’architecture retenue, ces paramètres peuvent être :
- portés directement par le `PlanningContext`,
- ou référencés par lui puis injectés comme `ProblemFact` dans le monde solveur.

Dans tous les cas :
- leur origine doit être traçable,
- leur rôle doit être explicite,
- et le moteur ne les invente jamais.

---

## 5. Invariants de conception

Les invariants suivants s’appliquent au `PlanningContext`.

### 5.1 Explicite
Tout ce qui influence la lecture d’une solution doit être explicite.

### 5.2 Immuable
Le contexte n’est pas modifié pendant la résolution.

### 5.3 Traçable
Un résultat moteur doit toujours pouvoir être relié au contexte qui a servi à le produire.

### 5.4 Non décisionnel
Le contexte ne décide pas à la place du solveur.

### 5.5 Non compensatoire
Une information absente du dataset ne doit pas être “devinée” via le contexte.

---

## 6. Interaction avec les autres briques

Le `PlanningContext` est consommé ou utilisé par plusieurs composants.

| Composant                              | Rôle du PlanningContext                                              |
|----------------------------------------|----------------------------------------------------------------------|
| `ScenarioDatasetBuilder`               | cadre de construction du monde solveur                               |
| `PlanningProblem` / `PlanningSolution` | portage du contexte de résolution                                    |
| `ConstraintProvider`                   | lecture des seuils / stratégie / horizons                            |
| `WorkMetricsCalculator`                | bornage des calculs post-résolution                                  |
| `ScenarioResponseMapper`               | contextualisation et restitution (sans impact sur la logique métier) |

Le `PlanningContext` ne remplace aucun de ces composants.
Il fournit un cadre partagé entre eux.

---

## 7. Exemple de lecture correcte

Un scénario de type `REMPLACEMENT` sur 5 jours peut fournir :
- un horizon borné sur 5 jours,
- une stratégie `EXPLOITATION`,
- un historique neutre,
- une plage de nuit explicite,
- des seuils réglementaires standard.

Le moteur :
- ne déduit pas qu’il s’agit d’un remplacement à partir du dataset,
- ne prolonge pas l’analyse au-delà des 5 jours,
- ne reconstruit pas les semaines antérieures,
- applique les contraintes et le scoring à l’intérieur du cadre fourni.

---

## 8. Risques évités par ce design

Le `PlanningContext` existe pour éviter plusieurs dérives :
- contexte implicite variable selon les scénarios,
- règles cachées dans les builders,
- paramètres réglementaires diffus dans plusieurs couches,
- tests non reproductibles,
- résultats impossibles à expliquer a posteriori.

---

## 9. Lien avec les autres documents

Ce document complète directement :
- `HORIZON_TEMPOREL_ET_REGLEMENTAIRE.md`
- `DECISIONS_CONCEPTION_OPTAPLANNER.md`
- `ScenarioContract.schema.json`
- `TESTING_STRATEGY_ENGINE.md`
- `TestPlanningContextFactory — Spécification`

Il doit rester cohérent avec :
- le contrat d’entrée,
- le modèle de résolution,
- et les factories de test.

---

## 10. Statut du document

- Document de référence
- Normatif sur le rôle du contexte
- À maintenir à chaque évolution de `PlanningContext`

---

**Fin du document**