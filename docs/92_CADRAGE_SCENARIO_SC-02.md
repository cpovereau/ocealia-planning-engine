# 92 — Cadrage : SC-02, remplacement d'un salarié absent

> **Statut** : cadrage d'analyse, 2026-08-11. Les arbitrages de la §4 sont **tranchés** (métier,
> 2026-08-11). Le découpage de la §8 reste à valider, et la §9 liste trois points encore ouverts.
> Ce document sert à découper le chantier, pas à le réaliser.
>
> Il ne modifie ni `50_SCENARIO_CONTRACT.md`, ni le code. L'inscription de SC-02 au contrat
> fonctionnel est une décision d'architecture, portée par le lot **S1**.

---

## 1. Intention métier

> Un salarié est absent. Ses créneaux sont orphelins. **Qui les reprend, et à quel prix — sans
> défaire le planning de ceux qui n'ont rien demandé ?**

Le contrat l'énonce ainsi : *assurer la continuité de service en perturbant le moins possible
l'existant*. Toute la difficulté du scénario tient dans la seconde moitié de cette phrase.

| | SC-02 |
|---|---|
| Ce qui est décidé | l'affectation des seuls créneaux du salarié absent |
| Ce qui est figé | l'intégralité du reste du planning transmis |
| Ce qui est restitué | un planning ajusté, les différences avant / après, et le volume à pourvoir |

---

## 2. Positionnement par rapport aux scénarios existants

| Scénario | Différence essentielle |
|---|---|
| **SC-06** — désignation de la meilleure ressource | part d'un besoin nu et **classe** des possibilités sans rien réaffecter ; SC-02 part d'une absence identifiée et **réaffecte réellement** |
| **SC-03** — ajustement ponctuel | réaffecte un sous-ensemble de créneaux, mais sans planning préexistant à préserver |
| **SC-01** — génération | construit un planning depuis rien ; SC-02 en répare un |

SC-02 est le premier scénario où le moteur passe de *« je génère »* à *« j'ajuste sans casser »*.
`92_CADRAGE_DONNEES_AMONT_SCENARIOS.md` §7 l'avait anticipé : trois familles de données
apparaissent à SC-02 et ne disparaissent plus — le planning existant, le contrat salarié et le
seuil de surcharge.

**Ni SC-01, ni SC-03, ni SC-06 ne sont modifiés par ce chantier.** Les évolutions du contrat
d'entrée décrites ici sont additives.

---

## 3. Principe structurant — l'existant est un fait, jamais une variable

C'est la décision qui commande tout le reste (§4.1).

> Seuls les créneaux du salarié absent sont décidés. Tout le reste du planning transmis est
> **épinglé** : le solveur le voit, les contraintes le mesurent, mais aucune affectation existante
> ne peut être déplacée pour faire de la place.

Le mécanisme existe déjà et il est testé — `@PlanningPin` sur `Creneau.fige`,
`ScenarioCreneauMapper.toCreneauxFiges()`, livrés au lot S1 de SC-06. Il n'est aujourd'hui câblé
que dans SC-06. SC-02 le réemploie tel quel.

Trois propriétés en découlent :

| Propriété | Conséquence |
|---|---|
| **Espace de recherche minuscule** | seuls les créneaux de l'absent portent une décision — la résolution est rapide et le découpage de la §5 reste de taille modeste |
| **Restitution lisible** | tout ce qui a changé a changé *parce que* quelqu'un était absent. Aucun effet de bord à expliquer à l'encadrement |
| **Réponse honnête** | le moteur ne cache pas ce qu'il n'a pas su couvrir : il le chiffre (§4.3) |

Le prix à payer est assumé : le moteur répondra « à pourvoir » là où un échange entre deux
collègues aurait suffi. **C'est voulu.** Un remaniement du planning des présents est une décision
d'encadrement, pas une décision de moteur.

---

## 4. Arbitrages

### 4.1 Tranché — on ne bouge pas l'existant

Aucun créneau déjà affecté à un salarié présent n'est déplaçable. Pas de pénalité SOFT de
déplacement, pas de périmètre de perturbation paramétrable : l'existant est épinglé, point.

### 4.2 Tranché — trois issues pour un créneau orphelin, dans cet ordre

| Rang | Issue | Description |
|---|---|---|
| 1 | **Reprise entière** | un créneau prévu du salarié remplacé est positionné sur son remplaçant estimé |
| 2 | **Reprise partielle** | le créneau est découpé et couvert en partie — **jamais de morceau de moins de 30 minutes** |
| 3 | **À pourvoir** | à défaut de disponibilité, la part non couverte devient du besoin résiduel |

