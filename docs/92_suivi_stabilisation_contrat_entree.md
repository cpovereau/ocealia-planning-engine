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

## Phase 3 — Cohérence des diagnostics pré-résolution

### Objectif

Clarifier et harmoniser la sémantique de `ignoredCreneaux` après Phase 2.

### Travaux

* aligner `horsHorizon` sur la même sémantique qu'`activiteInconnue` : exclusion réelle avant solveur
* maintenir `aucuneRessourceDansDataset` comme diagnostic pur
* rebaser `aucuneRessourceDansDataset` sur les créneaux réellement transmis (suppression du double comptage introduit par Phase 2)
* documenter l'ordre de partition et le cas limite `date == null`

### Critère de sortie

Les trois compteurs de `ignoredCreneaux` reflètent chacun une sémantique claire et cohérente.

### État

✅ Terminé — 2026-03-23

### Réalisation

#### Politique retenue

Deux décisions structurantes :

1. **`horsHorizon` → exclusion réelle avant solveur.** Un créneau à activité connue mais hors de l'horizon de résolution n'est plus transmis au solveur. Même sémantique qu'`activiteInconnue` depuis Phase 2.
2. **`aucuneRessourceDansDataset` → maintien en diagnostic pur.** Un créneau sans ressource compatible représente un écart de couverture légitime : le solveur l'affecte à `RessourceNonAffectee`. Exclure ces créneaux masquerait des besoins non couverts.

**Ordre de partition** (codé et documenté) :

1. exclusion `activiteInconnue` (Phase 2) → `creneauxValides`
2. exclusion `horsHorizon` (Phase 3) → `creneauxDansHorizon`
3. calcul `aucuneRessourceDansDataset` sur `creneauxDansHorizon`

Un créneau inconnu + hors-horizon est compté dans `activiteInconnue` uniquement.

**Cas limite noté explicitement** : créneau à `date == null` — ni compté, ni exclu par la partition horizon. Hors périmètre Phase 3, à traiter ultérieurement.

#### Implémentation

**`ScenarioSc03PreparationService`** (seul fichier modifié) :

* Extraction de `dateDebut` / `dateFin` remontée avant le mapping des créneaux.
* Ajout d'une boucle `for` de partition horizon sur `creneauxValides` : DTOs hors-horizon incrémentent `horsHorizon` avec `log.warn`, DTOs dans l'horizon ajoutés à `creneauxDansHorizon`.
* `creneauMapper.toCreneaux()` reçoit `creneauxDansHorizon` (était `creneauxValides`).
* `aucuneRessourceDansDataset` rebasé sur `creneauxDansHorizon` (était `request.getDataSet().getCreneaux()`).
* Suppression du bloc de comptage `horsHorizon` par stream sur la liste brute.

#### Tests

**`ScenarioSc03PreparationServicePhase3Test`** (nouvelle classe, 10 tests) :

* `creneauHorsHorizon_activiteConnue_estExcluAvantSolveur` — avant horizon
* `creneauApresHorizon_activiteConnue_estExcluAvantSolveur` — après horizon
* `creneauDansHorizon_activiteConnue_passeToujoursAuSolveur`
* `mixte_1DansHorizon_1HorsHorizon_seulDansHorizonPasseAuSolveur`
* `tousCreneauxHorsHorizon_solveurRecoit0Creneaux_sansBlocage`
* `creneauInconnu_etHorsHorizon_compteUniquementDansActiviteInconnue` — vérification de l'ordre de partition
* `aucuneRessourceDansDataset_creneauHorsHorizon_nonCompte` — suppression du double comptage
* `aucuneRessourceDansDataset_creneauInconnu_nonCompte` — suppression du double comptage
* `aucuneRessourceDansDataset_creneauTransmis_sansRessourceCompatible_estCompte` — comportement attendu conservé
* `creneauDateNull_nEstNiCompteNiExcluCommeHorsHorizon` — cas limite documenté

#### Non-régression

