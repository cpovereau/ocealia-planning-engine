# 🧱 20_DATASET_BUILDER.md

Ce document décrit le rôle, le périmètre et les invariants des composants de type `ScenarioDatasetBuilder` dans le moteur de planification.

Il sert de référence pour toute brique chargée de transformer un **contrat d’entrée scénario** en un **monde solveur exploitable**.

---

## 1. Rôle du Dataset Builder

Le `DatasetBuilder` est responsable de la **traduction contrôlée** entre :
- les données d’entrée d’un scénario,
- et les objets attendus par le moteur de résolution.

Il constitue une couche d’assemblage entre :
- le contrat API amont,
- le modèle métier moteur,
- et le monde OptaPlanner.

> Le `DatasetBuilder` prépare.
> Le solveur décide.

---

## 2. Ce que fait un Dataset Builder

Un `DatasetBuilder` prend en entrée :
- un `scenarioType`,
- un contrat JSON déjà validé,
- un `PlanningContext`,
- un `dataSet` métier,
- éventuellement des paramètres propres au scénario.

Il produit un ensemble d’objets cohérents permettant de construire le problème de planification, par exemple :
- la liste des `Creneau`,
- la liste des `Ressource`,
- les paramètres réglementaires,
- les diagnostics amont,
- et les éventuelles alertes de cohérence non bloquantes.

---

## 3. Ce que le Dataset Builder ne fait pas

Le `DatasetBuilder` ne doit jamais :
- optimiser,
- arbitrer,
- scorer,
- simuler le solveur,
- interpréter le résultat futur,
- recalculer des WorkMetrics,
- ni masquer une incohérence structurelle du dataset.

Il ne choisit pas quelle ressource couvrira quel créneau.

Il ne remplace ni :
- le `ConstraintProvider`,
- ni `ScoreWeights`,
- ni `WorkMetricsCalculator`,
- ni `ScenarioResponseMapper`.

---

## 4. Position dans la chaîne d’exécution

Le `DatasetBuilder` s’insère dans une chaîne de responsabilité claire :

```text
ScenarioContract
→ validation contrat
→ ScenarioDatasetBuilder
→ PlanningProblem / PlanningSolution initiale
→ Solver
→ WorkMetricsCalculator
→ ScenarioResponseMapper
→ ScenarioResponseDTO
```

Son rôle est donc limité à la **construction du monde initial**.

## 4.1 Monde solveur cible construit par le builder

Le rôle du `ScenarioDatasetBuilder` ne se limite pas à transformer un contrat JSON en objets métier isolés.

Il doit construire un **monde solveur complet et cohérent**, compatible avec le modèle OptaPlanner utilisé par le moteur.

Dans l’architecture actuelle, le builder prépare un `PlanningRequest`, puis le monde solveur de type `PlanningProblem`, contenant notamment :

- un `PlanningContext` ;
- des `RegulatoryParameters` ;
- un `ReferentielComptabiliteActivite` ;
- une collection de `Ressource` ;
- une collection de `Creneau` ;
- et, côté solveur, un `score` qui n’est jamais calculé par le builder lui-même.

Le builder est donc responsable de la **construction du monde initial** sur lequel le solveur pourra ensuite arbitrer.

---

## 5. Responsabilités explicites

## 5.1 Traduction du contrat d’entrée

Le builder transforme les structures du contrat en objets internes du moteur.

Exemples :
- JSON `creneaux[]` → `Creneau`
- JSON `salaries[]` → `SalarieReel`
- JSON `postesVirtuels[]` → `PosteVirtuel`

Cette traduction doit être :
- fidèle,
- déterministe,
- traçable,
- sans enrichissement implicite non documenté.

---

## 5.2 Contrôle de cohérence structurelle

Le builder doit vérifier les cohérences minimales nécessaires à la construction d’un monde exploitable.

