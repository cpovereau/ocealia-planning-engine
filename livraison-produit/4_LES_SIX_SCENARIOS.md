# 4 — Les six scénarios

Le moteur ne répond pas à une question, mais à six. Chacune correspond à une situation de terrain
différente, et chacune apporte quelque chose qu'aucune autre n'apporte.

Choisir le bon scénario, c'est déjà la moitié du travail : c'est lui qui détermine **ce que le
moteur a le droit de bouger**.

| | La question posée | Ce qui lui est propre |
|---|---|---|
| **SC-01** | *Comment organiser le travail de cette personne ?* | partir de rien |
| **SC-02** | *Untel est absent, qui prend son travail ?* | réagir à un imprévu sans casser le reste |
| **SC-03** | *Réorganise cette partie-là du planning* | traiter un sous-ensemble désigné |
| **SC-04** | *Ce planning peut-il être amélioré ?* | juger une **période**, pas un instant |
| **SC-05** | *Ces deux-là se partagent mal le travail* | arbitrer entre deux personnes |
| **SC-06** | *Qui est le plus à même de couvrir ce besoin ?* | classer des candidats sans rien décider |

---

## Ce qui est commun aux six

Toute demande a la même forme. Seul le bloc `scenarioParameters` change d'un scénario à l'autre.

| Bloc | Contenu |
|---|---|
| `requestId`, `scenarioType`, `metadata` | l'en-tête obligatoire — voir [3_LE_REPERTOIRE_D_ECOUTE.md](3_LE_REPERTOIRE_D_ECOUTE.md) |
| `planningContext` | **la période traitée** et les règles du jeu : seuils réglementaires, tolérance d'équité, poids de la pénibilité |
| `scenarioParameters` | **la question précise**. C'est le seul bloc propre à chaque scénario |
| `dataSet` | **la matière** : les personnes, leurs contrats, leurs absences, et les créneaux à couvrir |

Et toute réponse aussi :

| Bloc | Contenu |
|---|---|
| `planning` | **le résultat** : jour par jour, créneau par créneau, qui fait quoi |
| `workMetrics` | **la mesure**, personne par personne : heures, jours, écart au contrat, week-ends, nuits |
| `solutionSummary` | le résumé chiffré : ce qui est couvert, ce qui ne l'est pas |
| `diagnostics` | **ce qu'il faut lire avant tout le reste** : les alertes, et les créneaux écartés avec la raison |
| `solverResult` | le temps passé et l'état du calcul |

Quatre scénarios ajoutent un bloc qui leur est propre : `remplacement` pour SC-02, `arbitrage` pour
SC-05, `optimisation` pour SC-04, `candidats` pour SC-06.

> **Les alertes ne sont pas des erreurs.** Ce sont les remarques du moteur sur sa propre réponse :
> « ce salarié dépasse son contrat », « ce besoin n'a trouvé personne », « vous avez déclaré des
> coefficients qui ne servent à rien ». Une réponse sans alerte est une réponse sans réserve. Une
> réponse avec alertes reste utilisable — mais quelqu'un doit les lire.

---

## SC-01 — Concevoir un planning à partir de rien

**Quand l'utiliser.** Une personne arrive, ou l'on repart de zéro sur une période. Il n'y a pas de
planning existant à ménager.

**Ce que vous transmettez en plus.** Le cadre de la semaine type : amplitude quotidienne, heure de
début, alerte de fin de journée, pause déjeuner, jours travaillés, jours fériés.

**Ce que vous récupérez.** Un planning complet, la mesure, et les alertes. C'est le scénario le
plus simple, et le seul où le moteur ne ménage rien puisqu'il n'y a rien à ménager.

---

## SC-02 — Remplacer un salarié absent

**Quand l'utiliser.** Quelqu'un est absent ; son travail doit être repris.

**Ce que vous transmettez en plus.** `salarieAbsentId`, la personne absente — et l'absence
elle-même dans `dataSet.indisponibilites`. Deux réglages facultatifs, mais décisifs :

- `posteVirtuelAutorise` — le moteur a-t-il le droit de proposer un renfort extérieur, ou doit-il
  se débrouiller avec l'équipe ?
- `surchargeMaxHeuresJour` / `surchargeMaxHeuresSemaine` — **de combien** on accepte de charger les
  collègues restants. Sans ces valeurs, le moteur n'applique aucune limite de surcharge : rappelez-
  vous qu'une borne absente n'est pas une borne à zéro.

**Ce que vous récupérez.** Le planning, et le bloc `remplacement` : qui a repris quoi, ce qui n'a
trouvé personne, ce qui est parti au renfort extérieur.

**Le point à connaître.** Seuls les créneaux de l'absent sont rouverts. Le travail des collègues
n'est pas redistribué au passage — ils n'ont rien demandé.

---

## SC-03 — Réorganiser une partie du planning

**Quand l'utiliser.** Un événement ponctuel bouscule une portion du planning et il faut la
recomposer, sans toucher au reste.

**Ce que vous transmettez en plus.** Peu de choses : un niveau de priorité de couverture, et une
période si elle diffère de celle du `planningContext`. Ce sont les créneaux transmis qui délimitent
le périmètre.

**Ce que vous récupérez.** Le planning recomposé et la mesure.

---

## SC-04 — Optimiser un planning existant sur une période

**Quand l'utiliser.** Le planning tourne, mais on soupçonne qu'il pourrait être mieux réparti. Sur
un mois, sur un trimestre.

