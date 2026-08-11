# 92 — Cadrage : remise en service du socle réglementaire (lot S7)

> **Objet** — six contraintes réglementaires sont déclarées dans le moteur et ne se déclenchent
> jamais pour un client conforme au contrat. Ce document établit le constat, l'ordre de
> réparation et le point de contrôle de chaque étape.
>
> **Statut** — **chantier clos.** S7.0 à S7.9 livrés : les six contraintes dormantes sont
> remises en service, les deux contraintes manquantes sont écrites, le code mort et la règle
> dupliquée sont supprimés, la valorisation du jour férié fonctionne (S7.9a) et le repos
> hebdomadaire est en service (S7.9b).
> **Contrat concerné** — `50_ScenarioContract.md` §3.7 · **Suivi** — `90_SUIVI_DEVELOPPEMENT_MOTEUR.md`

---

## 1. Le constat

### 1.1 Six contraintes muettes

Elles lisent l'activité du créneau via `ref.getByCode(creneau.getActivite())` — le champ
**déprécié** — sans repli vers `codeActiviteId`. Or SC-03 et SC-06 n'envoient que
`codeActiviteId`. Le référentiel ne trouve rien, le filtre « compte dans la charge » est faux,
la contrainte ne produit **aucun match**.

| Contrainte | Portée | Règle | Seconde cause d'extinction |
|---|---|---|---|
| `NuitsConsecutivesMax` | **HARD** | R3 | seuil global à 0, **sans garde** — ✅ traitée en S7.2 |
| `ReposObligatoireApresNuits` | **HARD** | R4 | seuil global à 0, garde interne — ✅ traitée en S7.3 |
| `ReposHebdomadaireMin` | **HARD** | R7 | — (socle 7 j / 1 j off en dur) — ✅ traitée en S7.5 |
| `ReposHebdomadaireGlissant` | **HARD** | R7 | seuil global à 0, garde interne — ✅ traitée en S7.4 |
| `DureeMaximaleLegaleParSalarie` | **HARD** | — | maille erronée (§1.3) — ✅ traitée en S7.6 |
| `DimanchesTravaillesMax` | SOFT | R9 | seuil global à 0, **sans garde** — ✅ traitée en S7.1 |

Neuf autres contraintes portent bien le repli, posé en Phase 10A :
`AlternanceJourNuit`, `AmplitudeJournaliere`, `HeuresMaximumParSemaine`, `HeuresMinimumParJour`,
`JoursConsecutifsMax`, `PenibilitesLegalesMinutes`, `ReposQuotidienMinimum`, `CreneauDeNuit`,
`DetteReposSurReposHebdomadaire`. Le repli n'a pas été généralisé aux six autres.

`Creneau.getCodeActiviteEffectif()` existe et sert déjà à la restitution — **aucune contrainte ne
l'utilise**.

### 1.2 Pourquoi la suite de tests n'a rien vu

Les tests alimentent `activite` ; les clients alimentent `codeActiviteId`. Les contraintes sont
donc **vertes en test et mortes en production**. C'est la seule raison pour laquelle l'écart a
tenu aussi longtemps.

Preuve empirique : le jeu de référence SC-03 comporte une nuit (`CRE-VN-01` 22:00–06:00) et un
dimanche travaillé (`CRE-DI-01`), `workMetrics` restitue `nbDimanchesTravailles: 1` et
`heuresNuit: 8.0` — et le `scoreBreakdown` ne contient ni `LEGAL_SOFT_DIMANCHES_TRAVAILLES_MAX`
ni `LEGAL_HARD_NUITS_CONSECUTIVES_MAX`.

### 1.3 Deux défauts de fond, masqués par le premier

**Les seuils globaux valent 0.** `PlanningContext.defaultSeuilsDeTolerance()` n'appelle que le
constructeur à trois arguments. Les cinq champs `int` non finaux de `SeuilsDeTolerance`
(`maxNuitsConsecutives`, `reposApresNuitsEnJours`, `reposHebdoFenetreJours`,
`reposHebdoMinJoursOffDansFenetre`, `maxDimanchesTravailles`) ne sont **jamais** affectés.
`ReposObligatoireApresNuits` et `ReposHebdomadaireGlissant` s'en gardent ; `NuitsConsecutivesMax`
et `DimanchesTravaillesMax` non.

**`DureeMaximaleLegaleParSalarie` compare une maille à une autre.** Son `groupBy` agrège sur
**tout l'horizon** et compare le total à `DUREE_MAX_LEGALE = 780`, une valeur **journalière**
(13 h). Deux journées ordinaires suffisent à la franchir. C'est la plus explosive des six.

> **Le couplage est la difficulté centrale.** Réparer le repli seul rendrait toute nuit
> consécutive immédiatement HARD (seuil 0), chaque dimanche travaillé pénalisé de 5 000 points,
> et toute semaine normale illégale. Les deux défauts s'annulent : c'est pourquoi rien n'a
> jamais échoué.

### 1.4 Code mort

`CreneauJourFerie` et `CreneauDeNuit` ne sont enregistrées dans aucun `ConstraintProvider`.

---

## 2. Décisions

| # | Décision | Motif |
|---|---|---|
| D1 | **Les seuils sont individuels**, portés par `contraintesReglementaires` | Un plafond de nuits ou de dimanches relève du contrat de la personne, pas du contexte de calcul. Trois salariés = trois jeux de seuils. |
| D2 | ~~Seuil absent ou nul ⇒ contrainte inactive~~ → **seule l'absence désactive** | Révisée au lot S7.7a sur arbitrage : le jeu SC-03 transmet `nuitsMaximumParSemaine: 0` pour signifier « aucune nuit ». Le zéro garde son sens arithmétique ; une valeur négative est ignorée. Exception : une **largeur** de fenêtre est prise en compte à partir de 1. Voir §7.7a. |
| D3 | **Une contrainte par lot**, score réévalué à chaque fois | Six réveils simultanés rendraient tout écart de score inattribuable. |
| D4 | La **paire** fenêtre / jours off est indissociable | Une fenêtre sans minimum de jours off ne décrit aucune règle. Transmise à moitié : contrainte inactive + WARN. |
| D5 | `SeuilsDeTolerance` conserve ses trois champs de **tolérance globale** | `surchargeMaxParSalarie`, `violationsLegalesMax`, `violationsMetierMax` sont des bornes d'acceptabilité d'une solution, pas des règles individuelles. Ils restent à leur place. |

### D2 et l'invariant « un vide ne suppose jamais que la chose est possible »

Sur l'**absence**, il n'y a pas contradiction. Le moteur ne conclut pas que la nuit consécutive
est autorisée : il constate qu'**on ne lui a donné aucun plafond à faire respecter**, et
s'abstient de juger plutôt que d'inventer une limite. L'invariant interdit de déduire une
permission d'un silence ; il n'oblige pas à fabriquer une interdiction à partir de rien.

Sur le **zéro**, la première rédaction de D2 était en revanche fautive. Un vide est une absence
d'information ; un zéro est un chiffre. Les confondre revenait à traiter « aucune nuit
autorisée » comme un silence — et donc, au titre de l'invariant lui-même, à en déduire une
permission. La correction du lot S7.7a rétablit la distinction.

---

## 3. Découpage en lots

L'ordre va du moins au plus perturbant, pour qu'une surprise reste imputable.

