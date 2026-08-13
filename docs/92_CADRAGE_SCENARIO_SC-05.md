# 92 — Cadrage : SC-05, arbitrage de répartition entre deux salariés

> **Statut** : cadrage d'analyse, 2026-08-13, produit au lot **L6** du chantier équité.
>
> ⚠️ **Les arbitrages de la §5 ne sont PAS tranchés.** Ce sont des propositions, chacune assortie
> de ce qu'elle implique. SC-05 est le dernier scénario annoncé sans contrat d'entrée : le
> construire suppose de décider ce qu'il fait, et cela ne se déduit d'aucun code.
>
> Ce document ne modifie ni `50_SCENARIO_CONTRACT.md`, ni le code.

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

## 4. Ce que SC-05 exige du moteur, et qui n'existe pas

Une seule brique manque, et elle est structurante.

> **Restreindre l'affectation d'un créneau à deux ressources désignées.**

Le domaine de la variable de décision est **global** : toute ressource du dataset est candidate
pour tout créneau. Rien, aujourd'hui, ne permet de dire « ce créneau ne peut revenir qu'à A ou à
B ». SC-02 s'en passe parce qu'il libère et laisse le solveur choisir librement ; SC-05 ne le peut
pas — son objet même est un arbitrage **borné à deux personnes**.

Deux formes possibles, à trancher (§5.5) :

| Forme | Ce qu'elle donne | Ce qu'elle coûte |
|---|---|---|
| **Contrainte HARD** — un créneau du périmètre affecté hors du couple est interdit | simple, homogène avec le reste du moteur | le solveur explore des affectations qu'il devra rejeter |
| **Périmètre réduit** — ne transmettre que A et B comme ressources | aucune contrainte nouvelle | le reste du planning devient invisible, et les bornes hebdomadaires avec lui |

La seconde est trompeuse : sans le planning des autres, le moteur ne voit plus les créneaux qui
bornent A et B, et déclarerait conforme une répartition qui ne l'est pas. **La contrainte HARD est
la seule forme sûre.**

---

## 5. Arbitrages à rendre

Chacun est une question au métier. Les recommandations sont celles du moteur, pas des décisions.

### 5.1 ⚠️ À trancher — qu'est-ce que le « périmètre commun » ?

C'est la question qui commande le contrat d'entrée.

| Option | Description | Conséquence |
|---|---|---|
| **A — transmis** | l'appelant liste les créneaux à arbitrer | explicite, sans surprise ; WinDev doit savoir les désigner |
| **B — déduit** | tous les créneaux de la période que A et B peuvent tous deux servir | aucun travail côté appelant ; le moteur décide d'un périmètre que personne n'a validé |

> **Recommandation : A.** Le moteur ne fabrique pas le périmètre d'un arbitrage. Un périmètre
> déduit trop large déplacerait des créneaux que personne ne voulait bouger ; trop étroit, il
> rendrait l'arbitrage sans effet — et dans les deux cas l'appelant ne verrait pas pourquoi.

### 5.2 ⚠️ À trancher — que fait-on d'un créneau du périmètre affecté à un tiers ?

Le cas se produit dès que l'appelant désigne un périmètre par le lieu ou l'activité.

| Option | Conséquence |
|---|---|
| **Refuser la demande** | net, mais bloquant sur un détail |
| **Épingler le créneau et le signaler** | l'arbitrage porte sur ce qui reste, et l'appelant sait quoi |
| **Le rendre au couple** | le moteur retire du travail à un tiers qui n'a rien demandé |

> **Recommandation : épingler et signaler.** Cohérent avec toute la doctrine du moteur — *l'existant
> ne se réécrit pas*, et un constat vaut mieux qu'un refus.

### 5.3 ⚠️ À trancher — l'objectif d'arbitrage est-il encore nécessaire ?

Le contrat historique annonce `objectif: EQUITE_CHARGE | MINIMISATION_SURCHARGE | PREFERENCES`.
Depuis, chacun de ces trois objectifs a trouvé son paramètre :

| Objectif annoncé | Ce qui l'exprime aujourd'hui | État |
|---|---|---|
| équité de charge | `planningContext.equite.ecartTolerePourcent` | ✅ livré au lot L5 |
| minimisation de surcharge | `scenarioParameters.surchargeMaxHeuresJour` / `...Semaine` | ✅ livré au lot S3 de SC-02 |
| respect des préférences | — | ❌ **rang 10 du backlog**, en attente de la Production |

