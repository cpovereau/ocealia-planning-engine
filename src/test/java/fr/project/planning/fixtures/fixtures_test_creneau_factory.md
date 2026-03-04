# 🧩 TestCreneauFactory — Spécification

Ce document définit **le contrat de conception et d’utilisation** de la factory
`TestCreneauFactory`.

Il s’agit d’un **document normatif** : toute implémentation devra s’y conformer strictement.

---

## 🎯 Rôle de la factory

`TestCreneauFactory` est responsable de la création de **créneaux de test** :

- cohérents temporellement,
- complets fonctionnellement,
- explicites sur leur nature,
- et **valides par construction**.

Elle constitue **l’unique point d’entrée autorisé** pour créer des `Creneau` dans les tests.

---

## 🧠 Pourquoi cette factory est critique

Le `Creneau` est :

- l’unique `PlanningEntity` du moteur,
- le porteur de la variable de décision `ressourceAffectee`,
- le point d’application de la quasi-totalité des contraintes.

👉 Un créneau incohérent rend tout test :
- non fiable,
- non interprétable,
- potentiellement trompeur.

Cette factory est **structurante pour tous les niveaux de test V1 à V4**.

---

## 📦 Objets produits

- `Creneau`

Aucun autre objet ne doit être créé ou modifié par cette factory.

---

## 🧱 Responsabilités explicites

La factory **doit garantir** :

### 1️⃣ Cohérence temporelle

- `heureDebut != heureFin`
- et durée calculable et strictement positive
- si heureFin < heureDebut, le créneau traverse minuit
- aucune ambiguïté sur l’intervalle temporel (début/fin explicites)
- la nuit est mesurée par TimeBreakdownCalculator, jamais déduite par la factory

👉 Aucune création de créneau ne doit violer ces invariants.

---

### 2️⃣ Nature du créneau

Chaque créneau doit être explicitement typé :

- `IMPOSE`
- `GENERE`

👉 Le type n’est jamais déduit implicitement.

---

### 3️⃣ Qualifiants calendaires explicites

La factory doit rendre explicite :

- travail de nuit (`segmentNuit`),
  - segmentNuit peut rester comme indicateur d’entrée
  - mais ne constitue plus une source de vérité réglementaire
  
- jour férié (`isJourFerie`),
  - jour férié : ne doit pas être une source de vérité sur Creneau
  - si le champ existe encore dans Creneau, il doit rester neutre (ex: false) ou être traité comme indicatif non réglementaire
  - la vérité est fournie par RegulatoryParameters via la fixture correspondante
  
- repos hebdomadaire (`isReposHebdo`).

👉 Aucun qualifiant ne doit être implicite ou déduit par le test.

---

### 4️⃣ Neutralité décisionnelle

La factory **ne doit jamais** :

- affecter une ressource au créneau,
- fixer `ressourceAffectee`,
- simuler une décision.

La décision reste **exclusive au solveur**.

---

## 🚫 Interdictions formelles

`TestCreneauFactory` **ne doit jamais** :

- créer des séquences de créneaux,
- raisonner sur plusieurs jours,
- dépendre d’un `PlanningContext`,
- appliquer une règle métier ou légale,
- déduire des qualifiants à partir des heures.

Si un comportement dépend du contexte ou d’une règle,
il est **hors factory**.

---

## 🧪 Variantes attendues (conceptuelles)

La factory devra proposer des **variantes nommées**, par exemple :

- créneau jour standard
- créneau nuit standard
- créneau jour férié
- créneau sur repos hebdomadaire

Chaque variante :

- est auto‑documentée par son nom,
- n’introduit qu’une seule particularité,
- reste isolée (un créneau = un fait).

---

## 🔢 Niveaux de test autorisés

| Niveau | Autorisé |
|------|----------|
| V1 | ✅ |
| V2 | ✅ |
| V3 | ✅ |
| V4 | ✅ |
| V5 | ❌ |

---

## 🧠 Règle de lecture

> Un test qui utilise un créneau
> sans pouvoir expliquer **sa nature exacte**
> est un test invalide.

---

## 📌 Statut du document

- Document normatif
- Version : V1
- Toute évolution de la structure `Creneau`
  **doit mettre à jour ce document**

---

**Fin du document**

