# Plan de migration temporaire — Alimentation WinDev → moteur

## Objectif du document

Ce document sert de **pilote de migration temporaire** pour faire évoluer le contrat d’entrée du moteur sans casser le socle existant.

Il doit permettre de :
- introduire progressivement les nouveaux champs transmis par WinDev ;
- sécuriser chaque étape par des tests existants ou à créer ;
- garder SC-01 opérationnel pendant la transition ;
- préparer l’ouverture vers SC-03 puis les autres scénarios ;
- tracer clairement ce qui est **transporté**, **mappé**, **utilisé par le builder**, puis **exploité par le solveur**.

---

## Principes de migration

### 1. Ne pas tout activer d’un coup
Chaque nouveauté doit passer par 4 niveaux distincts :
1. **transport DTO / JSON** ;
2. **validation / schéma** ;
3. **mapping builder → domaine** ;
4. **usage solveur / scoring / WorkMetrics**.

### 2. Tolérance transitoire sur le référentiel d’activités

Pendant la phase de migration et de développement :
- une activité absente du référentiel peut être tolérée ;
- le créneau concerné est alors traité comme neutre du point de vue des calculs dépendant du référentiel ;
- un diagnostic ou un signalement peut être produit afin de rendre cette situation visible.

Cette tolérance est **transitoire**.
Elle ne constitue pas une règle cible du moteur.

À terme, le contrat d’entrée devra permettre :
- soit le rejet explicite d’une activité inconnue ;
- soit l’application d’une politique de défaut documentée et maîtrisée.

### 3. Préserver le socle existant
Aucune étape ne doit casser :
- la désérialisation SC-01 ;
- l’exécution solveur déjà validée ;
- le contrat de sortie `ScenarioResponseDTO` ;
- les tests de stabilité du score et des affectations.

### 4. Tolérance contractuelle provisoire
Pendant la migration :
- les nouveaux champs peuvent être transportés avant d’être utilisés ;
- certains champs peuvent être tolérés sans effet immédiat côté solveur ;
- les redondances temporaires sont acceptées uniquement si elles sont documentées.

### 5. Nettoyage différé mais planifié
Un champ redondant ne doit pas devenir définitif.
Chaque redondance temporaire doit être associée à une étape de suppression cible.

---

## Nouveaux blocs à intégrer côté entrée

### Ressources salariées
Champs déjà identifiés :
- `axesOrganisationnels`
- `contratTravail`
- `contraintesReglementaires`
- `travailDeNuit` : `null | permanent | occasionnel`
- `heureDebutNuit`
- `heureFinNuit`
- `travailleJourFerie`

### Créneaux / besoins
Champs déjà identifiés :
- `groupeBesoinId`
- `blocJourId`
- `ordreDansBloc`
- `estSegmentDePause`

### Données globales
Blocs déjà identifiés :
- `referentiels`
- `indisponibilites`
- paramètres réglementaires globaux

---

## Décisions de modélisation transitoires

### A. Nuit globale vs nuit par salarié
Pendant la migration, on distingue :
- la **plage de nuit réglementaire globale** portée par le contexte ;
- la **qualification RH / indemnitaire du salarié** portée par la ressource.

Conséquence :
- `heureDebutNuit` / `heureFinNuit` au niveau salarié ne remplacent pas immédiatement les `RegulatoryParameters` globaux ;
- ils sont d’abord transportés, puis exploités dans un second temps pour les pénalités ou règles spécifiques au salarié.

### B. Travail jour férié autorisé ou non
Le booléen `travailleJourFerie` doit à terme devenir une règle de compatibilité.

Phase transitoire :
- champ transporté ;
- builder capable de le mapper ;
- règle solveur activée ensuite.

### C. Statut de travail de nuit
Le champ `travailDeNuit` ne doit pas être interprété trop tôt.

Phase transitoire :
- stockage sur la ressource ;
- pas d’impact immédiat sur le score ;
- intégration ultérieure dans les règles de pénibilité / scoring / diagnostics.

### D. Durée, libellés, champs redondants
Sont considérés comme temporaires ou secondaires :
- `activite` comme libellé si `codeActiviteId` existe ;
- `sitesAutorises` si `axesOrganisationnels.lieuIds` devient la source de vérité.

