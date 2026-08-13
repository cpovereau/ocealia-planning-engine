# 92 — Cadrage : WorkMetrics d'équité

> **Statut** : cadrage d'analyse, 2026-08-13. Les arbitrages de la §4 sont **tranchés** (métier,
> 2026-08-13). La §9 porte les points ouverts. Ce document sert à découper le chantier, pas à le
> réaliser.
>
> Il ne modifie ni le code, ni `40_WORKMETRICS.md`, ni le contrat série 50. Deux conséquences y
> sont annoncées et seront portées par les lots qui les mettent en œuvre.

---

## 1. Intention métier

> À **pénibilité équivalente**, qui a été le plus sollicité — et qui doit l'être ensuite ?

Aucune WorkMetric actuelle ne répond à cette question, parce qu'aucune ne regarde deux personnes à
la fois. Le moteur sait dire « Paul a fait 38 h dont 6 de nuit ». Il ne sait pas dire « et c'est
9 h de plus que ce que son contrat prévoit, quand Sophie est 5 h en dessous ».

### 1.1 Deux objets distincts, à ne pas confondre

| | **Règle de sélection** | **Mesure d'équité** |
|---|---|---|
| Question | qui prend ce créneau, maintenant ? | qui a été servi correctement ? |
| Portée | l'instant | la fenêtre transmise |
| Nature | comparative, immédiate | comparative, accumulée |
| Sert | SC-02, SC-06 | SC-05, lecture RH |

Les deux s'appuient sur la **même unité** (§3) et se calculent sur les **mêmes primitives**, mais
ce ne sont pas les mêmes objets. Les mélanger dans un seul indicateur interdirait de répondre
séparément à « Sophie est-elle en état de venir ce soir ? » et « Sophie a-t-elle été traitée
correctement ce mois-ci ? ».

---

## 2. L'existant — ce qui est déjà là, et ce qui manque

Vérifié dans le code à la date de ce cadrage.

### 2.1 Acquis, et réutilisable tel quel

| Élément | Où | Ce qu'il apporte |
|---|---|---|
| `minutesNuit`, `minutesDimanche`, `minutesFerie` **et leurs intersections** | `TimeBreakdownCalculator` | les primitives de pénibilité, calculées par intersection réelle |
| `DominancePenibilites` | `domain/contexte` | l'ordre NUIT > DIMANCHE > FERIE, **paramétrable**, et la règle de non-double-pondération |
| `maxJoursConsecutifsObservees` | `WorkMetricsCalculator` | le critère « jours consécutifs », déjà mesuré |
| `amplitudeJournaliere` avant / après | `ImpactMesureDTO`, SC-06 | le critère « amplitude », déjà mesuré et déjà restitué |
| `contrat.heuresHebdomadairesHabituelles` | contrat d'entrée | le dénominateur de la comparaison (§4.5) |
| `ReposHebdomadaire` porteur de sa **nature RH ou RHD** | `domain/repos` | la donnée dont §4.6 a besoin — elle existe déjà |

### 2.2 Ce qui manque

* **Aucune métrique comparative.** `40_WORKMETRICS.md` §5.1.5 les a anticipées et nommées ; aucune
  n'est écrite.
* **Aucune contrainte ne compare deux salariés.** Toutes vérifient une personne contre *sa* borne.
  Un planning où l'un fait 48 h et l'autre 25 h a exactement le même score qu'un planning à 35 h
  chacun.
* **Aucune pondération de pénibilité pour la comparaison.** Les primitives existent, l'échelle qui
  les rend additionnables n'existe pas (§4.2).

### 2.3 ⚠️ Un manque découvert en cadrant, et il n'a pas attendu l'équité

`DetteReposSurReposHebdomadaire` est la seule règle qui regarde le travail posé un jour de repos.
Trois limites, cumulatives :

1. elle est **SOFT** — la situation est explicitement « autorisée, pénalisée » ;
2. elle **ne distingue pas RH de RHD**, alors que le fait porte la nature ;
3. elle ne se déclenche que si l'activité porte `genereDetteRepos`. **Pour toute autre activité,
   travailler le jour de repos de quelqu'un coûte zéro.**

L'arbitrage §4.6 en fait une règle forte. Le correctif est donc **indépendant de l'équité** et
n'attend rien : c'est le lot **L0**.

---

## 3. Principe structurant — la pénibilité est l'unité, pas un critère

> On ne compare pas des heures. On compare des **heures pondérées**.

