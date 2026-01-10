# 🧪 TESTING_STRATEGY_ENGINE.md

Ce document définit la **stratégie de tests officielle du moteur de planification**.

Il constitue :
- la **référence normative** pour tout test existant ou futur,
- un **garde-fou contre les dérives de conception**,
- un **outil de transmission** pour tout contributeur (présent ou futur).

Aucun test ne doit être écrit, modifié ou interprété sans se conformer explicitement à ce document.

---

## 1️⃣ Objectifs des tests

### 1.1 Pourquoi on teste

Les tests ont pour objectifs de :

- verrouiller les **capacités réelles du moteur** (et non des implémentations accidentelles),
- garantir la **stabilité conceptuelle** du modèle dans le temps,
- détecter toute **régression fonctionnelle ou métier**,
- documenter ce que le moteur **sait faire**, **ne sait pas faire**, et **refuse de faire**.

Les tests sont une **preuve d’aptitude**, pas une démonstration de conformité réglementaire.

---

### 1.2 Ce que les tests garantissent

Selon leur niveau, les tests garantissent :

- la validité d’une **logique isolée** (V1),
- la cohérence des **indicateurs dérivés** produits par une solution (V2),
- la pertinence des **arbitrages de scoring** (V3),
- l’existence d’une **solution faisable** (V4),
- la robustesse de l’intégration bout-en-bout (V5).

Un test qui passe garantit **uniquement ce qui relève de son niveau**.

---

### 1.3 Ce que les tests ne garantissent pas

Les tests ne garantissent jamais :

- la conformité juridique exhaustive,
- la validité RH ou contractuelle d’un planning,
- la qualité métier finale d’une solution,
- l’unicité ou la reproductibilité exacte d’un résultat,
- l’optimalité absolue d’un score.

Toute interprétation métier est **hors du périmètre des tests du moteur**.

---

### 1.4 Ce que les tests n’ont pas le droit de faire

Il est strictement interdit qu’un test :

- contourne le modèle métier pour « faire passer » un cas,
- invente un état impossible du domaine,
- modifie un `ProblemFact` pour simuler une décision,
- mélange plusieurs niveaux de test dans un même scénario,
- valide implicitement un comportement non documenté.

Un test qui triche est **pire qu’une absence de test**.

---

## 2️⃣ Typologie des tests (V1 → V5)

Chaque niveau de test répond à une **question différente**.
Ils ne sont ni substituables, ni cumulables.

### 2.1 Tableau contractuel des niveaux

| Niveau | Nom | Question à laquelle il répond | Autorisé | Interdit |
|------|-----|-------------------------------|----------|----------|
| V1 | Test unitaire pur | Le calcul est-il correct ? | POJO, utilitaires | PlanningSolution, Solver |
| V2 | Test métier post-résolution | Les constats sont-ils cohérents ? | PlanningSolution minimale | Solver |
| V3 | Test de scoring | Les arbitrages sont-ils cohérents ? | Solver + score | API |
| V4 | Test de résolution | Une solution faisable existe-t-elle ? | Solver complet | WebDev |
| V5 | Test d’intégration | Le flux bout-en-bout tient-il ? | API | Mock métier |

---

### 2.2 Frontières strictes

- Un test V2 **n’évalue jamais** la performance du solveur.
- Un test V3 **ne valide jamais** une règle légale isolée.
- Un test V4 **ne vérifie jamais** des métriques fines.
- Un test V5 **ne teste jamais** une règle métier unitaire.

Si un test semble devoir franchir une frontière, il est **mal positionné**.

---

### 2.3 Exemples de mauvais positionnement

- Vérifier un score dans un test V2 → erreur de niveau
- Vérifier des WorkMetrics dans un test V4 → erreur de responsabilité
- Créer un salarié fictif incomplet pour « simplifier » un V3 → violation du cadre

---

## 3️⃣ Environnement de test canonique

### 3.1 Version et socle technique

- Java : version du projet (figée)
- OptaPlanner : version du moteur utilisée en production
- Aucune dépendance métier externe implicite

---

### 3.2 Hypothèses autorisées

Les tests peuvent supposer :

- un horizon temporel explicite et borné,
- des données métier **cohérentes mais minimales**,
- l’absence d’historique si explicitement déclaré,
- des paramètres réglementaires neutres par défaut.

---

### 3.3 Minimal valide

Un environnement de test est considéré comme **minimal valide** s’il contient :

- au moins un créneau cohérent,
- au moins une ressource valide,
- un contexte explicite,
- des paramètres réglementaires complets.

---

### 3.4 Invalide par construction

Est invalide par construction tout test qui :

- instancie un objet métier incomplet,
- simule une décision via un `ProblemFact`,
- introduit un `null` décisionnel,
- force un état que le moteur ne peut produire.

---

## 4️⃣ Factories de test — Socle commun

### 4.1 Package de référence

Tous les objets de test doivent être créés via le package :

```
fr.project.planning.testfixtures
```

Aucun `new()` métier n’est autorisé hors de ce package.

---

### 4.2 Factories officielles

Les factories de référence incluent notamment :

- `TestPlanningContextFactory`
- `TestRessourceFactory`
- `TestReferentielMetierFactory`
- `TestCreneauFactory`
- `TestPlanningProblemFactory`

Cette liste est **extensible mais contrôlée**.

---

### 4.3 Responsabilité d’une factory

Une factory :

- crée des objets **valides par construction**,
- documente ses hypothèses,
- ne contient **aucune logique métier**,
- ne dépend d’aucun test spécifique.

Si une factory doit « réfléchir », elle est mal placée.

---

## 5️⃣ Règles d’or (anti-dérives)

### 5.1 Interdits absolus

- ❌ Inventer un constructeur métier
- ❌ Utiliser `new()` hors factory
- ❌ Approximations de package
- ❌ Mélange de niveaux de test
- ❌ Tests qui ne citent pas leur niveau

---

### 5.2 Bonnes pratiques obligatoires

- Chaque test mentionne explicitement son niveau (V1–V5)
- Chaque test documente son **objectif réel**
- Chaque échec doit être **interprétable sans debugger**

---

### 5.3 Signaux d’alerte

Un test est suspect si :

- il nécessite un commentaire explicatif long,
- il dépend d’un ordre d’exécution,
- il « casse souvent » sans modification métier,
- il ne sait pas expliquer ce qu’il valide.

---

## 6️⃣ Lecture recommandée

### Pour un nouveau développeur

1. Modèle métier
2. Décisions de conception
3. STRATEGIE_SCORING
4. Ce document
5. Tests V1 puis V2

### Pour un contributeur moteur

- Lire ce document **avant toute écriture de test**
- Refuser tout test non positionnable clairement

---

## 📌 Statut du document

- Version : **V1**
- Document normatif
- Toute dérogation doit être **explicitement justifiée**
- Toute évolution du moteur doit être accompagnée d’une mise à jour de ce document

---

**Fin du document**