> `duree` sur le créneau : considérée à l'origine comme recalculable, cette décision a été inversée. `Creneau.duree` est la **source de vérité** pour tous les agrégats et la restitution — elle n'est jamais recalculée depuis `heureDebut`/`heureFin`. Voir `20_DECISIONS_CONCEPTION_OPTAPLANNER.md`.

---

## Plan de progression détaillé

## Phase 0 — Gel du point de départ

### Objectif
Photographier l’existant avant toute évolution du contrat d’entrée.

### Travaux
- [x] identifier les DTO réellement utilisés par l’API — documenté dans `60_interface_windev_moteur_plan_documentaire.md` §8.7 ;
- [x] lister les tests de non-régression déjà en place — documenté dans `60_interface_windev_moteur_plan_documentaire.md` §7 ;
- [x] figer un JSON de référence SC-01 — `src/test/resources/scenarios/sc01/sc01_dataset_reference.json` ;
- [x] figer un JSON cible SC-03 / technique de migration — `docs/Windev_part/DataStructure/sc_03_migration_reference.json` ;
- [x] noter les champs déjà documentés mais non supportés dans le code — `contraintesReglementaires` (§8.7.4), `creneaux` ignoré dans DataSetDTO, couplage DTO/domaine (§3.1).

### Tests à exécuter
- `ScenarioControllerRuntimeTest`
- `ScenarioControllerValidationTest`
- `PlanningServiceSolverStabilityTest`
- `PlanningServiceScoreRegressionTest`
- `./gradlew test`

### Critère de sortie
Le socle actuel est vert avant toute modification.

### État au 2026-03-13
Tous les travaux documentaires sont réalisés. Suite de tests exécutée : **BUILD SUCCESSFUL** (47s, 0 échec). **Phase 0 TERMINÉE — critère de sortie validé.**

---

## Phase 1 — Stabiliser la couche transport

### Objectif
Permettre au moteur de **recevoir** les nouveaux champs sans les exploiter encore.

### Travaux
- faire évoluer `ScenarioRequestDTO` pour sortir du verrou SC-01 strict ;
- étendre `DataSetDTO` pour supporter au minimum :
  - `creneaux`
  - `referentiels`
  - `indisponibilites`
- étendre les DTO de ressources salariées pour supporter :
  - `axesOrganisationnels`
  - `contratTravail`
  - `contraintesReglementaires`
  - `travailDeNuit`
  - `heureDebutNuit`
  - `heureFinNuit`
  - `travailleJourFerie`
- étendre les DTO de créneaux pour supporter :
  - `groupeBesoinId`
  - `blocJourId`
  - `ordreDansBloc`
  - `estSegmentDePause`

### Règle importante
À ce stade, ces champs peuvent être :
- désérialisés ;
- validés minimalement ;
- ignorés par le builder si nécessaire.

### Tests à créer
- test de désérialisation SC-03 enrichi ;
- test de désérialisation d’un salarié avec les 4 nouveaux champs nuit/férié ;
- test de désérialisation d’un créneau structuré (`blocJourId`, etc.).

### Tests à rejouer
- tous les tests d’API existants ;
- tous les tests solveur existants.

### Critère de sortie
Le moteur accepte le JSON enrichi sans casser SC-01.

### État au 2026-03-13
Tous les travaux réalisés. Suite de tests exécutée : **BUILD SUCCESSFUL** (39s, 0 échec). **Phase 1 TERMINÉE — critère de sortie validé.**

---

## Phase 2 — Formaliser un dataset technique de référence

### Objectif
Créer le jeu de test canonique de migration WinDev → moteur.

### Contenu minimal du dataset
- 1 direction ;
- 1 service ;
- 1 lieu ;
- 1 poste comptable ;
- 2 salariés ;
- 1 poste virtuel ;
- 1 semaine complète ;
- au moins un créneau jour ;
- un créneau soirée ;
- un créneau nuit ;
- un mercredi férié ;
- un dimanche travaillé ;
- des indisponibilités ;
- des besoins structurés (`groupeBesoinId`, `blocJourId`) ;
- des champs nuit/férié par salarié.

### Fichier de référence
Le dataset cible est disponible à :
`docs/Windev_part/DataStructure/sc_03_migration_reference.json`

Il couvre toutes les situations listées ci-dessus. Les champs `referentiels`, `indisponibilites`, `travailDeNuit`, `heureDebutNuit`, `heureFinNuit` et `travailleJourFerie` sont présents dans le fichier mais marqués `_phaseActivation: Phase 1` — ils seront activés en Phase 1.