| Lot | Objet | Effet attendu sur le score |
|---|---|---|
| **S7.0** ✅ | Socle : seuils portés au salarié, règle d'activation, **test de référence** | **Aucun** — point de contrôle |
| **S7.1** ✅ | `DimanchesTravaillesMax` — repli + `dimanchesTravaillesMaximum` | **Aucun** — mesuré (§7.1) |
| **S7.2** ✅ | `NuitsConsecutivesMax` — repli + `nuitsConsecutivesMaximum` | **Aucun** — mesuré (§7.2) |
| **S7.3** ✅ | `ReposObligatoireApresNuits` — repli + `joursReposMinimumApresNuits` | **Aucun** — mesuré (§7.3) |
| **S7.4** ✅ | `ReposHebdomadaireGlissant` — repli + paire de seuils | **Aucun** — mesuré (§7.4) |
| **S7.5** ✅ | `ReposHebdomadaireMin` — repli seul (socle légal, sans seuil individuel) | **Aucun** — mesuré (§7.5) |
| **S7.6** ✅ | `DureeMaximaleLegaleParSalarie` — **correction de maille** + `heuresMaximumParJour` | **Aucun** — mesuré (§7.6) |
| **S7.7a** ✅ | Lecture littérale du zéro — révision de D2 sur arbitrage | **Aucun** — mesuré (§7.7a) |
| **S7.7b** ✅ | Contraintes absentes : `heuresMinimumParSemaine`, `nuitsMaximumParSemaine` | **SC-03 : −960 → −66 960** (§7.7b) |
| **S7.8** ✅ | Nettoyage : code mort, cinq champs de `SeuilsDeTolerance`, règle de repli unifiée | **Aucun** — mesuré (§7.8) |

### Contenu type d'un lot S7.1 → S7.6

1. Remplacer `getActivite()` par `getCodeActiviteEffectif()`.
2. Lire le seuil sur le salarié, via `ContraintesReglementairesSalarie.seuilActif(...)`.
3. Déplacer l'assertion correspondante dans `SocleReglementaireBaselineTest` (§4).
4. Couvrir la contrainte par un `ConstraintVerifier` dédié : seuil absent, seuil nul, égalité,
   dépassement.
5. Ajouter le motif SC-06 correspondant dans `MotifCandidat` si la contrainte doit expliquer un
   rang.
6. Exécuter la suite complète, **relever l'écart de score** sur SC-01, SC-03 et SC-06, et le
   consigner dans `90_SUIVI_DEVELOPPEMENT_MOTEUR.md`.

---

## 4. L'instrument de bascule

`SocleReglementaireBaselineTest` rend la dormance **exécutable**. Chaque situation fautive y est
jouée deux fois, dans trois blocs :

- **`ClientHistorique`** — le créneau porte `activite`. Ces assertions établissent que la
  situation est réellement fautive. Sans elles, un `penalizesBy(0)` passerait pour la mauvaise
  raison — c'est le piège classique du test de non-déclenchement.
- **`ClientConformeAuContrat`** — le créneau porte `codeActiviteId`, comme WinDev. Chaque
  assertion à 0 est un **constat, pas un objectif**, et nomme le lot qui la fera changer.
- **`Reveillees`** — contraintes remises en service. Elles réagissent aux **deux** champs, et
  restent inactives tant qu'aucun seuil individuel n'est transmis.

Réveiller une contrainte consiste alors à déplacer une assertion vers le troisième bloc. Un écart
non voulu se lit ici, sur une contrainte isolée, avant de se lire dans un scénario complet.

Deux tests du bloc témoin assertent **0** : `ReposObligatoireApresNuits` et
`ReposHebdomadaireGlissant` sont éteintes deux fois, et le repli d'activité seul ne les
réveillera pas.

---

## 5. Dette repérée en cours de route

Les neuf contraintes déjà pourvues du repli le **réimplémentent en ligne** :

```java
String codeActivite = (creneau.getCodeActiviteId() != null && !creneau.getCodeActiviteId().isBlank())
        ? creneau.getCodeActiviteId() : creneau.getActivite();
```

C'est exactement le corps de `Creneau.getCodeActiviteEffectif()`. Les contraintes remises en
service au titre de S7 appellent la méthode. Tant que la règle existe en dix exemplaires, une
évolution du repli en oubliera un.

> **Soldé au lot S7.8** (§7.8). La règle vit dans `domain/creneau/CodeActivite.java`, les onze
> sites y délèguent, et un test de garde échoue si l'expression réapparaît en ligne. Le compte
> de onze — et non dix — s'explique par deux sites hors contraintes : `WorkMetricsCalculator`
> et les services de préparation SC-01 et SC-03. `CreneauDeNuit`, citée plus haut parmi les
> porteuses du repli, a été supprimée au même lot.

---

## 6. Suites

> Arbitrage rendu avant S7.6 : `heuresMaximumParJour` est un plafond de **durée travaillée**
> par journée, distinct de `amplitudeJournaliereMaximum` (première à dernière heure, pauses
> comprises) appliqué par `AmplitudeJournaliere`. Les deux coexistent et ne mesurent pas la
> même chose. Traité au §7.6.

### 6.1 Deux dormances d'une autre famille, repérées en clôture de S7.8

Elles ne relèvent pas du repli d'activité mais du **même mécanisme** : un champ que le mapper
ne sait pas calculer, donc écrit en dur, donc un filtre de tête qui ne matche jamais. Un lot
distinct est nécessaire ; il touche au contrat d'entrée et demande un arbitrage.

> **7.9a — traité.** Le calendrier est désormais alimenté ; voir §7.9a pour la mesure.

**La valorisation du jour férié ne fonctionne pas.** `TimeBreakdownCalculator` interroge
`RegulatoryParameters.estJourFerie(date)`, or les trois services de préparation construisent
`RegulatoryParameters.neutre()`, dont la liste `joursFeries` est **vide**. `minutesFerie` vaut
donc 0 pour tout client : `LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES` ne se déclenche jamais et
`heuresJourFerie` vaut 0.0 dans toutes les réponses. Aucun test n'assied cette métrique — c'est
ce qui a permis à l'écart de tenir.

Ce qui fonctionne est l'**interdiction** : `JourFerieRefuse` (HARD) lit `Creneau.jourFerie`,
alimenté par le champ `isJourFerie` du contrat. Le schéma qualifie pourtant ce champ
d'« INDICATIF (non réglementaire) » en désignant `RegulatoryParameters` comme source de vérité.
C'est la source vide qui fait autorité, et le champ dit indicatif qui porte seul la règle qui
marche.

> **7.9b — traité.** Le repos vient désormais d'un calendrier construit à la préparation ;
> voir §7.9b.

**`DetteReposSurReposHebdomadaire` est dormante.** Elle filtre sur
`getQualificationJour() == RH || == RHD`, or `ScenarioCreneauMapper` et
`ScenarioSc06PreparationService` écrivent `QualificationJour.OUVRE` en dur — « non computable
depuis le DTO seul », dit le commentaire — et `ScenarioDatasetBuilderSc01` saute purement les
jours RH/RHD au lieu d'y produire des créneaux. **Aucun créneau n'est jamais qualifié RH ou
RHD en production.** C'est une septième contrainte muette, de la même nature que les six du
§1.1, découverte parce que la suppression de `CreneauJourFerie` obligeait à recenser les
lecteurs de `qualificationJour`.

