# 50_ECHANGE_WINDEV_MOTEUR_FICHIER_API.md

---

# 📌 1. Contexte

Le moteur de planification OptaPlanner est désormais capable de :
- recevoir un contrat d’entrée structuré (SC-01, SC-03) ;
- exécuter un pipeline complet de résolution ;
- restituer une réponse métier stabilisée (`ScenarioResponseDTO`).

L’intégration avec le logiciel WinDev doit maintenant être industrialisée.

Le contexte applicatif impose une contrainte forte :

## Double mode de déploiement WinDev

### 1. Version Full Web
- point d’entrée unique
- multi-tenant
- routage vers la base client via annuaire webservice

### 2. Version SaaS non web
- un exécutable par client
- environnement isolé
- connexion via Remote Directory Web

---

# 🎯 2. Objectif du document

Ce document définit le cadre de mise en place d’un **mode d’échange hybride** entre WinDev et le moteur :

👉 **Mode Fichier** (court terme)
👉 **Mode API (OpenAPI)** (cible)

Il constitue la **référence d’architecture et de conception** pour :
- les échanges techniques
- le contrat d’intégration
- les évolutions futures

---

# 🧠 3. Principe fondamental

## Séparation stricte des responsabilités

L’architecture repose sur une règle centrale :

> ❗ Le **contrat métier JSON est indépendant du mode de transport**

On distingue 3 couches :

### 1. Contrat métier
- JSON SC-01 / SC-03
- DTO
- `ScenarioResponseDTO`

### 2. Transport
- Fichier (input/output JSON)
- API HTTP (OpenAPI)

### 3. Moteur
- mapping DTO → domaine
- solveur OptaPlanner
- restitution

---

# 🧩 4. Modes d’échange supportés

## 4.1 Mode Fichier (Phase initiale)

### Description
WinDev produit un fichier JSON d’entrée, le moteur produit un fichier JSON de sortie.

### Flux

```
WinDev → input.json
            ↓
         Moteur
            ↓
       output.json → WinDev
```

### Caractéristiques

| Critère | Valeur |
|--------|--------|
| Synchrone | ❌ |
| Complexité | Faible |
| Traçabilité | Excellente |
| Robustesse | Élevée |
| Temps réel | Non |

### Objectif

- démarrage rapide de l’intégration
- limitation des dépendances réseau
- facilité de debug et de replay

---

## 4.2 Mode API (Cible)

### Description
WinDev appelle directement le moteur via HTTP (OpenAPI).

### Flux

```
WinDev → HTTP POST /solve → Moteur → réponse JSON
```

### Caractéristiques

| Critère | Valeur |
|--------|--------|
| Synchrone | ✅ |
| Complexité | Moyenne |
| Temps réel | Oui |
| Industrialisation | Élevée |

### Objectif

- intégration temps réel
- standardisation OpenAPI
- sécurisation des échanges

---

# 🏗️ 5. Architecture cible

Le moteur expose deux adaptateurs d’entrée :

```
                WinDev
                   │
        ┌──────────┴──────────┐
        │                     │
   Mode Fichier          Mode API
        │                     │
  FileAdapter         ApiController
        │                     │
        └──────────┬──────────┘
                   ▼
            PlanningService
                   ▼
                Solveur
                   ▼
          ScenarioResponse
```

---

# 🔄 6. Typologie des contrats d’entrée

Le moteur distingue deux modes d’alimentation :

## 6.1 Contrat paramétrique (SC-01)

- les créneaux sont générés par le moteur
- utilisation de `CreneauGenerationService`
- `dataSet.creneaux` ignoré

### Logique

```
paramètres → génération → solveur
```

---

## 6.2 Contrat dataset-driven (SC-03)

- les créneaux sont fournis par WinDev
- le moteur consomme directement le dataset

### Logique

```
dataSet.creneaux → solveur
```

---

## 6.3 Trajectoire cible

À terme :

```
paramètres → génération → dataSet.creneaux → solveur
```

👉 convergence vers un pipeline unique

---

# 🧭 7. Stratégie d’intégration

## Phase 1 — Mode fichier

