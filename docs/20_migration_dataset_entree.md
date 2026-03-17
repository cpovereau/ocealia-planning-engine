# 20 — Migration du DataSet d’entrée du moteur

Ce document décrit la **stratégie de migration et de stabilisation du DataSet d’entrée** utilisé pour alimenter le moteur de planification.

Il est construit à partir de l’analyse du document de suivi du développement et constitue la référence pour :

* l’évolution du contrat JSON entre l’application et le moteur
* l’enrichissement progressif du modèle de données du solveur
* la traçabilité des champs réellement exploités par le moteur

---

# 1. Objectif de la migration du DataSet

L’objectif est d’aligner progressivement le **contrat d’entrée du moteur** avec les structures réelles du logiciel de planning.

Le DataSet initial a été volontairement minimal afin de :

* brancher le solveur
* tester les scénarios
* valider l’architecture du moteur

La migration vise désormais à introduire les informations nécessaires à une planification réaliste.

---

# 2. Principe de migration progressive

La migration du DataSet se fait **de manière incrémentale** afin de ne jamais casser les scénarios existants.

Pipeline général :

```
JSON scénario
  → Controller
  → ScenarioDatasetBuilder
  → PlanningRequest
  → PlanningService
  → SolverLauncher
  → OptaPlanner
```

Le DatasetBuilder joue un rôle central :

* il transforme le JSON en **structures exploitables par le moteur**
* il introduit progressivement les nouvelles données

---

# 3. Évolution du DataSet amont

L’évolution du DataSet vise à intégrer les éléments structurels nécessaires à la résolution.

Principaux enrichissements identifiés :

## Axes organisationnels

Ajout d’informations organisationnelles sur les ressources :

* direction
* service
* lieu
* poste comptable

Ces axes permettront d’introduire ultérieurement des contraintes organisationnelles.

---

## Données portées par les ressources

Ajout progressif d’informations RH :

* contrat de travail
* contraintes réglementaires
* travail de nuit
* travail les jours fériés

Ces informations permettront de construire des contraintes plus réalistes.

---

## Structuration des besoins de couverture

Le modèle de besoins évolue afin de représenter correctement les blocs de travail.

Champs introduits ou prévus :

```
groupeBesoinId
blocJourId
ordreDansBloc
estSegmentDePause
```

Ces champs permettent de modéliser :

* des blocs journaliers
* des séquences de créneaux
* les pauses

---

# 4. Gestion du faux sentiment de couverture fonctionnelle

Pendant la migration du DataSet, certains champs peuvent :

* être présents dans le JSON
* être désérialisés dans les DTO
* être mappés partiellement dans le domaine

mais **ne pas encore être exploités par le moteur**.

Il est donc nécessaire de suivre explicitement :

* l’état réel d’exploitation des champs
* les redondances temporaires
* la phase cible d’activation.

---

# 5. Matrice d’exploitation des champs

Une matrice de suivi permet d’identifier les blocs réellement utilisés par le moteur.

Exemples de blocs suivis :

```
dataSet.referentiels
dataSet.indisponibilites
ressources.salaries.axesOrganisationnels
ressources.salaries.contraintesReglementaires
ressources.salaries.travailDeNuit
ressources.salaries.travailleJourFerie
```

Cette matrice permet de suivre :

* le statut d’exploitation
* les redondances temporaires
* les évolutions prévues.

---

# 6. Dataset de référence pour les scénarios

Un dataset de référence technique est utilisé afin de garantir la cohérence des tests.

Ce dataset permet :

* de tester l’alimentation WebDev → moteur
* de vérifier la compatibilité des évolutions du contrat
* de garantir la reproductibilité des scénarios.

Un alignement permanent est maintenu avec le dataset de référence **SC-03**.

---

# 7. Règles de gestion de la migration

Afin de sécuriser la migration du DataSet :

1. chaque champ critique doit être documenté dans la matrice
2. chaque évolution doit être compatible avec les scénarios existants
3. les redondances doivent être identifiées
4. chaque champ critique doit être associé à **au moins un test prouvant son exploitation réelle**.

---

# 8. Critère de stabilisation du contrat d’entrée

La migration sera considérée comme stabilisée lorsque :

* tous les champs critiques auront un statut clair
* le mapping JSON → domaine sera entièrement maîtrisé
* les redondances temporaires auront été supprimées ou documentées

---

# Conclusion

La migration du DataSet constitue une étape structurante du projet.

Elle permet de faire évoluer le moteur :

* d’un modèle de test minimal
* vers un modèle de planification complet et exploitable.

Cette évolution doit rester **maîtrisée, traçable et progressive** afin de garantir la stabilité du moteur et des scénarios de test.
