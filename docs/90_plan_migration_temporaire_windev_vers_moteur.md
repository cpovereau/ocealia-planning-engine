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

## Phase 9 — Stabilisation technique et contractuelle du contrat d’entrée

### Objectif

Finaliser la migration en **stabilisant le contrat d’entrée réel** entre WinDev et le moteur, en cohérence avec :

* les DTO effectivement utilisés ;
* le mapping builder → domaine ;
* les contraintes déjà actives dans le solveur ;
* les WorkMetrics exposées en sortie.

Cette phase marque le passage :

```
migration progressive → contrat maîtrisé et stabilisé
```

---

## 9.1 Stabilisation du contrat SC-03

### Principe

À ce stade, tout champ présent dans le contrat doit être :

* soit **explicitement supporté** par le moteur ;
* soit **toléré et documenté** (sans effet immédiat) ;
* soit **supprimé** s’il est inutile ou ambigu.

Aucun champ ne doit rester dans un état implicite ou inconnu.

---

## 9.2 Classification des champs

Chaque champ du contrat est désormais classé selon trois statuts.

### A. SUPPORTÉ

Le champ est :

* transporté dans les DTO ;
* mappé vers le domaine (`ScenarioDatasetBuilder` / mappers) ;
* exploité par :

  * une contrainte,
  * ou le scoring,
  * ou les WorkMetrics,
  * ou les diagnostics.

👉 Il participe réellement au comportement du moteur.

---

### B. TOLÉRÉ

Le champ est :

* accepté dans le JSON ;
* désérialisé et éventuellement mappé ;
* **non exploité** à ce stade par le solveur.

👉 Il est conservé pour :

* compatibilité amont (WinDev) ;
* activation future planifiée.

⚠️ Tout champ toléré doit être :

* tracé ;
* associé à une phase cible d’activation.

---

### C. SUPPRIMÉ / INTERDIT

Le champ est :

* supprimé des DTO ;
* refusé dans le contrat d’entrée ;
* ou explicitement ignoré côté API avec rejet futur prévu.

👉 Objectif : éliminer toute ambiguïté ou redondance.

---

## 9.3 Nettoyage des DTO

### Décisions appliquées

* suppression des champs marqués `IGNORÉ` ou sans usage identifié ;
* suppression des redondances évidentes entre champs métier ;
* conservation des champs dépréciés uniquement si :

  * nécessaires à la compatibilité,
  * clairement identifiés comme tels.

### Conséquence

Les DTO deviennent la **représentation fidèle du contrat réel**,
et non plus un support de migration temporaire.

---

## 9.4 Alignement JSON ↔ DTO ↔ Domaine

### Objectif

Garantir une cohérence stricte entre :

```
JSON WinDev → DTO → Builder → Domaine solveur
```

### Règles

* tout champ JSON doit avoir :

  * une correspondance DTO explicite ;
  * un mapping maîtrisé ;
* aucun champ domaine ne doit dépendre :

  * d’un champ implicite ;
  * ou d’un comportement non documenté.

---

## 9.5 Cohérence avec les contraintes et le scoring

### Vérification effectuée

Chaque champ SUPPORTÉ a été vérifié vis-à-vis de :

* contraintes HARD / SOFT ;
* clés de pénalité (`PenaliteKey`) ;
* calcul des WorkMetrics ;
* diagnostics d’affectation.

👉 Aucun champ actif ne doit être :

* sans effet ;
* ou en contradiction avec une règle existante.

---

## 9.6 Impact sur SC-01

### Principe de non-régression

La stabilisation du contrat SC-03 ne doit pas :

* casser la désérialisation SC-01 ;
* modifier le comportement du solveur SC-01 ;
* altérer les tests existants.

### Règle

Tout champ absent du scénario SC-01 doit :

* conserver un comportement par défaut stable ;
* ou être ignoré sans effet.

---

## 9.7 Verrou de stabilisation

À l’issue de cette phase :