En Phase 2, ce fichier sera copié vers `src/test/resources/scenarios/sc03/sc03_migration_reference.json` pour servir de base aux tests automatisés.

### But
Ce dataset devient la base commune pour :
- les tests JSON ;
- les tests builder ;
- les futurs tests SC-03 ;
- la documentation transverse.

### Tests à créer
- test de chargement complet du dataset technique ;
- test d’intégrité minimale (ids, axes, activités, cohérence de base).

### Critère de sortie
Un dataset unique sert de référence de migration.

### État au 2026-03-14
Dataset complet et copié en Phase 1. Suite de tests exécutée : **BUILD SUCCESSFUL** (47s, 0 échec). **Phase 2 TERMINÉE — critère de sortie validé.**

---

## Phase 3 — Brancher les nouveaux champs dans le builder

### État : TERMINÉE (2026-03-14)

Refactoring architectural complet + mapping Phase 3 réalisés. Suite de tests exécutée : **BUILD SUCCESSFUL** (38s, 0 échec). **Phase 3 TERMINÉE — critère de sortie validé.**

### Objectif
Faire en sorte que les nouveaux champs ne soient plus seulement transportés, mais réellement mappés vers le monde solveur.

### Travaux
- mapper `axesOrganisationnels` ;
- mapper `contratTravail` ;
- mapper `contraintesReglementaires` ;
- mapper `indisponibilites` ;
- transporter les marqueurs de structuration des besoins ;
- stocker sur la ressource les champs spécifiques :
  - `travailDeNuit`
  - `heureDebutNuit`
  - `heureFinNuit`
  - `travailleJourFerie`

### À ce stade
- les champs existent dans le monde solveur ;
- certaines contraintes peuvent encore ne pas les utiliser.

### Tests à créer
- test builder → domaine pour salarié enrichi ;
- test builder → domaine pour créneau structuré ;
- test builder → domaine pour indisponibilité ;
- test builder garantissant que les nouveaux champs n’altèrent pas SC-01 si absents.

### Critère de sortie
Le builder devient la traduction officielle du nouveau contrat d’entrée.

---

## Phase 4 — Introduire les incompatibilités structurelles simples

### État : TERMINÉE (2026-03-14)

Deux contraintes HARD introduites (`JourFerieRefuse`, `IndisponibiliteSalarie`). `optaplanner-test` ajouté. Suite de tests exécutée : **BUILD SUCCESSFUL (37s, 0 échec)**. **Phase 4 TERMINÉE — critère de sortie validé.**

### Objectif
Exploiter les nouveaux champs dans des règles simples, sans encore toucher au scoring fin.

### Champs activés dans cette phase (matrice d’exploitation)
Les champs suivants progressent de **Mappé domaine** vers **Exploité solveur** en Phase 4 :

| Champ | Niveau actuel (Phase 3) | Niveau cible (Phase 4) |
|-------|------------------------|------------------------|
| `ressources.salaries.travailleJourFerie` | Mappé domaine ✅ | Exploité solveur |
| `dataSet.indisponibilites` | Mappé domaine ✅ | Exploité solveur |

⚠️ `ressources.salaries.axesOrganisationnels` est hors périmètre Phase 4 : il est uniquement **Transporté** (pas encore mappé vers le domaine — SalarieReel ne porte pas ce bloc). Son activation est reportée en Phase 5, après mapping domaine préalable.

### Règles visées
- un salarié avec `travailleJourFerie = false` ne peut pas être affecté sur un jour férié ;
- une indisponibilité interdit l’affectation du salarié sur l’intervalle concerné.

### Nature recommandée
- contrainte **HARD** — ce sont de vraies interdictions métier et légales ;
- alternative : filtrage du value range si cela simplifie le solveur sans perte d’explicabilité.

### Tests à créer
- salarié refusé sur jour férié (`travailleJourFerie = false`) ;
- salarié autorisé sur jour férié (`travailleJourFerie = true`) ;
- affectation impossible à cause d’une indisponibilité ;
- scénario de non-régression SC-01 inchangé sans ces champs.

### Critère de sortie
Les nouveaux champs commencent à avoir un effet métier observable et prouvé par les tests.

---

