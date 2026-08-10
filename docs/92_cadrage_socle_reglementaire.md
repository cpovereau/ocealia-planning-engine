# 92 — Cadrage : remise en service du socle réglementaire (lot S7)

> **Objet** — six contraintes réglementaires sont déclarées dans le moteur et ne se déclenchent
> jamais pour un client conforme au contrat. Ce document établit le constat, l'ordre de
> réparation et le point de contrôle de chaque étape.
>
> **Statut** — S7.0 et S7.1 livrés. S7.2 à S7.8 à réaliser.
> **Contrat concerné** — `50_ScenarioContract.md` §3.7 · **Suivi** — `90_SUIVI_DEVELOPPEMENT_MOTEUR.md`

---

## 1. Le constat

### 1.1 Six contraintes muettes

Elles lisent l'activité du créneau via `ref.getByCode(creneau.getActivite())` — le champ
**déprécié** — sans repli vers `codeActiviteId`. Or SC-03 et SC-06 n'envoient que
`codeActiviteId`. Le référentiel ne trouve rien, le filtre « compte dans la charge » est faux,
la contrainte ne produit **aucun match**.

| Contrainte | Portée | Règle | Seconde cause d'extinction |
|---|---|---|---|
| `NuitsConsecutivesMax` | **HARD** | R3 | seuil global à 0, **sans garde** |
| `ReposObligatoireApresNuits` | **HARD** | R4 | seuil global à 0, garde interne |
| `ReposHebdomadaireMin` | **HARD** | R7 | — (socle 7 j / 1 j off en dur) |
| `ReposHebdomadaireGlissant` | **HARD** | R7 | seuil global à 0, garde interne |
| `DureeMaximaleLegaleParSalarie` | **HARD** | — | maille erronée (§1.3) |
| `DimanchesTravaillesMax` | SOFT | R9 | seuil global à 0, **sans garde** — ✅ traitée en S7.1 |

Neuf autres contraintes portent bien le repli, posé en Phase 10A :
`AlternanceJourNuit`, `AmplitudeJournaliere`, `HeuresMaximumParSemaine`, `HeuresMinimumParJour`,
`JoursConsecutifsMax`, `PenibilitesLegalesMinutes`, `ReposQuotidienMinimum`, `CreneauDeNuit`,
`DetteReposSurReposHebdomadaire`. Le repli n'a pas été généralisé aux six autres.

`Creneau.getCodeActiviteEffectif()` existe et sert déjà à la restitution — **aucune contrainte ne
l'utilise**.

### 1.2 Pourquoi la suite de tests n'a rien vu

Les tests alimentent `activite` ; les clients alimentent `codeActiviteId`. Les contraintes sont
donc **vertes en test et mortes en production**. C'est la seule raison pour laquelle l'écart a
tenu aussi longtemps.

Preuve empirique : le jeu de référence SC-03 comporte une nuit (`CRE-VN-01` 22:00–06:00) et un
dimanche travaillé (`CRE-DI-01`), `workMetrics` restitue `nbDimanchesTravailles: 1` et
`heuresNuit: 8.0` — et le `scoreBreakdown` ne contient ni `LEGAL_SOFT_DIMANCHES_TRAVAILLES_MAX`
ni `LEGAL_HARD_NUITS_CONSECUTIVES_MAX`.

### 1.3 Deux défauts de fond, masqués par le premier

**Les seuils globaux valent 0.** `PlanningContext.defaultSeuilsDeTolerance()` n'appelle que le
constructeur à trois arguments. Les cinq champs `int` non finaux de `SeuilsDeTolerance`
(`maxNuitsConsecutives`, `reposApresNuitsEnJours`, `reposHebdoFenetreJours`,
`reposHebdoMinJoursOffDansFenetre`, `maxDimanchesTravailles`) ne sont **jamais** affectés.
`ReposObligatoireApresNuits` et `ReposHebdomadaireGlissant` s'en gardent ; `NuitsConsecutivesMax`
et `DimanchesTravaillesMax` non.

**`DureeMaximaleLegaleParSalarie` compare une maille à une autre.** Son `groupBy` agrège sur
**tout l'horizon** et compare le total à `DUREE_MAX_LEGALE = 780`, une valeur **journalière**
(13 h). Deux journées ordinaires suffisent à la franchir. C'est la plus explosive des six.

> **Le couplage est la difficulté centrale.** Réparer le repli seul rendrait toute nuit
> consécutive immédiatement HARD (seuil 0), chaque dimanche travaillé pénalisé de 5 000 points,
> et toute semaine normale illégale. Les deux défauts s'annulent : c'est pourquoi rien n'a
> jamais échoué.

### 1.4 Code mort

`CreneauJourFerie` et `CreneauDeNuit` ne sont enregistrées dans aucun `ConstraintProvider`.

---

## 2. Décisions

