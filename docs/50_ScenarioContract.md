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

## 3.5 Évolution du DataSet amont (V2 progressive)

Le `dataSet` constitue l’interface principale entre **WebDev** et le moteur de planification.

La version initiale (V1) repose sur une structure simple :
- `creneaux`
- `ressources`
- paramètres réglementaires

Cette structure reste **le socle contractuel actuel**.

Toutefois, afin de mieux représenter les données métier issues du logiciel de planning,
une évolution progressive du DataSet est introduite.

### Objectifs

Permettre de représenter explicitement :

- les **besoins de couverture** ;
- les **affectations existantes** issues du planning ;
- les **indisponibilités** des ressources ;
- les **axes organisationnels** du logiciel métier.

### Axes organisationnels supportés

Le DataSet peut désormais porter les axes organisationnels suivants :

- `direction`
- `service`
- `lieu`
- `posteComptable`

Ces axes peuvent être présents sur :

- les ressources
- les créneaux
- les besoins

### Données portées par la ressource

Les ressources peuvent désormais transmettre :

- un bloc `contratTravail`
- un bloc `contraintesReglementaires`

Ces informations proviennent directement du logiciel de planning.

### Structuration des besoins

Afin de préparer l'évolution vers une représentation explicite des besoins,
les champs suivants peuvent être transmis sur les créneaux :

- `groupeBesoinId`
- `blocJourId`
- `ordreDansBloc`
- `estSegmentDePause`

Ces champs permettent de reconstruire la logique métier d’un besoin
sans modifier le modèle conceptuel du moteur.

### Trajectoire d'évolution

Deux niveaux sont définis :

**V1 (actuelle)**

dataSet
├─ ressources
└─ creneaux


**V2 cible**

dataSet
├─ referentiels
├─ ressources
├─ besoins
├─ affectationsExistantes
└─ indisponibilites

Cette évolution est progressive et n'affecte pas le fonctionnement
des scénarios existants.

---

## 4. Scénarios supportés (V1)

---

### 🟢 SC-01 — Conception d’un planning pour un nouveau salarié

#### 🎯 Intention métier

Construire un **planning de référence** conforme aux règles, sans historique.

#### Paramètres spécifiques

* resourceRef (SALARIE ou POSTE_VIRTUEL) ;
* période de planification (planningContext.horizon) ;
* dailyAmplitudeHours (incluant pause réglementaire) ;
* shiftStart ;
* shiftEndAlert (borne d’alerte, non bloquante) ;
* lunchBreak (optionnel, défaut 12:00–13:00) ;
* workedDays (DayOfWeek ISO : MONDAY…SUNDAY) ;
* holidayDates (jours non travaillés) ;
* codeActiviteId (code activité des créneaux générés, issu du référentiel client —
  optionnel pendant la transition, repli signalé sur `travail`).

#### Données clés transmises

* aucune affectation transmise ;
* ressource cible (salarié réel ou poste virtuel fourni) ;
* génération des créneaux réalisée côté moteur ;
* jours fériés explicitement transmis ;
* absence d’historique préalable.

#### Restitution attendue

* planning généré ;
* alertes de cohérence (pause, dépassement borne, repos insuffisant) ;
* aucune optimisation à ce stade (génération déterministe) ;
* pas encore d’indicateurs de charge.

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