## Phase 5 — Structurer les besoins et blocs journaliers

### État : TERMINÉE (2026-03-14)

Mapping domaine des 4 champs de structuration. `ScenarioCreneauMapper` créé. Suite de tests exécutée : **BUILD SUCCESSFUL (50s, 0 échec)**. **Phase 5 TERMINÉE — critère de sortie validé.**

### Objectif
Préparer le moteur à raisonner sur des groupes logiques de créneaux sans changer la variable de décision principale.

### Travaux
- mapper `groupeBesoinId` vers le domaine `Creneau` ;
- mapper `blocJourId` vers le domaine `Creneau` ;
- mapper `ordreDansBloc` vers le domaine `Creneau` ;
- mapper `estSegmentDePause` vers le domaine `Creneau` ;
- préparer les futures contraintes de continuité / fragmentation / amplitude d’une journée.

### Attention
À cette phase, il n’est pas obligatoire d’implémenter immédiatement toutes les contraintes combinatoires associées.
Le premier objectif est de rendre la structure exploitable.

### Tests à créer
- regroupement stable des créneaux par bloc ;
- ordre conservé dans le builder ;
- segment de pause correctement identifié ;
- absence de régression sur les métriques existantes.

### Critère de sortie
Le moteur sait recevoir et porter la structuration métier des besoins.

---

## Phase 6 — Exploiter les données nuit par salarié

### État : TERMINÉE (2026-03-14)

Méthodes utilitaires nuit ajoutées à `SalarieReel`. Suite de tests exécutée : **BUILD SUCCESSFUL (40s, 0 échec)**. **Phase 6 TERMINÉE — critère de sortie validé.**

### Objectif
Préparer la future prise en compte du travail de nuit selon le profil salarié.

### Données à exploiter
- `travailDeNuit = null | permanent | occasionnel`
- `heureDebutNuit`
- `heureFinNuit`

### Décision d’évolution
Cette phase ne doit pas casser le calcul global existant basé sur `RegulatoryParameters`.

Stratégie recommandée :
- conserver la mesure globale actuelle pour la stabilité ;
- ajouter une couche d’interprétation ressource pour les futures pénalités spécifiques.

### Cas d’usage futurs préparés
- pénalité différente selon salarié de nuit permanent / occasionnel ;
- calcul de pénibilité RH spécifique ;
- diagnostics plus précis.

### Tests à créer
- transport et mapping du statut nuit ;
- transport et mapping des heures nuit salarié ;
- non-régression sur les calculs actuels `TimeBreakdownCalculator`.

### Critère de sortie
Les informations de nuit par salarié sont disponibles et testées, sans régression sur le calcul global existant.

---

## Phase 7 — Étendre SC-03 côté API

### État : TERMINÉE (2026-03-14)

`Sc03ScenarioRequestDTO`, `ScenarioSc03PreparationService`, `ScenarioSc03ExecutionService`, endpoint `/sc03/solve` créés. `toReferentiel()` branché dans `ScenarioResourceMapper`. Suite de tests exécutée : **BUILD SUCCESSFUL (41s, 0 échec)**. **Phase 7 TERMINÉE — critère de sortie validé.**

### Objectif
Sortir du mode “SC-01 seulement” et ouvrir le vrai scénario cible de couverture locale.

### Travaux
- introduire `Sc03ScenarioParametersDTO` ;
- formaliser `prioriteCouverture` ;
- exposer l’endpoint `/sc03/solve` ;
- brancher `ScenarioCreneauMapper` pour les créneaux SC-03 ;
- brancher le référentiel JSON via `ScenarioResourceMapper.toReferentiel()`.

### Tests créés
- `ScenarioControllerSc03ValidationTest` (4 tests négatifs : scenarioType invalide, dataSet absent, creneaux vides, JSON invalide) ;
- `ScenarioControllerSc03RuntimeTest` (1 test runtime : dataset de référence SC-03, score hard=0).

### Critère de sortie
Le chantier sort du simple test technique et devient un vrai scénario supporté.

---

## Phase 8 — Ajuster scoring, WorkMetrics et explicabilité

### État : TERMINÉE (2026-03-14)

Contrainte SOFT `NuitSalarieNonNuit`, diagnostic `JOUR_FERIE_NON_COUVERT`, métrique `nbCreneauxNuitNonNuit`. Suite de tests exécutée : **BUILD SUCCESSFUL (44s, 0 échec)**. **Phase 8 TERMINÉE — critère de sortie validé.**

