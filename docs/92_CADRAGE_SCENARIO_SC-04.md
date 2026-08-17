# 92 — Cadrage : SC-04, optimisation globale d'un planning existant

> **Statut** : cadrage d'analyse, 2026-08-17, produit à l'ouverture du **rang 9**.
> **Les six arbitrages de la §5 sont tranchés** (métier, 2026-08-17). Plus aucun ne bloque SC-04 :
> ce qui reste est du travail moteur. Les lots **O0 et O1 sont livrés** le jour même.
>
> Ce cadrage a déjà produit un correctif hors de son périmètre : le **rang 14**, livré le
> 2026-08-17, parce qu'on ne peut pas optimiser une période large en lisant les congés comme du
> temps disponible.
>
> Ce document ne modifie ni `50_SCENARIO_CONTRACT.md`, ni le code. Il pose les questions au niveau
> où elles se tranchent.

---

## 1. Intention métier

`50_SCENARIO_CONTRACT.md` §3.4 tient en une phrase :

> **Améliorer un planning réel sans le reconstruire entièrement.**

et en trois listes : un degré de liberté et des priorités d'optimisation en paramètres, le planning
existant et un « historique des compteurs » en données, un planning optimisé avec gains et
régressions explicitées en restitution.

Douze lignes au contrat, et rien d'autre — ni schéma, ni endpoint, ni jeu d'essai. **C'est le seul
scénario annoncé qui n'a jamais été écrit.**

---

## 2. Positionnement — ce que SC-04 apporte et que nul autre n'apporte

Les scénarios de base ne se recouvrent pas : chacun apporte un élément que les autres n'ont pas.

| | Son élément propre |
|---|---|
| SC-01 | générer depuis rien |
| SC-02 | réagir à un aléa sans casser l'existant |
| SC-03 | réaffecter un sous-ensemble désigné |
| **SC-04** | **la profondeur temporelle — juger sur une période, pas sur une semaine** |
| SC-05 | arbitrer entre deux pairs sur un périmètre borné |
| SC-06 | classer des candidats pour un besoin |

### 2.1 Une objection, et pourquoi elle tombe

On peut lire SC-04 comme la **généralisation** de SC-02, SC-03 et SC-05 : ces trois-là ne sont
après tout que SC-04 avec un degré de liberté préréglé — SC-02 libère les créneaux de l'absent,
SC-05 ceux du périmètre, SC-03 un sous-ensemble, et tous épinglent le reste. Si c'était vrai, SC-04
n'apporterait qu'un réglage, et les trois cas particuliers auraient été construits d'abord parce que
leur préréglage se justifie sans arbitrage.

**Ce n'est pas la bonne lecture.** L'élément propre de SC-04 n'est pas la liberté laissée au
solveur : c'est la **largeur de la période jugée**. Les cinq autres scénarios répondent à une
question posée sur un instant — une absence, un besoin, un périmètre. SC-04 est le seul qui demande
au moteur de juger une durée, et de voir dans cette durée ce qu'une semaine ne montre pas.

Ce qui suit en découle : les deux difficultés que l'on croit tenir à l'optimisation — le degré de
liberté et la pondération des règles — sont **accessoires**, et la seule vraie question est celle de
la période.

---

## 3. Principe structurant — l'historique ne se reçoit pas, il se recalcule

Le contrat annonce un « historique des compteurs » en donnée d'entrée, et le rang 9 du suivi en a
fait pendant des mois la cause du blocage : *SC-04 dépend d'un historique des compteurs qui
n'existe pas*.

**L'arbitrage du 2026-08-17 renverse cette lecture.** La profondeur est celle de la **période
demandée**, avec un regroupement des données à la semaine, au mois, et sur la période entière. Le
moteur ne lit donc aucun compteur tenu ailleurs : il **recalcule** tout depuis les créneaux qu'on
lui transmet, sur une fenêtre que l'appelant élargit à la profondeur qu'il veut voir.

Trois conséquences, et elles réorientent tout le chantier :

1. **Il n'y a aucun bloc d'entrée à recevoir.** Pas de structure `historiqueCompteurs`, pas de
   solde, pas de date de remise à zéro — rien n'est conservé entre deux appels, donc rien n'est à
   réinitialiser. La question « qui remet les compteurs à zéro, et quand » est sans objet.