Les deux partagent une même cause : le contrat transporte des faits bruts (une date, un
drapeau) là où le moteur attend une **qualification** que seul l'appelant peut établir. Le
calendrier des fériés et la qualification des jours de repos doivent venir du contrat, ou être
dérivés d'une règle explicite — pas être laissés à une valeur par défaut.

## 7. Journal des lots

> Une section par lot livré, dans l'ordre. Chacune consigne l'écart de score mesuré —
> c'est la raison d'être du découpage.

### 7.0 — Socle

**Aucune contrainte n'a été modifiée. Le score est inchangé** — 434 tests, 0 échec (413 avant).

| Livrable | Fichier |
|---|---|
| 5 seuils portés au salarié + `seuilActif()` | `domain/ressource/ContraintesReglementairesSalarie.java` |
| Transport | `scenarios/dto/input/ContraintesReglementairesDTO.java` |
| Mapping + WARN `0` + WARN paire incomplète | `scenarios/mapper/ScenarioResourceMapper.java` |
| Champs globaux marqués `@Deprecated(forRemoval)` | `domain/contexte/SeuilsDeTolerance.java` |
| Lecture null-safe du référentiel | `domain/metier/ReferentielComptabiliteActivite.java` |
| Test de référence (12 cas) | `constraints/SocleReglementaireBaselineTest.java` |
| Test de mapping (9 cas) | `scenarios/mapper/SeuilsIndividuelsMappingTest.java` |
| Contrat §3.7, schéma JSON, OpenAPI | `50_*` |

#### Une correction non prévue : `getByCode(null)`

`ReferentielComptabiliteActivite.getByCode` déléguait directement à `Map.get`. Le comportement
dépend alors de l'implémentation : `HashMap` — celle des mappers — renvoie `null`, là où
`Map.of(...)` lève une `NullPointerException`. La même contrainte se taisait donc en production
et faisait échouer le calcul de score en test. Le garde explicite aligne les deux et supprime une
NPE latente. `contient()` a été aligné de même.

#### Ce que S7.0 ne fait pas

Aucune contrainte ne lit encore les cinq nouveaux seuils. Les renseigner n'a **aujourd'hui aucun
effet** sur le résultat. C'est délibéré : le lot est le point zéro à partir duquel les écarts des
lots suivants deviennent mesurables.

---

### 7.1 — `DimanchesTravaillesMax` (SOFT, R9)

**Écart de score : aucun.** 438 tests, 0 échec.

| Livrable | Fichier |
|---|---|
| Repli d'activité + seuil individuel + match sur dépassement seul | `constraints/legales/DimanchesTravaillesMax.java` |
| Repli non nul pour les seuils | `domain/ressource/SalarieReel.contraintesOuAucune()` |
| Motif SC-06 `DIMANCHES_TRAVAILLES_DEPASSES` | `scenarios/dto/MotifCandidat.java` |
| Bloc `Reveillees` (4 cas) | `constraints/SocleReglementaireBaselineTest.java` |
| Seuil individuel + `codeActiviteId` + 2 cas d'activation | `constraints/Phase13ConstraintsTest.java` |

#### Trois choix à retenir

**Seuls les dépassements produisent un match.** L'implémentation antérieure pénalisait de 0
lorsque le plafond était respecté, ce qui inscrivait une ligne à impact nul au `scoreBreakdown`.
Un filtre précède désormais la pénalité. La distinction est lisible en restitution : une
contrainte **présente avec `impact 0`** a produit un match sans pénalité, une contrainte
**absente** n'a produit aucun match.

**Le motif SC-06 est non éliminatoire.** Contrairement au repos quotidien, le plafond de
dimanches est une borne conventionnelle d'équité et non un seuil de légalité : un dimanche de
plus reste une décision possible, elle doit être rendue visible et non interdite.

**La pénalité reste globale, seul le seuil est individuel.**
`Penalites.depassementMaxDimanchesTravailles` est un poids de scoring — il arbitre entre familles
de contraintes, il ne décrit pas une règle applicable à une personne.

#### Mesure de l'écart

SC-03 reste à `0hard/-960soft`, et `LEGAL_SOFT_DIMANCHES_TRAVAILLES_MAX` reste absent de son
`scoreBreakdown`. Aucun jeu d'essai ni payload de référence ne transmet `dimanchesTravaillesMaximum` :
la contrainte est donc réveillée mais sans seuil, et reste inactive. **La remise en service est
sans effet tant que WinDev n'envoie pas le champ** — ce qui est exactement le comportement voulu.

Corollaire pour les lots suivants : leur écart de score sera lui aussi nul par le même mécanisme,
**sauf** pour `ReposHebdomadaireMin` (S7.5, socle légal en dur, sans seuil individuel) et
`DureeMaximaleLegaleParSalarie` (S7.6, dont la maille est à corriger). Ce sont les deux seuls lots
qui feront réellement bouger les scénarios existants.

---


### 7.2 — `NuitsConsecutivesMax` (HARD, R3)

**Écart de score : aucun.** 448 tests, 0 échec.

| Livrable | Fichier |
|---|---|
| Repli d'activité + plafond individuel | `constraints/legales/NuitsConsecutivesMax.java` |
| Motif SC-06 `NUITS_CONSECUTIVES_DEPASSEES` (éliminatoire) | `scenarios/dto/MotifCandidat.java` |
| Couverture dédiée — 9 cas, elle n'en avait aucune | `constraints/NuitsConsecutivesMaxConstraintsTest.java` |
| Garde-fou de score SC-03 | `scenarios/sc03/api/ScenarioControllerSc03RuntimeTest.java` |

#### La contrainte la plus piégeuse du lot

C'est ici que les deux défauts s'annulaient le plus dangereusement : plafond global à **0** et
**aucune garde**. Réparer le seul repli d'activité aurait rendu toute deuxième nuit consécutive
immédiatement HARD, pour tout le monde, sans qu'aucune donnée d'entrée ne l'ait demandé. C'est la
raison de l'ordre des lots — S7.0 devait poser la règle d'activation avant tout réveil.

#### Éliminatoire, contrairement aux dimanches

R3 borne la légalité, pas l'équité : enchaîner trop de nuits n'est pas un arbitrage possible.
Le motif SC-06 est donc `ERROR` et éliminatoire, au même titre que le repos quotidien.

#### Le garde-fou de score est désormais permanent

La mesure d'écart était faite à la main, lot après lot. Elle est maintenant **asserée** :
`ScenarioControllerSc03RuntimeTest` vérifie `soft = -960` en plus de `hard = 0`. Stabilité
confirmée sur trois exécutions consécutives. Toute variation de score des lots suivants fera
échouer ce test, avec le lot en cours pour seul suspect — c'est précisément ce qui manquait.

#### Couverture créée de zéro

Aucun test n'interrogeait cette contrainte : elle était enregistrée, muette, et personne ne la
regardait. Les neuf cas ajoutés fixent notamment ce que « consécutif » veut dire — comptage par
date distincte, une journée intercalée ne prolonge pas une séquence de nuits, une activité hors
charge l'interrompt.

### 7.3 — `ReposObligatoireApresNuits` (HARD, R4)

**Écart de score : aucun.** 457 tests, 0 échec.

| Livrable | Fichier |
|---|---|
| Repli d'activité + seuil individuel + retrait d'un tri destructeur | `constraints/legales/ReposObligatoireApresNuits.java` |
| Motif SC-06 `REPOS_APRES_NUITS_INSUFFISANT` (éliminatoire) | `scenarios/dto/MotifCandidat.java` |
| Couverture dédiée — 8 cas, elle n'en avait aucune | `constraints/ReposObligatoireApresNuitsConstraintsTest.java` |