Exemples :
- un créneau a bien un identifiant,
- une ressource référencée existe réellement,
- les dates sont dans un format valide,
- les activités manipulées sont exploitables dans le cadre du scénario,
- les objets requis par le modèle ne sont pas absents,
- la durée stockée (`duree`) est présente et cohérente avec `heureDebut`/`heureFin` — toute divergence est tracée en diagnostic.

Cette étape ne remplace pas la validation JSON Schema, mais la complète par une validation métier de structure.

---

## 5.3 Normalisation contrôlée

Le builder peut effectuer certaines normalisations purement techniques, à condition qu’elles soient :
- déterministes,
- documentées,
- et sans impact métier caché.

Exemples possibles :
- conversion de formats horaires,
- résolution de types enum,
- initialisation d’états neutres,
- création explicite de la pseudo-ressource `A_AFFECTER` si le modèle la requiert.

---

## 5.4 Préparation du monde solveur

Le builder prépare le monde solveur de manière cohérente avec les invariants moteur.

Il assemble :
- les `ProblemFacts`,
- les `PlanningEntity`,
- le contexte,
- les paramètres réglementaires,
- et les structures nécessaires au lancement du solveur.

Il doit garantir que le monde construit est :
- complet,
- cohérent,
- stable,
- explicable.

## 5.4.1 Objets attendus dans le monde solveur

Le builder doit produire des objets conformes au modèle solveur réel.

| Élément                           | Nature côté solveur                | Rôle du builder                                   |
|-----------------------------------|------------------------------------|---------------------------------------------------|
| `PlanningContext`                 | `ProblemFactProperty`              | injecter le contexte de résolution                |
| `RegulatoryParameters`            | `ProblemFactProperty`              | injecter les paramètres réglementaires            |
| `ReferentielComptabiliteActivite` | `ProblemFactProperty`              | injecter la source de vérité métier des activités |
| `List<Ressource>`                 | `ProblemFactCollectionProperty`    |                                                   |
|                                   | + `ValueRangeProvider`             | fournir toutes les ressources mobilisables        |
| `List<Creneau>`                   | `PlanningEntityCollectionProperty` | fournir toutes les entités à affecter             |
| `score`                           | score solveur                      | ne jamais le construire côté builder              |

Le builder ne calcule jamais le score et ne décide jamais l’affectation finale.
Il fournit seulement un **monde initial valide**.

## 5.4.2 Invariants de construction du monde solveur

La construction du monde solveur doit respecter plusieurs invariants structurels.

### A. Invariants sur les créneaux

Chaque `Creneau` doit être construit avec des champs d’entrée cohérents et stables, notamment :
- `id`
- `date`
- `heureDebut`
- `heureFin`
- `duree` — **champ obligatoire, source de vérité pour tous les agrégats horaires**
- `activite`
- `typeCreneau`
- `typePlageHoraire`
- `jourFerie`
- `qualificationJour`

Ces champs sont des **faits d’entrée**.
Ils ne sont jamais modifiés par le solveur, à l’exception de la variable de décision `ressourceAffectee`.

> `duree` est issue du système source (WinDev). Le builder ne doit pas la recalculer silencieusement à partir de `heureDebut`/`heureFin`. Toute divergence entre la durée stockée et la durée calculable doit être tracée dans les diagnostics de préparation. Voir `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`.

### B. Invariants sur les ressources

La collection `ressources` doit contenir toutes les ressources autorisées à être utilisées par le solveur.

Cela inclut, selon le scénario et le modèle retenu :
- les `SalarieReel` ;
- les `PosteVirtuel` ;
- et, si l’invariant d’absence de `null` est retenu, la pseudo-ressource `RessourceNonAffectee` (`A_AFFECTER`).

**Interprétation d’une liste d’activités vide ou nulle**

Une ressource dont `activitesCompatibles` (salarié) ou `activitesAutorisees` (poste virtuel) est vide ou nulle est considérée comme **non contrainte** : elle peut potentiellement couvrir tout créneau, quelle que soit l’activité.

Cette règle s’applique dans la couche de préparation pour le calcul du diagnostic `ignoredCreneaux.aucuneRessourceDansDataset` : un créneau est comptabilisé dans ce diagnostic uniquement si aucune ressource ne déclare son activité ET qu’aucune ressource non contrainte (liste vide/nulle) n’est présente dans le dataset.