2. **Le blocage n'est pas chez WinDev.** Il est chez le moteur, qui ne sait produire qu'un agrégat
   unique par salarié sur tout l'horizon.
3. **Le levier de profondeur est déjà documenté** : *plus la fenêtre transmise est large, plus la
   mesure a de sens* — `92_CADRAGE_WORKMETRICS_EQUITE.md` §4.3, et la règle de transmission
   J-7 / J+7 qui en découle.

---

## 4. Ce que SC-04 exige du moteur

| Exigence | État |
|---|---|
| Une période large | ✅ `planningContext.horizon`, au contrat |
| Le planning existant sur toute sa largeur | ✅ au contrat depuis le lot L8 (2026-08-10) |
| Épingler une partie du planning | ✅ `@PlanningPin`, livré au lot S1 de SC-06 — mais **le moteur décide seul** de ce qui est épinglé (SC-02, SC-05) ; l'appelant ne le désigne pas |
| Ne pas lire une absence comme une sous-charge | ✅ **livré le 2026-08-17, rang 14** |
| WorkMetrics agrégés semaine / mois / période | ✅ **livré le 2026-08-17, lot O1** — côté domaine |
| Ces agrégats **restitués**, avant et après | ❌ lot O2 |
| Un score qui juge l'équité sur cette période | ✅ **livré le 2026-08-17, lot O0** |

`WorkMetricsByRessourceDTO` porte **un seul agrégat par ressource**, borné par `periodeDebut` /
`periodeFin`. C'était là, et nulle part ailleurs, que se situait le travail de SC-04.

### 4.2 Ce que le lot O1 a livré

`DecoupageTemporel` découpe l'horizon en semaines ISO — le lundi ouvre, comme partout ailleurs dans
le moteur — puis en mois calendaires, puis en lui-même. Les bords sont **tronqués, jamais étendus**,
et la tranche se déclare alors `partielle()` pour que ses volumes bruts ne se comparent pas à ceux
d'une semaine pleine.

`WorkMetricsParTranche` mesure chaque tranche en **rejouant le calculateur de production** sur une
vue du problème dont seul l'horizon est rétréci — `PlanningContext.surHorizon(...)`. Écrire un
agrégateur qui aurait additionné les créneaux lui-même était plus court et faux : la pondération par
la pénibilité, la dominance, le filtre `compteDansCharge` et la déduction des absences offrent
**quatre** occasions de diverger, et une divergence de méthode se lirait comme un mouvement du
planning. C'est §5.1 appliqué.

Tout suit la fenêtre, y compris ce qui s'en déduit : les jours disponibles d'une semaine où le
salarié a posé deux jours valent cinq, et son écart au contrat s'y proratise. **Une semaine de congé
complet ne le montre pas à −100 %** — elle ne le compare à rien.

> ⚠️ **Ce qui ne se lit pas tranche par tranche.** Les volumes se somment ; les **séries** non.
> `maxJoursConsecutifsObservees` et `maxNuitsConsecutivesObservees` sont observés *à l'intérieur* de
> la tranche : sept jours consécutifs à cheval sur deux semaines s'y lisent 4 puis 3. Seule la
> tranche `PERIODE` porte la série réelle, et c'est elle qu'il faut lire pour juger d'un
> enchaînement. Un test pin ce comportement pour qu'il ne se prenne jamais pour un défaut.

### 4.1 Pourquoi le rang 14 est sorti de ce cadrage

Le moteur ne place aucun créneau dans un congé — la contrainte HARD `METIER_HARD_INDISPONIBILITE`
y veille depuis l'origine. Mais il proratisait le contrat sur l'horizon **entier** : une absence se
lisait comme du temps disponible non travaillé, et le salarié revenant de congé apparaissait sous
son contrat, donc préférable.

Sur une semaine, l'effet est du bruit. **Sur un trimestre, il devient systématique** — et SC-04 ne
propose rien d'autre que de juger sur un trimestre. Le défaut lui préexistait ; SC-04 le rendait
inévitable. Il a donc été corrigé avant, non pendant : voir le rang 14 du suivi.

---

## 5. Arbitrages

### 5.1 Tranché — les mêmes axes que WorkMetrics

> *« On ne peut pas comparer des torchons et des serviettes. »* — métier, 2026-08-17