#### Un tri qui mutait l'état du solveur

La méthode de vérification appelait `creneauxTravail.sort(...)` sur la liste produite par
`ConstraintCollectors.toList`. Cette liste appartient à OptaPlanner : la trier sur place revient à
modifier l'état interne du calcul de score. Le tri n'était en outre **pas utilisé** — les dates de
nuit sont retriées juste après, et la boucle de détection parcourt la liste sans ordre. Supprimé.

#### Une récupération, pas une coupure

R4 exige des **journées entières** après une séquence de nuits. À ne pas confondre avec
`ReposQuotidienMinimum`, qui mesure des **heures** entre deux journées travaillées successives.
Les deux règles peuvent être satisfaites ou violées indépendamment, et le contrat les expose sous
deux motifs distincts.

#### La garde interne devient superflue

`reposExige <= 0 → false` protégeait la contrainte du seuil global nul. Le filtre d'activation
étant désormais appliqué en tête de flux, cette garde est retirée : une seule règle décide qu'un
seuil est actif, et elle est commune à toutes les contraintes du chantier.

### 7.4 — `ReposHebdomadaireGlissant` (HARD, R7 conventionnel)

**Écart de score : aucun.** 467 tests, 0 échec.

| Livrable | Fichier |
|---|---|
| Repli d'activité + paire de seuils individuels | `constraints/legales/ReposHebdomadaireGlissant.java` |
| Motif SC-06 `REPOS_HEBDOMADAIRE_GLISSANT_INSUFFISANT` (éliminatoire) | `scenarios/dto/MotifCandidat.java` |
| Couverture dédiée — 9 cas, elle n'en avait aucune | `constraints/ReposHebdomadaireGlissantConstraintsTest.java` |

#### Le seul cas du chantier où deux champs se conditionnent

`reposHebdomadaireFenetreJours` et `reposHebdomadaireJoursOffMinimum` ne décrivent une règle
qu'ensemble. Trois cas de test le fixent explicitement : fenêtre seule, minimum seul, et paire
dont l'une des valeurs vaut 0 — dans les trois, la contrainte reste inactive, y compris sur une
semaine travaillée sept jours sur sept. Le mapper émet un WARN dans le cas de la paire à moitié
renseignée, parce que l'appelant croit alors avoir posé une limite.

#### Deux volets distincts pour R7

Ce lot traite le volet **conventionnel** — fenêtre paramétrée par le contrat. Le plancher légal,
au moins un jour off sur sept, est porté par `ReposHebdomadaireMin` et **ne se paramètre pas** :
c'est l'objet du lot S7.5. Les deux contraintes coexistent et portent deux clés de pénalité
distinctes.

#### Un jour de formation est un jour off

Au sens de la charge, une activité dont `compteDansCharge` est faux ne remplit pas la journée.
Une semaine couverte sept jours sur sept dont deux de formation satisfait une exigence de deux
jours off — c'est cohérent avec la définition du jour travaillé retenue partout ailleurs.

### 7.5 — `ReposHebdomadaireMin` (HARD, R7 plancher légal)

**Écart de score : aucun — et c'est une surprise.** 475 tests, 0 échec.

| Livrable | Fichier |
|---|---|
| Repli d'activité (aucun seuil individuel) | `constraints/legales/ReposHebdomadaireMin.java` |
| Motif SC-06 `SEMAINE_SANS_JOUR_DE_REPOS` (éliminatoire) | `scenarios/dto/MotifCandidat.java` |
| Couverture dédiée — 7 cas, elle n'en avait aucune | `constraints/ReposHebdomadaireMinConstraintsTest.java` |

#### Une prévision démentie

Ce lot devait être l'un des deux à déplacer les scores : le plancher légal n'a **aucun seuil
individuel**, le repli d'activité seul suffit donc à l'activer pour tout le monde, sans qu'aucune
donnée d'entrée ne l'ait demandé. Ce raisonnement était juste ; sa conclusion ne l'était pas.

La mesure montre un écart nul. Le seul test rouge après la modification était l'assertion de
dormance du fichier de référence — celle que le lot devait précisément déplacer. **Aucun jeu
d'essai ne fait travailler sept jours d'affilée** : la contrainte s'active, mais ne rencontre
aucune situation à sanctionner. Les scénarios existants sont conformes au plancher légal, ce qui
est en soi une information utile.

Reste donc **S7.6 comme unique lot susceptible de faire bouger les scores** — et pour une raison
différente : sa maille est fausse, pas seulement son repli.

#### Pas de seuil individuel, et c'est délibéré

Un plancher légal ne se négocie pas au contrat. C'est le seul lot du chantier où rien n'est porté
au salarié, et la seule contrainte que la remise en service active inconditionnellement.

#### Coexistence assumée avec le volet conventionnel

Un salarié qui transmettrait exactement 7 jours / 1 jour off verrait les deux contraintes se
déclencher sur la même situation. Ce n'est pas un double comptage fautif : deux règles distinctes
sont alors violées, restituées sous deux clés de pénalité et deux motifs différents.

### 7.6 — `DureeMaximaleLegaleParSalarie` (HARD, durée journalière)

**Écart de score : aucun.** 486 tests, 0 échec. Les six contraintes dormantes sont remises en
service.

| Livrable | Fichier |
|---|---|
| Correction de maille + plafond individuel + repli | `constraints/legales/DureeMaximaleLegaleParSalarie.java` |
| Clé de pénalité renommée et unité corrigée | `scoring/PenaliteKey.java` |
| Motif SC-06 `DUREE_JOURNALIERE_DEPASSEE` (éliminatoire) | `scenarios/dto/MotifCandidat.java` |
| Couverture dédiée — 9 cas, elle n'en avait aucune | `constraints/DureeMaximaleParJourConstraintsTest.java` |

#### Le seul défaut du chantier qui n'était pas un défaut de branchement

Les cinq autres contraintes étaient correctes mais débranchées. Celle-ci était **fausse** : son
`groupBy` agrégeait sur tout l'horizon et comparait ce cumul à une constante de 780 minutes, qui
est une valeur journalière. Elle mesurait une période et la comparait à un jour. Réveillée en
l'état, deux journées ordinaires l'auraient violée.

Trois corrections : agrégation par `(salarié, date)`, plafond lu sur le salarié via
`heuresMaximumParJour`, suppression de la constante globale. Quatre cas de test existent
uniquement pour interdire le retour de ce défaut de maille.

#### Une clé de pénalité renommée

`LEGAL_HARD_DUREE_MAX_LEGALE_PAR_PERIODE` devient `LEGAL_HARD_DUREE_MAX_PAR_JOUR`, et son unité
passe de `JOUR` à `MINUTE_PONDEREE` — la pénalité a toujours été exprimée en minutes. Une clé qui
annonce une période là où elle mesure une journée aurait trompé le client durablement. Le
renommage est sans risque : la contrainte étant dormante, cette clé n'a **jamais** été émise dans
un `scoreBreakdown`.

#### Le plancher physique n'est pas concerné

Supprimer la constante globale ne laisse aucun trou : `LimitePhysique` continue d'interdire
inconditionnellement plus de 24 h cumulées sur une journée et plus de 12 h pour un créneau. Ce
sont des impossibilités physiques, pas des règles négociables au contrat, et elles ne dépendent
d'aucun seuil transmis.

