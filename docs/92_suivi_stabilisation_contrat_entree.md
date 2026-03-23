# 92_suivi_stabilisation_contrat_entree

## Objectif du document

Ce document sert de **pilotage opérationnel** du chantier de stabilisation du contrat d’entrée.

Il permet de :

* suivre les étapes de réalisation,
* tracer les décisions prises,
* vérifier la non-régression du moteur,
* aligner progressivement le contrat cible (SC-03) avec son implémentation réelle.

⚠️ Ce document s’appuie sur :

* `92_audit_contrat_entree` (constat)
* `92_cadrage_stabilisation_contrat_entree_temp` (intention)

---

## Principes de travail

* Ne jamais casser le comportement utile existant sans décision explicite
* Prioriser les zones à risque fonctionnel réel
* Éviter toute dérive vers SC-01
* Travailler par étapes courtes, testables et validées

---

## Phase 1 — Visibilité des comportements implicites

### Objectif

Rendre visibles les tolérances silencieuses sans modifier le comportement.

### Travaux

* ajouter des logs ou diagnostics sur :

  * activités inconnues
  * fallbacks `codeActiviteId / activite`
  * type créneau ignoré
  * champs SC-03 non exploités
* compléter les tests pour vérifier ces comportements

### Critère de sortie

Aucune tolérance critique ne reste invisible.

### État

✅ Terminé — 2026-03-23

### Réalisation

#### Comportements rendus visibles

Quatre tolérances silencieuses instruites par des logs, sans modification de comportement.

| Tolérance | Signal ajouté | Niveau | Localisation |
|---|---|---|---|
| `prioriteCouverture` reçu mais non exploité | `log.warn` conditionnel en tête de `prepare()` | WARN | `ScenarioSc03PreparationService` |
| `periode` reçu mais non exploité | `log.warn` conditionnel en tête de `prepare()` | WARN | `ScenarioSc03PreparationService` |
| Fallback `codeActiviteId → activite` | `log.warn` par créneau concerné, dans la boucle de comptage | WARN | `ScenarioSc03PreparationService` |
| Activité inconnue | `log.warn` par créneau concerné, avec id et code utilisé | WARN | `ScenarioSc03PreparationService` |
| Champ `type` du créneau ignoré | `log.debug` conditionnel sur non-blank | DEBUG | `ScenarioCreneauMapper` |

`debug` retenu pour le champ `type` : comportement architectural connu, pas une anomalie de données entrantes. Les quatre autres cas sont `warn` car ils peuvent signaler une incohérence côté client.

La boucle `stream` de comptage `activiteInconnue` a été convertie en boucle `for` explicite pour permettre l'émission d'un log par créneau. Le résultat du comptage est identique.

#### Tests ajoutés

**`ScenarioSc03PreparationServicePhase1Test`** (nouvelle classe, 6 tests) :

* `activiteInconnue_incrementeCompteurMaisNeBloquesPas` — le créneau inconnu est transmis au solveur, compteur à 1
* `activiteInconnue_codeActiviteIdNullEtActiviteNonReferencee_compteCommeInconnu` — fallback + activité inconnue
* `fallback_codeActiviteIdNull_activiteReferencee_pasCompteeInconnue` — fallback réussi, compteur à 0
* `fallback_codeActiviteIdPresent_prioritaireSurActivite` — `codeActiviteId` prime sur `activite`
* `prioriteCouverture_nAucunEffetSurLePlanningContext` — aucun impact sur `StrategieScoring` ni horizon
* `periode_nAucunEffetSurLHorizonDuPlanningContext` — horizon de `planningContext` toujours utilisé

**`ScenarioCreneauMapperTest`** (2 tests ajoutés) :

* `toCreneau_typeIgnore_imoseApplique_quelqueSoitLaValeurDTO`
* `toCreneau_typeNull_imoseApplique`

#### Non-régression

Suite complète exécutée après les modifications : **BUILD SUCCESSFUL** (1m 47s, 0 échec).

---

## Phase 2 — Politique sur les activités inconnues

### Objectif

Définir et implémenter une politique claire et unique.

### Travaux

* formaliser la règle cible :

  * rejet / exclusion / tolérance encadrée
* aligner :

  * pré-résolution
  * solveur
  * diagnostics
* corriger la cohérence de `ignoredCreneaux`

### Critère de sortie

Comportement unique, documenté et testé.

### État

✅ Terminé — 2026-03-23

### Réalisation

#### Politique retenue

Exclusion réelle avant solveur. Un créneau dont l'activité est absente du référentiel n'est plus transmis au solveur. La sémantique de `ignoredCreneaux.activiteInconnue` est désormais exacte : les créneaux comptés sont réellement exclus.

#### Implémentation

**`ScenarioSc03PreparationService`** (seul fichier modifié) :

* Référentiel construit en **premier** (déplacé avant le mapping des créneaux).
* Boucle `for` de Phase 1 (détection, logs) étendue avec partition directe des DTOs :
  * les DTOs valides (activité connue) sont ajoutés à `creneauxValides`,
  * les DTOs inconnus incrémentent `activiteInconnue` et émettent un `log.warn` "créneau exclu avant solveur".
* `creneauMapper.toCreneaux()` reçoit uniquement `creneauxValides` — plus de filtrage par id.
* Ancienne boucle Phase 1 supprimée (fusionnée) ; variable transitoire `final ref` supprimée.

