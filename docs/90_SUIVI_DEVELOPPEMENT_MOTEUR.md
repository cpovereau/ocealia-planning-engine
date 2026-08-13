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
  réintégration côté appelant, voir `92_CADRAGE_DONNEES_AMONT_SCENARIOS.md` §6.7
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
> Constat et journal des lots : `92_CADRAGE_SOCLE_REGLEMENTAIRE.md`.

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
| Durée hebdomadaire minimale (sous-emploi) | SOFT | ✅ lot S7.7 | –                        | Non                    | Non                                       | Oui (si déficit) | 92_cadrage_socle_reglementaire |
| Nuits maximum par semaine             | SOFT | ✅ lot S7.7  | heuresNuit                    | Oui                    | Non                                       | Oui (si dépassement) | 92_cadrage_socle_reglementaire |
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
  Voir `92_CADRAGE_SCENARIO_SC-06.md` §4.6.
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
contrat série 50 : `50_SCENARIO_CONTRACT.md` §4, `50_SCENARIO_RESPONSE_CONTRACT.md` §6, OpenAPI et
schémas JSON. Notice d'intégration : `Windev_part/SC-06/sc_06_notice_integration.md`.

**Les lots S1 à S6 de SC-06 sont livrés.** Voir `92_CADRAGE_SCENARIO_SC-06.md`.

Le lot S7 initialement prévu — activer les trois dernières contraintes individuelles — s'est
révélé être le sommet d'un écart plus large : **six contraintes enregistrées ne se déclenchent
jamais** pour un client conforme au contrat. Ce chantier fait l'objet de son propre cadrage et de
son propre découpage : `92_CADRAGE_SOCLE_REGLEMENTAIRE.md`.

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
* Contrat : `50_SCENARIO_CONTRACT.md` §3.7, schéma JSON et OpenAPI.

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

# C. Reste à faire — backlog consolidé

Trois lots restent ouverts à la clôture du chantier S8. Ils portent le **numéro de rang** du
backlog dressé au terme de ce chantier — les rangs 1 à 7 sont livrés, lots S8.0 à S8.5 — pour que
le journal des lots et ce tableau de bord se lisent ensemble.

**Cette section est la seule liste ouverte du projet.** Ce qui n'y figure pas est soit livré (§ A),
soit explicitement hors moteur (§ Analyse métier aval).

| Rang | Sujet | Nature du blocage | Qui tranche |
|---|---|---|---|
| **8** | Champs au contrat sans effet : `capaciteCible`, structuration des besoins | arbitrage — activer ou retirer — **non traitable avant le 25/08/2026** | Métier + moteur |
| **9** | SC-04 et SC-05 — deux scénarios annoncés, jamais écrits | cadrage — aucun n'a de contrat d'entrée. **SC-02 est sorti de ce rang** : cadré le 11/08, lots S0 à S4 livrés, inscrit au contrat série 50 le 13/08 | Métier |
| **10** | Contraintes personnelles : lieux, activités, préférences, annualisation | échanges avec la Production | Production |
| **12** | Fermer les blocs restés délibérément tolérants aux champs inconnus | arbitrage — changement de comportement visible par l'appelant | Métier + WinDev |

~~**Rang 11** — blocs annoncés stricts qui ignorent en silence.~~ ✅ **Traité le 2026-08-13.**

**Rang 12, ce qu'il reste du 11.** Six blocs déclarent ouvertement `ignoreUnknown = true` et
laissent donc encore passer un champ inconnu : l'enveloppe de requête des quatre scénarios,
`dataSet`, `salaries[]`, `postesVirtuels[]` et les paramètres SC-03. **Ils ne mentent pas** — c'est
ce qui les distinguait du rang 11 — mais ils gardent une zone de silence. Les fermer demande de
nommer ce que chacun tolère, et se voit immédiatement côté appelant : c'est un arbitrage, pas un
correctif.

Le cas le plus tentant est `Sc03ScenarioParametersDTO`, symétrique de celui de SC-02 qui a motivé
le rang 11 : un intégrateur qui envoie à SC-03 un paramètre inexistant reçoit encore un 200.

Deux chantiers plus anciens restent ouverts **sans dépendre d'un arbitrage** : les WorkMetrics
d'équité et l'explicabilité pédagogique du score (§ 1️⃣). Réalisables à tout moment ; l'équité est
de surcroît un prérequis de SC-05.

📄 **L'équité est cadrée** — arbitrages métier rendus le 13/08/2026, découpage en sept lots L0 à
L6 : `92_CADRAGE_WORKMETRICS_EQUITE.md`. Le lot **L0 est immédiatement actionnable** et ne porte
pas sur l'équité : il rend le **RHD inviolable**. Le cadrage a mis au jour que travailler le repos
dominical de quelqu'un est aujourd'hui pénalisé en SOFT, sans distinguer RH de RHD, et **coûte
zéro** dès que l'activité ne porte pas `genereDetteRepos`.

⚠️ Ce cadrage **corrige `40_WORKMETRICS.md` §5.2** : `ecartChargeAvecMoyenne` compare à la moyenne
du groupe, quand l'arbitrage retient le **contrat de chacun**. À reprendre au lot qui livre la
mesure.

La **stabilisation du contrat d'entrée**, qui figurait ici comme jalon, est close : phases 1 à 10C
terminées, voir `92_SUIVI_STABILISATION_CONTRAT_ENTREE.md`.

### Rangs 1 à 7 — livrés

Rappel de traçabilité, pour que la numérotation ci-dessus se comprenne sans remonter au journal.

| Rang | Sujet | Lot |
|---|---|---|
| 1 | Chevauchement de créneaux invisible de part et d'autre de minuit | S8.3 |
| 2 | Règle d'activation d'une borne réglementaire non uniforme (5 contraintes sur 12) | S8.3 |
| 3 | Filtre d'activité du poste virtuel en SC-06 | S8.3 |
| 4 | `joursFeries` déclaré au contrat mais ignoré par la contrainte HARD | S8.3 |
| 5 | `alerts` figé à la liste vide en SC-03 et SC-06 | S8.4 |
| 6 | `ignoredCreneaux` comptait sans dire **quels** créneaux | S8.4 |
| 7 | Rattrapage documentaire : nommage, liens morts, index | S8.5 |

---

## Rang 8 — les champs annoncés qui ne produisent rien

Un champ transporté, mappé, puis lu par personne est **pire qu'un champ absent** : l'appelant le
renseigne et croit l'avoir dit. Le principe du projet — *un vide ne suppose jamais que la chose est
possible* — impose de les activer ou de les retirer.

| Champ | Trajet effectif | Conséquence observable |
|---|---|---|
| `postesVirtuels[].capaciteCible` | DTO → `ScenarioResourceMapper` → `PosteVirtuel.capaciteCible` | Aucune contrainte ne le lit : **un poste virtuel absorbe un nombre illimité de créneaux**. Une capacité déclarée à 2 n'empêche pas 40 affectations. |
| `creneaux[].groupeBesoinId` | DTO → `Creneau` | Aucun lecteur — deux créneaux d'un même besoin sont indépendants pour le solveur. |
| `creneaux[].blocJourId` | DTO → `Creneau` | Aucun lecteur — un bloc journalier peut être éclaté entre plusieurs salariés. |
| `creneaux[].ordreDansBloc` | DTO → `Creneau` | Aucun lecteur — il n'ordonne rien. |

À l'inverse, `estSegmentDePause` **est** lu : trois contraintes légales excluent les segments de
pause de leurs décomptes. La famille n'est donc pas morte en bloc, ce qui rend l'écart d'autant
moins lisible de l'extérieur.

**Décision attendue** : pour chaque champ, activer avec la contrainte qui va avec, ou retirer du
contrat. Les trois champs de structuration ne s'activent utilement qu'**ensemble** — un bloc
journalier sans ordre ni groupe n'exprime rien.

⏳ **Disponibilité du décideur** : le Métier est absent jusqu'au **25/08/2026**. Ce rang est donc
gelé jusqu'à cette date — ce n'est pas un blocage technique, et rien d'autre n'en dépend.

**Coût si activation** : `capaciteCible` est une contrainte de comptage isolée, de portée faible.
La structuration des besoins introduit une **contrainte de cohésion** — même bloc, même ressource —
qui change la nature du problème posé au solveur et demande sa propre évaluation de scoring.

---

## Rang 9 — les trois scénarios annoncés et jamais écrits

`50_SCENARIO_CONTRACT.md` décrit six scénarios. Trois existent (SC-01, SC-03, SC-06), trois n'ont
qu'une intention métier : **aucun n'a de contrat d'entrée, d'endpoint, ni de jeu d'essai**.

