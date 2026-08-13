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
affectation. SC-06 fige tout son `dataSet` ; SC-01 et SC-03 ignorent le champ et conservent leur
liberté de décision entière.

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

#### 🎯 Intention métier

Assurer la continuité de service **en perturbant le moins possible l’existant**.

#### Paramètres spécifiques

* salarié absent ;
* période d’absence ;
* liste de remplaçants autorisés ;
* seuil de surcharge acceptable ;
* autorisation de poste virtuel (oui / non).

#### Données clés transmises

* créneaux imposés existants ;
* planning initial partiellement figé ;
* ressources disponibles.

#### Restitution attendue

* planning ajusté ;
* différences avant / après ;
* niveaux de surcharge par salarié ;
* volume de besoin résiduel (poste virtuel).

#### État d'implémentation — lots S1 et S2 livrés le 2026-08-11

`POST /scenarios/sc02/solve` est exposé. ⚠️ **Le contrat ci-dessous n'est pas complet** : il
grandira aux lots S2 (découpage) et S3 (seuils de surcharge), et son inscription définitive à
l'OpenAPI et aux schémas JSON est le lot S4. Voir `92_CADRAGE_SCENARIO_SC-02.md` §8.

**Ce que le lot S1 accepte** — et rien d'autre, délibérément : un champ que personne ne lit est
pire qu'un champ absent.

| Champ | Obligatoire | Rôle |
|---|:---:|---|
| `scenarioParameters.salarieAbsentId` | ✅ | Désigne le salarié dont l'absence motive le scénario. Il ne sert **pas** à la contrainte : l'absence elle-même est portée par `dataSet.indisponibilites`, déjà tenu par une contrainte HARD. Il sert à savoir de quelle absence tirer les conséquences |
| `scenarioParameters.posteVirtuelAutorise` | ○ | Défaut `false`. Le poste virtuel ne s'invite jamais de lui-même |

Le bloc `scenarioParameters` est **strict** : un paramètre d'un lot à venir produit une erreur
explicite, jamais un silence.

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

**Ce que le moteur restitue** : un bloc `remplacement`, propre à SC-02 et absent des autres
scénarios — `salarieAbsentId`, `creneauxLiberes` (créneaux d'origine, que le découpage ne gonfle
pas), `creneauxRepris` (repris **en entier**), `heuresAPourvoir`, et le `details[]` du sort de
chaque morceau restitué, y compris ceux que personne n'a repris.

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

### 🔴 SC-04 — Optimisation globale d’un planning existant

#### 🎯 Intention métier

Améliorer un planning réel **sans le reconstruire entièrement**.

#### Paramètres spécifiques

* degré de liberté (créneaux figés / ajustables) ;
* priorités d’optimisation ;
* pondération des règles.

#### Données clés transmises

* planning existant complet ;
* historique des compteurs.

#### Restitution attendue

* planning optimisé ;
* gains / régressions explicitées ;
* indicateurs comparatifs.

---

### 🟣 SC-05 — Arbitrage de répartition horaire / lieu entre deux salariés

#### 🎯 Intention métier

Arbitrer une **répartition équitable ou optimale** entre deux salariés concurrents
pour un même périmètre de travail.

👉 Ce scénario **ne crée pas de nouveaux besoins** :
il arbitre **l’affectation relative**.

#### Cas d’usage typiques

* deux salariés sur un même site ;
* rééquilibrage de charge ;
* conflit de préférences ;
* arbitrage équité vs compétence.

#### Paramètres spécifiques

* salarié A ;
* salarié B ;
* période concernée ;
* lieux et activités communs ;
* objectif principal :

  * équité de charge ;
  * minimisation de surcharge ;
  * respect de préférences ;
* autorisation de déséquilibre contrôlé.

#### Données clés transmises

* créneaux communs ou concurrents ;
* historique de charge des deux salariés ;
* seuils comparatifs.

#### Restitution attendue

* répartition proposée ;
* indicateurs comparatifs A / B ;
* justification des arbitrages ;
* alertes d’inéquité résiduelle.

📌 **Point important**
Ce scénario **ne nécessite aucune nouvelle variable de décision** :
il exploite les mêmes affectations que les autres scénarios,
mais avec une **lecture comparative ciblée**.

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

Par rapport aux scénarios voisins : **SC-02** part d'une absence identifiée et d'une liste de
remplaçants imposée, quand SC-06 part d'un besoin nu et évalue tout le monde ; **SC-03** réaffecte
un sous-ensemble de créneaux, quand SC-06 n'en réaffecte aucun ; **SC-05** compare deux salariés
désignés, quand SC-06 les découvre.

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
