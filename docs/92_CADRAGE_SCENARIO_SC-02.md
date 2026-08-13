# 92 — Cadrage : SC-02, remplacement d'un salarié absent

> **Statut** : cadrage d'analyse, 2026-08-11. Les arbitrages de la §4 sont **tranchés** (métier,
> 2026-08-11, complétés le même jour sur le seuil des 30 minutes et sur la distinction poste
> virtuel / heures à pourvoir). La §9 porte le dernier point ouvert.
>
> ✅ **Chantier clos le 2026-08-13** : les six lots S0 à S5 de la §8 sont livrés. SC-02 est inscrit
> au contrat série 50 et accessible par les deux canaux, HTTP et FileAdapter. Ce document reste la
> référence des arbitrages métier ; l'état d'avancement vit dans
> `90_SUIVI_DEVELOPPEMENT_MOTEUR.md`.

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

### 4.4 Tranché — le seuil de 30 minutes porte sur ce qu'on confie à quelqu'un

> Un bloc confié à un salarié ne fait **jamais moins de 30 minutes**. Le reliquat non couvert, lui,
> n'a pas de minimum : il part en heures à pourvoir, quelle que soit sa durée.

Illustration sur un créneau à remplacer de **13h30 – 16h00** :

| Situation | Ce que fait le moteur |
|---|---|
| M. X prend habituellement son service à **15h30** | il se voit proposer **13h30 – 15h30**. Les 30 minutes restantes ne sont pas couvertes et partent à pourvoir |
| M. X commence à **13h45** | on ne lui propose **pas** 13h30 – 13h45 : quinze minutes ne se confient pas. Le fragment part à pourvoir |

Deux conséquences, qui corrigent la lecture initiale de ce cadrage :

1. **Les points de coupe ne viennent pas d'une grille**, mais des **frontières de disponibilité
   réelles** des remplaçants. 13h45 est un point de coupe légitime qu'aucune grille de 30 minutes
   ne produirait jamais.
2. **Le seuil ne s'applique pas au reliquat**, seulement au bloc effectivement confié — mesuré
   après recombinaison des segments contigus attribués à la même personne (§5.1).

### 4.5 Tranché — surcharge : SOFT + alerte + niveau affiché

Traitement identique au plafond de dimanches arbitré au lot S7.1 : le dépassement est **signalé,
jamais éliminatoire**. Un seuil de surcharge est une borne de confort et d'équité, pas un seuil de
légalité — les bornes légales, elles, restent HARD et gardent leur rôle.

La surcharge s'exprime :

* en **heures / jour** lorsqu'elle porte sur une journée,
* en **heures / semaine** lorsqu'elle touche également la semaine.

Les deux grandeurs existent déjà et sont calculées : `ImpactMesureDTO` porte avant / après / delta,
un plafond et un drapeau `depassement`. SC-02 le réemploie sans le redéfinir (§7).

⚠️ **Précision apportée à la livraison du lot S3** : le `plafond` restitué ici est le **seuil
déclaré par la demande**, pas le plafond individuel du salarié comme dans SC-06. Ce sont deux
notions distinctes — une borne de confort propre à cette situation, et une borne réglementaire
propre à la personne — et les mélanger dans un même champ aurait rendu le chiffre illisible. Les
bornes individuelles gardent leurs propres contraintes et leurs propres lignes au `scoreBreakdown`.

**Calibration de la pénalité, découverte à la livraison.** Ne pas couvrir un créneau coûte 2 000
points forfaitaires. Une pénalité de surcharge trop lourde rendrait donc l'abandon *moins cher* que
la couverture, et le seuil de confort se comporterait en interdit — l'inverse exact de l'arbitrage.
Retenu : 5 points la minute, soit 300 points l'heure de dépassement. Il faut plus de six heures
d'excédent pour égaler un créneau laissé à pourvoir.

### 4.6 Tranché — poste virtuel et heures à pourvoir sont deux notions distinctes

