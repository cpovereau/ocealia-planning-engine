# 📄 50_SCENARIO_CONTRACT.md

**Contrat de scénarios — Interface WebDev ↔ Moteur de planification**

---

## 1. Objectif du document

Ce document définit les **scénarios de résolution exposés par le moteur de planification**.

Il précise, pour chaque scénario :

* l’intention métier ;
* les paramètres attendus ;
* les données transmises au moteur ;
* la forme de la restitution attendue.

👉 Ce document **ne décrit ni l’IHM, ni l’implémentation OptaPlanner**.
Il constitue un **contrat fonctionnel stable** entre WebDev et Spring Boot.

---

## 2. Principe structurant

> Le moteur de planification **ne reçoit jamais une demande libre**.
> Il reçoit **un scénario identifié**, avec un périmètre et des paramètres contrôlés.

**Conséquences :**

* l’utilisateur choisit **un scénario métier** ;
* chaque scénario :

  * ouvre certains paramètres ;
  * en interdit d’autres ;
* le solveur **ne déduit jamais l’intention utilisateur**.

---

## 3. Structure générique d’un scénario

Chaque appel au moteur respecte la structure logique suivante :

```json
{
  "scenarioType": "SCENARIO_CODE",
  "planningContext": { ... },
  "scenarioParameters": { ... },
  "dataSet": { ... }
}
```

### 3.1 `scenarioType`

Identifiant **fermé et versionné** du scénario métier.

### 3.2 `planningContext`

Cadre commun à tous les scénarios :

* horizon temporel ;
* stratégie de scoring ;
* seuils de tolérance ;
* options d’explicabilité.

### 3.3 `scenarioParameters`

Paramètres **spécifiques au scénario**, validés côté API.

### 3.4 `dataSet`

Données métier normalisées :

* créneaux ;
* ressources ;
* paramètres réglementaires.

---

## 3.5 Évolution du DataSet amont (V2 progressive)

Le `dataSet` constitue l’interface principale entre **WebDev** et le moteur de planification.

La version initiale (V1) repose sur une structure simple :
- `creneaux`
- `ressources`
- paramètres réglementaires

Cette structure reste **le socle contractuel actuel**.

Toutefois, afin de mieux représenter les données métier issues du logiciel de planning,
une évolution progressive du DataSet est introduite.

### Objectifs

Permettre de représenter explicitement :

- les **besoins de couverture** ;
- les **affectations existantes** issues du planning ;
- les **indisponibilités** des ressources ;
- les **axes organisationnels** du logiciel métier.

### Axes organisationnels supportés

Le DataSet peut désormais porter les axes organisationnels suivants :

- `direction`
- `service`
- `lieu`
- `posteComptable`

Ces axes peuvent être présents sur :

- les ressources
- les créneaux
- les besoins

### Données portées par la ressource

Les ressources peuvent désormais transmettre :

- un bloc `contratTravail`
- un bloc `contraintesReglementaires`

Ces informations proviennent directement du logiciel de planning.

### Structuration des besoins

Afin de préparer l'évolution vers une représentation explicite des besoins,
les champs suivants peuvent être transmis sur les créneaux :

- `groupeBesoinId`
- `blocJourId`
- `ordreDansBloc`
- `estSegmentDePause`

Ces champs permettent de reconstruire la logique métier d’un besoin
sans modifier le modèle conceptuel du moteur.

### Identité des créneaux

Le champ `id` d’un créneau est **requis et unique** sur l’ensemble d’un scénario,
quelle que soit l’origine du créneau. Cette exigence est technique : elle conditionne
la résolution elle-même, pas seulement la restitution.

WinDev renseigne ce champ dans tous les cas :

| Nature du créneau transmis                  | Contenu de l’`id`                    |
| ------------------------------------------- | ------------------------------------ |
| Créneau servi, enregistré en base           | la clé primaire WinDev (`Id_Journee`) |
| Besoin à couvrir, sans ligne en base        | un identifiant dédié (`BES-00X`)      |

Un scénario peut mêler les deux natures — c’est le cas de SC-03, qui transmet
à la fois des créneaux actuellement servis et des besoins potentiellement non servis.

**Le moteur traite `id` comme une chaîne opaque.** Il ne l’interprète pas, ne le parse pas,
et n’en déduit aucun comportement. La convention de préfixe appartient à WinDev, qui est
seul à la lire — au retour, pour décider entre mise à jour et création.

Les créneaux que le moteur génère lui-même (cas de SC-01) reçoivent un identifiant sous
son propre préfixe, `SC01-<date>-<séquence>`. Le moteur ne fabrique jamais d’`Id_Journee`.

Voir `20_DECISIONS_CONCEPTION_OPTAPLANNER.md` — *Identité des créneaux et clé de
réintégration WinDev*.

### Trajectoire d'évolution

Deux niveaux sont définis :

**V1 (actuelle)**

dataSet
├─ ressources
└─ creneaux


**V2 cible**

