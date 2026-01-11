# Stabilisation ScoreWeights & exploitation WorkMetrics V2 — Référentiel V2

> **Objectif** : rendre le scoring **lisible, explicable et maîtrisable** en s’appuyant sur **WorkMetrics V2**, **sans ajouter de nouvelles règles métier**.

Ce document formalise :
- le rôle de **ScoreWeights**,
- la liste des **PenaliteKey** (V2),
- la **table minimale** de pondération validée,
- le maintien de **StrategieScoring** (multiplicateur),
- la **couverture** (existant / à brancher / à venir),
- les points **volontairement écartés**.

---

## 1) Cadre et invariants

### 1.1 Séparation stricte des couches

| Couche | Rôle | Interdit |
|---|---|---|
| **Contraintes** | Interdire (HARD) ou pénaliser (SOFT) une situation | Porter des poids globaux “magiques” |
| **SeuilsDeTolerance** | Dire **quand** une situation est problématique (bornes) | Porter des poids, des arbitrages |
| **Penalites** | Dire **quelle pénalité logique** appliquer | Calculer des scores, décider de la hiérarchie |
| **ScoreWeights** | Dire **à quel point c’est grave** (pondération technique stable) | Porter des règles métier / seuils |
| **WorkMetrics** | Constats post-résolution pour explicabilité & analyse | Déclencher exclusion / piloter décisions |

Principe directeur :
> **Le moteur juge. Il ne calcule pas le métier.**

### 1.2 Interdits V2
- ❌ Ajouter une nouvelle règle métier.
- ❌ Introduire des seuils numériques dans `ScoreWeights`.
- ❌ Faire dépendre les poids de WorkMetrics.
- ❌ Laisser des coefficients “cachés” dispersés dans les contraintes.

### 1.3 Choix sémantique validé (V2)
- **NUIT** et **FÉRIÉ** sont scorés en **MINUTES** (coût volumétrique/exposition).
- `NON_COUVERT` et `POSTE_VIRTUEL` restent scorés en **OCCURRENCE** (événements structurants).

---

## 2) Rôle de ScoreWeights (V2)

### 2.1 Définition
`ScoreWeights` est une **table de pondération technique**.

Il sert à :
- garantir une hiérarchie d’arbitrage **stable**,
- rendre les compromis **explicables**,
- assurer qu’**à règles identiques**, deux développeurs interprètent les poids **de la même manière**.

### 2.2 Ce que ScoreWeights contient
- des poids **explicites** par **PenaliteKey** (SOFT)
- des multiplicateurs explicites par **StrategieScoring** (si conservé)

### 2.3 Ce que ScoreWeights ne contient pas
- ❌ aucune règle métier
- ❌ aucun seuil
- ❌ aucune condition (pas de `if` métier)
- ❌ aucune dépendance aux WorkMetrics

---

## 3) PenaliteKey — liste normalisée

> Une `PenaliteKey` identifie **ce qui est pénalisé** (pas pourquoi, pas quand, pas combien).

### 3.1 Bloc HARD — Faisabilité (non pondéré)
Clés présentes pour la classification/explicabilité (pas de ScoreWeights).

**PHYS**
- `PHYS_HARD_CRENNEAUX_OVERLAP`
- `PHYS_HARD_DUREE_MAX_CRENEAU`
- `PHYS_HARD_CUMUL_JOURNALIER_MAX`

**LEGAL**
- `LEGAL_HARD_DUREE_MAX_LEGALE_PAR_PERIODE`
- `LEGAL_HARD_NUITS_CONSECUTIVES_MAX`
- `LEGAL_HARD_REPOS_OBLIGATOIRE_APRES_NUITS`

### 3.2 Bloc SOFT — Arbitrage (pondéré)
Clés SOFT **minimales V2**.

**METIER**
- `METIER_SOFT_CRENEAU_NON_COUVERT` *(unité : occurrence)*
- `METIER_SOFT_AFFECTATION_POSTE_VIRTUEL` *(unité : occurrence)*

**LEGAL (atypiques)**
- `LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES` *(unité : minute)*
- `LEGAL_SOFT_TRAVAIL_NUIT_MINUTES` *(unité : minute)*

**(Option Lot B ultérieur — légal minimal)**
- `LEGAL_SOFT_DEPASSEMENT_DIMANCHES_MAX` *(unité : jour distinct)*

---

## 4) Table minimale ScoreWeights V2 (validée)

> Ces valeurs forment la **baseline V2** (SoftScore).

### 4.1 Invariants d’arbitrage
1. `NON_COUVERT` domine tout le reste (éviter quasi à tout prix).
2. `POSTE_VIRTUEL` est préférable au non couvert mais doit rester coûteux/visible.
3. Entre atypiques : `FÉRIÉ` ≥ `NUIT` en coût volumétrique.

### 4.2 Valeurs

| PenaliteKey | Unité | Poids V2 |
|---|---:|---:|
| `METIER_SOFT_CRENEAU_NON_COUVERT` | occurrence | **10 000** |
| `METIER_SOFT_AFFECTATION_POSTE_VIRTUEL` | occurrence | **2 000** |
| `LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES` | minute | **5** |
| `LEGAL_SOFT_TRAVAIL_NUIT_MINUTES` | minute | **3** |

