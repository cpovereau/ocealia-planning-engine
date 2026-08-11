# 📚 Documentation — Moteur de planification

Ce dossier contient la documentation technique et métier du moteur de planification.

Les documents sont organisés par **niveau de responsabilité** afin de faciliter la navigation et la compréhension globale du moteur.

# Diagramme de navigation

```mermaid
flowchart TD

subgraph S00["🧭 00 — Introduction"]
  A[Principe OptaPlanner]
end

subgraph S10["🧠 10 — Référentiel métier"]
  B1[Glossaire des concepts]
  B2[Référentiel métier]
  B3[Seuils surcharge salarié]
end

subgraph S20["🏗 20 — Architecture"]
  C1[Architecture moteur]
  C2[Décisions de conception]
  C3[Dataset Builder]
  C4[Planning Context]
end

subgraph S30["⚙️ 30 — Modèle de résolution"]
  D1[Modèle conceptuel]
  D2[UML OptaPlanner]
  D3[UML solveur simplifié]
end

subgraph S40["📊 40 — Contraintes et scoring"]
  E1[Règles combinatoires]
  E2[Stratégie de scoring]
  E3[WorkMetrics]
end

subgraph S50["🔌 50 — Contrats et interface"]
  F1[Contrat de scénario]
  F2[Contrat de réponse]
  F3[Interface WinDev ↔ Moteur]
end

subgraph S60["🧪 60 — Tests"]
  G1[Stratégie de test]
end

subgraph S90["📍 90 — État du moteur"]
  H1[Suivi développement]
end

subgraph S91["📜 91 — Journal"]
  I1[Journal développement]
end

subgraph S92["🗂 92 — Cadrages et audits"]
  J1[Cadrages de chantier]
  J2[Audits et références]
  J3[Archives décisions]
end

A --> B1
B1 --> C1
C1 --> D1
D1 --> E2
E2 --> F1
F1 --> G1
G1 --> H1
H1 --> I1
I1 --> J1
```
---

# Rôle des familles documentaires

La documentation du moteur de planification est structurée en plusieurs familles
de documents ayant chacune un rôle précis.

Cette séparation vise à éviter les confusions entre :
- règles métier
- décisions d’architecture
- état du développement
- historique du projet.

---

## Série 00 — Introduction

Le document de la série **00** constitue le point d’entrée du projet.

Il explique :
- le fonctionnement général du moteur
- les principes OptaPlanner
- la logique d’ensemble avant toute lecture technique.

---

## Série 10 — Référentiel métier

Les documents de la série **10** définissent le vocabulaire et les règles métier partagés.

Ils contiennent :
- les définitions canoniques des concepts (créneau, ressource, score…)
- le référentiel d’activités utilisé par le moteur
- les seuils métier de surcharge.

Ces documents font autorité pour le vocabulaire du projet.

---

## Série 20 — Décisions d’architecture

Les documents de la série **20** décrivent les **choix de conception
structurants du moteur**.

Ils contiennent :
- les invariants d’architecture
- les conventions techniques
- les décisions structurantes
- les règles de séparation des responsabilités.

Ces documents font autorité pour l’architecture du moteur.

Ils ne contiennent jamais :
- d’historique de développement
- d’état d’avancement
- de roadmap.

---

## Série 30 — Modèle de résolution

Les documents de la série **30** décrivent le modèle de données du solveur.

Ils contiennent :
- le modèle conceptuel du domaine
- les diagrammes UML du monde OptaPlanner
- les vues simplifiées de la structure de résolution.

Ces documents servent de référence visuelle pour comprendre les objets manipulés par le solveur.

---

## Série 40 — Référentiel fonctionnel du moteur

Les documents de la série **40** décrivent les **concepts métier
et la logique fonctionnelle du moteur**.

Ils définissent notamment :
- les WorkMetrics
- la stratégie de scoring
- les indicateurs métier
- les principes d’évaluation d’un planning.

Ces documents servent de référence pour l’implémentation
des règles dans le moteur.

---

## Série 50 — Interfaces et contrats

Les documents de la série **50** définissent les contrats d’échange entre le moteur et ses appelants.

Deux sous-ensembles coexistent :
- les **contrats fonctionnels stables** (`50_SCENARIO_*`) : référence normative indépendante de l’intégrateur
- les **documents d’intégration WinDev** (`50_INTERFACE_WINDEV_MOTEUR*`, `50_ECHANGE_WINDEV_MOTEUR_FICHIER_API.md`) : implémentation concrète, exemples, jalons de migration.

