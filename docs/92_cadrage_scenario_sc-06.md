# 92 — Cadrage : SC-06, désignation de la ressource la plus à même de couvrir un besoin

> **Statut** : cadrage d'analyse, 2026-08-10. Les arbitrages de la §4 sont **tranchés**.
> Le découpage de la §9 reste à valider. Ce document sert à découper le chantier, pas à le réaliser.
>
> Ce document ne modifie ni `50_ScenarioContract.md`, ni le code. L'inscription de SC-06 au
> contrat fonctionnel est une décision d'architecture, portée par le lot **S4**.

---

## 1. Intention métier

> Un besoin apparaît sur une journée. Parmi les personnes dont le planning de la semaine est
> connu, **qui est la plus à même de le prendre en charge, et à quel prix pour elle ?**

Le moteur ne réorganise rien. Il **insère** dans des plannings existants qu'il traite comme
des faits acquis, et il **classe** les manières de couvrir le besoin.

Trois éléments distinguent SC-06 des scénarios déjà définis :

| | SC-06 |
|---|---|
| Ce qui est décidé | l'affectation des seuls créneaux du besoin |
| Ce qui est figé | l'intégralité du planning transmis |
| Ce qui est restitué | un **classement** de solutions, pas un planning optimisé |

---

## 2. Positionnement par rapport aux scénarios existants

| Scénario | Différence essentielle |
|---|---|
| **SC-02** — remplacement d'un absent | part d'une absence identifiée et d'une liste de remplaçants imposée ; SC-06 part d'un besoin nu et évalue tout le monde |
| **SC-03** — ajustement ponctuel | **réaffecte** un sous-ensemble de créneaux ; SC-06 n'en réaffecte aucun |
| **SC-05** — arbitrage entre deux salariés | compare deux personnes désignées ; SC-06 les découvre |

**SC-03 n'est pas modifié par ce chantier.** Les évolutions du contrat d'entrée décrites ici
sont toutes additives et sans effet sur son comportement (§8).

---

## 3. Principe structurant — énumération, pas optimisation

C'est la décision qui commande tout le reste.

> Le besoin n'est pas d'optimiser un planning, mais de **classer des possibilités**.
> Une recherche heuristique rend *une* solution, sans garantie de reproductibilité ni de
> couverture. SC-06 procède par **énumération exhaustive des candidats éligibles**, chacun
> évalué par `SolutionManager.explain()` sans lancer de recherche.

Trois propriétés en découlent, qu'aucun `solve()` ne peut offrir :

| Propriété | Conséquence |
|---|---|
| **Déterminisme** | même entrée ⇒ même podium, à chaque exécution |
| **Exhaustivité** | aucun candidat éligible n'échappe à l'évaluation |
| **Explicabilité** | le `scoreBreakdown` de chaque candidat fournit le motif de son rang |

`SolutionManager` est déjà injecté dans `PlanningService` — l'outil existe, il n'est pas
encore employé de cette façon.

### 3.1 Algorithme retenu

**Filtre d'éligibilité** (aucun coût solveur) — écarte d'emblée les ressources dont l'activité
n'est pas compatible, la journée indisponible, ou dont un créneau déjà affecté chevauche le besoin.

> **Le lieu n'entre pas dans ce filtre.** `sitesAutorises` est transporté et mappé jusqu'au
> domaine, mais **aucune contrainte ni aucun filtre ne le lit** — vérifié : ses seuls lecteurs
> sont `ScenarioResourceMapper` et `ScenarioDatasetBuilderSc01`. C'est la posture actuelle du
> moteur, assumée (§5.4 du cadrage général : il n'y a pas d'arbitrage entre lieux à ce jour).
> SC-06 ne la modifie pas. Voir §10.3.

**Passe 1 — solutions mono-ressource.** Chaque candidat éligible reçoit *tous* les créneaux du
besoin ; on évalue. `N` évaluations.

**Passe 2 — solutions composées.** Déclenchée seulement si la passe 1 ne produit pas trois
solutions conformes. Chaque créneau du besoin est évalué candidat par candidat (`N × k`), les
meilleures combinaisons sont composées, puis **réévaluées entières** — les créneaux d'une même
journée interagissent (amplitude, chevauchement, cumul), une somme de scores partiels serait fausse.

