# 📍 Suivi de développement — Moteur de planification

Ce document constitue **le tableau de bord réel du projet moteur de planification**.

Il répond à la question :

> Où en est réellement le moteur aujourd’hui et que reste-t-il à construire ?

Ce document est volontairement :

* factuel
* aligné sur l’implémentation réelle
* orienté pilotage du projet

Il ne redéfinit aucune règle métier et ne décrit que des **capacités implémentées ou manquantes**.

---

# 🧭 Rôle du document

Ce document :

* complète le **référentiel métier** (qui explique le pourquoi)
* complète les documents techniques (qui expliquent le comment)
* sert de **support de pilotage du développement du moteur**

Il doit être mis à jour **à chaque évolution structurante du moteur**.

---

# 📌 État & Cap (source de vérité)

## A. Livré (fait et prouvé)

### WorkMetrics V3

Implémentation complète des métriques RH suivantes :

* calcul des pénibilités en minutes
* détection des nuits
* détection des jours fériés
* dominance jour / nuit
* séquences observées

Preuves :

```
.\\gradlew test
```

Fichiers principaux impliqués :

* TimeBreakdownCalculator
* PenibilitesLegalesMinutes
* ScoreUtils
* RegulatoryParameters

---

### Branchement du solveur OptaPlanner

Le solveur est désormais intégré dans le pipeline complet du moteur.

Pipeline actuel :

```
ScenarioController
    ↓
PlanningRequest
    ↓
PlanningService
    ↓
SolverLauncher
    ↓
OptaPlanner
```

Preuves d'exécution :

* score observé en exécution
* affectations confirmées dans les logs
* récupération de la solution via `solved.solution().getCreneaux()`

---

### Restitution solveur V1 stabilisée

Le moteur restitue désormais une réponse complète via `ScenarioResponseDTO`.

Blocs principaux exposés :

```
planning
workMetrics
solutionSummary
solverResult
scoreBreakdown
```

Fonctionnalités incluses :
* mapping de la solution via `ScenarioResponseMapper`
* planning détaillé par créneau
* restitution à l'identique de l'`id` et du `lieu` reçus (lot L2, 2026-07-31) — clé de
  réintégration côté appelant, voir `92_cadrage_donnees_amont_scenarios.md` §6.7
* gestion explicite des créneaux non couverts (`A_AFFECTER`)
* résumé de solution
* métriques par ressource et globales
* score explicable

La structure de réponse est stabilisée et exploitable côté API,
avec un niveau d’information suffisant pour :
- analyser un planning,
- comprendre le score,
- identifier les créneaux non couverts.

---

### Explicabilité du solveur V2

Ajout d’une couche d’explicabilité avancée.

Évolutions :

* `PenaliteKey` devient la référence pour l’unité des pénalités
* centralisation de la construction des `ScoreBreakdownItemDTO`
* suppression des résolutions d’unité par `switch`

Ajout du bloc :

```
diagnostics.assignmentDiagnostics
```

Ce bloc expose :

* créneaux non couverts
* raison de non affectation
* message diagnostique

Diagnostics implémentés :

* UNCOVERED
* NO_RESOURCE_ASSIGNED
* IMPOSSIBLE_TO_ASSIGN

---

### Contraintes métier SOFT

Contraintes actuellement implémentées :

| Règle                          | Statut     |
| ------------------------------ | ---------- |
| Créneau non couvert            | Implémenté |
| Pénalisation poste virtuel     | Implémenté |
| Travail sur repos hebdomadaire | Implémenté |

Le calcul des pénibilités repose désormais sur une approche unifiée.

Voir :
- 40_WORKMETRICS.md
- 40_STRATEGIE_DE_SCORING.md

---

### Paramétrage du scoring

Le scoring repose désormais sur :

| Élément           | Rôle                        |
| ----------------- | --------------------------- |
| SeuilsDeTolerance | bornes métier               |
| Penalites         | clés métier du score        |
| ScoreWeights      | pondérations stabilisées    |
| ScoreUtils        | construction du score       |
| StrategieScoring  | contexte d’analyse du score |

Le paramétrage du scoring est désormais centralisé et stable, permettant d’ajuster les pondérations sans modifier les contraintes.

---

## 🧩 Cartographie des contraintes et des métriques associées

> 📌 Cette section constitue la **vue de synthèse centrale** entre :
>
> * les contraintes du moteur (HARD / SOFT)
> * les métriques observables (WorkMetrics)
> * les éléments d’explicabilité (diagnostics, score)
>
> Elle permet de répondre à la question :
> **"Qu’est-ce qui est contrôlé, observable et implémenté dans le moteur ?"**

---

### 🔎 Principe de lecture