### 4.3 Lecture (équivalences utiles)
- 8h férié = 480 min × 5 = 2 400
- 8h nuit = 480 min × 3 = 1 440
- 1 poste virtuel = 2 000
- 1 non couvert = 10 000

---

## 5) StrategieScoring (conservé)

### 5.1 Statut
`StrategieScoring` est conservé comme **multiplicateur explicite**.

### 5.2 Règle V2 (normative)
- Les contraintes calculent uniquement :
  - des **unités** (minutes / occurrences / jours distincts)
  - associées à une **PenaliteKey**
- `ScoreWeights` applique :
  - le **poids** de la PenaliteKey
  - × le **multiplicateur** de la stratégie

> Aucun coefficient spécifique ne doit rester “caché” dans une contrainte.

---

## 6) Mapping contraintes → PenaliteKey (V2)

### 6.1 Contraintes SOFT déjà branchées (actives aujourd’hui)

| Contrainte | PenaliteKey | Unité |
|---|---|---|
| `CreneauNonAffecte` | `METIER_SOFT_CRENEAU_NON_COUVERT` | occurrence |
| `AffectationPosteVirtuel` | `METIER_SOFT_AFFECTATION_POSTE_VIRTUEL` | occurrence |
| `CreneauDeNuit` | `LEGAL_SOFT_TRAVAIL_NUIT_MINUTES` | minute |
| `CreneauJourFerie` | `LEGAL_SOFT_TRAVAIL_JOUR_FERIE_MINUTES` | minute |

### 6.2 Contraintes HARD (actives ou candidates)

| Contrainte | PenaliteKey | Unité |
|---|---|---|
| `NuitsConsecutivesMax` | `LEGAL_HARD_NUITS_CONSECUTIVES_MAX` | séquence |
| `ReposObligatoireApresNuits` | `LEGAL_HARD_REPOS_OBLIGATOIRE_APRES_NUITS` | occurrence/minute |
| `DureeMaximaleLegaleParSalarie` | `LEGAL_HARD_DUREE_MAX_LEGALE_PAR_PERIODE` | minute |

---

## 7) Couverture — existant / à réaliser / à venir

### 7.1 Existant (dans le moteur)
- WorkMetrics V1 + V2 calculées post-résolution.
- Contraintes physiques HARD existantes.
- Une partie des contraintes légales HARD existe.
- Contraintes métier SOFT actives : non couvert / poste virtuel / nuit / férié.

### 7.2 À réaliser (V2 — indispensable, cohérent)
1. **Normaliser les PenaliteKey** côté scoring.
2. **Aligner** nuit & férié sur une pénalisation **en minutes**.
3. **Centraliser** les coefficients de stratégie dans `ScoreWeights` (plus de coefficients dispersés).
4. Écrire des tests ciblés “dominance” (voir §8).

### 7.3 À venir (hors V2)
- Exploitation plus large des WorkMetrics (V3 : séquences, équité).
- Contraintes de service/perso (couverture minimale, préférences) une fois ScoreWeights stabilisé.
- Explicabilité détaillée (restitution structurée des violations et unités).

---

## 8) Tests ciblés (niveau V2) — dominance / comparaison

> Objectif : **comparer** des solutions (A domine B) sans tester l’optimum.

### 8.1 Lot A — HARD (faisabilité)
- A respecte vs B viole :
  - `LEGAL_HARD_NUITS_CONSECUTIVES_MAX`
  - `LEGAL_HARD_REPOS_OBLIGATOIRE_APRES_NUITS`
  - `LEGAL_HARD_DUREE_MAX_LEGALE_PAR_PERIODE`

Assertion : `HardScore(B) < HardScore(A)`.

### 8.2 Lot SOFT V2 — arbitrage
- `NON_COUVERT` : +1 occurrence doit dominer les écarts atypiques.
- `POSTE_VIRTUEL` : arbitrage face à atypiques testable.
- `NUIT_MINUTES` : +60 minutes doit dégrader le soft.
- `FERIE_MINUTES` : +60 minutes doit dégrader le soft.

Assertion : `HardScore(A)==HardScore(B)` et `SoftScore(B) < SoftScore(A)`.

---

## 9) Points volontairement écartés (V2)

- ❌ Calcul de paie / primes / valorisation financière exacte.
- ❌ Nouvelles règles combinatoires (jours consécutifs, alternance jour/nuit, amplitude) tant que V3 n’est pas lancé.
- ❌ Pénibilité “par occurrence” pour nuit/férié (prévu V3 via clés dédiées si besoin).
- ❌ Service & personnel (couverture minimale, préférences) avant stabilisation complète ScoreWeights.
- ❌ WorkMetrics utilisées comme déclencheurs de faisabilité (HARD).

---

## 10) Décisions figées (résumé)

1. **NUIT/FÉRIÉ en minutes** (V2).
2. Table ScoreWeights V2 validée : 10 000 / 2 000 / 5 / 3.
3. `StrategieScoring` conservé comme multiplicateur, centralisé.
4. `PenaliteKey` devient l’unité de lecture commune du scoring.
5. On branche uniquement l’indispensable ; le reste est reporté.