**Passe 3 — repli.** Si le besoin reste non couvert, la solution « ressource à pourvoir »
(poste virtuel, à défaut `A_AFFECTER`) complète le podium.

Ordre de grandeur : 30 salariés, 3 créneaux ⇒ environ 120 évaluations. Sans commune mesure
avec les temps de résolution actuels.

---

## 4. Arbitrages

### 4.1 Tranché — périmètre du besoin

De 1 à n créneaux, **tous sur une même journée**. La date est portée une seule fois, au niveau
du besoin : l'invariant est structurel, pas déclaratif.

### 4.2 Tranché — le besoin est un paramètre, pas une donnée

> **`dataSet.creneaux` = le passé, intégralement figé.
> `scenarioParameters.besoin` = la question posée, seule variable de décision.**

Motif : l'alternative — un créneau du `dataSet` sans ressource affectée vaut besoin — rend la
question implicite. Un `ressourceAffecteeId` omis par erreur sur une ligne de planning
deviendrait silencieusement un besoin à couvrir. La séparation explicite supprime cette classe
d'anomalie et permet au moteur de garantir que **tout créneau du `dataSet` est figé, sans exception**.

### 4.3 Tranché — podium de trois

La meilleure solution et deux alternatives, classées. Nombre fixe, non paramétrable.

### 4.4 Tranché — classement par paliers lexicographiques

Dans l'ordre, chaque palier départageant à égalité du précédent :

| Rang | Palier | Justification |
|---|---|---|
| 1 | **Conformité** — aucune règle éliminatoire violée | ne jamais recommander l'illégal en tête |
| 2 | **Couverture complète** avant couverture partielle | le besoin prime |
| 3 | **Une seule personne** avant plusieurs ; **salarié réel** avant poste virtuel | réponse 2 du cadrage |
| 4 | **Personne déjà en poste ce jour-là** avant personne rappelée | éviter de casser un repos |
| 5 | **Score SOFT du moteur** | amplitude, nuit, jours consécutifs, pénibilités |
| 6 | **Écart aux heures hebdomadaires habituelles** | départage : le plus loin de son volume habituel |

Motif du choix lexicographique plutôt qu'un score unique : les `ScoreWeights` ont été calibrés
pour optimiser un planning, pas pour choisir une personne. Les réemployer tels quels pour arbitrer
« déjà en poste » contre « amplitude » produirait un classement juste mais injustifiable devant
l'utilisateur. Les paliers rendent chaque rang lisible ligne à ligne — et le score SOFT garde son
rôle, au palier 5, là où il est pertinent.

Le palier 6 suppose le bloc `contrat` (lot **S2**).

### 4.5 Tranché — les solutions non conformes sont restituées, jamais masquées

Une solution violant le repos quotidien ou le maximum hebdomadaire est **restituée, marquée
`conforme: false`, classée en dernier**, avec le motif précis.

Motif : l'invariant du contrat — *« le moteur ne refuse pas : il rend visible l'impossible »*
(`50_ScenarioContract.md` §5). Écarter ces solutions rendrait inexplicable la disparition d'une
personne, et renverrait une liste vide sans raison le jour où aucune solution conforme n'existe.
Les restituer en queue de classement satisfait l'invariant sans jamais recommander l'illégal.

### 4.6 Tranché — bloc `contrat` salarié, sans les dates

✅ **Livré au lot S2, 2026-08-10.** Quatre champs, tous facultatifs :

| Champ | Rôle | Exploitation |
|---|---|---|
| `heuresMoyennesParJour` | volume journalier habituel | palier 6 |
| `heuresHebdomadairesHabituelles` | volume hebdomadaire habituel | palier 6, impacts |
| `joursTravaillesParSemaine` | défaut **5** si non renseigné | impacts |
| `estAnnualise` | booléen | **transporté, non exploité** — §10.1 |

**Valeur absente et valeur transmise restent distinguables.** `ContratSalarie` conserve les
valeurs brutes telles que reçues, `null` compris, et expose en regard des méthodes
`…Effectif()` portant la valeur réellement appliquée. Le défaut de 5 jours n'écrase donc
jamais l'information « WinDev n'a rien transmis » — il l'accompagne. Même motif pour
`estAnnualise()`, qui lit `null` comme « non annualisé » : *un vide ne suppose jamais que la
chose est possible*.