- stabilisation du JSON
- validation du contrat métier
- validation du moteur

## Phase 2 — Mode API

- exposition OpenAPI
- réutilisation du même contrat JSON
- ajout de la sécurité

## Phase 3 — convergence

- harmonisation SC-01 / SC-03
- généralisation dataset-driven

---

# 🔐 8. Sécurité (à compléter)

Cette section sera enrichie progressivement.

Axes prévus :

- authentification (API Key / OAuth2)
- sécurisation transport (HTTPS)
- gestion des erreurs
- traçabilité (`requestId`)
- quotas et limitations

---

# 📊 9. Règles de gouvernance

## 9.1 Contrat JSON

- unique pour fichier et API
- versionné
- documenté via OpenAPI

## 9.2 Non-duplication

❌ interdit :
- JSON différent fichier vs API

## 9.3 Évolution contrôlée

- toute évolution du contrat doit être tracée
- mise à jour obligatoire des exemples

---

# ⚠️ 10. Risques identifiés

| Risque | Description |
|------|------------|
| Double contrat | divergence fichier / API |
| Mauvaise abstraction | mélange transport / métier |
| Complexité prématurée | API trop tôt sans stabilisation |

---

# ✅ 11. Décisions structurantes

- adoption d’un mode hybride fichier + API
- séparation stricte contrat / transport
- maintien de deux pipelines (paramétrique / dataset)
- convergence progressive vers dataset-driven

---

# 📄 13. Format des échanges — Mode Fichier (V1)

## 13.1 Objectif

Définir un mode d’échange simple, robuste et traçable entre WinDev et le moteur via des fichiers JSON, sans dépendance réseau.

---

## 13.2 Structure des dossiers

Arborescence standard recommandée :

```
/exchange/
    /inbox/
    /processing/
    /outbox/
    /error/
    /archive/
```

### Rôle des dossiers

| Dossier | Rôle |
|--------|------|
| inbox | dépôt des fichiers par WinDev |
| processing | fichiers en cours de traitement (verrou) |
| outbox | réponses du moteur (succès) |
| error | réponses du moteur (erreurs) |
| archive | historisation input/output |

---

## 13.3 Nommage des fichiers

### Format

```
<timestamp>_<clientId>_<scenario>_<requestId>.json
```

### Exemple

```
20260330T101530_CLIENT01_SC03_REQ12345.json
```

### Règles

- `timestamp` : UTC ISO compact
- `clientId` : identifiant client
- `scenario` : SC-01, SC-03, ...
- `requestId` : identifiant unique de requête

---

## 13.4 Format des fichiers INPUT

### Principe

Le contenu est **strictement identique** au contrat JSON API.

### Exemple

```json
{
  "requestId": "REQ12345",
  "scenarioType": "SC-03",
  "metadata": {
    "clientId": "CLIENT01",
    "source": "WINDEV",
    "timestamp": "2026-03-30T10:15:30Z"
  },
  "planningContext": { },
  "scenarioParameters": { },
  "dataSet": { }
}
```

### Champs obligatoires

- `requestId`
- `scenarioType`
- `metadata.clientId`
- `metadata.timestamp`

---

## 13.5 Format des fichiers OUTPUT

### Nommage

```
<nom_input>_RESULT.json
```

### Exemple

```
20260330T101530_CLIENT01_SC03_REQ12345_RESULT.json
```

---

### Cas succès

Le mode fichier utilise une **enveloppe de transport** spécifique afin de faciliter :
- la corrélation entrée / sortie ;
- la traçabilité d’exploitation ;
- la symétrie avec le format d’erreur.

Cette enveloppe **n’altère pas le contrat métier de sortie du moteur**.

Le moteur continue de produire son contrat métier standard :
`ScenarioResponseDTO`

Le mode fichier encapsule cette réponse dans une structure de transport :

```json
{
  "requestId": "REQ12345",
  "status": "SUCCESS",
  "response": {
    "scenarioType": "SC-03",
    "solverResult": { },
    "planning": { },
    "workMetrics": { },
    "solutionSummary": { },
    "diagnostics": { }
  }
}
```