| Scénario | Intention | Ce qui existe déjà | Ce qui manque |
|---|---|---|---|
| ~~**SC-02** — remplacement d'un absent~~ | ~~assurer la continuité en perturbant le moins possible l'existant~~ | ✅ **Clos le 2026-08-13** — six lots S0 à S5, inscrit au contrat série 50, accessible par les deux canaux | — |
| **SC-04** — optimisation globale d'un planning existant | améliorer sans reconstruire | figement, WorkMetrics | historique des compteurs, degrés de liberté, indicateurs comparatifs |
| **SC-05** — arbitrage entre deux salariés | répartir équitablement un périmètre commun | WorkMetrics de charge | objectif d'arbitrage, historique de charge, seuils comparatifs, WorkMetrics d'équité |

`92_CADRAGE_DONNEES_AMONT_SCENARIOS.md` §7 le résume : trois familles de données apparaissent dès
SC-02 et ne disparaissent plus — le **planning existant**, le **contrat salarié** et le **seuil de
surcharge**. Les trois sont désormais câblées : le planning existant est épinglé par SC-02 comme il
l'était par SC-06, et le seuil de surcharge a ses deux contraintes depuis le lot S3. Le contrat
salarié reste **transporté sans qu'aucune contrainte ne le lise**.

SC-04 dépend d'un historique de compteurs qui n'existe pas ; SC-05, de WorkMetrics d'équité non
implémentées. **Aucun des deux n'est actionnable aujourd'hui.**

📄 **SC-02 est cadré et livré à 5/6** — arbitrages métier rendus le 11/08/2026, découpage en six
lots S0 à S5, **S0 à S4 livrés**, S5 (canal FileAdapter) restant :
`92_CADRAGE_SCENARIO_SC-02.md`.

---

## Rang 10 — le lot des contraintes personnelles

En attente des échanges avec la Production. Quatre sujets de même nature : des **restrictions
portées par la personne**, que WinDev n'alimente pas encore.

| Sujet | État constaté |
|---|---|
| `sitesAutorises` / lieux | transporté (alias `lieuxAutorises`), aucune contrainte ne le lit |
| `activitesCompatibles` | TOLÉRÉ au contrat — arbitré **HARD sur le salarié seul** ; un poste virtuel n'y est pas soumis, il existe pour combler un besoin |
| Préférences | non transportées |
| Annualisation | `contrat.salarieAnnualise` transporté, aucun lecteur |

⚠️ **Risque à porter dans ce lot** (`92_SUIVI_STABILISATION_CONTRAT_ENTREE.md`, cas B) :
l'appariement d'activité doit reposer **exclusivement sur `codeActiviteId`**, jamais sur un libellé.
Comme compteur de diagnostic, un libellé produit un faux positif silencieux ; **en HARD, il rend
tout créneau inaffectable** — le moteur ne rendrait plus aucune solution.

---

## Correctifs identifiés hors rang

Écarts constatés dans le code, sans arbitrage à demander : ils se corrigent quand leur sujet est
abordé.

| Écart | Constat | Quand le traiter |
|---|---|---|
| ~~`IndisponibiliteSalarie` ignore le passage de minuit~~ | ~~La contrainte compare `creneau.getDate()` aux bornes de l'absence.~~ | ✅ **Corrigé au lot S0 de SC-02** — le défaut avait un second lecteur, le filtre d'éligibilité de SC-06 |

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
Le contrat détaillé champ par champ est dans `50_INTERFACE_WINDEV_MOTEUR_CONTRAT_DETAIL.md`.

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
| 10A   | Incohérences internes et fallbacks silencieux        | ✅ Terminé  |
| 10B   | Réduction de `@JsonIgnoreProperties`                  | ✅ Terminé  |
| 10C   | Nettoyage DTO final                                  | ✅ Terminé  |

Chantier clos. Le détail phase par phase est dans `92_SUIVI_STABILISATION_CONTRAT_ENTREE.md`.

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
- 50_SCENARIO_RESPONSE_CONTRACT.md

### Restitution du solveur

La restitution du solveur est désormais implémentée
et expose les blocs principaux du contrat API.

Voir :
- 50_SCENARIO_RESPONSE_CONTRACT.md
- 50_INTERFACE_WINDEV_MOTEUR_CONTRAT.md

---

# 3️⃣ Ordre logique recommandé pour la suite

Ordonné par **dépendance**, pas par valeur métier. Les rangs renvoient au backlog du § C.

> **SC-02 est clos** (six lots, 2026-08-11 → 2026-08-13) et sort de cette liste.
> **Rang 11** traité le 2026-08-13 ; ce qu'il en reste est devenu le rang 12, qui demande un
> arbitrage.

1. **Rang 12** — fermer les blocs restés tolérants. À poser à l'appelant : c'est visible côté
   WinDev.
2. **Rang 8** — trancher les champs sans effet. Indépendant du reste, et il retire du contrat des
   promesses que personne ne tient. SC-02 en a rendu un plus visible : `capaciteCible` ne borne pas
   le volume qu'on gare sur un poste virtuel.
3. **Équité, lots L3 à L6** — la calibration des coefficients, puis leur effet sur la sélection,
   puis SC-05. **L0 à L2 sont livrés** : voir `92_CADRAGE_WORKMETRICS_EQUITE.md` §8.
5. **Rang 10** — dès que la Production a rendu ses arbitrages.
6. **SC-04** — le dernier, conditionné à un historique des compteurs qui n'existe pas.

L'explicabilité pédagogique du score (§ 1️⃣) et le nettoyage technique OptaPlanner restent
souhaitables mais ne conditionnent rien : ils s'intercalent où ils veulent.

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

### Socle réglementaire — lot S7.7b : les deux contraintes manquantes (2026-08-11)

`heuresMinimumParSemaine` et `nuitsMaximumParSemaine` étaient transportées depuis la Phase 1 sans
qu'aucune contrainte ne les lise. C'est fait.

* **`HeuresMinimumParSemaine`** (SOFT, 50 par minute manquante) — sous-emploi hebdomadaire, en
  **deux volets**. Seules les semaines complètes de l'horizon sont jugées : sur une semaine
  tronquée, un minimum signalerait un déficit qui n'existe pas.
* **`NuitsMaximumParSemaine`** (SOFT, 5 000 par nuit excédentaire) — un **volume** hebdomadaire,
  à distinguer de `NuitsConsecutivesMax` qui borne un **enchaînement**.
* Motif SC-06 `NUITS_HEBDOMADAIRES_DEPASSEES` (ERROR, éliminatoire). Aucun motif pour le
  sous-emploi : un salarié en déficit est un **bon** candidat, et le delta lui serait toujours
  favorable.

**Le premier jet produisait l'inverse de l'effet voulu.** Regroupé sur les seuls créneaux
existants, il a conduit le solveur à confier les six créneaux de SC-03 au poste virtuel : un
salarié sans créneau ne produisait aucun tuple, donc aucune pénalité, tandis que lui en confier
un seul déclenchait le déficit entier. Le second volet
(`LEGAL_SOFT_SEMAINE_SANS_AFFECTATION`) rétablit l'ordre des coûts en pénalisant les semaines
complètes sans aucune affectation. Un test garde explicitement cet ordre.

**Écart de score : SC-03 passe de `0hard/-960soft` à `0hard/-66960soft`.** Seule variation de
tout le chantier, et elle est voulue : deux salariés à 35 h hebdomadaires pour 48 h de travail
disponible, soit 11 h de déficit chacun — inévitable, et désormais visible. Le garde-fou de score
est mis à jour, complété d'une assertion sur les affectations : le poste virtuel doit rester à
0 h.

510 tests, 0 échec.

### Socle réglementaire — lot S7.8 : nettoyage, et clôture du chantier S7 (2026-08-11)

**Aucun écart de score — mesuré.** 519 tests, 0 échec (510 avant). Le lot ne modifie le
comportement d'aucune contrainte.

* **Deux contraintes mortes supprimées** — `CreneauJourFerie` et `CreneauDeNuit`, enregistrées
  dans aucun `ConstraintProvider` depuis leur remplacement par `PenibilitesLegalesMinutes`. Les
  réenregistrer **doublerait** le comptage des pénibilités, et `CreneauDeNuit` pénalisait la durée
  entière du créneau là où le breakdown mesure les seules minutes de nuit réelles.
* **Cinq seuils orphelins retirés** de `SeuilsDeTolerance` — jamais alimentés, donc à 0, donc
  neutralisants pour les contraintes qui les lisaient. Rapatriés dans
  `ContraintesReglementairesSalarie` par les lots S7.0 à S7.7. Zéro appelant vérifié avant retrait.
* **Règle de repli d'activité unifiée** — `domain/creneau/CodeActivite.java` porte la règle unique
  (`codeActiviteId` prioritaire, repli sur `activite`, `null` si aucun). `Creneau` et
  `CreneauInputDTO` y délèguent via `getCodeActiviteEffectif()`. **Onze** sites ramenés à
  l'accesseur, dont deux hors contraintes : `WorkMetricsCalculator` — donc la réponse API — et les
  services de préparation SC-01 et SC-03.