dataSet
├─ referentiels
├─ ressources
├─ besoins
├─ affectationsExistantes
└─ indisponibilites

Cette évolution est progressive et n'affecte pas le fonctionnement
des scénarios existants.

---

## 3.6 Planning existant et créneaux figés

Un créneau du `dataSet` peut porter `ressourceAffecteeId` : l'identifiant de la ressource qui le
sert déjà. Le créneau relève alors d'un **planning existant transmis comme fait acquis**.

Le **scénario** décide de ce qu'il en fait — l'appelant ne pilote pas le figement, il désigne une
affectation. SC-06 fige tout son `dataSet` ; **SC-02 fige tout sauf les créneaux du salarié absent
que son absence recouvre** ; SC-01 et SC-03 ignorent le champ et conservent leur liberté de
décision entière.

Un créneau figé reste **pleinement visible des contraintes** : c'est ce qui permet à un planning
existant de peser sur les décisions restantes sans être lui-même remis en cause.

Voir `20_DECISIONS_CONCEPTION_OPTAPLANNER.md` — *Créneau figé : un fait d'entrée, pas une décision*.

---

## 3.7 Les seuils réglementaires sont individuels

Toute limite réglementaire est portée par le **salarié**, dans son bloc
`contraintesReglementaires`. Trois salariés = trois jeux de seuils. Le moteur n'applique aucun
plafond global : il n'a pas à supposer qu'une règle vaut pour tout le monde.

Le bloc compte treize champs. Les cinq derniers ont été rapatriés au **lot S7.0** depuis
`SeuilsDeTolerance`, où ils étaient globaux et — c'est le point — **jamais alimentés** :

| Champ | Ce qu'il plafonne |
|---|---|
| `nuitsConsecutivesMaximum` | nuits d'affilée |
| `joursReposMinimumApresNuits` | jours de repos exigés après une séquence de nuits |
| `dimanchesTravaillesMaximum` | dimanches travaillés sur la période |
| `reposHebdomadaireFenetreJours` | largeur de la fenêtre glissante |
| `reposHebdomadaireJoursOffMinimum` | jours non travaillés exigés dans cette fenêtre |

Les deux derniers forment une **paire indissociable** : une fenêtre sans minimum de jours off
n'interdit rien. Transmis seul, l'un des deux laisse la contrainte inactive et déclenche un WARN.

### Seule l'absence désactive — le 0 est lu à la lettre

> Pour désactiver une limite : **omettre le champ**.
> Pour interdire complètement quelque chose : **envoyer 0**.

| Ce que vous envoyez | Ce que le moteur applique |
|---|---|
| champ absent | aucune règle — le moteur s'abstient de juger |
| `nuitsMaximumParSemaine: 0` | **aucune nuit autorisée** |
| `heuresMinimumParSemaine: 0` | aucun minimum exigé |
| valeur négative | ignorée, tracée en WARN — une borne négative ne décrit rien |

Le zéro garde son sens arithmétique : un **maximum** à 0 interdit tout, un **minimum** à 0
n'exige rien. C'est l'invariant « un vide ne suppose jamais que la chose est possible » appliqué
au bon niveau — un vide est une absence d'information, pas un chiffre.

**Une exception, `reposHebdomadaireFenetreJours`.** Une fenêtre est une *taille*, pas une borne :
« au moins 2 jours off sur 0 jour » ne décrit aucune règle. Ce champ est donc pris en compte à
partir de 1 jour ; à 0, la contrainte reste inactive.

### Renseigner un seuil ne suffit pas à le voir appliqué

Le transport d'un seuil et son application par une contrainte sont deux choses distinctes, et le
second est en cours de rattrapage — un lot par contrainte. Avant de faire d'un champ une règle de
gestion, vérifier son état dans `90_SUIVI_DEVELOPPEMENT_MOTEUR.md`.

---

## 4. Scénarios supportés (V1)

---

### 🟢 SC-01 — Conception d’un planning pour un nouveau salarié

#### 🎯 Intention métier

Construire un **planning de référence** conforme aux règles, sans historique.

#### Paramètres spécifiques

* resourceRef (SALARIE ou POSTE_VIRTUEL) ;
* période de planification (planningContext.horizon) ;
* dailyAmplitudeHours (incluant pause réglementaire) ;
* shiftStart ;
* shiftEndAlert (borne d’alerte, non bloquante) ;
* lunchBreak (optionnel, défaut 12:00–13:00) ;
* workedDays (DayOfWeek ISO : MONDAY…SUNDAY) ;
* holidayDates (jours non travaillés) ;
* codeActiviteId (code activité des créneaux générés, issu du référentiel client —
  optionnel pendant la transition, repli signalé sur `travail`).

#### Données clés transmises

* aucune affectation transmise ;
* ressource cible (salarié réel ou poste virtuel fourni) ;
* génération des créneaux réalisée côté moteur ;
* jours fériés explicitement transmis ;
* absence d’historique préalable.

#### Restitution attendue

