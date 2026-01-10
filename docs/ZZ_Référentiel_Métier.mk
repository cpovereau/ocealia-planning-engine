# 📌 Référentiel métier — Moteur de planification (version clarifiée)

Ce document a pour objectif **d’expliquer clairement où en est le moteur aujourd’hui**,
ce qu’il fait **déjà**, ce qu’il **ne fait pas**, et **comment lire les fichiers existants**.

Il ne décrit pas une vision cible abstraite :
➡️ il décrit **l’état réel du moteur tel qu’il est implémenté**.

---

## 🎯 Finalité du moteur

Le moteur de planification a pour rôle de :

* proposer une **affectation de créneaux à des ressources**,
* sous **contraintes physiques, légales et métier**,
* en produisant une **solution explicable**,
* même lorsqu’aucune solution parfaite n’existe.

👉 Le moteur **n’automatise pas la décision métier**.
👉 Il **met en évidence les compromis**, les tensions et les manques.

---

## 🧱 Principe structurant — Séparation des couches

Le moteur repose sur une séparation stricte des responsabilités.

| Couche            | Rôle                                 |
| ----------------- | ------------------------------------ |
| Contraintes       | Interdire (HARD) ou pénaliser (SOFT) |
| SeuilsDeTolerance | Bornes métier impératives            |
| Penalites         | Intensité des contraintes SOFT       |
| ScoreWeights      | Pondération technique interne        |
| WorkMetrics       | Constats post‑résolution             |
| Analyse métier    | Interprétation hors moteur           |

👉 **Aucune couche ne consomme ce qui relève d’une autre.**

---

## 🧩 Ce que le moteur manipule réellement

### 1️⃣ Le créneau — l’unité de décision

Le **créneau** représente un **besoin de travail à couvrir**.

* il existe indépendamment des ressources,
* il peut être imposé ou généré,
* il porte **l’unique variable de décision** : `ressourceAffectee`.

👉 OptaPlanner **ne décide que sur les créneaux**, jamais sur les salariés.

---

### 2️⃣ Les ressources — faits immuables

Le moteur manipule deux types de ressources :

* **Salarié réel** : personne existante, jamais modifiée par le moteur
* **Poste virtuel** : capacité manquante ou hypothétique

👉 Le salarié réel est un **fait d’entrée**.
👉 Le poste virtuel est un **outil de révélation du manque**.

---

### 3️⃣ Les contraintes — ce que le moteur juge

Les contraintes évaluent les affectations.
Elles ne modifient **jamais** les données.

Deux catégories structurantes :

* **HARD** : règles impératives → solution interdite si violées
* **SOFT** : règles d’optimisation → arbitrage entre solutions valides

Exemples actuellement implémentés :

* nuits consécutives maximales (HARD)
* repos obligatoire après nuits (HARD)
* repos hebdomadaire glissant (HARD / SOFT selon variante)
* dimanches travaillés (SOFT fort)

---

### 4️⃣ Seuils et pénalités — paramètres métier

* `SeuilsDeTolerance` définit **les bornes métier**
* `Penalites` définit **l’intensité des violations SOFT**

👉 Une contrainte HARD **ne consomme jamais de pénalité**.
👉 Une contrainte SOFT **ne consomme jamais de seuil HARD**.

---

### 5️⃣ Score et ScoreWeights — arbitrage technique

Le **score** sert uniquement à comparer des solutions valides.

* il ne représente ni la légalité,
* ni la conformité RH,
* ni un jugement individuel.

`ScoreWeights` est une couche **strictement technique**,
chargée de traduire les pénalités métier vers OptaPlanner.

---

### 6️⃣ WorkMetrics — ce que la solution produit

Les **WorkMetrics** sont :

* calculées **après résolution**,
* strictement descriptives,
* indépendantes des contraintes.

Exemples :

* heures travaillées
* heures de nuit
* dimanches travaillés
* travail sur repos hebdomadaire

👉 Les métriques **n’interdisent jamais une solution**.
👉 Elles servent à l’**explicabilité** et à l’analyse aval.

---

## 🚫 Ce que le moteur ne fait volontairement pas

Le moteur ne :

* calcule pas la paie,
* n’applique pas exhaustivement le droit du travail,
* ne statue pas sur la conformité réglementaire,
* n’interprète pas la surcharge salarié.

Ces éléments relèvent **exclusivement de l’analyse métier aval**.

---

## 🧾 Surcharge salarié — positionnement clair

La surcharge salarié :

* n’est pas une entité du moteur,
* n’est pas une variable de décision,
* n’est pas un fait consommé par le solveur.

Elle est construite **après coup**, à partir :

* des WorkMetrics,
* des règles combinatoires violées,
* des seuils définis dans le contexte.

👉 Le moteur **signale**.
👉 Le métier **interprète**.

---

## 📍 Où en est le moteur aujourd’hui

À ce stade, le moteur :

* sait interdire l’impossible (HARD),
* sait arbitrer entre des solutions imparfaites (SOFT),
* rend visibles les tensions et manques,
* reste explicable et extensible.

👉 Les fondations sont stabilisées.
👉 L’industrialisation peut démarrer sans dette conceptuelle.

Socle conceptuel V1 — gelé

Les contrats WebDev ↔ moteur sont définis dans des documents dédiés,
alignés sur le référentiel métier V1, et susceptibles d’évolution contrôlée.

---

## 🧠 Principe directeur

> **Le moteur juge. Il ne calcule pas.**

Il ne remplace pas le métier.
Il lui donne les moyens de décider en connaissance de cause.
