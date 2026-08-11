# 90_SUIVI_ECHANGE_WINDEV_MOTEUR_FICHIER_API.md

---

# 📊 Suivi du chantier — Échange WinDev ↔ Moteur (Fichier / API)

## 🎯 Objectif

Suivre l’avancement réel du chantier de mise en place du double mode d’échange :
- Fichier (V1)
- API (cible)

Ce document complète le document de cadrage :
👉 `50_ECHANGE_WINDEV_MOTEUR_FICHIER_API.md`

---

# 🧭 1. État global du chantier

| Axe | Statut | Commentaire |
|-----|--------|-------------|
| Cadrage global | ✅ Terminé | Document 50 validé |
| Mode fichier — format | ✅ Terminé | JSON + nommage + workflow |
| Mode fichier — sécurité | ✅ Terminé | ACL + validation + isolation |
| Mode fichier — archivage/purge | ✅ Terminé | Règles définies |
| Implémentation moteur fichier | ✅ Terminé | Pipeline complet : validation, dispatch, archivage YYYY/MM/clientId/ |
| Intégration WinDev fichier | 🔶 Partielle | Tests dépôt/lecture validés côté moteur — génération JSON et écriture atomique WinDev restent à faire |
| Spécification OpenAPI | ✅ Réalisé | `50_openapi_windev_moteur_v_1.yaml` réaligné sur le code réel (2026-03-31) |
| Sécurité API | ⏳ À faire | À définir |
| Tests end-to-end | ✅ Terminé | `FileAdapterEndToEndTest` — pipeline complet avec vrais fichiers |

---

# 🧩 2. Jalons du chantier

## Phase 1 — Mode fichier (MVP)

### Objectif
Permettre un échange complet WinDev ↔ moteur sans API.

### Tâches

- [x] Implémenter le FileAdapter côté moteur — structure complète créée (`fr.project.planning.fileadapter`)
- [x] Implémenter la détection des fichiers — polling par `FileAdapterScheduler` (`@Scheduled`, `file-adapter.enabled`)
- [x] Implémenter le déplacement inbox → processing — `FileClaimService` avec `ATOMIC_MOVE` + validation nom §14.7
- [x] Implémenter la lecture JSON — `FileRequestReader` (ObjectMapper Spring, retour `JsonNode`)
- [x] Brancher PlanningService — via `Sc01FileScenarioExecutionFacade` → `ScenarioSc01ExecutionService` et `Sc03FileScenarioExecutionFacade` → `ScenarioSc03ExecutionService`
- [x] Générer output SUCCESS / ERROR — `FileResponseWriter` (outbox) + `FileErrorWriter` (error), format erreur §13.5
- [x] Implémenter l’archivage — `FileArchiveService.archiveJob()` : input déplacé + output copié, structure `archive/YYYY/MM/clientId/`, fallback UNKNOWN (§15.5)
- [x] Implémenter les logs de traitement — `FileProcessingLogger` + extraction `requestId` dans pipeline

---

### Sous-section : implémentation initiale du FileAdapter (2026-03-30)

#### Packages créés

```
fr.project.planning.fileadapter/
├── config/    FileAdapterProperties, FileAdapterConfiguration
├── model/     FileJobContext, FileJobPaths, FileProcessingStatus, FileProcessingResult
├── service/   FileInboxScanner, FileClaimService, FileRequestReader,
│              FileResponseWriter, FileErrorWriter, FileArchiveService,
│              FileProcessingLogger, FileAdapterService
├── scenario/  FileScenarioExecutionFacade (interface), FileScenarioDispatcher,
│              Sc01FileScenarioExecutionFacade, Sc03FileScenarioExecutionFacade
└── scheduler/ FileAdapterScheduler
```

#### Tests créés (19 cas, tous verts)

- `FileInboxScannerTest` (4 cas)
- `FileClaimServiceTest` (5 cas — dont 2 cas de validation nom de fichier §14.7)
- `FileScenarioDispatcherTest` (5 cas)
- `FileAdapterServiceTest` (5 cas)

#### Points conformes au cadrage §50