Cette interprétation est cohérente avec le comportement du solveur : en l’absence de contrainte d’activité explicite dans le `ConstraintProvider`, une ressource non contrainte reste assignable à n’importe quel créneau.

### C. Invariants sur le contexte

Le `PlanningContext`, les `RegulatoryParameters` et le `ReferentielComptabiliteActivite` sont des **faits immuables**.
Le builder doit les injecter de manière explicite et traçable.

### D. Invariant de non-responsabilité sur le score

Le builder ne calcule jamais :
- le score OptaPlanner,
- les arbitrages,
- ni l’affectation optimale.

Il prépare uniquement le monde solveur initial.

---

## 5.5 Production de diagnostics amont

Le builder peut produire des diagnostics de pré-résolution utiles pour l’intégration.

Exemples :
- données absentes,
- activité inconnue,
- pause incohérente,
- créneau hors fenêtre,
- ressource non compatible avec le type de scénario.

Ces diagnostics :
- n’appartiennent pas au score,
- ne constituent pas des WorkMetrics,
- ne remplacent pas les contraintes,
- mais aident à comprendre la qualité du dataset fourni.

---

## 6. Invariants de conception

## 6.1 Déterminisme
À contrat identique, le builder doit produire le même monde solveur.

## 6.2 Absence d’arbitrage
Le builder ne prend aucune décision relevant du solveur.

## 6.3 Traçabilité
Chaque objet construit doit rester explicable à partir du contrat d’entrée.

## 6.4 Fidélité métier
Le builder ne doit pas inventer une donnée métier structurante absente du contrat.

## 6.5 Isolation des responsabilités
Toute logique de scoring, de contrainte ou de métrique post-résolution est hors builder.

---

## 7. Cas typiques de responsabilité

### Relève du builder
- transformer un créneau JSON en objet `Creneau`
- rattacher le `PlanningContext`
- injecter les paramètres réglementaires transmis
- produire une alerte si une activité n’existe pas dans le référentiel amont
- créer le monde initial du solveur

### Ne relève pas du builder
- déterminer si un créneau est bien affecté
- pénaliser un poste virtuel
- calculer un score de solution
- mesurer les heures de nuit finales par ressource
- conclure qu’un salarié est surchargé

## 7.1 Cas concret — rôle du builder dans SC-01

Dans le scénario `SC-01`, le builder `ScenarioDatasetBuilderSc01` construit les créneaux à partir de paramètres utilisateur de haut niveau, notamment :
- `dateDebut` / `dateFin` ;
- ressource cible ;
- amplitude journalière ;
- horaire de début de poste ;
- borne d’alerte de fin de poste ;
- pause midi optionnelle ;
- jours travaillés ;
- jours fériés.

Le builder applique ensuite plusieurs règles de génération :
- attribution du code activité déclaré par l'appelant (`codeActiviteId`), avec repli signalé ;
- qualification hebdomadaire des jours de repos (`RH` / `RHD`) ;
- exclusion des jours fériés ;
- exclusion des jours de repos ;
- génération d’un ou deux créneaux selon la cohérence de la pause déjeuner ;
- affectation initiale de chaque créneau à `RessourceNonAffectee.INSTANCE`.

Le builder `SC-01` produit également des alertes de pré-résolution, parmi lesquelles :

| Code | Sévérité | Signification |
|------|----------|---------------|
| `SHIFT_END_EXCEEDED` | `WARNING` | Fin de poste au-delà de la borne d'alerte |
| `LUNCH_BREAK_OUTSIDE_AMPLITUDE` | `WARNING` | Pause midi incohérente : un seul créneau généré |
| `INSUFFICIENT_WEEKLY_REST` | `ERROR` | Aucun jour de repos hebdomadaire configurable |
| `TOO_MANY_NON_WORKED_DAYS` | `INFO` | Jours non travaillés au-delà du repos hebdomadaire |
| `UNKNOWN_ACTIVITY` | `ERROR` | Code activité des créneaux générés absent du référentiel injecté |
| `ACTIVITY_CODE_DEFAULTED` | `WARNING` | Aucun `codeActiviteId` déclaré : code historique `travail` appliqué |

