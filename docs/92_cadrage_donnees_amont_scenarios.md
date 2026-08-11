# 92 — Cadrage : données amont attendues de WinDev, par scénario

> **Statut** : cadrage d'analyse, 2026-07-30. Les arbitrages de la §6 sont **tranchés**.
> Le découpage de la §7 reste à valider. Ce document sert à découper le chantier, pas à le réaliser.

---

## 1. Objet

Déterminer, scénario par scénario, quelles données WinDev doit transmettre pour que le moteur
produise un résultat exploitable, et dans quel ordre les intégrer.

Quatre familles motivent ce cadrage :

1. le **référentiel d'activités** ;
2. les **lieux** ;
3. le **contrat de travail** du salarié ;
4. les **contraintes métier** attachées au salarié.

## 2. Positionnement documentaire

Ce document ne remplace pas `92_suivi_stabilisation_contrat_entree.md` (10 phases, closes).
Celui-ci regardait **en arrière** : nettoyer et durcir ce que le contrat portait déjà.
Le présent document regarde **en avant** : ce que le contrat devra porter pour SC-02, SC-04 et SC-05.

## 3. Principe directeur — la hiérarchie des sources

Ce principe commande la lecture de tout le reste.

> **Le cadre réglementaire porte des valeurs par défaut** — des moyennes qui s'appliquent en
> l'absence d'information sur un salarié donné.
> **Le contrat et les contraintes individuelles portent le spécifique.**
> Le moteur applique : *valeur du salarié si transmise, sinon valeur réglementaire par défaut.*

Ainsi une durée hebdomadaire réglementaire de 35 h n'est pas en concurrence avec la durée
contractuelle d'un salarié : elle prend le relais quand WinDev n'a rien transmis pour lui.

Corollaire : **toute donnée individuelle est facultative.** Le moteur doit fonctionner sans elle,
et le préciser dans sa restitution plutôt que de la supposer.

## 4. Convention de statut

| Statut | Signification |
|---|---|
| **EXPLOITÉ** | Une contrainte ou un calcul de métrique lit effectivement la donnée |
| **EN ATTENTE** | Transporté et mappé, pas encore lu — **activation progressive assumée** (§5.1) |
| **À RAPATRIER** | Le moteur possède le concept en global ; il doit devenir individuel |
| **ABSENT** | Le concept n'existe ni au contrat ni au domaine |

La distinction entre **EN ATTENTE** et un simple oubli est essentielle : les règles réglementaires
sont activées progressivement, afin d'évaluer à chaque étape la pertinence du scoring. Un champ
non lu n'est donc pas un défaut — c'est une étape non encore franchie.

Les statuts ont été **vérifiés dans le code** (appels réels aux accesseurs), pas déduits de la
documentation. Méthode en annexe.

---

## 5. État des lieux vérifié

### 5.1 Ce qui est réellement branché

Le référentiel d'activités est le pivot : `compteDansCharge` est lu par **14 contraintes** et par
le calcul des WorkMetrics. Le principe est uniforme — code introuvable au référentiel → « ce n'est
pas du travail » → le créneau sort du champ de la règle. Un code non déclaré ne dégrade pas le
résultat, il le vide.

Sont également branchés : `travailleJourFerie` (`JourFerieRefuse`, HARD), `travailDeNuit`
(`NuitSalarieNonNuit`, SOFT + WorkMetrics), et trois contraintes individuelles —
`amplitudeJournaliereMaximum`, `joursConsecutifsMaximum`, `heuresMinimumParJour`.

### 5.2 Contraintes métier — écart entre le contrat actuel et la cible

Cible retenue (§6.4) et correspondance avec les 8 champs déjà au contrat, aujourd'hui regroupés
sous `contraintesReglementaires` — bloc appelé à être renommé (§6.6) :

