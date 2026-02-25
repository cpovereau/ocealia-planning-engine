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
  
* **Paramètres conventionnels**
  
  * RegulatoryParameters est porté par le contexte.
  * les règles de conversion (heures → repos / majorations) sont fournies par le métier (WebDev / paramétrage), le moteur ne fait que scorer / arbitrer.

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


### Seuils de tolérance

Certaines contraintes se basent sur des seuils de tolérance.
Les seuils de tolérance sont fournis par le logiciel de planning via PlanningContext. Le moteur ne définit pas de valeurs par défaut implicites : toute valeur absente ou incohérente doit être rejetée (ou remplacée par un défaut explicite documenté).

---

## 4. Invariant fondamental — Définition du travail

Cette définition constitue un invariant d’architecture. Toute règle, contrainte, métrique ou évolution future doit s’y conformer.

### 1. Définition du travail (règle unique)

Un créneau est considéré comme travaillé si et seulement si son activité compte dans la charge (compteDansCharge = true dans le référentiel d’activité).

Conséquences :
- Nature TRAVAIL → compteDansCharge = true
- Nature REPOS (RH, RHD) → false
- Nature FERIE (JF substitutif) → false
- Nature RECUP → false
- Nature NON_TRAVAILLE → false

👉 Le moteur ne déduit jamais le travail à partir du code d’activité brut.
👉 Il s’appuie uniquement sur le référentiel.

### 2. Définition du repos hebdomadaire
- RH = repos hebdomadaire du samedi (nature REPOS)
- RHD = repos hebdomadaire du dimanche (nature REPOS)

Ces codes représentent un repos attendu, pas du travail.

### 3. Définitions dérivées
- Dimanche travaillé
Dimanche travaillé Un dimanche travaillé est un dimanche calendaire (DayOfWeek.SUNDAY) comportant au moins un créneau dont l’activité compte dans la charge.

- Repos hebdomadaire travaillé
Minutes de créneaux dont l’activité compte dans la charge positionnées un samedi (Saturday) ou un dimanche (Sunday).

### 4. Principe structurant V2
- Les contraintes mesurent.
- Les ScoreWeights pondèrent.
- Les WorkMetrics constatent post-résolution.

Aucune métrique ne redéfinit la notion de travail.

📌 Cette définition devient la référence pour :
- les contraintes légales,
- les métriques V2,
- les futures métriques V3,
- et la restitution planning.

🔒 Règle de cohérence transversale
- Les contraintes n’utilisent jamais directement Nature pour déterminer le travail.
- Les WorkMetrics n’interprètent jamais QualificationJour comme du travail.
- Toute évolution V3 ou ultérieure devra s’appuyer exclusivement sur la définition canonique ci-dessus.

---

## 5. Règles fondamentales sur les contraintes

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

## 6. Rôle réel des scénarios de test (1 à 5)

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

### Décision – Alignement strict des fixtures sur le modèle métier

Les fixtures de test doivent construire des objets valides au sens du domaine.
Aucun constructeur ou raccourci ne doit être ajouté au modèle pour faciliter les tests.

---

## 7. Décision d’architecture majeure

### Principe retenu

* Un **seul moteur** de résolution.
* Un **ensemble stable de contraintes**.
* Des **variations de comportement** pilotées par le contexte.

### Mise en œuvre prévue

* Introduction d’un `PlanningContext` fourni par l’appelant (WebDev).
* Utilisation de contraintes **configurables** (poids dynamiques).
* Aucun code spécifique par scénario.

---

## 7 bis. Décision de stabilisation du scoring (V2)

### Contexte

La phase V2 du moteur de planification vise à **stabiliser le scoring** afin de :
- rendre les arbitrages explicables ;
- dissocier clairement la **mesure des violations** de leur **pondération** ;
- préparer les évolutions futures (V3+) sans dette technique.

Cette stabilisation s’appuie sur les retours des scénarios de test et sur l’exploitation des indicateurs de type *WorkMetrics*.

---

### Principe fondamental retenu