* Une **contrainte** agit dans le solveur (validation ou pénalisation)
* Une **métrique** décrit le résultat après résolution
* Un **diagnostic** explique certains cas particuliers

👉 Une métrique n’active jamais une contrainte.
👉 Une contrainte peut être expliquée par une métrique.

**Colonne « Implémentée » — trois états, pas deux :**

| Marque | Sens |
|---|---|
| ✅ | écrite, enregistrée et **effective** pour un client conforme au contrat |
| ⏳ | partielle : effective, mais tous les cas ne sont pas couverts |
| ⛔ | **dormante** — écrite et enregistrée, mais ne produit aucun match en production |

> ⛔ ne veut pas dire « non écrite ». Une contrainte dormante est dans
> `ConstraintProviderImpl`, ses tests sont verts, et elle ne sert à rien : elle lit le champ
> d'activité déprécié, que les clients n'envoient plus. Une métrique peut alors compter un
> dimanche travaillé pendant que la contrainte censée le plafonner reste muette.
>
> **Aucune contrainte n'est plus dans cet état** : les six identifiées ont été remises en service
> par les lots S7.1 à S7.6. Le marqueur est conservé — c'est un état que la cartographie doit
> pouvoir décrire, et ne pas savoir le nommer est précisément ce qui a permis à l'écart de durer.
> Constat et journal des lots : `92_cadrage_socle_reglementaire.md`.

---

### 📊 Tableau de cartographie

| Contrainte                            | Type | Implémentée  | Métrique associée             | Exposée en WorkMetrics | Diagnostic associé                        | ScoreBreakdown | Source documentaire         |
| ------------------------------------- | ---- | ------------ | ----------------------------- | ---------------------- | ----------------------------------------- | -------------- | --------------------------- |
| Créneau non couvert                   | SOFT | ✅            | nbCreneauxNonAffectes         | Oui                    | UNCOVERED / NO_RESOURCE_ASSIGNED          | Oui            | 50_ScenarioResponseContract |
| Pénalisation poste virtuel            | SOFT | ✅            | –                             | Non                    | VIRTUAL_ASSIGNED / POSTE_VIRTUEL_ASSIGNED | Oui            | 50_ScenarioResponseContract |
| Travail sur repos hebdomadaire (R8)   | SOFT | ✅            | heuresReposHebdoTravaille     | Oui                    | Non                                       | Oui            | 40_WORKMETRICS              |
| Repos hebdo glissant (R7 conventionnel) | HARD | ✅ lot S7.4 | –                          | Non                    | Non                                       | Oui (si violation) | 92_cadrage_socle_reglementaire |
| Repos hebdomadaire minimum (R7 socle) | HARD | ✅ lot S7.5  | –                             | Non                    | Non                                       | Oui (si violation) | 92_cadrage_socle_reglementaire |
| Repos obligatoire après nuits (R4)    | HARD | ✅ lot S7.3  | –                             | Non                    | Non                                       | Oui (si violation) | 92_cadrage_socle_reglementaire |
| Durée travaillée max par jour         | HARD | ✅ lot S7.6  | –                             | Non                    | Non                                       | Oui (si violation) | 92_cadrage_socle_reglementaire |
| Nuits consécutives max (R3)           | HARD | ✅ lot S7.2  | maxNuitsConsecutivesObservees | Oui                    | Non                                       | Oui (si violation) | 92_cadrage_socle_reglementaire |
| Jours consécutifs max (R1)            | SOFT | ✅            | maxJoursConsecutifsObservees  | Oui                    | Non                                       | Non            | 40_REGLES_COMBINATOIRES     |
| Alternance jour / nuit (R5+R6)        | SOFT | ✅            | –                             | Non                    | Non                                       | Oui            | 40_REGLES_COMBINATOIRES     |
| Amplitude journalière max (R10)       | SOFT | ✅            | –                             | Non                    | Non                                       | Oui            | 40_REGLES_COMBINATOIRES     |
| Repos quotidien minimum (R12)         | SOFT | ✅ lot S3     | –                             | Non                    | Non                                       | Oui            | 40_REGLES_COMBINATOIRES §6 bis |
| Durée hebdomadaire maximale (R13)     | SOFT | ✅ lot S3     | –                             | Non                    | Non                                       | Oui            | 40_REGLES_COMBINATOIRES §6 bis |
| Dimanches maximum (R9)                | SOFT | ✅ lot S7.1  | nbDimanchesTravailles         | Oui                    | Non                                       | Oui (si dépassement) | 92_cadrage_socle_reglementaire |
| Pénibilité nuit                       | SOFT | ✅            | heuresNuit                    | Oui                    | Non                                       | Oui            | 40_WORKMETRICS              |
| Pénibilité jours fériés               | SOFT | ✅            | heuresJourFerie               | Oui                    | Non                                       | Oui            | 40_WORKMETRICS              |
| Nuit affectée à salarié non-nuit      | SOFT | ✅            | nbCreneauxNuitNonNuit         | Oui                    | Non                                       | Oui            | 40_WORKMETRICS              |
| Jour férié refusé (travailleJourFerie)| HARD | ✅            | –                             | Non                    | JOUR_FERIE_NON_COUVERT                    | Non            | 50_ScenarioResponseContract |
| Indisponibilité salarié               | HARD | ✅            | –                             | Non                    | Non                                       | Non            | 90_plan_migration §Phase 4  |