* planning généré ;
* alertes de cohérence (pause, dépassement borne, repos insuffisant) ;
* aucune optimisation à ce stade (génération déterministe) ;
* pas encore d’indicateurs de charge.

---

### 🟡 SC-02 — Remplacement d’un salarié absent

> Inscrit au contrat le 2026-08-13, lot S4. Cadrage complet : `92_CADRAGE_SCENARIO_SC-02.md`.
> Schémas normatifs : `Sc02ScenarioRequest` et `Remplacement` dans
> `50_openapi_windev_moteur_v_1.yaml`, bloc de sortie détaillé dans
> `50_SCENARIO_RESPONSE_CONTRACT.md` §7.
>
> **Deux canaux, un seul comportement** : `POST /scenarios/sc02/solve` et le **FileAdapter**
> (`scenarioType: SC-02`, lot S5). Ils entrent dans le même service au même point ; un test
> compare leurs deux réponses entières.

#### 🎯 Intention métier

Un salarié est absent, ses créneaux sont orphelins. **Qui les reprend, et à quel prix — sans
défaire le planning de ceux qui n'ont rien demandé ?**

Assurer la continuité de service **en perturbant le moins possible l’existant**. Toute la
difficulté tient dans la seconde moitié de cette phrase.

#### Ce qui distingue SC-02

| | SC-02 |
|---|---|
| Ce qui est décidé | l'affectation des seuls créneaux du salarié absent que son absence recouvre |
| Ce qui est figé | l'intégralité du reste du planning transmis |
| Ce qui est restitué | un planning ajusté, les différences avant / après, et le volume à pourvoir |

Par rapport aux scénarios voisins : **SC-06** part d'un besoin nu et **classe** des possibilités
sans rien réaffecter ; **SC-03** réaffecte un sous-ensemble de créneaux, mais sans planning
préexistant à préserver ; **SC-01** construit depuis rien, quand SC-02 répare.

#### Principe structurant — l'existant est un fait, jamais une variable

> Seuls les créneaux du salarié absent sont décidés. Tout le reste du planning transmis est
> **épinglé** — y compris les créneaux de l'absent situés **hors** de sa période d'absence. Le
> solveur le voit, les contraintes le mesurent, mais aucune affectation existante n'est déplacée
> pour faire de la place.

Le prix est assumé : le moteur répondra « à pourvoir » là où un échange entre deux collègues aurait
suffi. **C'est voulu.** Remanier le planning des présents est une décision d'encadrement, pas une
décision de moteur.

> **Il n'y a jamais zéro solution.** SC-02 ne rend jamais « pas de solution », jamais une erreur,
> jamais un planning vide. Ce qui n'a pas pu être couvert est **chiffré et restitué** comme volume
> à pourvoir.

#### Paramètres spécifiques

Le bloc `scenarioParameters` est **strict** : un champ inconnu produit une erreur `UNKNOWN_FIELD`
qui donne son chemin, jamais un silence. Il n'expose que ce que le moteur honore — un paramètre que
personne ne lit est pire qu'un paramètre absent, puisque l'appelant le renseigne et croit l'avoir
dit. C'est la règle générale du contrat d'entrée, voir `50_SCENARIO_TECHNICAL_CONTRACT.md` §3.

| Champ | Obligatoire | Rôle |
|---|:---:|---|
| `salarieAbsentId` | ✅ | Désigne le salarié dont l'absence motive le scénario. Il ne sert **pas** à la contrainte : l'absence elle-même est portée par `dataSet.indisponibilites`, déjà tenu par une contrainte HARD. Il sert à savoir de quelle absence tirer les conséquences |
| `posteVirtuelAutorise` | ○ | Défaut `false`. Le poste virtuel ne s'invite jamais de lui-même |
| `surchargeMaxHeuresJour` | ○ | Charge journalière au-delà de laquelle un remplaçant est jugé en surcharge, en heures décimales |
| `surchargeMaxHeuresSemaine` | ○ | Idem sur la semaine calendaire lundi → dimanche |

**Non retenus à ce jour**, faute de règle qui les lise : `remplacantsAutorises[]` — tous les
salariés du dataset sont candidats — et `decoupageAutorise`, la reprise partielle étant toujours
permise. Ils entreront au contrat avec le lot qui les mettra en œuvre.

#### Données clés transmises

| Bloc | Rôle dans SC-02 |
|---|---|
| `dataSet.creneaux[].ressourceAffecteeId` | le planning existant. C'est ce champ qui sépare les créneaux à remplacer de l'existant à préserver. Une ressource introuvable dans le dataset fait **rejeter la demande**, plutôt que de laisser un créneau glisser en besoin nu |
| `dataSet.indisponibilites.items` | **porte l'absence**. Sans elle, aucun créneau n'est libéré et une alerte `AUCUNE_ABSENCE_DECLAREE` le dit : un vide ne vaut pas absence sur tout l'horizon |
| `dataSet.referentiels.activites` | nécessaire pour que la charge se calcule (`compteDansCharge`) |
| `salaries[].contraintesReglementaires` | bornes individuelles, actives — distinctes des seuils de surcharge |

