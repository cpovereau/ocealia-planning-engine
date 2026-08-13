# 92 — Cadrage : SC-05, arbitrage de répartition entre deux salariés

> **Statut** : cadrage d'analyse, 2026-08-13, produit au lot **L6** du chantier équité.
> Les arbitrages de la §5 sont **tranchés** (métier, 2026-08-13). Les lots **A0 et A1 sont livrés**
> (2026-08-14) ; il reste A2 à A4.
>
> Ce document ne modifie ni `50_SCENARIO_CONTRACT.md`, ni le code : l'inscription de SC-05 au
> contrat fonctionnel est portée par le lot **A4**.

---

## 1. Intention métier

> Deux salariés se partagent un même périmètre de travail. **Comment le répartir équitablement,
> et que coûte l'arbitrage à chacun ?**

`50_SCENARIO_CONTRACT.md` §3.5 en donne l'intention depuis l'origine : *arbitrer une répartition
équitable ou optimale entre deux salariés concurrents pour un même périmètre*. Le moteur **ne crée
aucun besoin** — il redistribue ce qui existe.

### 1.1 Pourquoi maintenant

SC-05 attendait une brique que le moteur n'avait pas : **de quoi comparer deux personnes**. Le
backlog le disait sans détour — *SC-05 dépend de WorkMetrics d'équité non implémentées*. Le
chantier équité les a livrées :

| Lot | Ce qu'il apporte à SC-05 |
|---|---|
| **L1** | l'unité — l'heure pondérée par la pénibilité, sans quoi on compare des durées et non des charges |
| **L2** | la mesure comparative — l'écart signé au contrat de chacun, et non à la moyenne du groupe |
| **L3** | de quoi calibrer l'échelle de cette mesure sur des cas réels |
| **L4** | les trois critères de départage, et leur ordre : aptitude, partage, confort |
| **L5** | la contrainte SOFT qui rend un déséquilibre coûteux au score |

**C'est le scénario que tout ceci débloque.** Rien de ce qui suit ne demande d'inventer une
nouvelle mesure : SC-05 assemble, il n'invente pas.

---

## 2. Positionnement par rapport aux scénarios livrés

| Scénario | Différence essentielle |
|---|---|
| **SC-02** — remplacement d'un absent | part d'une **absence** et libère ce qu'elle laisse ; SC-05 part d'un périmètre que deux personnes se partagent déjà |
| **SC-03** — ajustement ponctuel | réaffecte sans désigner personne ; SC-05 borne l'arbitrage à **deux salariés nommés** |
| **SC-06** — désignation | **classe** des manières de couvrir un besoin sans rien réaffecter ; SC-05 **réaffecte** et rend une répartition |

La parenté est avec SC-02 : même mécanique — libérer, épingler le reste, laisser le solveur
décider — mais un déclencheur différent. SC-02 réagit à une absence ; SC-05 réagit à un
déséquilibre.

---

## 3. Principe structurant proposé — réaffecter, pas classer

> SC-05 rend **une répartition**, pas un podium.