---

### 🧠 Lecture architecturale

Cette cartographie matérialise la chaîne suivante :

```text
Contrainte → Score → Métrique → Diagnostic
```

Elle permet :

* d’identifier les écarts entre implémentation et cible
* de vérifier la cohérence entre scoring et restitution
* de prioriser les développements futurs

---

### 📌 Règles d’évolution

* Toute nouvelle contrainte doit être ajoutée à ce tableau
* Toute nouvelle métrique doit être reliée à une contrainte ou explicitement marquée comme "descriptive uniquement"
* Le statut "Implémentée" doit refléter l’état réel du code (tests + exécution)

---

### ⚠️ Important

Cette section ne remplace pas :

* `40_REGLES_COMBINATOIRES.md` (définition des règles)
* `40_WORKMETRICS.md` (définition des métriques)

Elle joue un rôle de **cartographie transverse uniquement**.

---

## Tableau de suivi des WorkMetrics

Ce tableau constitue la source de vérité sur l'état d'implémentation des WorkMetrics.
La définition fonctionnelle de chaque domaine est dans `40_WORKMETRICS.md`.

| Domaine                              | Livré | Où (code)                                           | Où (tests)         | Doc                  |
| ------------------------------------ | ----- | --------------------------------------------------- | ------------------ | -------------------- |
| Mesures temporelles (nuit/dim/férié) | ✅    | TimeBreakdownCalculator + PenibilitesLegalesMinutes | tests existants    | 40_WORKMETRICS §2    |
| WorkMetrics de restitution           | ⏳    | WorkMetricsCalculator                               | scénarios          | 40_WORKMETRICS §4-5  |
| Dominance                            | ✅    | ScoreUtils                                          | ScoreDominanceTest | 40_WORKMETRICS §2    |
| Séquences (contraintes)              | ✅    | ReposHebdomadaireMin/Glissant                       | tests contraintes  | 40_REGLES_COMBINATOIRES |
| Séquences (WorkMetrics observées)    | ✅    | WorkMetricsCalculator                               | scénario 1         | 40_WORKMETRICS §5.1  |
| Équité (WorkMetrics) — V3-C          | ❌    | –                                                   | –                  | 40_WORKMETRICS §5.2  |
| Référentiel contractuel — V4         | ❌    | –                                                   | –                  | 40_WORKMETRICS §5.3  |
| Dettes & coûts abstraits — V5        | ❌    | –                                                   | –                  | 40_WORKMETRICS §5.4  |

---

## Détail des WorkMetrics implémentées et validées (V2)

Les règles suivantes sont implémentées et validées par les tests automatisés.

### Métriques calculées

**Travail total**
- Somme des durées de tous les créneaux valides (`compteDansCharge = true`).

**Travail de nuit**
- Somme des minutes dans la plage réglementaire de nuit, calculées par intersection via `TimeBreakdownCalculator`.

**Travail les jours fériés**
- Somme des minutes sur un jour férié, calculées par intersection via `TimeBreakdownCalculator`.

**Repos hebdomadaire travaillé**
- Un créneau `RH` ou `RHD` est un repos hebdomadaire non travaillé.
- La durée du créneau est ajoutée aux minutes de repos hebdomadaire travaillé.
- La dette de repos est pilotée par le `ReferentielComptabiliteActivite`, comptabilisée par jour distinct.

**Dimanches travaillés**
- Un créneau sur un dimanche calendaire dont l'activité compte dans la charge.
- Comptage par date distincte — plusieurs créneaux le même jour = un seul dimanche travaillé.

---

# B. En cours (fait partiellement)

## Évolution du DataSet d’entrée

Objectif :

Aligner le contrat d’entrée du moteur avec les structures réelles du logiciel de planning.

Évolutions introduites :

### Axes organisationnels

* direction
* service
* lieu
* poste comptable

### Données portées par la ressource

* contrat de travail — bloc `contrat` livré au lot S2 (2026-08-10) : durée journalière moyenne,
  durée hebdomadaire habituelle, jours travaillés par semaine (défaut 5), salarié annualisé.
  Transporté et mappé vers `ContratSalarie` ; **aucune contrainte ne le lit à ce stade**.
  Voir `92_cadrage_scenario_sc-06.md` §4.6.