👉 **Aucun champ nouveau n'a été créé pour décrire l'absence.** Conserver une source unique évite
précisément le genre de divergence que les lots S8.3 et S8.4 ont passé leur temps à réparer.

#### Couverture partielle

Quand aucun remplaçant n'est disponible sur toute la durée d'un créneau libéré, celui-ci est
couvert en partie. Les coupes tombent aux **frontières de disponibilité réelles** des remplaçants —
début ou fin d'un de leurs créneaux, bord d'une de leurs absences — et jamais sur une grille
horaire.

* **Un bloc confié à un salarié ne fait jamais moins de 30 minutes.** Sur un créneau 13h30–16h00,
  un remplaçant qui prend son service à 13h45 ne se verra pas proposer les quinze minutes qui
  précèdent.
* **Le reliquat non couvert n'a aucun minimum** : il part à pourvoir tel qu'il est.
* Un même besoin éclaté entre plusieurs personnes est **pénalisé** — sans être interdit : mieux
  vaut deux remplaçants que des heures à pourvoir.

⚠️ **La réponse peut donc contenir plus de créneaux que la demande.** Un créneau couvert en deux
fois ressort en deux entrées, dont les identifiants sont dérivés du sien — `<id>#S1`, `<id>#S2` —
et qui portent un `creneauOrigineId` pour être rattachées sans analyser la chaîne. Un créneau
repris en entier par une seule personne **garde son identifiant inchangé**, comme s'il n'avait
jamais été découpé : c'est le cas courant. Le découpage est signalé par une alerte
`CRENEAUX_DECOUPES` de sévérité `INFO`.

#### Seuils de surcharge

Ce sont des bornes de **confort**, propres à la demande, à ne pas confondre avec les bornes
réglementaires individuelles du salarié qui gardent leur rôle. Leur dépassement est **pesé dans le
score et signalé** par une alerte `SURCHARGE_ACCEPTABLE_DEPASSEE` (WARNING), **jamais
éliminatoire** : le moteur préfère confier un remplacement en surcharge plutôt que de laisser des
heures à pourvoir. Une borne absente n'est pas une borne à zéro.

#### Restitution attendue

Un bloc `remplacement`, propre à SC-02 et absent des autres scénarios — voir
`50_SCENARIO_RESPONSE_CONTRACT.md` §7 pour le détail champ par champ. Il porte :

* **ce que l'absence a libéré et ce qu'il en est advenu**, en créneaux et en heures :
  `creneauxLiberes` / `creneauxRepris` / `creneauxPartiellementRepris`, et
  `heuresLiberees` = `heuresReprises` + `heuresSurPosteVirtuel` + `heuresNonCouvertes` ;
* **le sort de chaque morceau** dans `details[]`, y compris ceux que personne n'a repris — un
  remplacement qui n'a pas eu lieu est une information, pas un silence ;
* **ce que le remplacement coûte à ceux qui l'assurent**, dans `surchargeParRessource[]`.

Le bloc `planning` porte le **planning ajusté complet**, l'existant épinglé compris : l'appelant
peut recharger la réponse telle quelle.

> **Poste virtuel et heures à pourvoir sont deux notions distinctes**, pas une solution et son
> repli. Le poste virtuel n'existe que si le scénario l'a demandé ; sinon les heures reviennent
> sans ressource. Mais `heuresAPourvoir` **compte les deux** : du point de vue de l'encadrement, la
> question est « combien me reste-t-il à staffer ? », et la réponse est la même dans les deux cas.
> Le planning dit **où** elles sont, le total dit **combien** il y en a.

#### ⚠️ Limites connues, et elles comptent

* **`activitesCompatibles` n'est lu par aucune contrainte** (rang 10 du backlog). Un salarié peut
  donc se voir confier un créneau dont il ne pratique pas l'activité. Les seules règles qui
  écartent réellement un remplaçant sont, à ce jour : chevauchement physique, indisponibilité, jour
  férié refusé. C'est SC-02 qui rend ce manque coûteux — SC-06 s'en protégeait par un filtre
  d'éligibilité hors solveur, un scénario qui affecte réellement n'a pas cette échappatoire.
* **`capaciteCible` du poste virtuel ne borne rien** (rang 8). Le volume qu'on y gare n'est pas
  limité par la capacité déclarée.

Voir `90_SUIVI_DEVELOPPEMENT_MOTEUR.md` avant de faire de l'un ou l'autre une règle de gestion.

**Ce que le moteur fait.** Seuls les créneaux du salarié absent que son absence recouvre sont
rendus au solveur. Tout le reste du planning transmis est épinglé — aucune affectation existante
n'est déplacée pour faire de la place, y compris les créneaux de l'absent situés hors de sa
période d'absence.

