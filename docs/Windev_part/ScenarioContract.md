# 📄 ScenarioContract.md

**Contrat de scénarios — Interface WebDev ↔ Moteur de planification**

---

## 1. Objectif du document

Ce document définit les **scénarios de résolution exposés par le moteur de planification**.

Il précise, pour chaque scénario :

* l’intention métier ;
* les paramètres attendus ;
* les données transmises au moteur ;
* la forme de la restitution attendue.

👉 Ce document **ne décrit ni l’IHM, ni l’implémentation OptaPlanner**.
Il constitue un **contrat fonctionnel stable** entre WebDev et Spring Boot.

---

## 2. Principe structurant

> Le moteur de planification **ne reçoit jamais une demande libre**.
> Il reçoit **un scénario identifié**, avec un périmètre et des paramètres contrôlés.

**Conséquences :**

* l’utilisateur choisit **un scénario métier** ;
* chaque scénario :

  * ouvre certains paramètres ;
  * en interdit d’autres ;
* le solveur **ne déduit jamais l’intention utilisateur**.

---

## 3. Structure générique d’un scénario

Chaque appel au moteur respecte la structure logique suivante :

```json
{
  "scenarioType": "SCENARIO_CODE",
  "planningContext": { ... },
  "scenarioParameters": { ... },
  "dataSet": { ... }
}
```

### 3.1 `scenarioType`

Identifiant **fermé et versionné** du scénario métier.

### 3.2 `planningContext`

Cadre commun à tous les scénarios :

* horizon temporel ;
* stratégie de scoring ;
* seuils de tolérance ;
* options d’explicabilité.

### 3.3 `scenarioParameters`

Paramètres **spécifiques au scénario**, validés côté API.

### 3.4 `dataSet`

Données métier normalisées :

* créneaux ;
* ressources ;
* paramètres réglementaires.

---

## 4. Scénarios supportés (V1)

---

### 🟢 SC-01 — Conception d’un planning pour un nouveau salarié

#### 🎯 Intention métier

Construire un **planning de référence** conforme aux règles, sans historique.

#### Paramètres spécifiques

* salarié concerné ;
* période de planification ;
* jours travaillables autorisés ;
* bornes horaires (jour / semaine / mois) ;
* lieux autorisés ;
* exclusions horaires ;
* répartition cible par lieu (optionnel).

#### Données clés transmises

* créneaux générables ;
* salarié réel ;
* absence d’historique préalable.

#### Restitution attendue

* planning proposé ;
* indicateurs de charge ;
* alertes légales / métier ;
* créneaux non affectés (le cas échéant).

---

### 🟡 SC-02 — Remplacement d’un salarié absent

#### 🎯 Intention métier

Assurer la continuité de service **en perturbant le moins possible l’existant**.

#### Paramètres spécifiques

* salarié absent ;
* période d’absence ;
* liste de remplaçants autorisés ;
* seuil de surcharge acceptable ;
* autorisation de poste virtuel (oui / non).

#### Données clés transmises

* créneaux imposés existants ;
* planning initial partiellement figé ;
* ressources disponibles.

#### Restitution attendue

* planning ajusté ;
* différences avant / après ;
* niveaux de surcharge par salarié ;
* volume de besoin résiduel (poste virtuel).

---

### 🔵 SC-03 — Ajustement ponctuel / événementiel

#### 🎯 Intention métier

Traiter un **déséquilibre localisé** sans recalcul global.

#### Paramètres spécifiques

* période courte ;
* lieux concernés ;
* contraintes temporaires ;
* priorité de couverture.

#### Données clés transmises

* sous-ensemble de créneaux ;
* ressources locales.

#### Restitution attendue

* planning local corrigé ;
* zones impactées identifiées ;
* compromis réalisés.

---

### 🔴 SC-04 — Optimisation globale d’un planning existant

#### 🎯 Intention métier

Améliorer un planning réel **sans le reconstruire entièrement**.

#### Paramètres spécifiques

* degré de liberté (créneaux figés / ajustables) ;
* priorités d’optimisation ;
* pondération des règles.

#### Données clés transmises

* planning existant complet ;
* historique des compteurs.

#### Restitution attendue

* planning optimisé ;
* gains / régressions explicitées ;
* indicateurs comparatifs.

---

### 🟣 SC-05 — Arbitrage de répartition horaire / lieu entre deux salariés

#### 🎯 Intention métier

Arbitrer une **répartition équitable ou optimale** entre deux salariés concurrents
pour un même périmètre de travail.

👉 Ce scénario **ne crée pas de nouveaux besoins** :
il arbitre **l’affectation relative**.

#### Cas d’usage typiques

* deux salariés sur un même site ;
* rééquilibrage de charge ;
* conflit de préférences ;
* arbitrage équité vs compétence.

#### Paramètres spécifiques

* salarié A ;
* salarié B ;
* période concernée ;
* lieux et activités communs ;
* objectif principal :

  * équité de charge ;
  * minimisation de surcharge ;
  * respect de préférences ;
* autorisation de déséquilibre contrôlé.

#### Données clés transmises

* créneaux communs ou concurrents ;
* historique de charge des deux salariés ;
* seuils comparatifs.

#### Restitution attendue

* répartition proposée ;
* indicateurs comparatifs A / B ;
* justification des arbitrages ;
* alertes d’inéquité résiduelle.

📌 **Point important**
Ce scénario **ne nécessite aucune nouvelle variable de décision** :
il exploite les mêmes affectations que les autres scénarios,
mais avec une **lecture comparative ciblée**.

---

## 5. Invariants du contrat

* Un scénario = une intention métier claire ;
* aucun scénario ne modifie le modèle conceptuel ;
* aucun paramètre libre n’est transmis au solveur ;
* toute décision est explicable via indicateurs ;
* le moteur ne refuse pas : il **rend visible l’impossible**.

---

## 6. Évolutivité

* ajout de scénario → versionnement explicite ;
* aucun scénario ne doit introduire :

  * de nouvelles décisions ;
  * des règles implicites ;
* les scénarios pilotent :

  * le contexte ;
  * la pondération ;
  * la restitution.

---

## 7. Rôle respectif des couches

### WebDev

* interprète la demande utilisateur ;
* choisit le scénario ;
* construit les paramètres ;
* interprète les résultats.

### API Spring Boot

* valide le contrat ;
* protège le moteur ;
* adapte les données.

### Moteur de planification

* arbitre ;
* score ;
* explique ;
* n’interprète jamais l’intention.

---

## 8. Statut du document

* Document structurant ;
* référence contractuelle ;
* toute modification est une **décision d’architecture**.