* contraintes réglementaires

### Structuration des besoins

* groupeBesoinId
* blocJourId
* ordreDansBloc
* estSegmentDePause

### Scénario SC-06 — désignation de la ressource la plus à même de couvrir un besoin (lot S4, 2026-08-10)

`POST /scenarios/sc06/solve`. Restitue un **podium de trois solutions classées** dans un bloc
`candidats[]` propre à ce scénario — clé absente pour SC-01 et SC-03.

Particularité architecturale : **SC-06 n'appelle pas le solveur**. Il classe des possibilités au
lieu d'en chercher une, par énumération exhaustive des candidats éligibles, chacun évalué par
`SolutionManager.explain()`. D'où trois propriétés qu'un `solve()` n'offre pas — déterminisme
(vérifié par test), exhaustivité, et un motif attaché à chaque rang.

Classement par paliers lexicographiques : conformité → couverture → mono-ressource → déjà en
poste → score SOFT → charge relative au contrat.

Chaque candidat porte ses **impacts chiffrés** (lot S5) : amplitude journalière, heures du jour
et heures de la semaine, en avant / après / delta, avec le plafond individuel du salarié.
Une entrée par ressource réelle mobilisée. ⚠️ Un `depassement` signalé décrit une conséquence,
il ne garantit pas qu'une contrainte la sanctionne — c'est notamment le cas de `heuresJour`,
mesuré alors que `heuresMaximumParJour` n'est pas encore individualisé (lot S7).

Accessible par **les deux canaux** (lot S6) : `POST /scenarios/sc06/solve` et FileAdapter
(`scenarioType: SC-06`), qui produisent le même résultat — vérifié par test. SC-06 est inscrit au
contrat série 50 : `50_ScenarioContract.md` §4, `50_ScenarioResponseContract.md` §6, OpenAPI et
schémas JSON. Notice d'intégration : `Windev_part/SC-06/sc_06_notice_integration.md`.

**Les lots S1 à S6 de SC-06 sont livrés.** Voir `92_cadrage_scenario_sc-06.md`.

Le lot S7 initialement prévu — activer les trois dernières contraintes individuelles — s'est
révélé être le sommet d'un écart plus large : **six contraintes enregistrées ne se déclenchent
jamais** pour un client conforme au contrat. Ce chantier fait l'objet de son propre cadrage et de
son propre découpage : `92_cadrage_socle_reglementaire.md`.

### Socle réglementaire — lot S7.0 (2026-08-10)

Point zéro du chantier de remise en service. **Aucune contrainte modifiée, score inchangé** —
c'est l'objet du lot : rendre mesurables les écarts des lots suivants.

* Cinq seuils rapatriés de `SeuilsDeTolerance` vers `contraintesReglementaires` du salarié
  (`nuitsConsecutivesMaximum`, `joursReposMinimumApresNuits`, `dimanchesTravaillesMaximum`,
  `reposHebdomadaireFenetreJours`, `reposHebdomadaireJoursOffMinimum`) — ils étaient globaux et
  n'ont jamais été alimentés : ils valaient 0 en production.
* Règle d'activation unique : `ContraintesReglementairesSalarie.seuilActif()` — absent ou nul
  ⇒ contrainte inactive, `0` tracé en WARN. WARN également si la paire fenêtre / jours off est
  transmise à moitié.
* `SocleReglementaireBaselineTest` : chaque situation fautive est jouée avec le champ historique
  (elle déclenche) puis avec le champ du contrat (elle ne déclenche pas). Réveiller une
  contrainte consiste à déplacer une assertion d'un bloc à l'autre.
* `ReferentielComptabiliteActivite.getByCode(null)` rendu null-safe — son comportement dépendait
  jusqu'ici de l'implémentation de `Map` (silencieux en production, NPE en test).
* Contrat : `50_ScenarioContract.md` §3.7, schéma JSON et OpenAPI.

434 tests, 0 échec.

### Socle réglementaire — lot S7.1 : dimanches travaillés (2026-08-10)

Première contrainte remise en service. `DimanchesTravaillesMax` (SOFT, R9) lit désormais
l'activité via `Creneau.getCodeActiviteEffectif()` et son plafond sur le salarié
(`contraintesReglementaires.dimanchesTravaillesMaximum`), et non plus dans `SeuilsDeTolerance`.

* Seuls les **dépassements** produisent un match : un salarié dans les clous n'apparaît plus au
  `scoreBreakdown` avec un impact nul.
