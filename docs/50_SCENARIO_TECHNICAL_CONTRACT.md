# 📄 50_SCENARIO_TECHNICAL_CONTRACT.md

**Contrat technique — Alimentation du moteur de planification**

---

## 1. Principe général

### Règles absolues

* WebDev **ne construit jamais un planning** ;
* WebDev **ne décide jamais des règles** ;
* WebDev **ne calcule jamais de charge** ;
* WebDev **ne déduit jamais une intention**.

### Rôles respectifs

**WebDev :**

* collecte ;
* qualifie ;
* transmet.

**Le moteur :**

* arbitre ;
* score ;
* explique.

---

## 2. Format d’échange

### 2.1 Protocole

* HTTP REST
* `POST /scenarios/{scenarioCode}/solve`
ex : POST /scenarios/sc01/solve

### 2.2 Format

* `Content-Type: application/json`
* Encodage UTF-8
* Dates au format **ISO-8601**

---

## 3. Structure JSON obligatoire

```json
{
  "scenarioType": "SC-01",
  "planningContext": { ... },
  "scenarioParameters": { ... },
  "dataSet": { ... }
}
```

📌 **Tout champ obligatoire manquant est rejeté**

📌 **Tout champ inconnu est rejeté** — `400 UNKNOWN_FIELD`, avec le **chemin JSON complet** du champ
fautif et la liste des noms acceptés à cet endroit. La règle vaut sur les deux canaux, HTTP et
FileAdapter, qui partagent la même désérialisation.

### La règle n'a pas toujours été tenue

Elle était annoncée depuis la phase 10B au motif que certains DTO ne portaient plus de
`@JsonIgnoreProperties`. Ce motif était faux : Spring Boot désactive
`FAIL_ON_UNKNOWN_PROPERTIES`, et retirer l'annotation ne rend donc rien strict. Constaté au lot S5
de SC-02, généralisé au **rang 11** — la strictness est désormais portée par la configuration du
contrat d'entrée, et non par la présence ou l'absence d'une annotation.

⚠️ Un contrat qui promet un refus qu'il ne prononce pas est pire qu'un contrat tolérant assumé :
l'appelant en repart avec une réponse 200 et la conviction d'avoir été entendu.

### Deux exceptions, nommées et volontaires

| Exception | Pourquoi |
|---|---|
| `priorite`, `type`, `isReposHebdo`, `axesOrganisationnels` sur un créneau | Le schéma publié les déclare « encore accepté et silencieusement ignoré ; ne plus émettre ». Ce ne sont pas des inconnus, ce sont des **retraités déclarés** : refuser une requête pour un champ que le contrat dit d'ignorer casserait une migration en cours |
| L'enveloppe de requête, `dataSet`, les salariés, les postes virtuels, les paramètres SC-03 | Ces blocs restent tolérants. Ils **ne prétendent pas le contraire** — c'est ce qui les distingue du défaut ci-dessus. Les fermer serait un changement de comportement pour SC-01, SC-03 et SC-06, à décider avec l'appelant |

---

## 4. Règles de conception du JSON

### 4.1 Règles générales

#### ✅ Autorisé

* identifiants ;
* bornes (min / max) ;
* listes autorisées ;
* flags explicites.

#### ❌ Interdit

* règles implicites ;
* champs calculés ;
* agrégats ;
* logique conditionnelle.

**Exemples interdits :**

```json
"heuresTotales": 37
"surcharge": true
"prioriteCalculee": 5
```

---

### 4.2 Gestion du temps

* Toutes les dates sont **qualifiées en amont** ;
* le moteur ne déduit jamais :

  * jour / nuit ;
  * jour férié ;
  * repos hebdomadaire.

📌 Les jours travaillés (workedDays) sont transmis sous forme ISO (DayOfWeek Java : MONDAY…SUNDAY).
Aucun format abrégé (MON, TUE…) n’est accepté dans la V1.

**Exemple attendu :**

```json
{
  "date": "2026-01-12",
  "heureDebut": "08:00",
  "heureFin": "16:00",
  "isJourFerie": false,
  "isReposHebdo": false,
  "segmentNuit": false
}
```

---

### 4.3 Créneaux (`dataSet.creneaux`)

#### Règles

* un créneau = un besoin ;
* aucun créneau n’appartient à un salarié ;
* aucune affectation n’est fournie par WebDev.

📌 Dans SC-01, aucun créneau n’est fourni par WebDev.
Les créneaux sont générés par le moteur à partir des paramètres du scénario.

---

### 4.4 Ressources

#### Salarié réel

* transmis comme **fait immuable** ;
* jamais modifié par le moteur.

#### Poste virtuel

* jamais créé côté WebDev **sauf scénario explicitement autorisé** ;
* sinon **révélé par le moteur**.

---

## 5. Spécificités par scénario

Chaque `scenarioType` impose :

* une **liste fermée** de paramètres autorisés ;
* une **liste obligatoire** de paramètres requis.

**Exemple (SC-05) :**

```json
"scenarioParameters": {
  "salarieAId": "SAL001",
  "salarieBId": "SAL002",
  "objectif": "EQUITE_CHARGE",
  "autoriserDesequilibre": true
}
```

---

## 6. Validation côté API (Spring Boot)

Avant toute résolution :

1. validation JSON (structure) ;
2. validation scénario ↔ paramètres ;
3. validation de cohérence métier ;
4. rejet explicite en cas d’erreur.

👉 **Aucune donnée invalide n’arrive au solveur**.

SC-01 V1 ne déclenche pas le solveur OptaPlanner.
Il exécute uniquement une phase de génération déterministe.

---

## 7. Restitution (réponse API)

```json
{
  "scenarioType": "SC-05",
  "status": "SOLVED",
  "score": { ... },
  "planning": { ... },
  "workMetrics": { ... },
  "alerts": [ {
      "code": "SHIFT_END_EXCEEDED",
      "severity": "WARNING",
      "date": "2026-02-23",
      "message": "Fin prévue (...)"
    } ],
  "explanations": [ ... ]
}
```

📌 Le moteur **explique**, il ne tranche pas à la place du métier.

---

## 8. Invariants techniques

* aucun calcul métier côté WebDev ;
* aucun champ optionnel ambigu ;
* aucune logique cachée dans le JSON ;
* tout est traçable et loggable ;
* tout est versionné.

---

## 9. Ce que ce contrat protège

* le moteur (stabilité) ;
* WebDev (simplicité) ;
* le projet (évolutivité) ;
* maintenance.
