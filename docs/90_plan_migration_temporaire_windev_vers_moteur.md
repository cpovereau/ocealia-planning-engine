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

### 2. Préserver le socle existant
Aucune étape ne doit casser :
- la désérialisation SC-01 ;
- l’exécution solveur déjà validée ;
- le contrat de sortie `ScenarioResponseDTO` ;
- les tests de stabilité du score et des affectations.

### 3. Tolérance contractuelle provisoire
Pendant la migration :
- les nouveaux champs peuvent être transportés avant d’être utilisés ;
- certains champs peuvent être tolérés sans effet immédiat côté solveur ;
- les redondances temporaires sont acceptées uniquement si elles sont documentées.

### 4. Nettoyage différé mais planifié
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
- `duree` sur le créneau si recalculable ;
- `activite` comme libellé si `codeActiviteId` existe ;
- `sitesAutorises` si `axesOrganisationnels.lieuIds` devient la source de vérité.

---

## Plan de progression détaillé

## Phase 0 — Gel du point de départ

### Objectif
Photographier l’existant avant toute évolution du contrat d’entrée.

### Travaux
- identifier les DTO réellement utilisés par l’API ;
- lister les tests de non-régression déjà en place ;
- figer un JSON de référence SC-01 ;
- figer un JSON cible SC-03 / technique de migration ;
- noter les champs déjà documentés mais non supportés dans le code.

### Tests à exécuter
- `ScenarioControllerRuntimeTest`
- `ScenarioControllerValidationTest`
- `PlanningServiceSolverStabilityTest`
- `PlanningServiceScoreRegressionTest`
- `./gradlew test`

### Critère de sortie
Le socle actuel est vert avant toute modification.

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

---

## Phase 3 — Brancher les nouveaux champs dans le builder

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

### Objectif
Exploiter les nouveaux champs dans des règles simples, sans encore toucher au scoring fin.

### Règles visées
- un salarié avec `travailleJourFerie = false` ne peut pas être affecté sur un jour férié ;
- une indisponibilité interdit l’affectation du salarié sur l’intervalle concerné ;
- les incompatibilités de base d’axes organisationnels sont appliquées proprement.

### Nature recommandée
- d’abord en **HARD** si ce sont de vraies interdictions ;
- sinon en règle de filtrage du value range si cela simplifie le solveur.

### Tests à créer
- salarié refusé sur jour férié ;
- salarié autorisé sur jour férié ;
- affectation impossible à cause d’une indisponibilité ;
- scénario de non-régression SC-01 inchangé sans ces champs.

### Critère de sortie
Les nouveaux champs commencent à avoir un effet métier observable.

---

## Phase 5 — Structurer les besoins et blocs journaliers

### Objectif
Préparer le moteur à raisonner sur des groupes logiques de créneaux sans changer la variable de décision principale.

### Travaux
- exploiter `groupeBesoinId` ;
- exploiter `blocJourId` ;
- exploiter `ordreDansBloc` ;
- reconnaître `estSegmentDePause` ;
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

### Objectif
Sortir du mode “SC-01 seulement” et ouvrir le vrai scénario cible de couverture locale.

### Travaux
- introduire `Sc03ScenarioParametersDTO` ;
- formaliser `prioriteCouverture` ;
- formaliser `lieuxCibles` si retenu ;
- exposer l’endpoint SC-03 ;
- brancher le builder adéquat.

### Tests à créer
- validation API SC-03 ;
- désérialisation complète SC-03 ;
- exécution d’un scénario SC-03 minimal ;
- exécution d’un scénario SC-03 enrichi.

### Critère de sortie
Le chantier sort du simple test technique et devient un vrai scénario supporté.

---

## Phase 8 — Ajuster scoring, WorkMetrics et explicabilité

### Objectif
Faire évoluer la lecture métier du planning à partir des nouveaux champs transmis par WinDev.

### Pistes visées
- intégrer le statut `travailDeNuit` dans les futures pénalités ;
- exposer les refus d’affectation liés à `travailleJourFerie = false` ;
- enrichir les diagnostics d’incompatibilité ;
- préparer les futures métriques d’équité et de contractuel.

### Tests à créer
- tests de score ciblés ;
- tests de diagnostics ;
- tests WorkMetrics avec données salarié enrichies.

### Critère de sortie
Les nouveaux champs ont un impact visible dans la restitution, pas seulement dans l’entrée.

---

## Phase 9 — Nettoyage du contrat transitoire

### Objectif
Supprimer les redondances et figer le contrat d’entrée.

### Champs candidats au nettoyage
- `sitesAutorises` si remplacé par `axesOrganisationnels.lieuIds` ;
- `activite` comme clé si `codeActiviteId` est généralisé ;
- `duree` si entièrement recalculée ;
- champs tolérés mais non utilisés.

### Décisions associées
- durcir les schémas JSON ;
- supprimer progressivement la tolérance documentaire temporaire ;
- préparer ensuite le rejet strict des champs inconnus.

### Tests à créer
- tests de validation stricte ;
- tests de rejet des anciens champs supprimés ;
- campagne complète de non-régression.

### Critère de sortie
Le contrat d’entrée est stabilisé et nettoyé.

---

## Matrice de sécurité par étape

| Phase | Transport | Builder | Solveur | Sortie API | Risque principal |
|------|-----------|---------|---------|------------|------------------|
| 0 | = | = | = | = | casser le socle sans point de départ fiable |
| 1 | ++ | = | = | = | désérialisation / DTO incohérents |
| 2 | + | + | = | = | absence de dataset canonique |
| 3 | = | ++ | + | = | mapping partiel ou ambigu |
| 4 | = | + | ++ | + | nouvelles incompatibilités non couvertes |
| 5 | = | ++ | + | + | structure non exploitée ou mal comprise |
| 6 | = | + | + | + | confusion nuit globale / nuit salarié |
| 7 | ++ | ++ | ++ | + | ouverture SC-03 trop tôt |
| 8 | = | + | ++ | ++ | régression scoring / WorkMetrics |
| 9 | + | + | + | + | nettoyage prématuré |

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

## Journal de pilotage à compléter au fil des itérations

### Itération 1
- objectif :
- fichiers touchés :
- tests ajoutés :
- tests rejoués :
- résultat :
- régression observée :
- décision prise :

### Itération 2
- objectif :
- fichiers touchés :
- tests ajoutés :
- tests rejoués :
- résultat :
- régression observée :
- décision prise :

### Itération 3
- objectif :
- fichiers touchés :
- tests ajoutés :
- tests rejoués :
- résultat :
- régression observée :
- décision prise :

---

## Point d’attention final

Le vrai risque n’est pas d’ajouter des champs.
Le vrai risque est de mélanger trop tôt :
- le contrat transport,
- le modèle domaine,
- la logique builder,
- et les règles solveur.

Ce plan impose donc une progression volontairement incrémentale, afin que chaque couche évolue séparément et reste testable.