C'est la formulation exacte de l'arbitrage métier : *on ne peut juger l'équité qu'à pénibilité
équivalente*. La pénibilité n'est donc **pas un palier de départage** placé avant les autres —
c'est le changement d'unité qui rend toute comparaison possible. Huit heures un mardi et huit
heures un dimanche ne sont pas la même chose et ne doivent jamais entrer dans la même addition
sans coefficient.

**L'ordre de dominance est conservé tel qu'il est défini** : `NUIT > DIMANCHE > FERIE`. Une minute
appartenant à plusieurs catégories est attribuée à la dominante, **sans double pondération**.

⚠️ **Conséquence assumée** : une nuit du dimanche pèse exactement comme une nuit ordinaire. Pour
une pénalité, c'est le comportement voulu — on ne compte pas deux fois. Pour l'équité, c'est un
choix : celui qui fait les nuits du dimanche et celui qui fait les nuits du mardi ressortent à
égalité.

---

## 4. Arbitrages tranchés

### 4.1 Tranché — l'unité est l'heure pondérée, rapportée au contrat

Deux transformations successives, dans cet ordre :

```text
minutes brutes  →  minutes pondérées par pénibilité  →  % du volume contractuel
```

La seconde est ce qui rend deux personnes comparables quand elles n'ont pas le même contrat. La
comparaison se fait **au contrat de chacun**, jamais à la moyenne du groupe en heures brutes : un
salarié à 50 % serait sinon perpétuellement « sous la moyenne », et le moteur passerait son temps
à vouloir le charger.

Illustration de l'arbitrage :

| | Contrat | Demandé | Écart |
|---|---|---|---|
| Salarié A | 30 h | 35 h | **+16,7 %** |
| Salarié B | 35 h | 40 h | **+14,3 %** |

À pénibilité équivalente, B est le moins sollicité — bien qu'il fasse cinq heures de plus en
valeur absolue.

### 4.2 Tranché — les coefficients se calibrent, ils ne se décrètent pas

L'ordre de pénibilité est un **rang** ; l'addition exige une **échelle**. Une heure de nuit
vaut-elle 1,5 heure ouvrable, ou 3 ? Le choix décide qui gagne, et aucune valeur *a priori* n'est
défendable.

> **Décision** : les coefficients sont **paramétrables**, jamais écrits en dur, et sont **calibrés
> par simulation sur des cas concrets**.

Deux conséquences d'architecture, à tenir dès le premier lot qui les introduit :

* aucun coefficient de pondération dans le code ;
* le chantier doit fournir de quoi **rejouer** un même jeu avec plusieurs jeux de coefficients et
  comparer les classements obtenus. Sans cela, « calibrer » veut dire « deviner deux fois ».

⚠️ Le moteur porte déjà une échelle implicite dans ses pénalités (5 000 pour les dimanches, 5 000
pour les nuits consécutives). **Ne pas créer une seconde échelle qui la contredise** : soit les
coefficients d'équité s'y adossent, soit l'écart est documenté et voulu.

> **Confrontation faite au lot L3** — détail dans `92_CALIBRATION_PENIBILITE.md` §4.
>
> L'échelle qui compte n'est pas celle des forfaits cités ci-dessus mais celle de `ScoreWeights`,
> **à la minute** : en `EXPLOITATION`, nuit 3, dimanche 4, férié 5. Elle classe donc la nuit comme
> la **moins** coûteuse — l'inverse de l'ordre de pénibilité du métier.
>
> Les deux tiennent ensemble, mais pour des raisons opposées : le score lit la dominance comme « la
> situation la plus favorable au salarié » (`40_STRATEGIE_DE_SCORING.md` §4.1.1) et y prend la
> catégorie la moins chère ; l'équité y prend la pénibilité la plus lourde. L'accord n'est donc
> **pas une propriété mais une coïncidence entretenue** — réordonner la liste pour servir l'une
> casserait l'autre en silence. Depuis L1 les deux partagent la même implémentation, ce qui rend le
> couplage réel. `CoherenceEchelleTest` verrouille les deux moitiés de la condition.
>
> **S'adosser est impossible** : adopter 3 : 4 : 5 donnerait à la nuit le coefficient le plus
> faible.
>
> ✅ **Tranché le 2026-08-13** : le score garde sa lecture — *la situation la plus favorable au
> salarié* — et **rien n'y est modifié**. Sur un même planning, le moteur pénalise donc le moins la
> nuit tout en la mesurant comme la plus lourde ; les deux répondent à des questions différentes, et
> l'écart est voulu.

### 4.3 Tranché — l'historique vient de l'horizon transmis

> Le moteur **ne fabrique pas d'historique**. Il ne connaît que ce qu'on lui transmet.