**Le bloc est strict** : `ContratSalarieDTO` ne porte pas de `@JsonIgnoreProperties`, alors que
`SalarieInputDTO` qui le contient est tolérant. L'annotation étant portée par classe, la
tolérance du parent ne se propage pas — vérifié par test. Un `dateDebutContrat` envoyé par
erreur est donc rejeté au lieu d'être silencieusement absorbé, ce qui rend visible l'arbitrage
« les dates relèvent de WinDev » plutôt que de le laisser deviner.

**Les dates de début et de fin de contrat sont écartées.** Le filtrage des salariés hors contrat
à la date visée relève de WinDev, en amont. Le moteur n'a pas à connaître la vie administrative
du contrat.

**`travailDeNuit` et `travailleJourFerie` restent à la racine du salarié.** Ils y sont déjà
exploités par deux contraintes ; les déplacer serait une rupture de contrat pour SC-03 sans
contrepartie. La cible §6.3 de `92_cadrage_donnees_amont_scenarios.md` reste valide comme
trajectoire, elle n'est pas ouverte par SC-06.

### 4.7 Tranché — sémantique de l'absence de limite

**Règle SC-03 appliquée à l'identique : champ absent ou `null` ⇒ contrainte inactive.**

Conséquence directe pour WinDev : **pour désactiver une limite, omettre le champ — ne jamais
envoyer `0`.** Une valeur `0` activerait la contrainte avec un seuil de zéro, et tout créneau
deviendrait une violation.

Le moteur émet un **WARN** à la réception d'un `0` sur l'une des huit contraintes, afin que
l'écart soit visible en intégration plutôt que silencieux. Il ne le corrige pas : corriger
reviendrait à deviner l'intention.

✅ **Implémenté au lot S2, 2026-08-10** — `ScenarioResourceMapper.signalerZeroInterdit()`, appelé
sur les huit champs, avec l'identifiant du salarié et le nom du champ dans le message. Ce geste
n'appartenait explicitement ni à S2 ni à S3 ; il a été rattaché à S2 parce qu'il vit dans le
mapper que ce lot modifiait déjà, et parce qu'un arbitrage tranché sans porteur finit par se
perdre.

### 4.8 Tranché — semaine et horizon

**Semaine = lundi → dimanche calendaire.** WinDev transmet toujours une **semaine pleine**, pour
**toutes** les ressources candidates.

Sans cette garantie, l'impact hebdomadaire est incalculable et le maximum hebdomadaire
invérifiable — le moteur ne verrait qu'une fraction de la charge et déclarerait conforme une
solution qui ne l'est pas. Le moteur **vérifie** que l'horizon couvre une semaine ISO complète
contenant la date du besoin, et refuse la demande sinon.

### 4.9 Tranché — indisponibilités à la journée

Granularité journée, comme aujourd'hui. La demi-journée et la plage horaire relèvent d'une
évolution ultérieure (§10.2).

### 4.10 Tranché — canaux d'accès

`POST /scenarios/sc06/solve` **et** FileAdapter. Le FileAdapter est le canal de test initial
retenu côté WinDev — il doit donc être livré, pas différé.

---

## 5. Contrat d'entrée

### 5.1 Structure

```json
{
  "scenarioType": "SC-06",
  "planningContext": {
    "horizon": { "dateDebut": "2026-05-11", "dateFin": "2026-05-17" },
    "strategieScoring": "ANALYSE_RH"
  },
  "scenarioParameters": {
    "besoin": {
      "date": "2026-05-14",
      "creneaux": [
        {
          "id": "BES-001",
          "heureDebut": "14:00",
          "heureFin": "22:00",
          "codeActiviteId": "ACT-SOIN",
          "lieu": "HOPITAL-NORD",
          "posteComptable": "PC-SOINS"
        }
      ]
    }
  },
  "dataSet": {
    "referentiels": { "activites": [ "… identique SC-03 …" ] },
    "ressources": {
      "salaries": [
        {
          "id": "SAL-2002",
          "statut": "CDI",
          "sitesAutorises": ["HOPITAL-NORD"],
          "activitesCompatibles": ["ACT-SOIN"],
          "contrat": {
            "heuresMoyennesParJour": 7.0,
            "heuresHebdomadairesHabituelles": 35.0,
            "joursTravaillesParSemaine": 5,
            "estAnnualise": false
          },
          "contraintesReglementaires": {
            "heuresMaximumParJour": 10.0,
            "amplitudeJournaliereMaximum": 13.0,
            "reposQuotidienMinimum": 11.0,
            "heuresMaximumParSemaine": 44.0,
            "joursConsecutifsMaximum": 6
          },
          "travailDeNuit": "occasionnel",
          "travailleJourFerie": true
        }
      ],
      "postesVirtuels": [ "… identique SC-03 …" ]
    },
    "creneaux": [
      {
        "id": "1041-20260511-01",
        "date": "2026-05-11",
        "heureDebut": "07:00",
        "heureFin": "15:00",
        "codeActiviteId": "ACT-SOIN",
        "lieu": "HOPITAL-NORD",
        "ressourceAffecteeId": "SAL-2002"
      }
    ],
    "indisponibilites": { "items": [ "… identique SC-03 …" ] }
  }
}
```

