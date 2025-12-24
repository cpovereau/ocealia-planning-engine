# Planning Engine – OptaPlanner (POC structurant)

## 🎯 Objectif du projet

Ce dépôt contient un **moteur de planification basé sur OptaPlanner**, conçu comme un **socle conceptuel et technique** pour des besoins de planification complexes (gestion de temps, affectation de ressources, aide à la décision).

L’objectif n’est **pas** de fournir immédiatement un moteur métier complet, mais de :

* valider un **modèle conceptuel robuste** ;
* éprouver la **capacité d’arbitrage** d’OptaPlanner ;
* poser les **fondations d’une future intégration** avec un outil amont (ex. WebDev / HFSQL).

---

## 🧠 Principes directeurs

* Le moteur doit **proposer une solution explicable**, pas seulement optimale.
* Les situations imparfaites sont acceptées (manque de ressources, violations contrôlées).
* Les contraintes sont **classées par nature métier** (physique, légale, métier, personnelle).
* Les variations de comportement ne dépendent pas du code, mais du **contexte de résolution**.

👉 Les décisions structurantes sont documentées dans :

```
docs/DECISIONS_CONCEPTION_OPTAPLANNER.mk
```

Ce document fait foi pour toute évolution du projet.

---

## 🧩 Modèle conceptuel (résumé)

* **Créneau** : unité de travail à affecter (entité de planification).
* **Ressource** : concept abstrait pouvant être :

  * un **salarié réel** ;
  * un **poste virtuel** (besoin non couvert) ;
  * l’état explicite **A_AFFECTER** (pas de `null`).
* **PlanningSolution** : encapsule les créneaux, les ressources et le score.

---

## 🧪 Scénarios de test

Le projet repose sur **5 scénarios de test**, chacun validant une capacité clé du moteur.

| Scénario | Fichier                              | Capacité validée                       |
| -------- | ------------------------------------ | -------------------------------------- |
| 1        | `Scenario1SimpleAffectationTest`     | Affectation simple                     |
| 2        | `Scenario2DepassementPenaliseTest`   | Limite bloquante (HARD)                |
| 3        | `Scenario3PosteVirtuelTest`          | Manque de ressource (poste virtuel)    |
| 4        | `Scenario4ArbitrageSoftTest`         | Arbitrage entre solutions imparfaites  |
| 5        | `Scenario5PreferencePersonnelleTest` | Violation d’une préférence personnelle |

📌 Ces scénarios **ne sont pas tous compatibles simultanément** :
ils ont servi à révéler et classifier les règles.

---

## ⚙️ Architecture technique

* Java 21
* OptaPlanner
* Gradle
* Tests : JUnit 5 + AssertJ

Arborescence principale :

```
src/
 ├─ main/java/com/example/planning/
 │   ├─ domain/     # Modèle métier
 │   ├─ solver/     # PlanningSolution + ConstraintProvider
 │   ├─ config/     # Configuration du solveur
 │   └─ api/        # (prévu) API d’entrée
 └─ test/java/com/example/planning/
     └─ Scenario*Test.java
```

---

## ▶️ Lancer les tests

### Prérequis

* Java 21 installé
* Git

### Commande

Sous Windows (PowerShell), à la racine du projet :

```powershell
.\gradlew test
```

Les rapports de tests sont générés dans :

```
build/reports/tests/test/index.html
```

---

## 🚧 Hors périmètre actuel

Les éléments suivants sont volontairement absents à ce stade :

* dates et heures précises ;
* temps de trajet ;
* contrats détaillés ;
* performance à grande échelle ;
* interface utilisateur.

Ils seront introduits **après validation du socle conceptuel**.

---

## 🔜 Pistes d’évolution

* Introduction d’un **PlanningContext** pour piloter les contraintes.
* Mapping WebDev / HFSQL → moteur de planification.
* Externalisation des poids de contraintes.
* Exposition du moteur via API HTTP.

---

## 📌 Statut du projet

* POC structurant
* Base pédagogique et technique
* Support de réflexion métier et d’architecture

Toute évolution majeure doit être cohérente avec les décisions documentées.

---

**Auteur** : Christophe P.