**Couverture partielle (lot S2).** Quand aucun remplaçant n'est disponible sur toute la durée d'un
créneau libéré, celui-ci est couvert en partie. Les coupes tombent aux **frontières de
disponibilité réelles** des remplaçants — début ou fin d'un de leurs créneaux, bord d'une de leurs
absences — et jamais sur une grille horaire.

* **Un bloc confié à un salarié ne fait jamais moins de 30 minutes.** Sur un créneau 13h30–16h00,
  un remplaçant qui prend son service à 13h45 ne se verra pas proposer les quinze minutes qui
  précèdent.
* **Le reliquat non couvert n'a aucun minimum** : il part à pourvoir tel qu'il est.
* Un même besoin éclaté entre plusieurs personnes est **pénalisé** — sans être interdit : mieux
  vaut deux remplaçants que des heures à pourvoir.

⚠️ **La réponse peut donc contenir plus de créneaux que la demande.** Un créneau couvert en deux
fois ressort en deux entrées, dont les identifiants sont dérivés du sien — `<id>#S1`, `<id>#S2` —
et qui portent un `creneauOrigineId` pour être rattachées sans analyser la chaîne. Un créneau
repris en entier par une seule personne **garde son identifiant inchangé**, comme s'il n'avait
jamais été découpé : c'est le cas courant. Le découpage est signalé par une alerte
`CRENEAUX_DECOUPES` de sévérité `INFO`.

**Seuils de surcharge (lot S3).** Ce sont des bornes de **confort**, propres à la demande, à ne pas
confondre avec les bornes réglementaires individuelles du salarié qui gardent leur rôle. Leur
dépassement est **pesé dans le score et signalé** par une alerte
`SURCHARGE_ACCEPTABLE_DEPASSEE` (WARNING), **jamais éliminatoire** : le moteur préfère confier un
remplacement en surcharge plutôt que de laisser des heures à pourvoir. Une borne absente n'est pas
une borne à zéro — sans seuil déclaré, aucun dépassement n'est possible de ce côté.

**Ce que le moteur restitue** : un bloc `remplacement`, propre à SC-02 et absent des autres
scénarios — `salarieAbsentId`, `creneauxLiberes` (créneaux d'origine, que le découpage ne gonfle
pas), `creneauxRepris` (repris **en entier**), `heuresAPourvoir`, le `details[]` du sort de chaque
morceau restitué, y compris ceux que personne n'a repris, et `surchargeParRessource[]`.

Ce dernier porte, **par remplaçant et par jour repris**, la charge `heuresJour` et la charge
`heuresSemaine` en `avant / apres / delta`, avec le `plafond` déclaré et un drapeau `depassement` —
mêmes structures que les `impacts[]` de SC-06. « Avant » est la situation qu'on aurait eue sans
remplacement, c'est-à-dire le planning épinglé du salarié ; le delta est donc exactement ce que
l'absence lui a coûté. Les mesures sont rendues **même sans seuil déclaré** : elles informent, et
le `plafond` vaut alors `null`.

⚠️ **Limite connue de ce lot** : le moteur n'oppose pas encore `activitesCompatibles` à une
affectation — aucune contrainte ne lit ce champ (rang 10 du backlog). Un salarié peut donc se voir
confier un créneau dont il ne pratique pas l'activité. Les seules règles qui écartent réellement un
remplaçant sont, à ce jour, les contraintes HARD en vigueur : chevauchement physique,
indisponibilité, jour férié refusé.

---

### 🔵 SC-03 — Ajustement ponctuel / événementiel

#### 🎯 Intention métier

Traiter un **déséquilibre localisé** sans recalcul global.

#### Paramètres spécifiques

* période courte ;
* lieux concernés ;
* contraintes temporaires ;
* priorité de couverture.

#### Données clés transmises

* sous-ensemble de créneaux ;
* ressources locales.

#### Restitution attendue

* planning local corrigé ;
* zones impactées identifiées ;
* compromis réalisés.

---

### 🟢 SC-04 — Optimisation globale d’un planning existant

> Inscrit au contrat le 2026-08-17, lot O4. Cadrage complet : `92_CADRAGE_SCENARIO_SC-04.md`.
> Arbitrages métier rendus le 2026-08-17, réalisation en cinq lots O0 à O4.
> Schémas normatifs : `Sc04ScenarioRequest` et `Optimisation` dans
> `50_openapi_windev_moteur_v_1.yaml`, bloc de sortie détaillé dans
> `50_SCENARIO_RESPONSE_CONTRACT.md` §9.
>
> **Deux canaux, un seul comportement** : `POST /scenarios/sc04/solve` et le **FileAdapter**
> (`scenarioType: SC-04`). Ils entrent dans le même service au même point ; un test compare leurs
> deux réponses entières.

#### 🎯 Intention métier

Améliorer un planning réel **sans le reconstruire entièrement**.