Ces documents ne contiennent jamais de règles métier ni de décisions d’architecture.

---

## Série 60 — Tests

Le document de la série **60** définit la stratégie de test du moteur.

Il précise :
- les niveaux de test et leurs garanties
- ce que les tests prouvent et ne prouvent pas
- les conventions de nommage et d’organisation.

---

## Série 90 — Pilotage du développement

Les documents de la série **90** décrivent :
- l’état réel du moteur
- les capacités implémentées
- les fonctionnalités manquantes
- les jalons de développement.

Ils ne redéfinissent jamais les règles métier ni l’architecture.

Ils doivent rester alignés avec le code.

---

## Série 91 — Journal de développement

Les documents de la série **91** conservent la mémoire du projet :
- étapes de développement
- validations techniques
- décisions prises pendant l’implémentation.

Ils ne constituent pas la source de vérité actuelle du moteur.

---

## Série 92 — Cadrages, audits et archives

Les documents de la série **92** portent la mémoire des chantiers : ce qui a été constaté avant
d’agir, ce qui a été arbitré, et pourquoi.

Plusieurs sont des **instantanés datés**. Ils décrivent un état passé et ne font pas autorité sur
le code actuel — chacun le signale en tête de document. Les confondre avec l’état réel du moteur
est l’erreur de lecture que cette série doit éviter : pour l’état réel, lire la série **90**.

---

## Convention de nommage

Les documents de ce dossier portent un nom **en majuscules, sans accents**, préfixé du numéro de
série : `NN_SUJET_DU_DOCUMENT suivi de .md`.

Cette règle n’est pas cosmétique. Le dépôt est configuré `core.ignorecase=true` : la casse d’un
nom de fichier peut diverger entre le disque et l’index git **sans que rien ne le signale**, et
un lien correct sur un poste devient mort sur un autre. C’est arrivé à
`90_SUIVI_DEVELOPPEMENT_MOTEUR.md`, suivi en minuscules par git pendant que le disque le portait
en majuscules. Les accents posaient le même problème d’encodage selon les outils.

Un test verrouille la convention et l’index — voir `60_TESTING_STRATEGY_ENGINE.md`. Deux
exceptions assumées : les artefacts machine (`*.schema.json`, `*.yaml`), dont le nom est une
référence externe, et le sous-répertoire `Windev_part/`, partagé avec une autre équipe.

---

# 🧭 0 — Introduction

| Document                        | Description                                                                          |
| ------------------------------- | ------------------------------------------------------------------------------------ |
| `00_PRINCIPE_OPTAPLANNER.md`    | Introduction au fonctionnement du moteur et aux principes OptaPlanner                |

---

# 🧠 10 — Référentiel métier

| Document                         | Description                               |
| -------------------------------- | ----------------------------------------- |
| `10_GLOSSAIRE_CONCEPTS.md`       | Glossaire des concepts fondamentaux       |
| `10_REFERENTIEL_METIER.md`       | Référentiel métier utilisé par le moteur  |
| `10_SEUILS_SURCHARGE_SALARIE.md` | Seuils métier liés à la surcharge salarié |

---

# 🏗 20 — Architecture du moteur

| Document                                 | Description                                                |
| ---------------------------------------- | ---------------------------------------------------------- |
| `20_DECISIONS_CONCEPTION_OPTAPLANNER.md` | Décisions d’architecture et choix techniques du moteur                   |
| `20_ARCHITECTURE_MOTEUR.md`              | Vue globale de l’architecture du moteur                                  |
| `20_DATASET_BUILDER.md`                  | Construction du monde solveur à partir du contrat scénario               |
| `20_PLANNING_CONTEXT.md`                 | Contexte de résolution : horizon temporel, cadre réglementaire, scoring  |
| `20_MIGRATION_DATASET_ENTREE.md`         | Evolution du dataset d’entrée                                            |

Les document '20_' donnent les principes et invariants de l'information.

---

# ⚙️ 30 — Modèle de résolution