Les trois issues coexistent sur un même créneau : une matinée reprise par Paul, un après-midi à
pourvoir, c'est une réponse valide et fréquente.

### 4.3 Tranché — il n'y a jamais zéro solution

> La solution est toujours : **« heures sur poste à pourvoir »**.

SC-02 ne rend jamais « pas de solution », jamais une erreur, jamais un planning vide. Ce qui n'a
pas pu être couvert est **chiffré et restitué** comme volume à pourvoir. C'est l'application
directe du principe du moteur : *il ne refuse pas, il rend visible l'impossible*.

### 4.4 Tranché — granularité du découpage : 30 minutes

Aucun morceau produit par un découpage ne fait moins de 30 minutes.

**Lecture retenue** (voir §9.1) : la règle porte sur les **morceaux produits**, pas sur les points
de coupe. La grille de 30 minutes est ancrée sur le début du créneau, et un reliquat de moins de
30 minutes est **absorbé par le dernier segment** plutôt que laissé seul. Un créneau de 7 h 15
produit donc 13 segments de 30 minutes et un dernier de 45 minutes — jamais un morceau de 15.

### 4.5 Tranché — surcharge : SOFT + alerte + niveau affiché

Traitement identique au plafond de dimanches arbitré au lot S7.1 : le dépassement est **signalé,
jamais éliminatoire**. Un seuil de surcharge est une borne de confort et d'équité, pas un seuil de
légalité — les bornes légales, elles, restent HARD et gardent leur rôle.

La surcharge s'exprime :

* en **heures / jour** lorsqu'elle porte sur une journée,
* en **heures / semaine** lorsqu'elle touche également la semaine.

Les deux grandeurs existent déjà et sont calculées : `ImpactMesureDTO` porte `heuresJour` et
`heuresSemaine` en avant / après / delta, avec le plafond individuel du salarié et un drapeau
`depassement`. SC-02 les réemploie sans les redéfinir (§7).

### 4.6 Tranché — à défaut de poste virtuel, ressource non affectée

Si le poste virtuel n'est pas autorisé et que personne ne peut couvrir, le créneau — ou le segment
— est restitué **sans ressource**, avec son alerte. Convention `RessourceNonAffectee` déjà en
vigueur partout ailleurs dans le moteur. Jamais une erreur, jamais un silence.

### 4.7 Tranché — `salarieAbsentId` entre au contrat

Le paramètre est retenu et **doit être documenté au contrat du scénario** (lot S1).

Il ne sert pas à la contrainte : l'absence elle-même est déjà portée par le bloc
`indisponibilites`, au contrat et tenue par la contrainte HARD `METIER_HARD_INDISPONIBILITE`.
`salarieAbsentId` sert à la **restitution** — sans lui, le moteur ne sait pas de quelle absence il
doit raconter les conséquences.

👉 Il n'y a donc **aucun champ nouveau à demander à WinDev pour décrire l'absence**. Conserver une
source unique évite précisément le genre de divergence que les lots S8.3 et S8.4 ont passé leur
temps à réparer.

---

## 5. Ce que le découpage impose au modèle

L'arbitrage §4.2 est le seul du lot qui touche à l'architecture. Il mérite d'être posé avant
d'écrire quoi que ce soit.

### 5.1 Le solveur ne crée pas d'entités

OptaPlanner affecte des valeurs à un ensemble d'entités **fixé à la construction du problème**.
« Découper un créneau » ne peut donc pas être une décision du solveur.

**Conséquence retenue** : le découpage est une **préparation, en amont de la résolution**.

1. Les créneaux du salarié absent sont pré-découpés en segments de 30 minutes (§4.4).
2. Chaque segment est une entité de planification ordinaire, dont la variable est la ressource ;
   le poste virtuel et `RessourceNonAffectee` sont des valeurs légitimes.
3. À la restitution, les segments **contigus affectés à la même ressource sont recombinés** en un
   créneau unique. Un créneau repris en entier par une seule personne ressort donc entier, comme
   s'il n'avait jamais été découpé.

Le volume reste modeste **parce que l'existant est épinglé** (§3) : seuls les créneaux de l'absent
sont découpés. Une absence d'une semaine à raison d'un créneau de 8 heures par jour produit
environ 80 segments — sans commune mesure avec un planning complet. Les deux arbitrages se
renforcent : c'est le refus de bouger l'existant qui rend le découpage praticable.

### 5.2 `duree` reste calculée une seule fois, mais plus toujours en amont du moteur

