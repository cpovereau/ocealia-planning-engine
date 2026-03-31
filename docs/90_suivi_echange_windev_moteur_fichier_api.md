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
| Spécification OpenAPI | ⏳ À faire | Non démarré |
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

## Phase 4 — API OpenAPI

### Objectif
Exposer le moteur via HTTP sans changer le contrat.

### Tâches

- [ ] Définir endpoints (paramétrique / dataset)
- [ ] Définir schémas OpenAPI
- [ ] Générer YAML OpenAPI
- [ ] Implémenter ApiController
- [ ] Brancher PlanningService
- [ ] Ajouter gestion erreurs HTTP

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