* le contrat d’entrée est considéré comme **maîtrisé** ;
* toute évolution ultérieure devra :

  * passer par une décision explicite ;
  * être tracée dans la matrice des champs ;
  * être accompagnée de tests.

---

## Critère de sortie

Le contrat d’entrée est stabilisé lorsque :

* aucun champ n’est dans un état implicite ;
* chaque champ est classé (SUPPORTÉ / TOLÉRÉ / SUPPRIMÉ) ;
* le mapping DTO → domaine est complet et maîtrisé ;
* les tests couvrent les champs critiques ;
* SC-01 et SC-03 sont tous deux stables.

---

## État au [À compléter]

* stabilisation SC-03 réalisée ;
* nettoyage DTO effectué ;
* contrat aligné avec les contraintes actives ;
* suite de tests exécutée : **BUILD SUCCESSFUL**.


---

## 9.8 Matrice contractuelle des champs (référence Phase 9)

### Objectif

Cette matrice constitue la **référence officielle du contrat d’entrée**.

Elle complète la matrice d’exploitation technique en répondant à la question :

```text
Ce champ fait-il partie du contrat métier du moteur ?
```

---

### Légende des statuts

| Statut       | Signification                                           |
| ------------ | ------------------------------------------------------- |
| **SUPPORTÉ** | Champ pleinement intégré au moteur (utilisé réellement) |
| **TOLÉRÉ**   | Accepté mais sans effet fonctionnel                     |
| **DÉPRÉCIÉ** | Conservé temporairement mais destiné à disparaître      |
| **INTERDIT** | Ne doit plus apparaître dans le contrat                 |

---

### Matrice contractuelle

| Champ                         | Bloc    | Statut             | Utilisation réelle    | Source de vérité | Remplacement / cible                     | Action Phase 9           |
| ----------------------------- | ------- | ------------------ | --------------------- | ---------------- | ---------------------------------------- | ------------------------ |
| `id`                          | salarié | SUPPORTÉ           | Solveur               | DTO              | —                                        | —                        |
| `statut`                      | salarié | TOLÉRÉ             | Aucun                 | DTO              | —                                        | À exploiter ou supprimer |
| `sitesAutorises`              | salarié | DÉPRÉCIÉ           | Aucun                 | DTO              | `axesOrganisationnels.lieuIds`           | Migration progressive    |
| `activitesCompatibles`        | salarié | SUPPORTÉ           | Diagnostics           | DTO              | —                                        | —                        |
| `postesComptablesCompatibles` | salarié | DÉPRÉCIÉ           | Aucun                 | DTO              | `axesOrganisationnels.posteComptableIds` | Migration progressive    |
| `travailleJourFerie`          | salarié | SUPPORTÉ           | Contrainte HARD       | Domaine          | —                                        | —                        |
| `travailDeNuit`               | salarié | SUPPORTÉ           | Scoring + diagnostics | Domaine          | —                                        | —                        |
| `heureDebutNuit`              | salarié | TOLÉRÉ             | Préparation           | DTO              | —                                        | Activation future        |
| `heureFinNuit`                | salarié | TOLÉRÉ             | Préparation           | DTO              | —                                        | Activation future        |
| `contraintesReglementaires`   | salarié | SUPPORTÉ (partiel) | Contraintes           | Domaine          | Remplace `RegulatoryParameters`          | Activation progressive   |
| `axesOrganisationnels`        | salarié | TOLÉRÉ             | Aucun                 | DTO              | Devient source principale                | À mapper                 |
| `contratTravail`              | salarié | TOLÉRÉ             | Aucun                 | DTO              | —                                        | À définir                |

---

