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

- Restitution solveur V1 stabilisée
  - Intégration complète du solveur OptaPlanner dans SC-01
  - Mapping de la solution via `ScenarioResponseMapper`
  - Mise en place du contrat API `ScenarioResponseDTO`
  - Planning détaillé (créneaux journaliers + ressourceAffecteeId)
  - Gestion explicite des créneaux non couverts (`A_AFFECTER`)
  - `solutionSummary` (créneaux affectés / non affectés)
  - `workMetrics` (par ressource + global)
  - `scoreBreakdown` explicable (penaliteKey / unité / volume / impact)

- Explicabilité solveur V2 (scoreBreakdown + diagnostics d’affectation) ✅
    - `PenaliteKey` devient la source de vérité pour l’unité du `scoreBreakdown`
    - suppression des `switch` de résolution d’unité dans la restitution
    - centralisation de la construction des `ScoreBreakdownItemDTO`
    - ajout du bloc `diagnostics.assignmentDiagnostics`
    - exposition des créneaux non couverts avec :
        - creneauId
        - date / heureDebut / heureFin
        - activite
        - status
        - reasonCode
        - message
    - diagnostics implémentés :
        UNCOVERED / NO_RESOURCE_ASSIGNED
        IMPOSSIBLE_TO_ASSIGN

### B. En cours (fait partiellement)

- Evolution du DataSet amont
  Objectif : aligner le contrat d'entrée du moteur avec les structures réelles du logiciel de planning.

     - ajout des axes organisationnels
      - direction
      - service
      - lieu
      - poste comptable

    - ajout des données portées par la ressource
      - contrat de travail
      - contraintes réglementaires

    - ajout des champs de structuration des besoins
      - groupeBesoinId
      - blocJourId
      - ordreDansBloc
      - estSegmentDePause

    - introduction d’un jeu de test technique de référence
      permettant de tester l'alimentation WebDev → moteur.

  Stratégie :
    1. évolution compatible du schéma V1
    2. adaptation du builder
    3. introduction progressive de la structure V2

- Contrôles combinatoires (repos hebdo, dimanches max, etc.) ✅ côté contraintes
    - WorkMetrics “séquences observées” désormais calculées côté moteur
    - exposition complète des métriques encore à structurer

- Exposition complète des WorkMetrics
    - stabilisation des compteurs dans l’API scénario

### C. Prochains jalons (ce qu’on fait ensuite)

- WorkMetrics “Équité”
    - écarts vs moyenne

- Compléter le DataSet amont (ou définir une base de données de référence pour les scénarios) afin d’intégrer les éléments structurels nécessaires à la résolution :
  - identifiants de groupes de besoins ou de blocs journaliers (`groupeBesoinId`, `blocJourId`) permettant de regrouper les créneaux techniques ou fonctionnels ;
  - ordre des créneaux au sein d’un bloc (`ordreDansBloc`) afin de formaliser les règles de liaison ou de continuité sur une même journée ;
  - identification des segments de pause (`estSegmentDePause`) permettant de modéliser explicitement les interruptions dans une journée de travail ;
  - constitution d’un entrepôt des codes d’activités et identifiants associés utilisés par le moteur lors de la résolution ;
  - intégration des paramètres réglementaires applicables aux ressources nécessaires au calcul des contraintes légales.

- Améliorer l’explicabilité détaillée du solveur :
  - exposer les raisons d’incompatibilité ressource / créneau
  - étudier l’intégration de justifications typées via `ScoreExplanation`

- Améliorer la gestion des contraintes dans le `ScoreExplanation` :
Faire porter la mesure métier par la contrainte elle-même, au lieu d’essayer de la reconstruire après coup à partir du score :
La contrainte expose aussi une justification typée du genre :
  - clé de pénalité
  - unité
  - quantité
  - éventuellement ressource / créneau / date
Puis, récupérer dans `ScoreExplanation` directement ces objets.


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

🛠️ [Note technique] — Unification future des unités temporelles et des types numériques

Lors de l’implémentation des contraintes et des WorkMetrics, deux représentations numériques coexistent actuellement :
- utilisation de long pour certains calculs intermédiaires (minutes calculées, intersections temporelles),
- conversion en int lors de l’application des pénalités OptaPlanner.

Cette situation provient du fait que le score OptaPlanner utilisé (HardSoftScore) repose sur un ScoreImpacter basé sur des entiers, ce qui impose l’utilisation de penalize(...) avec des valeurs int.
Une conversion explicite (Math.toIntExact(...)) est donc parfois nécessaire lors de l’application des pénalités.

👉 Cette coexistence long / int est tolérée pour l’instant, mais une unification devra être décidée lors d’un nettoyage technique ultérieur.

Deux stratégies sont envisagées :
- Utiliser long pour tous les calculs internes, puis convertir en int uniquement au moment du scoring.
- Normaliser l’ensemble du moteur sur int, si les volumes manipulés restent garantis dans les bornes de ce type.

La première approche est actuellement privilégiée car elle :
- simplifie les calculs temporels,
- évite les risques d’overflow lors d’agrégations.

---

🕒 Convention de représentation des heures

Les durées manipulées dans le moteur doivent pouvoir être exprimées sous deux formes complémentaires :