Dans cet exemple, les contraintes non applicables (`heuresMinimumParJour`,
`heuresMinimumParSemaine`, `nuitsMaximumParSemaine`) sont **omises**, conformément à la §4.7.

### 5.2 Ce qui change par rapport au contrat actuel

| Élément | Nature | Portée |
|---|---|---|
| `creneaux[].ressourceAffecteeId` | **ajout** | tous scénarios, facultatif — sans effet sur SC-01/SC-03 qui ne le renseignent pas |
| `salaries[].contrat` | **ajout** | bloc entièrement nouveau |
| `scenarioParameters.besoin` | **ajout** | propre à SC-06 |

Aucune suppression, aucun renommage, aucun changement de sémantique sur l'existant.

### 5.3 Exigences à porter au dossier de livraison WinDev

1. **Semaine pleine lundi → dimanche**, pour toutes les ressources candidates.
2. **Tout créneau du `dataSet` porte son `ressourceAffecteeId`** — un créneau du planning sans
   affectation n'a pas de sens en SC-06.
3. **Pour désactiver une limite : omettre le champ, jamais `0`.**
4. **Valeurs par défaut toujours transmises** : `heuresMoyennesParJour`,
   `heuresHebdomadairesHabituelles`, `joursTravaillesParSemaine` (5 à défaut).
5. **Identifiants du besoin sous préfixe dédié** (`BES-00X`), uniques sur l'ensemble du scénario —
   convention déjà arbitrée (`92_cadrage_donnees_amont_scenarios.md` §6.7).
6. **Filtrage amont** des salariés hors contrat à la date visée.

---

## 6. Contrat de sortie — bloc `candidats[]`

La structure de la série 50 est conservée. Un sixième bloc s'ajoute à la racine.

```json
"candidats": [
  {
    "rang": 1,
    "conforme": true,
    "couvertureComplete": true,
    "nature": "MONO_RESSOURCE",
    "affectations": [
      {
        "creneauId": "BES-001",
        "ressourceId": "SAL-2002",
        "activite": "ACT-SOIN",
        "lieu": "HOPITAL-NORD",
        "heureDebut": "14:00",
        "heureFin": "22:00"
      }
    ],
    "impacts": [
      {
        "ressourceId": "SAL-2002",
        "amplitudeJournaliere": { "avant": 0.0, "apres": 8.0,  "delta": 8.0, "plafond": 13.0, "depassement": false },
        "heuresJour":           { "avant": 0.0, "apres": 8.0,  "delta": 8.0, "plafond": 10.0, "depassement": false },
        "heuresSemaine":        { "avant": 30.0, "apres": 38.0, "delta": 8.0, "plafond": 44.0, "depassement": false },
        "heuresHabituellesSemaine": 35.0
      }
    ],
    "motifs": [
      { "code": "RAPPEL_SUR_REPOS", "severite": "INFO", "message": "Le salarié ne travaillait pas ce jour" }
    ]
  }
]
```

### 6.1 Champs

| Champ | Type | Description |
|---|---|---|
| `rang` | integer | 1 = le plus favorable |
| `conforme` | boolean | `false` si une règle éliminatoire est violée (§4.5) |
| `couvertureComplete` | boolean | `false` si une partie du besoin reste non couverte |
| `nature` | string | `MONO_RESSOURCE` \| `COMPOSEE` \| `RESSOURCE_A_POURVOIR` |
| `affectations[]` | tableau | une entrée par créneau du besoin — **c'est la réponse attendue** : identité, activité, lieu |
| `impacts[]` | tableau | une entrée par ressource mobilisée par cette solution |
| `motifs[]` | tableau | même forme que `diagnostics.alerts[]` : `code`, `severite`, `message` |

