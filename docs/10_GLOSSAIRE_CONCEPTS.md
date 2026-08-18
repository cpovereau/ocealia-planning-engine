# 📖 Glossaire des concepts — Moteur de planification

Ce document définit les **concepts fondamentaux utilisés dans le moteur de planification**.

Son objectif est de garantir que tous les développeurs utilisent les **mêmes termes avec le même sens**.

Le glossaire ne décrit pas l’architecture complète du moteur. Il fournit uniquement des **définitions courtes et normatives** des concepts structurants.

---

# 1. Créneau

## Définition

Un **créneau** est l’unité élémentaire à affecter.

Il représente un intervalle temporel associé à une activité.

## Caractéristiques principales

Un créneau possède notamment :

- un identifiant
- une date
- une heure de début
- une heure de fin
- une durée (stockée — source de vérité pour les agrégats ; voir `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`)
- une activité

## Variable de décision

La variable de décision du solveur est :

```
ressourceAffectee
```

Le moteur optimise **quelle ressource couvre quel créneau**.

---

# 1 bis. Travail (au sens du moteur)

## Deux sens du terme

Le mot "travail" est utilisé dans deux sens distincts dans la documentation :

| Sens | Signification |
| ---- | ------------- |
| **Général** | Un créneau à affecter, quelle que soit son activité |
| **Moteur** | Une activité comptant dans la charge de travail (`compteDansCharge = true`) |

La distinction est importante : tous les créneaux ne sont pas du "travail" au sens moteur.

## Définition canonique

> **Un créneau est considéré comme travaillé (au sens moteur) si et seulement si
> son activité a `compteDansCharge = true` dans le référentiel d’activité.**

Cette définition est un **invariant d’architecture** : toute contrainte, métrique ou règle
s’y conforme sans exception.

## Référence

La définition complète, les mappings `Nature → compteDansCharge` et la règle de cohérence
transversale sont dans :

> `20_DECISIONS_CONCEPTION_OPTAPLANNER.md §5`

---

# 2. Ressource

## Définition

Une **ressource** est une entité capable de couvrir un créneau.

## Types de ressources

Deux catégories existent :

### Salarié réel

Personne existante dans l'organisation.

### Poste virtuel

Représente un besoin non couvert ou une capacité de recrutement.

## Ressource spéciale

Une pseudo‑ressource peut être utilisée pour représenter l’absence d’affectation :

```
A_AFFECTER
```

Cette approche évite l’utilisation de valeurs `null`.

---

# 3. PlanningProblem

## Définition

Le **PlanningProblem** représente le problème de planification transmis au solveur.

Il constitue le monde solveur exploité par OptaPlanner.

## Contenu typique

Un PlanningProblem contient notamment :

- les créneaux
- les ressources
- le PlanningContext
- les paramètres réglementaires
- le référentiel d’activités

---

# 4. PlanningSolution

## Définition

La **PlanningSolution** est la solution produite par le solveur. C'est un concept OptaPlanner —
l'annotation `@PlanningSolution` — et non une classe du moteur.

Dans ce projet, elle est portée par **`PlanningProblem`**, et par elle seule : le solveur reçoit une
copie non résolue de cette classe et en rend une résolue. Il n'existe pas de classe distincte pour
le résultat, et en chercher une est une erreur fréquente à la lecture des diagrammes.

Elle contient :

- les créneaux avec leur ressource affectée
- le score calculé par OptaPlanner
- les faits de problème, dont certains que `PlanningProblem` dérive lui-même

## Statut

Cette structure reste **interne au moteur**.

Elle n’est pas exposée directement par l’API.

---

# 5. PlanningContext

## Définition

Le **PlanningContext** est un objet de contexte immuable décrivant le cadre d’évaluation du scénario.

Il fournit les paramètres nécessaires pour juger les affectations produites par le solveur.

## Informations typiques

Le contexte peut notamment contenir :

- l’horizon temporel
- la stratégie de scoring
- les seuils réglementaires
- les hypothèses d’historique

## Principe

Le PlanningContext **ne contient aucune décision du solveur**.

Il décrit uniquement le cadre d’évaluation.

---

# 6. DatasetBuilder

## Définition

Le **DatasetBuilder** transforme le contrat d’entrée d’un scénario en un monde solveur exploitable.

## Responsabilités

Il est responsable de :

- créer les créneaux
- créer les ressources
- injecter le PlanningContext
- injecter les paramètres réglementaires

## Principe fondamental

Le DatasetBuilder prépare le problème.

Le solveur prend les décisions.

---

# 7. Contrainte

## Définition

Une **contrainte** est une règle évaluée par le solveur pour déterminer la qualité d’une solution.

Deux types de contraintes existent.

### Contraintes HARD

Règles impératives qui ne peuvent pas être violées.

Une solution violant une contrainte HARD est considérée comme invalide.

### Contraintes SOFT

Préférences permettant d’arbitrer entre plusieurs solutions valides.

Les contraintes SOFT contribuent au score mais ne rendent pas une solution invalide.