| Format                      | Usage                               |
|-----------------------------|-------------------------------------|
| HH:MM	                      | représentation humaine / API / logs |
| HH,DC (heures en centièmes)	| calculs simplifiés côté moteur      |

Exemples :

| Temps |	HH:MM | HH,DC |
|-------|-------|-------|
| 1h30  |	01:30 |	1,50  |
| 2h15  |	02:15 |	2,25  |
| 7h45	| 07:45 |	7,75  |

La conversion en heures décimales (centièmes) facilite :
- certains calculs statistiques,
- les agrégations RH,
- les restitutions analytiques.

⚠️ Les calculs internes du moteur restent cependant basés sur les minutes, afin de garantir la précision et d’éviter les effets d’arrondi.

---

### 1.7 WorkMetrics

| Version | Statut | Contenu                                   |
|---------|--------|-------------------------------------------|
| V1      | ✅     | Volumes horaires de base                  |
| V2      | ✅     | Occurrences structurantes                 |
| V3-A    | ✅     | Pénibilités légales (minutes + dominance) |
| V3-B    | ✅     | Séquences                                 |
| V3-C    | ⏳     | Equité                                    |
| V4      | ⏳     | référentiel contractuel                   |
| V5      | ⏳     | dettes abstraites                         |

---

## 2️⃣ Capacités partiellement implémentées

| Sujet                   | Limitation actuelle                          |
|-------------------------|----------------------------------------------|
| Explicabilité détaillée | Résultats bruts uniquement                   |
|                         |(pas encore de lecture pédagogique du score)  |

- Logs de diagnostic du solveur disponibles :
  - ScoreExplanation (SolutionManager)
  - WorkMetrics calculées après résolution

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

La conception détaillée des WorkMetrics et leur état d’avancement
sont documentés dans :

`WORKMETRICS.md`

Ce document constitue la source de vérité pour :
- les métriques existantes
- les métriques à venir
- leur rôle dans le scoring et l’analyse RH.

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
SC-01 est désormais exposé via : `POST /scenarios/sc01/solve`

### Implémentation
Le scénario :
1. valide le contrat JSON ;
2. résout la ressource cible (salarié ou poste virtuel fourni) ;
3. exécute ScenarioDatasetBuilderSc01 ;
4. génère les créneaux journaliers (matin / après-midi) ;
5. produit des alertes de cohérence ;
6. retourne une réponse REST structurée.

📌 SC-01 est désormais exécuté via le solveur OptaPlanner.

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

*Historique de décision à date — remplacé depuis le 05/03/2026*

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

## [2026-03-05] — Branchement du solveur OptaPlanner sur SC-01

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

## [2026-03-06] - Contrat de sortie du moteur de planification

**Objectif**
Le moteur de planification renvoie désormais une structure de réponse normalisée décrivant :
1. la solution produite (planning résolu),
2. l’évaluation du solveur (score et pénalités),
3. les conséquences observées du planning (WorkMetrics),
4. les diagnostics techniques utiles à l’intégration.

Ce contrat de sortie constitue la référence d’échange entre le moteur de planification et le logiciel de planning.

Il est défini dans le fichier : `ScenarioResponse.schema.json`

Le détail de la conception est définie dans le document : `ScenarioResponseContract.md`

## [2026-03-08] - ScenarioResponseDTO V1 stabilisé
- scoreBreakdown (unit + quantity)
- planning avec ressourceAffecteeId
- solutionSummary
- workMetrics (byRessource + global)
- diagnostics

## [2026-03-09] – Explicabilité solveur V2 (scoreBreakdown + diagnostics d’affectation)

### Objectif
Renforcer l’explicabilité de la réponse solveur sans ouvrir à ce stade un chantier complet autour de `ScoreExplanation`.

### Évolutions réalisées
- `PenaliteKey` devient la source de vérité de l’unité de restitution du `scoreBreakdown`.
- La construction des `ScoreBreakdownItemDTO` est centralisée via une factory dédiée.
- La résolution d’unité par `switch` dans `PlanningService` est supprimée au profit d’une logique pilotée par `PenaliteKey`.
- Le bloc `diagnostics` est enrichi avec `assignmentDiagnostics`.
- Chaque diagnostic d’affectation remonte :
  - `creneauId`
  - `date`
  - `heureDebut`
  - `heureFin`
  - `activite`
  - `status`
  - `reasonCode`
  - `message`
- Les cas actuellement implémentés sont :
  - `UNCOVERED` / `NO_RESOURCE_ASSIGNED` / `IMPOSSIBLE_TO_ASSIGN`

### Effet sur le contrat de sortie
Le contrat API expose désormais explicitement les créneaux non couverts, en complément de :
- `planning` (`ressourceAffecteeId = A_AFFECTER`)
- `solutionSummary.nbCreneauxNonAffectes`
- `scoreBreakdown` (`METIER_SOFT_CRENEAU_NON_COUVERT`)

### Validation observée sur SC-01
- 5 créneaux générés
- 1 créneau affecté
- 4 créneaux non couverts
- `assignmentDiagnostics.size() = 4`
- cohérence avec `solutionSummary.nbCreneauxNonAffectes = 4`