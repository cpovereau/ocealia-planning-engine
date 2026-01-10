# 🧩 TestPlanningSolutionFactory — Spécification

Ce document définit **le contrat de conception et d’utilisation** de la factory
`TestPlanningSolutionFactory`.

Il s’agit d’un **document normatif** : toute implémentation devra s’y conformer strictement.

---

## 🎯 Rôle de la factory

`TestPlanningSolutionFactory` est responsable de l’**assemblage** d’un monde OptaPlanner
**minimal valide** pour les tests.

Elle produit des `PlanningSolution` (ou `PlanningProblem` selon le nom réel du modèle)
qui sont :

- complets,
- cohérents,
- comparables entre tests,
- et conformes aux invariants du moteur.

👉 C’est la **factory d’assemblage centrale**.
Sans elle, chaque test reconstruit un monde différent et devient non comparable.

---

## 📦 Objets produits

- `PlanningSolution` **ou** `PlanningProblem` (selon le modèle effectif)

Aucun solveur n’est instancié ici.

---

## 🧱 Responsabilités explicites

La factory doit assembler explicitement :

### 1️⃣ Créneaux

- liste de `Creneau`
- tous validés par `TestCreneauFactory`

👉 Aucun créneau « bricolé » localement.

---

### 2️⃣ Ressources

- liste de ressources (`SalarieReel`, `PosteVirtuel`, et l’état non affecté si modélisé)
- toutes créées par `TestRessourceFactory`

👉 Aucune ressource incomplète.

---

### 3️⃣ Paramètres réglementaires

- une instance de `RegulatoryParameters`
- créée par `TestRegulatoryParametersFactory`

👉 Aucun paramètre partiel.

---

### 4️⃣ Contexte de résolution

Si le modèle porte un `PlanningContext` dans la solution :

- il doit être fourni par `TestPlanningContextFactory`

Si le contexte est injecté ailleurs (ex. solver config), la factory doit :

- rendre explicite le mode d’injection attendu.

👉 Aucune ambiguïté sur l’origine du contexte.

---

### 5️⃣ WorkMetrics

La factory doit respecter le principe :

- V2 : WorkMetrics peuvent être présents, mais uniquement comme **constats post-résolution**
- V3/V4 : WorkMetrics ne doivent pas être nécessaires à l’assemblage initial

Si WorkMetrics sont requis par le modèle (présence obligatoire dans la solution),
ils doivent être créés via `TestWorkMetricsFactory`.

---

## 🚫 Interdictions formelles

`TestPlanningSolutionFactory` **ne doit jamais** :

- lancer un solveur,
- calculer un score,
- modifier une variable de décision,
- affecter une ressource à un créneau,
- générer des WorkMetrics par calcul métier,
- dépendre d’un test particulier.

Elle assemble, point.

---

## 🧪 Variantes attendues (conceptuelles)

La factory devra proposer des variantes nommées pour couvrir les cas classiques :

- `minimalValide()` : 1 créneau + 1 salarié + paramètres neutres
- `avecPosteVirtuel()` : ajoute un poste virtuel
- `avecNonAffecte()` : inclut l’état « à affecter » si modélisé
- `plusieursCreneaux(...)` : assemble une liste de créneaux
- `plusieursRessources(...)` : assemble plusieurs salariés

Chaque variante :

- est documentée,
- ne fait que de l’assemblage,
- délègue toute création à des factories spécialisées.

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

> Un test qui assemble un monde sans passer par `TestPlanningSolutionFactory`
> est un test non comparable et suspect.

---

## 📌 Statut du document

- Document normatif
- Version : V1
- Toute évolution du modèle de solution (PlanningSolution/PlanningProblem)
  **doit mettre à jour ce document**

---

**Fin du document**