### Objectif
Faire évoluer la lecture métier du planning à partir des nouveaux champs transmis par WinDev.

### Réalisations
- contrainte SOFT `NuitSalarieNonNuit` : pénalise l’affectation d’un créneau de nuit à un salarié non déclaré travailleur de nuit — exploite `travailDeNuit` via `SalarieReel.estTravailleurDeNuit()` (Phase 6)
- diagnostic enrichi `JOUR_FERIE_NON_COUVERT` dans `AssignmentDiagnosticsFactory` : reasonCode précis quand un créneau férié est non couvert
- métrique WorkMetrics `nbCreneauxNuitNonNuit` : comptabilise les inadéquations nuit par salarié dans `WorkMetrics`, `WorkMetricsCalculator`, `WorkMetricsByRessourceDTO`

### Tests créés
- `Phase8ConstraintsTest` (7 tests `ConstraintVerifier`)
- `AssignmentDiagnosticsFactoryTest` (4 tests unitaires)
- `WorkMetricsNuitNonNuitTest` (5 tests unitaires)

### Critère de sortie
Les nouveaux champs ont un impact visible dans la restitution, pas seulement dans l’entrée.

---

## Phase 9 — Consolidation pipeline SC-03 et diagnostics complets

### État : TERMINÉE (2026-03-18)

Pipeline SC-03 end-to-end validé. `ignoredCreneaux` réel implémenté. Null éliminé de la réponse. Code debug supprimé. Suite de tests exécutée : **BUILD SUCCESSFUL (1m 25s, 0 échec)**. **Phase 9 TERMINÉE — critère de sortie validé.**

### Objectif
Consolider le pipeline complet WinDev → API → Solver → ResponseDTO et implémenter les diagnostics pré-résolution `ignoredCreneaux`.

### Réalisations

#### 9a — Suppression du code debug `PlanningService`
- Suppression du bloc de 36 lignes qui forçait tous les créneaux sur la ressource "1041" (diagnostic temporaire des contraintes HARD)
- `solve()` est maintenant propre : `PlanningProblem` → `solverLauncher.solve()` → `solutionManager.explain()` → `PlanningResponse`

#### 9b — Finalisation `ScenarioResponseMapper`
- `toResponse()` accepte `IgnoredCreneauxDTO` en 11e argument (supprime le `new IgnoredCreneauxDTO(0,0,0)` hardcodé)
- `buildDiagnostics()` utilise l’objet transmis (avec fallback null-safe vers `0,0,0`)
- `toCreneauPlanningDTO()` : null-safety garantie — retourne `RessourceNonAffectee.INSTANCE.getId()` (`"A_AFFECTER"`) si `getRessourceAffectee() == null`

#### 9c — Implémentation `ignoredCreneaux` dans la chaîne SC-03
- `PreparedSc03Scenario` : ajout du 4e champ `IgnoredCreneauxDTO ignoredCreneaux`
- `ScenarioSc03PreparationService` : calcul pré-résolution de `horsHorizon` (date hors [dateDebut, dateFin]) et `activiteInconnue` (code absent du référentiel) ; les deux compteurs sont produits avant `planningService.solve()`
- `ScenarioSc03ExecutionService` : transmission `prepared.ignoredCreneaux()` au mapper
- `ScenarioSc01ExecutionService` : ajout `new IgnoredCreneauxDTO(0, 0, 0)` comme 11e argument (SC-01 ne filtre pas de créneaux en pré-résolution)

#### 9d — Tests d’intégration `Phase9IntegrationTest`
- `sc03_creneauHorsHorizon_doitComptabiliserHorsHorizon` : horsHorizon=1, activiteInconnue=0
- `sc03_activiteInconnue_doitComptabiliserActiviteInconnue` : activiteInconnue=1, horsHorizon=0
- `sc03_referenceDataset_ressourceAffecteeId_jamaisNull` : tous les `ressourceAffecteeId` non null dans `planning.jours`
- `sc03_referenceDataset_tousLesBlocs_presents` : présence des 5 blocs, hard=0, nbCreneaux=6, horsHorizon=0, activiteInconnue=0

Nouveaux datasets de test :
- `src/test/resources/scenarios/sc03/sc03_hors_horizon.json`
- `src/test/resources/scenarios/sc03/sc03_activite_inconnue.json`