- Séparation stricte contrat métier / transport / moteur ✅
- Aucune logique solveur dans la couche fichier ✅
- Aucune modification de PlanningService ou des services SC-01/SC-03 ✅
- Réutilisation des DTO existants (`ScenarioRequestDTO`, `Sc03ScenarioRequestDTO`, `ScenarioResponseDTO`) ✅
- Workflow inbox → processing → outbox/error → archive ✅
- Claim obligatoire avant traitement (ATOMIC_MOVE) ✅
- Scan uniquement `.json` ✅
- Création répertoires au démarrage ✅
- Scheduler activable/désactivable (`file-adapter.enabled`) ✅
- Nommage fichier sortie `<nom_input>_RESULT.json` ✅ (§13.5)
- Format fichier erreur `{requestId, status:"ERROR", error:{code,message,details}}` ✅ (§13.5)
- Extraction `requestId` depuis le JSON pour traçabilité dans les logs ✅ (§14.9)
- Validation nom de fichier (protection `..`, séparateurs injectés) ✅ (§14.7)
- Valeurs `scenarioType` "SC-01" / "SC-03" confirmées par fixtures existantes ✅

---

### Points ouverts — Phase 1

Tous les points ouverts de la phase 1 technique sont clos.

| Point | Réf | Statut |
|-------|-----|--------|
| ~~Archivage du fichier de sortie (outbox/error)~~ | §15.5 | ✅ Fait |
| ~~Organisation archive `/YYYY/MM/clientId/`~~ | §15.5 | ✅ Fait |
| ~~Validation champs obligatoires (`requestId`, `metadata.*`)~~ | §14.6 | ✅ Fait |
| ~~Validation taille maximale fichier~~ | §14.6 | ✅ Fait |
| ~~Tests end-to-end (dépôt réel → fichier outbox)~~ | — | ✅ Fait |

---

## Clôture de la phase 1 technique — FileAdapter (2026-03-31)

### Ce qui est implémenté et testé

| Composant | Implémenté | Testé |
|-----------|-----------|-------|
| `FileInboxScanner` — scan inbox `.json` uniquement | ✅ | ✅ 4 cas |
| `FileClaimService` — ATOMIC_MOVE + protection §14.7 | ✅ | ✅ 5 cas |
| `FileInputValidator` — extension, taille, structure, 4 champs | ✅ | ✅ 11 cas |
| `FileRequestReader` — lecture JsonNode | ✅ | couvert end-to-end |
| `FileScenarioDispatcher` — routage SC-01 / SC-03 | ✅ | ✅ 5 cas |
| `FileResponseWriter` — enveloppe `FileSuccessResponse` | ✅ | couvert end-to-end |
| `FileErrorWriter` — format §13.5, codes machine-readable | ✅ | couvert end-to-end |
| `FileArchiveService` — input MOVE + output COPY, YYYY/MM/clientId/ | ✅ | ✅ 6 cas |
| `FileAdapterService` — pipeline orchestrateur | ✅ | ✅ 5 cas unitaires + 3 end-to-end |
| `FileAdapterScheduler` — polling activable/désactivable | ✅ | — |

### Ce qui reste hors scope phase 1 (non implémenté, documenté)

| Point | Réf doc 50 | Décision |
|-------|-----------|---------|
| Purge automatique outbox/error/archive | §15.6 | Exploitation — tâche planifiée hors moteur |
| Alertes fichiers bloqués inbox/processing | §15.3 | Exploitation — monitoring externe |
| Reprise après crash (fichier en processing/) | §15.8 | À traiter en phase de stabilisation |
| Déduplication par requestId | §13.7 règle 3 | Partiel — le ATOMIC_MOVE couvre le cas concurrent, pas le re-dépôt délibéré |

### Points faibles connus et acceptés

- Les logs de traitement couvrent `requestId`, `clientId`, statut. La date de dépôt et le chemin fichier sortie (§14.9) ne sont pas explicitement loggués.
- `FileResponseWriter` et `FileRequestReader` sont couverts uniquement par les tests end-to-end, pas en isolation.
- SC-03 bout en bout non testé dans `FileAdapterEndToEndTest` (couvert par les tests du service SC-03 existant).