| Document                      | Description                       |
| ----------------------------- | --------------------------------- |
| `30_MODELE_CONCEPTUEL.md`     | Modèle conceptuel du moteur       |
| `30_DIAGRAMME_CONCEPTUEL.md`  | Diagrammes conceptuels du domaine |
| `30_UML_OPTAPLANNER.md`       | UML du modèle OptaPlanner         |
| `30_UML_SOLVER_SIMPLIFIE.md` | Vue simplifiée du solveur         |

---

# 📊 40 — Contraintes et scoring

| Document                     | Description                             |
| ---------------------------- | --------------------------------------- |
| `40_REGLES_COMBINATOIRES.md` | Contraintes combinatoires du moteur     |
| `40_STRATEGIE_DE_SCORING.md` | Stratégie de scoring et pondérations    |
| `40_WORKMETRICS.md`          | Calcul des métriques RH post-résolution |

---

# 🔌 50 — Interface moteur & contrats d’échange

Le moteur de planification s’appuie sur trois contrats principaux :
1. un contrat d’entrée décrivant le scénario à résoudre,
2. un contrat technique décrivant la structure JSON utilisée par le moteur,
3. un contrat de sortie décrivant la solution produite par le solveur.

Ces contrats constituent l’interface officielle entre le logiciel de planning
et le moteur de planification.

> **Note de lecture** : Les documents `50_SCENARIO_*` définissent le contrat
> fonctionnel stable (référence normative). Les documents `50_INTERFACE_WINDEV_MOTEUR*`
> décrivent l’implémentation concrète de ce contrat entre WinDev et le moteur,
> y compris les phases de migration et les exemples d’intégration.

### Contrats fonctionnels

| Document                            | Description                                |
| ----------------------------------- | ------------------------------------------ |
| `50_SCENARIO_CONTRACT.md`            | Contrat fonctionnel d’entrée du moteur     |
| `50_SCENARIO_TECHNICAL_CONTRACT.md`   | Spécification technique du contrat         |
| `50_SCENARIO_RESPONSE_CONTRACT.md`    | Contrat de sortie du moteur                |
| `50_ScenarioContract.schema.json`   | Schéma JSON du contrat d’entrée            |
| `50_ScenarioResponse.schema.json`   | Schéma JSON de la réponse                  |

### Interface WinDev ↔ Moteur

| Document                                      | Description                                              |
| --------------------------------------------- | -------------------------------------------------------- |
| `50_INTERFACE_WINDEV_MOTEUR.md`               | Vue d’ensemble, chaîne de traitement, principes          |
| `50_INTERFACE_WINDEV_MOTEUR_CONTRAT.md`        | Endpoint, structure requête/réponse, règles, jalons      |
| `50_INTERFACE_WINDEV_MOTEUR_CONTRAT_DETAIL.md` | Détail champ par champ, contraintes de validation        |
| `50_INTERFACE_WINDEV_MOTEUR_TESTS.md`          | Batterie de tests automatisés de l’interface             |
| `50_INTERFACE_WINDEV_MOTEUR_EXEMPLES.md`       | Exemples curl, JSON requête, JSON réponse complet        |
| `50_ECHANGE_WINDEV_MOTEUR_FICHIER_API.md`      | Échange par fichier — cadrage du mode d’intégration alternatif à l’API |
| `50_openapi_windev_moteur_v_1.yaml`            | Spécification OpenAPI de l’API du moteur                 |

---

# 🧪 60 — Stratégie de test

| Document                        | Description                 |
| ------------------------------- | --------------------------- |
| `60_TESTING_STRATEGY_ENGINE.md` | Stratégie de test du moteur |

---

# 📍 90 — Suivi de développement

| Document                                             | Description                 |
| ---------------------------------------------------- | --------------------------- |
| `90_SUIVI_DEVELOPPEMENT_MOTEUR.md`                   | État d’avancement du moteur, lot par lot — porte le **backlog consolidé** (§ C), seule liste ouverte du projet |
| `90_PLAN_MIGRATION_TEMPORAIRE_WINDEV_VERS_MOTEUR.md` | Jalons de la migration WinDev → moteur |
| `90_SUIVI_ECHANGE_WINDEV_MOTEUR_FICHIER_API.md`      | Suivi du chantier d’échange par fichier |

---

# 📜 91 — Journal de développement

| Document                             | Description                 |
| ------------------------------------ | --------------------------- |
| `91_JOURNAL_DEVELOPPEMENT_MOTEUR.md` | Historique du développement, étape par étape |