Ce sont deux réponses différentes à la même situation — des heures que personne ne couvre — et il
ne faut pas les confondre.

| | Quand | Ce que voit l'appelant |
|---|---|---|
| **Poste virtuel** | uniquement **si le scénario l'a demandé** (`posteVirtuelAutorise`) | les heures sont affectées à un poste fictif, qui apparaît comme une ressource du planning |
| **Heures à pourvoir** | sinon | le créneau est restitué **sans ressource** (`RessourceNonAffectee`), et les heures sont totalisées dans le résultat |

Le poste virtuel ne s'invite jamais de lui-même : il faut l'avoir demandé. C'est la même règle que
partout ailleurs — *un vide ne suppose jamais que la chose est possible*.

**Le total des heures à pourvoir compte les deux cas.** Du point de vue de l'encadrement, la
question est « combien d'heures me reste-t-il à staffer ? », et la réponse est la même que les
heures soient garées sur un poste fictif ou laissées vides. Le planning dit *où* elles sont ; le
total dit *combien* il y en a.

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

1. Pour chaque créneau orphelin, on rassemble les **frontières de disponibilité** des remplaçants
   éligibles qui tombent à l'intérieur du créneau — début de service habituel, fin d'un créneau
   déjà affecté, bord d'une indisponibilité, borne réglementaire atteinte.
2. Ces frontières découpent le créneau en segments. Chaque segment est une entité de planification
   ordinaire, dont la variable est la ressource ; le poste virtuel et `RessourceNonAffectee` sont
   des valeurs légitimes.
3. À la restitution, les segments **contigus affectés à la même ressource sont recombinés** en un
   créneau unique. Un créneau repris en entier par une seule personne ressort donc entier, comme
   s'il n'avait jamais été découpé.
4. Le seuil de 30 minutes (§4.4) s'applique **au bloc recombiné**, jamais aux segments
   intermédiaires : deux segments de 15 minutes confiés à la même personne forment un bloc de
   30 minutes, parfaitement valide. C'est ce bloc que la personne travaille réellement, donc c'est
   lui qu'il faut mesurer.

Une **grille fixe de 30 minutes**, envisagée dans la première version de ce cadrage, est écartée :
elle produirait à la fois plus d'entités et des coupes qui ne correspondent à rien. Une coupe n'a
de sens que là où la disponibilité de quelqu'un change.

Le volume reste donc faible sur deux tableaux : les frontières sont peu nombreuses — une ou deux
par remplaçant éligible — et **seuls les créneaux de l'absent sont découpés**, puisque l'existant
est épinglé (§3). Les deux arbitrages se renforcent : c'est le refus de bouger l'existant qui rend
le découpage praticable.

### 5.2 `duree` reste calculée une seule fois, mais plus toujours en amont du moteur

`20_DECISIONS_CONCEPTION_OPTAPLANNER.md` pose que `Creneau.duree` est la source de vérité unique
des agrégats et de la restitution — calculée à l'entrée, **jamais recalculée** ensuite.

Un segment est un créneau que le moteur fabrique : sa durée est nécessairement calculée par le
moteur. **L'invariant survit, sa formulation doit être précisée** : la durée est calculée *une
seule fois, avant la résolution*, et rien ne la recalcule ensuite. Ce qui change, c'est que
« l'amont » inclut désormais une étape de préparation côté moteur pour SC-02.

À écrire dans la décision d'architecture au moment de livrer le lot S2 — pas avant, pour ne pas
documenter une intention.

### 5.3 Identité des segments — une convention interne, pas une négociation

Un segment est un créneau qui n'existait pas dans la demande. Deux invariants du projet sont en
jeu : *l'identifiant reçu est restitué tel quel*, et *le moteur ne fabrique jamais d'`Id_Journee`*.

**Convention retenue** :

* un créneau **non découpé** conserve son identifiant **inchangé** — l'invariant est intact pour
  le cas courant, qui est le cas majoritaire ;
