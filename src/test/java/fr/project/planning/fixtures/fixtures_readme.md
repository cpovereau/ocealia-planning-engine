# 📦 Test Fixtures — Guide d’utilisation

Ce document définit **le rôle, les responsabilités et les règles d’utilisation** des factories de test
contenues dans le package :

```
fr.project.planning.fixtures
```

Il complète et applique strictement :
- `TESTING_STRATEGY_ENGINE.md`

Toute utilisation des fixtures **en dehors de ce cadre est interdite**.

---

## 🎯 Rôle du package `fixtures`

Le package `fixtures` fournit **l’unique point d’entrée autorisé** pour créer :

- des objets métier utilisés dans les tests,
- des contextes de résolution cohérents,
- des mondes de test *valides par construction*.

👉 Les fixtures servent à **éviter l’invention locale**,
👉 à **uniformiser les hypothèses**,
👉 et à **garantir la comparabilité des tests**.

---

## 🚫 Principe fondamental

> **Aucun test ne doit instancier directement un objet métier du moteur.**

Toute création d’objet métier doit passer par **une factory officielle** de ce package.

Un test qui fait un `new()` direct est **invalidé par construction**.

---

## 🧩 Liste des factories officielles

### 1️⃣ `TestRessourceFactory`

**Rôle**  
Créer des ressources **valides et complètes**.

**Objets concernés**
- `SalarieReel`
- `PosteVirtuel`

**Utilisation typique**
- V1 (tests unitaires isolés)
- V2 (tests métier post-résolution)
- V3 (tests de scoring)

**Garanties**
- aucune ressource incomplète
- aucun état métier impossible

**Interdictions**
- logique métier
- adaptation contextuelle

---

### 2️⃣ `TestPlanningContextFactory`

**Rôle**  
Créer des contextes de résolution **explicites et traçables**.

**Responsabilités**
- horizon temporel
- type de résolution
- hypothèses d’historique
- stratégie de scoring

**Utilisation typique**
- V2 → V4

**Interdictions**
- activer/désactiver des contraintes
- calculer des métriques

---

### 3️⃣ `TestRegulatoryParametersFactory`

**Rôle**  
Fournir un référentiel réglementaire **cohérent mais neutre**.

**Responsabilités**
- plages de nuit
- jours fériés
- seuils légaux
- stratégie payé / récup

**Utilisation typique**
- V2 → V4

**Garanties**
- aucun mock partiel
- aucun paramètre manquant

**Interdictions**
- valeurs RH réalistes
- conventions collectives spécifiques

---

### 4️⃣ `TestCreneauFactory`

**Rôle**  
Créer des créneaux **valides par construction**.

**Responsabilités**
- cohérence date / heure / durée
- type (IMPOSÉ / GÉNÉRÉ)
- qualifiants calendaires (nuit, férié, repos hebdo)

**Variantes attendues**
- créneau jour standard
- créneau nuit
- créneau jour férié
- créneau repos hebdomadaire

**Utilisation typique**
- V1 → V4

**Interdictions**
- affectation d’une ressource
- création de séquences

---

### 5️⃣ `TestPlanningSolutionFactory`

**Rôle**  
Assembler un **monde OptaPlanner minimal valide**.

**Responsabilités**
- liste de créneaux
- liste de ressources
- paramètres réglementaires
- WorkMetrics (si requis par le niveau de test)

**Utilisation typique**
- V2 → V4

**Interdictions**
- lancer le solveur
- modifier des décisions
- calculer un score

---

### 6️⃣ `TestWorkMetricsFactory`

**Rôle**  
Créer des **WorkMetrics post-résolution cohérentes**.

**Utilisation typique**
- V2 uniquement

**Garanties**
- métriques descriptives
- aucun impact décisionnel

**Interdictions**
- utilisation en V3/V4
- déclenchement de contraintes

---

## Typologie des factories de test

- TestPlanningContextFactory
  → fournit uniquement des PlanningContext normés

- TestRessourceFactory
  → fournit uniquement des ressources planifiables (salariés, postes)

- TestReferentielFactory
  → fournit des référentiels métier minimaux mais valides

❗ Une factory de test ne doit jamais contourner le modèle métier.
Les objets fournis doivent être valides au sens du domaine.

---

## 🔢 Correspondance factories ↔ niveaux de test

| Factory | V1 | V2 | V3 | V4 | V5 |
|------|----|----|----|----|----|
| TestRessourceFactory | ✅ | ✅ | ✅ | ✅ | ❌ |
| TestPlanningContextFactory | ❌ | ✅ | ✅ | ✅ | ❌ |
| TestRegulatoryParametersFactory | ❌ | ✅ | ✅ | ✅ | ❌ |
| TestCreneauFactory | ✅ | ✅ | ✅ | ✅ | ❌ |
| TestPlanningSolutionFactory | ❌ | ✅ | ✅ | ✅ | ❌ |
| TestWorkMetricsFactory | ❌ | ✅ | ❌ | ❌ | ❌ |

---

## 🧠 Règle de lecture essentielle

> **Une factory crée.**  
> **Un test assemble.**  
> **Une contrainte juge.**

Si une factory commence à décider, elle est mal placée.

---

## ⚠️ Signaux de dérive

Une mauvaise utilisation des fixtures est suspecte si :

- une factory dépend d’un test précis,
- un test modifie un objet créé par une factory,
- une valeur est « bricolée » après création,
- une factory devient plus complexe qu’un test.

---

## 📌 Statut du document

- Document normatif
- Obligatoire pour toute écriture ou relecture de test
- Toute évolution des fixtures **doit mettre à jour ce fichier**

---

**Fin du document**