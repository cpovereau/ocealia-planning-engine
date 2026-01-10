# 🧩 TestWorkMetricsFactory — Spécification

Ce document définit **le contrat de conception et d’utilisation** de la factory
`TestWorkMetricsFactory`.

Il s’agit d’un **document normatif** : toute implémentation devra s’y conformer strictement.

---

## 🎯 Rôle de la factory

`TestWorkMetricsFactory` est responsable de la création de **WorkMetrics de test**
utilisées comme **constats post-résolution**.

Elle permet de :

- tester la cohérence des indicateurs dérivés,
- expliciter les conséquences d’une solution,
- éviter toute création ad-hoc ou partielle de métriques dans les tests.

👉 Cette factory existe **uniquement** pour sécuriser les tests de niveau V2.

---

## 🧠 Rappel fondamental sur WorkMetrics

Les `WorkMetrics` sont :

- des **ProblemFacts**,
- des **conséquences** des décisions,
- jamais des décisions elles-mêmes.

Elles :
- n’influencent pas la faisabilité,
- ne déclenchent aucune contrainte HARD,
- ne modifient aucune affectation.

👉 Toute tentative d’utiliser des WorkMetrics pour « orienter » une solution est invalide.

---

## 📦 Objets produits

- `WorkMetrics`

Aucun autre objet ne doit être créé ou modifié par cette factory.

---

## 🧱 Responsabilités explicites

La factory doit permettre de créer des WorkMetrics :

### 1️⃣ Complètes

Toutes les métriques requises par le modèle doivent être présentes.

👉 Aucun champ optionnel laissé à `null`.

---

### 2️⃣ Cohérentes

Les valeurs fournies doivent être :

- compatibles entre elles,
- positives ou nulles selon leur nature,
- bornées par l’horizon de test.

👉 Aucune incohérence volontaire pour « forcer » un test.

---

### 3️⃣ Explicitement documentées

Chaque méthode de la factory doit :

- indiquer **ce qu’elle représente**,
- indiquer **ce qu’elle ne représente pas**,
- préciser les hypothèses retenues.

---

## 🚫 Interdictions formelles

`TestWorkMetricsFactory` **ne doit jamais** :

- calculer des métriques à partir de créneaux,
- dépendre d’un `PlanningContext`,
- être utilisée avant résolution,
- être utilisée dans un test V3, V4 ou V5,
- déclencher ou simuler une contrainte.

Si un calcul est nécessaire, il est **hors factory** (et hors test moteur).

---

## 🧪 Variantes attendues (conceptuelles)

La factory devra proposer des variantes nommées, par exemple :

- `neutres()` : aucune tension, aucune dette
- `avecTravailDeNuit(...)`
- `avecTravailJourFerie(...)`
- `avecDetteReposCompensateur(...)`

Chaque variante :

- correspond à une situation lisible,
- introduit une seule source de tension,
- n’encode aucune règle de calcul.

---

## 🔢 Niveaux de test autorisés

| Niveau | Autorisé |
|------|----------|
| V1 | ❌ |
| V2 | ✅ |
| V3 | ❌ |
| V4 | ❌ |
| V5 | ❌ |

---

## 🧠 Règle de lecture

> Un test qui valide des WorkMetrics
> doit pouvoir dire **d’où elles viennent**
> et **ce qu’elles signifient**, sans invoquer une règle métier.

---

## 📌 Statut du document

- Document normatif
- Version : V1
- Toute évolution du modèle `WorkMetrics`
  **doit mettre à jour ce document**

---

**Fin du document**