#### Un écart de contrat refermé

La notice SC-06 signalait que `heuresJour` était mesuré et son plafond restitué **alors qu'aucune
contrainte ne l'appliquait**. Ce lot referme cet écart : les trois mesures du bloc `impacts[]`
sont désormais adossées à une contrainte. La notice, le contrat de sortie et le schéma JSON ont
été corrigés — ils annonçaient l'inverse.

#### Durée travaillée et amplitude, deux notions complémentaires

`heuresMaximumParJour` plafonne le **temps de travail** ; `amplitudeJournaliereMaximum` borne la
première à la dernière heure, **pauses comprises**. Une journée 08:00–12:00 puis 18:00–22:00
totalise 8 h travaillées pour 14 h d'amplitude. Les deux règles coexistent, et le contrat les
restitue sous deux motifs de sévérité différente : le dépassement de durée est éliminatoire,
celui d'amplitude reste un signalement.

### 7.7a — Lecture littérale du zéro (correction de la décision D2)

**Écart de score : aucun.** 490 tests, 0 échec.

| Livrable | Fichier |
|---|---|
| `borneRenseignee()` et `largeurRenseignee()` remplacent `seuilActif()` | `domain/ressource/ContraintesReglementairesSalarie.java` |
| WARN sur borne négative au lieu de WARN sur 0 | `scenarios/mapper/ScenarioResourceMapper.java` |
| Correction d'un décompte de séquence | `constraints/legales/NuitsConsecutivesMax.java` |
| Contrat §3.7, schéma JSON, OpenAPI, notice SC-06 | `50_*`, `Windev_part/SC-06/` |

#### Ce que les données ont tranché

La décision **D2** du lot S7.0 lisait `0` comme une désactivation. Le jeu de référence SC-03 l'a
démentie : il transmet `nuitsMaximumParSemaine: 0` pour SAL-2001 et `3` pour SAL-2002.
L'intention est limpide — le premier ne fait pas de nuit — et la lecture D2 lui aurait permis d'en
faire toutes. L'exact contraire de ce qui était demandé.

**La règle devient** : seule l'**absence** désactive. Le zéro garde son sens arithmétique — un
maximum à 0 interdit tout, un minimum à 0 n'exige rien. Une valeur négative est ignorée et
tracée : elle ne décrit rien.

D2 n'était pas absurde, elle était prudente : les cinq seuils rapatriés étaient absents de tous
les payloads, et les activer sur une donnée non renseignée aurait été brutal. Mais l'argument
valait pour l'**absence**, pas pour le **zéro** — deux choses que la règle confondait.

#### Une exception, et une seule

`reposHebdomadaireFenetreJours` est une **taille**, pas une borne. « Au moins 2 jours off sur
0 jour » ne décrit aucune règle, là où « au plus 0 nuit » en décrit une parfaitement. Le zéro
littéral n'a de sens que sur ce qui se compare, pas sur ce qui se mesure. D'où deux méthodes
distinctes plutôt qu'une seule, et un cas de test pour chacune.

#### Un bug révélé par l'arbitrage

`NuitsConsecutivesMax` ne testait le dépassement qu'en **prolongeant** une séquence, jamais en
l'ouvrant. Conséquence : avec un plafond de 0, deux nuits d'affilée violaient mais une nuit
isolée passait. L'incohérence était inatteignable tant que 0 n'était pas une valeur recevable ;
la lecture littérale l'a rendue atteignable, et un test l'a immédiatement fait tomber. Le
décompte est corrigé — c'est exactement le service qu'on attend d'un jeu de test qui suit une
décision de contrat.

### 7.7b — Les deux contraintes qui n'existaient pas

**Écart de score : SC-03 passe de −960 à −66 960.** C'est la seule variation de tout le
chantier, et elle est voulue. 510 tests, 0 échec.

| Livrable | Fichier |
|---|---|
| Sous-emploi hebdomadaire, deux volets | `constraints/legales/HeuresMinimumParSemaine.java` |
| Plafond hebdomadaire de nuits | `constraints/legales/NuitsMaximumParSemaine.java` |
| Trois clés de pénalité | `scoring/PenaliteKey.java` |
| Motif SC-06 `NUITS_HEBDOMADAIRES_DEPASSEES` (éliminatoire) | `scenarios/dto/MotifCandidat.java` |
| Couverture dédiée — 10 + 10 cas | `constraints/HeuresMinimumParSemaineConstraintsTest.java`, `NuitsMaximumParSemaineConstraintsTest.java` |
| Garde-fou de score et d'affectation | `scenarios/sc03/api/ScenarioControllerSc03RuntimeTest.java` |

#### Une première implémentation qui produisait l'inverse de l'effet voulu

Le premier jet regroupait par `(salarié, semaine)` sur les créneaux existants — construction
reprise de `HeuresMinimumParJour`. Mesure faite : le solveur a confié **les six créneaux au poste
virtuel** et zéro aux deux salariés, là où il les répartissait auparavant entre eux.

La cause est structurelle : un salarié sans créneau ne produit aucun tuple, donc aucune pénalité,
tandis que lui confier un seul créneau déclenche le déficit entier. **Ne rien donner devenait le
choix le moins cher.** Une contrainte censée lutter contre le sous-emploi le récompensait.

Le correctif ajoute un second volet, `LEGAL_SOFT_SEMAINE_SANS_AFFECTATION` : les semaines
complètes de l'horizon sont énumérées, et celles où le salarié n'a rien sont pénalisées au
déficit maximal. L'ordre des coûts redevient correct — ne rien confier coûte 105 000, en confier
trop peu coûte 33 000 — et toute affectation améliore le score. Après correctif, les six créneaux
reviennent aux salariés réels et le poste virtuel est vide.

Deux volets sont nécessaires parce que les jointures des Constraint Streams sont **internes** :
faire exister un tuple pour un salarié qui n'a aucun créneau impose d'énumérer les semaines
depuis l'horizon, puis d'exclure celles qui sont pourvues. Un test garde explicitement l'ordre
des coûts — c'est lui qui interdira la régression.

#### Ce que devient le score de SC-03

`−66 960 = −960` (pénibilités) `− 66 000` (sous-emploi). Deux salariés à 35 h hebdomadaires pour
48 h de travail disponible : le déficit de 11 h chacun est **inévitable**, et c'est bien ce que
le contrat demande de rendre visible. La valeur absolue du score n'a pas de sens en soi ; ce qui
compte est qu'elle départage correctement deux solutions du même problème, et c'est le cas — le
déficit s'aggrave dès qu'un créneau part au poste virtuel.

#### Seules les semaines complètes sont jugées

Un minimum a un mode de défaillance dangereux, inverse de celui d'un maximum : sur une semaine
tronquée le total est mécaniquement sous-évalué et la règle signalerait un déficit qui n'existe
pas. Un maximum peut se permettre de mesurer ce qu'il reçoit — il sous-détecte, ce qui est sans
danger. Un minimum, non.

#### Volume et enchaînement, deux règles distinctes sur les nuits

`NuitsMaximumParSemaine` borne un **volume** hebdomadaire ; `NuitsConsecutivesMax` borne un
**enchaînement**. Trois nuits lundi, mercredi et vendredi ne violent aucun enchaînement mais
peuvent dépasser un volume de deux. Deux clés, deux motifs.

