# 🧠 Lecture pédagogique — Modèle OptaPlanner (version alignée)

Ce document explique **comment lire les stéréotypes OptaPlanner** appliqués au modèle
métier du moteur de planification.

Il sert de **pont pédagogique** entre :

* le diagramme conceptuel,
* l’UML OptaPlanner,
* et la logique réelle du solveur.

---

## 🟦 `<<PlanningSolution>>`

### Le monde complet du solveur

**Rôle**

* Contient tout ce que le solveur manipule pendant la résolution
* Porte le score global
* Sert de conteneur aux faits, décisions et résultats

**Contenu typique**

* liste des créneaux
* liste des ressources (réelles et virtuelles)
* paramètres réglementaires et stratégiques
* indicateurs dérivés (coûts, dettes)

📌 Lecture clé :

> *Tout ce qui n’est pas dans la PlanningSolution n’existe pas pour le solveur.*

---

## 🟩 `<<PlanningEntity>>`

### Là où OptaPlanner agit

Dans ce modèle, **une seule PlanningEntity existe** :

### ➤ Créneau

**Pourquoi le créneau ?**

* C’est le besoin à couvrir
* Il est indépendant des ressources
* Il existe avant toute décision

**Ce que fait OptaPlanner**

* Il choisit une valeur pour la variable de décision du créneau
* Il peut remplacer une affectation par une autre

📌 Lecture clé :

> *Si un objet n’est pas une PlanningEntity, OptaPlanner ne décide rien dessus.*

---

## 🟨 `<<PlanningVariable>>`

### La décision

### ➤ `ressourceAffectee`

**Nature**

* Unique variable de décision du modèle
* Valeur possible :

  * salarié réel
  * poste virtuel
  * état « à affecter »

**Ce que cela implique**

* Toute la complexité est portée par les règles
* La variable reste volontairement simple

📌 Lecture clé :

> *Une seule décision, évaluée par de nombreuses règles.*

---

## 🟪 `<<ProblemFact>>`

### Les faits immuables

Les ProblemFacts sont **lus par le solveur mais jamais modifiés**.

### Catégories de facts dans le modèle

#### 1. Ressources

* Salarié réel
* Poste virtuel

#### 2. Paramètres réglementaires

* règles légales
* règles conventionnelles
* stratégies (payé / récup)

#### 3. Indicateurs dérivés

* heures spécifiques (nuit, férié, repos hebdo)
* heures supplémentaires / complémentaires
* dette de repos compensateur
* coûts directs et indirects

📌 Lecture clé :

> *Les ProblemFacts décrivent le réel ou ses conséquences, jamais une décision.*

---

## 🟥 `<<ConstraintProvider>>`

### Le cerveau métier

**Rôle**

* Centralise toutes les règles
* Évalue les décisions
* Ne modifie jamais les données

**Ce qu’il utilise**

* les créneaux (entités)
* les ressources (facts)
* les paramètres
* les indicateurs dérivés

**Ce qu’il produit**

* un score global
* des arbitrages explicables

📌 Lecture clé :

> *Les contraintes jugent, elles ne décident rien.*

---

## ⚖️ Invariant fondamental

> Les règles n’ajoutent aucune contrainte au salarié réel.
> Elles encadrent le poste virtuel et évaluent les décisions.

---

## 🧠 Synthèse mentale

* Le salarié réel **subit** les règles
* Le poste virtuel **hérite** des règles
* Le créneau **porte la décision**
* Les indicateurs **rendent visibles les coûts**
* Le score **exprime les compromis**

> Le moteur ne cherche pas la perfection,
> mais la solution la plus explicable possible.