Suite complète Phase 1 + Phase 2 + Phase 3 exécutée après les modifications : **BUILD SUCCESSFUL** (8s, 0 échec).

---

## Phase 4 — Suppression des illusions du contrat d’entrée

### Objectif

Rendre visibles les champs ignorés ou écrasés silencieusement, sans modifier le comportement du moteur.

### Champs concernés

| Champ | DTO | Comportement réel |
|---|---|---|
| `type` (créneau) | `CreneauInputDTO.type` | Écrasé par `TypeCreneau.IMPOSE` — signal existant en `log.debug` (Phase 1) |
| `priorite` (créneau) | `CreneauInputDTO.priorite` (Integer) | Ignoré — `null` passé au domaine — aucun signal |
| `axesOrganisationnels` (créneau) | `CreneauInputDTO.axesOrganisationnels` | Ignoré — jamais lu par le mapper — aucun signal |
| `libelle` (référentiel) | `ReferentielActiviteDTO.libelle` | Ignoré — absent de `ComptabiliteActivite` — aucun signal |

### Décision retenue

**Option B pour tous les champs** : signal explicite (log.warn ou documentation), sans implémentation métier, sans modification du solveur ni du domaine.

### Actions par champ

* `type` → élever le `log.debug` existant en `log.warn` (alignement avec les autres signaux du pipeline)
* `priorite` → ajouter `log.warn` si valeur non nulle reçue ; documenter le mismatch de types (Integer vs enum `PrioriteCreneau`)
* `axesOrganisationnels` → ajouter `log.warn` si objet non nul reçu
* `libelle` → documenter (champ de présentation, aucune incidence sur la résolution)

### Ce que la phase ne fait pas

* ne branche aucun champ dans le solveur
* ne modifie pas le modèle domaine
* ne supprime pas les champs des DTO
* ne modifie pas SC-01

### Critère de sortie

Chaque champ ignoré émet un signal visible ou est explicitement documenté. Aucun champ n’est absorbé silencieusement.

### État

⏳ À faire

---

## Phase 5 — Clarification du contrat visible

### Objectif

Rendre le contrat d'entrée SC-03 lisible et fiable pour les intégrateurs.

### Travaux

* classifier chaque champ : SUPPORTÉ / TOLÉRÉ / ⚠️ DÉPRÉCIÉ / IGNORÉ
* produire le document de référence `92_contrat_entree_sc03.md`
* tracer la décision de dépréciation sur `activite`
* documenter les comportements implicites (partition, valeurs par défaut)
* différer les décisions sur `prioriteCouverture` et `periode` (Phase 7+, décision métier requise)

### Critère de sortie

Le contrat d'entrée SC-03 est documenté avec un statut explicite pour chaque champ.

### État

✅ Terminé — 2026-03-23

### Réalisation

#### Classification produite

Quatre statuts définis :

| Statut | Signification |
|---|---|
| SUPPORTÉ | Champ exploité, influence la résolution |
| TOLÉRÉ | Champ reçu, mappé ou stocké, sans effet sur la résolution actuelle |
| ⚠️ DÉPRÉCIÉ | `activite` — fallback de `codeActiviteId`, destiné à être supprimé |
| IGNORÉ | Ignoré avec signal explicite (log.warn) |

Champs classés IGNORÉ (signal log.warn actif depuis Phase 4) :
* `creneaux[].priorite`
* `creneaux[].axesOrganisationnels`
* `referentiels.activites[].libelle`

Champs IGNORÉ sans signal pour l'instant (couverture Phase 4 non étendue aux ressources) :
* `salaries[].axesOrganisationnels`
* `salaries[].contratTravail`

#### Document produit

`docs/92_contrat_entree_sc03.md` — tableau complet SUPPORTÉ / TOLÉRÉ / ⚠️ DÉPRÉCIÉ / IGNORÉ pour tous les champs SC-03, comportements documentés, ordre de partition, sémantique des diagnostics.

#### Décisions différées

