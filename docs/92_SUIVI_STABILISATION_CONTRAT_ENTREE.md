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

✅ Terminé — 2026-03-24

### Réalisation

#### Signaux ajoutés

| Champ | Signal | Niveau | Localisation | Condition |
|---|---|---|---|---|
| `type` (créneau) | `log.warn` — "champ type reçu et ignoré — TypeCreneau.IMPOSE appliqué" | WARN | `ScenarioCreneauMapper.toCreneau()` | `type` non blank |
| `priorite` (créneau) | `log.warn` — "champ priorite (Integer) reçu et ignoré — PrioriteCreneau non exploité, null transmis au domaine" | WARN | `ScenarioCreneauMapper.toCreneau()` | `priorite` non null |
| `axesOrganisationnels` (créneau) | `log.warn` — "champ axesOrganisationnels reçu et ignoré — aucune contrainte organisationnelle implémentée" | WARN | `ScenarioCreneauMapper.toCreneau()` | objet non null |
| `libelle` (référentiel activité) | Commentaire de code explicite dans `toReferentiel()` | — | `ScenarioResourceMapper.toReferentiel()` | `libelle` absent de `ComptabiliteActivite` — champ de présentation, aucune incidence sur la résolution |

**Élévation `type`** : `log.debug` (Phase 1) → `log.warn` (Phase 4), aligné sur le niveau des autres signaux du pipeline.

**Mismatch documenté** : `CreneauInputDTO.priorite` est `Integer`, le domaine `Creneau.priorite` est `PrioriteCreneau` (enum CRITIQUE/NORMALE/FAIBLE). Aucune conversion n’est réalisée — `null` est toujours transmis au domaine quelle que soit la valeur reçue.

**`libelle`** : `ScenarioResourceMapper` n’a pas de logger (pas d’autres logs dans cette classe). Ajouter un logger pour un seul champ de présentation contreviendrait à l’esprit "signal cohérent avec l’architecture en place". Signal : commentaire `// [Phase 4]` dans la boucle `toReferentiel()`.

#### Comportement inchangé

* `TypeCreneau.IMPOSE` toujours appliqué — valeur de `type` DTO ignorée.
* `PrioriteCreneau null` toujours transmis — aucune lecture de `priorite` DTO.
* `axesOrganisationnels` absent du domaine `Creneau` — aucune transmission.
* `ComptabiliteActivite` inchangée — `libelle` non ajouté.
* Aucune contrainte OptaPlanner touchée.

#### Tests ajoutés

**`ScenarioCreneauMapperTest`** (3 tests ajoutés) :

* `toCreneau_prioriteNonNull_transmisNullAuDomaine` — Integer reçu, `creneau.getPriorite()` est null, `TypeCreneau.IMPOSE` inchangé
* `toCreneau_axesOrganisationnelsNonNull_nAucunEffetSurLeMapping` — objet reçu, pas d’exception, champs de base inchangés
* `toCreneau_prioriteEtAxesNull_champsIgnoresComportementInchange` — cas nominal, comportement de base non régressé

#### Non-régression

Suite complète `fr.project.planning.scenarios.*` exécutée après les modifications : **BUILD SUCCESSFUL** (45s, 0 échec).

---

## Phase 5 — Clarification du contrat visible

### Objectif

Rendre le contrat d'entrée SC-03 lisible et fiable pour les intégrateurs.

### Travaux

* classifier chaque champ : SUPPORTÉ / TOLÉRÉ / ⚠️ DÉPRÉCIÉ / IGNORÉ
* produire le document de référence `92_CONTRAT_ENTREE_SC03.md`
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

`docs/92_CONTRAT_ENTREE_SC03.md` — tableau complet SUPPORTÉ / TOLÉRÉ / ⚠️ DÉPRÉCIÉ / IGNORÉ pour tous les champs SC-03, comportements documentés, ordre de partition, sémantique des diagnostics.

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

Durcir progressivement la validation du contrat d’entrée SC-03 :

* remplacer les comportements implicites ou instables (NPE silencieuses, résultats vides sans signal)
* sécuriser les entrées en rejetant les incohérences critiques
* signaler explicitement les incohérences non critiques

### Politique de validation cible

#### ERROR — rejet immédiat

Validations à ajouter dans `prepare()` avec `IllegalArgumentException` explicite, avant toute partition :

| Cas | Localisation |
|---|---|
| `planningContext.getHorizon()` null | null check avant accès aux dates |
| `horizon.dateDebut` null | null check individuel |
| `horizon.dateFin` null | null check individuel |
| `dateDebut > dateFin` (horizon inversé) | cohérence après les deux null checks |
| `dataSet.getReferentiels()` null | null check avant construction du référentiel |
| `creneau.heureDebut` null | validation dans la boucle de partition (ou dans le mapper) |
| `creneau.heureFin` null | idem |

Ces cas sont directement promus en ERROR sans passage préalable par WARN : ils provoquent déjà un NPE non géré à l’exécution — rendre l’erreur explicite n’est jamais une rupture.

#### WARN — toléré avec signal obligatoire

| Cas | Signal | Granularité |
|---|---|---|
| Référentiel vide (zéro activités) | `"[SC-03] référentiel vide — tous les créneaux seront exclus"` | Global (1 fois) |
| Zéro créneaux après les deux partitions | `"[SC-03] aucun créneau transmis au solveur"` | Global (1 fois) |
| `codeActiviteId` et `activite` tous deux présents et discordants | `"[SC-03] créneau id=’{}’ : codeActiviteId et activite discordants — activite ignorée"` | Par créneau |
| `creneau.date` null | maintenu — cas limite Phase 3 documenté | Par créneau |
| `salarie.id` null | `"[SC-03] salarié sans id — comportement solveur non garanti"` | Par ressource |
| Aucune ressource réelle (salaries + postes vides) | `"[SC-03] aucune ressource dans le dataset — tous les créneaux affectés à RessourceNonAffectee"` | Global (1 fois) |

#### INFO — trace technique