---

## Phase 2 — Intégration WinDev

### Objectif
Permettre à WinDev de produire et consommer les fichiers.

### Tâches

- [ ] Génération du JSON SC-01 / SC-03
- [ ] Écriture atomique (.tmp → .json)
- [x] Dépôt dans inbox
- [x] Lecture des fichiers outbox
- [x] Gestion des fichiers error
- [ ] Gestion du requestId

---

## Phase 3 — Stabilisation fichier

### Objectif
Valider la robustesse du mode fichier.

### Tâches

- [ ] Tests multi-clients
- [ ] Tests volumétrie
- [ ] Tests erreurs
- [ ] Tests reprise après crash
- [ ] Validation purge automatique

---

# Phase 4 — API OpenAPI (stabilisation V1)

## Objectif

Produire une **API HTTP V1 stable, testable et alignée** :

* avec le code réel
* avec le contrat fichier
* exploitable immédiatement par le service Produit

Cette phase vise une **stabilisation contractuelle**, pas une évolution fonctionnelle du moteur.

## 📦 Périmètre de la phase

### Inclus

* ajout de `requestId` et `metadata` dans les DTO API
* homogénéisation des erreurs HTTP
* réalignement OpenAPI avec le code
* validation SC-01 / SC-03 end-to-end via API

### Exclus

* refonte du moteur
* enrichissement complet du `PlanningContext`
* refonte des règles métier
* sécurité API avancée (phase 5)

## Tâches détaillées

### 4.1 — Intégration `requestId` et `metadata` dans l’API ✅ TERMINÉ (2026-03-31)

* [x] Ajouter dans `ScenarioRequestDTO` et `Sc03ScenarioRequestDTO` : `requestId`, `metadata.clientId`, `metadata.timestamp`, `metadata.source` (optionnel)
* [x] Rendre obligatoires `requestId`, `metadata.clientId`, `metadata.timestamp` via Bean Validation (`@NotBlank`, `@NotNull`, `@Valid`)
* [x] Vérifier la compatibilité avec SC-01, SC-03 et FileAdapter — aucune régression, suite complète verte
* [x] Adapter les validations d’entrée — `@Valid` ajouté sur les 3 méthodes POST du controller

#### Fichiers modifiés

| Fichier | Nature |
|---|---|
| `build.gradle` | Ajout `spring-boot-starter-validation` |
| `MetadataDTO` | Nouveau DTO — `clientId` (`@NotBlank`), `timestamp` (`@NotBlank`), `source` (optionnel) |
| `ScenarioRequestDTO` | Ajout `requestId` + `metadata` avec contraintes |
| `Sc03ScenarioRequestDTO` | Idem |
| `ScenarioController` | `@Valid` sur les 3 méthodes POST |
| 5 fixtures JSON (sc01 + sc03) | Ajout `requestId` + `metadata` |
| `ScenarioControllerValidationTest` | Payloads inline mis à jour |
| `ScenarioControllerSc03ValidationTest` | Payloads inline mis à jour |
| `RequestMetadataValidationTest` | **Nouveau** — 11 tests dédiés 4.1 |

#### Choix techniques retenus

- Validation via Bean Validation Jakarta (hibernate-validator) — standard Spring Boot 3.
- `timestamp` stocké comme `String` sans parsing ISO-8601 — valider le format est hors périmètre 4.1.
- Le FileAdapter **n’est pas impacté** : il désérialise les DTO via `ObjectMapper.treeToValue()` sans passer par le controller Spring ni déclencher Bean Validation.
- Pas de `ControllerAdvice` ajouté — la gestion d’erreur unifiée est le périmètre de 4.2.

#### Points d’attention pour 4.2

