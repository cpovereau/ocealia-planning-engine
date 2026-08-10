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
Pénaliser les changements de rythme entre travail de jour et travail de nuit sur des jours consécutifs.

**Definition technique**
Une transition est caractérisée lorsque :
- un salarié travaille un jour J en JOUR
- puis un jour J+1 en NUIT
La situation inverse est également à prendre en compte.

**Règle**
Pénaliser si :
- repos < seuil légal (11h)
- OU transition immédiate sans jour OFF

**Unité de pénalité**
Pénalité pondérée en cas de répétition (1 → 3 → 5)

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

## 6 bis. Bornes individuelles de durée et de repos

Ces deux règles se distinguent des précédentes sur un point : leur seuil n'est pas global mais
**porté par le salarié**, via `contraintesReglementaires`. Elles sont inactives pour tout salarié
dont le champ correspondant n'est pas renseigné.

### R12 — Repos quotidien minimum entre deux journées travaillées

**Description**
Entre la fin de la dernière activité d'une journée travaillée et le début de la première activité
de la journée travaillée suivante, une durée minimale de repos doit être respectée.

**Définition technique**
La mesure porte sur les **journées travaillées successives**, prises dans l'ordre chronologique —
et non sur les jours calendaires adjacents. Un jour de repos intercalé produit mécaniquement un
repos long, donc sans déficit ; il n'y a pas lieu de tester l'adjacence des dates.

Un créneau dont l'heure de fin n'est pas postérieure à son heure de début se termine le lendemain :
sa fin réelle est reportée de 24 h. C'est le cas d'usage principal de la règle.

**Seuil**
`contraintesReglementaires.reposQuotidienMinimum` (heures), par salarié.

**Périmètre**
Segments de pause exclus, activités `compteDansCharge = false` exclues.

**Unité de pénalité**
Minutes manquantes, cumulées sur toutes les transitions en défaut.

**Population**
Tous salariés disposant d'un seuil configuré

**Nature**
Légale

**Type**
SOFT (fort)

**Priorité relative**
Très élevée

---

### R13 — Durée hebdomadaire maximale de travail

**Description**
Limiter le volume horaire travaillé sur une semaine.

**Définition technique**
La semaine est **calendaire, du lundi au dimanche**, identifiée par la date de son lundi.
Un créneau est rattaché **en entier** à la semaine de sa `date`, y compris lorsqu'il franchit
minuit du dimanche au lundi : `Creneau.duree` est une donnée d'entrée atomique, jamais scindée
par le moteur.

La règle mesure ce qu'elle reçoit. Une semaine transmise incomplète produit un total
sous-évalué et aucun dépassement détecté — d'où l'exigence de semaine pleine formulée par SC-06.

**Seuil**
`contraintesReglementaires.heuresMaximumParSemaine` (heures), par salarié.

**Périmètre**
Segments de pause exclus, activités `compteDansCharge = false` exclues.

**Unité de pénalité**
Minutes de dépassement, par semaine en dépassement.

**Population**
Tous salariés disposant d'un seuil configuré

**Nature**
Légale / conventionnelle

**Type**
SOFT (fort)

**Priorité relative**
Très élevée

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