### Critère de sortie
Pipeline SC-03 end-to-end validé : `ignoredCreneaux` réel, `ressourceAffecteeId` jamais null, tous les blocs présents, hard=0.

---

## Matrice d'exploitation des champs

Suivi du niveau réel d'exploitation de chaque champ enrichi depuis la Phase 2.
Mise à jour à chaque itération de migration.

**Légende des niveaux** (définis dans `90_suivi_developpement_moteur.md`) :

| Niveau | Signification |
|--------|---------------|
| **Transporté** | Présent dans le JSON et désérialisé dans les DTO |
| **Validé** | Contrôlé par le controller ou une validation dédiée |
| **Mappé domaine** | Converti vers un objet métier |
| **Exploité solveur** | Utilisé par au moins une contrainte du solveur |
| **Exploité scoring** | Influence le score ou une pénalité |
| **Exploité diagnostics** | Utilisé pour produire des diagnostics ou alertes |
| **Exploité WorkMetrics** | Impact visible dans les métriques calculées |
| **Testé** | Couvert par au moins un test prouvant l'exploitation |

Un champ est **fonctionnellement actif** uniquement à partir de : Transporté → Mappé domaine → Exploité solveur/scoring/diagnostics.

---

### Bloc : `ressources.salaries`

| Champ | Transporté | Validé | Mappé domaine | Exploité solveur | Exploité scoring | Exploité diagnostics | Exploité WorkMetrics | Testé | Phase cible | Source de vérité (Phase 9) |
|-------|:----------:|:------:|:-------------:|:----------------:|:----------------:|:--------------------:|:--------------------:|:-----:|-------------|---------------------------|
| `id` | ✅ | — | ✅ | ✅ | — | — | — | ✅ | *(actif)* | — (stable) |
| `statut` | ✅ | — | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | Phase 8 | — (stable) |
| `sitesAutorises` | ✅ | — | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | Phase 4/5 | `axesOrganisationnels.lieuIds` |
| `activitesCompatibles` | ✅ | — | ✅ | ❌ | ❌ | ✅ | ❌ | ✅ | Phase 4/5 | — (stable) |
| `postesComptablesCompatibles` | ✅ | — | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | Phase 4/5 | `axesOrganisationnels.posteComptableIds` |
| `travailleJourFerie` | ✅ | — | ✅ | ✅ | ❌ | ✅ | ❌ | ✅ | *(actif)* | — (stable) |
| `travailDeNuit` | ✅ | — | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | *(actif)* | — (stable) |
| `heureDebutNuit` | ✅ | — | ✅ | ❌ | ❌ | ✅ | ❌ | ✅ | Phase 9+ | — (stable) |
| `heureFinNuit` | ✅ | — | ✅ | ❌ | ❌ | ✅ | ❌ | ✅ | Phase 9+ | — (stable) |
| `contraintesReglementaires` | ✅ | — | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ | *(partiel)* | — (remplace `RegulatoryParameters` globaux) |
| `axesOrganisationnels` | ✅ | — | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | Phase 4/5 | — (cible, absorbe les redondances) |
| `contratTravail` | ✅ | — | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | Phase 8 | — (stable) |

> `contraintesReglementaires` : mappé vers `ContraintesReglementairesSalarie` sur `SalarieReel`. Partiellement exploité depuis Phase 10/12 : `joursConsecutifsMaximum` (contrainte `JoursConsecutifsMax`) et `amplitudeJournaliereMaximum` (contrainte `AmplitudeJournaliere`). Les autres champs (`heuresMinimumParJour`, `heuresMaximumParJour`, `reposQuotidienMinimum`, `heuresMinimumParSemaine`, `heuresMaximumParSemaine`, `nuitsMaximumParSemaine`) restent mappés mais non exploités par le solveur — les `RegulatoryParameters` globaux restent la source de vérité pour ces axes.
> `axesOrganisationnels` : transporté dans `SalarieInputDTO`, mais `SalarieReel` ne porte pas ce bloc — aucun mapping domaine réalisé.
> `travailDeNuit` / `heureDebutNuit` / `heureFinNuit` : "Exploité diagnostics ✅" — Phase 6 ajoute `estTravailleurDeNuit()`, `heureDebutNuitEffective(fallback)`, `heureFinNuitEffective(fallback)` sur `SalarieReel`. Ces méthodes préparent les diagnostics et pénalités Phase 8 sans modifier le solveur ni `RegulatoryParameters`.
> `activitesCompatibles` : "Exploité diagnostics ✅" depuis la Phase 12 — lu par `ScenarioSc03PreparationService.auMoinsUneRessourceCompatible()` pour le calcul de `ignoredCreneaux.aucuneRessourceDansDataset`. Une liste vide/nulle est considérée comme non contrainte (peut couvrir toute activité). Voir `20_dataset_builder.md §5.4.2B`.