| Cas | Note |
|---|---|
| `codeActiviteId` en doublon dans le référentiel | Si le comportement de `toReferentiel()` est déterministe (dernier gagne / premier gagne), un INFO suffit |

### Champs devenant obligatoires

À documenter dans `92_CONTRAT_ENTREE_SC03.md` comme **obligatoires** dès Phase 7 :

* `planningContext.horizon.dateDebut`
* `planningContext.horizon.dateFin`
* `creneau.heureDebut`
* `creneau.heureFin`
* `dataSet.referentiels`

### Stratégie de transition

* **Étape 1** — ajouter les WARN globaux manquants (référentiel vide, zéro créneaux, discordance) sans modifier aucun comportement existant. Aucun risque de casse.
* **Étape 2** — observer les fréquences en production. Pour chaque candidat ERROR non NPE (`dateDebut > dateFin`, etc.) : si le WARN n’apparaît jamais → promotion directe ; si des clients l’émettent → coordination avant promotion.
* **Étape 3** — promouvoir en ERROR les cas confirmés sans client légitime.
* **Exception** : les NPE latentes (`horizon` null, `dateDebut`/`dateFin` null, `referentiels` null, `heureDebut`/`heureFin` null) sont directement converties en ERROR — elles provoquent déjà un crash non documenté.

### Travaux

* Ajouter les guards ERROR dans `prepare()` (null checks + cohérence horizon + referentiels)
* Ajouter les WARN globaux manquants (référentiel vide, zéro créneaux après partition)
* Ajouter les WARN de cohérence `codeActiviteId` vs `activite` (discordance)
* Mettre à jour `92_CONTRAT_ENTREE_SC03.md` : marquer les champs devenus obligatoires
* Ajouter `ScenarioSc03PreparationServicePhase7Test` (8 tests ciblés)

### Ce que la phase ne fait pas

* pas de validation métier avancée (cohérence organisationnelle, plages horaires métier)
* pas de Bean Validation (`@NotNull`, `@Valid`) — les guards restent dans `prepare()`
* pas de promotion en ERROR de cas non observés en WARN
* pas de refonte du solveur
* pas de modification SC-01

### Critère de sortie

Le moteur :

* rejette explicitement les incohérences critiques (ERROR avec message clair)
* signale toutes les incohérences non critiques (WARN visibles)
* ne présente plus de NPE silencieuses sur des données manquantes prévisibles

### État

✅ Terminé — 2026-03-24

### Réalisation

#### Guards ERROR ajoutés

Tous dans `ScenarioSc03PreparationService.prepare()`, avant toute construction du monde solveur. Remplacent des NPE silencieuses ou des comportements silencieusement incorrects.

| Cas | Message d'erreur | Comportement précédent |
|---|---|---|
| `planningContext.getHorizon() == null` | `[SC-03] planningContext.horizon est requis.` | NPE ligne `getHorizon().getDateDebut()` |
| `horizon.dateDebut == null` | `[SC-03] planningContext.horizon.dateDebut est requise.` | NPE dans la partition hors-horizon |
| `horizon.dateFin == null` | `[SC-03] planningContext.horizon.dateFin est requise.` | NPE dans la partition hors-horizon |
| `dateDebut.isAfter(dateFin)` | `[SC-03] planningContext.horizon incohérent : dateDebut (…) est postérieure à dateFin (…).` | Tous créneaux exclus silencieusement comme hors-horizon |
| `dataSet.getReferentiels() == null` | `[SC-03] dataSet.referentiels est requis.` | `toReferentiel(null)` → référentiel neutre → tous créneaux exclus en `activiteInconnue` |
| `creneau.heureDebut == null` | `[SC-03] créneau id='…' : heureDebut est requise.` | NPE dans `calculerDureeMinutes()` |
| `creneau.heureFin == null` | `[SC-03] créneau id='…' : heureFin est requise.` | NPE dans `calculerDureeMinutes()` |

Note : `dateDebut` et `dateFin` sont désormais déclarées dans la section guards (avant le référentiel), éliminant leur double déclaration dans l'étape de partition Phase 3.

#### Signaux WARN ajoutés

| Signal | Condition | Granularité |
|---|---|---|
| Référentiel vide | `referentiels.activites` null ou vide | Global (1 fois) |
| Discordance `codeActiviteId` / `activite` | les deux présents et non égaux | Par créneau |
| Zéro créneaux après partitions | `creneauxDansHorizon` vide | Global (1 fois) |
| Salarié sans id | `sal.getId()` null ou blank | Par salarié |
| Aucune ressource réelle | salaries vides ET postes vides | Global (1 fois) |

#### Cas volontairement hors périmètre

* `creneau.date == null` : cas limite Phase 3 documenté — créneau passe au solveur, toléré
* `salarie.id == null` : WARN émis, non bloquant — rejet différé si besoin confirmé

#### Sémantique `ignoredCreneaux` inchangée

Les trois compteurs (`activiteInconnue`, `horsHorizon`, `aucuneRessourceDansDataset`) conservent leur sémantique exacte. Aucune contrainte OptaPlanner touchée.

#### Avertissements SonarLint

Les avertissements `java:S6541` (Brain Method) et `java:S3776` (Cognitive Complexity) sur `prepare()` sont des indicateurs de qualité préexistants, aggravés par Phase 7. La refactorisation de la méthode est hors périmètre Phase 7 — à traiter dans un chantier dédié.

#### Tests ajoutés

**`ScenarioSc03PreparationServicePhase7Test`** (nouvelle classe, 11 tests) :

* `horizon_null_leve_IllegalArgumentException` — T-P7-01
* `dateDebut_null_leve_IllegalArgumentException` — T-P7-02
* `dateFin_null_leve_IllegalArgumentException` — T-P7-03
* `dateDebut_apres_dateFin_leve_IllegalArgumentException` — T-P7-04
* `referentiels_null_leve_IllegalArgumentException` — T-P7-05
* `heureDebut_null_leve_IllegalArgumentException` — T-P7-06
* `heureFin_null_leve_IllegalArgumentException` — T-P7-07
* `referentiel_vide_neLevePasException_tousCreneauxExclus` — T-P7-08
* `codeActiviteIdEtActiviteDiscordants_codeActiviteIdPrime_pasDException` — T-P7-09
* `aucuneRessourceReelle_neLevePasException_creneauTransmis` — T-P7-10
* `requeteValide_comportementInchange` — T-P7-11 (non-régression)