---

### Cas erreur

```json
{
  "requestId": "REQ12345",
  "status": "ERROR",
  "error": {
    "code": "SCENARIO_VALIDATION_FAILED",
    "message": "Incohérence dans le dataset",
    "details": [ ]
  }
}
```

### Gouvernance des contrats

Le projet distingue explicitement :

- le **contrat métier de sortie** du moteur : `ScenarioResponseDTO`
- le **contrat de transport fichier** : enveloppe `requestId` / `status` / `response`

Cette distinction permet :
- de conserver un contrat métier stable ;
- d’éviter de polluer la réponse API avec des champs purement techniques ;
- d’assurer une bonne exploitabilité du mode fichier.

---

## 13.6 Workflow de traitement

### Étapes

1. WinDev écrit un fichier dans `/inbox`
2. Le moteur détecte le fichier
3. Le moteur déplace le fichier vers `/processing`
4. Traitement (validation → mapping → solveur)
5. Écriture du résultat :
   - succès → `/outbox`
   - erreur → `/error`
6. Archivage des fichiers

---

## 13.7 Règles techniques obligatoires

### 1. Écriture atomique (WinDev)

- écrire en `.tmp`
- renommer en `.json`

### 2. Verrouillage

- déplacement vers `/processing` obligatoire avant traitement

### 3. Idempotence

- `requestId` unique obligatoire
- le moteur doit tolérer une double lecture

### 4. Traçabilité

- chaque fichier contient `requestId`, `clientId`, `timestamp`

### 5. Immutabilité

- un fichier ne doit jamais être modifié après écriture

---

## 13.8 Gestion multi-clients

### Mode Full Web

- dossier partagé possible
- `clientId` obligatoire dans le JSON

### Mode SaaS

- un dossier par client recommandé

```
/exchange/clientA/
/exchange/clientB/
```

---

## 13.9 Compatibilité avec le mode API

- même JSON
- même `requestId`
- même structure de réponse

👉 Le mode fichier et le mode API partagent le même contrat métier.

---

# 🔐 14. Sécurisation du mode fichier (V1)

## 14.1 Objectif

Sécuriser les échanges WinDev ↔ moteur lorsque le transport repose sur des fichiers JSON déposés dans des répertoires d’échange.

Le mode fichier doit être considéré comme un **mode d’intégration à part entière**, avec des exigences de sécurité comparables à une API interne.

---

## 14.2 Principes de sécurité

### 1. Principe du moindre privilège

Les accès aux répertoires d’échange doivent être accordés uniquement aux comptes techniques nécessaires :
- compte de dépôt WinDev ;
- compte de traitement moteur ;
- compte d’administration / support si nécessaire.

Les droits doivent être attribués à des **groupes** et non directement à des utilisateurs lorsque cela est possible.

### 2. Séparation des rôles

Les rôles suivants doivent être distingués :
- **dépôt** des fichiers d’entrée ;
- **lecture / traitement** par le moteur ;
- **lecture des résultats** ;
- **administration / purge / archivage**.

### 3. Cloisonnement par environnement

Les répertoires d’échange doivent être séparés au minimum par :
- environnement (`DEV`, `RECETTE`, `PROD`) ;
- client si le mode SaaS impose une isolation forte.

---

## 14.3 Sécurisation des répertoires Windows

### Règles recommandées

- utiliser des ACL NTFS explicites ;
- utiliser des groupes de sécurité dédiés ;
- limiter chaque compte au strict nécessaire (`lecture`, `écriture`, `modification`) ;
- éviter les droits larges sur les partages ;
- vérifier l’**accès effectif** réel côté partage + NTFS.

### Répartition cible des droits

| Répertoire | Compte dépôt WinDev | Compte moteur | Compte support/admin |
|-----------|----------------------|--------------|----------------------|
| inbox | Écriture | Lecture + déplacement | Lecture/administration |
| processing | Aucun | Contrôle total | Administration |
| outbox | Lecture | Écriture | Lecture/administration |
| error | Lecture | Écriture | Lecture/administration |
| archive | Aucun | Écriture | Lecture/administration |