---

# 8. Score

## Définition

Le **score** est la valeur utilisée par le solveur pour comparer différentes solutions.

Dans le moteur, il est représenté par :

```
HardSoftScore
```

## Structure

Le score comporte deux dimensions :

- hard : violations interdites
- soft : qualité de la solution

## Principe

Le score sert uniquement à **arbitrer entre plusieurs solutions**.

Il ne constitue pas un indicateur métier directement interprétable.

---

# 9. ScoreWeights

## Définition

**ScoreWeights** est le composant qui traduit les pénalités métier en pondérations techniques utilisées par le score.

## Rôle

Il permet notamment :

- de hiérarchiser les contraintes SOFT
- d’adapter l’arbitrage du solveur
- de varier le comportement selon la stratégie de scoring

---

# 10. WorkMetrics

## Définition

Les **WorkMetrics** sont des indicateurs calculés après la résolution du planning.

Elles décrivent les conséquences du planning sur les ressources.

## Exemples

- heures travaillées
- heures de nuit
- dimanches travaillés
- séquences de travail

## Principe

Les WorkMetrics :

- ne modifient jamais le planning
- ne participent pas aux décisions du solveur
- servent à l’analyse et à l’explicabilité

---

# 11. ScenarioResponseDTO

## Définition

Le **ScenarioResponseDTO** est la structure de réponse exposée par l’API du moteur.

Il constitue le contrat de sortie du moteur.

## Contenu

La réponse contient généralement :

- le résultat du solveur
- le planning résolu
- les WorkMetrics
- les diagnostics

Cette structure garantit une **interface stable entre le moteur et les systèmes appelants**.

---

# 12. Diagnostic

## Définition

Un **diagnostic** est une information technique produite par le moteur afin d’aider à comprendre l’exécution d’un scénario.

## Exemples

Les diagnostics peuvent signaler :

- des créneaux ignorés
- des activités inconnues
- des incohérences de données
- des situations impossibles à résoudre

Les diagnostics ne participent ni au score ni aux décisions du solveur.

---

# 13. Indisponibilité

## Définition

Une **indisponibilité** est un intervalle temporel pendant lequel une ressource ne peut pas être affectée à un créneau.

Elle représente une contrainte externe au planning (absence, congé, formation, etc.).

## Nature

- Fait d’entrée du problème (comme un créneau ou une ressource)
- Immuable pendant la résolution
- Ne constitue pas une décision du solveur

## Caractéristiques principales

Une indisponibilité possède :

- une ressource concernée
- une date de début
- une date de fin

## Rôle dans le moteur

Une indisponibilité interdit l’affectation d’un créneau à une ressource si :

- le créneau intersecte la période d’indisponibilité

Cette règle est portée par une contrainte HARD.

## Propriétés clés

- Une indisponibilité ne génère jamais de créneau
- Elle ne participe pas au score directement
- Elle agit uniquement comme filtre de faisabilité

## Différence avec un créneau

| Créneau | Indisponibilité |
|--------|----------------|
| Besoin à couvrir | Impossibilité de couvrir |
| Peut être affecté | Ne peut jamais être affecté |
| Porte une activité | N’en porte pas |

## Exemple

Un salarié en congé du 10/04 au 12/04 :

→ tous les créneaux sur cette période lui sont interdits

## Principe

L’indisponibilité réduit l’espace des solutions possibles, mais ne crée aucune décision.

---

# 14. Principe général du moteur

Le moteur de planification repose sur une séparation stricte des responsabilités :

- le DatasetBuilder construit le monde solveur
- le solveur optimise les affectations
- les WorkMetrics analysent la solution
- l’API expose un résultat stable

Cette séparation garantit :

- une architecture lisible
- une explicabilité des résultats
- une évolution du moteur sans rupture de contrat API.

---

# 15. Scoring (au sens du moteur)

## Définition

Le **scoring** désigne le mécanisme global par lequel le moteur évalue et compare les solutions de planning.

Il repose sur :

- les contraintes (HARD / SOFT),
- les pénalités métier (`Penalites`),
- les pondérations techniques (`ScoreWeights`),
- et la stratégie de scoring (`StrategieScoring`).

## Rôle

Le scoring permet de :

- comparer plusieurs solutions possibles,
- arbitrer entre des compromis,
- orienter le comportement du solveur.

Il ne sert pas à :

- produire un indicateur métier,
- mesurer la performance RH,
- calculer une conformité réglementaire.

## Lien avec le score

Le **score** est la valeur numérique produite par le scoring.

Le scoring est donc le processus,
le score en est le résultat.

## Lien avec la stratégie de scoring

La `StrategieScoring` permet de faire varier le comportement du scoring selon l’objectif :

- exploitation (planning opérationnel),
- analyse RH,
- audit.

Elle modifie la pondération des contraintes,
mais ne change pas le problème à résoudre.

## Principe

> Le moteur ne cherche pas une solution parfaite,
> mais la solution la moins pénalisée selon une stratégie donnée.