* Motif SC-06 `DIMANCHES_TRAVAILLES_DEPASSES` (WARNING, **non éliminatoire**) : le plafond de
  dimanches est une borne conventionnelle d'équité, pas un seuil de légalité. Un dimanche de plus
  doit être visible, pas interdit.
* `SalarieReel.contraintesOuAucune()` : repli non nul, pour que les contraintes des lots suivants
  évaluent un seuil sans imbriquer deux tests de nullité.
* `Phase13ConstraintsTest` alimente maintenant `codeActiviteId` et non plus `activite` — c'est ce
  détail qui rendait ces tests verts alors que la contrainte était morte. Deux cas ajoutés : seuil
  absent et seuil à 0, tous deux inactifs.

**Écart de score mesuré : aucun.** SC-03 reste à `0hard/-960soft`, et
`LEGAL_SOFT_DIMANCHES_TRAVAILLES_MAX` reste absent de son `scoreBreakdown` — aucun jeu d'essai ni
payload de référence ne transmet le plafond de dimanches, la contrainte demeure donc inactive
faute de seuil. La remise en service est sans effet tant que WinDev n'envoie pas le champ.

438 tests, 0 échec.

### Planning existant et créneaux figés — lot S1 (2026-08-10)

* `creneaux[].ressourceAffecteeId` : affectation existante transmise par l'appelant
* `Creneau.fige` (`@PlanningPin`) : créneau soustrait aux décisions du solveur, mais toujours
  visible des contraintes
* `ScenarioCreneauMapper.toCreneauxFiges()` : résolution et figement

Socle du lot L8 du cadrage général — ouvre SC-02, SC-04 et SC-06. **Sans effet sur SC-01 et
SC-03**, qui passent par `toCreneaux()` et ne figent rien.
Voir `20_DECISIONS_CONCEPTION_OPTAPLANNER.md` — *Créneau figé : un fait d'entrée, pas une décision*.

### Infrastructure de test

Introduction d’un **dataset de référence technique** permettant de tester l’alimentation WebDev → moteur.

Stratégie de migration :

1. évolution compatible du schéma V1
2. adaptation du DatasetBuilder
3. introduction progressive d’une structure V2

---

### Contrôles combinatoires

Premières briques implémentées :

* repos hebdomadaire
* dimanches maximum
* séquences observées

Les métriques correspondantes sont désormais calculées côté moteur.

---

### Exposition des WorkMetrics

Les métriques calculées sont désormais exposées dans l’API scénario.

---

# C. Prochains jalons

## WorkMetrics Équité

Ajout d’indicateurs d’équité :
* écart par rapport à la moyenne
* dispersion de charge

---

## Compléter le DataSet amont

Ajout progressif des éléments nécessaires à la résolution :

* groupes de besoins (`groupeBesoinId`)
* blocs journaliers (`blocJourId`)
* ordre des créneaux dans un bloc
* identification des segments de pause

Ajouts prévus :

* entrepôt des activités
* paramètres réglementaires des ressources

---

## Stabilisation du contrat d’entrée

Objectif :

Éviter le faux sentiment de couverture fonctionnelle pendant l’enrichissement progressif du dataset.

Chaque champ doit être qualifié selon son niveau réel d’exploitation.

---

# 1️⃣ Capacités partiellement implémentées

| Sujet                   | Limitation actuelle                         |
| ----------------------- | ------------------------------------------- |
| Explicabilité détaillée | lecture pédagogique du score encore absente |

Des logs de diagnostic existent déjà :

* ScoreExplanation
* WorkMetrics calculées après résolution

---

# 2️⃣ Capacités identifiées mais non implémentées

## Contraintes combinatoires avancées

| Sujet                        | Priorité |
| ---------------------------- | -------- |
| Jours consécutifs travaillés | ~~Haute~~ → ✅ implémenté Phase 10 |
| Alternance jour / nuit       | ~~Haute~~ → ✅ implémenté Phase 11 |
| Amplitude journalière        | ~~Moyenne~~ → ✅ implémenté Phase 12 |

---

## WorkMetrics futures

Les métriques futures sont documentées dans `40_WORKMETRICS.md`.

### V4 — Référentiel contractuel (§5.3)

Ces métriques expriment l'écart entre la charge planifiée et le temps contractuel de référence du salarié.

| Champ                                 | Type    | Description                                                       |
| ------------------------------------- | ------- | ----------------------------------------------------------------- |
| `deltaMinutesParRapportAuContractuel` | Decimal | Écart entre minutes travaillées et temps contractuel de référence |
| `ratioChargeContractuelle`            | Decimal | Rapport charge réelle / charge contractuelle                      |

**Pré-requis :**
- définition du temps contractuel côté métier (hors moteur)
- injection de cette information comme fait immuable dans le dataset d'entrée