C'est une **règle de transmission WinDev** : lorsqu'un arbitrage porte sur une date, l'appelant
transmet une **fenêtre encadrante** — J-7 / J+7 par exemple — suffisante pour que la comparaison
ait un sens.

C'est la même logique que l'exigence de « semaine pleine » de SC-06, et elle appelle le même
traitement : **le moteur dit sur quelle fenêtre il a jugé**. Une équité calculée sur trois jours
n'est pas fausse, elle est faible — et l'appelant doit pouvoir le voir plutôt que le supposer.

### 4.4 Tranché — l'écart est signé

La sous-charge compte autant que la surcharge. Un salarié à 20 h sur un contrat de 30 est à
**−33 %**, et cela doit le rendre **préférable**, pas seulement « non exclu ».

Sans écart signé, le moteur se contente d'éviter de surcharger : il ne rééquilibre jamais, et
l'équité ne se produit pas — elle est seulement moins violée.

### 4.5 Tranché — ce qui se rapporte au contrat s'y rapporte partout

La proportionnalité au contrat vaut aussi pour la **pénibilité**, pas seulement pour le volume. Un
salarié à 50 % doit une part de nuits et de dimanches proportionnée à son contrat, au même titre
que ses heures.

### 4.6 Tranché — le RHD ne se supprime pas

> **Règle forte.** On ne retire pas à un salarié son repos hebdomadaire dominical.

Motif : la loi impose un **délai de prévenance** le plus souvent incompatible avec une
réorganisation qui, par nature, se décide dans l'urgence. Ce n'est donc pas un critère de
départage que l'on pondère — c'est une **exclusion**.

| | Règle | Portée |
|---|---|---|
| **RHD** | **HARD** — interdiction | toutes activités, sans condition |
| **RH** | SOFT — pesé, autorisé | inchangé |

La donnée nécessaire existe déjà : `ReposHebdomadaire` porte sa nature. Voir §2.3 pour l'état
actuel, qui est bien en deçà.

### 4.7 Tranché — les critères, et leur ordre

Après conversion dans l'unité de la §4.1, et une fois écartés les cas interdits (§4.6) :

| Rang | Critère | Nature | Déjà mesuré |
|:---:|---|---|:---:|
| 1 | **Jours travaillés consécutifs** | aptitude — ne pas rappeler qui enchaîne | ✅ |
| 2 | **Écart signé au contrat, en %** | équité proprement dite | ○ |
| 3 | **Amplitude journalière** | confort — départage final | ✅ |

L'écart au contrat **remonte** : placé en dernier, il n'aurait quasiment jamais servi, les deux
autres départageant presque toujours. Un salarié à +30 % ne doit pas perdre contre un salarié à
+5 % pour trente minutes d'amplitude.

Les jours consécutifs restent **devant** l'équité : l'aptitude prime sur le partage. Faire revenir
quelqu'un au sixième jour d'affilée n'est pas un arbitrage qu'un écart favorable doit pouvoir
emporter.

**Amplitude — formulation corrigée.** « Préférer une amplitude plus faible » ne peut pas se lire
sur l'amplitude *de départ* : elle vaut zéro pour qui ne travaille pas ce jour-là, ce qui
favoriserait mécaniquement un rappel sur repos — l'inverse du critère 1 et du palier 4 de SC-06.
Le critère porte donc sur l'**amplitude après affectation**, et ne départage que des personnes
**déjà en poste ce jour-là**.

### 4.8 Tranché — l'équité ne déplace aucune borne, et l'annualisation n'en exempte pas

> **`contraintesReglementaires.heuresMaximumParSemaine` borne la semaine — annualisation ou pas.**
> C'est le cadre général, et il ne relève pas de ce chantier.

La frontière est nette, et il faut la tenir :

| | Cadre général | Équité |
|---|---|---|
| Question | jusqu'où a-t-on le droit d'aller ? | qui a été le plus sollicité ? |
| Référence | `contraintesReglementaires` — **prescriptif** | `contrat.heuresHebdomadairesHabituelles` — **descriptif** |
| Nature | borne, opposable | comparaison, informative |
| Statut | **livré**, inchangé par ce chantier | à écrire |

**Vérifié à la date de ce cadrage** : `estAnnualise` n'est lu par **aucune contrainte**. Aucun
salarié n'échappe donc au plafond hebdomadaire, ce qui est le comportement voulu. C'est écrit ici
pour qu'une future prise en compte de l'annualisation ne se traduise pas par une exemption : elle
n'en est pas une.

Ce que l'annualisation change relève uniquement de la **référence de comparaison** — voir §9.2.

