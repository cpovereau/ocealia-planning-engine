# 🧩 TestPlanningContextFactory — Spécification

Ce document définit **le contrat de conception et d’utilisation** de la factory
`TestPlanningContextFactory`.

Il s’agit d’un **document normatif** : toute implémentation devra s’y conformer strictement.

---

## 🎯 Rôle de la factory

`TestPlanningContextFactory` est responsable de la création de **PlanningContext de test** :

- explicites,
- cohérents,
- traçables,
- et **valides par construction**.

Elle constitue **l’unique point d’entrée autorisé** pour fournir un contexte de résolution
au moteur dans les tests.

📌 Le constructeur socle test de PlanningContext est responsable de :
- la construction de HorizonTemporel
- l’initialisation des valeurs neutres
Les factories de test n’ont PAS à créer ces objets.

---

## 🧠 Pourquoi cette factory est critique

Le `PlanningContext` :

- conditionne l’activation et la pondération des contraintes,
- fixe l’horizon temporel,
- porte les hypothèses sur l’historique,
- influence directement le scoring.

👉 Un contexte implicite ou bricolé invalide **toute lecture de test**.

Cette factory est donc **structurante pour les tests V2, V3 et V4**.

---

## 📦 Objets produits

- `PlanningContext`

Aucun autre objet ne doit être créé ou modifié par cette factory.

---

## 🧱 Responsabilités explicites

La factory **doit définir explicitement** :

### 1️⃣ Horizon temporel

- date de début
- date de fin

L’horizon est toujours :
- borné,
- explicite,
- cohérent avec le type de résolution.

---

### 2️⃣ Type de résolution

Le contexte doit porter un type clair, par exemple :

- `PLANNING_GLOBAL`
- `CYCLE`
- `REMPLACEMENT`
- `PROJECTION`

👉 Le moteur **ne déduit jamais** ce type.

---

### 3️⃣ Hypothèses sur l’historique

La factory doit rendre explicite si :

- l’historique est neutre,
- des compteurs initiaux existent,
- ou si l’état passé est volontairement ignoré.

👉 Aucune reconstitution implicite du passé n’est autorisée.

---

### 4️⃣ Stratégie de scoring

Le contexte doit préciser :

- la stratégie d’arbitrage (payé / récup / mixte),
- les priorités globales si nécessaires.

👉 Le contexte **n’active ni ne désactive** des contraintes.
Il ne fait que fournir un cadre.

---

## 🚫 Interdictions formelles

`TestPlanningContextFactory` **ne doit jamais** :

- activer ou désactiver des contraintes,
- calculer ou modifier des `WorkMetrics`,
- dépendre d’un test particulier,
- contenir une logique métier décisionnelle,
- interpréter une demande utilisateur.

Si un comportement dépend du métier, il est **hors factory**.

---

## 🧪 Variantes attendues (conceptuelles)

La factory devra proposer des **variantes nommées**, par exemple :

- contexte neutre (par défaut)
- contexte de remplacement
- contexte de cycle

Chaque variante :
- est documentée,
- explicite ses hypothèses,
- ne modifie que ce qui est nécessaire.

---

## 🔢 Niveaux de test autorisés

| Niveau | Autorisé |
|------|----------|
| V1 | ❌ |
| V2 | ✅ |
| V3 | ✅ |
| V4 | ✅ |
| V5 | ❌ |

---

## 🧠 Règle de lecture

> Un test qui utilise un `PlanningContext`
> sans pouvoir dire **d’où il vient et ce qu’il suppose**
> est un test invalide.

---

## 📌 Statut du document

- Document normatif
- Version : V1
- Toute évolution du `PlanningContext` **doit mettre à jour ce document**

---

**Fin du document**