#### Non-régression

Suite complète `fr.project.planning.scenarios.*` exécutée après les modifications : **BUILD SUCCESSFUL** (43s, 0 échec).

---

## Phase 8 — Activation ou retrait des champs partiels

### Objectif

Traiter les champs “partiels” du contrat d'entrée SC-03 — champs désérialisés et mappés, mais sans effet réel sur le solveur :

* activer ceux qui ont une valeur métier immédiate et un coût faible
* maintenir en transport ceux qui attendent une spécification ou une phase ultérieure
* documenter une trajectoire claire pour chaque champ

### Familles analysées

* Travail de nuit — champs salarié (`travailDeNuit`, `heureDebutNuit`, `heureFinNuit`)
* Jour férié — champs salarié et créneau
* Contraintes réglementaires salarié — 8 champs de `ContraintesReglementairesSalarie`
* Structuration des créneaux — `groupeBesoinId`, `blocJourId`, `ordreDansBloc`, `estSegmentDePause`

### Décisions par famille

#### Travail de nuit (salarié)

| Champ | Décision | Justification |
|---|---|---|
| `travailDeNuit` | **SUPPORTÉ** — déjà activé | Exploité par `NuitSalarieNonNuit` (SOFT) depuis Phase 8 implementation |
| `segmentNuit` (créneau) | **SUPPORTÉ** — déjà activé | `TypePlageHoraire` → utilisé par `NuitSalarieNonNuit` et `AlternanceJourNuit` |
| `heureDebutNuit` (salarié) | **Maintien en transport** | Méthode `heureDebutNuitEffective(fallback)` préparée sur `SalarieReel` mais aucune contrainte ne l'appelle. Activation conditionnée à la spécification de la relation avec `segmentNuit` : deux sources de vérité possibles pour la qualification de nuit — arbitrage métier requis |
| `heureFinNuit` (salarié) | **Maintien en transport** | Idem |

#### Jour férié

| Champ | Décision |
|---|---|
| `travailleJourFerie` (salarié) | **SUPPORTÉ** — `JourFerieRefuse` (HARD) actif |
| `isJourFerie` (créneau) | **SUPPORTÉ** — propagé dans `Creneau.jourFerie`, exploité par `JourFerieRefuse` |

#### Contraintes réglementaires salarié

| Champ | Décision | État |
|---|---|---|
| `joursConsecutifsMaximum` | **SUPPORTÉ** — `JoursConsecutifsMax` (SOFT) actif | ✅ |
| `amplitudeJournaliereMaximum` | **SUPPORTÉ** — `AmplitudeJournaliere` (SOFT) actif | ✅ |
| `nuitsMaximumParSemaine` | **INCERTAIN** — `NuitsConsecutivesMax` (HARD) enregistré, lecture requise pour confirmer si ce champ individuel est lu ou une limite globale | 🔍 |
| `heuresMaximumParJour` | **INCERTAIN** — `DureeMaximaleLegaleParSalarie` (HARD) enregistré, idem | 🔍 |
| `reposQuotidienMinimum` | **INCERTAIN** — `ReposObligatoireApresNuits` (HARD) enregistré, idem | 🔍 |
| `heuresMinimumParJour` | **Candidat à activation** — SOFT, pattern identique à `AmplitudeJournaliere` | ⏳ |
| `heuresMinimumParSemaine` | **Candidat à activation** — SOFT, même pattern | ⏳ |
| `heuresMaximumParSemaine` | **Candidat à activation** — SOFT, même pattern, conditionné à la clarification de `heuresMaximumParJour` | ⏳ |

#### Structuration des créneaux

| Champ | Décision | Justification |
|---|---|---|
| `estSegmentDePause` | **Activation prioritaire isolée** | Modification de filtres dans les contraintes existantes (`AmplitudeJournaliere`, `JoursConsecutifsMax`) pour exclure les pauses des calculs. Faible coût, valeur immédiate, ne touche ni le mapper ni le domaine |
| `groupeBesoinId` | **Maintien en transport — Phase 9+** | Contrainte de couverture de groupe (nouveau paradigme). Architecture plus complexe, à ne pas mélanger avec les contraintes salarié-créneau actuelles |
| `blocJourId` | **Maintien en transport — Phase 9+** | Idem — structuration intra-journalière |
| `ordreDansBloc` | **Maintien en transport — Phase 9+** | Idem |

### Décisions structurantes

* Ne pas activer `heureDebutNuit`/`heureFinNuit` sans arbitrage métier sur la relation avec `segmentNuit` (créneau). Activer sans spécification créerait deux sources de vérité pour la qualification de nuit.
* Ne pas lancer les contraintes de groupe/bloc en Phase 8 — paradigme distinct, complexité beaucoup plus élevée que les contraintes salarié-créneau.
* Privilégier des activations simples et isolées : une contrainte à la fois, avec tests dédiés.
* Clarifier les INCERTAIN avant d'activer les candidats — éviter de dupliquer une contrainte déjà existante.

### Priorités Phase 8

1. **Clarification des INCERTAIN** — lire `DureeMaximaleLegaleParSalarie`, `NuitsConsecutivesMax`, `ReposObligatoireApresNuits` et mettre à jour la documentation du contrat
2. **Activation de `estSegmentDePause`** — modification isolée des filtres dans les contraintes concernées + tests
3. **Activation séquentielle des contraintes simples** (`heuresMinimumParJour`, `heuresMinimumParSemaine`, `heuresMaximumParSemaine`) — une par une, conditionnées à l'étape 1
4. **Documentation** — mettre à jour `92_CONTRAT_ENTREE_SC03.md` : TOLÉRÉ → SUPPORTÉ pour les champs activés, note de trajectoire pour les champs maintenus

