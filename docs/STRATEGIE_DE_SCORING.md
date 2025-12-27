# ⚖️ STRATÉGIE DE SCORING — Moteur de planification

Ce document décrit la **philosophie de scoring** du moteur de planification et
liste les **indicateurs réellement utiles** pour évaluer une solution.

Il sert de référence pour :

* l’implémentation des contraintes OptaPlanner,
* la pondération des règles,
* l’explicabilité des décisions produites par le moteur.

Le scoring est la résultante des pénalités définies :
 * Penalites exprime l’importance relative des règles métier.
 * ScoreWeights traduit ces pénalités en pondérations techniques du score.
Les deux sont volontairement séparés afin de préserver la lisibilité métier et la stabilité du moteur.

## Rôle du score dans le moteur de planification

Le score produit par le moteur de planification a pour unique objectif
d’arbitrer entre plusieurs solutions valides.

Il ne constitue pas :
- une mesure de conformité réglementaire,
- une évaluation RH,
- un indicateur de surcharge individuelle,
- ni un score métier interprétable tel quel.

Toute lecture métier du planning repose sur les WorkMetrics,
calculées après résolution.

---

## 1. Principe fondamental

> Le moteur ne cherche pas une solution parfaite,
> mais la **solution la moins mauvaise**,
> en rendant visibles les compromis réalisés.

Le score est l’**expression chiffrée** de ces compromis.

---

## 2. Rôle du scoring dans le moteur

Le scoring permet de :

* comparer des solutions imparfaites,
* arbitrer entre des violations concurrentes,
* rendre mesurable le coût des décisions.

Il ne sert pas à :

* calculer la paie,
* appliquer exhaustivement le droit du travail,
* masquer des impossibilités métier.

---

## 3. Hiérarchie des contraintes

Les contraintes sont classées selon leur **gravité**.

### 3.1 Contraintes HARD

Les contraintes HARD représentent des règles impératives :
- légales,
- réglementaires,
- ou structurelles.

Toute solution violant une contrainte HARD est considérée comme invalide
et éliminée par le solveur.

Exemples :
- dépassement du nombre maximal de nuits consécutives,
- non-respect d’un repos obligatoire,
- incompatibilité ressource / activité.


---

### 3.2 Contraintes SOFT

Les contraintes SOFT représentent des préférences ou des objectifs
d’amélioration de la qualité du planning.

Elles permettent :
- d’arbitrer entre plusieurs solutions valides,
- de favoriser des solutions plus équilibrées,
- sans jamais invalider une solution légale.

Exemples :
- approche d’un seuil réglementaire,
- répartition plus équitable des nuits,
- limitation du recours aux postes virtuels.

---

## 4. Indicateurs utilisés pour le scoring (WorkMetrics)

Les indicateurs sont des **conséquences des décisions**,
calculées à partir des affectations retenues.

Ils ne sont jamais des décisions en tant que telles.

## Pondération technique du score (ScoreWeights)

La pondération du score est assurée par le composant `ScoreWeights`.

`ScoreWeights` :
- est strictement technique,
- ne porte aucune règle métier,
- traduit les pénalités métier (`Penalites`) en pondérations du score OptaPlanner.

Il permet notamment :
- de garantir la domination absolue des contraintes HARD,
- d’adapter l’importance relative des contraintes SOFT,
- de faire varier le comportement du solveur selon la `StrategieScoring`.

La relation entre les concepts est volontairement unidirectionnelle :

Penalites → ScoreWeights → Score OptaPlanner

## Score et WorkMetrics : séparation des responsabilités

Le score est utilisé exclusivement par le moteur pour comparer des solutions.

Les WorkMetrics :
- ne participent pas au calcul du score,
- ne sont pas modifiées par les contraintes,
- sont calculées après résolution.

Elles constituent le support unique de :
- l’explicabilité,
- l’analyse métier,
- la restitution RH.

Il n’existe volontairement aucune correspondance directe
entre une valeur de score et un indicateur métier.

---

### 4.1 Indicateurs indispensables (socle)

Ces indicateurs sont **nécessaires** pour un moteur réaliste.

| Indicateur                  | Description                      | Utilisation dans le score |
| --------------------------- | -------------------------------- | ------------------------- |
| `heuresTravaillees`         | Heures totales sur la période    | Charges globales          |
| `heuresNuit`                | Heures en plage de nuit          | Contraintes légales       |
| `heuresJourFerie`           | Heures travaillées un jour férié | Coût / dette              |
| `heuresReposHebdoTravaille` | Travail sur repos hebdomadaire   | Dette repos               |
| `heuresSupplementaires`     | Heures > durée contractuelle     | Coût / contingent         |
| `heuresComplementaires`     | Heures compl. temps partiel      | Coût                      |
| `detteReposCompensateur`    | Heures de repos à récupérer      | Pénalité forte            |

---

### 4.2 Indicateurs utiles mais non bloquants

Ces indicateurs améliorent l’arbitrage mais peuvent être introduits plus tard.

| Indicateur                   | Description                    |         |
| ---------------------------- | ------------------------------ | ------- |
| `joursConsecutifsTravailles` | Nombre de jours consécutifs    | Fatigue |
| `variationsAmplitude`        | Variations d’horaires          | Confort |
| `desiquilibreCharge`         | Écart de charge entre salariés | Équité  |
| `tauxOccupation`             | Charge vs capacité cible       | Aide RH |

---

### 4.3 Indicateurs volontairement exclus

Ces éléments sont **hors périmètre** du moteur :

* calcul de rémunération,
* calcul de primes,
* valorisation financière exacte,
* gestion fine des contrats.

Ils peuvent exister **en aval**, hors du solveur.

---

## 5. Pondération relative (exemple)

Les valeurs exactes ne sont pas figées,
mais les **ordres de grandeur** sont structurants.

| Catégorie   | Ordre de grandeur |
| ----------- | ----------------- |
| Physique    | HARD              |
| Légal       | -10 000           |
| Dette repos | -5 000            |
| Métier      | -1 000            |
| Service     | -100              |
| Personnel   | -10               |

👉 Ce tableau exprime une **priorité relative**,
pas une implémentation chiffrée définitive.

---

## 6. Invariants de scoring

* Les contraintes physiques sont inviolables.
* Les contraintes légales sont prioritaires sur toute autre.
* Les dettes doivent toujours être visibles dans les résultats.
* Le score doit rester explicable.
* Une solution peut être acceptée même imparfaite.

---

## 7. Lien avec les autres documents

Ce document complète :

* le diagramme conceptuel,
* le modèle UML,
* `HORIZON_TEMPOREL_ET_REGLEMENTAIRE.md`,
* les décisions de conception.

Il sert de référence pour toute évolution des règles de scoring.

## Évolution progressive de la stratégie de scoring

La stratégie de scoring évolue par paliers, en cohérence avec les WorkMetrics.

### Phase actuelle
- contraintes HARD stabilisées
- premières contraintes SOFT
- ScoreWeights défini mais usage limité
- aucune interprétation métier du score

### Phase suivante
- enrichissement des contraintes combinatoires
- premières contraintes SOFT d’équité
- utilisation maîtrisée de ScoreWeights

### Phase ultérieure
- WorkMetrics complètes (séquences, équité, contractuel)
- analyse métier aval (SurchargeSalarie)
- scénarios comparatifs