Les impacts sont exprimés en **heures décimales**, comme `workMetrics.byRessource` (§3.2 du
contrat de sortie). `plafond` reprend la valeur individuelle du salarié ; `null` si la contrainte
est inactive pour lui.

### 6.2 Codes de motif prévus

| Code | Sévérité | Éliminatoire |
|---|---|:---:|
| `REPOS_QUOTIDIEN_INSUFFISANT` | ERROR | ✅ |
| `HEURES_HEBDO_DEPASSEES` | ERROR | ✅ |
| `JOUR_FERIE_NON_AUTORISE` | ERROR | ✅ (HARD existant) |
| `INDISPONIBILITE` | ERROR | ✅ (HARD existant) |
| `AMPLITUDE_DEPASSEE` | WARNING | — |
| `JOURS_CONSECUTIFS_DEPASSES` | WARNING | — |
| `NUIT_SALARIE_NON_NUIT` | WARNING | — |
| `RAPPEL_SUR_REPOS` | INFO | — |
| `BESOIN_PARTIELLEMENT_COUVERT` | WARNING | — |
| `AUCUNE_RESSOURCE_ELIGIBLE` | ERROR | — |

Une sévérité `INFO` n'est pas une anomalie — règle déjà posée au §4.2 du contrat de sortie.

---

## 7. Ce que SC-06 exige des règles du moteur

Réponse 2 du cadrage : *« si on fait revenir quelqu'un, il faut que ça respecte le nombre de
jours max travaillés, le repos de 11 h entre deux journées, le nombre d'heures max hebdo »*.

| Règle exigée | État vérifié dans le code | Conséquence |
|---|---|---|
| Jours travaillés consécutifs max | **EXPLOITÉ** — `JoursConsecutifsMax`, SOFT | rien à faire |
| Repos quotidien minimum (11 h) | ~~AUCUNE CONTRAINTE~~ → **EXPLOITÉ** — `ReposQuotidienMinimum`, SOFT | ✅ **livré S3, 2026-08-10** |
| Heures maximum hebdomadaires | ~~AUCUNE CONTRAINTE~~ → **EXPLOITÉ** — `HeuresMaximumParSemaine`, SOFT | ✅ **livré S3, 2026-08-10** |

### 7.1 SOFT et non HARD — décision de S3

Les deux règles sont **SOFT**, alignées sur les trois contraintes individuelles déjà en service
(`AmplitudeJournaliere`, `JoursConsecutifsMax`, `HeuresMinimumParJour`). Trois motifs :

1. **L'invariant du contrat** — *« le moteur ne refuse pas : il rend visible l'impossible »*. Une
   contrainte HARD ferait activement fuir le solveur, jusqu'à laisser des créneaux non couverts
   plutôt que d'accepter un dépassement — `CreneauNonAffecte` étant lui-même SOFT.
2. **SC-06 ne peut pas s'appuyer sur le score HARD** — le planning existant y est figé. Une
   violation déjà présente dans le figé polluerait identiquement le score HARD de *tous* les
   candidats, rendant l'indicateur `conforme` inutilisable.
3. **La conformité se mesure en delta**, pas en absolu : SC-06 comparera la contribution de
   chaque contrainte avec et sans le besoin affecté. Ce mécanisme fonctionne indifféremment sur
   du SOFT, et il est robuste à l'état antérieur du planning.

Le caractère « éliminatoire » du §4.5 est donc une **notion propre au classement SC-06**, portée
par le palier 1, et non une contrainte HARD du solveur. La distinction est volontaire.

### 7.2 Pondération

`PENALITE_REPOS_QUOTIDIEN = 100` et `PENALITE_HEURES_MAX_PAR_SEMAINE = 100` par minute, soit le
double de `penaliteAmplitude` (50) : dépassement d'une borne légale contre une borne de confort.

Constantes locales aux deux classes, suivant le précédent de `HeuresMinimumParJour`. Elles ne
sont pas externalisées vers `Penalites` : ces poids n'interviennent qu'au palier 5 du classement,
là où — par construction — les solutions encore en lice ne violent aucune des deux règles et où
leur contribution est donc nulle. Externaliser avant de savoir si un réglage indépendant est utile
serait prématuré ; le lot S7 tranchera.