### Plan d'action

* Lecture des contraintes INCERTAIN (3 fichiers) → mise à jour immédiate du statut dans `92_CONTRAT_ENTREE_SC03.md`
* Activation isolée de `estSegmentDePause` dans les filtres de contraintes existantes + `ScenarioSc03PreparationServicePhase8Test` ou classe dédiée
* Activation séquentielle des trois contraintes heures min/max avec garde-fou null (contrainte inactive si champ non renseigné)
* Renforcement du signal documentaire sur `heureDebutNuit`/`heureFinNuit` : “transport préparé, activation conditionnée à l'arbitrage segmentNuit/plages individuelles”
* Trajectoire documentée pour `groupeBesoinId`, `blocJourId`, `ordreDansBloc` : “Phase 9+ — contraintes de bloc”

### Ce que la phase ne fait pas

* pas d'activation des plages de nuit individuelles salarié (`heureDebutNuit` / `heureFinNuit`)
* pas de contraintes de groupe ou de bloc
* pas de refonte du solveur ni du mapping
* pas de modification SC-01

### Critère de sortie

Chaque champ partiel du contrat SC-03 dispose d'un statut documenté :

* **SUPPORTÉ** — champ actif avec contrainte enregistrée
* **TOLÉRÉ** — maintenu en transport, trajectoire documentée
* Aucun champ ne reste dans une zone grise sans décision explicite

### État

✅ Terminé — 2026-03-25

### Réalisation

#### Étape 1 — Clarification des champs INCERTAIN

Audit factuel des trois contraintes signalées INCERTAIN. Aucune modification de comportement solveur.

| Champ | Contrainte auditée | Ce que la contrainte lit réellement | Décision |
|---|---|---|---|
| `heuresMaximumParJour` | `DureeMaximaleLegaleParSalarie` | Constante hardcodée `DUREE_MAX_LEGALE = 780` (13h par période de résolution) — champ individuel non lu | **TOLÉRÉ** — sémantique distincte (période vs journée) |
| `nuitsMaximumParSemaine` | `NuitsConsecutivesMax` | `context.getSeuilsDeTolerance().getMaxNuitsConsecutives()` — paramètre global `PlanningContext` — champ individuel non lu | **TOLÉRÉ** — seuil global non lié au champ individuel |
| `reposQuotidienMinimum` | `ReposObligatoireApresNuits` | `context.getSeuilsDeTolerance().getReposApresNuitsEnJours()` — paramètre global — sémantique "repos après nuits" ≠ "repos quotidien" | **TOLÉRÉ** — arbitrage sémantique requis |

Aucun test ajouté pour cette étape : la preuve est factuelle (lecture du code de contrainte).

#### Étape 2 — Activation de `estSegmentDePause`

`estSegmentDePause` était déjà mappé et disponible sur le domaine (`Creneau.getEstSegmentDePause(): Boolean`). Seul le filtre dans les contraintes manquait.

Contraintes modifiées (filtre `.filter(c -> !Boolean.TRUE.equals(c.getEstSegmentDePause()))` ajouté dans le join créneaux) :

| Contrainte | Impact sans filtre | Impact avec filtre [Phase 8] |
|---|---|---|
| `AmplitudeJournaliere` | Une pause avant/après le travail élargit incorrectement l'amplitude `[minDebut, maxFin]` | Amplitude calculée sur les créneaux de travail seuls |
| `JoursConsecutifsMax` | Un jour avec uniquement une pause compte comme "jour travaillé" | Un jour sans créneau de travail (pause seule) n'est pas compté |

Ni le mapper ni le domaine ne sont modifiés — activation strictement locale aux deux contraintes.

#### Étape 3 — Activation de `heuresMinimumParJour`

