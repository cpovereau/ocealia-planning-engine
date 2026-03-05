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

## 📌 État & Cap (source de vérité)

### A. Livré (fait, prouvé)

- WorkMetrics V3 (pénibilités minutes + dominance) ✅
    - Preuves : .\gradlew test OK + grep OK (isNuit/isJourFerie/TypeCreneau.NUIT)
    - Fichiers clés : TimeBreakdownCalculator, PenibilitesLegalesMinutes, ScoreUtils, RegulatoryParameters, fixtures

- Branchement du solveur OptaPlanner sur SC-01 ✅
    - Pipeline validé : ScenarioController → PlanningRequest → PlanningService → SolverLauncher → OptaPlanner
    - Score vérifié en exécution : `0hard/-40000soft → 0hard/0soft`
    - Affectations confirmées via logs (`creneau → ressource`)
    - Solution récupérée via `solved.solution().getCreneaux()`

### B. En cours (fait partiellement)

- Contrôles combinatoires (repos hebdo, dimanches max, etc.) ✅ côté contraintes
    - ⚠️ pas encore exposés en WorkMetrics “observées”

### C. Prochain jalon (ce qu’on fait ensuite)

- WorkMetrics “Séquences observées”
    - max jours consécutifs
    - max nuits consécutives

- WorkMetrics “Équité”
    - écarts vs moyenne

### D. Hors périmètre (assumé)

- surcharge salarié
- aide RH décisionnelle (analyse aval)

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

| Règle                                        | Statut | Fichier                   |
|----------------------------------------------|--------|---------------------------|
| Repos hebdomadaire glissant                  | ✅     | ReposHebdomadaireGlissant |
| Dimanches travaillés max                     | ✅     | DimanchesTravaillesMax    |
| Pénibilités minutes nuit / dimanche / férié  | ✅     | PenibilitesLegalesMinutes |
| Dominance des pénibilités                    | ✅     | ScoreUtils                |
| Calcul d’intersection temporelle             | ✅     | TimeBreakdownCalculator   |


**Commentaires :**
- Les pénibilités sont désormais calculées par minutes réelles et non par type de créneau.
- Les anciennes contraintes `CreneauDeNuit` et `CreneauJourFerie` ont été remplacées par une contrainte unifiée.
- Le moteur utilise un calcul par intersection temporelle afin de représenter correctement les cas :
  - nuit partielle,
  - nuit + dimanche,
  - nuit + jour férié.

---

### 1.5 Contraintes métier (SOFT)

| Règle                      | Statut  | Fichier                        |
|----------------------------|---------|--------------------------------|
| Créneau non couvert        | ✅      | NonAffectationCreneau          |
| Pénalisation poste virtuel | ✅      | PosteVirtuelPenalite           |
| Travail de nuit            | obsolète | CreneauNuit                    |
| Travail jour férié         | obsolète | CreneauJourFerie               |
| Travail sur repos hebdo    | ✅      | DetteReposSurReposHebdomadaire |

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

| Version | Statut | Contenu                                   |
|---------|--------|-------------------------------------------|
| V1      | ✅     | Volumes horaires de base                  |
| V2      | ✅     | Occurrences structurantes                 |
| V3-A    | ✅     | Pénibilités légales (minutes + dominance) |
| V3-B    | ⏳     | Séquences                                 |
| V3-C    | ⏳     | Equité                                    |
| V4      | ⏳     | référentiel contractuel                   |
| V5      | ⏳     | dettes abstraites                         |

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

1. Amélioration de l’explicabilité (lecture du score)
2. Nettoyage technique OptaPlanner (API deprecated)
3. Analyse métier aval

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

## ✅ [04/03/2026] – Historique d’évolution — V2 → V3 (Scoring des pénibilités)

### Contexte V2
Dans la version V2 du moteur, certaines pénibilités étaient détectées directement à partir des caractéristiques du créneau :
- créneau de nuit (TypePlageHoraire == NUIT)
- créneau de jour férié (QualificationJour == FERIE)
- créneau de dimanche

Les contraintes associées étaient notamment :
- CreneauDeNuit
- CreneauJourFerie

