# 🧩 TestRegulatoryParametersFactory — Spécification

Ce document définit **le contrat de conception et d’utilisation** de la factory
`TestRegulatoryParametersFactory`.

Il s’agit d’un **document normatif** : toute implémentation devra s’y conformer strictement.

---

## 🎯 Rôle de la factory

`TestRegulatoryParametersFactory` est responsable de la création des **paramètres réglementaires et conventionnels**
consommés par le moteur dans les tests.

Elle produit un référentiel :

- **complet** (aucun champ “oublié”),
- **cohérent** (pas de contradictions internes),
- **neutre** (pas de réalisme RH implicite),
- **stable** (réutilisable entre tests).

👉 Cette factory vise à supprimer définitivement les « mocks fantômes » et les référentiels partiels.

---

## 📦 Objets produits

- `RegulatoryParameters`

Aucun autre objet ne doit être créé ou modifié par cette factory.

---

## 🧱 Responsabilités explicites

La factory doit rendre explicite, au minimum :

### 1️⃣ Plages de nuit

- définition stable des plages horaires qualifiées de nuit
- règles de chevauchement si nécessaire (mais **sans logique métier**)

👉 La factory **déclare** les plages, elle ne déduit pas des nuits.

---

### 2️⃣ Jours fériés

- une liste de jours fériés **déclarée**
- bornée au contexte d’un test

👉 Aucun calcul calendrier implicite.

---

### 3️⃣ Stratégie payé / récup

- un mode explicite : `PAYE` / `RECUP` / `MIXTE` (ou équivalent)

👉 Le moteur arbitre selon cette stratégie ; il ne la déduit jamais.

---

### 4️⃣ Seuils légaux

La factory doit fournir des seuils **au moins** pour :

- repos quotidien minimal
- repos hebdomadaire minimal
- maximum nuits consécutives

Ces seuils sont :
- cohérents entre eux,
- exprimés dans une unité stable (minutes/heures/jours).

👉 La factory ne “choisit pas” une valeur réaliste, elle fournit une base neutre et documentée.

---

## 🚫 Interdictions formelles

`TestRegulatoryParametersFactory` **ne doit jamais** :

- introduire une convention collective réelle,
- refléter des règles RH implicites,
- dépendre d’un `PlanningContext`,
- appliquer des calculs complexes (pauses, modulation annuelle, etc.),
- contenir une logique d’interprétation métier.

Si un besoin exige une règle complexe, il est **hors factory** (et hors V1/V2).

---

## 🧪 Variantes attendues (conceptuelles)

La factory devra proposer des variantes nommées :

- `neutre()` : socle par défaut
- `avecJoursFeries(...)` : injecte explicitement une liste de jours
- `strategiePaye()` / `strategieRecup()` / `strategieMixte()`
- `seuilsStricts()` / `seuilsSouples()` (si besoin)

Chaque variante :

- est documentée,
- n’introduit qu’une différence maîtrisée,
- évite toute magie implicite.

---

## 🔢 Niveaux de test autorisés

| Niveau | Autorisé |
|--------|----------|
| V1     | ❌      |
| V2     | ✅      |
| V3     | ✅      |
| V4     | ✅      |
| V5     | ❌      |

---

## 🧠 Règle de lecture

> Un test qui dépend de règles réglementaires
> sans pouvoir citer **la variante de factory utilisée**
> est un test invalide.

---

## 📌 Statut du document

- Document normatif
- Version : V1
- Toute évolution de `RegulatoryParameters`
  **doit mettre à jour ce document**

---

**Fin du document**