Ces deux règles quittent donc le lot d'activation progressive pour rejoindre le **chemin critique**.
Sans elles, SC-06 recommanderait des solutions que vous avez explicitement déclarées inacceptables.

Leur création n'est pas sans effet sur les scénarios existants : le fournisseur de contraintes
est partagé. Voir **§9.2**.

---

## 8. État des lieux vérifié

Statuts établis par recherche des **appels réels** aux accesseurs dans `src/main/java`, en
écartant les *getters*, les mappers et les tests — même méthode qu'en annexe de
`92_cadrage_donnees_amont_scenarios.md`.

### 8.1 Les huit contraintes générales

Les huit champs demandés existent **déjà** au contrat, au nom près, dans
`ContraintesReglementairesDTO` et `ContraintesReglementairesSalarie`. Aucun ajout n'est nécessaire.

| Contrainte | Champ | État | Lot |
|---|---|---|---|
| Amplitude journalière max | `amplitudeJournaliereMaximum` | **EXPLOITÉ** (SOFT) | — |
| Jours consécutifs max | `joursConsecutifsMaximum` | **EXPLOITÉ** (SOFT) | — |
| Heures minimum / jour | `heuresMinimumParJour` | **EXPLOITÉ** (SOFT) | — |
| Repos quotidien minimum | `reposQuotidienMinimum` | **EXPLOITÉ** (SOFT) — `ReposQuotidienMinimum` | ✅ S3 |
| Heures maximum / semaine | `heuresMaximumParSemaine` | **EXPLOITÉ** (SOFT) — `HeuresMaximumParSemaine` | ✅ S3 |
| Heures maximum / jour | `heuresMaximumParJour` | **EN ATTENTE** — la contrainte existante compare à une constante `DUREE_MAX_LEGALE = 780` écrite en dur | S7 |
| Heures minimum / semaine | `heuresMinimumParSemaine` | **EN ATTENTE** — aucune contrainte | S7 |
| Nuits maximum / semaine | `nuitsMaximumParSemaine` | **EN ATTENTE** — aucune contrainte | S7 |

Cette correspondance exacte **clôt l'arbitrage §6.5 resté ouvert** du cadrage général : les quatre
champs qui n'y figuraient pas dans la cible sont bien attendus par le métier. Ils sont conservés.

### 8.2 Les briques absentes

| Élément | État | Lot |
|---|---|---|
| `ressourceAffecteeId` sur un créneau d'entrée | **ABSENT** de `CreneauInputDTO` | **S1** |
| Figement d'un créneau | **ABSENT** — `Creneau` n'a pas de `@PlanningPin` | **S1** |
| Bloc `contrat` salarié | ~~ABSENT~~ → **TRANSPORTÉ ET MAPPÉ** — `ContratSalarieDTO` → `ContratSalarie` | ✅ S2 |
| Énumération et classement de candidats | **ABSENT** — aucune notion de candidat dans le code | **S4** |
| Bloc `candidats[]` et impacts avant/après | **ABSENT** | **S5** |
| Endpoint SC-06 et FileAdapter SC-06 | **ABSENT** | S4 / S6 |

**S1 correspond au lot L8** du cadrage général (« Planning existant + créneaux figés »), jamais
réalisé, et **S2 au lot L4** (« Bloc `contrat` salarié »). L'investissement ne sert donc pas que
SC-06 : L8 est le prérequis reconnu de SC-02 et SC-04.

### 8.3 Briques réutilisables telles quelles

`SolutionManager.explain()` déjà injecté · `AmplitudeJournaliere.calculerAmplitudeMinutes()`
(statique) · `WorkMetricsCalculator` · le référentiel d'activités · les indisponibilités ·
le pipeline SC-03 comme patron de service · la forme `alerts[]` réemployée pour `motifs[]`.

---

## 9. Découpage

Ordonné par dépendance. Numérotation **S**, distincte des lots **L** du cadrage général pour
éviter toute confusion.