| Contrainte cible | Champ actuel | Statut |
|---|---|---|
| Amplitude journalière max | `amplitudeJournaliereMaximum` | **EXPLOITÉ** |
| Jours travaillés consécutifs max | `joursConsecutifsMaximum` | **EXPLOITÉ** |
| Durée travaillée journalière max | `heuresMaximumParJour` | **EN ATTENTE** — la contrainte existante compare à une constante de 780 min |
| Durée travaillée hebdomadaire max | `heuresMaximumParSemaine` | **EN ATTENTE** — aucune contrainte |
| Nuits consécutives max | — | **À RAPATRIER** — seuil global `SeuilsDeTolerance` |
| Repos après nuits | — | **À RAPATRIER** — seuil global `SeuilsDeTolerance` |
| Dimanches travaillés max | — | **À RAPATRIER** — seuil global `SeuilsDeTolerance` |

Quatre champs du contrat actuel ne figurent pas dans la cible : `heuresMinimumParJour`
(pourtant **exploité** aujourd'hui), `heuresMinimumParSemaine`, `reposQuotidienMinimum`,
`nuitsMaximumParSemaine`.

👉 **À confirmer** : les conserve-t-on ? `heuresMinimumParJour` est actif — le retirer changerait
le comportement du moteur. Voir §6.5.

### 5.3 Les trois seuils à rapatrier — point de vigilance

`maxNuitsConsecutives`, `reposApresNuitsEnJours` et `maxDimanchesTravailles` vivent aujourd'hui
dans `SeuilsDeTolerance`, au niveau du contexte de planification. Le constructeur employé en
production ne les initialise pas et aucun *setter* n'est appelé hors test : **ils valent 0**.
Trois contraintes enregistrées les lisent.

Le cas ne s'est jamais présenté sur les jeux d'essai (ni nuit, ni dimanche travaillé), donc
aucun symptôme observé. Mais le rapatriement vers le salarié devra traiter la question :
**que fait le moteur quand la valeur est absente ?** Le principe de la §3 impose une réponse
explicite — valeur réglementaire par défaut, ou contrainte désactivée.

> **Traité — chantier S7** (`92_cadrage_socle_reglementaire.md`). Les trois seuils, et deux autres
> repérés au passage, vivent désormais dans `ContraintesReglementairesSalarie`, alimentés par le
> contrat, un jeu de seuils par salarié. Ils ont été retirés de `SeuilsDeTolerance` au lot S7.8.
>
> Réponse retenue à la question posée ici : **seule l'absence désactive une règle.** Un maximum à
> 0 interdit tout, un minimum à 0 n'exige rien, et le zéro se lit à la lettre. Une fenêtre
> glissante fait exception — c'est une taille, pas une borne : elle exige au moins 1 jour. Cette
> lecture a d'ailleurs révélé un décalage d'une unité dans `NuitsConsecutivesMax`, qui ne testait
> le dépassement qu'en prolongeant une séquence et jamais en l'ouvrant.

### 5.4 Lieux — transport et restitution, pas de règle

Le rôle des lieux est de produire des données cohérentes avec l'environnement client WinDev.
Aucune contrainte ne les exploite, et c'est un choix : il n'y a pas d'arbitrage entre lieux à ce jour.

| Emplacement | État |
|---|---|
| `sitesAutorises` (salarié) | Transporté, mappé |
| `sitesAutorises` (poste virtuel) | Transporté, mappé |
| `lieu` (créneau, SC-03) | Transporté, mappé jusqu'au domaine |
| **`lieu` dans la réponse** | ~~ABSENT~~ → **restitué** (L2, 2026-07-31) |
| Référentiel de lieux | **ABSENT** — chaînes libres, aucun libellé |

👉 ~~**Écart identifié** : un planning entrant qui porte un lieu ressort sans.~~
**Corrigé au lot L2 (2026-07-31)** : `lieu` et `id` sont désormais restitués dans
`planning.jours[].creneaux[]`. L'exigence « la donnée reçue doit être restituée » est tenue
pour ces deux champs.

**Trajectoire** : le lieu doit pouvoir porter, dans un second temps, des plages de contrainte
par jour (« ce site n'est ouvert que tels jours, à telles heures »). Le référentiel doit donc
être conçu extensible, même si sa première version se limite à `id` + `libellé`.

### 5.5 Contrat de travail — bloc absent

Aucun bloc `contrat` n'existe au contrat d'entrée. Deux des six champs cibles existent
néanmoins déjà, à la racine de l'objet salarié : `travailleJourFerie` et `travailDeNuit`.

👉 Leur intégration au bloc `contrat` est donc une **migration**, pas un ajout : il faudra
décider entre déplacement avec période de compatibilité, ou maintien à la racine.

---

## 6. Arbitrages

### 6.1 Tranché — codes d'activité réels

WinDev transmet le référentiel **et** déclare quels codes d'activité utiliser.

Motif : le résultat doit pouvoir être réintégré côté WinDev. Des codes fictifs imposeraient une
table de correspondance complexe et sans valeur. Le moteur doit parler le vocabulaire du client.

**Conséquence directe** : SC-01 attribue aujourd'hui le code `"travail"`, écrit en dur dans le
moteur. Un champ de code activité doit être ajouté aux paramètres SC-01. C'est le seul geste
qui supprime cette valeur en dur.

### 6.2 Tranché — lieux : liste de codes

Référentiel de lieux réduit à `id` + `libellé` dans un premier temps. Pas d'arbitrage entre
lieux, donc pas d'attributs de site à ce stade.

Le lieu doit être **restitué** partout où il a été reçu.

### 6.3 Tranché — bloc `contrat` salarié

| Champ | État actuel |
|---|---|
| Durée journalière travaillée moyenne | ~~ABSENT~~ → `contrat.heuresMoyennesParJour` — **transporté** |
| Durée hebdomadaire | ~~ABSENT~~ → `contrat.heuresHebdomadairesHabituelles` — **transporté** |
| Nombre de jours travaillés par semaine | ~~ABSENT~~ → `contrat.joursTravaillesParSemaine` — **transporté**, défaut 5 |
| Travaille de nuit | Existe à la racine du salarié — **EXPLOITÉ** |
| Travaille le week-end | **ABSENT** — toujours pas au contrat |
| Travaille les jours fériés | Existe à la racine du salarié — **EXPLOITÉ** |
| *(ajout)* Salarié annualisé | `contrat.estAnnualise` — **transporté, non exploité** |

Ce bloc décrit **ce que le salarié fait normalement**. Il ne décrit pas ce qui lui est interdit —
c'est l'objet des contraintes individuelles.

> **Réalisé au lot S2 de SC-06, 2026-08-10** — voir `92_cadrage_scenario_sc-06.md` §4.6.
> Trois écarts par rapport à la cible ci-dessus, tous arbitrés :
>
> - **`travailDeNuit` et `travailleJourFerie` restent à la racine.** Les migrer dans le bloc
>   serait une rupture de contrat pour SC-03, déjà en service, sans contrepartie. La cible reste
>   valide comme trajectoire ; elle n'est pas ouverte.
> - **« Travaille le week-end » n'a pas été ajouté** — aucun besoin exprimé, et les dimanches
>   sont déjà couverts par `DimanchesTravaillesMax`.
> - **`estAnnualise` s'ajoute à la cible.** Motif métier : un dépassement hebdomadaire est moins
>   grave pour un salarié annualisé. Transporté sans exploitation — le critère cible porte sur le
>   cumul d'heures excédentaires sur la période d'annualisation, et deux données manquent encore
>   au contrat pour le calculer.
>
> Les **dates de début et de fin de contrat**, un temps envisagées, ont été écartées : le filtrage
> des salariés hors contrat relève de WinDev, en amont.

### 6.4 Tranché — contraintes métier, attachées au salarié

Sept contraintes, **toutes facultatives**, pouvant diverger d'un salarié à l'autre :

durée travaillée journalière max · amplitude journalière max · durée travaillée hebdomadaire max ·
jours travaillés consécutifs max · nuits consécutives max · repos après nuits · dimanches
travaillés max

Trois d'entre elles sont aujourd'hui des seuils globaux et doivent devenir individuelles (§5.3).

### 6.5 Ouvert — sort des quatre champs hors cible

`heuresMinimumParJour` (actif), `heuresMinimumParSemaine`, `reposQuotidienMinimum`,
`nuitsMaximumParSemaine` : conservés, ou retirés du contrat ?

Le cas de `heuresMinimumParJour` doit être tranché en premier : il est exploité, le retirer
modifierait le comportement du moteur.

### 6.6 Tranché — « contraintes métier »

Les sept plafonds de la §6.4 s'appellent **contraintes métier**.

Motif : ce ne sont pas des règles légales brutes, mais **l'application locale — conventionnelle,
sectorielle — de la réglementation**. Un plafond de nuits consécutives inscrit dans un accord
d'entreprise reste une contrainte métier même si son fondement est légal.

Le vocabulaire final :

| Bloc | Contenu | Nature |
|---|---|---|
| `contrat` | Ce que le salarié **fait normalement** | Descriptif |
| `contraintes métier` | Ce à quoi le salarié **est soumis** | Prescriptif, plafonds |

#### Conséquence — renommage d'un champ déjà en service

`contraintesReglementaires` est **déjà transmis par WinDev**. Il figure dans le schéma SC-03
exposé au client, le JSON de référence, l'entrée du FileAdapter et les exemples validés — 17
documents au total, plus `ContraintesReglementairesDTO` et `ContraintesReglementairesSalarie`
côté code.

Le renommer est donc une rupture de contrat, pas un simple ajustement de vocabulaire.

**Chemin de migration recommandé** : `@JsonAlias("contraintesReglementaires")` sur le nouveau
champ `contraintesMetier`. WinDev migre à son rythme, les deux noms sont acceptés pendant la
transition, l'ancien est retiré à une échéance annoncée.

Ce motif est déjà éprouvé dans ce projet : `SalarieInputDTO` accepte `lieuxAutorises` pour
`sitesAutorises` et `activitesAutorisees` pour `activitesCompatibles`. La migration ne demande
donc aucune invention.

À noter : la Phase 10B du chantier précédent a durci la tolérance aux champs inconnus. L'alias
doit être **déclaré explicitement**, et non absorbé silencieusement — c'est cohérent avec cette
posture de durcissement.

#### Point de vigilance — collision avec le découpage des contraintes

Le code sépare `constraints/legales/` et `constraints/metier/`. Or les contraintes qui
consommeront le bloc `contraintesMetier` vivent dans `legales/` — un développeur les cherchera
dans `metier/` et ne les y trouvera pas.

Ce n'est pas nécessairement une incohérence : les deux mots portent sur des axes différents.

- `legales/` vs `metier/` classe les règles selon **l'origine de la règle** : cadre du temps de
  travail d'un côté, besoin de service de l'autre.
- « contrainte métier » qualifie **l'origine de la valeur** : une calibration locale.

Une règle peut donc légitimement être d'origine légale et paramétrée par une valeur
conventionnelle. Mais cette distinction n'est écrite nulle part, et le commentaire de
`DimanchesTravaillesMax` qualifie déjà son seuil de « conventionnel / métier » tout en résidant
dans `legales/`.

**Recommandation** : conserver le découpage des paquets et **écrire son critère** dans
`20_DECISIONS_CONCEPTION_OPTAPLANNER.md`. Renommer un paquet n'apporterait rien — `metier/`
est déjà occupé par une famille réellement différente.

👉 **À confirmer** : le critère actuel semble être « plafond quantitatif » pour `legales/`
(durées, comptages, repos) contre « autorisation individuelle et besoin de service » pour
`metier/` (`JourFerieRefuse`, `NuitSalarieNonNuit`, couverture, postes virtuels). C'est une
lecture déduite du contenu des paquets, pas une règle écrite — à valider avant de la figer.

---

### 6.7 Tranché — identité des créneaux et clé de réintégration

`Id_Journee` est la **clé primaire d'une ligne de la base WinDev**, pas un identifiant métier.
Le moteur la reçoit dans `creneaux[].id`, la transporte et la restitue — il n'en est jamais
propriétaire.

**Décision** : WinDev renseigne `id` dans tous les cas. Pour un créneau servi, c'est
l'`Id_Journee`. Pour un besoin sans ligne en base, c'est un identifiant dédié sous préfixe
distinct (`BES-00X`). Un même scénario peut mêler les deux natures — c'est le cas de SC-03,
qui transmet des créneaux servis **et** des besoins potentiellement non servis.

Motif : `Creneau.id` porte l'annotation `@PlanningId`. OptaPlanner exige une valeur non nulle
et unique sur tous les créneaux d'un scénario. Un besoin arrivant sans identifiant ne dégrade
pas la restitution — **il empêche la résolution**. La convention `BES-00X` satisfait la
contrainte au lieu de la contourner, et laisse le contrat d'entrée inchangé : `id` y est déjà
requis.

**Trois règles pour le moteur** :

1. `id` est une **chaîne opaque** — transportée, restituée, jamais interprétée. La convention
   de préfixe est lue par WinDev seul.
2. Le moteur ne fabrique **jamais** d'`Id_Journee`. Les créneaux qu'il génère portent son
   propre préfixe, `SC01-<date>-<séquence>`, qui ne désigne aucune ligne en base.
3. L'unicité vaut sur l'ensemble du scénario, toutes origines confondues.

Point vérifié côté moteur : SC-01 respectait déjà cette convention sans qu'elle soit écrite —
le builder produit `SC01-<date>-<séquence>`. Aucune correction de code n'était nécessaire de
ce côté, seulement une mise par écrit.

👉 **À confirmer côté WinDev** : que les créneaux servis partent avec leur `Id_Journee` telle
quelle, sans préfixe susceptible de rejoindre un jour l'espace des besoins. Avec des clés
primaires brutes d'un côté et `BES-` de l'autre, aucune collision n'est possible.

Écrit dans : `20_DECISIONS_CONCEPTION_OPTAPLANNER.md` (invariant),
`50_ScenarioContract.md` §3.5 (entrée), `50_ScenarioResponseContract.md` §2.1 (sortie).

---

## 7. Ce que chaque scénario exige

| Donnée | SC-01 | SC-02 | SC-03 | SC-04 | SC-05 |
|---|:---:|:---:|:---:|:---:|:---:|
| Référentiel activités | ✅ | ✅ | ✅ | ✅ | ✅ |
| Code activité des créneaux générés | ⚠️ manquant | — | n/a | n/a | n/a |
| Référentiel lieux (id + libellé) | ○ | ○ | ✅ | ○ | ✅ |
| Restitution du lieu | ○ | ✅ | ✅ | ✅ | ✅ |
| Bloc contrat salarié | ○ | ✅ | ○ | ✅ | ✅ |
| Contraintes métier | ○ | ✅ | ○ | ✅ | ✅ |
| Jours fériés (cadre global) | ⚠️ partiel | ✅ | ✅ | ✅ | ✅ |
| Planning existant / affectations | — | ✅ | partiel | ✅ | ✅ |
| Créneaux figés vs ajustables | — | ✅ | ○ | ✅ | ○ |
| Historique des compteurs | — | ○ | — | ✅ | ✅ |
| Seuil de surcharge acceptable | — | ✅ | ○ | ✅ | ✅ |
| Objectif d'arbitrage | — | — | — | ○ | ✅ |

✅ requis · ○ utile non bloquant · — sans objet · ⚠️ manque identifié

**Lecture** : trois familles apparaissent dès SC-02 et ne disparaissent plus — le **planning
existant**, le **contrat salarié** et le **seuil de surcharge**. Ce sont elles qui font basculer
le moteur du mode « je génère » vers le mode « j'ajuste sans casser ». Aucune n'est au contrat.

---

## 8. Découpage proposé

Ordonné par dépendance, pas par valeur métier. Chaque lot est livrable seul.

| Lot | Objet | Prérequis | Portée |
|---|---|---|---|
| **L1** | Code activité explicite en SC-01 | 6.1 ✅ | Contrat SC-01 + builder — **livré 2026-07-30** |
| **L2** | Restitution du lieu **et de l'identifiant** dans la réponse | 6.2 ✅, 6.7 ✅ | Contrat de sortie — **livré 2026-07-31** |
| **L3** | Référentiel de lieux (`id` + `libellé`) | L2 | Contrat d'entrée |
| **L4** | Bloc `contrat` salarié | 6.3 ✅, 6.6 ✅ | Contrat d'entrée + domaine — **livré 2026-08-10** (lot S2 de SC-06) |
| **L5** | Renommage `contraintesReglementaires` → `contraintesMetier` avec alias | 6.6 ✅ | Contrat + DTO + domaine + docs |
| **L6** | Contraintes métier : compléter les 2 manquantes, rapatrier les 3 seuils globaux | L5, 6.5 | Contrat + domaine + 3 contraintes |
| **L7** | Activation progressive des règles | L6 | Une règle à la fois, avec évaluation du scoring |
| **L8** | Planning existant + créneaux figés | — | Ouvre SC-02 et SC-04 — **socle livré 2026-08-10** (lot S1 de SC-06) : `ressourceAffecteeId`, `@PlanningPin`, mapping. Reste à câbler dans les scénarios eux-mêmes |

**L1 et L2 sont immédiatement actionnables** — arbitrages tranchés, aucune dépendance, périmètre
réduit. L2 corrige de surcroît un écart avéré.

**L2 couvre `id` autant que `lieu`.** Les deux relèvent de la même exigence — « la donnée reçue
doit être restituée » — et se corrigent au même endroit, `ScenarioResponseMapper`. Les traiter
séparément imposerait deux migrations de contrat successives à WinDev. Sans `id` en sortie, le
planning ne peut être réassocié aux lignes d'entrée que par triangulation
(salarié + jour + horaires), ce qui devient ambigu dès que deux créneaux se ressemblent.

**L5 précède L6 volontairement.** Renommer d'abord, enrichir ensuite : l'inverse ferait naître
les nouveaux champs sous un nom qu'on sait devoir changer, et doublerait la migration côté WinDev.

**Seul L6 reste conditionné à un arbitrage ouvert** — le sort des quatre champs hors cible (§6.5),
dont `heuresMinimumParJour` qui est actif.

**L6 n'a pas de fin datée** : c'est le rythme d'activation progressive, dicté par l'évaluation
du scoring, pas par le calendrier d'intégration.

**L7 est le plus structurant** et le plus indépendant. Il mérite son propre cadrage et ne devrait
pas être mélangé aux autres lots.

---

## Annexe — méthode de vérification

Les statuts de la §5 proviennent d'une recherche des **appels réels** aux accesseurs concernés
dans `src/main/java`, en écartant les *getters*, les mappers et les tests. Un champ est déclaré
EN ATTENTE lorsque ses seuls lecteurs sont la couche de transport et la couche de mapping.

Cette méthode est fiable pour les lectures directes. Elle ne détecte pas une lecture par
réflexion — non utilisée ici — ni une donnée lue puis neutralisée par un filtre en aval.

Les affirmations de la §5.3 reposent sur l'absence d'appel de *setter* en production et sur la
signature du constructeur employé. La conséquence comportementale reste à établir par le test,
au moment du rapatriement.
