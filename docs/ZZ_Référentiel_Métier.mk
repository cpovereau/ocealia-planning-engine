# 📌 Référentiel projet — Moteur de planification OptaPlanner

Ce document sert de **fil conducteur unique** pour le développement du moteur de planification.
Il recense :

* les fichiers **déjà créés et validés** dans ce fil,
* les fichiers **à créer ou à enrichir**,
* l’**ordre logique de progression**,
* les liens explicites avec les documents de conception existants.

---

## 1️⃣ Architecture globale validée

### 1.1 Domaine métier (`fr.project.planning.domain`)

#### 📂 contexte

| Fichier                     | Statut | Rôle                                                            |
| --------------------------- | ------ | --------------------------------------------------------------- |
| `PlanningContext.java`      | ✅      | Contexte global de résolution (objectif, stratégie, pénalités…) |
| `ObjectifResolution.java`   | ✅      | Intention principale (ex : couvrir au mieux)                    |
| `StrategieScoring.java`     | ✅      | Mode d’analyse (`EXPLOITATION`, `ANALYSE_RH`, `AUDIT`)          |
| `HorizonTemporel.java`      | ✅      | Fenêtre temporelle de résolution                                |
| `StrategieCouverture.java`  | ✅      | Règles d’autorisation (poste virtuel, non-affecté)              |
| `SeuilsDeTolerance.java`    | ✅      | Seuils métier (surcharge, dérives acceptables)                  |
| `Penalites.java`            | ✅      | Pondérations relatives des contraintes                          |
| `OptionsExplicabilite.java` | ✅      | Paramètres d’explication des résultats                          |

---

#### 📂 creneau

| Fichier                 | Statut | Rôle                                              |
| ----------------------- | ------ | ------------------------------------------------- |
| `Creneau.java`          | ✅      | **PlanningEntity** principale (besoin de travail) |
| `PrioriteCreneau.java`  | ✅      | Hiérarchisation métier des créneaux               |
| `TypeCreneau.java`      | ✅      | Typologie (imposé, facultatif…)                   |
| `TypePlageHoraire.java` | ✅      | Qualification jour / nuit                         |

---

#### 📂 metier

| Fichier                      | Statut | Rôle                                              |
| ---------------------------- | ------ | ------------------------------------------------- |
| `SurchargeSalarie.java`      | ✅      |  Lecture métier de la surcharge d'un scénario     |
| `CompatibiliteActivite.java` | ✅      |  Lecture de l'impact des affectations             |


---

#### 📂 ressource

| Fichier                     | Statut | Rôle                                             |
| --------------------------- | ------ | ------------------------------------------------ |
| `Ressource.java`            | ✅      | Abstraction de ressource                         |
| `SalarieReel.java`          | ✅      | Ressource réelle (compétences, sites, activités) |
| `PosteVirtuel.java`         | ✅      | Capacité fictive / révélée                       |
| `RessourceNonAffectee.java` | ✅      | État volontairement pénalisé                     |
| `TypePosteVirtuel.java`     | ✅      | Typologie des postes virtuels                    |

---

#### 📂 score

| Fichier              | Statut | Rôle                                              |
| -------------------- | ------ | ------------------------------------------------- |
| `ScoreWeights.java`  | ✅     | Pondération technique du score (non métier)       |


### 🧮 ScoreWeights — Pondération technique du score

`ScoreWeights` est un composant **strictement technique**, interne au moteur de planification.

Il a pour rôle de :
- traduire les pénalités métier (`Penalites`) en pondérations techniques du score OptaPlanner,
- garantir la hiérarchie entre contraintes HARD et SOFT,
- adapter le comportement du scoring selon la `StrategieScoring`.

`ScoreWeights` :
- ne porte **aucune règle métier**,
- n’est **pas manipulé par le métier**,
- peut évoluer indépendamment du référentiel métier.

Il constitue une couche d’adaptation entre :
- le **vocabulaire métier** (pénalités, seuils),
- et le **mécanisme d’arbitrage** du solveur (score).

La relation entre les concepts est volontairement unidirectionnelle :

Penalites (métier) → ScoreWeights (technique) → Score OptaPlanner

Ce choix garantit :
- la lisibilité métier,
- la stabilité du modèle,
- l’évolutivité de la stratégie de scoring.

---

## 2️⃣ Couche Solver (`fr.project.planning.solution` / `solver`)

| Fichier                 | Statut | Rôle                                             |
| ----------------------- | ------ | ------------------------------------------------ |
| `PlanningProblem.java`  | ✅      | **PlanningSolution** (faits + décisions + score) |
| `solverConfig-test.xml` | ✅      | Configuration OptaPlanner dédiée aux tests       |

---

## 3️⃣ Contraintes OptaPlanner (`fr.project.planning.constraints`)

### 3.1 Provider

| Fichier                       | Statut | Rôle                                  |
| ----------------------------- | ------ | ------------------------------------- |
| `ConstraintProviderImpl.java` | ✅     | Point d’entrée unique des contraintes |

---