C'est ce que le contrat annonce depuis l'origine (*répartition proposée, indicateurs comparatifs
A/B, justification des arbitrages, alertes d'inéquité résiduelle*), et cela commande le reste :

* le moteur **résout** — il ne fait pas l'énumération exhaustive de SC-06 ;
* c'est donc **par le score** que l'équité l'atteint, exactement comme SC-02 (lot L5) ;
* les **indicateurs comparatifs** sont ceux de `workMetrics.byRessource`, déjà livrés.

⚠️ **Conséquence à tenir** : SC-05 hérite de l'exigence posée au lot L4 — *les deux mécanismes
doivent produire le même arbitrage*. Une répartition que SC-05 juge équitable ne doit pas être
celle que les paliers de SC-06 classeraient en dernier.

---

## 4. Ce que SC-05 exige du moteur — ✅ livré au lot A0 (2026-08-14)

Une seule brique manquait, et elle était structurante.

> **Restreindre l'affectation d'un créneau à deux ressources désignées.**

Le domaine de la variable de décision est **global** : toute ressource du dataset est candidate
pour tout créneau. Rien, aujourd'hui, ne permet de dire « ce créneau ne peut revenir qu'à A ou à
B ». SC-02 s'en passe parce qu'il libère et laisse le solveur choisir librement ; SC-05 ne le peut
pas — son objet même est un arbitrage **borné à deux personnes**.

Deux formes possibles :

| Forme | Ce qu'elle donne | Ce qu'elle coûte |
|---|---|---|
| **Contrainte HARD** — un créneau du périmètre affecté hors des ressources autorisées est interdit | simple, homogène avec le reste du moteur | le solveur explore des affectations qu'il devra rejeter |
| **Périmètre réduit** — ne transmettre que A et B comme ressources | aucune contrainte nouvelle | le reste du planning devient invisible, et les bornes hebdomadaires avec lui |

La seconde est trompeuse : sans le planning des autres, le moteur ne voit plus les créneaux qui
bornent A et B, et déclarerait conforme une répartition qui ne l'est pas. **La contrainte HARD est
la seule forme sûre.**

⚠️ **Elle porte sur un ensemble de ressources autorisées, pas sur un couple** (§5.5) : à deux,
l'ensemble a deux éléments. C'est ce qui fera de l'ouverture à N un élargissement du contrat et non
une réécriture du moteur.

### 4.1 Ce que le lot A0 a livré

`PerimetreArbitre` — fait d'entrée portant les créneaux remis en jeu et l'ensemble des ressources
autorisées — et `AffectationHorsRessourcesAutorisees`, contrainte HARD. Collection **vide** partout
ailleurs, donc contrainte inerte pour les quatre scénarios livrés.

Trois règles fixées par le lot, qui commandent la suite :

| Règle | Pourquoi |
|---|---|
| Seul un **salarié réel** est jugé | rester à pourvoir ou passer sur un poste virtuel demeure possible : le problème reste soluble |
| Un créneau **épinglé** n'est pas jugé | c'est ce qui rend l'arbitrage §5.2 tenable — sinon « épingler et signaler » deviendrait « épingler et rendre insoluble » |
| La jointure porte sur le **besoin**, pas sur le créneau | un créneau découpé garde l'identifiant de son origine ; joindre sur celui du créneau ferait échapper tous les segments, en silence |

⚠️ **Pour le lot A1** : le périmètre se transmet donc en identifiants du `dataSet`, et le découpage
éventuel n'a pas à s'en soucier.

---

## 5. Arbitrages tranchés

Rendus par le métier le 2026-08-13. Deux d'entre eux portent une **condition de retour** — ce qui
disparaît du contrat aujourd'hui est attendu demain, et c'est écrit ici pour que la suppression ne
se lise pas comme un abandon.

### 5.1 Tranché — le périmètre commun est transmis, jamais déduit

> L'appelant liste les créneaux à arbitrer : `scenarioParameters.creneauxArbitres[]`.

**Le moteur ne fabrique pas le périmètre d'un arbitrage.** Un périmètre déduit trop large
déplacerait des créneaux que personne ne voulait bouger ; trop étroit, il rendrait l'arbitrage sans
effet — et dans les deux cas l'appelant ne verrait pas pourquoi.

Conséquence pour WinDev : désigner le périmètre est de sa responsabilité, et à porter au dossier de
livraison au même titre que la semaine pleine de SC-06.

### 5.2 Tranché — un créneau tenu par un tiers est épinglé et signalé — ✅ lot A1

> Ni refus, ni reprise : l'arbitrage porte sur ce qui reste, et l'appelant apprend ce qui a été
> écarté.

Cohérent avec toute la doctrine du moteur — *l'existant ne se réécrit pas*, et un constat vaut
mieux qu'un refus. Le tiers n'a rien demandé ; on ne lui retire pas son travail pour équilibrer
deux autres personnes.

Demande une alerte dédiée, portant les identifiants des créneaux écartés (lot **A1**).

> ⚠️ **Ce que l'arbitrage protège est le travail d'une personne, pas un emplacement vide.** Un
> créneau du périmètre porté par un **poste virtuel** est donc **libéré**, pas épinglé : un poste
> virtuel n'est le travail de personne, l'argument du §5.2 ne lui vaut rien, et l'épingler rendrait
> sans effet un créneau que l'appelant a désigné. Décidé au lot A1 ; à confirmer si le cas se
> présente en production.

### 5.3 Tranché — `objectif` est supprimé, et son retour est conditionné

> L'objectif se déduit de ce que l'appelant transmet.

| Objectif annoncé | Ce qui l'exprime aujourd'hui | État |
|---|---|---|
| équité de charge | `planningContext.equite.ecartTolerePourcent` | ✅ livré au lot L5 |
| minimisation de surcharge | `scenarioParameters.surchargeMaxHeuresJour` / `...Semaine` | ✅ livré au lot S3 de SC-02 |
| respect des préférences | — | ❌ **rang 10 du backlog**, en attente de la Production |

Un enum qui double des paramètres existants finit par les contredire, et annoncer `PREFERENCES`
promettrait ce que le moteur ne peut pas tenir.

> 🔁 **Condition de retour, décidée avec la suppression.** L'enum est attendu de nouveau **quand le
> rang 10 livrera les préférences** — c'est-à-dire quand un objectif cessera d'être déductible des
> paramètres transmis. Il reviendra alors avec **une seule valeur qui ne se déduit pas**, et non
> les trois d'origine : les deux autres resteront portées par leurs paramètres.
>
> Ce n'est donc pas un abandon, c'est un ajournement. À reprendre au rang 10, pas avant.

### 5.4 Tranché — `autoriserDesequilibre` est remplacé par la tolérance

> `planningContext.equite.ecartTolerePourcent` dit **de combien** on accepte de déséquilibrer ; un
> booléen ne disait pas jusqu'où.

« Équité stricte » se transmet déjà : c'est une tolérance à `0`. Aucun champ nouveau, un champ de
moins.

### 5.5 Tranché — deux salariés, et la généralisation à N reste ouverte

> `salarieAId` et `salarieBId`. Un arbitrage à deux se justifie ligne à ligne devant les
> intéressés ; à cinq, il redevient une optimisation de groupe — ce que SC-03 fait déjà.

> 🔁 **Condition de retour, décidée avec la limite.** L'ouverture à **N est attendue à brève
> échéance**. Elle contraint donc la réalisation dès maintenant :
>
> * la contrainte HARD du lot **A0** porte sur un **ensemble de ressources autorisées**, pas sur un
>   couple. À deux, l'ensemble a deux éléments ; passer à N n'y change rien ;
> * ce qui reste propre au « deux », et qu'il faudra rouvrir, est le **contrat d'entrée** —
>   `salarieAId` / `salarieBId` — et la **restitution comparative A / B** ;
> * aucun `if (deux)` dans le calcul. La limite est une règle de contrat, pas une hypothèse de
>   calcul.
>
> Écrite ainsi, la généralisation est un **élargissement du contrat**, pas une réécriture du
> moteur.

### 5.6 Tranché — sans répartition acceptable, la moins mauvaise est rendue

> Jamais une erreur. La répartition est restituée avec les motifs qui la disqualifient.

Invariant du projet : *le moteur ne refuse pas, il rend visible l'impossible*. Même traitement
qu'en SC-06 §4.5, où les solutions non conformes sont restituées et jamais masquées — l'appelant
voit l'impasse au lieu de la deviner.

---

## 6. Contrat d'entrée

Conforme aux arbitrages de la §5.

```json
"scenarioParameters": {
  "salarieAId": "SAL-2001",
  "salarieBId": "SAL-2002",
  "creneauxArbitres": ["PLN-001", "PLN-002", "PLN-003"]
}
```

| Champ | Obligatoire | Rôle |
|---|:---:|---|
| `salarieAId`, `salarieBId` | ✅ | les deux personnes entre lesquelles on arbitre |
| `creneauxArbitres[]` | ✅ | le périmètre — identifiants de créneaux du `dataSet` (§5.1) |
| `planningContext.equite.ecartTolerePourcent` | ○ | au-delà de quel écart il y a inéquité — **existe déjà** |
| `planningContext.coefficientsPenibilite` | ○ | l'échelle de pénibilité — **existe déjà** |

**Aucun champ nouveau hors `scenarioParameters`.** Le planning existant, les contrats, les
indisponibilités, le cadre réglementaire : tout est déjà au contrat et déjà lu.

**Deux champs annoncés de longue date disparaissent** : `objectif` (§5.3) et
`autoriserDesequilibre` (§5.4). Le premier reviendra avec les préférences du rang 10, et avec une
seule valeur ; le second ne reviendra pas — la tolérance dit mieux ce qu'il disait mal.

⚠️ Comme SC-06, SC-05 exige que **le planning complet de la période soit transmis pour A et pour
B** — sans quoi les bornes hebdomadaires sont invérifiables et le moteur déclarerait conforme une
répartition qui ne l'est pas.

---

## 7. Contrat de sortie proposé

| Bloc | Contenu | Existe ? |
|---|---|---|
| `planning` | la répartition proposée, créneau par créneau | ✅ commun à tous les scénarios |
| `workMetrics.byRessource` | les indicateurs comparatifs A / B — `heuresPonderees`, `ecartContratPourcent`, `partNuits`… | ✅ livré aux lots L1 et L2 |
| `solverResult.scoreBreakdown` | la justification de l'arbitrage, ligne par ligne | ✅ commun |
| `arbitrage` | ce qui a changé pour chacun : créneaux repris, cédés, écart avant / après | ❌ **à écrire** |
| `diagnostics.alerts` | l'inéquité résiduelle, quand la tolérance reste dépassée | ❌ **un code à ajouter** |

Le bloc `arbitrage` est à SC-05 ce que `remplacement` est à SC-02 : la réponse à *qu'est-ce qui a
bougé, et pour qui*. Sa forme se décalque de `RemplacementDTO`.

---

## 8. Découpage

Ordonné par dépendance. **Actionnable** depuis les arbitrages du 2026-08-13.

| Lot | Objet | Pourquoi à ce rang |
|---|---|---|
| ~~**A0**~~ | ~~Contrainte HARD « affectation bornée aux ressources autorisées »~~ | ✅ **Livré le 2026-08-14** — `PerimetreArbitre` (fait) + `AffectationHorsRessourcesAutorisees` (HARD), écrits sur un **ensemble** (§5.5). Inerte tant qu'aucun périmètre n'est transmis |
| ~~**A1**~~ | ~~Endpoint, préparation, périmètre épinglé / libéré, alerte du créneau tenu par un tiers (§5.2)~~ | ✅ **Livré le 2026-08-14** — `POST /scenarios/sc05/solve`, décalque de SC-02 S1. Trois alertes : tiers, salarié introuvable, créneau du périmètre introuvable. ⚠️ Non inscrit au contrat série 50 ni à l'OpenAPI — c'est le lot **A4** |
| **A2** | Bloc `arbitrage` — avant / après par salarié | La réponse à « qu'est-ce qui a bougé » |
| **A3** | Alerte d'inéquité résiduelle, et restitution de la moins mauvaise répartition (§5.6) | Ce que la tolérance ne parvient pas à résorber |
| **A4** | Inscription au contrat série 50 + canal FileAdapter | Comme SC-02 S4 et S5 |

Ordre de grandeur : celui de SC-02, soit **cinq à six lots**.

---

## 9. Points ouverts

### 9.1 L'arbitrage doit-il pouvoir dégrader une situation conforme ?

Rééquilibrer deux salariés peut faire franchir une borne de confort à celui qui reçoit. Le moteur
le signale — il ne refuse pas — mais faut-il qu'il le **cherche** ? La réponse tient dans le poids
relatif de l'équité et de la surcharge, et relève du même protocole de calibration que les
coefficients : `92_CALIBRATION_PENIBILITE.md`.

### 9.2 Le volontariat, encore — et c'est le même rendez-vous que §5.3

Deux salariés désignés, c'est le cas où les préférences comptent le plus — et le moteur ne les
connaît pas avant le **rang 10**. Une répartition parfaitement équitable et contraire au souhait
des deux intéressés est la façon habituelle dont ce type d'arbitrage se fait rejeter sur le
terrain. À garder en tête au moment d'écrire A3.

Le rang 10 est donc attendu **deux fois** par SC-05 : pour les préférences elles-mêmes, et pour le
retour de l'enum `objectif` qu'elles conditionnent. Les traiter ensemble, pas l'un après l'autre.

### 9.3 L'historique

SC-05 arbitre sur la fenêtre transmise. Un déséquilibre installé depuis trois mois ne s'y voit pas.
C'est le même manque que celui qui bloque SC-04, et il ne se comble pas dans ce chantier — mais la
règle de transmission du §4.3 de `92_CADRAGE_WORKMETRICS_EQUITE.md` l'atténue : **plus la fenêtre
est large, plus l'arbitrage a de sens**.