`20_DECISIONS_CONCEPTION_OPTAPLANNER.md` pose que `Creneau.duree` est la source de vérité unique
des agrégats et de la restitution — calculée à l'entrée, **jamais recalculée** ensuite.

Un segment est un créneau que le moteur fabrique : sa durée est nécessairement calculée par le
moteur. **L'invariant survit, sa formulation doit être précisée** : la durée est calculée *une
seule fois, avant la résolution*, et rien ne la recalcule ensuite. Ce qui change, c'est que
« l'amont » inclut désormais une étape de préparation côté moteur pour SC-02.

À écrire dans la décision d'architecture au moment de livrer le lot S2 — pas avant, pour ne pas
documenter une intention.

### 5.3 Identité des segments — ce que WinDev doit pouvoir rattacher

Un segment est un créneau qui n'existait pas dans la demande. Deux invariants du projet sont en
jeu : *l'identifiant reçu est restitué tel quel*, et *le moteur ne fabrique jamais d'`Id_Journee`*.

**Convention proposée** :

* un créneau **non découpé** conserve son identifiant **inchangé** — l'invariant est intact pour
  le cas courant ;
* un segment porte un identifiant **dérivé et explicite**, `<idOrigine>#S1`, `#S2`… ;
* la réponse porte un champ `creneauOrigineId` sur chaque segment, pour que le rattachement ne
  repose jamais sur l'analyse d'une chaîne.

Précédent : SC-01 fabrique déjà des identifiants (`SC01-<date>-<séquence>`) qui ne désignent
aucune ligne en base. Le principe n'est pas nouveau ; seule la convention de suffixe l'est.

👉 **À confirmer côté WinDev** (§9.2) : que le caractère `#` ne heurte aucun usage existant.

### 5.4 Fragmentation — la contrainte qui manque

Sans contrepoids, rien n'empêche seize segments d'aller à six personnes différentes. Le résultat
serait conforme aux règles et **inutilisable sur le terrain**.

Il faut donc une contrainte **SOFT de cohésion** : minimiser le nombre de ressources distinctes
affectées aux segments d'un même créneau d'origine.

C'est, à la forme près, exactement la contrainte de cohésion que le **rang 8** du backlog
(`blocJourId`, `groupeBesoinId`) devra introduire — voir `90_SUIVI_DEVELOPPEMENT_MOTEUR.md` §C.
SC-02 produira donc la première contrainte de cohésion du moteur, que le rang 8 pourra
généraliser plutôt que réinventer. L'inverse — attendre le rang 8, gelé jusqu'au 25/08/2026 —
n'apporterait rien.

---

## 6. Contrat d'entrée

### 6.1 Ce qui existe déjà et suffit

| Bloc | Rôle dans SC-02 |
|---|---|
| `indisponibilites` | **porte l'absence** — au contrat, mappé, tenu par une contrainte HARD |
| `creneaux[].ressourceAffecteeId` | planning existant (lot S1 de SC-06) |
| `salaries[].contrat` | base des seuils de surcharge (lot S2 de SC-06) |
| `salaries[].contraintesReglementaires` | bornes individuelles, actives depuis le chantier S7 |
| `planningContext.regulatoryParameters` | cadre réglementaire commun (lot S8.0) |

### 6.2 Ce qui est à ajouter

| Champ | Obligatoire | Rôle |
|---|:---:|---|
| `parametres.salarieAbsentId` | ✅ | désigne l'absence qui motive le scénario (§4.7) |
| `parametres.remplacantsAutorises[]` | ○ | restreint les candidats — voir §9.3 pour le sens de l'absence de liste |
| `parametres.posteVirtuelAutorise` | ○ | défaut `false` — un vide ne suppose jamais que la chose est possible |
| `parametres.surchargeMaxHeuresJour` | ○ | seuil de confort journalier (§4.5) |
| `parametres.surchargeMaxHeuresSemaine` | ○ | seuil de confort hebdomadaire (§4.5) |
| `parametres.decoupageAutorise` | ○ | défaut `true` — permet de désactiver la reprise partielle sans changer d'endpoint |

---

## 7. Contrat de sortie

SC-02 restitue le tronc commun (`planning`, `score`, `workMetrics`, `diagnostics`, `alerts`) et
**un bloc propre au scénario**, absent des autres — comme `candidats[]` l'est pour SC-06.

| Bloc | Contenu |
|---|---|
| `remplacement.creneauxReaffectes[]` | `creneauId`, `creneauOrigineId`, `ressourceAvant`, `ressourceApres`, `debut`, `fin`, `duree` |
| `remplacement.heuresAPourvoir` | volume résiduel total, en heures décimales (§4.3) |
| `remplacement.surchargeParRessource[]` | par salarié mobilisé : `heuresJour` et `heuresSemaine` en `ImpactMesureDTO` — avant / après / delta / plafond / dépassement |