---

# 🗂 92 — Cadrages, audits et archives

Les documents de la série **92** portent la mémoire des chantiers : ce qui a été constaté,
ce qui a été arbitré, et pourquoi. Plusieurs sont des **instantanés datés** — ils décrivent
un état passé et ne font pas autorité sur le code actuel ; ils le disent en tête de document.

### Cadrages

| Document                                          | Description |
| ------------------------------------------------- | ----------- |
| `92_CADRAGE_DONNEES_AMONT_SCENARIOS.md`           | Données amont attendues de WinDev par scénario (SC-01 → SC-05) : état vérifié, arbitrages ouverts, découpage |
| `92_CADRAGE_SCENARIO_SC-02.md`                    | Cadrage SC-02 — remplacement d’un salarié absent : arbitrages tranchés (existant figé, reprise partielle à 30 min, jamais zéro solution), conséquences du découpage sur le modèle, découpage en lots S0→S5 |
| `92_CADRAGE_SCENARIO_SC-06.md`                    | Cadrage SC-06 — désignation de la ressource la plus à même de couvrir un besoin : intention, arbitrages tranchés, contrats d’entrée/sortie, découpage en lots S1→S7 |
| `92_CADRAGE_SOCLE_REGLEMENTAIRE.md`               | Socle réglementaire — lots S7 (contraintes dormantes) puis S8 (cadre réglementaire au contrat, calculs faux, restitution des constats). Journal des écarts de score, lot par lot |
| `92_CADRAGE_STABILISATION_SC-01.md`               | Cadrage du chantier de stabilisation SC-01 |
| `92_CADRAGE_STABILISATION_CONTRAT_ENTREE_TEMP.md` | Cadrage du chantier de stabilisation du contrat d’entrée cible |

### Audits et références de contrat

| Document                      | Description |
| ----------------------------- | ----------- |
| `92_CONTRAT_ENTREE_SC03.md`   | **Référence intégrateur** — statut de chaque champ du contrat d’entrée SC-03 (SUPPORTÉ / TOLÉRÉ / DÉPRÉCIÉ / IGNORÉ) |
| `92_AUDIT_CONTRAT_ENTREE.md`  | Instantané daté (2026-03-20) — état des lieux factuel du contrat d’entrée, partiellement obsolète |
| `92_AUDIT_SCENARIO_SC-01.md`  | Instantané daté (2026-03-27) — audit SC-01, largement traité depuis |

### Suivis de chantier

| Document                                    | Description |
| ------------------------------------------- | ----------- |
| `92_SUIVI_STABILISATION_SC-01.md`           | Pilotage du chantier SC-01 — phases A→D, closes le 2026-03-30 |
| `92_SUIVI_STABILISATION_CONTRAT_ENTREE.md`  | Pilotage du chantier de stabilisation du contrat d’entrée |

### Archives

| Document                                  | Description              |
| ----------------------------------------- | ------------------------ |
| `92_ARCHIVES_DES_DECISIONS_TECHNIQUES.md` | Historique des décisions techniques structurantes |

---

# 🤖 Contexte agent

| Document                             | Description |
| ------------------------------------ | ----------- |
| `AI_CONTEXT_CLAUDE_CODE_MOTEUR.md`   | Contexte d’architecture, invariants et règles de modification pour une intervention assistée sur le moteur |

---

# 🤝 Documents partagés avec l’équipe WinDev

Le sous-répertoire `Windev_part/` contient les artefacts échangés avec l’équipe WinDev —
schémas JSON, jeux de référence et notices. **Ses noms de fichiers ne suivent pas la convention
de la documentation moteur** : ce sont des références externes, que le moteur ne renomme pas
unilatéralement.

| Document | Description |
| --- | --- |
| `Windev_part/SC-06/sc_06_notice_integration.md` | Notice d’intégration SC-06 pour WinDev : exigences, conventions, lecture de la réponse |
| `Windev_part/SC-01/sc_01_cadrage_produit.md`    | Cadrage produit SC-01 côté WinDev |
| `Windev_part/SC-01/sc-01_mapping_ui.md`         | Correspondance écran ↔ contrat pour SC-01 |
| `Windev_part/SC-03/sc_03_cadrage_produit_et_mapping_ui_json.md` | Cadrage produit et correspondance écran ↔ JSON pour SC-03 |

---