### 3.2 Contraintes physiques (HARD)

| Fichier                      | Statut | Rôle                  |
| ---------------------------- | ------ | --------------------- |
| `ChevauchementCreneaux.java` | ✅     | Interdit les overlaps |
| `DureeMaxCreneau.java`       | ✅     | Créneau ≤ 12h         |
| `CumulJournalierMax.java`    | ✅     | Journée ≤ 24h         |

---

### 3.3 Contraintes légales (HARD)

| Fichier                              | Statut | Rôle                     |
| ------------------------------------ | ------ | ------------------------ |
| `DureeMaximaleLegaleParSalarie.java` | ✅     | Limite légale de travail |

---

### 3.4 Contraintes métier (SOFT)

| Fichier                      | Statut | Rôle                               |
| ---------------------------- | ------ | ---------------------------------- |
| `NonAffectationCreneau.java` | ✅     | Pénalisation du non-couvert        |
| `PosteVirtuelPenalite.java`  | ✅     | Pénalisation du fictif             |
| `CreneauNuit.java`           | ✅     | Travail de nuit                    |
| `CreneauJourFerie.java`      | ✅     | Travail jour férié                 |
| `DetteRepossurRH.java`       | ✅     | Travail sur RH                     |

---

## 4️⃣ Tests (`src/test/java/fr/project/planning`)

| Fichier                               | Statut | Rôle                          |
| ------------------------------------- | ------ | ----------------------------- |
| `StrategieScoringComparisonTest.java` | ✅     | Test de référence stratégique |

---

### 📂 fixtures

| Fichier                     | Statut | Rôle                           |
| --------------------------- | ------ | ------------------------------ |
| `TestRessourceFactory.java` | ✅     | Fabrique de ressources de test |

---

### 🧾 Surcharge salarié — Décision de conception

| Élément            | Rôle                     |
| ------------------ | ------------------------ |
| Moteur             | Évalue et pénalise       |
| Score              | Arbitre                  |
| Résultats          | Exposent les indicateurs |
| `SurchargeSalarie` | **Interprète**           |

La surcharge salarié ne constitue pas une entité du moteur de planification.
Elle n’est ni une variable de décision, ni un fait consommé par le solveur.
La surcharge est une lecture métier dérivée, construite à partir :
   - des indicateurs de charge (WorkMetrics),
   - des violations de règles combinatoires,
   - des seuils définis dans le PlanningContext.

Le moteur de planification :
   - évalue les situations de surcharge,
   - applique des pénalités ou des exclusions,
   - rend visibles les dépassements dans les résultats.

L’objet métier SurchargeSalarie est construit en aval de la résolution,
afin de :
   - qualifier le niveau de surcharge (alerte / SOFT / HARD),
   - expliciter les causes,
   - soutenir l’aide à la décision RH.

Ce choix garantit :
   - la séparation stricte entre décision et interprétation,
   - l’évolutivité des règles métier, 
   - l’absence de logique métier figée dans le moteur.

L’analyse de la surcharge salarié est volontairement différée tant que :
- les WorkMetrics ne sont pas stabilisés,
- la stratégie de pondération du score (`ScoreWeights`) n’est pas finalisée.

Cette séparation permet d’éviter toute interprétation prématurée du score
et garantit la robustesse du moteur de décision.

---

## 5️⃣ Ordre logique de développement à venir

1. **Stabilisation des métriques de sortie (WorkMetrics)**

   * WorkMetrics V1 : volume de travail, nuit, férié, repos hebdomadaire
   * WorkMetrics V2 : occurrences structurantes (ex. dimanches travaillés)
   * Aucun usage analytique à ce stade

   Les métriques liées à la durée légale ou contractuelle du travail sont exprimées de manière relative au temps contractuel de référence du salarié.
   Le moteur ne statue pas sur la légalité d’un dépassement, mais mesure un écart observé exploitable par l’analyse métier.

2. **Consolidation des contraintes combinatoires**

   * contraintes légales HARD (nuits consécutives, repos obligatoires…)
   * variantes SOFT d’approche des seuils
   * alignement avec `SeuilsDeTolerance`

3. **Stabilisation du scoring**

   * clarification du rôle respectif `Penalites` / `ScoreWeights`
   * premiers branchements expérimentaux de `ScoreWeights`
   * comparaison de scénarios

4. **Analyse métier aval (différée)**

   * construction de `SurchargeSalarie`
   * interprétation via WorkMetrics + seuils
   * aide à la décision RH

5. **Finalisation documentation**

   * delta UML
   * alignement avec `STRATEGIE_DE_SCORING.md`
   * gel du modèle V1

---

## 6️⃣ Principe directeur à ne jamais perdre

> **Le moteur juge. Il ne calcule pas.**

* Toutes les qualifications (nuit, férié, durée…) sont **faites en amont**.
* OptaPlanner arbitre selon une intention explicite.
* Le score reflète un compromis, pas une vérité comptable.

---

📍 **Ce document est la référence de suivi du projet.**
À mettre à jour à chaque évolution structurante.