**Objectif :** rendre visibles les écarts charge/contrat sans statuer sur la légalité.

---

### V5 — Dettes et coûts abstraits (§5.4)

Ces métriques représentent des coûts abstraits (non financiers) liés à la pénibilité et à la récupération obligatoire.

| Champ                    | Type    | Description                   |
| ------------------------ | ------- | ----------------------------- |
| `detteReposCompensateur` | Decimal | Volume de repos à récupérer   |
| `detteReposNuit`         | Decimal | Part liée au travail de nuit  |
| `detteReposFerie`        | Decimal | Part liée aux jours fériés    |

**Pré-requis :**
- WorkMetrics V3 complètes (séquences + équité stabilisées)
- ScoreWeights stabilisés
- Analyse métier aval définie

**Objectif :** support à la restitution RH et aide à la décision, hors moteur.

---

### V6 — Contraintes combinatoires
Refonte des contraintes combinatoires en Constraint Streams plus natifs / incrémentaux

---

## Analyse métier aval

Certaines analyses resteront hors moteur :

| Sujet                         | Statut      |
| ----------------------------- | ----------- |
| Construction SurchargeSalarie | Hors moteur |
| Aide à la décision RH         | Hors moteur |

---

## Évolution prévue de la stratégie de scoring

La stratégie de scoring évolue par paliers, en cohérence avec les WorkMetrics.
La définition fonctionnelle du scoring est dans `40_STRATEGIE_DE_SCORING.md`.

| Phase            | Contraintes                    | WorkMetrics                    | ScoreWeights         |
| ---------------- | ------------------------------ | ------------------------------ | -------------------- |
| Actuelle         | HARD stabilisées, SOFT basiques| restitution partielle          | défini, usage limité |
| Suivante         | SOFT combinatoires, équité     | V3 équité                      | maîtrisé             |
| Ultérieure       | contraintes contractuelles     | équité, contractuel, dettes    | scénarios comparatifs|

---

## Feuille de route — Interface WinDev / moteur

Suivi des phases de stabilisation du contrat d'entrée.
Le contrat détaillé champ par champ est dans `50_interface_windev_moteur_contrat_detail.md`.

| Phase | Objectif                                             | Statut      |
| ----- | ---------------------------------------------------- | ----------- |
| 0     | Gel du point de départ / socle tests                 | ✅ Terminé  |
| 1     | Transport des nouveaux champs sans exploitation      | ✅ Terminé  |
| 2     | Dataset technique de référence SC-03                 | ✅ Terminé  |
| 3     | Branchement builder (mapping domaine)                | ✅ Terminé  |
| 4     | Incompatibilités structurelles (JourFerié, Indisp.)  | ✅ Terminé  |
| 5     | Structuration des besoins et blocs journaliers       | ✅ Terminé  |
| 6     | Données nuit par salarié                             | ✅ Terminé  |
| 7     | Ouverture SC-03 côté API                             | ✅ Terminé  |
| 8     | Scoring, WorkMetrics, diagnostics enrichis           | ✅ Terminé  |
| 9     | Consolidation pipeline SC-03 et diagnostics complets | ✅ Terminé  |

---

## Intégration du solveur OptaPlanner

Le moteur de planification appelle désormais le solveur OptaPlanner en exécution réelle dans le scénario SC-01.

L’appel est effectué via la chaîne suivante :

ScenarioController  
→ PlanningRequest  
→ PlanningService  
→ SolverLauncher  
→ OptaPlanner  
→ PlanningProblem résolu

La solution retournée par le solveur est récupérée via : `solved.solution().getCreneaux()`

### Validation

Le fonctionnement du solveur et du scoring a été vérifié en exécution.
Des logs supplémentaires dans `ScenarioController` confirment :
- le score final calculé par OptaPlanner
- les affectations des créneaux aux ressources.

### Représentation des créneaux non couverts

La représentation des créneaux non affectés est implémentée
et exposée via l’API.

Voir :
- 20_DECISIONS_CONCEPTION_OPTAPLANNER.md
- 60_interface_windev_moteur_plan_documentaire.md

### Restitution du solveur

La restitution du solveur est désormais implémentée
et expose les blocs principaux du contrat API.

Voir :
- 60_interface_windev_moteur_plan_documentaire.md

---

# 3️⃣ Ordre logique recommandé pour la suite

1. amélioration de l’explicabilité du score
2. nettoyage technique OptaPlanner
3. extension WorkMetrics
4. extension du dataset

---

# 🧠 Principe de lecture

Une capacité n’est considérée comme acquise que lorsqu’elle est :

* implémentée
* testée
* observée en exécution.

Ce document doit rester **la photographie fidèle du moteur à un instant donné**.

### Socle réglementaire — lot S7.2 : nuits consécutives (2026-08-11)