# 🔍 Je cherche…

| Question | Document |
| --- | --- |
| Qu’est-ce qu’un créneau, une ressource, un score ? | `10_GLOSSAIRE_CONCEPTS.md` |
| Quelles contraintes le moteur applique-t-il ? | `40_REGLES_COMBINATOIRES.md` |
| Quel JSON envoyer au moteur ? | `50_INTERFACE_WINDEV_MOTEUR_EXEMPLES.md` |
| Quelle est la structure de la réponse API ? | `50_SCENARIO_RESPONSE_CONTRACT.md` |
| Qu’est-ce qui est implémenté aujourd’hui ? | `90_SUIVI_DEVELOPPEMENT_MOTEUR.md` |
| Pourquoi telle décision d’architecture ? | `20_DECISIONS_CONCEPTION_OPTAPLANNER.md` |
| Comment le moteur calcule les heures de nuit ? | `40_WORKMETRICS.md §2` |
| Que signifie le score renvoyé ? | `40_STRATEGIE_DE_SCORING.md` |
| Comment tester l’interface bout en bout ? | `50_INTERFACE_WINDEV_MOTEUR_TESTS.md` |
| Quelle est la prochaine étape de développement ? | `90_SUIVI_DEVELOPPEMENT_MOTEUR.md` |

---

# 🧭 Parcours de lecture par profil

### Nouveau contributeur au projet

1. `00_PRINCIPE_OPTAPLANNER.md` — comprendre le moteur
2. `10_GLOSSAIRE_CONCEPTS.md` — vocabulaire partagé
3. `20_ARCHITECTURE_MOTEUR.md` — vue globale de la chaîne
4. `30_UML_SOLVER_SIMPLIFIE.md` — modèle visuel
5. `90_SUIVI_DEVELOPPEMENT_MOTEUR.md` — état réel aujourd’hui

### Développeur moteur (contraintes, scoring, WorkMetrics)

1. `20_DECISIONS_CONCEPTION_OPTAPLANNER.md` — invariants à respecter
2. `40_REGLES_COMBINATOIRES.md` — règles métier
3. `40_STRATEGIE_DE_SCORING.md` — logique de scoring
4. `40_WORKMETRICS.md` — métriques post-résolution
5. `20_DATASET_BUILDER.md` — construction du monde solveur
6. `60_TESTING_STRATEGY_ENGINE.md` — stratégie de test

### Intégrateur WinDev

1. `50_INTERFACE_WINDEV_MOTEUR.md` — vue d’ensemble
2. `50_INTERFACE_WINDEV_MOTEUR_CONTRAT.md` — endpoint, règles, jalons
3. `50_INTERFACE_WINDEV_MOTEUR_CONTRAT_DETAIL.md` — détail des champs
4. `50_INTERFACE_WINDEV_MOTEUR_EXEMPLES.md` — exemples JSON complets
5. `50_SCENARIO_RESPONSE_CONTRACT.md` — structure de la réponse

### Chef de projet / pilotage

1. `90_SUIVI_DEVELOPPEMENT_MOTEUR.md` — état et roadmap
2. `90_PLAN_MIGRATION_TEMPORAIRE_WINDEV_VERS_MOTEUR.md` — jalons d’intégration
3. `40_STRATEGIE_DE_SCORING.md` — logique fonctionnelle

---

## Gouvernance documentaire

| Document | Rôle |
| --- | --- |
| `20_DECISIONS_CONCEPTION_OPTAPLANNER.md` | invariants et décisions d’architecture |
| `90_SUIVI_DEVELOPPEMENT_MOTEUR.md` | état réel du moteur |
| `91_JOURNAL_DEVELOPPEMENT_MOTEUR.md` | historique du développement |
| `92_ARCHIVES_DES_DECISIONS_TECHNIQUES.md` | décisions techniques structurantes |
| `40_STRATEGIE_DE_SCORING.md` | fonctionnement théorique du scoring |

Principe fondamental :
- **20_DECISIONS explique les invariants d’architecture**
- **92 explique pourquoi une décision a été prise**
- **40 décrit comment fonctionne le modèle**
- **90 décrit ce qui est réellement implémenté**
- **91 garde la mémoire des étapes du projet**

Une règle métier ou une implémentation ne doit apparaître **que dans le document correspondant à son niveau** afin d’éviter les divergences documentaires.