| # | Décision | Motif |
|---|---|---|
| D1 | **Les seuils sont individuels**, portés par `contraintesReglementaires` | Un plafond de nuits ou de dimanches relève du contrat de la personne, pas du contexte de calcul. Trois salariés = trois jeux de seuils. |
| D2 | **Seuil absent ou nul ⇒ contrainte inactive** | Ces cinq champs sont absents de tous les payloads existants. Les faire déclencher sur une donnée non renseignée rendrait illégal tout planning en cours. `0` reste tracé en WARN. |
| D3 | **Une contrainte par lot**, score réévalué à chaque fois | Six réveils simultanés rendraient tout écart de score inattribuable. |
| D4 | La **paire** fenêtre / jours off est indissociable | Une fenêtre sans minimum de jours off ne décrit aucune règle. Transmise à moitié : contrainte inactive + WARN. |
| D5 | `SeuilsDeTolerance` conserve ses trois champs de **tolérance globale** | `surchargeMaxParSalarie`, `violationsLegalesMax`, `violationsMetierMax` sont des bornes d'acceptabilité d'une solution, pas des règles individuelles. Ils restent à leur place. |

### D2 et l'invariant « un vide ne suppose jamais que la chose est possible »

Il n'y a pas contradiction. Le moteur ne conclut pas que la nuit consécutive est autorisée : il
constate qu'**on ne lui a donné aucun plafond à faire respecter**, et s'abstient de juger plutôt
que d'inventer une limite. L'invariant interdit de déduire une permission d'un silence ; il
n'oblige pas à fabriquer une interdiction à partir de rien.

---

## 3. Découpage en lots

L'ordre va du moins au plus perturbant, pour qu'une surprise reste imputable.

| Lot | Objet | Effet attendu sur le score |
|---|---|---|
| **S7.0** ✅ | Socle : seuils portés au salarié, règle d'activation, **test de référence** | **Aucun** — point de contrôle |
| **S7.1** ✅ | `DimanchesTravaillesMax` — repli + `dimanchesTravaillesMaximum` | **Aucun** — mesuré (§7) |
| S7.2 | `NuitsConsecutivesMax` — repli + `nuitsConsecutivesMaximum` | HARD |
| S7.3 | `ReposObligatoireApresNuits` — repli + `joursReposMinimumApresNuits` | HARD |
| S7.4 | `ReposHebdomadaireGlissant` — repli + paire de seuils | HARD |
| S7.5 | `ReposHebdomadaireMin` — repli seul (socle légal, sans seuil individuel) | HARD |
| S7.6 | `DureeMaximaleLegaleParSalarie` — **correction de maille** + `heuresMaximumParJour` | HARD, le plus sensible |
| S7.7 | Contraintes absentes : `heuresMinimumParSemaine`, `nuitsMaximumParSemaine` | nouvelles |
| S7.8 | Nettoyage : code mort, retrait des cinq champs de `SeuilsDeTolerance`, doc | Aucun |

### Contenu type d'un lot S7.1 → S7.6

1. Remplacer `getActivite()` par `getCodeActiviteEffectif()`.
2. Lire le seuil sur le salarié, via `ContraintesReglementairesSalarie.seuilActif(...)`.
3. Déplacer l'assertion correspondante dans `SocleReglementaireBaselineTest` (§4).
4. Couvrir la contrainte par un `ConstraintVerifier` dédié : seuil absent, seuil nul, égalité,
   dépassement.
5. Ajouter le motif SC-06 correspondant dans `MotifCandidat` si la contrainte doit expliquer un
   rang.
6. Exécuter la suite complète, **relever l'écart de score** sur SC-01, SC-03 et SC-06, et le
   consigner dans `90_SUIVI_DEVELOPPEMENT_MOTEUR.md`.

---

## 4. L'instrument de bascule

`SocleReglementaireBaselineTest` rend la dormance **exécutable**. Chaque situation fautive y est
jouée deux fois, dans trois blocs :

- **`ClientHistorique`** — le créneau porte `activite`. Ces assertions établissent que la
  situation est réellement fautive. Sans elles, un `penalizesBy(0)` passerait pour la mauvaise
  raison — c'est le piège classique du test de non-déclenchement.
- **`ClientConformeAuContrat`** — le créneau porte `codeActiviteId`, comme WinDev. Chaque
  assertion à 0 est un **constat, pas un objectif**, et nomme le lot qui la fera changer.
- **`Reveillees`** — contraintes remises en service. Elles réagissent aux **deux** champs, et
  restent inactives tant qu'aucun seuil individuel n'est transmis.

Réveiller une contrainte consiste alors à déplacer une assertion vers le troisième bloc. Un écart
non voulu se lit ici, sur une contrainte isolée, avant de se lire dans un scénario complet.

Deux tests du bloc témoin assertent **0** : `ReposObligatoireApresNuits` et
`ReposHebdomadaireGlissant` sont éteintes deux fois, et le repli d'activité seul ne les
réveillera pas.

---

## 5. Réalisé — lot S7.0

**Aucune contrainte n'a été modifiée. Le score est inchangé** — 434 tests, 0 échec (413 avant).

