# 📊 WorkMetrics — Définition partagée

Ce document définit **WorkMetrics**, l’ensemble des indicateurs dérivés utilisés par le moteur
pour **évaluer** une solution (scoring), sans jamais devenir des décisions.

---

## 1. Rôle et statut

* **Statut** : `ProblemFact` (lu par le solveur, jamais modifié par lui)
* **Nature** : agrégats dérivés des affectations
* **Usage exclusif** : scoring et explicabilité

> WorkMetrics rendent visibles les **conséquences** des décisions
> (coûts, dettes, charges), pas les décisions elles‑mêmes.

---

## 2. Portée temporelle

Chaque instance de WorkMetrics est **liée à :**

* un **salarié** (ou une ressource virtuelle agrégée),
* une **période** (issue du `PlanningContext`),
* un **type de résolution** (planning global, cycle, remplacement).

---

## 3. Champs retenus (socle)

### 3.1 Identification

| Champ            | Type | Description                                         |
| ---------------- | ---- | --------------------------------------------------- |
| `resourceId`     | UUID | Salarié réel ou identifiant agrégé                  |
| `periodeDebut`   | Date | Début de période                                    |
| `periodeFin`     | Date | Fin de période                                      |
| `resolutionType` | Enum | PLANNING_GLOBAL / CYCLE / REMPLACEMENT / PROJECTION |

---

### 3.2 Charges horaires

| Champ                       | Type    | Description                           |
| --------------------------- | ------- | ------------------------------------- |
| `heuresTravaillees`         | Decimal | Total heures affectées sur la période |
| `heuresNuit`                | Decimal | Heures en plage de nuit               |
| `heuresJourFerie`           | Decimal | Heures sur jours fériés               |
| `heuresReposHebdoTravaille` | Decimal | Travail sur repos hebdomadaire        |

---

### 3.3 Heures contractuelles

| Champ                     | Type    | Description                            |
| ------------------------- | ------- | -------------------------------------- |
| `heuresSupplementaires`   | Decimal | Heures au‑delà du contrat              |
| `heuresComplementaires`   | Decimal | Heures complémentaires (temps partiel) |
| `depassementContingentHS` | Decimal | Heures au‑delà du contingent           |

---

### 3.4 Dettes et coûts

| Champ                    | Type    | Description                         |
| ------------------------ | ------- | ----------------------------------- |
| `detteReposCompensateur` | Decimal | Heures de repos à récupérer         |
| `detteReposNuit`         | Decimal | Part liée au travail de nuit        |
| `detteReposFerie`        | Decimal | Part liée aux jours fériés          |
| `coutDirect`             | Decimal | Coût payé (abstrait, non financier) |
| `coutIndirect`           | Decimal | Coût différé (repos, fatigue)       |

---

## 4. Champs optionnels (phase 2)

À introduire **après stabilisation**.

| Champ                        | Type    | Usage                   |
| ---------------------------- | ------- | ----------------------- |
| `joursConsecutifsTravailles` | Integer | Fatigue / légalité      |
| `amplitudeMaxJour`           | Decimal | Confort                 |
| `variabiliteHoraires`        | Decimal | Qualité planning        |
| `tauxOccupation`             | Decimal | Aide RH (poste virtuel) |

---

## 5. Champs explicitement exclus

WorkMetrics **n’incluent pas** :

* rémunérations,
* primes,
* calculs de paie,
* valorisation financière réelle,
* gestion contractuelle fine.

Ces éléments relèvent du métier, hors moteur.

---

## 6. Calcul et mise à jour (principe)

* Les champs sont **dérivés des affectations**
* Calculés **dans les contraintes** (Constraint Streams)
* Recalcul incrémental recommandé

> Le moteur ne reçoit jamais WorkMetrics « pré‑calculés »
> en fonction d’une solution figée.

---

## 7. Invariants

* Aucun champ de WorkMetrics n’est une décision
* Toute dette générée doit être traçable à des affectations
* Les indicateurs sont bornés à l’horizon transmis
* Le score doit pouvoir expliquer chaque champ

---

## 8. Lien documentaire

WorkMetrics est référencé par :

* `UML_Optaplanner.md`
* `STRATEGIE_SCORING.md`
* `HORIZON_TEMPOREL_ET_REGLEMENTAIRE.md`

Il constitue la **référence unique** pour les indicateurs du moteur.
