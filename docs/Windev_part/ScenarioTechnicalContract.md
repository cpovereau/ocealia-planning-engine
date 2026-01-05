# 📄 ScenarioTechnicalContract.md

**Contrat technique — Alimentation du moteur de planification**

---

## 1. Principe général (non négociable)

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
* `POST /api/planning/solve`

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

📌 **Tout champ inconnu est rejeté**
📌 **Tout champ obligatoire manquant est rejeté**

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

---

## 7. Restitution (réponse API)

```json
{
  "scenarioType": "SC-05",
  "status": "SOLVED",
  "score": { ... },
  "planning": { ... },
  "workMetrics": { ... },
  "alerts": [ ... ],
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
* vous (maintenance).