### Règle importante

Les permissions de partage et les permissions NTFS se cumulent : le niveau réellement obtenu doit être validé sur le résultat final.

---

## 14.4 Isolation multi-clients

### Mode Full Web

Deux stratégies sont possibles :

#### A. Dossier partagé unique

Acceptable si :
- `clientId` est obligatoire dans les métadonnées ;
- le moteur filtre strictement selon le contenu ;
- l’accès au partage reste réservé aux seuls comptes techniques autorisés.

#### B. Sous-dossiers par client

Préférable si le volume augmente ou si l’exploitation demande une meilleure isolation :

```
/exchange/PROD/clientA/
/exchange/PROD/clientB/
```

### Mode SaaS non web

Le cloisonnement par client est recommandé par défaut :

```
/exchange/PROD/clientA/
/exchange/PROD/clientB/
```

---

## 14.5 Intégrité d’écriture et verrouillage

### Écriture atomique côté WinDev

WinDev ne doit jamais déposer directement le fichier final.

Séquence obligatoire :
1. écriture dans un fichier temporaire (`.tmp`) ;
2. renommage en `.json` une fois l’écriture terminée.

### Verrouillage côté moteur

Le moteur ne traite jamais un fichier directement dans `inbox`.
Il doit :
1. détecter le fichier ;
2. le déplacer vers `processing` ;
3. ne commencer le traitement qu’après déplacement réussi.

Cette règle constitue le verrou logique minimal du mode fichier.

---

## 14.6 Validation défensive des fichiers

Le moteur doit contrôler systématiquement :
- extension attendue (`.json`) ;
- taille maximale autorisée ;
- structure JSON attendue ;
- présence des métadonnées obligatoires ;
- cohérence du nom de fichier avec `requestId`, `scenarioType`, `clientId` si cette convention est retenue.

Le moteur ne doit jamais faire confiance au chemin ou au nom fourni sans validation.

---

## 14.7 Protection contre les chemins et noms dangereux

Les noms de fichiers et les segments de chemin doivent être limités à un jeu de caractères autorisé.

Doivent être interdits notamment :
- `..`
- chemins absolus injectés
- séparateurs de chemin inattendus
- noms trop longs ou ambigus

Le moteur doit toujours reconstruire lui-même les chemins cibles à partir d’une racine technique connue.

---

## 14.8 Confidentialité des données

### Règles minimales

- les répertoires d’échange ne doivent pas être exposés comme répertoires web publics ;
- les fichiers archivés doivent être protégés comme des données applicatives sensibles ;
- les traces et journaux ne doivent pas recopier en clair des données sensibles inutiles ;
- les fichiers d’erreur ne doivent pas contenir de stack traces techniques destinées aux utilisateurs métiers.

### Si échange inter-serveurs

En cas d’échange via réseau entre machines distinctes, privilégier un canal sécurisé (partage Windows maîtrisé dans un réseau interne contrôlé, ou SFTP si besoin d’un transport de fichiers chiffré et cloisonné).

---

## 14.9 Traçabilité et audit

Chaque traitement doit être traçable avec au minimum :
- `requestId` ;
- `clientId` ;
- date/heure de dépôt ;
- date/heure de prise en charge ;
- statut final (`SUCCESS` / `ERROR`) ;
- chemin du fichier de sortie ;
- motif d’erreur synthétique le cas échéant.

Les journaux doivent permettre de relier :
- le fichier d’entrée ;
- le traitement moteur ;
- le fichier de sortie.

---

## 14.10 Archivage, purge et rétention

Le mode fichier produit rapidement des volumes importants.

Des règles d’exploitation doivent être définies pour :
- durée de conservation des fichiers `outbox` ;
- durée de conservation des fichiers `error` ;
- durée de conservation des archives ;
- stratégie de purge automatique ;
- éventuel chiffrement des archives si nécessaire.

Ces règles seront précisées dans la documentation de suivi et d’exploitation.

---

## 14.11 Décisions V1 retenues