| Livrable | Fichier |
|---|---|
| 5 seuils portés au salarié + `seuilActif()` | `domain/ressource/ContraintesReglementairesSalarie.java` |
| Transport | `scenarios/dto/input/ContraintesReglementairesDTO.java` |
| Mapping + WARN `0` + WARN paire incomplète | `scenarios/mapper/ScenarioResourceMapper.java` |
| Champs globaux marqués `@Deprecated(forRemoval)` | `domain/contexte/SeuilsDeTolerance.java` |
| Lecture null-safe du référentiel | `domain/metier/ReferentielComptabiliteActivite.java` |
| Test de référence (12 cas) | `constraints/SocleReglementaireBaselineTest.java` |
| Test de mapping (9 cas) | `scenarios/mapper/SeuilsIndividuelsMappingTest.java` |
| Contrat §3.7, schéma JSON, OpenAPI | `50_*` |

### Une correction non prévue : `getByCode(null)`

`ReferentielComptabiliteActivite.getByCode` déléguait directement à `Map.get`. Le comportement
dépend alors de l'implémentation : `HashMap` — celle des mappers — renvoie `null`, là où
`Map.of(...)` lève une `NullPointerException`. La même contrainte se taisait donc en production
et faisait échouer le calcul de score en test. Le garde explicite aligne les deux et supprime une
NPE latente. `contient()` a été aligné de même.

### Ce que S7.0 ne fait pas

Aucune contrainte ne lit encore les cinq nouveaux seuils. Les renseigner n'a **aujourd'hui aucun
effet** sur le résultat. C'est délibéré : le lot est le point zéro à partir duquel les écarts des
lots suivants deviennent mesurables.

---

## 6. Réalisé — lot S7.1 : dimanches travaillés

**Écart de score : aucun.** 438 tests, 0 échec.

| Livrable | Fichier |
|---|---|
| Repli d'activité + seuil individuel + match sur dépassement seul | `constraints/legales/DimanchesTravaillesMax.java` |
| Repli non nul pour les seuils | `domain/ressource/SalarieReel.contraintesOuAucune()` |
| Motif SC-06 `DIMANCHES_TRAVAILLES_DEPASSES` | `scenarios/dto/MotifCandidat.java` |
| Bloc `Reveillees` (4 cas) | `constraints/SocleReglementaireBaselineTest.java` |
| Seuil individuel + `codeActiviteId` + 2 cas d'activation | `constraints/Phase13ConstraintsTest.java` |

### Trois choix à retenir

**Seuls les dépassements produisent un match.** L'implémentation antérieure pénalisait de 0
lorsque le plafond était respecté, ce qui inscrivait une ligne à impact nul au `scoreBreakdown`.
Un filtre précède désormais la pénalité. La distinction est lisible en restitution : une
contrainte **présente avec `impact 0`** a produit un match sans pénalité, une contrainte
**absente** n'a produit aucun match.

**Le motif SC-06 est non éliminatoire.** Contrairement au repos quotidien, le plafond de
dimanches est une borne conventionnelle d'équité et non un seuil de légalité : un dimanche de
plus reste une décision possible, elle doit être rendue visible et non interdite.

**La pénalité reste globale, seul le seuil est individuel.**
`Penalites.depassementMaxDimanchesTravailles` est un poids de scoring — il arbitre entre familles
de contraintes, il ne décrit pas une règle applicable à une personne.

### Mesure de l'écart

SC-03 reste à `0hard/-960soft`, et `LEGAL_SOFT_DIMANCHES_TRAVAILLES_MAX` reste absent de son
`scoreBreakdown`. Aucun jeu d'essai ni payload de référence ne transmet `dimanchesTravaillesMaximum` :
la contrainte est donc réveillée mais sans seuil, et reste inactive. **La remise en service est
sans effet tant que WinDev n'envoie pas le champ** — ce qui est exactement le comportement voulu.

Corollaire pour les lots suivants : leur écart de score sera lui aussi nul par le même mécanisme,
**sauf** pour `ReposHebdomadaireMin` (S7.5, socle légal en dur, sans seuil individuel) et
`DureeMaximaleLegaleParSalarie` (S7.6, dont la maille est à corriger). Ce sont les deux seuls lots
qui feront réellement bouger les scénarios existants.

---

## 7. Dette repérée en cours de route

Les neuf contraintes déjà pourvues du repli le **réimplémentent en ligne** :

```java
String codeActivite = (creneau.getCodeActiviteId() != null && !creneau.getCodeActiviteId().isBlank())
        ? creneau.getCodeActiviteId() : creneau.getActivite();
```

C'est exactement le corps de `Creneau.getCodeActiviteEffectif()`. Les contraintes remises en
service au titre de S7 appellent la méthode. **À unifier au lot S7.8** : tant que la règle existe
en dix exemplaires, une évolution du repli en oubliera un.

---

## 8. Suites

Avant S7.6, arbitrer la sémantique de `heuresMaximumParJour` : plafond de **durée travaillée**
par journée, à distinguer de `amplitudeJournaliereMaximum` (première à dernière heure, pauses
comprises), déjà appliqué par `AmplitudeJournaliere`. Les deux coexistent et ne mesurent pas la
même chose.