- La réponse 400 expose actuellement le format Spring par défaut (`MethodArgumentNotValidException`). Phase 4.2 devra introduire un `ControllerAdvice` pour produire le format `ErrorResponse` de la spec OpenAPI (`{error: {code, message, details}}`).
- `timestamp` est une `String` libre. Si 4.2 nécessite un parsing `Instant`/`OffsetDateTime`, il faudra changer le type dans `MetadataDTO` et gérer la désérialisation Jackson.
- Les erreurs IDE sur `jakarta.validation` sont dues au cache Gradle non rechargé dans l’IDE — la compilation et les 11 tests Gradle sont verts.

### 4.2 — Gestion d’erreur HTTP unifiée ✅ TERMINÉ (2026-03-31)

* [x] Mettre en place un format unique :

```json
{
  "error": {
    "code": "...",
    "message": "...",
    "details": []
  }
}
```

* [x] Implémenter `400` pour les erreurs de structure / validation (`INVALID_REQUEST`, `MALFORMED_JSON`)
* [x] Implémenter `422` pour les incohérences métier (`BUSINESS_ERROR`)
* [x] Implémenter `500` pour les erreurs techniques (`INTERNAL_ERROR`)
* [x] Mapper les exceptions existantes via `GlobalExceptionHandler`
* [x] Tests adaptés — suite complète verte (24 tests + 3 unitaires handler)

#### Fichiers créés / modifiés

| Fichier | Nature |
|---|---|
| `GlobalExceptionHandler` | Nouveau — `@RestControllerAdvice` interceptant les 4 cas |
| `ErrorResponseDTO` | Nouveau — wrapper `{ error: { code, message, details } }` |
| `ErrorDetailDTO` | Nouveau — entrée `{ field, message }` dans `details` |
| `GlobalExceptionHandlerTest` | Nouveau — 3 tests unitaires (sans contexte Spring) |
| `ScenarioControllerValidationTest` | Mis à jour — assertions sur 422 + JSON body |
| `ScenarioControllerSc03ValidationTest` | Mis à jour — idem |
| `RequestMetadataValidationTest` | Mis à jour — vérification `$.error.code` sur les 400 |

#### Mapping des exceptions

| Exception | HTTP | Code |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | `INVALID_REQUEST` |
| `HttpMessageNotReadableException` | 400 | `MALFORMED_JSON` |
| `IllegalArgumentException` | 422 | `BUSINESS_ERROR` |
| `Exception` (catch-all) | 500 | `INTERNAL_ERROR` |

#### Points d’attention pour 4.6

- `NullPointerException` sur `dataSet` absent dans SC-01 retourne actuellement 500 `INTERNAL_ERROR`. La cause est un accès null avant le guard `IllegalArgumentException` dans `ScenarioSc01PreparationService`. Corriger en ajoutant un guard IAE explicite sur `dataSet` nul → produira 422 `BUSINESS_ERROR` (TODO documenté dans `ScenarioControllerValidationTest.should_return_500_if_dataset_missing`).
- Le FileAdapter **n’est pas impacté** : le `ControllerAdvice` n’intercepte que les exceptions passant par le dispatcher Spring MVC.
- `timestamp` reste `String` — aucun changement de type en 4.2.

### 4.3 — Réalignement des DTO API ✅ TERMINÉ (2026-03-31)

* [x] Vérifier la cohérence de `ScenarioRequestDTO`, `Sc01ScenarioParametersDTO`, `Sc03ScenarioRequestDTO`
* [x] Nettoyer les champs obsolètes et les alias historiques
* [x] Vérifier `CreneauInputDTO`, `SalarieInputDTO`, `PosteVirtuelInputDTO`

#### Résultats de l'audit

Audit exhaustif de 11 DTO (request, input, context). La quasi-totalité des DTOs est alignée sur le code réel — les anciens noms signalés (`salarieId`, `heureDebutPoste`, `amplitudeJournaliereHeures`) avaient déjà été corrigés lors de phases antérieures.

**Une seule correction appliquée :**

| Fichier | Nature |
|---|---|
| `PosteVirtuelInputDTO` | Renommage Java : `activitesAutorisees` → `activitesCompatibles`, `lieuxAutorises` → `sitesAutorises` + `@JsonAlias` pour rétrocompat JSON |
| `ScenarioResourceMapper` | Mise à jour `toPosteVirtuel()` — appels des nouveaux getters |
| `ScenarioSc03PreparationService` | Mise à jour `auMoinsUneRessourceCompatible()` — appel du nouveau getter |
| `ScenarioResourceMapperTest` | Mise à jour des appels setters dans `toPosteVirtuel_doitMapperTousLesChamps` |