`NuitsConsecutivesMax` (HARD, R3) remise en service : repli d'activité via
`getCodeActiviteEffectif()` et plafond lu sur le salarié
(`contraintesReglementaires.nuitsConsecutivesMaximum`).

C'était la contrainte la plus piégeuse du chantier : plafond global à **0** et **aucune garde**.
Réparer le seul repli d'activité aurait rendu toute deuxième nuit consécutive immédiatement HARD,
pour tout le monde, sans qu'aucune donnée d'entrée ne l'ait demandé.

* Motif SC-06 `NUITS_CONSECUTIVES_DEPASSEES` (ERROR, **éliminatoire**) : R3 borne la légalité et
  non l'équité — enchaîner trop de nuits n'est pas un arbitrage possible.
* `NuitsConsecutivesMaxConstraintsTest` : 9 cas, créés de zéro. Aucun test n'interrogeait cette
  contrainte. Ils fixent notamment ce que « consécutif » veut dire — comptage par date distincte,
  une journée intercalée ne prolonge pas une séquence de nuits, une activité hors charge
  l'interrompt.
* **Garde-fou de score permanent** : `ScenarioControllerSc03RuntimeTest` asserte désormais
  `soft = -960` en plus de `hard = 0`. La mesure d'écart n'est plus manuelle — toute variation
  des lots suivants fera échouer ce test, avec le lot en cours pour seul suspect.

**Écart de score mesuré : aucun.** SC-03 reste à `0hard/-960soft`.

448 tests, 0 échec.

### Socle réglementaire — lot S7.3 : repos après nuits (2026-08-11)

`ReposObligatoireApresNuits` (HARD, R4) remise en service : repli d'activité et repos exigé lu sur
le salarié (`contraintesReglementaires.joursReposMinimumApresNuits`).

Elle était éteinte **deux fois** — champ d'activité déprécié et seuil global nul, sa garde interne
la neutralisant par surcroît. Corriger le seul repli d'activité ne l'aurait pas réveillée.

* **Tri destructeur supprimé** : la vérification appelait `sort()` sur la liste produite par
  `ConstraintCollectors.toList`, qui appartient à OptaPlanner — la trier sur place modifiait
  l'état interne du calcul de score. Le tri n'était de surcroît pas utilisé.
* Motif SC-06 `REPOS_APRES_NUITS_INSUFFISANT` (ERROR, **éliminatoire**). R4 exige des journées
  entières de récupération : à distinguer de `REPOS_QUOTIDIEN_INSUFFISANT`, qui mesure des heures
  entre deux journées travaillées.
* `ReposObligatoireApresNuitsConstraintsTest` : 8 cas, créés de zéro. Ils fixent la borne exacte
  de la fenêtre de repos, le comportement d'une séquence de nuits interrompue, et le fait qu'une
  activité hors charge dans la fenêtre n'est pas une reprise de travail.

**Écart de score mesuré : aucun.** SC-03 reste à `0hard/-960soft`, garde-fou asserté.

457 tests, 0 échec.

### Socle réglementaire — lot S7.4 : repos hebdomadaire glissant (2026-08-11)

`ReposHebdomadaireGlissant` (HARD, volet **conventionnel** de R7) remise en service : repli
d'activité et paire de seuils lue sur le salarié.

* **La paire est indissociable.** `reposHebdomadaireFenetreJours` et
  `reposHebdomadaireJoursOffMinimum` ne décrivent une règle qu'ensemble : fenêtre seule, minimum
  seul, ou paire dont l'une des valeurs vaut 0 laissent la contrainte inactive, y compris sur une
  semaine travaillée sept jours sur sept. C'est le seul cas du chantier où deux champs se
  conditionnent ; le mapper émet un WARN sur une paire à moitié renseignée.
* Motif SC-06 `REPOS_HEBDOMADAIRE_GLISSANT_INSUFFISANT` (ERROR, **éliminatoire**).
* `ReposHebdomadaireGlissantConstraintsTest` : 9 cas, créés de zéro.
* Distinction posée avec `ReposHebdomadaireMin`, plancher légal non paramétrable traité au lot
  S7.5 : deux contraintes, deux clés de pénalité, deux motifs.

**Écart de score mesuré : aucun.** SC-03 reste à `0hard/-960soft`, garde-fou asserté.

467 tests, 0 échec.

### Socle réglementaire — lot S7.5 : plancher de repos hebdomadaire (2026-08-11)

`ReposHebdomadaireMin` (HARD, plancher légal de R7) remise en service. Une seule modification
suffisait : le repli d'activité. Cette contrainte n'a **aucun seuil individuel** — un plancher
légal ne se négocie pas au contrat — et le repli seul l'active donc pour tout le monde.