| Champ                  | Bloc    | Statut   | Utilisation réelle | Source de vérité | Remplacement / cible | Action Phase 9      |
| ---------------------- | ------- | -------- | ------------------ | ---------------- | -------------------- | ------------------- |
| `activite` (libellé)   | créneau | DÉPRÉCIÉ | Affichage          | DTO              | `codeActiviteId`     | À supprimer à terme |
| `codeActiviteId`       | créneau | SUPPORTÉ | Solveur            | Référentiel      | —                    | —                   |
| `groupeBesoinId`       | créneau | SUPPORTÉ | Structuration      | Domaine          | —                    | —                   |
| `blocJourId`           | créneau | SUPPORTÉ | Structuration      | Domaine          | —                    | —                   |
| `ordreDansBloc`        | créneau | SUPPORTÉ | Structuration      | Domaine          | —                    | —                   |
| `estSegmentDePause`    | créneau | SUPPORTÉ | Structuration      | Domaine          | —                    | —                   |
| `axesOrganisationnels` | créneau | TOLÉRÉ   | Aucun              | DTO              | À exploiter          | À mapper            |

---

| Champ              | Bloc    | Statut   | Utilisation réelle   | Source de vérité | Remplacement / cible | Action Phase 9       |
| ------------------ | ------- | -------- | -------------------- | ---------------- | -------------------- | -------------------- |
| `referentiels`     | dataset | SUPPORTÉ | Référentiel activité | JSON             | —                    | Généralisation SC-01 |
| `indisponibilites` | dataset | SUPPORTÉ | Contrainte HARD      | Domaine          | —                    | —                    |

---

### Règles fondamentales

* Un champ **SUPPORTÉ** doit être :

  * testé ;
  * exploité ;
  * documenté.

* Un champ **TOLÉRÉ** doit :

  * avoir une cible ;
  * ne pas rester indéfini.

* Un champ **DÉPRÉCIÉ** doit :

  * avoir une date ou phase de suppression.

* Aucun champ ne doit rester **implicite**.

---

### Principe de gouvernance

Toute évolution du contrat doit :

* mettre à jour cette matrice ;
* être tracée dans le journal de développement ;
* être validée par des tests.

---

### Rôle de la matrice

Cette matrice devient :

* la référence pour les développeurs ;
* la référence pour l’intégration WinDev ;
* le garde-fou contre la dérive du contrat.

---En 

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
| `referentiels` (bloc) | ✅ | — | ✅ | ✅ | — | — | — | ✅ | *(actif SC-01 et SC-03)* | — (stable) |

> Phase 7 : `ScenarioResourceMapper.toReferentiel()` convertit le bloc vers `ReferentielComptabiliteActivite`. Utilisé par `ScenarioSc03PreparationService`. ~~SC-01 conserve son référentiel hardcodé (`"travail"`) jusqu'en Phase 9.~~
> **Mise à jour 2026-03-30 (chantier SC-01, tâches B1/B2)** : SC-01 lit lui aussi le bloc via `buildReferentielSc01()`. Le référentiel `{"travail": ...}` n'est plus qu'un fallback loggé lorsque `referentiels` est absent ou vide. Asymétrie résiduelle assumée : SC-03 rejette l'absence du bloc (guard IAE), SC-01 la tolère.
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
| `referentiels` JSON | référentiel hardcodé dans SC-01 | suppression du hardcodé SC-01 | ~~reporté (non traité en Phase 9)~~ → **traité le 2026-03-30** (chantier SC-01, B1/B2) — subsiste uniquement comme fallback loggé |
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

## Phase D — Génération de créneaux et convergence vers dataset-driven

### État : TERMINÉE (2026-03-30)

`CreneauGenerationService` créé. `ScenarioSc01PreparationService` adapté. Suite de tests exécutée : **BUILD SUCCESSFUL**. **Phase D TERMINÉE — critère de sortie validé.**

---

### 1. Rôle du `CreneauGenerationService`

Le `CreneauGenerationService` encapsule la logique de génération de créneaux SC-01 dans un composant de service autonome et réutilisable.

**Pourquoi il existe :**
- isole la responsabilité de génération du pipeline de préparation SC-01
- rend la logique de construction testable indépendamment du scénario
- prépare l’architecture vers un modèle dataset-driven (convergence vers SC-03)

