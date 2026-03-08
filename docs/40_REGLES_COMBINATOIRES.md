# 📚 Règles combinatoires — Référentiel de planification

Ce document recense les **règles combinatoires actuellement prises en compte** dans le moteur de planification.

Il précise pour chaque règle :

* sa description métier,
* la population concernée,
* sa nature (physique / légale / métier / service),
* son **caractère HARD ou SOFT**,
* sa **priorité relative**.

Ce document est aligné avec :

* le référentiel métier (ZZ_T),
* le modèle OptaPlanner,
* les décisions de conception existantes.

---

## 0. Cadre général

Une **règle combinatoire** est une règle qui s’applique à une **séquence de créneaux affectés à une même ressource**.

Elle dépend :

* du temps,
* de l’ordre des créneaux,
* du profil salarié,
* du contexte réglementaire.

👉 Ces règles sont implémentées **exclusivement dans le ConstraintProvider**.
👉 Elles **ne nécessitent pas de nouveaux WorkMetrics** à ce stade.

---

## 1. Jours consécutifs travaillés (travail de jour)

### R1 — Nombre maximal de jours consécutifs travaillés

**Description**
Un salarié travaillant en régime de jour ne peut pas enchaîner un nombre excessif de jours travaillés sans repos hebdomadaire.

**Population**
Travailleurs de jour

**Nature**
Légale / conventionnelle

**Type**
SOFT (fort)

**Priorité relative**
Très élevée

---

### R2 — Repos dominical après longue séquence

**Description**
Après une longue période travaillée, le repos hebdomadaire doit inclure un dimanche.

**Population**
Travailleurs de jour

**Nature**
Légale

**Type**
SOFT (fort)

**Priorité relative**
Très élevée

---

## 2. Travail de nuit

### R3 — Nombre maximal de nuits consécutives

**Description**
Limiter le nombre de nuits consécutives travaillées.

**Population**
Travailleurs de nuit / créneaux de nuit

**Nature**
Légale

**Type**
HARD

**Priorité relative**
Absolue

---

### R4 — Repos obligatoire après séquence de nuits

**Description**
Après une séquence de nuits consécutives, une période minimale de repos doit être respectée.

**Population**
Travailleurs de nuit

**Nature**
Légale / métier

**Type**
HARD

**Priorité relative**
Très élevée

---

## 3. Alternance jour / nuit

### R5 — Transition jour → nuit pénalisée

**Description**
Limiter les transitions brutales entre travail de jour et travail de nuit.

**Population**
Tous salariés

**Nature**
Physique / santé

**Type**
SOFT (fort)

**Priorité relative**
Élevée

---

### R6 — Transition nuit → jour encadrée

**Description**
Après une nuit travaillée, la reprise en journée doit être retardée.

**Population**
Tous salariés

**Nature**
Physique / légale

**Type**
SOFT (fort)

**Priorité relative**
Élevée

---

## 4. Repos hebdomadaire

### R7 — Repos hebdomadaire minimal sur fenêtre glissante

**Description**
Sur toute période glissante définie, un repos hebdomadaire doit exister.

**Population**
Tous salariés

**Nature**
Légale

**Type**
HARD

**Priorité relative**
Absolue

---

### R8 — Travail sur repos hebdomadaire exceptionnel

**Description**
Le travail sur repos hebdomadaire est autorisé mais fortement pénalisé.

**Population**
Tous salariés

**Nature**
Légale / service

**Type**
SOFT (fort)

**Priorité relative**
Très élevée

---

## 5. Travail du dimanche

### R9 — Fréquence maximale de dimanches travaillés

**Description**
Limiter le nombre de dimanches travaillés sur une période donnée.

**Population**
Tous salariés

**Nature**
Conventionnelle / métier

**Type**
SOFT

**Priorité relative**
Moyenne

---

## 6. Amplitude et fragmentation

### R10 — Amplitude journalière maximale

**Description**
Limiter l’amplitude entre la première et la dernière prise de poste d’une journée.

**Population**
Tous salariés

**Nature**
Physique / confort

**Type**
SOFT

**Priorité relative**
Moyenne

---

### R11 — Fragmentation excessive d’une journée

**Description**
Pénaliser les journées comportant trop de créneaux distincts.

**Population**
Tous salariés

**Nature**
Service / confort

**Type**
SOFT

**Priorité relative**
Faible

---

## 7. Règles volontairement hors périmètre

Les règles suivantes sont connues mais **non implémentées à ce stade** :

* calcul précis des pauses légales,
* modulation annuelle,
* conventions collectives spécifiques,
* calcul détaillé des repos compensateurs par origine,
* cycles multi-semaines figés.

---

## 8. Synthèse

* Les règles combinatoires sont **centralisées** et **classées**.
* Les contraintes HARD structurent l’espace des solutions.
* Les contraintes SOFT permettent l’arbitrage.
* Les priorités relatives guident la pondération du scoring.

Ce document constitue la **référence pour l’implémentation des contraintes de séquence** dans le moteur OptaPlanner.