| Lot | Objet | Taille | Dépend de |
|---|---|---|---|
| **S1** | Planning existant affecté et figé : `ressourceAffecteeId`, `@PlanningPin`, mapping, non-régression SC-01/SC-03 | **M** — 2 à 3 j | — |
| **S2** | Bloc `contrat` salarié : 4 champs, transport + domaine, sans exploitation de `estAnnualise` | **S** — 1 à 2 j | — · ✅ **livré 2026-08-10** |
| **S3** | Les deux règles exigées : repos quotidien minimum, heures maximum hebdomadaires | **M** — 2 à 3 j | — · ✅ **livré 2026-08-10** |
| **S4** | Le scénario : endpoint, DTO de requête, filtre d'éligibilité, énumération, classement | **L** — 4 à 6 j | S1 |
| **S5** | Impacts avant/après et restitution `candidats[]` | **M** — 2 à 3 j | S2, S3, S4 |
| **S6** | FileAdapter SC-06, schémas JSON, OpenAPI, jeux d'essai, documentation séries 50 et 90 | **M** — 2 à 3 j | S4, S5 |
| **S7** | Activation progressive des 3 contraintes restantes + correction de la constante 780 | 0,5 à 1 j par règle | S3 |

**Chemin critique : 13 à 19 jours.** Estimations indicatives, à recalibrer.

### 9.1 Ordre d'exécution retenu

**S3 → S2 → S1 → S4 → S5 → S6**, S7 au fil de l'eau.

Cet ordre respecte toutes les dépendances du tableau. Il place en tête les deux lots
indépendants de l'énumération (S3, S2), ce qui permet de valider le comportement des nouvelles
règles sur des jeux d'essai SC-03 existants **avant** d'introduire le figement des créneaux (S1),
qui est la modification la plus structurante du domaine.

### 9.2 ⚠️ S3 n'est pas neutre pour SC-03

`ConstraintProviderImpl` est **unique et partagé par tous les scénarios**. Les deux contraintes
créées en S3 s'appliqueront donc aussi à SC-01 et SC-03, sans que leur code soit touché.

Ce n'est pas théorique : les salariés du jeu de référence SC-03 portent déjà
`reposQuotidienMinimum: 11.0` et `heuresMaximumParSemaine: 39.0` / `44.0`.

#### Effet réellement observé après livraison de S3 (2026-08-10)

> ⚠️ Cette section corrige une prévision de la version initiale du présent document, qui
> annonçait un changement du score SC-03. **Ce changement n'a pas eu lieu.**

| Constat | Mesure |
|---|---|
| Les deux contraintes sont bien évaluées sur SC-03 | 2 *matches* chacune dans le `ScoreExplanation` |
| Pénalité produite sur le jeu de référence | **0** — le planning respecte déjà les deux règles |
| Score SC-03 avant / après S3 | **inchangé : `0hard / -960soft`** |
| Suite de tests complète | 360 tests, 0 échec |

Le solveur dispose en SC-03 de la liberté de réaffecter : il satisfait donc les deux nouvelles
règles sans dégrader le score. Le risque décrit plus haut reste réel pour d'autres jeux de
données — il ne s'est simplement pas matérialisé sur celui-ci.

**Un effet visible subsiste malgré tout** : le bloc `solverResult.scoreBreakdown` d'une réponse
SC-03 porte désormais deux lignes supplémentaires, `LEGAL_SOFT_REPOS_QUOTIDIEN_MINIMUM` et
`LEGAL_SOFT_HEURES_MAX_PAR_SEMAINE`, avec `quantity: 0.0` et `weightedImpact: 0`. C'est le
comportement normal du `scoreBreakdown` — une contrainte mesurée mais sans impact y figure à 0,
règle déjà posée au §1.1 du contrat de sortie. Additif, non bloquant, mais à signaler à WinDev.

#### Doctrine

C'est le fonctionnement prévu par l'activation progressive (§5.1 du cadrage général, lot L7) :
une règle à la fois, avec évaluation du scoring à chaque étape. Deux conséquences durables :

1. chaque lot activant une règle **inclut** la réévaluation des jeux d'essai SC-01 et SC-03 ;
2. l'engagement « SC-03 n'est pas modifié » porte sur **son contrat et son code**, pas sur son
   score — qui reste libre d'évoluer à mesure que les règles s'activent.

Isoler les contraintes par scénario supposerait un `ConstraintProvider` paramétré par scénario,
qui n'existe pas et dont le coût dépasserait celui de SC-06 entier. Non retenu.