**Ce qu’il remplace :**
- l’appel direct à `new ScenarioDatasetBuilderSc01()` dans `ScenarioSc01PreparationService`
- la dépendance directe de la préparation SC-01 sur la classe builder

**Pourquoi il est isolé :**
- la génération de créneaux est un concept transverse (pas lié à un scénario)
- d’autres scénarios ou services pourront le réutiliser sans toucher à SC-01
- en cas de migration vers Option 2 (dataset-driven), seul ce service évolue

---

### 2. Architecture actuelle (Option 1 — maintenue)

```text
SC-01 : paramètres → CreneauGenerationService → créneaux → solveur
SC-03 : dataSet.creneaux → solveur
```

Les deux pipelines restent distincts.
SC-01 génère ses créneaux à partir de paramètres utilisateur (amplitude, horaires, jours travaillés, jours fériés).
SC-03 consomme des créneaux fournis explicitement dans le contrat d’entrée.

#### Contrat SC-01 — point de clarification

`dataSet.creneaux` est **ignoré** dans SC-01. Les créneaux sont générés par le moteur via `CreneauGenerationService` à partir des `scenarioParameters`. Un `log.warn` est émis si `dataSet.creneaux` contient des éléments (guard A1).

---

### 3. Architecture cible (Option 2 — non implémentée)

```text
génération → dataSet.creneaux → partitioning → solveur (pipeline unifié)
```

Dans ce modèle :
- SC-01 utiliserait `CreneauGenerationService` pour produire des `CreneauInputDTO`
- ces créneaux alimenteraient `dataSet.creneaux` avant le solveur
- le partitioning SC-03 (activité inconnue + hors-horizon) s’appliquerait identiquement
- les deux scénarios convergeraient vers le même pipeline de résolution

**Point d’extension préparé (non codé) :** `CreneauGenerationService` pourra exposer une méthode `generateAsInputDtos(BuildRequest)` retournant `List<CreneauInputDTO>` pour alimenter `dataSet.creneaux`.

---

### 4. Stratégie de migration

| Étape | Description | Statut |
|-------|-------------|--------|
| 1 — Extraction | Création de `CreneauGenerationService`, injection dans SC-01 | ✅ Phase D |
| 2 — Réutilisation | Le service peut être utilisé par d’autres scénarios paramétriques | Futur |
| 3 — Injection dataset | Le service produit des `CreneauInputDTO` → `dataSet.creneaux` | Futur |
| 4 — Pipeline unifié | SC-01 et SC-03 partagent le même pipeline de résolution | Futur |

---

### 5. Règles de gouvernance

* SC-01 **reste génératif** tant que WinDev ne fournit pas les créneaux dans `dataSet.creneaux`
* `dataSet.creneaux` est **ignoré** dans SC-01 (guard A1 + log.warn) — jamais silencieusement consumé
* toute évolution du `CreneauGenerationService` doit préserver la compatibilité comportementale avec SC-01
* aucun champ des `scenarioParameters` SC-01 ne doit être implicitement ignoré sans warn
* la migration vers Option 2 est une décision produit, pas une décision technique isolée

---

### Fichiers créés / modifiés

| Fichier | Nature |
|---------|--------|
| `CreneauGenerationService.java` | Nouveau — service de génération isolé |
| `ScenarioSc01PreparationService.java` | Injecte `CreneauGenerationService` au lieu de `new ScenarioDatasetBuilderSc01()` |
| `CreneauGenerationServiceTest.java` | 4 tests unitaires du service de génération |
| Tests A/B/C (3 fichiers) | Constructeur mis à jour avec le nouveau paramètre |

---

## Point d’attention final

Le vrai risque n’est pas d’ajouter des champs.
Le vrai risque est de mélanger trop tôt :
- le contrat transport,
- le modèle domaine,
- la logique builder,
- et les règles solveur.

Ce plan impose donc une progression volontairement incrémentale, afin que chaque couche évolue séparément et reste testable.