> Les contraintes **mesurent des écarts ou des volumes**,  
> Le scoring **arbitre ces mesures** via des poids centralisés.
> Les WorkMetrics sont calculées post-résolution et ne participent jamais directement au calcul du score.

En conséquence :
- aucune contrainte ne porte de coefficient métier ;
- aucune contrainte ne dépend directement d’un scénario ;
- toute pondération est centralisée et explicite.

---

### Découpage des responsabilités

Le scoring est structuré selon trois niveaux clairement séparés :

#### 1. Contraintes (mesure)

Les contraintes :
- détectent une situation (ex. travail de nuit, créneau non couvert) ;
- produisent une **mesure neutre** :
  - en **minutes** (nuit, jour férié),
  - ou en **occurrence** (créneau non couvert, poste virtuel) ;
- sont identifiées par une clé métier (`PenaliteKey`).

Aucune logique de stratégie ou de priorité n’est portée par les contraintes.

---

#### 2. ScoreWeights (pondération)

La classe `ScoreWeights` :
- centralise les **poids par type de pénalité** (`PenaliteKey`) ;
- décline ces poids selon la **stratégie de scoring** (`StrategieScoring`, utilisée comme multiplicateur global) ;
- constitue la **seule source de vérité** pour l’arbitrage relatif des pénalités.

Les stratégies actuellement définies sont :
- `EXPLOITATION` (continuité de service),
- `ANALYSE_RH` (lecture RH et équité),
- `AUDIT` (signal réglementaire).

---

#### 3. ScoreUtils (construction du score)

`ScoreUtils` est le **point de passage unique** qui :
- combine une clé de pénalité, une mesure (volume) et une stratégie ;
- applique les poids définis dans `ScoreWeights` ;
- construit le score OptaPlanner (`HardSoftScore`).

Aucune règle métier n’est implémentée à ce niveau.

---

### Décisions structurantes associées

Les décisions suivantes sont **explicitement actées** :

- Les pénibilités **Nuit** et **Jour férié** sont comptabilisées en **minutes**.
- Les situations **Créneau non couvert** et **Poste virtuel** sont comptabilisées en **occurrence**.
- Les coefficients conditionnels par scénario ont été supprimés des contraintes.
- La stratégie de scoring est un **contexte de lecture**, pas un algorithme.

---

### Validation par tests (dominance)

Les arbitrages V2 sont validés par des **tests de dominance sans solver**, qui prouvent que :
- certaines pénalités dominent structurellement d’autres (ex. non couvert ≫ nuit) ;
- les seuils d’équivalence sont cohérents avec les poids définis ;
- un changement de stratégie modifie effectivement le score produit.

Ces tests constituent des **preuves d’invariants d’arbitrage**, et non des tests d’optimisation.

---

### Conséquences pour les évolutions futures

Ce découpage permet :
- d’introduire de nouvelles pénalités sans refonte globale ;
- de modifier les poids sans toucher aux contraintes ;
- d’expliquer une solution sous la forme :
  > (clé de pénalité, unité, volume, poids, stratégie, contribution au score).

Les évolutions V3 (équité, pénibilité par occurrence, préférences) s’appuieront sur ce socle sans en modifier les principes.

---

## 8. Éléments volontairement différés

Les éléments suivants sont identifiés mais volontairement repoussés :

* gestion fine du temps (heures, chevauchements, pauses) ;
* Prise en compte décisionnelle des coûts (dette repos, heures sup) : oui, via indicateurs dérivés ; calcul exact potentiellement externalisé ;
* trajets et distances ;
* contrats de travail détaillés ;
* explication utilisateur détaillée (UI).

Ces sujets seront traités **après validation du socle conceptuel**.

---

## 9. Invariants à respecter pour la suite du projet

* Pas de `null` pour représenter une absence d’affectation.
* Toute règle doit être classable (physique / légale / métier / personnelle).
* Les arbitrages doivent être explicables.
* Les tests servent à verrouiller des capacités, pas à figer des implémentations.

---

## 10. Statut du document

* Document vivant.
* Toute remise en cause d’un invariant doit être **explicitement discutée**.
* Sert de référence pour les échanges futurs et le mapping WebDev → moteur.

---

**Fin du document**