* un segment porte un identifiant **dérivé et explicite**, `<idOrigine>#S1`, `#S2`… ;
* la réponse porte un champ `creneauOrigineId` sur chaque segment, pour que le rattachement ne
  repose jamais sur l'analyse d'une chaîne de caractères.

**Rien de tout cela ne demande d'arbitrage à l'équipe WinDev** : elle envoie des créneaux et en
reçoit, comme aujourd'hui. Le précédent existe déjà — SC-01 fabrique des identifiants
(`SC01-<date>-<séquence>`) qui ne désignent aucune ligne en base.

Un seul fait est à écrire au contrat de sortie, non comme une question mais comme une
information : **la réponse peut contenir plus de créneaux que la demande**, lorsqu'un créneau a
été couvert en deux fois.

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
| `parametres.posteVirtuelAutorise` | ○ | défaut `false` — le poste virtuel ne s'invite jamais de lui-même (§4.6) |
| `parametres.surchargeMaxHeuresJour` | ○ | seuil de confort journalier (§4.5) |
| `parametres.surchargeMaxHeuresSemaine` | ○ | seuil de confort hebdomadaire (§4.5) |
| `parametres.decoupageAutorise` | ○ | défaut `true` — permet de désactiver la reprise partielle sans changer d'endpoint |

---

## 7. Contrat de sortie

SC-02 restitue le tronc commun (`planning`, `score`, `workMetrics`, `diagnostics`, `alerts`) et
**un bloc propre au scénario**, absent des autres — comme `candidats[]` l'est pour SC-06.

| Bloc | Contenu |
|---|---|
| `remplacement.details[]` | `creneauId`, `creneauOrigineId`, `ressourceAvantId`, `ressourceApresId`, `date`, `heureDebut`, `heureFin`, `dureeMinutes`, `nature` |
| `remplacement.heuresAPourvoir` | volume total non couvert par un salarié réel, en heures décimales — **poste virtuel compris** (§4.6) |
| `remplacement.surchargeParRessource[]` | par salarié mobilisé : `heuresJour` et `heuresSemaine` en `ImpactMesureDTO` — avant / après / delta / plafond / dépassement |

⚠️ **Complété au lot S4.** Le bloc ne comptait que des créneaux et ne chiffrait que les heures
restées à pourvoir : l'encadrement lisait « un créneau sur deux repris » sans savoir de combien
d'heures on parlait. S'y ajoutent donc les volumes `heuresLiberees`, `heuresReprises`,
`heuresSurPosteVirtuel` et `heuresNonCouvertes`, qui se recomposent
(`heuresLiberees = heuresReprises + heuresSurPosteVirtuel + heuresNonCouvertes`), ainsi que
`creneauxPartiellementRepris` — un créneau repris en partie n'est ni repris ni abandonné, et le
ranger d'un côté ou de l'autre faisait mentir les deux décomptes.

Le bloc `planning` dit **où** sont les heures non couvertes — sur un poste virtuel, ou sans
ressource ; `heuresAPourvoir` dit **combien** il y en a. Les deux sont nécessaires et ne se
déduisent pas l'un de l'autre.

⚠️ La réponse peut contenir **plus de créneaux que la demande** lorsqu'un créneau a été couvert en
deux fois (§5.3).

`ImpactMesureDTO` et `ImpactCandidatDTO` sont réemployés **tels quels**, sans nouvelle structure
de mesure. La grandeur restituée par SC-06 pour un candidat et celle restituée par SC-02 pour un
remplaçant sont la même grandeur ; elles doivent rester le même objet.

---

## 8. Découpage proposé

Ordonné par dépendance. Chaque lot est livrable seul et testable seul.