Cette approche présentait plusieurs limites :
1. elle ne permettait pas de représenter correctement des créneaux partiellement concernés
exemple : 18h–23h contenant seulement une heure de nuit ;
2. elle ne permettait pas de traiter correctement les chevauchements de pénibilités
exemple : samedi 22h → dimanche 06h ;
3. elle introduisait des doubles pénalités (nuit + dimanche, nuit + férié).

### Décision V3
La V3 adopte un modèle basé sur l’intersection temporelle réelle.
Chaque créneau est décomposé en minutes appartenant aux intervalles réglementaires via : `TimeBreakdownCalculator`

Ce calcul produit :
- minutes de nuit
- minutes de dimanche
- minutes de jour férié
- ainsi que leurs intersections.

### Nouvelle architecture de scoring
Les pénibilités sont désormais traitées par une contrainte unique :
`PenibilitesLegalesMinutes`

Cette contrainte :
1. calcule les intersections via TimeBreakdownCalculator
2. applique la logique de dominance
3. applique les poids définis dans le contexte de résolution

La logique de dominance est implémentée dans :
`ScoreUtils.penalitesLegalesAvecDominance(...)`

### Règle de dominance
Pour éviter toute double pénalité, un ordre de dominance est appliqué :
- NUIT > DIMANCHE > FERIE

Les minutes appartenant à plusieurs catégories sont attribuées à la pénibilité dominante uniquement.

Source réglementaire

### La définition des intervalles réglementaires est externalisée dans :
`RegulatoryParameters`

Ce composant contient :
- l’intervalle réglementaire de nuit
- la liste des jours fériés

Il est injecté dans le solveur comme ProblemFact via `PlanningProblem`.

### Conséquence sur les contraintes
Les contraintes suivantes ne sont plus utilisées pour le scoring :
- CreneauDeNuit
- CreneauJourFerie

Elles sont remplacées par une contrainte unique plus précise et plus robuste.

### Bénéfices de la V3
La nouvelle architecture permet :
- une modélisation fidèle des situations réelles
- l’élimination des doubles pénalités
- une extensibilité vers d’autres pénibilités temporelles
- une meilleure explicabilité des décisions du moteur

### Compatibilité avec V2
Les règles métier HARD et les contraintes de couverture ne sont pas affectées par cette évolution.

La V3 modifie uniquement :
- le calcul des pénibilités légales SOFT
- la façon dont les minutes de pénibilité sont calculées.

## 2026-03-05 — Branchement du solveur OptaPlanner sur SC-01

Le scénario SC-01 est désormais exécuté en passant par le solveur OptaPlanner.

Pipeline complet validé :

SC-01 JSON  
→ ScenarioController  
→ ScenarioDatasetBuilderSc01  
→ PlanningRequest  
→ PlanningService  
→ SolverLauncher  
→ OptaPlanner  
→ PlanningProblem résolu  
→ ScenarioResponseDTO

### Vérifications effectuées

1. Le solveur est bien appelé depuis `ScenarioController` via `PlanningService.solve(pr)`.
2. La solution retournée est récupérée via `solved.solution().getCreneaux()`.
3. Le scoring OptaPlanner est confirmé via les logs :
   - Solving started: best score (0hard/-40000soft)
   - Local Search ended: best score (0hard/0soft)
   - Solving ended
4. Vérification explicite dans `ScenarioController` :
   - [SC-01] Score final OptaPlanner = 0hard/0soft
   - [SC-01] Affectations = [
        SC01-2026-02-23-001 -> 1041,
        SC01-2026-02-24-002 -> 1041,
        SC01-2026-02-25-003 -> 1041,
        SC01-2026-02-26-004 -> 1041
      ]

Ces logs confirment que :

- le scoring est exécuté
- le solveur modifie l’état initial pour améliorer le score
- les créneaux sont effectivement affectés par OptaPlanner.

### État actuel de l’API

La réponse HTTP renvoyée par l’API reste pour l’instant basée sur le modèle métier (`ScenarioResponseDTO`).

La structuration complète de la réponse du solveur (score, affectations détaillées, diagnostics) est **différée volontairement** afin de la concevoir proprement lors de la définition du contrat de sortie du moteur.

### Conclusion

Le branchement solveur est considéré comme **validé** :

- génération dataset SC-01 ✔  
- construction `PlanningProblem` ✔  
- exécution solveur ✔  
- amélioration du score ✔  
- récupération de la solution ✔