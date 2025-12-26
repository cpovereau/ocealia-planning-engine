# ⚖️ STRATÉGIE DE SCORING — Moteur de planification

Ce document décrit la **philosophie de scoring** du moteur de planification et
liste les **indicateurs réellement utiles** pour évaluer une solution.

Il sert de référence pour :

* l’implémentation des contraintes OptaPlanner,
* la pondération des règles,
* l’explicabilité des décisions produites par le moteur.

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

### 3.1 Contraintes physiques — HARD

**Rôle** : définir l’espace des solutions possibles.

Exemples :

* chevauchement de créneaux,
* dépassement des bornes humaines absolues,
* impossibilité matérielle.

👉 Toute solution qui viole ces contraintes est rejetée.

---

### 3.2 Contraintes légales — SOFT (très fort)

**Rôle** : respecter le droit du travail autant que possible.

Exemples :

* repos quotidien insuffisant,
* repos hebdomadaire non respecté,
* dette de repos compensateur excessive,
* dépassement de contingent d’heures supplémentaires.

👉 Violables uniquement en dernier recours,
avec une pénalité élevée et visible.

---

### 3.3 Contraintes métier — SOFT (moyen)

**Rôle** : respecter l’organisation interne.

Exemples :

* continuité de service,
* règles de roulement,
* contraintes d’activité.

---

### 3.4 Contraintes de service — SOFT (faible)

**Rôle** : améliorer la qualité globale du planning.

Exemples :

* équilibrage de charge,
* limitation des écarts entre salariés.

---

### 3.5 Contraintes personnelles — SOFT (très faible)

**Rôle** : améliorer le confort individuel sans bloquer le moteur.

Exemples :

* souhaits ponctuels,
* préférences personnelles.

---

## 4. Indicateurs utilisés pour le scoring (WorkMetrics)

Les indicateurs sont des **conséquences des décisions**,
calculées à partir des affectations retenues.

Ils ne sont jamais des décisions en tant que telles.

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
