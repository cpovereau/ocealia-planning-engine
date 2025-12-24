# Décisions de conception – Moteur de planification (OptaPlanner)

---

## 1. Objectif du moteur de planification

### Objectif principal

Le moteur de planification a pour objectif de **proposer une affectation cohérente de créneaux de travail à des ressources**, en tenant compte de contraintes multiples, parfois contradictoires.

Il doit :

* produire **une solution explicable**, pas seulement optimale mathématiquement ;
* accepter l’existence de **situations imparfaites** ;
* mettre en évidence les **manques de ressources** ou les **violations nécessaires**.

### Hors périmètre volontaire (à ce stade)

* calculs horaires précis (heures, durées, pauses) ;
* dates calendaires ;
* gestion fine des contrats ;
* performance et optimisation à grande échelle.

Ces éléments seront introduits **après stabilisation du modèle conceptuel**.

---

## 2. Modèle conceptuel stabilisé

### Entités principales

* **Créneau**

  * Unité élémentaire de travail à affecter.
  * Porte la variable de décision `ressourceAffectee`.

* **Ressource** (concept abstrait)

  * **Salarié réel** : personne existante.
  * **Poste virtuel** : besoin non couvert / potentiel de recrutement.
  * **État A_AFFECTER** : absence d’affectation explicite (pas de `null`).

* **PlanningSolution**

  * Contient la liste des créneaux et des ressources.
  * Porte le score OptaPlanner.

* **PlanningContext** (prévu)

  * Objet de contexte décrivant l’objectif de la demande.
  * Permettra de configurer les poids et l’activation des contraintes.

---

## 3. Typologie des contraintes (décision structurante)

Les contraintes sont classées selon **leur nature métier**, indépendamment de leur implémentation technique.

### Catégories retenues

1. **Contraintes physiques**

   * Limites impossibles à dépasser (ex. 24h dans une journée).

2. **Contraintes légales**

   * Droit du travail, temps de repos, durées maximales.

3. **Contraintes métier**

   * Organisation, fonctionnement interne, règles de service.

4. **Contraintes personnelles**

   * Préférences individuelles, souhaits, confort.

👉 Cette classification est **invariante** et sert de base à toute évolution future.

---

## 4. Règles fondamentales sur les contraintes

### Contraintes HARD

* Ne doivent jamais être violées.
* Structurent l’espace de recherche.
* Une solution qui les viole est **interdite**.

### Contraintes SOFT

* Peuvent être violées.
* Sont hiérarchisées par des poids.
* Servent à arbitrer entre plusieurs solutions imparfaites.

### Décision clé

> Les contraintes **ne dépendent pas des scénarios**, mais du **contexte de résolution**.

Les scénarios ont servi à **révéler** les contraintes, pas à les figer.

---

## 5. Rôle réel des scénarios de test (1 à 5)

Les scénarios sont des **tests de capacité du moteur**, pas une configuration définitive.

| Scénario | Rôle               | Capacité validée                                |
| -------- | ------------------ | ----------------------------------------------- |
| 1        | Découverte         | Affecter quand tout est possible                |
| 2        | Structuration      | Dire « impossible » sans tricher                |
| 3        | Aide à la décision | Modéliser un manque (poste virtuel)             |
| 4        | Scoring            | Arbitrer entre plusieurs solutions imparfaites  |
| 5        | Réalisme           | Violer une préférence personnelle si nécessaire |

### Décision explicite

> Les scénarios **ne sont pas tous compatibles simultanément**.
> Ils ont servi à comprendre et classifier les règles.

---

## 6. Décision d’architecture majeure

### Principe retenu

* Un **seul moteur** de résolution.
* Un **ensemble stable de contraintes**.
* Des **variations de comportement** pilotées par le contexte.

### Mise en œuvre prévue

* Introduction d’un `PlanningContext` fourni par l’appelant (WebDev).
* Utilisation de contraintes **configurables** (poids dynamiques).
* Aucun code spécifique par scénario.

---

## 7. Éléments volontairement différés

Les éléments suivants sont identifiés mais volontairement repoussés :

* gestion fine du temps (heures, chevauchements, pauses) ;
* trajets et distances ;
* contrats de travail détaillés ;
* explication utilisateur détaillée (UI).

Ces sujets seront traités **après validation du socle conceptuel**.

---

## 8. Invariants à respecter pour la suite du projet

* Pas de `null` pour représenter une absence d’affectation.
* Toute règle doit être classable (physique / légale / métier / personnelle).
* Les arbitrages doivent être explicables.
* Les tests servent à verrouiller des capacités, pas à figer des implémentations.

---

## 9. Statut du document

* Document vivant.
* Toute remise en cause d’un invariant doit être **explicitement discutée**.
* Sert de référence pour les échanges futurs et le mapping WebDev → moteur.

---

**Fin du document**