#### Écarts constatés non corrigés (intentionnels ou ambigus)

| DTO | Observation | Décision |
|---|---|---|
| `ScenarioRequestDTO` | Absence de `@JsonIgnoreProperties` vs `Sc03ScenarioRequestDTO` | **Non corrigé** — cohérent avec le schéma Phase 10B/10C : SC-01 est strict, SC-03 est tolérant (documenté) |
| `CreneauInputDTO.activite` | Champ marqué `⚠️ DÉPRÉCIÉ` mais fonctionnellement utilisé (fallback) | **Conservé** — le fallback est actif dans `ScenarioCreneauMapper` et `ScenarioSc03PreparationService` |
| `SalarieInputDTO.@JsonAlias` | Aliases `activitesAutorisees`/`lieuxAutorises` conservés | **Conservé** — WinDev envoie encore les anciens noms en SC-01 (documenté dans le commentaire de classe) |

#### Points d'attention pour 4.5

- `PosteVirtuelInputDTO` : le contrat JSON WinDev continue d'utiliser `activitesAutorisees` et `lieuxAutorises`. Si WinDev migre vers les noms canoniques, les `@JsonAlias` peuvent être retirés (action mineure, sans impact sur le moteur).
- L'asymétrie `ScenarioRequestDTO` (strict) / `Sc03ScenarioRequestDTO` (tolérant) devra être documentée dans la spec OpenAPI 4.5 pour que WinDev en soit informé.

### 4.4 — Stabilisation des endpoints ✅ TERMINÉ (2026-04-07)

* [x] Conserver les routes réelles : routes V1 confirmées et verrouillées
* [x] Documenter explicitement : SC-01 = paramétrique avec génération ; SC-03 = dataset-driven
* [x] Ne pas renommer les routes en V1

#### Routes V1 — inventaire verrouillé

| Méthode | Route | Pipeline | DTO entrée | DTO sortie |
|---|---|---|---|---|
| `GET` | `/scenarios/ping` | Santé du service | — | `"OK"` (text/plain) |
| `POST` | `/scenarios/sc01/solve` | Paramétrique : `scenarioParameters` → génération créneaux → solveur | `ScenarioRequestDTO` | `ScenarioResponseDTO` |
| `POST` | `/scenarios/sc01/solve/file` | Identique SC-01, réponse en pièce jointe JSON | `ScenarioRequestDTO` | `ScenarioResponseDTO` + `Content-Disposition` |
| `POST` | `/scenarios/sc03/solve` | Dataset-driven : `dataSet.creneaux` → solveur | `Sc03ScenarioRequestDTO` | `ScenarioResponseDTO` |

**Décision V1 actée** : pas de routes génériques `/parametric/` ou `/dataset/`. Les noms SC-01 / SC-03 identifient à la fois le pipeline et la configuration WinDev correspondante.

#### Vérification des écarts code / spec / tests

| Point vérifié | État |
|---|---|
| Routes dans `ScenarioController` | ✅ Conformes aux 4 routes V1 |
| Routes dans `50_openapi_windev_moteur_v_1.yaml` | ✅ Chemins corrects |
| Routes dans les tests controller | ✅ Conformes (`/scenarios/sc01/solve`, `/scenarios/sc03/solve`) |
| `dataSet.creneaux` ignoré en SC-01 | ✅ Documenté dans javadoc contrôleur + log WARN service |
| `dataSet.creneaux` obligatoire en SC-03 | ✅ Guard IAE dans `ScenarioSc03PreparationService` |

#### Fichier modifié

| Fichier | Nature |
|---|---|
| `ScenarioController` | Javadoc complété — pipeline explicite par route, liste des 4 routes V1 |

#### Points d'attention pour 4.5