---

### Bloc : `ressources.postesVirtuels`

| Champ | Transporté | Validé | Mappé domaine | Exploité solveur | Exploité scoring | Exploité diagnostics | Exploité WorkMetrics | Testé | Phase cible | Source de vérité (Phase 9) |
|-------|:----------:|:------:|:-------------:|:----------------:|:----------------:|:--------------------:|:--------------------:|:-----:|-------------|---------------------------|
| `id` | ✅ | — | ✅ | ✅ | — | — | — | ✅ | *(actif)* | — (stable) |
| `type` | ✅ | — | ✅ | ✅ | ✅ | — | — | ✅ | *(actif)* | — (stable) |
| `capaciteCible` | ✅ | — | ✅ | ✅ | — | — | — | ✅ | *(actif)* | — (stable) |
| `activitesAutorisees` | ✅ | — | ✅ | ❌ | ❌ | ✅ | ❌ | ✅ | Phase 4/5 | — (stable) |
| `lieuxAutorises` | ✅ | — | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | Phase 4/5 | `axesOrganisationnels.lieuIds` |
| `postesComptablesCompatibles` | ✅ | — | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | Phase 4/5 | `axesOrganisationnels.posteComptableIds` |

> `activitesAutorisees` : "Exploité diagnostics ✅" depuis la Phase 12 — lu par `auMoinsUneRessourceCompatible()` pour le calcul de `ignoredCreneaux.aucuneRessourceDansDataset`. Même interprétation que `activitesCompatibles` salarié : liste vide/nulle = non contraint.

---

### Bloc : `dataSet.indisponibilites`

| Champ | Transporté | Validé | Mappé domaine | Exploité solveur | Exploité scoring | Exploité diagnostics | Exploité WorkMetrics | Testé | Phase cible | Source de vérité (Phase 9) |
|-------|:----------:|:------:|:-------------:|:----------------:|:----------------:|:--------------------:|:--------------------:|:-----:|-------------|---------------------------|
| `indisponibilites` (bloc) | ✅ | — | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | *(actif)* | — (stable) |

> Mappé vers `List<Indisponibilite>` dans `PlanningRequest`. Exploité par la contrainte HARD `IndisponibiliteSalarie` via `@ProblemFactCollectionProperty` dans `PlanningProblem`.

---

### Bloc : `dataSet.referentiels`