Les agrégats de SC-04 portent sur les axes de `WorkMetrics` et sur aucun autre. C'est la même règle
que le lot A2 de SC-05 a appliquée pour l'avant/après : **un seul calculateur, jamais deux**. La
pondération par la pénibilité, la fenêtre d'observation et le filtre `compteDansCharge` offrent
trois occasions de diverger, et une divergence de méthode se lirait comme un mouvement du planning.

### 5.2 Tranché — la profondeur est celle de la période demandée

Regroupement à la **semaine**, au **mois**, et **sur la période**. Rien n'est stocké entre deux
appels. Voir §3.

### 5.3 Tranché — comparer les éléments comparables entre eux

L'avant et l'après se comparent **à granularité identique** : semaine contre semaine, mois contre
mois, période contre période. C'est ce qui donne enfin un contenu à la mention « gains / régressions
explicitées » du contrat, restée jusqu'ici sans définition.

### 5.4 Tranché — une absence est une absence

> *« On ne peut pas optimiser un planning en disant : j'annule les congés. »* — métier, 2026-08-17

Deux exigences distinctes en découlent, et une seule était satisfaite :

* **ne pas affecter pendant une absence** — tenu depuis l'origine par la contrainte HARD ;
* **ne pas lire une absence comme un déficit** — faux jusqu'au 2026-08-17, corrigé au rang 14.

Compenser une absence de part et d'autre est une manière de l'annuler : le refus vaut pour les deux.

### 5.5 Tranché — une date pivot, et la liste explicite dans un second temps

Tous les scénarios livrés décident **pour l'appelant** de ce qui est épinglé. SC-04 est le premier
où ce choix lui revient, et c'est lui qui décide si le résultat est exploitable ou un remaniement
que personne ne voulait.

| Forme | Sort |
|---|---|
| **Date pivot** — avant = figé, après = ajustable | ✅ **retenue** (métier, 2026-08-17) |
| **Liste explicite** de créneaux ajustables | reportée — *« ajustable dans un second temps »* |

SC-04 sert donc à **corriger la suite au vu du passé** : un seul champ, déductible, cohérent avec
une période qui couvre du passé et du futur. Le moteur sait déjà épingler créneau par créneau
depuis le lot S1 de SC-06 ; seule la règle qui désigne manquait.

La liste explicite n'est pas écartée, elle est **différée**. Elle reste compatible : elle
s'ajouterait à côté de la date pivot, pas à sa place, et un appelant qui ne la transmet pas
retomberait sur le pivot.

### 5.6 Tranché — la pondération des règles est reportée, les poids restent fixes

Le contrat annonce « priorités d'optimisation ; pondération des règles ». Ce serait le premier
scénario où l'appelant règle la fonction de score — `strategieScoring` est aujourd'hui un enum à
poids fixes.

**Le moteur porte une doctrine contraire**, posée au lot L5 du chantier équité : *on ne pondère pas
une mesure dont l'échelle n'est pas calibrée*. Les coefficients de pénibilité ne le sont pas —
`92_CALIBRATION_PENIBILITE.md`.

**Reporté (métier, 2026-08-17) : les poids restent fixes.** À poids fixes, SC-04 reste SC-04 — la
pondération est un artefact du titre « optimisation globale », pas l'élément propre du scénario.
Elle ne conditionne donc pas sa livraison.

⚠️ **Conséquence sur le contrat d'entrée.** `50_SCENARIO_CONTRACT.md` §3.4 annonce ces « priorités
d'optimisation » aux paramètres de SC-04. À l'inscription (lot O4), soit elles sont retirées de
l'annonce, soit elles y figurent comme différées — mais **elles ne peuvent pas rester annoncées
sans effet** : ce serait le rang 8 recommencé, un champ au contrat que rien ne lit.

---

## 6. Contrat d'entrée proposé

Un seul champ nouveau, le degré de liberté (§5.5) :

| Champ | Origine |
|---|---|
| `planningContext.horizon` | existant — c'est lui qui porte la profondeur |
| `dataSet` complet, planning existant inclus | existant |
| `dataSet.indisponibilites` | existant — désormais déduit de la mesure |
| `scenarioParameters.datePivot` | **nouveau** — seul champ à créer. Avant : figé. À partir de : ajustable. §5.5 |