* **Garde anti-duplication** — `CodeActiviteTest.Unicite` parcourt `src/main/java` et échoue si
  l'expression réapparaît en ligne. Sans liste d'exemptions.

**Deux dormances d'une autre famille, repérées en clôture.** Elles relèvent d'un lot distinct
(voir `92_CADRAGE_SOCLE_REGLEMENTAIRE.md` §6.1) :

1. **La valorisation du jour férié ne fonctionne pas.** `RegulatoryParameters.neutre()` porte une
   liste `joursFeries` vide et les trois services de préparation l'utilisent : `minutesFerie` vaut
   0 pour tout client, la pénalité férié ne se déclenche jamais et `heuresJourFerie` vaut 0.0 dans
   toutes les réponses. L'**interdiction** (`JourFerieRefuse`) fonctionne, elle, en lisant le champ
   `isJourFerie` du contrat — que le schéma qualifie pourtant d'« indicatif, non réglementaire ».
2. **`DetteReposSurReposHebdomadaire` est dormante** — elle filtre sur `qualificationJour` à
   `RH`/`RHD`, valeur qu'aucun mapper ne produit (`OUVRE` en dur). Septième contrainte muette, de
   même nature que les six du chantier.

**Bilan du chantier S7** — six contraintes dormantes remises en service, deux contraintes
manquantes écrites, seuils portés au salarié, code mort et règle dupliquée supprimés. Une seule
variation de score sur l'ensemble : SC-03 de `0hard/-960soft` à `0hard/-66960soft` au lot S7.7b
(sous-emploi hebdomadaire), voulue et gardée par assertion. 413 tests au départ, 519 à l'arrivée.

### Socle réglementaire — lot S7.9a : valorisation du jour férié (2026-08-11)

**Écart de score : SC-03 passe de `0hard/-67440soft` (contre `-66960` avant).** Voulu et mesuré.
529 tests, 0 échec (519 avant).

`TimeBreakdownCalculator` interroge `RegulatoryParameters.estJourFerie(date)`, et les trois
services de préparation construisaient `RegulatoryParameters.neutre()` — calendrier **vide**.
Aucune minute n'a donc jamais été comptée comme fériée depuis l'origine : ni pénalité, ni
`workMetrics.heuresJourFerie`, resté à 0.0 dans toutes les réponses. L'interdiction
(`JourFerieRefuse`, HARD) fonctionnait, elle, ce qui rendait le défaut plausible.

* `CalendrierJoursFeries.declaresParLesCreneaux(...)` reconstitue le calendrier à partir du
  drapeau `isJourFerie` — **SC-03** et **SC-06**, qui ne transmettent aucun calendrier. Un seul
  créneau marqué qualifie la journée entière, pour tous les salariés : le férié est une propriété
  de la date.
* **SC-01** utilise `scenarioParameters.holidayDates`, jusqu'ici employé pour ne pas générer de
  créneau ces jours-là seulement.
* `RegulatoryParameters.avecJoursFeries(...)` remplace `neutre()` dans les trois services. La
  plage de nuit reste 22:00–06:00.
* **Contrat inchangé** — les deux sources existaient déjà. La documentation, elle, était fausse :
  `isJourFerie` y était qualifié d'« indicatif, non réglementaire » alors qu'il portait seul la
  règle opérante. Corrigé dans le schéma JSON, l'OpenAPI et `40_WORKMETRICS.md`.

**Limite assumée** : un créneau traversant minuit n'est rattaché qu'à sa date de début. La lever,
comme rendre la plage de nuit configurable, suppose d'ouvrir `planningContext.regulatoryParameters`
au contrat — emplacement spécifié dans l'OpenAPI, jamais implémenté.

Le garde-fou SC-03 assied désormais le score **et** la métrique (`heuresJourFerieTotales` à 8.0,
répartition entre salariés). L'absence de toute assertion sur cette métrique est ce qui a permis
au défaut de survivre.

Reste S7.9b : le repos hebdomadaire (`DetteReposSurReposHebdomadaire`, toujours muette).

### Socle réglementaire — lot S7.9b : repos hebdomadaire nominatif (2026-08-11)

**Aucun écart de score — mesuré.** 554 tests, 0 échec (529 avant). Les jeux d'essai ne déclarent
ni code de repos ni activité générant une dette : la contrainte est en service et reste muette
faute de situation à juger.

`DetteReposSurReposHebdomadaire` exigeait qu'un créneau soit **lui-même** qualifié `RH`/`RHD`
*et* que son activité compte dans la charge — deux conditions incompatibles, un repos n'étant pas
du travail — en lisant `Creneau.qualificationJour`, champ qu'aucun mapper n'alimente. Septième
contrainte muette du chantier.

* **Le client déclare ses identifiants** — `dataSet.referentiels.codeActiviteReposHebdomadaire`
  et `codeActiviteReposHebdomadaireDimanche`. L'identifiant change d'un client à l'autre ; le
  moteur ne connaît aucun code en dur et `RH` n'est pas un mot réservé.
* **Un repos est un fait, pas un créneau** (`domain/repos/ReposHebdomadaire`). Décision imposée
  par les chiffres : un repos couvre 00:00–23:59, soit 1 439 minutes, et violerait à lui seul
  trois contraintes HARD de `LimitePhysique` — 719 points sur la seule durée maximale de créneau.
  Réduit à une date et une nature, il n'a plus d'horaires. Corollaire : aucun filtre à ajouter
  aux contraintes physiques, et plus de question de figeage en SC-03, un fait n'étant pas une
  variable de décision.
* **Repli par salarié et par semaine** — une semaine sans marqueur retombe sur samedi/dimanche
  même si le salarié en déclare ailleurs. Une semaine oubliée ne devient jamais silencieusement
  travaillable.
* **Marqueurs restitués** dans `planning`, et là seulement : l'appelant recharge la réponse pour
  réafficher son planning. Ils sont exclus des diagnostics, des `workMetrics` et du résumé.
  SC-06 fait exception — sa réponse ne contient que le besoin.

**Bilan du chantier S7** — sept contraintes dormantes remises en service, deux contraintes
manquantes écrites, seuils portés au salarié, valorisation du férié rendue opérante, code mort et
règle dupliquée supprimés. Deux variations de score sur l'ensemble, toutes deux voulues et
gardées par assertion : SC-03 de `-960` à `-66960` (sous-emploi, S7.7b) puis à `-67440` (férié,
S7.9a). 413 tests au départ, 554 à l'arrivée.

### Socle réglementaire — lot S7.9c : les deux indicateurs de repos (2026-08-11)

**Aucun écart de score.** 557 tests, 0 échec (554 avant). Seules les `workMetrics` changent.

`heuresReposHebdoTravaille` comptait samedi **et** dimanche calendaires, sans consulter le
calendrier de repos : la métrique et `DetteReposSurReposHebdomadaire` annonçaient la même règle
sans appliquer la même.

Arbitrage rendu : les deux indicateurs ne doivent pas avoir la même maille.

* `heuresReposHebdoTravaille` → **dimanche calendaire uniquement**. Un indicateur d'observation ne
  doit pas dépendre d'une déclaration qui peut être absente ou partielle, sous peine de cesser
  d'être comparable entre salariés et entre clients. Même maille que `nbDimanchesTravailles` —
  l'un compte les heures, l'autre les jours. **Le samedi en sort**, où il entrait depuis
  l'origine ; aucun jeu d'essai n'ayant de créneau le samedi, aucune valeur restituée ne bouge.
* `nbCreneauxReposHebdoDetteRepos` → **calendrier de repos du salarié**. Contrepartie observée de
  la contrainte : les deux lisent le même calendrier, sans quoi la métrique annoncerait une règle
  que le score n'applique pas. Ce repos peut tomber n'importe quel jour.

La divergence entre les deux est voulue : « combien d'heures le dimanche » et « combien de fois ce
salarié a-t-il travaillé son repos » sont deux questions différentes.

### Cadre réglementaire — lot S8.0 : `planningContext.regulatoryParameters` (2026-08-11)

**Aucun écart de score — mesuré.** 568 tests, 0 échec (557 avant). Le bloc est facultatif et
absent de tous les jeux d'essai : le comportement du lot S7.9a est intégralement conservé.

Bloc annoncé depuis l'origine par la spécification d'interface — « présent dans les documents de
cadrage et dans le domaine, mais pas dans `PlanningContextDTO` en V1 […] valeurs par défaut […]
à intégrer en Phase 3+ » — et jamais implémenté. Ces valeurs par défaut portaient un nom dans le
code : `neutre()`, plage de nuit figée à 22:00–06:00 et calendrier de jours fériés **vide**.

