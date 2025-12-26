# ⚠️ Seuils de surcharge salarié — Cadre de décision

Ce document formalise les **seuils de surcharge applicables aux salariés réels** dans le moteur de planification.

Il précise :

* ce qu’est une surcharge au sens du moteur,
* les différents **niveaux de surcharge** (alerte / SOFT / HARD),
* les indicateurs mobilisés (WorkMetrics),
* le rôle respectif du moteur et du métier.

Il complète directement :

* le référentiel métier (ZZ_T),
* `REGLES_COMBINATOIRES.md`,
* `WORKMETRICS.md`.

---

## 1. Principe fondamental

> Le moteur **ne décide jamais qu’un salarié est surchargé**.
> Il **constate**, **évalue** et **pénalise** des situations de surcharge.

La surcharge est :

* un **signal métier**,
* une **conséquence des décisions d’affectation**,
* un **élément d’aide à la décision**, pas une interdiction automatique.

---

## 2. Définition générique de la surcharge salarié

Une **surcharge salarié** est constatée lorsque :

* un ou plusieurs **seuils de référence** sont dépassés,
* sur une **période donnée**,
* pour un **salarié réel**,
* sans que la situation soit nécessairement physiquement impossible.

---

## 3. Niveaux de surcharge

Trois niveaux sont distingués.

---

### 3.1 Niveau 1 — Alerte (informationnelle)

**Objectif**
Rendre visible une tension sans influencer fortement le solveur.

**Nature**
Signal métier / indicateur

**Traitement moteur**

* aucune interdiction
* pénalité faible ou nulle

**Exemples de déclencheurs**

* charge légèrement supérieure à la moyenne collective
* première occurrence de travail sur repos hebdomadaire
* dette de repos compensateur faible

**Usage principal**
UI / reporting / explicabilité

---

### 3.2 Niveau 2 — Surcharge pénalisée (SOFT)

**Objectif**
Décourager les solutions reposant sur une surcharge répétée ou significative.

**Nature**
Contraintes légales / métier

**Traitement moteur**

* pénalité SOFT significative
* arbitrage avec d’autres violations

**Exemples de déclencheurs**

* heures supplémentaires élevées
* dette de repos compensateur importante
* travail fréquent sur repos hebdomadaire
* déséquilibre de charge persistant

**Conséquence attendue**

* préférence pour une autre répartition
* révélation potentielle d’un poste virtuel

---

### 3.3 Niveau 3 — Surcharge critique (HARD)

**Objectif**
Exclure les situations inacceptables.

**Nature**
Physique / légale

**Traitement moteur**

* contrainte HARD
* solution rejetée

**Exemples de déclencheurs**

* dépassement absolu de bornes humaines
* absence totale de repos hebdomadaire
* dépassement maximal de nuits consécutives

---

## 4. Indicateurs mobilisés (WorkMetrics)

Les seuils s’appuient exclusivement sur des **WorkMetrics existants**.

### 4.1 Indicateurs principaux

* `heuresTravaillees`
* `heuresSupplementaires`
* `heuresComplementaires`
* `heuresNuit`
* `heuresJourFerie`
* `heuresReposHebdoTravaille`
* `detteReposCompensateur`

📌 Aucun indicateur spécialisé par origine (ex. dimanche) n’est requis.

---

## 5. Surcharge et règles combinatoires

Les règles combinatoires peuvent :

* déclencher une surcharge SOFT,
* ou devenir directement HARD.

Exemples :

* trop de nuits consécutives → HARD
* trop de jours consécutifs → SOFT fort
* alternance jour / nuit brutale → SOFT

👉 La surcharge est donc souvent **le résultat** d’une règle combinatoire violée.

---

## 6. Surcharge persistante et poste virtuel

Principe structurant :

> Une surcharge persistante sur un salarié réel est un **signal de besoin structurel**.

Conséquences :

* le moteur peut préférer affecter un poste virtuel,
* même si le salarié réel reste théoriquement disponible,
* afin de réduire la surcharge globale.

La surcharge alimente donc **l’aide à la décision RH**, sans automatisme.

---

## 7. Rôle respectif moteur / métier

### Le moteur :

* évalue les dépassements de seuils,
* applique pénalités et exclusions,
* rend les surcharges visibles dans les résultats.

### Le métier (WebDev / utilisateur) :

* définit les seuils exacts,
* interprète les alertes,
* arbitre entre surcharge acceptée et besoin RH.

---

## 8. Invariants

* Aucun statut « salarié surchargé » n’est stocké.
* Toute surcharge est explicable par des indicateurs.
* Les seuils sont paramétrables via le contexte.
* Le moteur ne masque jamais une surcharge critique.

---

## 9. Statut du document

* Document de référence.
* Stable conceptuellement.
* Les valeurs numériques exactes sont hors périmètre moteur.

Ce document sert de **cadre commun** pour toute discussion future sur la surcharge salarié.