`UNKNOWN_ACTIVITY` ne porte pas de date : elle concerne le dataset, pas un jour.
Elle est émise une seule fois — le code activité est commun à tous les créneaux générés.
Sans elle, la situation serait silencieuse côté client : le lookup du référentiel échoue,
les créneaux ne comptent pas dans la charge, les WorkMetrics tombent à zéro et les
contraintes métier restent inertes. Le compteur `ignoredCreneaux.activiteInconnue`
mesurait déjà le phénomène, sans jamais l'expliquer.

Chaque alerte porte une `severity` (`INFO` / `WARNING` / `ERROR`).
Une alerte `INFO` décrit une configuration atypique mais valide : elle ne doit pas être
restituée comme une anomalie. Un temps partiel à 4 jours travaillés relève de ce cas.

La qualification du repos hebdomadaire suit le week-end, pas l'ordre des jours :
dimanche non travaillé → `RHD`, samedi non travaillé → `RH`. Les jours non cochés
restants relèvent de l'horaire contractuel et sont qualifiés `NON_TRAVAILLE`.
Si le week-end est entièrement travaillé, le `RH` est reporté sur le premier jour non coché.

Ces alertes appartiennent au périmètre du builder.
Elles ne proviennent ni du solveur, ni du score.

---

## 8. Données d’entrée sensibles à documenter

Le builder devient particulièrement structurant dès lors que le dataset amont introduit des concepts de liaison ou de structuration intra-journalière.

Exemples :
- `groupeBesoinId`
- `blocJourId`
- `ordreDansBloc`
- `estSegmentDePause`
- référentiel d’activités
- paramètres réglementaires associés aux ressources

Ces éléments ne doivent pas être interprétés de manière implicite ou dispersée dans plusieurs couches.

S’ils existent dans le contrat d’entrée, le builder devient le point unique chargé de :
- les lire,
- les normaliser,
- les exposer proprement au moteur.

## 8.1 Distinction entre paramètres scénario et monde solveur

Le builder assure une traduction entre deux niveaux distincts :

### A. Niveau scénario

Le contrat d’entrée peut exprimer des paramètres métiers ou ergonomiques, par exemple :
- `dailyAmplitudeHours`
- `shiftStart`
- `shiftEndAlert`
- `lunchBreak`
- `workedDays`
- `holidayDates`

Ces paramètres ne sont pas directement des objets solveur.
Ils servent à **fabriquer** les objets du monde solveur.

### B. Niveau solveur

Le DatasetBuilder traduit le contrat d’entrée en monde solveur exploitable, sans arbitrer ni scorer en :
- `Creneau` concrets ;
- `Ressource` mobilisables ;
- `PlanningContext` ;
- `RegulatoryParameters` ;
- `PlanningRequest` puis `PlanningProblem`.

Cette séparation est essentielle pour éviter tout couplage direct entre l’API d’entrée et le modèle interne du solveur.

---

## 9. Variantes par scénario

Chaque scénario peut nécessiter un builder dédié.

Exemples :
- `ScenarioDatasetBuilderSc01`
- `ScenarioDatasetBuilderSc02`
- etc.

Principe :
- un scénario peut changer la forme du dataset attendu,
- mais ne doit pas remettre en cause les invariants globaux du moteur.

Autrement dit :
- le builder varie,
- le moteur reste stable.

---

## 10. Rapport avec le contrat d’entrée

Le builder est le premier consommateur réel du contrat `ScenarioContract`.

Il doit donc rester strictement aligné avec :
- `ScenarioContract.schema.json`
- la documentation de chaque champ structurant
- les décisions d’architecture du moteur.