* **`heureDebutNuit` / `heureFinNuit`** — la plage de nuit cesse d'être figée. Déclarée si les
  **deux** bornes le sont ; une borne seule est ignorée et tracée, mélanger une borne déclarée
  avec une borne par défaut produirait un intervalle que personne n'a voulu.
* **`joursFeries`** — source de vérité dès qu'elle est **présente**, fût-elle vide : une liste
  vide dit « aucun jour férié ». C'est l'absence du champ qui laisse le moteur déduire depuis
  `holidayDates` (SC-01) ou `isJourFerie` (SC-03, SC-06). Aucune fusion : une divergence est
  tracée, jamais absorbée.
* **Un créneau traversant minuit** peut enfin être qualifié correctement. La limite n'était pas
  dans le calcul — `TimeBreakdownCalculator` interroge séparément les deux jours civils — mais
  dans la déduction, le drapeau étant porté par le créneau et non par le jour.
* **Point unique de résolution** (`ScenarioRegulatoryParametersMapper`), partagé par les trois
  scénarios : la précédence ne peut pas diverger de l'un à l'autre.

**Reste ouvert** : `heureDebutNuit` / `heureFinNuit` **portés par le salarié**, transportés et lus
par personne, alors que `SalarieReel` expose déjà `heureDebutNuitEffective(fallback)`. Les
brancher rendrait la pénibilité d'un créneau dépendante de qui l'exécute — défendable, mais cela
change la nature du score et demande un arbitrage.

### Cadre réglementaire — lot S8.1 : la plage de nuit du salarié (2026-08-11)

**Aucun écart de score — mesuré.** 574 tests, 0 échec (568 avant). `SAL-2002` déclare 22:00–06:00
dans le jeu de référence SC-03, soit exactement la plage globale : la bascule est exercée sans
rien déplacer.

On distingue les salariés **veilleurs** et ceux qui font du travail de nuit **occasionnel**, et
les horaires de nuit ne sont pas les mêmes. `SalarieReel` portait déjà
`heureDebutNuitEffective(fallback)` — écrite, jamais appelée. `TimeBreakdownCalculator`
l'interroge désormais : plage du salarié affecté s'il en déclare une, cadre global sinon. Un
créneau non affecté, ou confié à un poste virtuel, relève du cadre global.

**Distorsion mesurée, non corrigée.** Sur un créneau 21:00–07:00 en `EXPLOITATION` : le veilleur
(plage 21:00–07:00) coûte 1 800 points de pénibilité, le salarié non-nuit (plage globale) 1 440
plus 1 point d'inadéquation. **Le solveur préfère le salarié inadapté, de 359 points.**

Ce n'est pas une calibration à 1 mais un poids **absent** : `NuitSalarieNonNuit` est la seule
contrainte métier à écrire `penalize(HardSoftScore.ONE_SOFT)` sans passer par
`context.getPenalites()`, là où `AffectationPosteVirtuel` vaut 500, `nonAffectation` 2 000 et
`detteRepos` 5 000. Un test mesure l'écart plutôt que de le valider. **Arbitrage attendu sur la
valeur du poids.**

### Cadre réglementaire — lot S8.2 : le poids de `NuitSalarieNonNuit` (2026-08-11)

**Aucun écart de score — mesuré.** 575 tests, 0 échec (574 avant). Le créneau de nuit de SC-03 va
à `SAL-2002`, travailleur de nuit occasionnel : la contrainte ne se déclenche pas.

`NuitSalarieNonNuit` écrivait `penalize(HardSoftScore.ONE_SOFT)`, seule du paquet métier à ne pas
passer par `context.getPenalites()`. Ce n'était pas une calibration à 1 mais un **poids absent** —
la clé déclarait pourtant son unité, `OCCURRENCE`.

Le lot S8.1 l'a rendue déterminante : un veilleur déclarant une plage de nuit plus large produit
mécaniquement plus de minutes pénibles qu'un salarié non-nuit sur le même créneau, et le solveur
préférait donc le salarié inadapté de 359 points.

**Poids retenu : 2 000**, aligné sur `nonAffectation` — confier une nuit à quelqu'un qui n'en fait
pas est du même ordre de gravité que laisser un créneau découvert. Sur un créneau 21:00–07:00 en
`EXPLOITATION`, le veilleur coûte 1 800 contre 3 440 au salarié non-nuit : l'ordre est rétabli de
1 640 points, et l'écart de plage réaliste (360 points) est dominé d'un facteur supérieur à cinq.

Deux tests : l'un verrouille l'ordre des coûts, l'autre énonce la **borne** — une plage
individuelle couvrant les 24 heures dépasserait le contrepoids. Le poids couvre l'écart réaliste,
pas l'absurde, et c'est écrit.

Le constructeur historique de `Penalites` à treize arguments est conservé et applique le poids par
défaut.

### Correctifs — lot S8.3 : quatre calculs faux, réparés ensemble (2026-08-11)

**Aucun écart de score — mesuré.** 596 tests, 0 échec (575 avant, soit 21 tests ajoutés). Le jeu
de référence SC-03 reste à `-67440` soft. Les quatre défauts partagent cette propriété : ils sont
invisibles sur le jeu de référence, et c'est pourquoi ils avaient survécu.