> **Recommandation : supprimer `objectif`.** L'objectif se déduit de ce que l'appelant transmet, et
> un enum qui double des paramètres existants finit par les contredire. Les préférences ne sont pas
> transmissibles aujourd'hui : les annoncer dans un enum promettrait ce que le moteur ne peut pas
> tenir.

### 5.4 ⚠️ À trancher — `autoriserDesequilibre` est-il autre chose que la tolérance de L5 ?

Le contrat historique annonce un booléen `autoriserDesequilibre`. Le lot L5 a livré
`planningContext.equite.ecartTolerePourcent`, qui dit **de combien** on accepte de déséquilibrer.

> **Recommandation : le remplacer par la tolérance.** Un booléen ne dit pas jusqu'où ; et « équité
> stricte » se transmet déjà — c'est une tolérance à 0.

### 5.5 ⚠️ À trancher — deux salariés, ou N ?

L'intention dit « deux ». Rien dans la mécanique proposée ne l'exige : une contrainte HARD sur un
couple s'écrit aussi bien sur un ensemble.

> **Recommandation : deux, et l'écrire comme tel.** Un arbitrage à deux se justifie ligne à ligne
> devant les intéressés ; à cinq, il redevient une optimisation, ce que SC-03 fait déjà. Ouvrir à N
> serait facile plus tard ; refermer, non.

### 5.6 ⚠️ À trancher — que rend-on quand aucune répartition n'est acceptable ?

Invariant du moteur : *il ne refuse pas, il rend visible l'impossible*. Appliqué ici, cela signifie
rendre la **moins mauvaise** répartition, avec les motifs qui la disqualifient — jamais une erreur.

> **Recommandation : cohérence avec SC-06 §4.5** — les solutions non conformes sont restituées,
> jamais masquées.

---

## 6. Contrat d'entrée proposé

Sous réserve des arbitrages ci-dessus.

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

## 8. Découpage proposé

Ordonné par dépendance. **Aucun lot n'est actionnable avant les arbitrages de la §5.**

| Lot | Objet | Pourquoi à ce rang |
|---|---|---|
| **A0** | Contrainte HARD « affectation bornée au couple » | La brique manquante (§4). Testable seule, sans scénario |
| **A1** | Endpoint, préparation, périmètre épinglé / libéré | Le squelette. Décalque de SC-02 S1 |
| **A2** | Bloc `arbitrage` — avant / après par salarié | La réponse à « qu'est-ce qui a bougé » |
| **A3** | Alerte d'inéquité résiduelle | Ce que la tolérance ne parvient pas à résorber |
| **A4** | Inscription au contrat série 50 + canal FileAdapter | Comme SC-02 S4 et S5 |

Ordre de grandeur : celui de SC-02, soit **cinq à six lots**.

---

## 9. Points ouverts

### 9.1 L'arbitrage doit-il pouvoir dégrader une situation conforme ?

Rééquilibrer deux salariés peut faire franchir une borne de confort à celui qui reçoit. Le moteur
le signale — il ne refuse pas — mais faut-il qu'il le **cherche** ? La réponse tient dans le poids
relatif de l'équité et de la surcharge, et relève du même protocole de calibration que les
coefficients : `92_CALIBRATION_PENIBILITE.md`.

### 9.2 Le volontariat, encore

Deux salariés désignés, c'est le cas où les préférences comptent le plus — et le moteur ne les
connaît pas avant le **rang 10**. Une répartition parfaitement équitable et contraire au souhait
des deux intéressés est la façon habituelle dont ce type d'arbitrage se fait rejeter sur le
terrain. À garder en tête au moment d'écrire A3.

### 9.3 L'historique

SC-05 arbitre sur la fenêtre transmise. Un déséquilibre installé depuis trois mois ne s'y voit pas.
C'est le même manque que celui qui bloque SC-04, et il ne se comble pas dans ce chantier — mais la
règle de transmission du §4.3 de `92_CADRAGE_WORKMETRICS_EQUITE.md` l'atténue : **plus la fenêtre
est large, plus l'arbitrage a de sens**.