- sécurité fondée sur ACL Windows + groupes techniques dédiés ;
- séparation stricte des rôles dépôt / traitement / lecture résultat / administration ;
- écriture atomique obligatoire côté WinDev ;
- déplacement vers `processing` obligatoire côté moteur avant traitement ;
- validation stricte du nom, du chemin, de l’extension et du JSON ;
- cloisonnement par environnement obligatoire ;
- cloisonnement par client recommandé, et par défaut en SaaS non web.

---

# 🗄️ 15. Archivage, purge et rétention (V1)

## 15.1 Objectif

Définir des règles simples, robustes et exploitables pour :
- conserver les fichiers utiles au support et au diagnostic ;
- éviter l’encombrement des répertoires d’échange ;
- limiter le risque de saturation disque ;
- préserver une traçabilité minimale sans conserver inutilement tous les fichiers en ligne.

Le mode fichier doit rester exploitable dans la durée : sans politique de purge, il dérive rapidement vers une accumulation non maîtrisée.

---

## 15.2 Principe général

Les répertoires opérationnels (`inbox`, `processing`, `outbox`, `error`) ne doivent pas devenir des zones de stockage de longue durée.

La logique retenue est la suivante :
- **les répertoires opérationnels servent au transit** ;
- **le répertoire `archive` sert à la conservation de courte ou moyenne durée** ;
- **la purge supprime automatiquement ce qui a dépassé la durée de rétention définie**.

---

## 15.3 Règles par répertoire

### `inbox`

- ne contient que les fichiers en attente de prise en charge ;
- aucun fichier ne doit y rester durablement ;
- tout fichier ancien y est considéré comme anormal.

**Règle V1 :**
- alerte si un fichier reste plus de **15 minutes** dans `inbox` ;
- traitement manuel ou relance d’exploitation si dépassement persistant.

### `processing`

- ne contient que les fichiers en cours de traitement ;
- tout fichier ancien y est également considéré comme anormal.

**Règle V1 :**
- alerte si un fichier reste plus de **30 minutes** dans `processing` ;
- déplacement vers `error` ou reprise manuelle selon diagnostic.

### `outbox`

- contient les résultats disponibles pour récupération par WinDev ;
- ne doit pas servir d’archive.

**Règle V1 :**
- conservation maximale : **7 jours**.

### `error`

- contient les résultats d’erreur disponibles pour analyse par WinDev ou le support ;
- doit rester accessible plus longtemps que `outbox`.

**Règle V1 :**
- conservation maximale : **30 jours**.

### `archive`

- contient les fichiers d’entrée et de sortie historisés ;
- constitue la zone de conservation de référence pour le support de niveau 1/2.

**Règle V1 :**
- conservation maximale standard : **90 jours**.

---

## 15.4 Durées de rétention retenues (V1)

| Répertoire | Usage | Durée retenue V1 |
|-----------|-------|------------------|
| inbox | transit entrant | 15 min max avant alerte |
| processing | traitement en cours | 30 min max avant alerte |
| outbox | restitution succès disponible | 7 jours |
| error | restitution erreur disponible | 30 jours |
| archive | conservation support / traçabilité | 90 jours |

---

## 15.5 Politique d’archivage

### Principe

À l’issue d’un traitement terminé :
- le fichier d’entrée doit être copié ou déplacé vers `archive` ;
- le fichier de sortie (`SUCCESS` ou `ERROR`) doit également être copié ou déplacé vers `archive` ;
- `outbox` et `error` restent des zones de consultation temporaire.

### Organisation recommandée dans `archive`

Arborescence recommandée :

```
/archive/
    /YYYY/
        /MM/
            /clientId/
```

### Exemple

```
/archive/2026/03/CLIENT01/
```

Cette organisation permet :
- une purge plus simple ;
- une navigation support plus lisible ;
- un cloisonnement naturel par client.

---

## 15.6 Politique de purge

### Principe

La purge doit être :
- **automatique** ;
- **planifiée** ;
- **journalisée** ;
- **sans intervention manuelle quotidienne**.