* `scenarioParameters.prioriteCouverture` : TOLÉRÉ — implémentation ou suppression différée (décision métier requise)
* `scenarioParameters.periode` : TOLÉRÉ — idem

---

## Phase 6 — Clarification des sources de vérité

### Objectif

Clarifier et unifier les sources de vérité du contrat d'entrée SC-03 :

* supprimer les ambiguïtés structurelles
* identifier les doublons de données
* préparer une convergence progressive sans casser les clients existants

### Cas identifiés

| Cas | Champs concernés | Risque |
|---|---|---|
| **A** | `codeActiviteId` vs `activite` (clé activité créneau) | ⚠️ dépréciation actée |
| **B** | matching ressource ↔ créneau (`activitesCompatibles` / `activitesAutorisees`) | ⚠️ risque actif de faux négatifs |
| **C** | `lieu` (créneau) vs `sitesAutorises` / `lieuxAutorises` (ressources) | aucune contrainte active actuellement |
| **D** | contraintes réglementaires individuelles (`SalarieReel`) vs globales (`RegulatoryParameters`) | risque d'incohérence sur futures contraintes |
| **E** | `planningContext.horizon` vs `scenarioParameters.periode` | TOLÉRÉ — décision métier requise |
| **F** | `activitesCompatibles` (salarié) vs `activitesAutorisees` (poste virtuel) | confusion documentaire uniquement |

### Décisions structurantes

* **Cas A** — `codeActiviteId` devient la **source de vérité unique** pour identifier une activité. `activite` est en voie de dépréciation (actée Phase 5) — suppression différée après audit des clients.
* **Cas B** — le matching ressource ↔ créneau dans `auMoinsUneRessourceCompatible()` doit être basé **exclusivement sur `codeActiviteId`**. `activitesCompatibles` des ressources doit contenir des valeurs `codeActiviteId`, jamais des libellés.
* **Cas C** — définir la règle de correspondance `lieu` ↔ `sitesAutorises` **avant** toute activation de contrainte site. Harmoniser le nommage (`sitesAutorises` comme nom canonique).
* **Cas D** — séparation stricte : `SalarieReel.*` pour les contraintes individuelles par salarié, `RegulatoryParameters` réservé aux paramètres globaux. Une contrainte ne doit pas lire les deux pour un même paramètre.
* **Cas E** — `planningContext.horizon` reste la seule source active. `scenarioParameters.periode` nécessite une décision métier explicite (implémenter comme surcharge ou supprimer).
* **Cas F** — `activitesCompatibles` est le nom canonique. `activitesAutorisees` est un alias toléré — harmonisation documentaire à terme.

### Stratégie de transition

* Exploiter les logs WARN Phase 4 pour auditer les appels utilisant le fallback `activite` sans `codeActiviteId`.
* Migration progressive des clients vers `codeActiviteId` — suppression du fallback différée au vu de l'audit.
* Documenter les règles de matching (cas B, cas C) avant toute activation de contrainte.
* Formaliser la frontière individuel/global (cas D) dès la prochaine contrainte réglementaire.

### Priorités

1. **Sécuriser le matching activité (cas B)** — documenter que `activitesCompatibles` attend des valeurs `codeActiviteId`
2. **Préparer la dépréciation `activite` (cas A)** — conditionné à l'audit des clients
3. **Formaliser les contraintes réglementaires (cas D)** — règle à documenter avant la prochaine contrainte
4. **Décision métier sur `periode` (cas E)** — hors périmètre technique
5. **Harmonisation du nommage (cas F)** — documentation uniquement

### Ce que la phase ne fait pas

* pas de suppression immédiate de champs
* pas de refactoring du solveur ni du matching
* pas d'activation de nouvelles contraintes
* pas de modification SC-01

### Critère de sortie

Chaque donnée du contrat SC-03 a une source de vérité claire et une règle de lecture documentée.

### État

✅ Terminé — 2026-03-23

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
| Phase 3  | ✅      |
| Phase 4  | ⏳      |
| Phase 5  | ✅      |
| Phase 6  | ✅      |
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