| Lot | Objet | Pourquoi à ce rang |
|---|---|---|
| **S0** ✅ | Correctif : `IndisponibiliteSalarie` et le passage de minuit | Prérequis. La contrainte compare `creneau.getDate()` aux bornes de l'absence : un créneau du 3 mars 22:00 → 06:00 échappe à une absence du 4 mars. Bâtir SC-02 sur cette règle sans la réparer serait bâtir sur du sable. **Livré le 2026-08-11** — le défaut avait un second lecteur, le filtre d'éligibilité de SC-06 |
| **S1** ✅ | Squelette SC-02 **sans découpage** : endpoint, contrat d'entrée, épinglage, reprise entière ou « à pourvoir » | Livrable et déjà utile seul. Couvre le cas majoritaire — un remplaçant prend la journée. **Livré le 2026-08-11**, précédé de l'extraction de la préparation dataset commune à SC-02 et SC-03 |
| **S2** ✅ | Découpage aux frontières de disponibilité, recombinaison à la restitution, seuil des 30 minutes sur le bloc confié, contrainte SOFT de cohésion | Le cœur technique (§5). Isolé pour être évalué seul, y compris son effet sur le score. **Livré le 2026-08-11** |
| **S3** ✅ | Surcharge : seuils, alerte, niveaux restitués | Réemploi de `ImpactMesureDTO` ; aucune mesure nouvelle à écrire. **Livré le 2026-08-11** |
| **S4** ✅ | Restitution complète avant / après + inscription au contrat série 50 (OpenAPI, schémas JSON) | Une seule migration de contrat pour WinDev, à la fin, plutôt que quatre. **Livré le 2026-08-13** — trois divergences de contrat trouvées en écrivant, dont deux qui auraient fait rejeter une requête ou une réponse conforme |
| **S5** ✅ | Canal FileAdapter (`scenarioType: SC-02`) | Symétrie avec SC-06 lot S6 : les deux canaux doivent produire le même résultat, vérifié par test. **Livré le 2026-08-13** — la symétrie est prouvée en comparant les deux réponses entières, et non en réaffirmant les mêmes valeurs des deux côtés. Un contrôle de strictness annoncé mais inexistant a été découvert au passage |

**S0 et S1 sont immédiatement actionnables** : aucun arbitrage en attente, aucune dépendance.

---

## 9. Point ouvert — sens d'une liste de remplaçants absente

La liste, lorsqu'elle est fournie, est une **barrière** : seuls les salariés listés sont
candidats, et leur épuisement conduit aux heures à pourvoir (§4.3, §4.6), jamais à un
élargissement silencieux.

Reste le cas de la **liste absente**. L'invariant du projet — *un vide ne suppose jamais que la
chose est possible* — conduirait à « aucun remplaçant autorisé », donc à un scénario qui rend tout
à pourvoir : inutilisable.

**Décision retenue par défaut, sauf avis contraire** : liste absente ⇒ **tous les salariés du
dataset sont candidats**. C'est le seul endroit du moteur où cet invariant est délibérément
écarté, et c'est pourquoi il est écrit ici plutôt que subi.

Le lot S1 est réalisable sous cette hypothèse ; l'inverser plus tard ne coûterait qu'un filtre.

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

---

## Annexe 2 — ce que le lot S1 a appris

⚠️ **`activitesCompatibles` n'est lu par aucune contrainte** (rang 10 du backlog). SC-02 peut donc
confier un créneau à un salarié qui ne pratique pas l'activité. Les seules règles qui écartent
réellement un remplaçant sont, à ce jour, les contraintes HARD en vigueur : chevauchement
physique, indisponibilité, jour férié refusé.

Ce manque était supportable tant que le moteur ne faisait qu'analyser ou classer. SC-06 s'en
protégeait par un filtre d'éligibilité écrit dans son énumération, **hors solveur** — il pouvait
se le permettre, il n'affecte rien. Un scénario qui affecte réellement n'a pas cette échappatoire :
c'est SC-02 qui rend le rang 10 coûteux, et c'est un argument à verser au dossier de la Production.

Le jeu d'essai du lot construit donc son cas « à pourvoir » sur un **chevauchement physique**, règle
en vigueur, et non sur l'activité, règle attendue. Un test adossé à une règle qu'on espère écrire
un jour ne prouve rien.
