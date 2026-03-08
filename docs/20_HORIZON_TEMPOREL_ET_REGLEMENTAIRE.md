# ⏱️ Horizon temporel et réglementaire — version courte

## 1. Objectif du document

Ce document définit **le cadre temporel transmis au moteur de planification**.

Il précise :

* ce qui est **décidé côté métier (WebDev)**,
* ce qui est **consommé par le moteur (Spring Boot / OptaPlanner)**,
* et ce que le moteur **n’a pas le droit d’inventer**.

Le moteur **n’interprète pas l’intention utilisateur** :
il évalue des décisions **dans un cadre temporel explicite**.

---

## 2. Principe fondamental

> **OptaPlanner ne choisit jamais l’horizon temporel.**
> Il reçoit un cadre de résolution et juge les décisions à l’intérieur de ce cadre.

---

## 3. Origine du cadre temporel (côté WebDev)

Le cadre temporel est construit à partir de la demande utilisateur, par exemple :

* planification sur une période donnée,
* optimisation sur un cycle de plusieurs semaines,
* remplacement ponctuel entre deux dates.

WebDev est responsable de :

* l’interprétation métier de la demande,
* la construction du **PlanningContext temporel**,
* la transmission de ce contexte au moteur.

---

## 4. Contenu minimal du contexte transmis

Le moteur reçoit un contexte contenant au minimum :

### 4.1 Fenêtre de résolution

* date de début
* date de fin

Cette fenêtre définit **l’espace de décision du solveur**.

---

### 4.2 Type de résolution

Indication du cadre métier, par exemple :

* `PLANNING_GLOBAL`
* `CYCLE`
* `REMPLACEMENT`
* `PROJECTION`

Ce type peut influencer l’activation ou la pondération de certaines contraintes,
mais **ne modifie pas la logique du moteur**.

---

### 4.3 Horizon réglementaire applicable

Pour chaque grande famille de règles, l’horizon de validité est précisé :

* repos quotidien
* repos hebdomadaire
* heures supplémentaires / complémentaires
* dette de repos compensateur

Ces horizons peuvent :

* être identiques à la fenêtre de résolution,
* ou la dépasser partiellement.

---

### 4.4 Héritage du passé

Le contexte peut inclure :

* des compteurs initiaux (heures déjà réalisées, dettes existantes),
* ou une hypothèse explicite d’état neutre.

Le moteur **ne reconstitue jamais l’historique**.

---

## 5. Ce que le moteur fait (et ne fait pas)

### Le moteur :

* évalue des affectations dans la fenêtre fournie,
* calcule des indicateurs dérivés (charges, dettes, coûts),
* applique les contraintes dans les horizons définis.

### Le moteur ne :

* choisit pas la période,
* n’interprète pas l’intention utilisateur,
* n’extrapole pas au-delà des horizons transmis.

---

## 6. Invariants à respecter

* Le cadre temporel est **toujours explicite**.
* Toute dette générée doit être **visible dans les résultats**.
* Le moteur n’altère jamais le passé.
* Aucune règle ne s’applique en dehors de l’horizon déclaré.

---

## 7. Portage explicite de l’horizon et des hypothèses de résolution

L’horizon temporel, le type de résolution et les hypothèses liées à l’historique
sont des **données d’entrée explicites du moteur**.

Ils sont **portés par le `PlanningContext`** fourni par l’appelant (WebDev / API),
et **ne sont jamais déduits, recalculés ou interprétés implicitement** par le moteur
de planification.

En particulier :

- le moteur ne choisit jamais l’horizon de résolution (dates de début / fin) ;
- le type de résolution (planning global, cycle, remplacement, projection)
  est fourni explicitement ;
- les hypothèses d’historique (prise en compte du passé, compteurs initiaux, etc.)
  sont explicites et non reconstruites.

Cette approche garantit :
- la traçabilité des décisions de planification ;
- la reproductibilité des résultats ;
- l’absence de logique implicite dans le moteur.

---

## 8. Rattachement documentaire

Ce document est transversal et complète :

* le diagramme conceptuel,
* l’UML OptaPlanner,
* le pseudo-modèle métier,
* les décisions de conception.

Il sert de **référence unique** pour toute question liée au temps dans le moteur.