- Les descriptions `Sc01ScenarioRequest` et `Sc03ScenarioRequest` dans l'OpenAPI mentionnent encore `requestId` et `metadata` comme "absents" — obsolète depuis Phase 4.1. À corriger en 4.5.
- L'exemple d'erreur 422 dans l'OpenAPI utilise le code `SCENARIO_VALIDATION_FAILED` — le code réel (`GlobalExceptionHandler`) retourne `BUSINESS_ERROR`. À aligner en 4.5.
- Les exemples `sc01_minimal` et `sc03_minimal` dans l'OpenAPI n'incluent pas `requestId` ni `metadata` — ils provoqueraient un 400 si utilisés tels quels. À corriger en 4.5.

### 4.5 — Mise à jour OpenAPI ✅ TERMINÉ (2026-04-07)

* [x] Mettre à jour les schémas request/response, les erreurs et les exemples
* [x] Vérifier la cohérence avec les DTO Java
* [x] Générer une version diffusable au Produit

#### Corrections appliquées

| Section | Correction |
|---|---|
| `info.description` | Suppression bullet "requestId/metadata absents" — remplacé par note sur caractère obligatoire |
| `Metadata` schema | Nouveau schema ajouté (`clientId`, `timestamp` obligatoires, `source` optionnel) |
| `Sc01ScenarioRequest` | `requestId` + `metadata` ajoutés aux `properties` et `required` — description mise à jour |
| `Sc03ScenarioRequest` | Idem |
| Exemple `sc01_minimal` | `requestId`, `metadata`, `shiftEndAlert` ajoutés |
| Exemple `sc03_minimal` | `requestId`, `metadata`, `referentiels` ajoutés |
| `BadRequest` | Ajout exemple `MALFORMED_JSON` ; libellés corrects |
| `UnprocessableEntity` | `SCENARIO_VALIDATION_FAILED` → `BUSINESS_ERROR` ; `details: []` (conforme handler) |
| `ErrorBody.code` | Liste de codes mise à jour : `INVALID_REQUEST`, `MALFORMED_JSON`, `BUSINESS_ERROR`, `INTERNAL_ERROR` |
| `ErrorResponse` | Note sur requestId corrigée (requestId est bien dans les DTOs, mais non répercuté dans la réponse d'erreur) |
| `PosteVirtuelInput` | Noms canoniques `activitesCompatibles`/`sitesAutorises` — aliases documentés |
| `SalarieInput` | Description aliases corrigée ("alias en entrée" au lieu de "sérialisé en") |
| `Sc01ScenarioParameters` | `shiftEndAlert` ajouté à `required` (requis métier réel — throw IAE si absent) |

#### Endpoints confirmés dans la spec

```
GET  /scenarios/ping
POST /scenarios/sc01/solve
POST /scenarios/sc01/solve/file
POST /scenarios/sc03/solve
```

#### Points à valider avec le Produit

- `shiftEndAlert` maintenant marqué obligatoire en SC-01 (était implicitement requis, désormais explicite).
- `PosteVirtuelInput` : noms canoniques `activitesCompatibles`/`sitesAutorises` — WinDev peut migrer vers ces noms ou continuer avec les aliases.
- `sc03_minimal` inclut désormais `referentiels` car SC-03 l'exige (sans référentiel, tous les créneaux sont exclus comme activité inconnue).

### 4.6 — Tests

* [ ] Adapter les tests controller
* [ ] Mettre à jour les fixtures JSON
* [ ] Tester SC-01
* [ ] Tester SC-03
* [ ] Tester les erreurs `400` / `422` / `500`

### 4.7 — Validation finale

* [ ] Faire un test manuel via Postman ou curl
* [ ] Vérifier la cohérence FileAdapter / API
* [ ] Valider en interne le contrat

## Critères de validation

Une fois la phase terminée :

* l’API accepte `requestId` et `metadata`
* les erreurs sont homogènes
* la spec OpenAPI est alignée avec le code
* SC-01 et SC-03 fonctionnent via HTTP
* aucun écart critique document/code

## Risques maîtrisés

| Risque                 | Maîtrise                          |
| ---------------------- | --------------------------------- |
| Régression API         | tests controller                  |
| Divergence fichier/API | contrat JSON unique               |
| surcharge Produit      | contrat stabilisé avant livraison |

## Définition de terminé

La phase est terminée si :

* le Produit peut consommer l’API sans adaptation ultérieure majeure
* la spec OpenAPI est considérée comme **contractuelle V1**

---

## Phase 5 — Sécurité API

### Objectif
Sécuriser les appels API.

### Tâches

- [ ] Choix auth (API Key / OAuth2)
- [ ] Mise en place HTTPS
- [ ] Gestion des rôles
- [ ] Ajout requestId
- [ ] Logs sécurisés

---

# 🧠 3. Décisions prises

| Date | Décision | Impact |
|------|----------|--------|
| 2026-03-30 | Mode hybride fichier + API | Structure globale validée |
| 2026-03-30 | JSON unique fichier/API | Pas de duplication contrat |
| 2026-03-30 | Workflow inbox/processing/outbox | Robustesse traitement |
| 2026-03-30 | ACL + séparation rôles | Sécurité fichier |
| 2026-03-30 | Archivage + purge automatique | Exploitabilité long terme |
| 2026-03-30 | `scenarioType` "SC-01"/"SC-03" confirmé | Valeurs validées par fixtures de tests existantes |
| 2026-03-30 | Façades FileAdapter branchées sur services existants | `ScenarioSc01ExecutionService` et `ScenarioSc03ExecutionService` réutilisés sans modification |
| 2026-03-30 | Format erreur §13.5 adopté côté moteur | `FileErrorWriter` produit `{requestId, status:"ERROR", error:{...}}` |
| 2026-03-30 | Enveloppe transport SUCCESS — `FileSuccessResponse` | `ScenarioResponseDTO` inchangé ; enveloppe `{requestId, status, response}` spécifique au mode fichier. API HTTP non impactée. |
| 2026-03-30 | Validation d'entrée fichier — `FileInputValidator` | Extension, taille max (`max-input-file-size-bytes`), structure JSON, champs obligatoires (`requestId`, `scenarioType`, `metadata.clientId`, `metadata.timestamp`). Codes machine-readable dans les fichiers ERROR. |
| 2026-03-31 | Archivage complet — input déplacé + output copié — structure `archive/YYYY/MM/clientId/` | `FileJobPaths.archiveFile` supprimé ; `FileArchiveService.archiveJob()` créé ; `clientId` propagé depuis payload |

---

# ⚠️ 4. Points de vigilance

| Sujet | Risque | Action |
|------|--------|--------|
| JSON | divergence fichier/API | OpenAPI source unique |
| Fichiers bloqués | saturation inbox/processing | alertes + monitoring |
| Multi-clients | fuite données | cloisonnement |
| Volumétrie | saturation disque | purge automatique |
| Reprise incident | perte traitement | idempotence + archive |

---

# 📊 5. Indicateurs de suivi

### Techniques

- temps moyen de traitement
- nombre de fichiers traités / jour
- nombre d’erreurs
- taille des répertoires

### Fonctionnels

- taux de succès
- nombre de créneaux traités
- stabilité des résultats

---

# 🧭 6. Prochaines actions immédiates

1. ~~Implémenter le FileAdapter côté moteur~~ — fait
2. ~~Définir la stratégie de détection (polling vs watcher)~~ — polling retenu (`FileAdapterScheduler`)
3. ~~Créer un test de bout en bout simple (TempDir, pipeline inbox → outbox complet)~~ — fait
4. ~~Décider du format outbox SUCCESS~~ — `FileSuccessResponse` adopté
5. ~~Compléter l'archivage du fichier de sortie (§15.5)~~ — fait
6. Valider avec un premier échange WinDev réel — en attente phase 2

---

# 🧠 Principe de pilotage

> Une étape n’est validée que si elle est :
> - implémentée
> - testée
> - observée en conditions réelles

---

# 📌 Rappel

Ce document est :
- évolutif
- opérationnel
- orienté suivi réel

Il doit être mis à jour à chaque avancée structurante du chantier.