---

## 5. Ce que les arbitrages imposent au modèle

### 5.1 Le calcul se fait en deux temps

`40_WORKMETRICS.md` §5.1.5 l'avait anticipé et la structure le permet déjà :

1. métriques **individuelles**, ressource par ressource — ce qui existe ;
2. métriques **comparatives**, sur l'ensemble, dans une seconde passe d'agrégation.

Rien à réorganiser : l'équité s'ajoute, elle ne déplace pas l'existant.

### 5.2 Une correction à porter dans `40_WORKMETRICS.md`

Le document annonce `ecartChargeAvecMoyenne` et `ecartNuitsAvecMoyenne` — un écart **à la
moyenne**. L'arbitrage §4.1 le contredit : la référence est le **contrat de chacun**, pas la
moyenne du groupe. À corriger au lot qui livre la mesure, pas avant.

### 5.3 Mesurer n'est pas sanctionner

L'invariant du projet vaut ici plus qu'ailleurs : la métrique **décrit** une répartition, elle ne
statue pas qu'elle est mauvaise. La pénalisation est une contrainte **SOFT** distincte, livrée
séparément (L5) — au même titre que le seuil de surcharge de SC-02, et pour la même raison :
l'encadrement dit à partir de quand un écart gêne.

Et **aucune borne du cadre général ne bouge** (§4.8). Un écart d'équité favorable n'autorise
jamais à dépasser un plafond réglementaire ; l'inverse non plus — un salarié au plafond n'est pas
« servi », il est empêché.

---

## 6. Contrat d'entrée

### 6.1 Ce qui existe déjà et suffit

`contrat.heuresHebdomadairesHabituelles`, `contrat.estAnnualise`, le référentiel d'activités, les
marqueurs de repos, et le planning existant avec ses `ressourceAffecteeId`.

### 6.2 Ce qui est à ajouter

| Champ | Obligatoire | Rôle |
|---|:---:|---|
| coefficients de pondération de pénibilité | ○ | défaut appliqué si absents, et **le moteur dit lequel** |
| — | — | *aucun autre champ nouveau* |

**Aucune donnée nouvelle n'est demandée à WinDev**, hors les coefficients. Ce que le chantier
demande est une **règle de transmission** (§4.3), pas un champ : élargir la fenêtre quand un
arbitrage l'exige.

---

## 7. Contrat de sortie

Les mesures s'ajoutent à `workMetrics.byRessource`, en **heures décimales** comme le reste :

| Champ | Description |
|---|---|
| `heuresPonderees` | charge convertie dans l'unité de la §4.1 |
| `ecartContratPourcent` | écart **signé** au volume contractuel (§4.4) |
| `partNuits`, `partDimanches`, `partFeries` | part de chaque pénibilité, rapportée au contrat (§4.5) |

Et, au niveau global, la **fenêtre effectivement observée** (§4.3) — sans quoi l'appelant ne peut
pas savoir ce que le chiffre vaut.

---

## 8. Découpage proposé

Ordonné par dépendance. Chaque lot est livrable seul et testable seul.

| Lot | Objet | Pourquoi à ce rang |
|---|---|---|
| **L0** ✅ | Le RHD devient inviolable : HARD, sur toutes activités, RH/RHD distingués | **Indépendant de l'équité et immédiatement actionnable.** Corrige un manque réel (§2.3), et la donnée existe déjà |
| **L1** ✅ | Heures pondérées : coefficients paramétrables, mesure individuelle | L'unité, sans laquelle rien n'est comparable. Aucune comparaison encore |
| **L2** ✅ | Écart signé au contrat + parts de pénibilité, restitution, fenêtre observée | La mesure comparative. **Descriptive, sans décision** |
| **L3** ✅ | Harnais de simulation et calibration des coefficients | Ne peut venir qu'après L1 et L2 : on calibre sur des mesures qui existent |
| **L4** ✅ | Les trois critères de §4.7 câblés dans la sélection **SC-06** | Premier effet visible sur une décision. Deux critères sur trois sont déjà mesurés |
| **L5** | Contrainte SOFT d'équité — **et c'est par elle que SC-02 est servi** | Pèse l'écart au score. Séparé de L2 pour être évalué seul |
| **L6** | SC-05 — arbitrage entre deux salariés | Le scénario que tout ceci débloque |

### 8.1 Correction apportée par L4 — SC-02 n'a pas de sélection à câbler

La ligne L4 annonçait « SC-02 **et** SC-06 ». Vérification faite en réalisant le lot, **les deux
scénarios ne choisissent pas de la même façon**, et un seul des deux a quelque chose où insérer un
critère :