La règle s'est vérifiée sur SC-03 sans jamais apparaître au `scoreBreakdown` : la nuit du
vendredi est allée à SAL-2002, dont le plafond est 3, et jamais à SAL-2001, plafonné à 0. Une
contrainte muette parce qu'elle est respectée — la bonne signature.

#### Aucun motif SC-06 pour le sous-emploi, et c'est volontaire

Le classement SC-06 lève un motif quand un candidat **aggrave** la situation de référence. Or
confier le besoin à un salarié *réduit* son déficit : le delta est toujours favorable, le motif
ne serait jamais levé. Pire, il serait sémantiquement inversé — un salarié sous-employé est un
**bon** candidat, pas un motif de rejet.

### 7.8 — Nettoyage : le code mort, les seuils orphelins, la règle en onze exemplaires

**Aucun écart de score — mesuré.** 519 tests, 0 échec (510 avant). Aucune contrainte n'a changé
de comportement : le lot ne fait que supprimer et unifier.

| Livrable | Fichier |
|---|---|
| Règle de repli unique | `domain/creneau/CodeActivite.java` (créé) |
| Délégation des deux porteurs | `domain/creneau/Creneau.java`, `scenarios/dto/input/CreneauInputDTO.java` |
| Onze sites ramenés à `getCodeActiviteEffectif()` | 7 contraintes, `WorkMetricsCalculator`, `ScenarioSc01PreparationService`, `ScenarioSc03PreparationService` (×2) |
| Deux contraintes mortes supprimées | `constraints/metier/CreneauJourFerie.java`, `constraints/metier/CreneauDeNuit.java` |
| Cinq seuils orphelins retirés | `domain/contexte/SeuilsDeTolerance.java` |
| Couverture et garde anti-duplication — 9 cas | `domain/creneau/CodeActiviteTest.java` |

#### Pourquoi supprimer plutôt que réenregistrer les deux contraintes

`CreneauDeNuit` et `CreneauJourFerie` n'étaient déclarées dans aucun `ConstraintProvider` depuis
la décision consignée en `20_DECISIONS_CONCEPTION_OPTAPLANNER.md` : les pénibilités sont calculées
par `PenibilitesLegalesMinutes` à partir de `TimeBreakdownCalculator`.

La différence n'est pas cosmétique. `CreneauDeNuit` pénalise `creneau.getDuree()` — la **durée
entière** du créneau — dès que le drapeau `segmentNuit` est levé, avec un poids fixe de 1. Le
breakdown mesure les minutes qui tombent réellement dans la plage de nuit, en répartissant de part
et d'autre de minuit, applique le poids configurable par stratégie, et fait jouer les règles de
dominance pour qu'une minute nuit + dimanche soit comptée une fois et non deux. Sur un créneau
20:00–23:00 déclaré nuit, l'ancienne compte 180 minutes, la nouvelle 60. **Les réenregistrer
doublerait le comptage.**

`CreneauJourFerie` est morte deux fois : son filtre teste `getQualificationJour() == FERIE`, et le
mapper écrit `OUVRE` en dur. Elle ne matcherait rien même enregistrée. Ce constat a conduit à la
découverte décrite au §6.1 — la valorisation du férié ne fonctionne pas davantage sur le chemin
censé la remplacer, mais la réponse n'est pas de ressusciter cette classe.

#### Les cinq seuils : supprimés parce qu'ils étaient devenus faux, pas seulement inutiles

Aucun constructeur de `SeuilsDeTolerance` ne les prenait en argument : ils valaient 0 en
production, et neutralisaient les contraintes qui les lisaient. Les lots S7.0 à S7.7 les ont
rapatriés dans `ContraintesReglementairesSalarie`, alimentés par le contrat, un seuil par salarié.
Les laisser en place aurait offert à un futur lecteur une valeur plausible et fausse — un zéro qui
ressemble à une borne. Vérification faite avant retrait : zéro appelant, en production comme en
test.

#### Onze sites, pas dix

Le §5 recensait dix exemplaires de la règle en comptant les seules contraintes. Le recensement
complet en donne onze, les deux sites supplémentaires étant hors contraintes :
`WorkMetricsCalculator` — donc la **réponse API** — et les services de préparation SC-01 et SC-03.
La règle arbitrait donc à la fois ce qui est compté au score et ce qui est restitué au client,
depuis des copies indépendantes.

Les deux porteurs — l'entité `Creneau` et le DTO d'entrée — exposent chacun un
`getCodeActiviteEffectif()` qui délègue à `CodeActivite.effectif(...)`. Le DTO en a besoin parce
que SC-01 et SC-03 arbitrent l'activité **avant** que l'entité n'existe. Son accesseur porte
`@JsonIgnore` : une propriété dérivée ne doit pas élargir la surface acceptée par le désérialiseur
strict.

#### Une équivalence à vérifier plutôt qu'à supposer

L'expression en ligne rendait `activite` telle quelle, y compris une chaîne blanche ;
`CodeActivite.effectif` rend `null`. Les appelants ont été repris un par un :
`ref.getByCode("")` et `ref.getByCode(null)` rendent tous deux `null` — comportement identique
pour les neuf jointures référentiel. Pour `WorkMetricsCalculator` et SC-01, le garde
`code == null || code.isBlank()` se réduit à `code == null`. Pour SC-03, `auMoinsUneRessourceCompatible`
n'est appelé que sur des créneaux déjà validés contre le référentiel : le code y est nécessairement
non nul.

Le bloc de diagnostic de SC-03 était le seul cas structurellement différent — un `if/else` porteur
de deux avertissements distincts. Il est réécrit sans réénoncer la règle : le repli s'est produit
si le code retenu existe et n'est pas la clé du contrat.

#### Le test de garde

`CodeActiviteTest.Unicite` parcourt `src/main/java` et échoue si une classe de production réécrit
`getCodeActiviteId().isBlank()`. Aucune liste d'exemptions n'est nécessaire : `CodeActivite` teste
ses paramètres, pas un getter. Cette garde est la leçon du chantier — la dormance de six mois n'a
pas été causée par une règle fausse, mais par une règle **juste ailleurs**. Un test qui interdit la
copie protège mieux qu'un test qui vérifie la copie.

### 7.9a — Le jour férié cesse d'être invisible

**Écart de score : SC-03 passe de −66 960 à −67 440.** Voulu, mesuré, garde-fou mis à jour.
529 tests, 0 échec (519 avant).

| Livrable | Fichier |
|---|---|
| Reconstitution du calendrier | `domain/reglementaire/CalendrierJoursFeries.java` (créé) |
| Paramètres assortis du calendrier | `domain/reglementaire/RegulatoryParameters.java` — `avecJoursFeries(...)` |
| Branchement des trois scénarios | `ScenarioSc01/Sc03/Sc06PreparationService.java` |
| Couverture dédiée — 10 cas | `domain/reglementaire/CalendrierJoursFeriesTest.java` |
| Garde-fou de score **et de métrique** | `scenarios/sc03/api/ScenarioControllerSc03RuntimeTest.java` |

#### Ce qui ne marchait pas

`TimeBreakdownCalculator` interroge `RegulatoryParameters.estJourFerie(date)`. Les trois
services de préparation construisaient `RegulatoryParameters.neutre()`, dont la liste
`joursFeries` est vide. **Aucune minute n'a donc jamais été comptée comme fériée**, depuis
l'origine, pour aucun client : ni pénalité `LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES`, ni
`workMetrics.heuresJourFerie`, resté à 0.0 dans toutes les réponses.