Choix de filtrage par inclusion directe (DTOs valides collectés) plutôt que par exclusion d'identifiants, pour éviter toute dépendance sur la fiabilité ou l'unicité de l'id de créneau.

#### Tests

**`ScenarioSc03PreparationServicePhase2Test`** (nouvelle classe, 7 tests) :

* `activiteInconnue_estExcluAvantSolveur` — créneau inconnu absent de `planningRequest().creneaux()`
* `activiteInconnue_estCompteInIgnoredCreneaux` — compteur = 1
* `activiteConnue_passeToujoursAuSolveur` — créneau connu présent, compteur = 0
* `mixte_1Connu1Inconnu_seulConnnuPasseAuSolveur` — 1 dans solveur, 1 dans diagnostics
* `fallback_activiteConnueViaChampActivite_passeToujoursAuSolveur` — fallback réussi → inclus
* `fallback_activiteInconnueViaChampActivite_estExclu` — fallback échoué → exclu
* `tousCreneauxInconnus_solveurRecoit0Creneaux_sansBlocage` — liste vide sans exception
* `plusieursCreneauxConnus_tousPassentAuSolveur` — 3 connus, 0 exclu

**`ScenarioSc03PreparationServicePhase1Test`** (mise à jour) :

* `activiteInconnue_incrementeCompteurMaisNeBloquesPas` — assertion `creneaux().size()` corrigée : 2 → 1, commentaire mis à jour pour refléter l'exclusion réelle.

#### Non-régression

Suite complète exécutée après les modifications : **BUILD SUCCESSFUL** (1m 56s, 0 échec).

---

## Phase 3 — Clarification SC-03 (contrat réel)

### Objectif

Supprimer les ambiguïtés contractuelles côté SC-03.

### Travaux

* identifier les champs :

  * réellement utilisés
  * ignorés
  * partiellement exploités
* documenter explicitement le statut de chaque champ
* corriger les incohérences les plus trompeuses

### Critère de sortie

Le contrat SC-03 reflète le comportement réel.

### État

⏳ À faire

---

## Phase 4 — Suppression des écrasements silencieux

### Objectif

Éliminer les champs “faussement actifs”.

### Travaux

* traiter en priorité :

  * `type` de créneau
  * `priorite`
  * `libelle` référentiel
* choisir pour chaque champ :

  * supporté
  * ignoré explicitement
  * supprimé

### Critère de sortie

Plus aucun champ n’est absorbé silencieusement.

### État

⏳ À faire

---

## Phase 5 — Nettoyage des champs non exploités

### Objectif

Réduire la zone grise du contrat.

### Travaux

* traiter :

  * `prioriteCouverture`
  * `periode`
* décider pour chacun :

  * implémentation
  * suppression
  * marquage explicite

### Critère de sortie

Aucun champ “mort” dans le contrat.

### État

⏳ À faire

---

## Phase 6 — Clarification des sources de vérité

### Objectif

Préparer la suppression des redondances.

### Travaux

* définir pour chaque couple :

  * source actuelle
  * source cible
* cas principaux :

  * `codeActiviteId` vs `activite`
  * référentiel vs hardcodé
  * axes organisationnels vs champs historiques

### Critère de sortie

Chaque donnée a une source de vérité identifiée.

### État

⏳ À faire

---

## Phase 7 — Durcissement progressif de la validation

### Objectif

Passer d’un contrat permissif à maîtrisé.

### Travaux

* ajouter validation sur champs réellement nécessaires
* introduire des tests négatifs
* préparer réduction des tolérances

### Critère de sortie

Le moteur rejette les incohérences réelles.

### État

⏳ À faire

---

## Phase 8 — Activation ou retrait des champs partiels

### Objectif

Aligner mapping et usage réel.

### Travaux

* traiter :

  * travail de nuit
  * jour férié
  * contraintes réglementaires
  * structuration des créneaux
* décider :

  * activer
  * maintenir en transport
  * supprimer

### Critère de sortie

Moins de champs “mappés sans effet”.

### État

⏳ À faire

---

## Phase 9 — Alignement documentation / code

### Objectif

Faire converger la documentation avec la réalité.

### Travaux

* produire une version claire du contrat :

  * supporté
  * toléré
  * non supporté
* documenter SC-03 proprement

### Critère de sortie

La documentation décrit le comportement réel.

### État

⏳ À faire

---

## Phase 10 — Nettoyage final

### Objectif

Finaliser la stabilisation.

### Travaux

* réduire `ignoreUnknown`
* supprimer les champs obsolètes
* supprimer les hardcodes transitoires
* simplifier les DTO

### Critère de sortie

Contrat stable, lisible et maîtrisé.

### État

⏳ À faire

---

## Suivi global

| Phase    | Statut |
| -------- | ------ |
| Phase 1  | ✅      |
| Phase 2  | ✅      |
| Phase 3  | ⏳      |
| Phase 4  | ⏳      |
| Phase 5  | ⏳      |
| Phase 6  | ⏳      |
| Phase 7  | ⏳      |
| Phase 8  | ⏳      |
| Phase 9  | ⏳      |
| Phase 10 | ⏳      |

---

## Point d’attention

Le risque principal n’est pas technique mais sémantique :

> faire évoluer le contrat sans savoir précisément ce qui est réellement utilisé.

Ce document doit donc rester aligné en permanence avec :

* l’audit,
* les tests,
* le comportement réel du moteur.