**1. Chevauchement à minuit (HARD).** `pasDeChevauchement` appariait les créneaux de même date
puis comparait des `LocalTime` nus. Aveugle des deux côtés : dates différentes (une nuit du 3 mars
22:00–06:00 et un créneau du 4 mars 02:00–10:00 n'étaient jamais appariés) et même date
(22:00–06:00 contre 23:00–23:30 : `23:00.isBefore(06:00)` est faux). Un salarié pouvait être à
deux endroits à la fois sans point HARD. L'appariement porte désormais sur le recouvrement des
intervalles absolus via `Joiners.overlapping`, la comparaison restant stricte. La convention
« `date` = jour de début » vit maintenant dans `Creneau.getDebutEffectif()/getFinEffectif()`, au
lieu d'être réécrite en trois endroits.

**2. Règle d'activation non uniforme.** `borneRenseignee` s'annonce source unique ; cinq
contraintes sur douze testaient `!= null` en direct. Sur une **valeur négative**, les sept
conformes s'abstiennent, les cinq autres s'activaient avec un seuil négatif — dépassé par
n'importe quelle affectation s'il s'agit d'un maximum. Le zéro reste littéral (arbitrage S7.7).

**3. Jour férié à deux sources.** La valorisation lisait le calendrier de `RegulatoryParameters`,
`JourFerieRefuse` (HARD) lisait le drapeau du créneau. Le lot S8.0 avait rendu l'écart atteignable :
déclarer son calendrier sans marquer ses créneaux valorisait les minutes en férié mais n'empêchait
personne d'y travailler. Les deux lisent désormais le même calendrier, toujours arbitré en un point
unique par `ScenarioRegulatoryParametersMapper`. Deux conséquences assumées : le férié devient une
propriété de la **date**, et un créneau traversant minuit est refusé si l'un des deux jours civils
est férié — sauf s'il s'arrête à minuit pile.

**4. Poste virtuel filtré sur l'activité en SC-06.** Arbitrage rendu : un poste virtuel n'est pas
soumis à la règle d'activité, il existe pour combler le besoin. La passe de repli le filtrait
pourtant et rendait `null` faute de correspondance : SC-06 répondait « rien à pourvoir » alors
qu'un poste à pourvoir figurait au dataset. Elle propose désormais toujours un poste virtuel dès
qu'il en existe un ; la déclaration ne sert plus qu'à choisir le plus parlant.

**Contrat.** `activitesCompatibles` était marqué SUPPORTÉ — ce qu'un intégrateur lit « le moteur
respecte ma déclaration ». En SC-03 il n'alimente qu'un compteur de diagnostic, qui pose de surcroît
une question globale (« quelqu'un le peut-il ? ») et n'attrape donc pas une affectation individuelle
incompatible. Passé **TOLÉRÉ**, avec l'arbitrage rendu : **HARD**, au lot des contraintes
personnelles. Idem `activitesAutorisees` du poste virtuel.

### Restitution — lot S8.4 : ce que le moteur savait et ne disait pas (2026-08-11)

**Aucun écart de score — mesuré.** 609 tests, 0 échec (596 avant). Le jeu de référence SC-03 reste
à `-67440` soft, et sa réponse reste sans alerte ni détail.

**Deux promesses du contrat, non tenues.** `diagnostics.alerts` valait `List.of()` en dur en SC-03
et SC-06 : tout ce que leur préparation constatait ne partait qu'aux journaux du serveur.
`diagnostics.ignoredCreneaux` ne restituait que trois entiers — un appelant qui transmet
quatre-vingts créneaux et en retrouve soixante-dix-sept savait qu'il en manquait trois, sans
pouvoir dire lesquels.

**Le pire cas n'était pas l'exclusion, c'était la substitution.** Un créneau écarté se remarque, il
manque à la réponse. Quand la plage de nuit déclarée est inexploitable, le moteur lui substitue la
plage légale 22:00–06:00 : cela **déplace le score**, ne laisse aucune trace, et l'appelant croit
que sa plage a été appliquée. Idem pour un calendrier de fériés déclaré qui écrase les drapeaux du
dataset. D'où `PLAGE_NUIT_PAR_DEFAUT` et `CALENDRIER_FERIES_DIVERGENT`, avec quatre autres codes
communs aux trois scénarios.

**Un seul geste, deux destinataires.** `CollecteurAlertes.signaler` écrit dans le journal et dans
la réponse ; c'est le seul moyen d'ajouter une alerte. Les deux canaux avaient divergé parce que
rien ne les tenait ensemble. `AlertCode`, `AlertSeverity` et `ScenarioAlert` quittent
`ScenarioDatasetBuilderSc01` : un vocabulaire commun aux trois scénarios n'a plus à porter le nom
du seul qui s'en servait.

**`exclu` sépare le constat de ses suites.** Le motif dit ce qui a été constaté, `exclu` ce qui en
a été fait : une activité inconnue exclut en SC-03, qui partitionne, et n'exclut pas en SC-01, qui
mesure. `AUCUNE_RESSOURCE_DANS_DATASET` n'exclut jamais. Un quatrième motif,
`MARQUEUR_REPOS_NON_RATTACHE`, n'alimente aucun compteur — il n'en existait pas — et c'est
pourtant le plus gênant : le repos écarté fera un trou dans le planning rechargé.

**Le troisième lecteur du drapeau férié.** S8.3 avait réconcilié la valorisation et la contrainte
HARD sur le calendrier. `AssignmentDiagnosticsFactory` lisait encore `isJourFerie` et restituait
`NO_RESOURCE_ASSIGNED` là où le calendrier déclarait un férié. La règle vit désormais dans
`CalendrierJoursFeries.toucheUnJourFerie` et les trois lecteurs l'appellent.

**Le schéma publié était faux.** `50_ScenarioResponse.schema.json` déclarait `sansRessource` — le
seul alias d'entrée, jamais sérialisé — et exigeait un `saucuneRessourceDansDataset` inexistant.
Avec `additionalProperties: false`, un client validant sa réponse contre le schéma publié la
voyait rejetée. Un test confronte désormais les noms dans les deux sens.

### Documentation — lot S8.5 : un nom, un seul, et un test qui le tient (2026-08-11)

**Aucun changement de code métier.** 612 tests, 0 échec (609 avant, soit 3 tests ajoutés). Aucun
écart de score : ce lot ne touche ni contrainte, ni mapper, ni DTO.

**37 documents renommés** en `NN_SUJET_DU_DOCUMENT` majuscules, accents retirés, et **203
citations réécrites** dans le corpus et le code.

#### Ce que la casse cachait

Le dépôt est configuré `core.ignorecase=true`. La casse d'un nom de fichier peut alors diverger
entre le disque et l'index git **sans que rien ne le signale**. C'est arrivé à
`90_SUIVI_DEVELOPPEMENT_MOTEUR.md` : git le suivait en minuscules pendant que le disque le portait
en majuscules, et treize citations en majuscules pointaient donc dans le vide pour qui clone sur un
système sensible à la casse. Un `git mv` ordinaire ne corrige pas cela — il faut passer par un nom
intermédiaire pour forcer l'index.

Trois documents portaient des accents, échappés en octal par git (`10_R\303\251f\303\251rentiel`)
et rendus fragiles selon l'outil qui les lit.

#### Les liens morts que le nettoyage a révélés

Renommer a obligé à confronter chaque citation à la réalité. Dix-neuf étaient déjà fausses **avant**
ce lot :

* des citations **sans préfixe de série** — `WORKMETRICS`, `DATASET_BUILDER`, `PLANNING_CONTEXT`,
  `ScenarioResponseContract` (sans leur préfixe de série) — qui ne désignaient aucun fichier ;
* `30_UML_SOLVEUR_SIMPLIFIE`, cité par l'index et par le dataset builder, quand le fichier
  s'est toujours appelé `SOLVER` ;
* `HORIZON_TEMPOREL_ET_REGLEMENTAIRE`, cité deux fois, qui n'a jamais existé — le sujet vit
  dans `20_PLANNING_CONTEXT.md` ;
* `60_interface_windev_moteur_plan_documentaire`, cité quatre fois avec des renvois à ses §7 et
  §8.7, et **jamais écrit**. Les sections sont irrécupérables : les renvois le disent désormais
  plutôt que de faire croire à une source.

#### L'index était incomplet d'un quart

Onze documents existaient sans figurer nulle part à l'index — dont toute la série 92 des cadrages
et audits, `92_CONTRAT_ENTREE_SC03.md` (la référence intégrateur), et le contexte agent. La série
92 n'avait même pas de rubrique : elle était rangée sous « 90 — Suivi de développement », ce qui
invite précisément à la confusion que la séparation 90/92 cherche à éviter.

L'index gagne trois rubriques — 91, 92, contexte agent — et une note explicite : la série 92
contient des **instantanés datés**, qui ne font pas autorité sur le code actuel.

#### Le garde-fou

`CorpusDocumentaireTest` verrouille trois choses : la convention de nommage, l'absence de lien
mort dans le corpus **et dans le code**, la complétude de l'index. C'est lui qui a trouvé les dix-
neuf liens morts ci-dessus — ils étaient invisibles à la lecture.

Il inaugure un niveau **V0** dans `60_TESTING_STRATEGY_ENGINE.md` : des tests qui ne testent pas le
moteur mais ce qui l'entoure, et qui existent tous parce qu'une dérive est déjà survenue sans
qu'on la voie.

#### Une décision d'architecture remise d'aplomb

`20_DECISIONS_CONCEPTION_OPTAPLANNER.md` posait que « la mesure temporelle de la nuit reste globale
et commune ». Le lot S8.1 avait tranché l'inverse sur arbitrage métier — un veilleur et un salarié
de nuit occasionnel ne relèvent pas des mêmes horaires — sans que la décision soit mise à jour. Elle
l'est, avec ce qui en reste vrai et le contrepoids posé en S8.2.

#### Périmètre — ce qui n'a pas été renommé

Deux exclusions assumées, écrites dans l'index et dans le test :

* les **artefacts machine** (`*.schema.json`, `*.yaml`) — leur nom est une référence externe,
  qu'un intégrateur peut avoir câblée ;
* le sous-répertoire **`Windev_part/`** — partagé avec l'équipe WinDev. On ne renomme pas
  unilatéralement les fichiers d'une autre équipe.

> **Convention d'écriture** : un document *disparu* se cite **sans son extension**. Lui donner
> son extension alors qu'il n'existe pas ferait échouer `CorpusDocumentaireTest`, qui ne peut pas
> distinguer une citation d'un exemple.

### SC-02 — lot S0 : l'absence ne s'arrêtait pas à minuit (2026-08-11)

617 tests, 0 échec (612 avant). **Aucun écart de score** : aucun jeu d'essai ne contenait le cas
fautif — c'est précisément pourquoi il avait survécu.

`IndisponibiliteSalarie` comparait la seule `date` du créneau aux bornes de l'absence. Un créneau
du 3 mars 22:00 → 06:00 échappait donc à une absence déclarée le 4 mars, alors qu'il fait
travailler six heures pendant celle-ci — **aucun point HARD n'était produit**. Même famille que les
quatre calculs réparés au lot S8.3, sur un lecteur que ce lot n'avait pas couvert.

**Le défaut avait un second lecteur.** `Sc06CandidatEnumerationService.estEligible` appliquait la
même comparaison, sur la date du besoin : un besoin de nuit débordant sur le lendemain rendait
éligible un salarié qui y est déclaré absent. SC-06 pouvait donc **proposer au podium quelqu'un en
arrêt maladie**. Les deux lecteurs appellent désormais la même méthode.

`Creneau.chevauchePeriode(debut, fin)` rejoint `couvre`, `getDebutEffectif` et `getFinEffectif` :
la règle de minuit vit à un seul endroit, et les appelants cessent de la redécrire.

Effet de bord réparé au passage : une absence aux bornes nulles faisait exploser la contrainte en
`NullPointerException` — `!c.getDate().isBefore(null)`. Elle est désormais sans effet, comme une
borne absente l'est partout ailleurs dans le moteur.

Cinq cas ajoutés à `Phase4ConstraintsTest` : le débordement, la comparaison stricte à minuit pile,
le démarrage pendant l'absence, le lendemain resté libre, et les bornes nulles.

### SC-02 — lot S1 : le squelette du remplacement (2026-08-11)

623 tests, 0 échec (617 avant). `POST /scenarios/sc02/solve` est exposé. **Ni SC-01, ni SC-03, ni
SC-06 ne changent de comportement** — leurs scores sont inchangés.

#### Le principe, tenu par les tests

Seuls les créneaux du salarié absent que son absence **recouvre** sont rendus au solveur. Tout le
reste du planning transmis est épinglé : le solveur le voit, les contraintes le mesurent, aucune
affectation existante ne bouge pour faire de la place. Les créneaux de l'absent situés **hors** de
sa période d'absence lui restent, eux aussi.

Le prix est assumé : le moteur répond « à pourvoir » là où un échange entre deux collègues aurait
suffi. C'est l'arbitrage métier du 11/08 — remanier le planning des présents est une décision
d'encadrement, pas une décision de moteur.

Vérification que ces tests mordent : en remplaçant la politique d'épinglage par la politique
neutre, **cinq des six tests tombent**. Ils ne passent pas par accident.

#### Aucun champ nouveau pour décrire l'absence

Elle est portée par `dataSet.indisponibilites`, déjà au contrat et déjà tenue par une contrainte
HARD. `salarieAbsentId` ne fait que désigner **de qui** il s'agit — donc quels créneaux libérer, et
de quoi rendre compte. Rien à demander à WinDev de ce côté.

#### Le contrat n'expose que ce que le lot honore

Le cadrage prévoit une liste de remplaçants autorisés, des seuils de surcharge et une autorisation
de découpage. **Aucun n'est au contrat**, parce qu'aucune règle ne les lit encore : un champ
transporté que personne n'exploite est pire qu'un champ absent, l'appelant le renseigne et croit
l'avoir dit. Ils arriveront avec les lots qui les mettent en œuvre. Le bloc `scenarioParameters`
est strict — un paramètre d'un lot à venir produit une erreur explicite, jamais un silence.

#### Ce que le lot restitue

Un bloc `remplacement`, propre à SC-02 comme `candidats[]` l'est à SC-06 : la clé est omise partout
ailleurs. Il porte le nombre de créneaux libérés, le nombre repris, les heures restant à pourvoir,
et le sort de **chaque** créneau libéré — y compris ceux que personne n'a repris. Un remplacement
qui n'a pas eu lieu est une information, pas un silence. Une alerte `HEURES_RESTANT_A_POURVOIR`
double le total : l'appelant l'apprend au lieu de le déduire.

#### Un préalable qui n'était pas prévu

La préparation d'un scénario dataset-driven — référentiel, partitions, marqueurs de repos, cadre
réglementaire, diagnostics — n'avait rien de propre à SC-03. Elle a été extraite avant d'écrire
SC-02, dans son propre commit : `ScenarioDatasetPreparationService`, et une
`PolitiqueAffectationCreneau` fournie par chaque scénario pour le seul point où ils divergent — ce
qu'ils font de l'affectation déjà portée par un créneau d'entrée. SC-03 passe la politique neutre
et ne change pas de comportement.

#### ⚠️ Limite connue, et elle compte

`activitesCompatibles` n'est lu par **aucune contrainte** (rang 10 du backlog). SC-02 peut donc
confier un créneau à un salarié qui ne pratique pas l'activité. Les seules règles qui écartent
réellement un remplaçant sont aujourd'hui les contraintes HARD en vigueur : chevauchement
physique, indisponibilité, jour férié refusé.

C'est SC-02 qui rend ce manque coûteux : SC-06 se protégeait par un filtre d'éligibilité écrit
dans son énumération, hors solveur. Un scénario qui affecte réellement n'a pas cette échappatoire.

#### Reste du chantier

S2 (découpage aux frontières de disponibilité), S3 (surcharge), S4 (restitution complète et
inscription au contrat série 50), S5 (canal FileAdapter). Voir `92_CADRAGE_SCENARIO_SC-02.md` §8.

### SC-02 — lot S2 : couvrir un créneau en partie (2026-08-11)

641 tests, 0 échec (623 avant). **Aucun autre scénario n'est touché** : les deux contraintes
ajoutées ne regardent que les créneaux portant un `creneauOrigineId`, que SC-01, SC-03 et SC-06 ne
produisent jamais.

#### Où l'on coupe

Le lot initial du cadrage prévoyait une grille de 30 minutes. L'arbitrage métier du 11/08 l'a
écartée, et pour une raison qui tient en un exemple : sur un créneau 13h30–16h00, un remplaçant qui
prend son service à **13h45** crée une coupe légitime — qu'aucune grille de 30 minutes n'aurait
produite.

Les coupes viennent donc des **frontières de disponibilité réelles** : début ou fin d'un créneau
déjà affecté à un remplaçant, bord d'une de ses indisponibilités. C'est à la fois plus juste et
moins coûteux — une ou deux frontières par remplaçant, au lieu de seize segments pour une journée
de huit heures.

Un créneau qu'aucune frontière ne traverse n'est **pas touché** : il garde son identifiant, et le
cas courant reste ce qu'il était au lot S1.

#### Le seuil des 30 minutes porte sur le bloc, pas sur le morceau

C'est la lecture que l'arbitrage a corrigée. Un bloc **confié à quelqu'un** ne fait jamais moins de
30 minutes ; le **reliquat non couvert**, lui, n'a aucun minimum et part à pourvoir tel qu'il est.

`BlocConfieTropCourt` (HARD) reconstitue donc les suites contiguës avant de mesurer : deux segments
de quinze minutes attribués à la même personne bout à bout forment une demi-heure valide, puisque
c'est une demi-heure qu'elle travaillera. Elle compte une violation par suite trop courte — on peut
en avoir deux sur le même créneau si on y fait revenir la même personne.

Cette contrainte HARD ne rend jamais le problème insoluble : le morceau trop court a toujours une
issue, rester à pourvoir. C'est précisément la réponse attendue.

#### Le contrepoids

Rien n'empêchait un créneau découpé de partir à quatre personnes — conforme aux règles, et
inutilisable sur le terrain. `CohesionCreneauOrigine` (SOFT, 300 points par ressource en excédent
de la première) rend l'éparpillement coûteux sans l'interdire. Le poids est délibérément inférieur
à celui d'un créneau non couvert (2 000) et à celui d'un poste virtuel (500) : mieux vaut deux
remplaçants réels que des heures à pourvoir, et mieux vaut deux remplaçants qu'un poste fictif.

Une part laissée à pourvoir n'est **pas** comptée comme de l'éparpillement : elle a déjà son propre
coût, et le compter deux fois pousserait le solveur à préférer un éparpillement à une couverture
partielle.

#### Le découpage est un moyen, pas un résultat

`RecombinaisonSegments` refait un créneau des morceaux qu'une même personne a repris. Un créneau
repris en entier ressort **entier et sous son identifiant d'origine** — la coupe interne n'a jamais
existé pour l'appelant. C'est le cas courant, et c'est le comportement du jeu de référence : le
mercredi est bien coupé à 09:00, mais Paul reprend les deux morceaux, et la réponse montre un
créneau unique de huit heures.

Quand le créneau ressort réellement en plusieurs morceaux, chacun porte un identifiant dérivé —
`<id>#S1`, `#S2` — **renumérotés après recombinaison**, et un `creneauOrigineId` qui évite d'avoir
à analyser la chaîne. Une alerte `CRENEAUX_DECOUPES` (INFO) prévient que la réponse contient plus
de créneaux que la demande.

#### Un invariant précisé plutôt que cassé

`Creneau.duree` était « calculée à l'entrée, jamais recalculée » — formulation qui supposait que
tout créneau vienne de l'appelant. SC-02 en fabrique. L'invariant tient, sa formulation se
précise : la durée est calculée **une seule fois, avant la résolution**, et « l'amont » inclut
désormais une étape de préparation côté moteur. Écrit dans
`20_DECISIONS_CONCEPTION_OPTAPLANNER.md`.

#### Ce que les tests prouvent

Les deux cas de l'arbitrage sont joués mot pour mot dans `sc02_decoupage.json` : le service à 15h30
qui produit une couverture partielle, et celui à 13h45 dont on ne veut pas les quinze minutes.
Vérification que la règle mord : en retirant `BlocConfieTropCourt` du provider, **le remplaçant
récupère les quinze minutes** et deux tests tombent.

`DecoupageEtCohesionTest` complète en isolation — où l'on coupe (V1, calcul pur, y compris le
passage de minuit sur le bord d'une absence) et ce qu'il en coûte (V3, contraintes vérifiées une à
une).

#### ⚠️ Effet de bord mesuré, laissé tel quel

`CreneauNonAffecte` pénalise **par occurrence**. Un créneau libéré découpé en trois morceaux tous
non couverts coûte donc trois fois, là où il coûtait une fois entier. La direction reste juste — ce
sont bien des heures non couvertes — mais l'ampleur dépend du découpage. Sans conséquence
aujourd'hui : SC-02 est un scénario neuf, aucun score de référence n'est comparé d'une version à
l'autre. À reconsidérer si le scoring de SC-02 devient un critère de décision.

### SC-02 — lot S3 : ce que le remplacement coûte à celui qui l'assure (2026-08-11)

646 tests, 0 échec (641 avant). **Aucun autre scénario n'est touché** : les deux contraintes
ajoutées ne se déclenchent qu'en présence d'un `SeuilsSurcharge` au problème, que SC-02 est seul à
y placer.

#### Deux grandeurs, parce que l'encadrement ne juge pas les deux de la même manière

La surcharge s'exprime **en heures par jour** quand elle porte sur une journée, **en heures par
semaine** quand la semaine est également touchée. Une longue journée dans une semaine creuse n'est
pas une semaine chargée faite de journées ordinaires. Les deux seuils sont donc indépendants, et
chacun a sa contrainte.

Le total mesuré porte sur **tout ce que le salarié travaille** — son planning existant compris — et
non sur le seul remplacement : c'est bien sa charge du jour que le seuil borne, pas ce qu'on vient
d'y ajouter.

#### La difficulté était la calibration, pas la mesure

Un seuil de confort doit peser sans interdire. Or ne pas couvrir un créneau coûte **2 000 points
forfaitaires**. Une pénalité de surcharge à 50 points la minute — le niveau des bornes de confort
existantes — aurait rendu l'abandon *moins cher* que la couverture dès trois quarts d'heure de
dépassement : le seuil se serait comporté en interdit, l'inverse exact de l'arbitrage rendu.

Retenu : **5 points la minute**, soit 300 points l'heure. Il faut plus de six heures d'excédent
pour égaler un créneau laissé à pourvoir. Le seuil départage donc deux remplaçants possibles sans
jamais dissuader de remplacer. Le jeu d'essai vérifie précisément cela : Paul dépasse les deux
seuils et reprend le créneau quand même.

#### Le plafond restitué n'est pas celui de SC-06

`ImpactMesureDTO` est réemployé tel quel, mais son champ `plafond` porte ici le **seuil déclaré par
la demande**, là où SC-06 y met le plafond individuel du salarié. Ce sont deux notions distinctes —
une borne de confort propre à la situation, une borne réglementaire propre à la personne — et les
mélanger aurait rendu le chiffre illisible. Les bornes individuelles gardent leurs contraintes et
leurs lignes au `scoreBreakdown`.

Les mesures sont rendues **même sans seuil déclaré** : elles informent, et le `plafond` vaut alors
`null`. Une borne absente n'est pas une borne à zéro.

#### « Avant » veut dire : la situation qu'on aurait eue sans remplacement

C'est le planning **épinglé** du salarié, celui que SC-02 n'a pas touché. « Après » est l'état
résolu. Le delta est donc exactement ce que l'absence a coûté à ce remplaçant, et rien d'autre.

#### Ce que les tests prouvent

Le `scoreBreakdown` est vérifié au point près — 900 pour les trois heures de dépassement
journalier, 300 pour l'heure hebdomadaire. Sans cela, rien ne prouverait que la contrainte existe :
la mesure et l'alerte sont produites par la restitution, qui ne consulte pas le solveur. Un test
qui vérifie la réponse sans vérifier le score ne teste que la moitié du lot.

#### Reste du chantier

S4 (restitution avant / après complète et inscription au contrat série 50, OpenAPI et schémas JSON)
et S5 (canal FileAdapter). Voir `92_CADRAGE_SCENARIO_SC-02.md` §8.

### SC-02 — lot S4 : ce que l'absence a coûté, et l'entrée au contrat (2026-08-13)

654 tests, 0 échec. **Aucun autre scénario n'est touché** : les évolutions portent sur le bloc
`remplacement`, que SC-02 est seul à produire.

#### Le bloc comptait des créneaux, il ne chiffrait pas des heures

Jusqu'ici la réponse disait « deux créneaux libérés, un repris » et totalisait les seules heures
restées à pourvoir. L'encadrement lisait donc un décompte d'objets sans savoir de quel volume on
parlait — un créneau de huit heures et un créneau d'une heure y pèsent pareil.

S'ajoutent quatre volumes qui se recomposent, et que l'appelant peut vérifier :

```text
heuresLiberees  = heuresReprises + heuresSurPosteVirtuel + heuresNonCouvertes
heuresAPourvoir = heuresSurPosteVirtuel + heuresNonCouvertes
```

Le partage entre poste virtuel et heures nues n'était pas lisible non plus, alors que l'arbitrage
du 11/08 en fait **deux notions distinctes**. Il l'est désormais, sans cesser d'être totalisé
ensemble : la question de l'encadrement est « combien me reste-t-il à staffer », et la réponse ne
change pas selon l'endroit où les heures sont garées. Un jeu d'essai dédié le prouve — même
situation que la référence, à un paramètre près, et les sept heures changent d'endroit sans changer
de volume. C'était jusqu'ici le seul chemin de SC-02 qu'aucun test n'empruntait.

#### Un créneau repris en partie n'est ni repris ni abandonné

`creneauxPartiellementRepris` manquait, et son absence faisait mentir l'alerte
`HEURES_RESTANT_A_POURVOIR` : le message soustrayait les repris des libérés et annonçait le reste
comme n'ayant « trouvé aucun salarié ». Sur le jeu de référence, il désignait ainsi un créneau dont
Sophie couvre la première heure.

#### Trois divergences trouvées en écrivant le contrat

C'est le rôle du lot, et elles n'auraient pas été vues autrement :

1. **`dataSet` fermé sur deux blocs.** `50_ScenarioContract.schema.json` déclarait
   `additionalProperties: false` avec les seuls `creneaux` et `ressources` : une requête portant
   `indisponibilites` — donc **toute** requête SC-02, l'absence n'ayant pas d'autre support — y
   était rejetée. `referentiels` l'était aussi, ce qui touchait déjà SC-03 et SC-06.
2. **Deux formats d'heure dans une même réponse.** `planning` produit `HH:mm`,
   `remplacement.details[]` sortait en `HH:mm:ss`. Corrigé côté code plutôt que documenté :
   SC-02 n'est pas encore intégré, et le lot S4 existe précisément pour n'imposer qu'une seule
   migration de contrat.
3. **`ImpactMesure.plafond` décrit une chose et en porte une autre.** Le schéma disait « limite
   individuelle du salarié » ; en SC-02 c'est le seuil de confort déclaré par la demande. Les deux
   lectures sont maintenant écrites côte à côte, dans le schéma comme dans l'OpenAPI.

#### Ce que les tests prouvent

Le bloc `remplacement` est **confronté au schéma publié**, dans les deux sens et sur les trois jeux
d'essai. `50_ScenarioResponse.schema.json` porte `additionalProperties: false` : un champ ajouté au
code et pas au schéma fait rejeter la réponse chez un client qui la valide — c'est exactement ce
qui s'était produit au lot S8.4, et rien ne l'avait vu parce que rien ne comparait les deux. La
confrontation, écrite alors pour `diagnostics`, est extraite en `SchemaPublie` et sert maintenant
aux deux blocs. Discrimination vérifiée en renommant un champ du schéma : le test tombe.

L'exemple JSON du contrat de sortie n'est pas rédigé à la main — il est **relevé sur la réponse
réelle** du jeu de référence. Un exemple qui diverge de ce que le moteur produit est pire qu'une
absence d'exemple.

#### Reste du chantier

S5 (canal FileAdapter, `scenarioType: SC-02`), dernier lot. Voir `92_CADRAGE_SCENARIO_SC-02.md` §8.

### SC-02 — lot S5 : le canal fichier, et deux promesses non tenues (2026-08-13)

660 tests, 0 échec. **SC-02 est clos** : les six lots S0 à S5 sont livrés.

La façade fait quinze lignes — le dispatcher enregistre automatiquement tout bean implémentant
`FileScenarioExecutionFacade`, et le scénario n'est pas réécrit, il est appelé. Le lot ne tient pas
dans ce branchement mais dans ce qu'il a fallu prouver, et dans ce que la preuve a révélé.

#### Prouver la symétrie, ce n'est pas la réaffirmer

Le test de SC-06 vérifiait, par la voie fichier, des valeurs que son auteur avait recopiées de la
voie HTTP. Cela ne prouve que ce qu'il a pensé à recopier. Le test de SC-02 **compare les deux
réponses entières**, sérialisées, sur deux jeux d'essai. Deux canaux qui appellent le même service
peuvent malgré tout diverger : une désérialisation qui ne suit pas les mêmes règles, une valeur par
défaut appliquée d'un côté, un bloc omis à la sérialisation.

La comparaison a d'abord échoué — sur les accents des messages d'alerte. **Artefact du harnais, pas
du moteur** : `MockHttpServletResponse.getContentAsString()` décode avec l'encodage déclaré par la
réponse, or un `application/json` n'en déclare pas — JSON est UTF-8 par spécification — et MockMvc
retombe sur le défaut servlet, ISO-8859-1. Les octets émis sont corrects. Aucun des quinze appels à
cette méthode dans le projet ne précisait l'encodage : personne ne l'avait vu parce que toutes les
assertions portaient sur de l'ASCII.

#### Le bloc annoncé strict ne l'était pas

`scenarioParameters` de SC-02 était déclaré strict — au contrat, à l'OpenAPI, dans le code — au
motif qu'il ne portait **pas** de `@JsonIgnoreProperties`. C'était faux : Spring Boot désactive
`FAIL_ON_UNKNOWN_PROPERTIES`, si bien qu'un paramètre inconnu était silencieusement ignoré. Un
appelant envoyant `remplacantsAutorises` — paramètre que le cadrage annonce et que le moteur
n'honore pas — recevait une réponse **200** et repartait convaincu d'avoir été entendu.

C'est le pire cas : un contrat tolérant assumé vaut mieux qu'un contrat qui promet un refus qu'il
ne prononce pas. Le refus est désormais réel et local à ce bloc, par un `@JsonAnySetter` qui nomme
le paramètre en cause — et les deux canaux refusent pareil, puisqu'ils partagent la
désérialisation.

⚠️ **La même illusion couvre plusieurs autres DTO** du contrat d'entrée, où les phases 10B et 10C
ont retiré des `@JsonIgnoreProperties` en concluant à un « contrat strict ». Inscrit au backlog
(§ C) plutôt que corrigé ici : aligner les autres blocs change le comportement de SC-01, SC-03 et
SC-06, et mérite son propre lot.

#### Ce que le lot laisse au projet

Les **quatre scénarios exposés voyagent maintenant par les deux canaux**. Le document d'échange
fichier n'en connaissait que deux — SC-06 n'y avait jamais été inscrit à son lot S6 ; c'est réparé.

### Équité — lots L0 à L2 : le RHD inviolable, puis l'unité de comparaison (2026-08-13)

689 tests, 0 échec. Trois lots livrés d'affilée : une règle forte, une unité, une mesure.

#### L0 — le repos dominical déclaré devient inviolable

`DetteReposSurReposHebdomadaire` était la seule règle à regarder le travail posé un jour de repos,
et elle cumulait trois limites : SOFT, ne distinguant pas RH de RHD alors que le fait porte la
nature, et conditionnée à `genereDetteRepos`. **Pour toute autre activité, faire travailler
quelqu'un son dimanche de repos ne coûtait rien du tout.** Le partage est désormais net : RHD
interdit, RH pesé.

⚠️ **Seul un RHD déclaré est inviolable.** Le repli fait de *tout* dimanche un RHD pour qui ne
déclare rien cette semaine-là ; interdire là rendrait le dimanche impossible à couvrir pour la
plupart des appelants. L'argument qui fonde la règle — le délai de prévenance — ne vaut que contre
un repos planifié et connu.

Seules les décisions du solveur sont jugées : l'existant épinglé est **signalé**, pas pesé. Le
pénaliser rendrait le problème insoluble pour une faute qu'il ne peut pas défaire.

#### L1 — l'heure pondérée

« On ne juge l'équité qu'à pénibilité équivalente » est un **changement d'unité**, pas un critère
de départage. Chaque minute est pondérée par le coefficient de sa seule catégorie — celle que la
dominance retient — de sorte qu'une nuit du dimanche n'est jamais comptée deux fois.

La répartition par dominance vivait enfouie dans la construction du score. Elle est **extraite**
en `RepartitionPenibilites` : deux implémentations de la même règle auraient fini par diverger,
et c'est exactement la classe d'écart que ce projet passe son temps à réparer.

Les coefficients sont transmis, jamais écrits en dur. Absents, tout vaut 1 — le défaut neutre est
le seul honnête, et **il ne déclenche pas d'alerte** : ce serait le cas de cent pour cent des
réponses jusqu'à la calibration. Une alerte permanente n'est pas un signal.

#### L2 — ce que chacun fait, rapporté à ce qu'il doit

`ecartContratPourcent`, **signé**, sur la fenêtre transmise. Sans signe, le moteur éviterait de
surcharger sans jamais rééquilibrer : l'équité ne se produirait pas, elle serait moins violée.

La proratisation sur la fenêtre traite l'**annualisation sans cas particulier** : lue sur toute la
fenêtre, la mesure ne pénalise pas un salarié annualisé pour une semaine au-dessus de sa moyenne.
`joursObserves` restitue ce dénominateur — le moteur dit sur quoi il a jugé.

#### Un défaut trouvé en chemin

Rien ne confrontait `workMetrics` au schéma publié, et il avait déjà dérivé :
`nbCreneauxNuitNonNuit` était restitué depuis la phase 8 sans y être déclaré. Même défaut qu'au lot
S8.4, autre bloc. La confrontation couvre désormais `workMetrics`.

#### Reste du chantier

L3 (harnais de simulation et calibration des coefficients), L4 (les trois critères de sélection
dans SC-02 et SC-06), L5 (contrainte SOFT), L6 (SC-05). Voir
`92_CADRAGE_WORKMETRICS_EQUITE.md` §8.

### Rang 11 — le contrat refuse enfin ce qu'il annonçait refuser (2026-08-13)

661 tests, 0 échec. **Un champ inconnu est désormais rejeté**, sur les deux canaux, avec le chemin
JSON complet du champ fautif et la liste des noms acceptés à cet endroit.

#### La correction tient en une ligne, le reste est ce qu'elle révèle

```properties
spring.jackson.deserialization.fail-on-unknown-properties=true
```

Les phases 10B et 10C avaient conclu au « contrat strict » en retirant des `@JsonIgnoreProperties`.
Le retrait ne rend rien strict : Spring Boot désactive `FAIL_ON_UNKNOWN_PROPERTIES`, et les blocs
continuaient d'ignorer en silence.

**Le test qui prétendait le prouver construisait son propre `ObjectMapper`.** Jackson est strict par
défaut : `StrictDeserializationPhase10CTest` passait donc sur un moteur imaginaire pendant que
l'application, elle, ignorait tout. C'est la leçon la plus réutilisable du lot — *un test de
configuration qui fabrique sa propre configuration ne teste rien*. Le remplaçant,
`ChampInconnuRefuseTest`, injecte le mapper de l'application et vérifie surtout par aller-retour
HTTP réel. Discrimination contrôlée : la propriété remise à `false`, trois cas tombent, dont celui
du canal fichier.

#### Une contradiction que personne ne pouvait voir

Le même test affirmait que `priorite`, `type` et `axesOrganisationnels` étaient **refusés** ; le
schéma publié affirme qu'ils sont « encore accepté et silencieusement ignoré ; ne plus émettre ».
Les deux ont coexisté des mois parce que chacun décrivait un moteur différent — celui du test, et
le vrai.

Le schéma a raison, et pas seulement parce qu'il est publié : **cinq jeux d'essai du projet
émettent encore ces champs**. Les refuser aujourd'hui casserait une migration en cours. Ils sont
donc déclarés nommément sur `CreneauInputDTO` — ce ne sont pas des inconnus, ce sont des
retraités — et basculeront en refus le jour où le contrat les retirera vraiment.

#### Refuser ne suffit pas, il faut dire quoi

Un champ inconnu n'est pas un JSON mal formé : la syntaxe est parfaite, c'est le contrat qui ne
connaît pas ce nom. Un `MALFORMED_JSON` enverrait l'appelant chercher une erreur de syntaxe
inexistante. D'où un code propre, `UNKNOWN_FIELD`, et deux informations qui font la différence
entre un diagnostic et une devinette :

* le **chemin complet** — `dataSet.creneaux[1].couleur` — parce que dans une requête de
  quatre-vingts créneaux, savoir *lequel* est tout le sujet ;
* la **liste des noms acceptés** à cet endroit, qui répond à « alors quoi ? ».

#### Effets de bord, tous instructifs

* Six jeux d'essai portaient des `_commentaire` imbriqués dans des blocs stricts. Retirés : une
  donnée de test n'a pas à violer le contrat qu'elle sert à valider. Les `_description` à la racine
  survivent, l'enveloppe de requête restant tolérante.
* Le `@JsonAnySetter` posé au lot S5 sur les paramètres SC-02 est **retiré** : la règle générale le
  couvre, et une exception bien intentionnée vaut moins qu'une règle qui vaut partout.

#### Ce qui reste, et pourquoi ce n'est pas le même problème

Six blocs déclarent ouvertement `ignoreUnknown = true`. **Ils ne mentent pas** — c'est toute la
différence avec ce que ce rang corrigeait. Les fermer se voit côté appelant : devenu **rang 12**,
il demande un arbitrage.