`ImpactMesureDTO` et `ImpactCandidatDTO` sont réemployés **tels quels**, sans nouvelle structure
de mesure. La grandeur restituée par SC-06 pour un candidat et celle restituée par SC-02 pour un
remplaçant sont la même grandeur ; elles doivent rester le même objet.

---

## 8. Découpage proposé

Ordonné par dépendance. Chaque lot est livrable seul et testable seul.

| Lot | Objet | Pourquoi à ce rang |
|---|---|---|
| **S0** | Correctif : `IndisponibiliteSalarie` et le passage de minuit | Prérequis. La contrainte compare `creneau.getDate()` aux bornes de l'absence : un créneau du 3 mars 22:00 → 06:00 échappe à une absence du 4 mars. Bâtir SC-02 sur cette règle sans la réparer serait bâtir sur du sable |
| **S1** | Squelette SC-02 **sans découpage** : endpoint, contrat d'entrée, épinglage, reprise entière ou « à pourvoir » | Livrable et déjà utile seul. Couvre le cas majoritaire — un remplaçant prend la journée |
| **S2** | Découpage en segments, recombinaison à la restitution, contrainte SOFT de cohésion | Le cœur technique (§5). Isolé pour être évalué seul, y compris son effet sur le score |
| **S3** | Surcharge : seuils, alerte, niveaux restitués | Réemploi de `ImpactMesureDTO` ; aucune mesure nouvelle à écrire |
| **S4** | Restitution complète avant / après + inscription au contrat série 50 (OpenAPI, schémas JSON) | Une seule migration de contrat pour WinDev, à la fin, plutôt que quatre |
| **S5** | Canal FileAdapter (`scenarioType: SC-02`) | Symétrie avec SC-06 lot S6 : les deux canaux doivent produire le même résultat, vérifié par test |

**S0 et S1 sont immédiatement actionnables** : aucun arbitrage en attente, aucune dépendance.

---

## 9. Points ouverts

### 9.1 Lecture de la règle des 30 minutes

Retenu en §4.4 : la règle porte sur les **morceaux produits** — aucun n'est plus court que 30
minutes — et non sur les points de coupe. L'autre lecture possible (les coupes tombent sur des
multiples de 30 minutes, quitte à produire un reliquat plus court) est écartée : elle
contredirait la règle telle qu'énoncée. **À confirmer d'un mot.**

### 9.2 Convention d'identifiant des segments

`<idOrigine>#S1` proposé en §5.3, à confirmer côté WinDev — c'est le seul point du chantier qui
demande un accord de l'autre équipe.

### 9.3 Sens d'une liste de remplaçants absente

La liste, lorsqu'elle est fournie, est une **barrière** : seuls les salariés listés sont
candidats, et leur épuisement conduit à « à pourvoir » (§4.3), jamais à un élargissement
silencieux.

Reste le cas de la **liste absente**. L'invariant du projet — *un vide ne suppose jamais que la
chose est possible* — conduirait à « aucun remplaçant autorisé », donc à un scénario qui rend
tout à pourvoir : inutilisable.

**Lecture retenue par défaut** : liste absente ⇒ **tous les salariés du dataset sont candidats**.
C'est le seul endroit du moteur où cet invariant est délibérément écarté, et c'est pourquoi il est
écrit ici plutôt que subi. **À confirmer.**

---

## Annexe — méthode de vérification

Les affirmations de la §6.1 proviennent d'une lecture des appels réels dans `src/main/java`, en
écartant les accesseurs, les mappers et les tests. Vérifiés à la date de ce cadrage :

* `Indisponibilite` est lu par `IndisponibiliteSalarie`, enregistrée dans `ConstraintProviderImpl`
  — l'absence est bien tenue par une règle HARD, et non seulement transportée ;
* `toCreneauxFiges()` n'a qu'un appelant, `ScenarioSc06PreparationService` — le socle d'épinglage
  existe mais n'est câblé que dans SC-06 ;
* `ImpactMesureDTO` porte déjà `avant`, `apres`, `delta`, `plafond`, `depassement` ;
* `capaciteCible` du poste virtuel n'est lu par aucune contrainte : si SC-02 autorise le poste
  virtuel, **la capacité déclarée ne le bornera pas** tant que le rang 8 n'est pas tranché. À dire
  explicitement dans la notice d'intégration du lot S1.