Ce que SC-04 apporte et qu’aucun autre scénario n’apporte est la **profondeur temporelle** : il est
le seul à juger une *période* plutôt qu’un instant. Les autres répondent à une question posée sur un
moment — une absence, un besoin, un périmètre. SC-04 relit une durée et montre **quand** un
déséquilibre s’est installé.

#### Paramètres spécifiques

| Champ | Rôle |
|---|---|
| `scenarioParameters.datePivot` | **obligatoire** — premier jour ajustable. Tout ce qui précède est figé |

**Un seul champ, et c’est le degré de liberté.** Le jour du pivot lui-même est ajustable : « à
partir de », non « après ». Un pivot hors de l’horizon est accepté et **signalé** — le moteur ne
refuse pas, il rend visible ce que la demande implique.

🔁 La **liste explicite** de créneaux ajustables **n’est pas encore exposée** — ne l’envoyez pas,
elle serait ignorée. Sa forme est en revanche arrêtée depuis le 2026-08-18 (§5.5 du cadrage) : un
champ optionnel `scenarioParameters.creneauxAjustables`, qui viendra **à côté** de `datePivot` et
non à sa place, en **intersection** avec lui — ajustable = après le pivot *et* dans la liste — et
borné à un mois. Un appelant qui ne la transmettra pas gardera le comportement décrit ci-dessus,
inchangé. Ce paragraphe sera remplacé par une ligne du tableau le jour de sa mise en service.

> ⚠️ **Les « priorités d’optimisation » et la « pondération des règles » annoncées jusqu’ici sont
> retirées.** Le moteur tient qu’on ne pondère pas une mesure dont l’échelle n’est pas calibrée, et
> les coefficients de pénibilité ne le sont pas (`92_CALIBRATION_PENIBILITE.md`). À poids fixes,
> SC-04 reste SC-04. Les rouvrir demandera un arbitrage, pas une simple mise au contrat.

#### Données clés transmises

* **le planning existant complet de la période**, pour tout le monde ;
* `dataSet.indisponibilites` — déduites de la mesure : une absence n’est pas du temps disponible
  non travaillé ;
* `planningContext.equite.ecartTolerePourcent` — **sans elle, SC-04 n’a rien vers quoi optimiser**.
  La contrainte d’équité reste inerte tant qu’aucune tolérance n’est déclarée, et c’est le scénario
  où cette borne compte le plus.

> ⚠️ **L’« historique des compteurs » n’est pas une donnée d’entrée.** Elle a longtemps figuré ici,
> et c’était une erreur de cadrage : la profondeur est celle de la **période demandée**, et le
> moteur **recalcule** tout depuis les créneaux transmis. Rien n’est conservé entre deux appels.
> **Plus la fenêtre transmise est large, plus la mesure a de sens** — c’est le levier même de ce
> scénario, et il est entre les mains de l’appelant.

#### Restitution attendue

* le planning optimisé, dans `planning` ;
* le bloc **`optimisation`**, propre à SC-04 : ce qui a bougé, et la charge de chaque salarié
  concerné **avant et après**, semaine par semaine, mois par mois, puis sur la période ;
* les **régressions nommées** — `REGRESSION_INDIVIDUELLE` est levée dès qu’un salarié sort plus
  loin de son contrat qu’il n’y entrait.

---

### 🟣 SC-05 — Arbitrage de répartition entre deux salariés

> Inscrit au contrat le 2026-08-14, lot A4. Cadrage complet : `92_CADRAGE_SCENARIO_SC-05.md`.
> Arbitrages métier rendus le 2026-08-13, réalisation en cinq lots A0 à A4.
> Schémas normatifs : `Sc05ScenarioRequest` et `Arbitrage` dans
> `50_openapi_windev_moteur_v_1.yaml`, bloc de sortie détaillé dans
> `50_SCENARIO_RESPONSE_CONTRACT.md` §8.
>
> **Deux canaux, un seul comportement** : `POST /scenarios/sc05/solve` et le **FileAdapter**
> (`scenarioType: SC-05`). Ils entrent dans le même service au même point ; un test compare leurs
> deux réponses entières.

#### 🎯 Intention métier

Deux salariés se partagent un même périmètre de travail. **Comment le répartir équitablement, et
que coûte l'arbitrage à chacun ?**

Le moteur **ne crée aucun besoin** : il redistribue ce qui existe.

#### Principe structurant — réaffecter, pas classer

> SC-05 rend **une répartition**, pas un podium.

C'est l'inverse de SC-06, qui énumère et classe sans rien réaffecter. Ici le solveur **résout**, et
c'est donc **par le score** que l'équité l'atteint — exactement comme SC-02.

La parenté est d'ailleurs avec SC-02 : même mécanique — libérer, épingler le reste, laisser le
solveur décider — mais un déclencheur différent. SC-02 réagit à une absence, SC-05 à un déséquilibre.

#### Ce qui borne l'arbitrage

Une contrainte HARD **interdit qu'un créneau du périmètre revienne hors des ressources
autorisées**. C'est la seule forme sûre : réduire le dataset aux deux salariés se passerait de toute
contrainte, mais le moteur ne verrait plus les créneaux qui les bornent et déclarerait conforme une
répartition qui ne l'est pas.