Microsoft documente le recours à une tâche planifiée Windows pour automatiser des suppressions périodiques de fichiers, ce qui cadre bien avec ce besoin d’exploitation. ([learn.microsoft.com](https://learn.microsoft.com/en-us/iis/manage/provisioning-and-managing-iis/managing-iis-log-file-storage?utm_source=chatgpt.com))

### Mise en œuvre recommandée

- une tâche planifiée d’exploitation exécute quotidiennement la purge ;
- la purge s’appuie sur la date de dernière modification du fichier ;
- les fichiers supprimés sont comptabilisés dans un journal technique.

La documentation PC SOFT expose bien des fonctions de gestion des dates de fichiers, ce qui confirme que ce type de logique peut aussi être piloté côté WinDev si nécessaire, même si la purge doit de préférence rester du ressort de l’exploitation moteur. ([doc.pcsoft.fr](https://doc.pcsoft.fr/fr-FR/?3036008=&name=constantes_des_fichiers_externes&utm_source=chatgpt.com))

### Fréquence retenue V1

- **purge quotidienne**, de préférence hors heures de pointe ;
- heure conseillée : **nuit applicative** (par exemple 02h00).

---

## 15.7 Suppression sécurisée

### Règles minimales

- ne purger que dans des racines techniques explicitement configurées ;
- ne jamais calculer les chemins à partir d’une entrée utilisateur non validée ;
- purger uniquement selon des règles de rétention connues ;
- journaliser chaque lot de purge (date, dossier, nombre de fichiers supprimés).

### Journal de purge minimal

Chaque exécution de purge doit produire un log contenant au minimum :
- date/heure de lancement ;
- répertoire traité ;
- règle appliquée ;
- nombre de fichiers supprimés ;
- éventuelles erreurs.

---

## 15.8 Cas anormaux à traiter explicitement

### Fichier bloqué dans `inbox`

Cas possible :
- WinDev n’a pas terminé correctement le dépôt ;
- le moteur n’a pas pris le fichier ;
- problème d’accès ou d’ACL.

**Traitement V1 :**
- pas de suppression automatique immédiate ;
- alerte d’exploitation ;
- analyse avant déplacement éventuel vers `error`.

### Fichier bloqué dans `processing`

Cas possible :
- plantage du moteur ;
- coupure serveur ;
- erreur de traitement non finalisée.

**Traitement V1 :**
- alerte d’exploitation ;
- reprise manuelle ou script de remédiation ;
- archivage ou bascule vers `error` après diagnostic.

### Fichier de sortie jamais récupéré

Cas possible :
- WinDev n’a pas consommé `outbox` ou `error`.

**Traitement V1 :**
- application de la durée de rétention normale ;
- conservation de référence dans `archive` jusqu’à échéance.

---

## 15.9 Ajustements possibles selon environnement

### Environnements non production

En `DEV` et `RECETTE`, des durées plus courtes peuvent être retenues pour limiter l’encombrement :
- `outbox` : 3 jours ;
- `error` : 7 jours ;
- `archive` : 30 jours.

### Production

En `PROD`, les durées V1 recommandées restent :
- `outbox` : 7 jours ;
- `error` : 30 jours ;
- `archive` : 90 jours.

---

## 15.10 Décisions V1 retenues

- les dossiers `inbox` et `processing` sont des zones de transit, pas de stockage ;
- alerte si un fichier reste plus de 15 min dans `inbox` ;
- alerte si un fichier reste plus de 30 min dans `processing` ;
- conservation `outbox` : 7 jours ;
- conservation `error` : 30 jours ;
- conservation `archive` : 90 jours ;
- archivage organisé par année / mois / client ;
- purge automatique quotidienne ;
- purge journalisée ;
- aucune suppression aveugle sur un chemin calculé dynamiquement.

---

# 📌 16. Prochaines étapes

- préciser les comptes techniques et groupes de sécurité ;
- rédiger le workflow d’exploitation et de reprise incident ;
- rédiger la spec OpenAPI alignée ;
- définir le mécanisme d’authentification API.

---

# 🧠 Principe directeur

> Le moteur ne doit jamais dépendre du mode d’échange.

> Le mode d’échange doit s’adapter au moteur — jamais l’inverse.