**Ce qui le distingue de tous les autres.** C'est le seul qui juge une **durée**. Les cinq autres
répondent à une question posée sur un moment ; celui-ci relit une période entière et montre **quand**
un déséquilibre s'est installé. Un salarié peut être parfaitement à son contrat sur le mois tout en
ayant fait deux semaines à zéro et deux semaines à cinquante heures : seul SC-04 le voit.

**Ce que vous transmettez en plus.**

- `datePivot`, **obligatoire** — le premier jour que le moteur a le droit de modifier. Tout ce qui
  précède est figé. C'est ce qui permet de corriger la suite au vu du passé sans réécrire le passé.
- `creneauxAjustables`, facultatif — la liste des créneaux qu'il a le droit de toucher. Il ne peut
  que **restreindre** : est modifiable ce qui est à la fois après le pivot **et** dans la liste.
  Absent, tout l'après-pivot est ouvert. Cette liste ne peut pas couvrir plus d'un mois.

**Ce que vous récupérez.** Le bloc `optimisation` : ce qui a bougé, et surtout, pour chaque
personne, sa charge **avant et après**, découpée par semaine, par mois et sur la période entière.
C'est ce découpage qui rend la proposition discutable avec les intéressés.

Il porte aussi des **motifs** : les raisons qui disqualifient le résultat. Le plus important est
`REGRESSION_INDIVIDUELLE` — quelqu'un sort du calcul plus loin de son contrat qu'il n'y entrait.
Optimiser un total en dégradant une personne est la façon la plus ordinaire de produire un planning
que personne n'acceptera.

**Le point à connaître.** Ne pas lister un créneau, c'est renoncer à le couvrir. Un besoin non
couvert et non listé restera un trou, et une alerte vous le dira.

---

## SC-05 — Arbitrer entre deux salariés

**Quand l'utiliser.** Deux personnes se partagent un même périmètre de travail et le partage est
devenu inéquitable.

**Ce que vous transmettez en plus.** `salarieAId` et `salarieBId`, et surtout `creneauxArbitres` :
**la liste des créneaux remis en jeu**. Cette liste est obligatoire, et elle est transmise, jamais
déduite — un périmètre deviné trop large déplacerait du travail que personne ne voulait bouger ;
trop étroit, l'arbitrage serait sans effet, et dans les deux cas vous ne verriez pas pourquoi.

L'équité se règle par `planningContext.equite.ecartTolerePourcent` : l'écart au contrat au-delà
duquel le moteur considère qu'il y a inéquité. Sans cette valeur, il ne juge rien — il ne décide
pas à votre place de ce qui est équitable.

**Ce que vous récupérez.** Le bloc `arbitrage` : la charge de chacun avant et après, mesurée par le
même calcul, et les créneaux effectivement déplacés.

**Le point à connaître.** Un créneau du périmètre tenu par une **troisième personne** n'est jamais
repris. Elle n'a rien demandé ; on ne lui retire pas son travail pour équilibrer deux autres. Le
moteur l'épingle et le signale.

---

## SC-06 — Désigner qui peut couvrir un besoin

**Quand l'utiliser.** Un besoin apparaît, et la question n'est pas « refais le planning » mais
« qui pourrait le prendre ? ».

**Ce que vous transmettez en plus.** `besoin` : le travail à couvrir, avec ses créneaux.

**Ce que vous récupérez.** Une **liste de candidats classés**, chacun avec ce qui le qualifie et ce
qui le disqualifie. Le bloc `planning` est bien présent, mais il ne contient que les créneaux du
besoin soumis : il décrit la question, pas une décision.

**Le point à connaître.** C'est le seul scénario qui ne décide de rien. Il ne modifie aucun
planning et n'affecte personne : il éclaire une décision qu'un humain prendra. La liste est un
classement, pas une désignation.

---

## Comment choisir, en pratique

Trois questions suffisent.

**Y a-t-il un planning existant à ménager ?** Non → SC-01. Oui → continuez.

**Est-ce que je veux modifier le planning, ou seulement savoir qui pourrait ?** Seulement savoir →
SC-06.

**Qu'est-ce qui déclenche la demande ?**

- une absence → **SC-02**
- un événement sur une portion de planning → **SC-03**
- un déséquilibre entre deux personnes précises → **SC-05**
- rien de particulier, juste l'envie de mieux répartir sur une période → **SC-04**

> **En cas d'hésitation entre SC-04 et SC-05**, la question à se poser est : *est-ce que je sais
> déjà qui est concerné ?* Si oui, SC-05 est plus précis et plus prévisible. Si vous cherchez
> justement à savoir où est le déséquilibre, c'est SC-04.

---

## Les jeux d'essai fournis

Le dossier `exemples/` contient une demande complète et valide par scénario. Ce sont les jeux
utilisés pour valider le moteur : leur contenu est éprouvé, et le résultat qu'ils produisent est
vérifié automatiquement à chaque livraison.

| Fichier | Ce qu'il contient |
|---|---|
| `sc01_exemple.json` | un planning conçu à partir de rien |
| `sc02_exemple.json` | une absence à couvrir |
| `sc03_exemple.json` | un ajustement sur un périmètre |
| `sc04_exemple.json` | deux semaines, la première figée, la seconde déséquilibrée sur une seule personne |
| `sc05_exemple.json` | deux salariés et un périmètre à repartager |
| `sc06_exemple.json` | un besoin, et des candidats à classer |

Le meilleur moyen de comprendre un scénario est de déposer son exemple dans `inbox`, puis d'ouvrir
côte à côte la demande et sa réponse.