| Champ | Transporté | Validé | Mappé domaine | Exploité solveur | Exploité scoring | Exploité diagnostics | Exploité WorkMetrics | Testé | Phase cible | Source de vérité (Phase 9) |
|-------|:----------:|:------:|:-------------:|:----------------:|:----------------:|:--------------------:|:--------------------:|:-----:|-------------|---------------------------|
| `referentiels` (bloc) | ✅ | — | ✅ | ✅ | — | — | — | ✅ | *(actif SC-03)* | — (stable SC-03 ; SC-01 reste hardcodé jusqu'en Phase 9) |

> Phase 7 : `ScenarioResourceMapper.toReferentiel()` convertit le bloc vers `ReferentielComptabiliteActivite`. Utilisé par `ScenarioSc03PreparationService`. SC-01 conserve son référentiel hardcodé (`"travail"`) jusqu'en Phase 9.
> Redondance temporaire documentée : `activite` (libellé) / `codeActiviteId` / `referentiels` coexistent — cible Phase 9.

---

### Bloc : `creneaux` (structuration des besoins)

| Champ | Transporté | Validé | Mappé domaine | Exploité solveur | Exploité scoring | Exploité diagnostics | Exploité WorkMetrics | Testé | Phase cible | Source de vérité (Phase 9) |
|-------|:----------:|:------:|:-------------:|:----------------:|:----------------:|:--------------------:|:--------------------:|:-----:|-------------|---------------------------|
| `groupeBesoinId` | ✅ | — | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | *(actif)* | — (stable) |
| `blocJourId` | ✅ | — | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | *(actif)* | — (stable) |
| `ordreDansBloc` | ✅ | — | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | *(actif)* | — (stable) |
| `estSegmentDePause` | ✅ | — | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | *(actif)* | — (stable) |
| `activite` (libellé) | ✅ | — | ✅ | ✅ | — | — | — | ✅ | Phase 9 | `codeActiviteId` |
| `codeActiviteId` | ✅ | — | ✅ | ✅ | — | — | — | ✅ | *(actif)* | — (stable) |
| `axesOrganisationnels` | ✅ | — | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | Phase 4/5 | — (cible, absorbe les redondances) |

> Mappés vers `Creneau` via `ScenarioCreneauMapper` (Phase 5). Testés dans `ScenarioCreneauMapperTest` (8 tests). Le builder SC-01 génère ses propres créneaux — ces champs sont null pour les créneaux SC-01, ce qui est intentionnel (pas d'impact solveur).

---

### Redondances temporaires documentées

| Champ source | Redondance avec | Cible à terme | Phase de nettoyage |
|--------------|-----------------|---------------|--------------------|
| `sitesAutorises` | `axesOrganisationnels.lieuIds` | axes organisationnels | reporté (non traité en Phase 9) |
| `activite` (libellé) | `codeActiviteId` | référentiel d'activités | reporté (non traité en Phase 9) |
| `referentiels` JSON | référentiel hardcodé dans SC-01 | suppression du hardcodé SC-01 | reporté (non traité en Phase 9) |
| `contraintesReglementaires` salarié | `RegulatoryParameters` globaux | paramètres par salarié | En cours — `joursConsecutifsMaximum` et `amplitudeJournaliereMaximum` exploités (Phase 10/12) ; autres champs à activer progressivement |

---

## Matrice de sécurité par étape

| Phase | Transport | Builder | Solveur | Sortie API | Risque principal                            |
|-------|-----------|---------|---------|------------|---------------------------------------------|
| 0     | =         | =       | =       | =          | casser le socle sans point de départ fiable |
| 1     | ++        | =       | =       | =          | désérialisation / DTO incohérents           |
| 2     | +         | +       | =       | =          | absence de dataset canonique                |
| 3     | =         | ++      | +       | =          | mapping partiel ou ambigu                   |
| 4     | =         | +       | ++      | +          | nouvelles incompatibilités non couvertes    |
| 5     | =         | ++      | +       | +          | structure non exploitée ou mal comprise     |
| 6     | =         | +       | +       | +          | confusion nuit globale / nuit salarié       |
| 7     | ++        | ++      | ++      | +          | ouverture SC-03 trop tôt                    |
| 8     | =         | +       | ++      | ++         | régression scoring / WorkMetrics            |
| 9     | +         | +       | +       | +          | nettoyage prématuré                         |

---

## Batterie de tests à rejouer systématiquement

À rejouer à chaque étape structurante :
- `./gradlew test`
- tests de runtime API
- tests de validation API
- tests de stabilité solveur
- tests de score / scoreBreakdown
- tests WorkMetrics

À compléter progressivement par :
- tests de désérialisation JSON ciblés ;
- tests builder → domaine ;
- tests de compatibilité SC-03 ;
- tests de diagnostics d’affectation.

---

## Ordre recommandé de réalisation

Ordre conseillé :
1. sécuriser le **transport** ;
2. figer le **dataset technique de référence** ;
3. brancher le **builder** ;
4. activer les **règles simples** ;
5. structurer les **besoins** ;
6. préparer le **nuit par salarié** ;
7. ouvrir **SC-03** ;
8. enrichir **score / métriques / diagnostics** ;
9. nettoyer et durcir le contrat.

---

## Journal de pilotage

L’historique détaillé des itérations (Phases 1 à 8) est conservé dans `91_Journal_Developpement_Moteur.md`.

---

## Point d’attention final

Le vrai risque n’est pas d’ajouter des champs.
Le vrai risque est de mélanger trop tôt :
- le contrat transport,
- le modèle domaine,
- la logique builder,
- et les règles solveur.

Ce plan impose donc une progression volontairement incrémentale, afin que chaque couche évolue séparément et reste testable.