Toute évolution du contrat d’entrée doit conduire à vérifier :
- si le builder doit être adapté,
- si les diagnostics doivent évoluer,
- si de nouvelles structures intermédiaires sont nécessaires.

## 10.1 Rapport avec `PlanningRequest`

Dans l’architecture actuelle, le builder alimente un `PlanningRequest` qui joue le rôle de structure d’échange interne avant l’entrée dans le solveur.

`PlanningRequest` contient explicitement :
- `planningContext`
- `regulatoryParameters`
- `referentielComptabiliteActivite`
- `ressources`
- `creneaux`

Le builder doit donc garantir que cet objet est :
- complet ;
- cohérent ;
- immuable dans son contenu utile ;
- et directement exploitable par la couche qui construira ou lancera le solveur.
  
---

## 11. Rapport avec les tests

Le comportement du builder doit être testable indépendamment :
- du solveur,
- du score,
- et de la restitution API finale.

Les tests du builder doivent vérifier principalement :
- la cohérence des objets produits,
- la fidélité au contrat d’entrée,
- la stabilité des diagnostics,
- l’absence d’enrichissement implicite non documenté.

---

## 12. Risques évités par ce design

Documenter explicitement le rôle du builder permet d’éviter :
- la logique métier cachée dans les contrôleurs,
- des transformations dispersées,
- des règles implicites selon les scénarios,
- des incohérences entre contrat d’entrée et monde solveur,
- un couplage trop fort entre API et OptaPlanner.

## 12.1 Évolution du DataSet amont (V2)

Le DataSet transmis par WebDev évolue afin de représenter plus fidèlement
la structure métier du logiciel de planning.

Le builder reste responsable de la transformation vers le modèle
interne du moteur.

### 12.1.1 Responsabilités du builder

Le builder doit :
- interpréter les axes organisationnels ;
- transformer les besoins en créneaux élémentaires ;
- injecter explicitement les paramètres réglementaires ;
- préserver les identifiants métiers transmis.

### 12.1.2 Distinction des types de données

Trois catégories peuvent être transmises :

**Besoins**
décrivent un volume de travail à couvrir.

**Affectations existantes**
décrivent des créneaux déjà planifiés.

**Indisponibilités**
décrivent les périodes où une ressource ne peut être planifiée.

### 12.1.3 Compatibilité V1

Le builder continue d'accepter la structure V1 :
dataSet
├─ ressources
└─ creneaux

La structure V2 sera introduite progressivement.

---

## 13. Lien avec les autres documents

Ce document complète directement :
- `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`
- `20_ARCHITECTURE_MOTEUR.md`
- `20_PLANNING_CONTEXT.md`
- `50_SCENARIO_CONTRACT.md`
- `50_SCENARIO_CONTRACT_SCHEMA.json`
- `50_SCENARIO_RESPONSE_CONTRACT.md`
- `30_UML_SOLVER_SIMPLIFIE.md`
- `90_SUIVI_DEVELOPPEMENT_MOTEUR.md`

Il sert de référence pour documenter proprement la phase :
**contrat d’entrée → monde solveur**.

## 13.1 Limite explicite du builder vis-à-vis des WorkMetrics

Le builder ne calcule pas les **WorkMetrics de restitution**.

Même si certains objets intermédiaires du monde solveur peuvent exposer une collection `workMetrics`, cela ne change pas la responsabilité documentaire du builder :
- il construit les données d’entrée du solveur ;
- il peut produire des diagnostics amont ;
- mais il ne produit pas la source de vérité métier des WorkMetrics exposées par l’API.

Les WorkMetrics de restitution relèvent d’un calcul **post-résolution**, assuré par la couche dédiée (`WorkMetricsCalculator` / `ScenarioResponseMapper` selon l’architecture retenue).

Cette règle évite toute confusion entre :
- construction du monde solveur,
- arbitrage du solveur,
- restitution des conséquences du planning. 

---

## 14. Statut du document

- Document de référence
- Normatif sur le rôle des builders de dataset
- À maintenir à chaque évolution du contrat d’entrée ou du pipeline de construction

---

**Fin du document**