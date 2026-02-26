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

| Élément           | Statut     | Commentaire                                                        |
|-------------------|------------|--------------------------------------------------------------------|
| SeuilsDeTolerance | ✅         | Bornes métier                                                      |
| Penalites         | ✅         | Clés de pénalité (`PenaliteKey`)                                   |
| ScoreWeights      | ✅         | Pondérations V2 centralisées et stabilisées                        |
| ScoreUtils        | ✅         | Point de passage unique pour la construction du score              |
| StrategieScoring  | ✅         | Contexte de lecture du scoring (EXPLOITATION / ANALYSE_RH / AUDIT) |

---

### 1.7 WorkMetrics

| Version | Statut | Contenu                   |
|---------|--------|---------------------------|
| V1      | ✅     | Volumes horaires de base  |
| V2      | ✅     | Occurrences structurantes |
| V3      | ⏳     | Séquences et équité       |

---

## 2️⃣ Capacités partiellement implémentées

| Sujet                   | Limitation actuelle                          |
|-------------------------|----------------------------------------------|
| Explicabilité détaillée | Résultats bruts uniquement                   |
|                         |(pas encore de lecture pédagogique du score)  |
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

| Sujet                         | Statut      |
|-------------------------------|-------------|
| Construction SurchargeSalarie | Hors moteur |
| Aide à la décision RH         | Hors moteur |

---

## 4️⃣ Ordre logique recommandé pour la suite

1. Cadrage du scoring V3 (équité, pénibilité, séquences)
2. Finalisation WorkMetrics V3
3. Amélioration de l’explicabilité (lecture du score)
4. Nettoyage technique OptaPlanner (API deprecated)
5. Analyse métier aval

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

## 🔒 [09/02/2026] — Gel officiel du scoring V2

Le scoring V2 est **considéré comme stabilisé**.

### Périmètre gelé
- 4 pénalités SOFT V2 stabilisées :
  - Créneau non couvert (occurrence)
  - Affectation poste virtuel (occurrence)
  - Travail de nuit (minutes)
  - Travail jour férié (minutes)
- Séparation stricte des responsabilités :
  - contraintes = mesure
  - ScoreWeights = pondération
  - ScoreUtils = construction du score
- `StrategieScoring` utilisée comme **contexte de lecture**, sans logique algorithmique.

### Preuves
- Tests de dominance implémentés et validés (`ScoreDominanceTest`).
- Seuils d’arbitrage cohérents avec les poids V2.
- Documentation synchronisée :
  - `DECISIONS_CONCEPTION_OPTAPLANNER.md`
  - `STRATEGIE_DE_SCORING.md`
  - `WORKMETRICS.md`

### Règles de non-régression
- ❌ Pas de modification des poids V2 sans ouverture explicite d’une V3.
- ❌ Pas de logique de stratégie dans les contraintes.
- ❌ Pas d’utilisation des WorkMetrics dans le calcul du score.

## ✅ [24/02/2026] — SC-01 — Génération de planning

### État
SC-01 est désormais exposé via : `POST /scenarios/sc-01/solve`

### Implémentation actuelle
Le scénario :
1. valide le contrat JSON ;
2. résout la ressource cible (salarié ou poste virtuel fourni) ;
3. exécute ScenarioDatasetBuilderSc01 ;
4. génère les créneaux journaliers (matin / après-midi) ;
5. produit des alertes de cohérence ;
6. retourne une réponse REST structurée.

📌 Le solveur OptaPlanner n’est pas encore appelé dans SC-01.

### Fonctionnement actuel
SC-01 réalise une génération déterministe, basée sur :
- dailyAmplitudeHours (incluant pause) ;
- shiftStart ;
- shiftEndAlert (borne d’alerte non bloquante) ;
- lunchBreak (optionnel, défaut 12:00–13:00) ;
- workedDays (ISO DayOfWeek) ;
- holidayDates.
Les règles RH / RHD sont appliquées par bloc hebdomadaire (lun→dim).

### Alertes actuellement produites
- SHIFT_END_EXCEEDED
- LUNCH_BREAK_OUTSIDE_AMPLITUDE
- INSUFFICIENT_WEEKLY_REST
- TOO_MANY_NON_WORKED_DAYS

📌 Ces alertes sont calculées en phase pré-solveur (builder).
Elles ne proviennent pas du score OptaPlanner.

### Points non encore implémentés
- aucune optimisation ;
- aucun scoring ;
- aucun équilibrage ;
- aucun calcul de surcharge ;
- aucune contrainte légale intégrée dans le solveur.

### Prochaine étape identifiée
- Brancher SC-01 sur :
- un modèle PlanningSolution minimal ;
- un appel SolverManager ;
- un ConstraintProvider initial ;
- un timeout contrôlé.

## ✅ [25/02/2026] — Statut de branchement solveur – SC-0

🧩 État actuel
Le moteur de contraintes est désormais :
- Aligné sur la définition canonique du travail réel (compteDansCharge)
- Cohérent entre :
  - Contraintes légales
  - Contraintes métier
  - Limites physiques
  - WorkMetrics
- Paramétré via PlanningContext / SeuilsDeTolerance
- Documenté dans DECISIONS_CONCEPTION_OPTAPLANNER.md

⚠️ Le scénario SC-01 n’est pas encore branché au solveur en exécution réelle.

🎯 Décision technique
Le branchement du solveur (appel réel à OptaPlanner + restitution planning) est volontairement différé jusqu’à :
1. Validation complète :
   - des contraintes
   - des WorkMetrics
   - du paramétrage référentiel
2. Stabilisation documentaire
3. Vérification de cohérence “doc ↔ code”

📌 Justification
Éviter :
- Débogage fonctionnel pendant que la structure métier évolue
- Faux diagnostics liés à des contraintes partiellement activées
- Effets de bord dans les scénarios de test

Le solveur sera branché uniquement lorsque :
- le périmètre V2 stabilisé est officiellement figé,
- la liste des contraintes activées dans ConstraintProviderImpl est définitive.

## ✅ [26/02/2026] – Activités : ajout du code planning + diagnostics WorkMetrics (dev)

### Objectif
Améliorer la réintégration des résultats côté logiciel de planning en transportant l’identifiant d’activité (base planning) dans les créneaux.

### Évolutions
- Modèle `Creneau` :
  - ajout de `codeActiviteId` (ex. `10101`, `101478`) en complément de `activite` (libellé).
- WorkMetrics (post-solve) :
  - `WorkMetricsCalculator` résout l’activité en **priorisant** `codeActiviteId`, avec **fallback** sur `activite` pour compatibilité.
  - Mode dev (Option B) : si activité inconnue / référentiel manquant ou incomplet, le créneau reste **neutre**.
  - Ajout de diagnostics de synthèse (affichage console) :
    - créneaux sans ressource,
    - créneaux hors horizon,
    - créneaux ignorés car activité inconnue / non mappée.

### Intégration attendue
- Le logiciel de planning doit fournir un `ReferentielComptabiliteActivite` dont les clés correspondent à `codeActiviteId`.
- Les champs `compteDansCharge` et `genereDetteRepos` peuvent être dérivés automatiquement (sous-type d’activité).
- `estServiceCritique` et `prioritaireSurConfort` restent des paramètres client, souvent optionnels.