Le défaut était visible dans le jeu de référence lui-même : `CRE-ME-01`, mercredi 13 mai,
8 heures travaillées, `isJourFerie: true`. Il produisait 0.0 heure fériée.

L'**interdiction**, elle, fonctionnait : `JourFerieRefuse` (HARD) lit `Creneau.jourFerie` et
croise `travailleJourFerie` du salarié. C'est ce qui rendait le défaut plausible — le férié
semblait pris en compte, puisqu'il changeait les affectations.

#### Le contrat portait déjà la réponse, à deux endroits

Rien n'a été ajouté au contrat. La spécification d'interface décrivait déjà l'écart, dans la
description du schéma `PlanningContext` : « `regulatoryParameters` […] pas dans
`PlanningContextDTO` en V1. Le DatasetBuilder les construit avec des **valeurs par défaut** […]
à intégrer en Phase 3+ ». Ces valeurs par défaut portaient un nom dans le code — `neutre()` — et
un calendrier vide.

Deux sources existantes ont donc suffi :

* **SC-01** transmet `scenarioParameters.holidayDates`. Le champ servait déjà, mais uniquement à
  *ne pas générer* de créneau ces jours-là. Il alimente désormais aussi la valorisation.
* **SC-03 et SC-06** ne transmettent aucun calendrier. La seule information disponible est le
  drapeau `isJourFerie` porté par chaque créneau, que le contrat qualifiait d'« INDICATIF (non
  réglementaire) » en désignant `RegulatoryParameters` comme source de vérité. La source
  désignée étant vide, c'était le champ dit indicatif qui portait seul la règle qui marchait.
  **La documentation a été corrigée dans ce sens** : pour SC-03 et SC-06, `isJourFerie` fait
  autorité.

#### Un créneau suffit à qualifier la journée

Le férié est une propriété de la **date**, pas du créneau : s'il est férié pour l'un, il l'est
pour tous. Marquer un seul créneau d'une date suffit donc à qualifier la journée entière, pour
tous les salariés. Une déclaration partielle — courante quand le planning amont vient de
plusieurs sources — est réparée plutôt que subie.

#### Limite assumée, et ce qu'il faudrait pour la lever

Un créneau qui traverse minuit n'est rattaché qu'à sa **date de début** : le drapeau ne dit pas
lequel des deux jours civils est férié. Un 22:00–06:00 déclaré férié ne verra valorisées que ses
minutes d'avant minuit. La plage de nuit reste par ailleurs figée à 22:00–06:00.

Lever ces deux limites suppose d'ouvrir `planningContext.regulatoryParameters` au contrat —
emplacement déjà spécifié, jamais implémenté. Ce n'était pas nécessaire pour rendre le férié
opérant, et la cible reste inchangée.

#### Le score et la métrique sont désormais tous deux gardés

`−67 440 = −1 440` pénibilités `− 66 000` sous-emploi. Les pénibilités se décomposent, à poids 1
en `ANALYSE_RH`, en 480 minutes de nuit, 480 de dimanche et **480 de férié** — ces dernières
nouvelles. Aucune affectation ne bouge : `SAL-2001` refusait déjà le créneau du 13 mai.

Le garde-fou de SC-03 assied désormais aussi `heuresJourFerieTotales` à 8.0 et la répartition
entre les deux salariés. L'absence de toute assertion sur cette métrique est précisément ce qui
a permis au défaut de survivre.

### 7.9b — Le repos hebdomadaire, enfin nominatif

**Aucun écart de score — mesuré.** 554 tests, 0 échec (529 avant). Les jeux d'essai ne déclarent
aucun code de repos et aucune activité générant une dette : la contrainte est en service et
reste muette faute de situation à juger, ce qui est la bonne signature.

| Livrable | Fichier |
|---|---|
| Le fait « ce salarié se repose ce jour-là » | `domain/repos/ReposHebdomadaire.java` (créé) |
| Construction du calendrier | `domain/repos/CalendrierReposHebdomadaire.java` (créé) |
| Déclaration des codes au référentiel | `domain/metier/ReferentielComptabiliteActivite.java`, `scenarios/dto/input/ReferentielsDTO.java` |
| Contrainte réécrite | `constraints/metier/DetteReposSurReposHebdomadaire.java` |
| Extraction et restitution | `ScenarioSc01/Sc03/Sc06PreparationService.java`, `ScenarioResponseMapper.java` |
| Couverture — 14 + 11 cas | `domain/repos/CalendrierReposHebdomadaireTest.java`, `constraints/DetteReposSurReposHebdomadaireConstraintsTest.java` |

#### La contrainte demandait une chose impossible

Elle exigeait qu'un créneau soit **lui-même** qualifié `RH`/`RHD` *et* que son activité compte
dans la charge. Un repos n'étant pas du travail, les deux conditions ne pouvaient pas être vraies
ensemble. Elle lisait de surcroît `Creneau.qualificationJour`, champ qu'aucun mapper n'alimente —
tous écrivent `OUVRE` en dur, « non computable depuis le DTO seul ».

Elle croise désormais **deux objets distincts** : le créneau de travail, et le fait qui dit quel
jour ce salarié-là se repose. C'est la même erreur de structure que le premier jet de S7.7b, où
un seul objet devait porter deux rôles incompatibles.

#### Le client déclare ses propres identifiants

L'identifiant d'activité valant repos **change d'un client à l'autre** : le moteur ne connaît
aucun code en dur, et `RH` n'est pas un mot réservé. Deux champs le déclarent, dans
`dataSet.referentiels` — là où le contrat disait déjà que le repos hebdomadaire est qualifié par
le référentiel d'activités :

```json
"referentiels": {
  "activites": [ … ],
  "codeActiviteReposHebdomadaire": "10450",
  "codeActiviteReposHebdomadaireDimanche": "10451"
}
```

Non déclarés, aucun créneau n'est un marqueur et le calendrier retombe entièrement sur le repli.

#### Un repos est un fait, pas un créneau

C'est la décision structurante du lot, et elle est imposée par les chiffres. Un repos couvre la
journée entière, 00:00–23:59, soit **1 439 minutes**. Introduit comme créneau, il violerait
immédiatement trois contraintes HARD de `LimitePhysique`, qui ne consultent pas le référentiel et
ne savent donc pas distinguer un repos d'un travail :

| Contrainte | Effet |
|---|---|
| `dureeMaxCreneau` | pénalise `durée − 720` → **719 points HARD**, à lui seul |
| `pasDeChevauchement` | tout autre créneau du salarié ce jour-là devient HARD |
| `cumulJournalierMax` | 1 439 + quoi que ce soit dépasse 1 440 → HARD |

Tout planning contenant un repos serait devenu infaisable. Réduit à une date et une nature, le
repos n'a plus d'horaires et le problème disparaît.

Ce choix en règle trois autres du même coup. Les contraintes physiques n'ont **aucun filtre à
ajouter** — c'est la duplication que S7.8 venait de supprimer. La question du figeage en SC-03
s'évanouit : un fait n'est pas une variable de décision, donc le solveur ne peut pas confier le
repos d'une personne à une autre. Et `ressourceAffecteeId`, que SC-03 ignore par construction sur
les créneaux, est lu à l'extraction, où il est disponible.

#### Le repli s'applique par salarié et par semaine