👉 Cette contrainte porte sur un **ensemble** de ressources autorisées, pas sur un couple. À deux,
l'ensemble a deux éléments ; l'ouverture à N sera un élargissement du contrat d'entrée, pas une
réécriture du moteur.

#### Paramètres spécifiques

| Champ | Obligatoire | Rôle |
|---|:---:|---|
| `scenarioParameters.salarieAId` | ✅ | premier des deux salariés, **distinct** du second |
| `scenarioParameters.salarieBId` | ✅ | second des deux salariés |
| `scenarioParameters.creneauxArbitres[]` | ✅ | le périmètre — identifiants de créneaux du `dataSet` |
| `planningContext.equite.ecartTolerePourcent` | ○ | au-delà de quel écart il y a inéquité — **existait déjà** |
| `planningContext.coefficientsPenibilite` | ○ | l'échelle de pénibilité — **existait déjà** |

**Aucun champ nouveau hors `scenarioParameters`.** Le planning existant, les contrats, les
indisponibilités et le cadre réglementaire étaient déjà au contrat et déjà lus.

⚠️ **Deux champs annoncés de longue date n'existent pas**, et c'est délibéré :

* **`objectif`** — il se *déduit* de ce que l'appelant transmet : l'équité de charge par
  `ecartTolerePourcent`, la minimisation de surcharge par les seuils de SC-02. Un enum qui double
  des paramètres existants finit par les contredire. Il reviendra quand le moteur connaîtra les
  **préférences**, et avec la seule valeur qui ne se déduira pas d'un paramètre ;
* **`autoriserDesequilibre`** — remplacé par `ecartTolerePourcent`, qui dit *de combien* là où un
  booléen ne disait pas jusqu'où. « Équité stricte » se transmet comme une tolérance à `0`.

#### Données clés transmises

* le **planning complet de la période** pour les deux salariés — sans lui les bornes hebdomadaires
  sont invérifiables, exigence identique à celle de SC-06 ;
* `referentiels.activites`, pour que la charge se calcule.

⚠️ **L'historique de charge n'est pas reçu.** SC-05 arbitre sur la fenêtre transmise : un
déséquilibre installé depuis trois mois ne s'y voit pas. C'est le même manque que celui qui bloque
SC-04. **Plus la fenêtre transmise est large, plus l'arbitrage a de sens.**

#### Ce qui est décidé, et ce qui ne l'est pas

| Créneau | Sort |
|---|---|
| du périmètre, tenu par A ou B | **remis en jeu** — c'est l'objet de l'arbitrage |
| du périmètre, tenu par un **tiers** | **épinglé et signalé** — le tiers n'a rien demandé, on ne lui retire pas son travail pour équilibrer deux autres personnes |
| du périmètre, sur un **poste virtuel** | **remis en jeu** — un poste virtuel n'est le travail de personne |
| **hors** du périmètre | **épinglé**, y compris les autres créneaux de A et de B |

#### Restitution attendue

* `planning` — la répartition proposée, créneau par créneau ;
* `workMetrics.byRessource` — les indicateurs comparatifs A / B ;
* `solverResult.scoreBreakdown` — la justification, ligne par ligne ;
* **`arbitrage`** — ce qui a changé pour chacun : créneaux repris et cédés, écart au contrat
  **avant / après**, et le sort de chaque créneau du périmètre ;
* `diagnostics.alerts` — dont `INEQUITE_RESIDUELLE` quand la tolérance reste dépassée.

📌 **Il n'y a jamais d'erreur.** Sans répartition acceptable, la **moins mauvaise** est rendue,
accompagnée des motifs qui la disqualifient (`arbitrage.acceptable`, `arbitrage.motifs`). Même
traitement qu'en SC-06 : l'appelant voit l'impasse au lieu de la deviner.

⚠️ **`acceptable: true` veut dire « au regard de ce que le moteur sait ».** Il ne connaît pas les
**préférences ni les contraintes personnelles** des deux intéressés — indisponibilité récurrente,
incompatibilité entre personnes, activités réellement pratiquées. Deux salariés désignés est
pourtant le cas où elles comptent le plus. L'absence de motif ne vaut donc **pas** accord des deux
salariés.

📌 **Aucune nouvelle variable de décision** : SC-05 exploite les mêmes affectations que les autres
scénarios, avec une lecture comparative ciblée.

---

### 🟠 SC-06 — Désignation de la ressource la plus à même de couvrir un besoin

> Inscrit au contrat le 2026-08-10. Cadrage complet : `92_CADRAGE_SCENARIO_SC-06.md`.

#### 🎯 Intention métier

Un besoin apparaît sur une journée. Parmi les personnes dont le planning de la semaine est connu,
**qui est la plus à même de le prendre en charge, et à quel prix pour elle ?**