Nouvelle contrainte SOFT `HeuresMinimumParJour` créée, suivant le pattern exact de `AmplitudeJournaliere` :
- Inactive si `contraintesReglementaires.heuresMinimumParJour == null`
- Active par salarié, par journée calendaire (`creneau.getDate()`)
- Pauses exclues du total (cohérence avec l'étape 2)
- Pénalité : constante locale `PENALITE_HEURES_MIN_PAR_JOUR = 50` × minutes de déficit
- Enregistrée dans `ConstraintProviderImpl` et `PenaliteKey`

##### Hypothèse retenue — définition du "temps de travail effectif"

Le calcul du total journalier porte exclusivement sur les créneaux vérifiant **les deux conditions suivantes** :

1. `estSegmentDePause != true` — les pauses sont exclues ;
2. activité avec `compteDansCharge = true` dans le référentiel — seuls les créneaux qui comptent dans la charge sont pris en compte.

Conséquences documentées :
- Un créneau de type "formation" (`compteDansCharge = false`) n'entre pas dans le décompte, même s'il représente du temps passé au travail. Cette hypothèse est cohérente avec `AmplitudeJournaliere` et `JoursConsecutifsMax` qui appliquent le même filtre.
- Si le métier souhaite inclure les activités hors-charge dans le calcul du minimum journalier, cette hypothèse devra être révisée en Phase 9.
- La contrainte ne produit aucune pénalité pour les journées sans affectation de travail réel (pas de créneau non-pause + compteDansCharge = true) — ce n'est pas une contrainte de présence obligatoire.

`heuresMinimumParSemaine` et `heuresMaximumParSemaine` maintenus en transport : aucun pattern de groupement par semaine calendaire n'existe dans le projet, et la définition de "semaine" sur un horizon partiel nécessite un arbitrage explicite.

#### Tests ajoutés

**`EstSegmentDePauseConstraintsTest`** (nouvelle classe, 6 tests) :

| Test | Contrainte | Cas |
|---|---|---|
| `amplitude_pauseEncadrante_nElargitPasAmplitude` | `AmplitudeJournaliere` | Pause avant le travail exclue du calcul d'amplitude |
| `amplitude_sansPause_comportementInchange` | `AmplitudeJournaliere` | Pas de pause → pénalité inchangée (non-régression) |
| `amplitude_pauseSeule_pasDePenalite` | `AmplitudeJournaliere` | Seule une pause sur la journée → amplitude = 0 |
| `joursConsecutifs_pauseSeule_neComptesPasCommeJourTravaille` | `JoursConsecutifsMax` | Jour avec pause seule ne casse pas la séquence consécutive |
| `joursConsecutifs_pausePlusTravailMemeJour_jourBienCompte` | `JoursConsecutifsMax` | Pause + travail même jour → le jour est bien compté |
| `joursConsecutifs_sansPause_comportementInchange` | `JoursConsecutifsMax` | Pas de pause → comportement inchangé (non-régression) |

**`HeuresMinimumParJourConstraintsTest`** (nouvelle classe, 7 tests) :

| Test | Cas |
|---|---|
| `heuresMinimumParJour_null_contrainterInactive` | Seuil null → contrainte inactive |
| `contraintesReglementaires_null_contrainterInactive` | Bloc null → contrainte inactive |
| `totalEgalMinimum_pasDePenalite` | Total = seuil → 0 pénalité |
| `totalInferieurMinimum_pénalitéProportionnelleAuDeficit` | Total < seuil → pénalité × déficit |
| `totalSuperieurMinimum_pasDePenalite` | Total > seuil → 0 pénalité |
| `pauseExclue_deficitCalculeSansLaPause` | Pause exclue → déficit calculé sans elle |
| `sc01_salarieStandard_pasDePenalite` | Non-régression SC-01 |

#### Champs maintenus en transport

| Champ | Raison |
|---|---|
| `heureDebutNuit` / `heureFinNuit` | Deux sources de vérité possibles pour la qualification de nuit — arbitrage métier requis |
| `heuresMinimumParSemaine` | Pas de pattern per-semaine existant — définition de "semaine" sur horizon partiel non arbitrée |
| `heuresMaximumParSemaine` | Idem |
| `nuitsMaximumParSemaine` | Seuil global dans `NuitsConsecutivesMax` — arbitrage global/individuel requis |
| `heuresMaximumParJour` | `DureeMaximaleLegaleParSalarie` utilise un seuil global hardcodé — activation nécessite refonte de cette contrainte |
| `reposQuotidienMinimum` | Sémantique distincte de `ReposObligatoireApresNuits` — arbitrage requis |
| `groupeBesoinId` / `blocJourId` / `ordreDansBloc` | Phase 9+ — nouveau paradigme de contraintes de bloc |

#### Fichiers modifiés

| Fichier | Nature |
|---|---|
| `constraints/legales/AmplitudeJournaliere.java` | Filtre pause ajouté au join créneaux |
| `constraints/legales/JoursConsecutifsMax.java` | Filtre pause ajouté au join créneaux |
| `constraints/legales/HeuresMinimumParJour.java` | Nouvelle contrainte SOFT |
| `constraints/ConstraintProviderImpl.java` | Enregistrement `HeuresMinimumParJour` |
| `scoring/PenaliteKey.java` | `LEGAL_SOFT_HEURES_MIN_PAR_JOUR` ajouté |
| `constraints/EstSegmentDePauseConstraintsTest.java` | 6 tests |
| `constraints/HeuresMinimumParJourConstraintsTest.java` | 7 tests |
| `docs/92_CONTRAT_ENTREE_SC03.md` | Statuts mis à jour |

#### Non-régression

Tests ciblés écrits pour les deux contraintes modifiées et la nouvelle contrainte. Les cas sans pause et les cas SC-01 sont couverts explicitement.

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

✅ Terminé — 2026-03-25

### Réalisation

#### Mini-audit factuel

Revue complète champ par champ de `92_CONTRAT_ENTREE_SC03.md` par rapport au code réel.
Périmètre : créneaux, salariés, référentiel activités, postes virtuels, tolérances structurelles.

**3 écarts identifiés :**

| Écart | Nature | Correction |
|---|---|---|
| `referentiels.activites[].estServiceCritique` classé SUPPORTÉ | `isEstServiceCritique()` défini dans `ComptabiliteActivite` mais jamais appelé par aucune contrainte | SUPPORTÉ → TOLÉRÉ |
| `creneaux[].estSegmentDePause` description incomplète | `HeuresMinimumParJour` (Phase 8) manquait dans la liste des contraintes utilisant le filtre pause | Description complétée |
| `salaries[].axesOrganisationnels` et `contratTravail` : IGNORÉ sans signal | La légende IGNORÉ garantit un log.warn — aucun signal existait (couverture Phase 4 non étendue aux ressources salarié) | Signal ajouté (voir ci-dessous) |

**Champs vérifiés et confirmés exacts :** `type`, `priorite`, `axesOrganisationnels` créneaux, `segmentNuit`, `isJourFerie`, `travailDeNuit`, `travailleJourFerie`, `heureDebutNuit`/`heureFinNuit`, toutes les `contraintesReglementaires`, `compteDansCharge`, `genereDetteRepos`, `libelle`, `postesVirtuels.*`, fallback `codeActiviteId → activite`, valeurs par défaut silencieuses, `@JsonIgnoreProperties`.

#### Corrections documentaires (`92_CONTRAT_ENTREE_SC03.md`)

* `estServiceCritique` : SUPPORTÉ → **TOLÉRÉ** — description précisant l'absence de contrainte active
* `estSegmentDePause` : ajout de `HeuresMinimumParJour` dans le comportement
* `axesOrganisationnels` (salariés) : "sans signal actuellement" → "log.warn émis si valeur fournie (Phase 9)"
* `contratTravail` (salariés) : idem
* `libelle` (référentiel) : note "pas de log.warn — le mapper n'a pas de logger" → supprimée (le mapper a désormais un Logger) — décision de ne pas ajouter de log.warn sur ce champ de présentation maintenue
* Date du document : 2026-03-23 → 2026-03-25

#### Signal mineur ajouté (`ScenarioResourceMapper.java`)

Logger ajouté à `ScenarioResourceMapper`. Deux log.warn conditionnels dans `toSalarieReel()` :

| Champ | Signal | Condition |
|---|---|---|
| `axesOrganisationnels` (salarié) | `log.warn` — "champ axesOrganisationnels reçu et ignoré — aucune contrainte organisationnelle salarié implémentée" | objet non null |
| `contratTravail` (salarié) | `log.warn` — "champ contratTravail reçu et ignoré — non mappé au domaine SalarieReel" | objet non null |

Cohérent avec Phase 4 (même approche pour `axesOrganisationnels` et `priorite` créneaux).
Aucun comportement solveur modifié.

#### Tests ajoutés (`ScenarioResourceMapperTest`)

2 tests ajoutés :

* `toSalarieReel_avecAxesOrganisationnels_nAucunEffetSurLeMapping` — champ reçu, mapping nominal inchangé
* `toSalarieReel_avecContratTravail_nAucunEffetSurLeMapping` — champ reçu, mapping nominal inchangé

#### Fichiers modifiés

| Fichier | Nature |
|---|---|
| `scenarios/mapper/ScenarioResourceMapper.java` | Logger ajouté + 2 log.warn Phase 9 |
| `scenarios/mapper/ScenarioResourceMapperTest.java` | 2 tests Phase 9 |
| `docs/92_CONTRAT_ENTREE_SC03.md` | 5 corrections de statut / description |

#### Points laissés pour Phase 10

* `AlternanceJourNuit` utilise `creneau.getActivite()` sans fallback `codeActiviteId` — inconsistance interne, hors périmètre Phase 9
* `PosteVirtuel.type` inconnu → fallback POTENTIEL silencieux — TOLÉRÉ sans signal, signal optionnel Phase 10
* Suppression des `@JsonIgnoreProperties(ignoreUnknown = true)` des sous-DTOs (T-02)
* Simplification DTO, suppression des champs obsolètes

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

---

## Phase 10A — Incohérences internes + fallbacks silencieux

### Objectif

Corriger les incohérences internes de lecture du code activité dans les contraintes solveur, et rendre explicites les fallbacks silencieux du mapper.

### Mini-audit réalisé

| Sujet | Comportement avant | Risque | Correction |
|---|---|---|---|
| Activity lookup dans `AlternanceJourNuit`, `JoursConsecutifsMax`, `HeuresMinimumParJour`, `AmplitudeJournaliere` | `ref.getByCode(creneau.getActivite())` uniquement | SC-03 créneaux avec seulement `codeActiviteId` silencieusement exclus de ces contraintes | Fallback `codeActiviteId → activite` aligné sur `DetteReposSurReposHebdomadaire` / `PenibilitesLegalesMinutes` |
| `PosteVirtuel.type` inconnu | `catch (IllegalArgumentException ignored)` — silencieux | Valeur mal formée indétectable par les opérateurs | `log.warn` ajouté dans le catch (Logger déjà disponible depuis Phase 9) |

### Réalisation

#### Corrections contraintes (4 fichiers)

Pattern appliqué identique dans les 4 contraintes :

```java
// [Phase 10A] Fallback codeActiviteId → activite (cohérence avec le reste du moteur)
String codeActivite = (creneau.getCodeActiviteId() != null && !creneau.getCodeActiviteId().isBlank())
        ? creneau.getCodeActiviteId() : creneau.getActivite();
ComptabiliteActivite ca = ref.getByCode(codeActivite);
```

#### Signal PosteVirtuel.type inconnu

```java
log.warn("[ScenarioResourceMapper] poste virtuel id='{}' : type='{}' inconnu — fallback sur POTENTIEL",
        dto.getId(), dto.getType());
```

#### Impact SC-01

Aucun : les créneaux SC-01 ont `codeActiviteId = null` et `activite = "travail"`. Le fallback revient à `activite` — comportement identique.

#### Tests ajoutés

| Fichier | Tests |
|---|---|
| `constraints/CodeActiviteIdFallbackConstraintsTest.java` | 8 tests (4 contraintes × 2 : codeActiviteId seul + non-régression SC-01) |
| `scenarios/mapper/ScenarioResourceMapperTest.java` | 1 test Phase 10A (PosteVirtuel.type inconnu sans exception) |

### État

✅ Terminé 2026-03-25

---

## Phase 10B — Réduction progressive de `@JsonIgnoreProperties(ignoreUnknown = true)`

### Objectif

Réduire progressivement l'acceptation silencieuse des champs inconnus dans les DTO du contrat d'entrée. Sans big bang, sans casser l'API.

### Mini-audit réalisé

| DTO | Champs connus | Décision | Justification |
|---|---|---|---|
| `IndisponibilitesDTO` | 1 (`items`) | **A — STRICT** | Seul champ connu, bloc stable, tout champ inconnu est une erreur |
| `ReferentielsDTO` | 1 (`activites`) | **A — STRICT** | Idem |
| `Sc03ScenarioRequestDTO` | 4 | B — TOLÉRANT | Root DTO — WinDev pourrait envoyer `correlationId`, `version`, etc. ; aligner avec SC-01 en 10C après vérification |
| `Sc03ScenarioParametersDTO` | 2 | B — TOLÉRANT | Bloc non activé, en transition, évolution prévisible |
| `DataSetDTO` | 4 sous-blocs | B — TOLÉRANT | Conteneur — métadonnées WinDev possibles au niveau dataset |
| `PosteVirtuelInputDTO` | 6 | B — TOLÉRANT | Périmètre poste virtuel encore potentiellement évolutif |
| `SalarieInputDTO` | ~15 + `@JsonAlias` | C — après 10C | Champs IGNORÉ encore présents, aliases rétrocompat actifs |
| `CreneauInputDTO` | ~17 | C — après 10C | Phase 5 partiel, champs IGNORÉ au niveau mapping ; nettoyer d'abord |

DTOs déjà stricts sans annotation (aucun `@JsonIgnoreProperties`) : `RessourcesDTO`, `IndisponibiliteItemDTO`, `ReferentielActiviteDTO`, `AxesOrganisationnelsDTO`, `ContratTravailDTO`, `ContraintesReglementairesDTO`.

### Réalisation

#### DTO durcis (Catégorie A)

Retrait de `@JsonIgnoreProperties(ignoreUnknown = true)` de 2 DTOs :

```java
// IndisponibilitesDTO — avant :
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndisponibilitesDTO { ... }

// Après :
public class IndisponibilitesDTO { ... }
// [Phase 10B] bloc stabilisé à 1 champ (items) — champ inconnu = erreur de contrat

// ReferentielsDTO — même traitement
```

#### Note sur le comportement en production

Les DTO sans `@JsonIgnoreProperties(ignoreUnknown = true)` rejettent les champs inconnus **quand l'ObjectMapper est en mode strict**. Le comportement de production dépend de la configuration globale Jackson (Spring Boot). Si `spring.jackson.deserialization.fail-on-unknown-properties` est activé, ces blocs rejettent immédiatement. Sinon, le signal reste au niveau DTO — prêt à être activé.

#### Tests ajoutés

| Fichier | Tests |
|---|---|
| `scenarios/dto/StrictDeserializationPhase10BTest.java` | 6 tests : 2 × valid + 1 × items null + 1 × liste vide + 2 × champ inconnu échoue |

#### DTO laissés tolérants — justification explicite

| DTO | Raison de maintien de la tolérance |
|---|---|
| `Sc03ScenarioRequestDTO` | Root DTO : le plus risqué à durcir ; à aligner avec SC-01 (déjà strict) une fois l'interface WinDev confirmée |
| `Sc03ScenarioParametersDTO` | Bloc non encore activé, évolution prévisible (champs à venir) |
| `DataSetDTO` | Conteneur des 4 sous-blocs ; des métadonnées WinDev niveau dataset sont plausibles |
| `PosteVirtuelInputDTO` | Périmètre évolutif (activités, lieux, types) |
| `SalarieInputDTO` | `@JsonAlias` actifs pour rétrocompatibilité SC-01 ; champs IGNORÉ non encore nettoyés |
| `CreneauInputDTO` | Phase 5 non finalisée ; champs IGNORÉ (`type`, `priorite`) présents dans le DTO mais pas dans le domaine |

### État

✅ Terminé 2026-03-25

---

## Phase 10C — Nettoyage DTO final

### Objectif

Supprimer les champs DTO devenus définitivement obsolètes (IGNORÉ sans trajectoire), durcir les DTO mûrs, nettoyer le scénario de référence de ses métadonnées de migration.

### Mini-audit réalisé

| DTO | Champ / annotation | Décision | Justification |
|---|---|---|---|
| `CreneauInputDTO` | `axesOrganisationnels` | **Supprimé** | IGNORÉ — jamais mappé, aucune contrainte organisationnelle, aucune trajectoire |
| `CreneauInputDTO` | `priorite` | **Supprimé** | IGNORÉ — mismatch Integer/PrioriteCreneau, null transmis au domaine |
| `CreneauInputDTO` | `type` | **Supprimé** | TypeCreneau.IMPOSE toujours appliqué — valeur ignorée sans exception |
| `CreneauInputDTO` | `@JsonIgnoreProperties` | **Retiré** | DTO strict — SC-01 a `creneaux: []`, aucun risque |
| `SalarieInputDTO` | `axesOrganisationnels` | **Supprimé** | IGNORÉ — jamais mappé au domaine SalarieReel |
| `SalarieInputDTO` | `contratTravail` | **Supprimé** | IGNORÉ — jamais mappé au domaine SalarieReel |
| `SalarieInputDTO` | `@JsonIgnoreProperties` | **Conservé** | SC-01 envoie `type:"SALARIE"` et `capaciteCible` dans ses salariés |
| `SalarieInputDTO` | `@JsonAlias` × 2 | **Conservés** | SC-01 utilise `activitesAutorisees` et `lieuxAutorises` |
| `AxesOrganisationnelsDTO` | classe entière | **Supprimée** | Orpheline après nettoyage |
| `ContratTravailDTO` | classe entière | **Supprimée** | Orpheline après nettoyage |
| `Sc03ScenarioParametersDTO` | — | **Tolérant conservé** | 2 champs TOLÉRÉS, pas de champ mort, périmètre évolutif |
| `DataSetDTO`, `Sc03ScenarioRequestDTO`, `PosteVirtuelInputDTO` | — | **Tolérants conservés** | Root DTOs et conteneurs — tolérance justifiée |

### Réalisation

#### Suppression champs IGNORÉS (5 champs + 2 classes)

**`CreneauInputDTO`** — 3 champs supprimés + `@JsonIgnoreProperties` retiré :

* `axesOrganisationnels` (AxesOrganisationnelsDTO) — IGNORÉ depuis Phase 4
* `priorite` (Integer) — IGNORÉ depuis Phase 4 — mismatch Integer/PrioriteCreneau
* `type` (String) — valeur ignorée, TypeCreneau.IMPOSE toujours appliqué

**`SalarieInputDTO`** — 2 champs supprimés, `@JsonIgnoreProperties` et `@JsonAlias` conservés :

* `axesOrganisationnels` (AxesOrganisationnelsDTO) — IGNORÉ depuis Phase 9
* `contratTravail` (ContratTravailDTO) — IGNORÉ depuis Phase 9

**`AxesOrganisationnelsDTO`** — classe supprimée (orpheline).
**`ContratTravailDTO`** — classe supprimée (orpheline).

#### Nettoyage mappers

**`ScenarioCreneauMapper`** — 3 blocs `log.warn` supprimés (type, priorite, axesOrganisationnels) — les champs n'existant plus, les signaux n'ont plus de raison d'être.

**`ScenarioResourceMapper`** — 2 blocs `log.warn` supprimés (axesOrganisationnels salarié, contratTravail) — idem.

#### Nettoyage scénario de référence (`sc03_migration_reference.json`)

Champs supprimés du JSON de référence :

| Bloc | Champ | Motif |
|---|---|---|
| Racine | `_phasesMigration` | Métadonnée de migration obsolète |
| `referentiels` | `_phaseActivation` | Phase 10B debt — ReferentielsDTO strict |
| `indisponibilites` | `_phaseActivation`, `_commentaire` | Phase 10B debt — IndisponibilitesDTO strict |
| `creneaux[]` (6) | `priorite`, `type`, `axesOrganisationnels` | Champs retirés du DTO |
| `creneaux[]` (3) | `_commentaire` | CreneauInputDTO désormais strict |
| `salaries[]` (2) | `axesOrganisationnels`, `contratTravail`, `_commentaire` | Champs retirés + JSON propre |
| `postesVirtuels[]` (1) | `_commentaire` | JSON propre |

Le scénario de référence reste fonctionnellement identique : les mêmes 2 salariés, 6 créneaux (dont jour férié, nuit, dimanche), 1 poste virtuel, 1 indisponibilité.

#### Tests ajoutés

**`StrictDeserializationPhase10CTest`** (nouvelle classe, 5 tests) :

* `creneauInputDTO_jsonValide_doitDeserializerCorrectement` — JSON valide sans les champs supprimés → désérialisation correcte
* `creneauInputDTO_champType_doitEchouerExplicitement` — `type` → `UnrecognizedPropertyException`
* `creneauInputDTO_champPriorite_doitEchouerExplicitement` — `priorite` → `UnrecognizedPropertyException`
* `creneauInputDTO_champAxesOrganisationnels_doitEchouerExplicitement` — `axesOrganisationnels` → `UnrecognizedPropertyException`
* `creneauInputDTO_champActiviteDeprecated_doitEncoreEtreAccepte` — `activite` (⚠️ DÉPRÉCIÉ) → accepté

#### Tests supprimés

**`ScenarioCreneauMapperTest`** — 5 tests Phase 1/4 sur les champs retirés :

* `toCreneau_typeIgnore_imoseApplique_quelqueSoitLaValeurDTO`
* `toCreneau_typeNull_imoseApplique`
* `toCreneau_prioriteNonNull_transmisNullAuDomaine`
* `toCreneau_axesOrganisationnelsNonNull_nAucunEffetSurLeMapping`
* `toCreneau_prioriteEtAxesNull_champsIgnoresComportementInchange`

Remplacé par : `toCreneau_typeDomaine_estToujoursIMPOSE` — prouve que TypeCreneau.IMPOSE reste le comportement câblé.

**`ScenarioResourceMapperTest`** — 2 tests Phase 9 sur les champs retirés :

* `toSalarieReel_avecAxesOrganisationnels_nAucunEffetSurLeMapping`
* `toSalarieReel_avecContratTravail_nAucunEffetSurLeMapping`

#### Fichiers modifiés

| Fichier | Nature |
|---|---|
| `scenarios/dto/input/CreneauInputDTO.java` | 3 champs supprimés + `@JsonIgnoreProperties` retiré |
| `scenarios/dto/input/SalarieInputDTO.java` | 2 champs supprimés |
| `scenarios/dto/input/AxesOrganisationnelsDTO.java` | **Supprimé** |
| `scenarios/dto/input/ContratTravailDTO.java` | **Supprimé** |
| `scenarios/mapper/ScenarioCreneauMapper.java` | 3 blocs log.warn supprimés |
| `scenarios/mapper/ScenarioResourceMapper.java` | 2 blocs log.warn supprimés |
| `scenarios/mapper/ScenarioCreneauMapperTest.java` | 5 tests supprimés + 1 ajouté |
| `scenarios/mapper/ScenarioResourceMapperTest.java` | 2 tests supprimés |
| `scenarios/dto/StrictDeserializationPhase10CTest.java` | **Nouveau** — 5 tests |
| `scenarios/sc03/sc03_migration_reference.json` | Nettoyage complet métadonnées et champs retirés |
| `scenarios/sc03/Sc03DatasetIntegrityTest.java` | Section 8 supprimée — 2 tests `axesOrganisationnels` obsolètes (champ retiré du DTO) |
| `docs/92_CONTRAT_ENTREE_SC03.md` | Champs retirés supprimés, statuts mis à jour, note Phase 10C |

#### Points délibérément hors périmètre

* `SalarieInputDTO.@JsonIgnoreProperties` conservé — SC-01 envoie des champs inconnus dans ses salariés
* `activite` conservé dans `CreneauInputDTO` — fallback ⚠️ DÉPRÉCIÉ encore actif
* `Sc03ScenarioRequestDTO`, `DataSetDTO`, `Sc03ScenarioParametersDTO`, `PosteVirtuelInputDTO` : tolérants conservés — trajectoires justifiées
* Aucune contrainte solveur touchée
* Aucun scoring modifié

#### Non-régression

* `sc03_migration_reference.json` nettoyé reste fonctionnellement équivalent — mêmes situations réglementaires couvertes
* Tests `StrictDeserializationPhase10CTest` + non-régression de `ScenarioCreneauMapperTest` + `ScenarioResourceMapperTest`

### État

✅ Terminé 2026-03-26

---

## Suivi global

| Phase    | Statut |
| -------- | ------ |
| Phase 1  | ✅      |
| Phase 2  | ✅      |
| Phase 3  | ✅      |
| Phase 4  | ✅      |
| Phase 5  | ✅      |
| Phase 6  | ✅      |
| Phase 7  | ✅      |
| Phase 8  | ✅      |
| Phase 9  | ✅      |
| Phase 10A | ✅      |
| Phase 10B | ✅      |
| Phase 10C | ✅      |

---

## Point d’attention

Le risque principal n’est pas technique mais sémantique :

> faire évoluer le contrat sans savoir précisément ce qui est réellement utilisé.

Ce document doit donc rester aligné en permanence avec :

* l’audit,
* les tests,
* le comportement réel du moteur.