Une semaine sans marqueur retombe sur samedi et dimanche, **même si le salarié en déclare
ailleurs**. La maille alternative — faire confiance aux déclarations partout dès qu'il en existe
une — rendrait une semaine oubliée silencieusement travaillable. Un vide ne suppose jamais que la
chose est possible.

#### Les marqueurs sont restitués

L'appelant recharge la réponse pour réafficher son planning : un repos absent y ferait un trou.
Les marqueurs reviennent donc dans `planning`, **et là seulement** — ils sont exclus des
diagnostics, des `workMetrics` et du résumé, un repos n'étant ni une affectation décidée par le
moteur ni de la charge.

SC-06 fait exception : sa réponse ne contient que les créneaux du besoin, pas le planning
complet. Rien à y restituer.

#### Trois scénarios, trois situations

* **SC-03** — les marqueurs sont extraits **avant** la partition « activité connue ». Leur
  déclaration au bloc `referentiels` vaut déclaration : les faire passer par ce contrôle les
  aurait comptés en `activiteInconnue` et jetés. L'extraction précoce évite aussi les gardes
  horaires — un repos couvre la journée, pas une plage.
* **SC-06** — le planning figé exige déjà `ressourceAffecteeId` pour chaque créneau. Les
  marqueurs y arrivent naturellement rattachés à leur salarié : rien à changer côté contrat.
* **SC-01** — génère ses créneaux et ignore `dataSet.creneaux`. Aucun marqueur ne peut lui
  parvenir ; son calendrier se réduit au repli.

### 7.9c — Deux indicateurs de repos, deux mailles assumées

**Aucun écart de score.** 557 tests, 0 échec (554 avant). Ce lot ne touche qu'aux `workMetrics`.

`heuresReposHebdoTravaille` comptait le week-end calendaire — samedi **et** dimanche — sans
consulter le calendrier de repos. La métrique et la contrainte annonçaient donc la même règle et
n'en appliquaient pas la même dès qu'un salarié se reposait un autre jour.

Le premier correctif alignait la métrique sur le calendrier individuel. Il était faux, et
l'arbitrage l'a redressé : **un indicateur d'observation ne doit pas dépendre d'une déclaration.**
Le calendrier de repos peut être absent, partiel, ou différent d'un client à l'autre ; un
indicateur RH qui en dépend cesse d'être comparable entre salariés. Un dimanche est un fait de
calendrier ; le repos d'une personne est une déclaration. Les deux ne se mélangent pas.

| Indicateur | Maille | Pourquoi |
|---|---|---|
| `heuresReposHebdoTravaille` | **dimanche calendaire** | fait objectif, comparable entre salariés et entre clients, même sans repos déclaré |
| `nbCreneauxReposHebdoDetteRepos` | **calendrier de repos du salarié** | contrepartie observée de `DetteReposSurReposHebdomadaire` — même lecture que le score |

Le samedi sort donc de `heuresReposHebdoTravaille`, où il entrait depuis l'origine. Aucun jeu
d'essai n'a de créneau le samedi : la valeur restituée ne bouge sur aucun scénario existant.

La divergence entre les deux indicateurs est voulue et documentée : le premier répond à « combien
d'heures le dimanche », le second à « combien de fois ce salarié a-t-il travaillé son repos ».
Ce sont deux questions différentes.

---

## 8. Suite du chantier — le cadre réglementaire au contrat

### 8.0 — `planningContext.regulatoryParameters`

**Aucun écart de score — mesuré.** 568 tests, 0 échec (557 avant). Le bloc étant facultatif et
absent de tous les jeux d'essai, le comportement du lot S7.9a est intégralement conservé.

| Livrable | Fichier |
|---|---|
| Bloc d'entrée | `scenarios/dto/RegulatoryParametersDTO.java` (créé), `scenarios/dto/PlanningContextDTO.java` |
| Résolution de la précédence | `scenarios/mapper/ScenarioRegulatoryParametersMapper.java` (créé) |
| Branchement des trois scénarios | `ScenarioSc01/Sc03/Sc06PreparationService.java` |
| Contrat | `50_openapi_windev_moteur_v_1.yaml`, `50_ScenarioContract.schema.json` |
| Couverture — 11 cas de précédence | `scenarios/mapper/ScenarioRegulatoryParametersMapperTest.java` |

#### Un bloc annoncé depuis l'origine, jamais implémenté

La spécification d'interface le disait déjà, dans la description du schéma `PlanningContext` :
« `regulatoryParameters` […] pas dans `PlanningContextDTO` en V1. Le DatasetBuilder les construit
avec des **valeurs par défaut** […] à intégrer en Phase 3+. » Ces valeurs par défaut portaient un
nom dans le code — `neutre()` — une plage de nuit figée à 22:00–06:00 et un calendrier de jours
fériés **vide**. C'est ce calendrier vide qui a rendu la valorisation du férié inopérante jusqu'au
lot S7.9a.

#### Précédence : déclaré d'abord, déduit ensuite

Un point unique la décide, partagé par les trois scénarios — la règle ne peut pas diverger de
l'un à l'autre.

**Plage de nuit.** Déclarée si les **deux** bornes le sont, sinon la plage légale par défaut. Une
borne seule est ignorée et tracée : mélanger une borne déclarée avec une borne par défaut
produirait un intervalle que personne n'a voulu — un client qui écrit « 21:00 » n'a pas demandé
21:00–06:00. Deux bornes identiques décrivent une plage vide et sont refusées de la même façon.

**Jours fériés.** Le calendrier déclaré fait autorité dès qu'il est **présent**, fût-il vide : une
liste vide dit « aucun jour férié sur la période ». C'est son *absence* qui laisse le moteur
déduire, depuis `holidayDates` en SC-01 ou le drapeau `isJourFerie` en SC-03 et SC-06.

Aucune fusion entre les deux sources. Une divergence est **tracée**, jamais absorbée : fusionner
rendrait fériée une date que l'appelant n'a pas retenue, et le calendrier réglementaire cesserait
d'être une réponse à la question « quels sont les jours fériés ».

#### Ce que cela corrige

La **plage de nuit** cesse d'être figée. Un établissement dont la nuit conventionnelle est
21:00–06:00 — celle du Code du travail — ne pouvait le déclarer nulle part.

Un **créneau traversant minuit** peut enfin être qualifié correctement. La limite n'était pas dans
le calcul : `TimeBreakdownCalculator` interroge séparément le jour de début et le jour suivant, et
un test le prouvait déjà. Elle était dans la **déduction** — le drapeau `isJourFerie` est porté par
le créneau, pas par le jour, et ne dit donc pas lequel des deux jours civils est férié. Un
calendrier de dates lève cette limite ; un test la documente désormais explicitement du côté de la
déduction.

#### Ce qui reste ouvert

`heureDebutNuit` et `heureFinNuit` **portés par le salarié** restent transportés et lus par
personne. `SalarieReel` expose pourtant `heureDebutNuitEffective(fallback)` et
`heureFinNuitEffective(fallback)` — écrites, jamais appelées — et le contrat qualifie ces champs
d'« override du paramètre réglementaire global ».

Les brancher rendrait la pénibilité d'un créneau **dépendante de qui l'exécute** : les mêmes
heures ne produiraient pas les mêmes minutes de nuit selon le salarié affecté. C'est défendable —
la plage de nuit d'un travailleur de nuit est contractuelle — mais cela change la nature du score
et demande un arbitrage explicite.