`scenarioType: "SC-04"` figure déjà à l'énumération de `50_ScenarioContract.schema.json`.

---

## 7. Contrat de sortie proposé

Le bloc `arbitrage` de SC-05 est le précédent le plus proche : il publie déjà un avant/après par
personne, mesuré par le même calcul. SC-04 en demande l'équivalent **par personne et par période**.

Forme envisagée — un bloc `optimisation` portant, pour chaque salarié, la série des agrégats
`WorkMetrics` par semaine, par mois et sur la période, en deux états. Les motifs disqualifiants de
SC-05 (`MotifArbitrage`) offrent le modèle de la lecture : le moteur ne refuse pas, il rend visible.

À détailler au lot de réalisation, une fois §5.5 tranché.

---

## 8. Découpage proposé

| Lot | Contenu | Dépend de |
|---|---|---|
| ~~**O0**~~ | ~~Aligner `EquiteChargeAuContrat` sur les jours disponibles~~ ✅ **livré le 2026-08-17** | — |
| ~~**O1**~~ | ~~Agrégation `WorkMetrics` par semaine / mois / période, côté domaine~~ ✅ **livré le 2026-08-17** | O0 |
| **O2** | Restitution des séries agrégées, avant/après, au contrat de sortie | O1, §5.3 |
| **O3** | Degré de liberté : `datePivot` et figement dérivé | O1, §5.5 |
| **O4** | Endpoint, jeux d'essai, canal fichier, inscription série 50 | O2, O3 |

**Plus aucun lot n'attend d'arbitrage.** O2 et O4 touchent la série 50, donc ce que lit WinDev ;
O3 est interne. Sur O4, ne pas oublier le sort des « priorités d'optimisation » annoncées au §3.4
du contrat et reportées en §5.6 — un champ annoncé sans effet est un rang 8 de plus.

---

## 9. Points ouverts

### 9.1 ~~Le second volet du rang 14~~ — ✅ clos, lot O0 du 2026-08-17

`EquiteChargeAuContrat` appelait `minutesAttendues` avec l'horizon nu, et le score contredisait donc
la mesure depuis le correctif du matin même. C'est réparé.

**Ce que la réalisation a dû trancher, et qui n'était pas dans le cadrage.** Le compte de jours
arrive par une **jointure**, et une jointure OptaPlanner est *interne* : un salarié sans fait
correspondant sortirait de la contrainte, et l'équité serait **silencieusement désactivée** pour
lui — pire que le défaut corrigé. Poser le fait depuis les services de préparation, comme
`ReposHebdomadaire`, exposait exactement à cet oubli.

`JoursDisponiblesSalarie` est donc **dérivé** par `PlanningProblem` lui-même, sur le modèle de
`getWorkMetrics()` que le projet annotait déjà en getter. Aucun service ne peut l'oublier puisqu'
aucun ne le pose. Le compte est mémorisé — un fait de problème qui change d'identité entre deux
lectures corrompt le score — et les trois setters dont il dépend l'invalident, les préparations
posant les indisponibilités après la construction.

**Le second volet demandait autre chose.** Il notait −100 % le salarié « qui ne travaille rien du
tout », donc désignait le salarié **absent toute la période** comme le plus sous-chargé de tous : il
se serait vu rattraper son arrêt maladie dès le premier créneau libre. Il en est exclu. Absent d'une
partie seulement, il reste jugé — il avait des jours pour travailler et n'a rien fait, ce que
l'équité doit continuer de voir. **La déduction n'est pas une excuse générale.**

### 9.2 L'annualisation

Plus la fenêtre est large, plus la mesure traite l'annualisation sans cas particulier : une semaine
au-dessus de la moyenne n'est pas une anomalie, c'est l'objet même de l'annualisation. SC-04 est
donc le scénario qui en profite le plus — mais l'annualisation proprement dite relève du **rang
10**, et son arbitre est la Production.

### 9.3 Ce que la période ne montre toujours pas

Le moteur ne verra jamais que ce qu'on lui transmet. Élargir la période élargit ce qu'il voit ; elle
ne lui apprend rien sur ce qui précède la fenêtre — un changement de contrat, une reprise après
arrêt long, un solde d'annualisation venu de la paie. C'est la limite assumée du principe §3 : elle
se déplace avec la fenêtre, elle ne disparaît pas.