* Motif SC-06 `SEMAINE_SANS_JOUR_DE_REPOS` (ERROR, **éliminatoire**), distinct de
  `REPOS_HEBDOMADAIRE_GLISSANT_INSUFFISANT` : deux volets de R7, deux clés, deux motifs.
* `ReposHebdomadaireMinConstraintsTest` : 7 cas, créés de zéro. Une violation par salarié et non
  par fenêtre ; une activité hors charge vaut jour off.

**Écart de score mesuré : aucun — contrairement à la prévision.** Ce lot était annoncé comme l'un
des deux susceptibles de déplacer les scores, puisqu'il s'active sans donnée d'entrée. La mesure
le dément : **aucun jeu d'essai ne fait travailler sept jours d'affilée**. La contrainte est
active mais ne rencontre aucune situation à sanctionner — les scénarios existants sont conformes
au plancher légal. SC-03 reste à `0hard/-960soft`.

Reste **S7.6 comme unique lot susceptible de faire bouger les scores**, pour une raison
différente : sa maille de calcul est fausse, pas seulement son repli d'activité.

475 tests, 0 échec.

### Socle réglementaire — lot S7.6 : durée journalière (2026-08-11)

`DureeMaximaleLegaleParSalarie` remise en service, **et corrigée**. C'est le seul défaut du
chantier qui n'était pas un simple défaut de branchement : son `groupBy` agrégeait sur tout
l'horizon et comparait ce cumul à une constante de 780 minutes, qui est une valeur journalière.
Elle mesurait une période et la comparait à un jour.

* Agrégation par `(salarié, date)`, plafond lu sur le salarié via `heuresMaximumParJour`,
  constante globale supprimée.
* Clé renommée : `LEGAL_HARD_DUREE_MAX_LEGALE_PAR_PERIODE` → `LEGAL_HARD_DUREE_MAX_PAR_JOUR`,
  unité `JOUR` → `MINUTE_PONDEREE`. Sans risque : la contrainte étant dormante, cette clé n'a
  jamais été émise.
* Le plancher **physique** reste inconditionnel : `LimitePhysique` interdit toujours plus de 24 h
  cumulées par jour et plus de 12 h par créneau, sans dépendre d'aucun seuil transmis.
* Motif SC-06 `DUREE_JOURNALIERE_DEPASSEE` (ERROR, **éliminatoire**), distinct de
  `AMPLITUDE_DEPASSEE` qui reste un signalement : durée travaillée et amplitude ne mesurent pas
  la même chose.
* `DureeMaximaleParJourConstraintsTest` : 9 cas, créés de zéro, dont quatre pour interdire le
  retour du défaut de maille.

**Écart de contrat refermé** : la notice SC-06 et le contrat de sortie signalaient que
`heuresJour` était mesuré et son plafond restitué alors qu'aucune contrainte ne l'appliquait. Les
trois mesures du bloc `impacts[]` sont désormais adossées à une contrainte ; la documentation a
été corrigée.

**Écart de score mesuré : aucun.** SC-03 reste à `0hard/-960soft`, garde-fou asserté.

**Les six contraintes dormantes sont remises en service.** 486 tests, 0 échec.

### Socle réglementaire — lot S7.7a : lecture littérale du zéro (2026-08-11)

Correction de la décision **D2** posée au lot S7.0, sur arbitrage. `0` n'est plus lu comme une
désactivation : **seule l'absence désactive**, le zéro garde son sens arithmétique.

Ce sont les données qui ont tranché : le jeu SC-03 transmet `nuitsMaximumParSemaine: 0` pour
SAL-2001 et `3` pour SAL-2002. La lecture D2 aurait autorisé le premier à travailler toutes les
nuits — l'exact contraire de l'intention.

* `seuilActif()` devient `borneRenseignee()` — renseigné et positif ou nul.
* `largeurRenseignee()` la complète pour `reposHebdomadaireFenetreJours` : une fenêtre est une
  **taille**, pas une borne, et 0 jour ne décrit aucune règle. Seule exception du contrat.
* Le WARN du mapper porte désormais sur les **valeurs négatives** — 0 n'est plus une anomalie.
* **Bug corrigé dans `NuitsConsecutivesMax`** : le dépassement n'était testé qu'en prolongeant
  une séquence, jamais en l'ouvrant. Avec un plafond de 0, deux nuits d'affilée violaient mais
  une nuit isolée passait. L'incohérence était inatteignable tant que 0 n'était pas recevable.

**Écart de score mesuré : aucun.** SC-03 reste à `0hard/-960soft` — aucune des cinq contraintes
concernées ne rencontre de seuil à 0 dans les jeux actuels.

490 tests, 0 échec.
