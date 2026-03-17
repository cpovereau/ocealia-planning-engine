# 📚 Archives des décisions techniques — Moteur de planification

Ce document conserve **les décisions techniques structurantes prises pendant le développement du moteur**.

Contrairement au journal de développement (91), ce document ne suit pas une chronologie.

Il regroupe uniquement :

* les arbitrages d’architecture
* les conventions techniques retenues
* les choix structurants du moteur

L’objectif est d’éviter de **réouvrir des décisions déjà prises** et de conserver la mémoire technique du projet.

---

# 1️⃣ Décisions liées au solveur et au scoring

## Utilisation d’OptaPlanner comme moteur de résolution

Décision :

Le moteur de planification repose sur **OptaPlanner** pour la résolution combinatoire.

Le pipeline retenu est :

```
ScenarioController
   → PlanningRequest
   → PlanningService
   → SolverLauncher
   → OptaPlanner
```

Justification :

* moteur robuste pour les problèmes combinatoires
* gestion native du scoring HARD / SOFT
* capacité d’optimisation incrémentale

---

## Modèle d’entité central : le créneau

Décision :

Le **créneau** constitue l’entité centrale du modèle de décision.

Caractéristiques :

* chaque créneau est une `PlanningEntity`
* chaque créneau reçoit **une seule ressource affectée**
* une ressource peut être :

```
salarié réel
poste virtuel
ressource non affectée
```

Justification :

Ce modèle simplifie fortement la résolution et permet de représenter explicitement les créneaux non couverts.

---

## Représentation des créneaux non couverts

Décision :

Un créneau non affecté est représenté explicitement via une ressource virtuelle.

Convention retenue :

```
A_AFFECTER
```

Justification :

* permet au solveur de produire une solution complète
* permet de pénaliser les créneaux non couverts
* facilite l’analyse des besoins non satisfaits

---

# 2️⃣ Décisions liées à l’explicabilité du moteur

## Score explicable via ScoreBreakdown

Décision :

Le moteur doit restituer **un score explicable**.

La restitution passe par une structure :

```
ScoreBreakdownItemDTO
```

Structure retenue :

```
penaliteKey
unit
quantity
weightedImpact
```

Justification :

Permettre au logiciel de planning de comprendre :

* l’origine des pénalités
* leur volume
* leur impact dans le score final

---

## `PenaliteKey` comme source de vérité

Décision :

`PenaliteKey` devient **la référence unique pour l’identification et l’unité des pénalités**.

Conséquences :

* suppression des `switch` techniques dans la restitution
* centralisation de la construction des objets `ScoreBreakdownItemDTO`

Objectif :

rendre le score **stable et interprétable**.

---

## Diagnostic d’affectation des créneaux

Décision :

Le moteur expose un diagnostic détaillé des créneaux non affectés.

Structure exposée :

```
diagnostics.assignmentDiagnostics
```

Informations fournies :

* creneauId
* date
* heureDebut / heureFin
* activité
* status
* reasonCode
* message

Diagnostics actuellement implémentés :

```
UNCOVERED
NO_RESOURCE_ASSIGNED
IMPOSSIBLE_TO_ASSIGN
```

Justification :

Faciliter l’analyse des impossibilités d’affectation.

---

# 3️⃣ Décisions liées aux règles de calcul et aux types

## Gestion des types numériques (long vs int)

Décision :

Le moteur utilise deux types numériques complémentaires.

```
long → calculs intermédiaires (durées, intersections temporelles)
int  → application des pénalités OptaPlanner (HardSoftScore)
```

Justification :

* `HardSoftScore` d'OptaPlanner impose des entiers (`int`) pour les pénalités
* les calculs temporels internes (intersections de minutes) peuvent dépasser `Integer.MAX_VALUE` sur de longues périodes
* la conversion explicite `Math.toIntExact(...)` est appliquée au moment du scoring

La stratégie retenue :

* calculs internes en `long`,
* conversion en `int` au moment de l'appel `penalize(...)`.

Une unification complète sera étudiée lors du nettoyage technique Phase 9.

---



## Calcul des pénibilités par intersections temporelles

Décision :

Les pénibilités (nuit, jours fériés, etc.) sont calculées **par intersections temporelles**.

Conséquence :

les anciennes règles spécifiques (ex : nuit entière) sont remplacées par :

```
calcul précis des minutes d’intersection
```

Justification :

* précision accrue
* modèle générique
* extensibilité pour d’autres types de pénibilités

---

## Unité de calcul interne : les minutes

Décision :

Tous les calculs internes du moteur sont réalisés en **minutes**.

Justification :

* éviter les erreurs d’arrondi
* faciliter les intersections temporelles

Les représentations en heures sont utilisées uniquement pour l’affichage.

---

## Représentation des durées

Deux formats sont utilisés :

| Format | Usage                  |
| ------ | ---------------------- |
| HH:MM  | représentation humaine |
| HH,DC  | analyse ou calculs     |

Exemples :

| Temps | HH:MM | HH,DC |
| ----- | ----- | ----- |
| 1h30  | 01:30 | 1.50  |
| 2h15  | 02:15 | 2.25  |

---

# 4️⃣ Décisions liées au DataSet d’entrée

## Migration progressive du DataSet

Décision :

Le dataset d’entrée évolue **progressivement** afin de préserver la compatibilité avec l’existant.

Stratégie retenue :

```
1. évolution compatible du schéma V1
2. adaptation du DatasetBuilder
3. introduction progressive d’un modèle V2
```

Objectif :

aligner le moteur avec les structures du logiciel de planning.

---

## Introduction des axes organisationnels

Décision :

Le dataset doit porter explicitement les axes organisationnels :

* direction
* service
* lieu
* poste comptable

Justification :

permettre des contraintes organisationnelles dans la planification.

---

## Structuration des besoins par blocs

Décision :

Les besoins doivent être structurés via :

```
groupeBesoinId
blocJourId
ordreDansBloc
estSegmentDePause
```

Justification :

permettre la modélisation :

* des blocs journaliers
* des séquences de travail
* des pauses

---

# 5️⃣ Décisions liées aux WorkMetrics

## Calcul des WorkMetrics après résolution

Décision :

Les **WorkMetrics sont calculées après la résolution**.

Conséquence :

elles sont indépendantes du solveur.

Justification :

* simplification du modèle OptaPlanner
* séparation claire optimisation / analyse RH

---

## Exposition des WorkMetrics via l’API scénario

Décision :

Les métriques calculées doivent être exposées dans la réponse du moteur.

Structure :

```
workMetrics
```

Elles incluent :

* métriques par ressource
* métriques globales

Objectif :

permettre l’analyse RH côté application.

---

# 6️⃣ Décisions de périmètre

Certaines analyses sont volontairement **hors moteur**.

Exemples :

| Sujet                 | Décision    |
| --------------------- | ----------- |
| surcharge salarié     | hors moteur |
| aide RH décisionnelle | hors moteur |

Justification :

le moteur doit rester concentré sur **la résolution du planning**.

Les analyses décisionnelles sont réalisées côté application.

---

# Conclusion

Ces décisions constituent **les fondations techniques du moteur de planification**.

Toute évolution majeure doit :

1. vérifier la compatibilité avec ces décisions
2. documenter les nouveaux arbitrages dans ce document.