**S7 est hors chemin critique.** Il suit le rythme d'activation progressive du cadrage général
(lot L7), dicté par l'évaluation du scoring et non par le calendrier d'intégration. SC-06 est
exploitable sans lui.

**Le renommage `contraintesReglementaires` → `contraintesMetier`** (lot L5 du cadrage général)
reste **hors périmètre**. La mise en garde du §6.6 — « ne pas faire naître de nouveaux champs
sous un nom qu'on sait devoir changer » — ne s'applique pas ici : les champs ajoutés par SC-06
naissent dans le bloc `contrat`, distinct.

---

## 10. Trajectoire et points ouverts

### 10.1 Annualisation — transportée, non exploitée

`estAnnualise` est transporté dès S2, sans effet sur le classement.

**Cible** : il est moins grave de faire travailler 40 h sur une semaine une personne
habituellement à 35 h si elle est annualisée. Le critère se déplace alors du plafond
hebdomadaire vers **le cumul d'heures excédentaires sur la période d'annualisation**.

Deux données manquent au contrat pour y parvenir : les **bornes de la période d'annualisation**
et le **cumul d'heures à date**. Aucune n'est déductible du planning de la semaine transmise.
Ce chantier fera l'objet de son propre cadrage.

### 10.2 Indisponibilité infra-journalière

Granularité journée aujourd'hui (`IndisponibiliteItemDTO` : `dateDebut`, `dateFin`, sans heures).
La demi-journée pèse ici plus qu'ailleurs, puisque SC-06 arbitre une plage horaire précise : une
personne absente le matin et disponible l'après-midi est aujourd'hui écartée à tort. Évolution
identifiée, hors périmètre.

### 10.3 Lieux — aucune exploitation, ni aujourd'hui ni en SC-06

SC-06 traite les lieux comme des chaînes opaques, transportées et restituées. `sitesAutorises`
n'est lu par aucune contrainte ni aucun filtre : un salarié peut être proposé sur un site qu'il
ne dessert pas. C'est la posture actuelle du moteur (§5.4 du cadrage général), **inchangée par
SC-06** — le lieu du besoin est restitué dans `affectations[].lieu`, il ne restreint pas les candidats.

**Trajectoire** : la restriction par site relève des contraintes individuelles, où elle est
pensée mais non traitée. Elle s'ajouterait au filtre d'éligibilité de la §3.1 sans en modifier
la structure. Le lot **L3** du cadrage général (référentiel de lieux `id` + `libellé`) reste
pertinent, sans être bloquant pour SC-06.

### 10.4 👉 À confirmer — contenu des blocs `planning` et `workMetrics`

La série 50 impose six blocs à la racine de la réponse. Deux d'entre eux n'ont pas de contenu
évident en SC-06, où l'attendu se réduit à « une identité, une activité, un lieu ».

**Proposition** : les deux blocs décrivent **la solution de rang 1, et elle seule**.

- `planning` ne porte **que les créneaux du besoin**, affectés selon la solution de rang 1.
  Le planning existant n'est pas réémis : WinDev le possède déjà, et le réémettre pour toutes
  les ressources candidates alourdirait considérablement la réponse.
- `workMetrics.byRessource` ne porte que **les ressources mobilisées** par la solution de rang 1.

Cette lecture conserve la structure de la série 50 sans la vider de sens. Elle est cohérente avec
l'exigence « on n'attend qu'une identité et l'activité à servir avec le lieu ».

---

## Annexe — méthode de vérification

Les statuts de la §8 proviennent d'une recherche des appels réels aux accesseurs concernés dans
`src/main/java`, *getters*, mappers et tests écartés. Un champ est déclaré EN ATTENTE lorsque
ses seuls lecteurs sont la couche de transport et la couche de mapping.

Fichiers inspectés pour établir les absences : `CreneauInputDTO`, `Creneau`,
`ContraintesReglementairesDTO`, `ContraintesReglementairesSalarie`, `SalarieInputDTO`,
`SalarieReel`, `ConstraintProviderImpl`, `PlanningService`, `ScenarioSc03PreparationService`,
`ScenarioResponseDTO`.

Cette méthode est fiable pour les lectures directes. Elle ne détecte pas une lecture par
réflexion — non utilisée ici — ni une donnée lue puis neutralisée par un filtre en aval.