| | Comment le remplaçant est choisi | Où insérer les critères |
|---|---|---|
| **SC-06** | énumération explicite, classement par paliers lexicographiques | dans les paliers — **fait au lot L4** |
| **SC-02** | **le solveur décide** : les créneaux libérés redeviennent des variables | dans le score, donc **au lot L5** |

SC-02 ne comporte aucun comparateur : sa préparation libère les créneaux de l'absent, épingle tout
le reste, et laisse le solveur affecter. Y faire entrer les trois critères n'a qu'une forme
possible — une contrainte qui les pèse — c'est-à-dire exactement l'objet du lot L5.

**Ce n'est pas un report de L4, c'est un déplacement de frontière** : le travail que la ligne
attribuait à L4 pour SC-02 n'existait pas sous cette forme. Rien n'est perdu, et L5 hérite d'un
objet plus clair — *la même équité, servie aux deux scénarios par deux mécanismes différents*.

⚠️ Conséquence à tenir au lot L5 : **les deux mécanismes doivent produire le même arbitrage**. Une
contrainte SOFT qui classerait autrement que les paliers de SC-06 ferait dire au moteur deux choses
différentes sur la même situation, selon le scénario interrogé.

---

## 9. Points ouverts

### 9.1 La largeur de la fenêtre

J-7 / J+7 a été donné **comme exemple**, pas comme règle. À fixer avec WinDev, et probablement à
exprimer comme un minimum plutôt qu'une valeur.

### 9.2 L'annualisation — la période d'agrégation, et elle seule

Le plafond légal, lui, est tranché : il s'applique sans exception (§4.8). Ce qui reste ouvert est
plus étroit.

Pour un salarié annualisé, `heuresHebdomadairesHabituelles` décrit une **moyenne**, pas une cible
hebdomadaire. Une semaine au-dessus n'est pas une anomalie — c'est l'objet même de
l'annualisation. Lire un écart hebdomadaire comme une inéquité le pénaliserait pour avoir travaillé
comme son contrat le prévoit.

**Décision par défaut, sauf avis contraire** : la mesure ne change pas, seule sa **période
d'agrégation** change. L'écart au contrat d'un salarié annualisé se lit sur **toute la fenêtre
transmise** (§4.3), jamais semaine par semaine. Une fenêtre trop courte ne permet pas de le juger,
et le moteur doit alors le **signaler** plutôt que de produire un chiffre qui ne veut rien dire.

Reste hors de portée : une vraie référence annuelle, que le moteur ne reçoit pas et qui relève du
même manque que l'historique de SC-04.

### 9.3 Le volontariat

Certains veulent ces heures, d'autres veulent qu'on les oublie. Le moteur ne le saura pas avant le
**rang 10** du backlog (préférences, en attente de la Production). Une équité parfaitement mesurée
et contraire aux souhaits de chacun est la façon habituelle dont ce type de calcul se fait rejeter
sur le terrain — à garder en tête au moment de peser la contrainte SOFT du lot L5.

### 9.4 Les coefficients eux-mêmes

Par construction (§4.2), ils sortiront de L3. Ce sont les seules valeurs du chantier qui ne
peuvent pas être décidées sur le papier.

> **Précision apportée par L3.** L'instrument est livré ; les valeurs ne le sont pas, et ne
> pouvaient pas l'être. Le harnais convertit « combien vaut une heure de nuit ? » — question à
> laquelle personne ne sait répondre — en « sur ce planning, l'ordre entre ces deux personnes change
> à 1,375 : lequel vous paraît le plus sollicité ? ». La valeur se déduit d'arbitrages réels au lieu
> de les précéder.
>
> **Ce qui manque n'est donc plus un outil mais une matière** : les jeux du dépôt sont des cas de
> démonstration — deux ou trois créneaux, un ou deux salariés — et le rapport les déclare muets. La
> calibration commence quand des exports WinDev réels sont versés. Marche à suivre :
> `92_CALIBRATION_PENIBILITE.md` §2.

### 9.5 Ce que L3 a fermé au passage

Le harnais évalue toutes les échelles à partir d'une **seule résolution** : la répartition d'une
minute entre catégories est une propriété du planning, pas du choix des coefficients. Cette licence
tient tant que les coefficients ne participent pas au score — **elle expire au lot L5**, où la
contrainte SOFT d'équité les y fera entrer. `HarnaisDeCalibrationTest` garde l'hypothèse et
échouera ce jour-là, ce qui vaut mieux que de s'en souvenir.
