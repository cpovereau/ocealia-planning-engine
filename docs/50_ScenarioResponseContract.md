# 📄 ScenarioResponseContract.md

**Contrat technique — Restitution du moteur de planification**

Ce document sert de **description du contenu du fichier `ScenarioResponse.schema.json`** qui contient la réponse fournie par le moteur de planification.

## Principe de conception

La réponse du moteur est structurée en quatre blocs fonctionnels indépendants.

| Bloc          | Rôle                                                                   |
|---------------|------------------------------------------------------------------------|
| solverResult	| explique comment le solveur évalue la solution                         |
| planning      |	contient les décisions produites par le moteur                         |
| workMetrics   |	décrit les conséquences du planning sur les ressources                 |
| diagnostics   |	fournit des informations techniques utiles pour l’analyse et le debug  |

Cette séparation garantit que :
- le moteur reste un moteur d’optimisation,
- l’analyse RH reste une couche aval.

## 1 solverResult — Évaluation du solveur

Ce bloc expose le résultat du solveur OptaPlanner.

Il contient :
- le statut de résolution
- le score final
- un détail agrégé des pénalités

Exemple :
"solverResult": {
  "status": "SOLVED",
  "score": {
    "hard": 0,
    "soft": -120
  },
  "solverDurationMillis": 1850
}

Le score comporte deux dimensions :

| Type |	Signification             |
|------|----------------------------|
| HARD |	contraintes obligatoires  |
| SOFT |	qualité de la solution    |

Une solution valide doit toujours avoir : hard = 0

## 2 planning — Solution produite

Ce bloc contient le planning résultant de la résolution.

Il correspond à la décision principale du moteur :
  quel salarié est affecté à quel créneau.

Exemple :
"planning": {
  "creneaux": [
    {
      "id": "SC01-2026-02-23-001",
      "ressourceAffecteeId": "1041"
    }
  ]
}

Dans le périmètre actuel du moteur, les caractéristiques temporelles des créneaux (date, heure de début, heure de fin, type) sont considérées comme des données d’entrée figées.
Le moteur optimise uniquement leur affectation aux ressources.

Cette décision relève du modèle actuel.
D’autres scénarios futurs pourraient introduire des variables de décision supplémentaires, par exemple l’ajustement des horaires ou la génération dynamique de créneaux.

## 3 workMetrics — Conséquences du planning

Les WorkMetrics décrivent les effets du planning sur chaque ressource.

Elles sont calculées après la résolution et permettent :
- d’expliquer le score
- d’analyser la charge de travail
- de préparer les analyses RH futures.

Exemple :
{
  "resourceId": "1041",
  "heuresTravaillees": 35.5,
  "heuresNuit": 6,
  "nbDimanchesTravailles": 1
}

Les WorkMetrics :
- ne modifient jamais le planning
- ne déclenchent aucune contrainte
- servent uniquement à décrire la solution produite.

## 4 diagnostics — Informations techniques

Ce bloc contient des informations utiles pour comprendre l’exécution du moteur.

Exemples :
- créneaux ignorés
- activités inconnues
- créneaux hors horizon
- alertes de cohérence

Ces informations sont particulièrement utiles :
- en phase d’intégration
- lors de l’analyse d’un scénario.

## 5 solutionSummary — Lecture synthétique de la solution

Ce bloc fournit une lecture condensée et lisible du planning résolu.

Il permet notamment :
- de résumer rapidement une solution,
- de comparer plusieurs résultats entre eux,
- d’exposer au Produit des indicateurs globaux compréhensibles sans l’obliger à lire le détail complet des WorkMetrics.

Il ne remplace ni :
- le détail des WorkMetrics par ressource,
- ni l’évaluation du solveur,
- ni les diagnostics techniques.

Il constitue une vue synthétique orientée pilotage et comparaison.

## Pourquoi ce contrat est important

Ce contrat permet :
✔ d’exposer clairement la solution produite par le moteur
✔ d’expliquer comment elle a été évaluée
✔ de préparer les futures analyses RH sans complexifier le solveur lui-même.

Cette architecture permet également de faire évoluer le moteur :
- d’un moteur interne de planification
- vers un moteur d’analyse et d’aide à la décision RH sans modifier le cœur du solveur.

## Évolution prévue

Ce contrat pourra être enrichi progressivement avec :
- de nouvelles WorkMetrics (équité, écarts de charge)
- des métriques contractuelles
- des indicateurs d’analyse RH.

Ces évolutions n’impacteront pas la structure générale de la réponse.