Le moteur ne réorganise rien. Il **insère** dans des plannings qu'il traite comme des faits
acquis, et il **classe** les manières de couvrir le besoin.

#### Ce qui distingue SC-06

| | SC-06 |
|---|---|
| Ce qui est décidé | l'affectation des seuls créneaux du besoin |
| Ce qui est figé | l'intégralité du planning transmis |
| Ce qui est restitué | un **classement** de solutions, pas un planning optimisé |

Par rapport aux scénarios voisins : **SC-02** part d'une absence identifiée et **réaffecte
réellement** ce qu'elle libère, quand SC-06 part d'un besoin nu et se contente de classer ;
**SC-03** réaffecte un sous-ensemble de créneaux, quand SC-06 n'en réaffecte aucun ; **SC-05**
compare deux salariés désignés, quand SC-06 les découvre.

#### Principe structurant — énumération, pas optimisation

> Le besoin n'est pas d'optimiser un planning, mais de **classer des possibilités**.

SC-06 **n'appelle pas le solveur**. Il énumère les candidats éligibles et évalue chacun sans
lancer de recherche. Trois propriétés en découlent, qu'aucune résolution heuristique n'offre :
**déterminisme** (même entrée, même podium), **exhaustivité** (aucun candidat éligible oublié),
**explicabilité** (un motif attaché à chaque rang).

#### Paramètres spécifiques

* `besoin.date` — jour du besoin, commun à tous ses créneaux ;
* `besoin.creneaux[]` — de 1 à n créneaux : `id`, `heureDebut`, `heureFin`, `codeActiviteId`,
  `lieu`, `posteComptable`.

Le nombre de solutions restituées **n'est pas paramétrable** : trois.

#### Données clés transmises

* le **planning existant de la semaine**, chaque créneau portant son `ressourceAffecteeId` —
  intégralement figé ;
* les salariés avec leur bloc `contrat` et leurs `contraintesReglementaires` ;
* le référentiel d'activités et les indisponibilités.

> **`dataSet.creneaux` = le passé, figé. `scenarioParameters.besoin` = la question posée, seule
> variable de décision.** L'alternative — un créneau sans ressource affectée vaut besoin — rendrait
> la question implicite : un `ressourceAffecteeId` omis par erreur deviendrait silencieusement un
> besoin à couvrir.

#### Trois exigences, sans lesquelles la demande est refusée

1. **Semaine pleine.** `planningContext.horizon` couvre exactement la semaine calendaire
   lundi → dimanche du besoin, pour **toutes** les ressources candidates. Une semaine tronquée
   sous-évalue le total hebdomadaire et déclarerait conformes des candidats qui ne le sont pas.
2. **Affectations complètes.** Tout créneau du `dataSet` porte son `ressourceAffecteeId`.
3. **Activité connue.** L'activité du besoin figure au référentiel transmis.

> **Convention sur les limites** : pour désactiver une contrainte individuelle, **omettre le
> champ** — ne jamais envoyer `0`, qui l'activerait avec un seuil nul. Le moteur signale la
> valeur `0` dans ses journaux mais ne la corrige pas : corriger reviendrait à deviner l'intention.

#### Restitution attendue

Un bloc `candidats[]` — voir `50_SCENARIO_RESPONSE_CONTRACT.md` §6 — portant au plus trois
solutions classées, chacune avec :

* **qui** : `affectations[]` — identité, activité, lieu ;
* **à quel prix** : `impacts[]` — amplitude journalière, heures du jour, heures de la semaine,
  en avant / après / delta ;
* **pourquoi ce rang** : `motifs[]`.

Une solution qui viole une règle éliminatoire est **restituée, marquée `conforme: false` et
classée en dernier** — jamais masquée. Écarter ces solutions rendrait inexplicable la disparition
d'une personne, et renverrait une liste vide sans raison le jour où aucune solution conforme
n'existe.

---

## 5. Invariants du contrat

* Un scénario = une intention métier claire ;
* aucun scénario ne modifie le modèle conceptuel ;
* aucun paramètre libre n’est transmis au solveur ;
* toute décision est explicable via indicateurs ;
* toute donnée d’identification reçue est restituée à l’identique ;
* le moteur ne refuse pas : il **rend visible l’impossible**.

---

## 6. Évolutivité

* ajout de scénario → versionnement explicite ;
* aucun scénario ne doit introduire :

  * de nouvelles décisions ;
  * des règles implicites ;
* les scénarios pilotent :

  * le contexte ;
  * la pondération ;
  * la restitution.

---

## 7. Rôle respectif des couches

### WebDev

* interprète la demande utilisateur ;
* choisit le scénario ;
* construit les paramètres ;
* interprète les résultats.

### API Spring Boot

* valide le contrat ;
* protège le moteur ;
* adapte les données.

### Moteur de planification

* arbitre ;
* score ;
* explique ;
* n’interprète jamais l’intention.

---

## 8. Statut du document

* Document structurant ;
* référence contractuelle ;
* toute modification est une **décision d’architecture**.
