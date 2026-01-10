# 📍 Suivi de développement — Moteur de planification

Ce document sert de **tableau de bord du projet**.

Il répond à une question simple :
> **Où en est le moteur aujourd’hui, et que reste‑t‑il à construire ?**

Il est volontairement :
- factuel,
- non prospectif,
- aligné sur l’existant réel.

---

## 🧭 Rôle du document

Ce document :
- complète le **Référentiel_Métier** (qui explique le *pourquoi*),
- complète les documents techniques (qui expliquent le *comment*),
- sert de **support de pilotage** pour la suite du développement.

👉 Il ne redéfinit aucune règle métier.
👉 Il ne décrit que des **capacités implémentées ou manquantes**.

---

## 1️⃣ Capacités actuellement implémentées

### 1.1 Modèle de décision

| Capacité                       | Statut | Commentaire                  |
|--------------------------------|--------|------------------------------|
| Créneau comme entité centrale  | ✅     | PlanningEntity unique        |
| Affectation unique par créneau | ✅     | ressourceAffectee            |
| Ressource abstraite            | ✅     | salarié réel / poste virtuel |
| État non affecté pénalisé      | ✅     | RessourceNonAffectee         |

---

### 1.2 Contraintes physiques (HARD)

| Règle                       | Statut | Fichier               |
|-----------------------------|--------|-----------------------|
| Chevauchement de créneaux   | ✅     | ChevauchementCreneaux |
| Durée maximale d’un créneau | ✅     | DureeMaxCreneau       |
| Cumul journalier maximal    | ✅     | CumulJournalierMax    |

---

### 1.3 Contraintes légales (HARD)

| Règle                         | Statut | Fichier                       |
|-------------------------------|--------|-------------------------------|
| Durée légale maximale         | ✅     | DureeMaximaleLegaleParSalarie |
| Nuits consécutives max        | ✅     | NuitsConsecutivesMax          |
| Repos obligatoire après nuits | ✅     | ReposObligatoireApresNuits    |
| Repos hebdomadaire minimal    | ✅     | ReposHebdomadaireMin          |

---

### 1.4 Contraintes légales (SOFT)

| Règle                       | Statut | Fichier                   |
|-----------------------------|--------|---------------------------|
| Repos hebdomadaire glissant | ✅     | ReposHebdomadaireGlissant |
| Dimanches travaillés max    | ✅     | DimanchesTravaillesMax    |

---

### 1.5 Contraintes métier (SOFT)

| Règle                      | Statut | Fichier                        |
|----------------------------|--------|--------------------------------|
| Créneau non couvert        | ✅     | NonAffectationCreneau          |
| Pénalisation poste virtuel | ✅     | PosteVirtuelPenalite           |
| Travail de nuit            | ✅     | CreneauNuit                    |
| Travail jour férié         | ✅     | CreneauJourFerie               |
| Travail sur repos hebdo    | ✅     | DetteReposSurReposHebdomadaire |

---

### 1.6 Paramétrage et scoring

| Élément           | Statut     | Commentaire          |
|-------------------|------------|----------------------|
| SeuilsDeTolerance | ✅         | Bornes métier        |
| Penalites         | ✅         | Intensité SOFT       |
| ScoreWeights      | ⚠️ Partiel | Branchage progressif |

---

### 1.7 WorkMetrics

| Version | Statut | Contenu                   |
|---------|--------|---------------------------|
| V1      | ✅     | Volumes horaires de base  |
| V2      | ✅     | Occurrences structurantes |
| V3      | ⏳     | Séquences et équité       |

---

## 2️⃣ Capacités partiellement implémentées

| Sujet | Limitation actuelle |
|-------------------------|---------------------------------------|
| ScoreWeights            | Pondération encore simplifiée         |
| WorkMetrics V2          | Implémentées et testées, non encore exploitées par le scoring |
| Explicabilité détaillée | Résultats bruts uniquement            |

---

## 3️⃣ Capacités identifiées mais non implémentées

### 3.1 Contraintes combinatoires avancées

| Sujet                        | Priorité |
|------------------------------|----------|
| Jours consécutifs travaillés | Haute    |
| Alternance jour / nuit       | Haute    |
| Amplitude journalière        | Moyenne  |

---

### 3.2 WorkMetrics futures

| Version | Sujet                     |
|---------|---------------------------|
| V3      | Séquences observées       |
| V4      | Référentiel contractuel   |
| V5      | Dettes et coûts abstraits |

---

### 3.3 Analyse métier aval

| Sujet | Statut |
|----|------|
| Construction SurchargeSalarie | Hors moteur |
| Aide à la décision RH | Hors moteur |

---

## 4️⃣ Ordre logique recommandé pour la suite

1. Exploiter WorkMetrics V2 dans le scoring
2. Finaliser WorkMetrics V3
3. Stabiliser ScoreWeights
4. Compléter règles combinatoires
5. Améliorer explicabilité
6. Lancer analyse métier aval

---

## 🧠 Principe de lecture

> Ce document évolue.
> Une capacité n’est marquée comme acquise que si elle est testée et intégrée.

Il doit être mis à jour **à chaque évolution structurante** du moteur.

## ✅ [10/01/2026] – Stabilisation du socle de tests WorkMetrics V2

- Finalisation du modèle `PlanningContext` :
  - ajout explicite du type de résolution
  - ajout des hypothèses d’historique
  - constructeurs normés (complet / socle test)
- Création de `TestPlanningContextFactory` (normative, pure)
- Séparation claire des factories de test :
  - ressources (`TestRessourceFactory`)
  - référentiels métier (`TestReferentielFactory`)
- Alignement strict des tests WorkMetrics avec le modèle réel :
  - utilisation de `ComptabiliteActivite`
  - référentiels minimaux mais valides
- Mise en place de deux tests WorkMetrics V2 :
  - activité générant une dette de repos
  - activité sans dette de repos

📌 Le socle de tests V2 est désormais considéré comme **stable**.

## ✅ [10/01/2026] - WorkMetrics V2 — règles validées par les tests
Ces règles constituent le périmètre fonctionnel définitif de WorkMetrics V2.
- WorkMetrics est un calcul post-résolution, indépendant d’OptaPlanner.
- Un WorkMetrics existe pour chaque ressource du PlanningProblem, même sans créneau.
- Les créneaux sont ignorés s’ils sont :
    - hors horizon temporel,
    - associés à une activité absente du référentiel.
- Les métriques sont isolées par ressource.
- Le repos hebdomadaire travaillé :
    - est comptabilisé pour les jours RH et RHD,
    - génère une dette de repos par jour distinct, pilotée par le référentiel.
- Les dimanches travaillés (RHD) :
    - sont comptés par date distincte,
    - indépendamment du nombre de créneaux sur la journée.


