## 📐 Diagramme UML simplifié — Modèle métier du moteur de planification

Ce diagramme représente les entités principales manipulées par le solveur
OptaPlanner. Il s'agit d'une vue conceptuelle simplifiée destinée à
faciliter la compréhension du moteur.

```plantuml
@startuml

title Modèle simplifié du moteur de planification

class PlanningSolution <<PlanningSolution>> {
    List<Creneau> creneaux
    List<Ressource> ressources
    RegulatoryParameters regulatoryParameters
    PlanningContext planningContext
}

class Creneau <<PlanningEntity>> {
    Long id
    Ressource ressourceAffectee <<PlanningVariable>>
    LocalDate date
    Activite activite
}

abstract class Ressource <<ProblemFact>> {
    Long id
}

class SalarieReel
class PosteVirtuel

class RegulatoryParameters <<ProblemFact>>
class PlanningContext <<ProblemFact>>

Ressource <|-- SalarieReel
Ressource <|-- PosteVirtuel

PlanningSolution --> Creneau
PlanningSolution --> Ressource
PlanningSolution --> RegulatoryParameters
PlanningSolution --> PlanningContext

Creneau --> Ressource : ressourceAffectee

@enduml
```
