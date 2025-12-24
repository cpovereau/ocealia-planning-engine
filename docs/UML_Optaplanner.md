## 📐 Diagramme UML — Modèle métier enrichi OptaPlanner

```plantuml
@startuml
skinparam style strictuml
skinparam classAttributeIconSize 0

' =========================
' Solution globale
' =========================

class PlanningSolution <<PlanningSolution>> {
  score
  scenarioParameters
}

' =========================
' Ressource (abstraction)
' =========================

abstract class Ressource <<ProblemFact>> {
}

class SalarieReel <<ProblemFact>> {
  id
  profilContractuel
  statut
  sitesAutorises
  activitesCompatibles
  postesComptablesCompatibles
}

class PosteVirtuel <<ProblemFact>> {
  type : POTENTIEL | REVELE
  capaciteCible
  activitesAutorisees
  lieuxAutorises
  postesComptablesCompatibles
}

Ressource <|-- SalarieReel
Ressource <|-- PosteVirtuel

' =========================
' Créneau (entité de décision)
' =========================

class Creneau <<PlanningEntity>> {
  date
  heureDebut
  heureFin
  duree
  lieu
  activite
  posteComptable
  priorite
  type : IMPOSE | GENERE
  ressourceAffectee
}

' =========================
' Variable de décision
' =========================

Creneau : ressourceAffectee <<PlanningVariable>>

' =========================
' Contraintes
' =========================

class ConstraintProvider <<ConstraintProvider>> {
  contraintesPhysiques()
  contraintesMetier()
  contraintesLegales()
  contraintesService()
  contraintesPersonnelles()
}

' =========================
' Relations
' =========================

PlanningSolution "1" o-- "*" Creneau
PlanningSolution "1" o-- "*" Ressource
ConstraintProvider --> Creneau : evalue
ConstraintProvider --> Ressource : utilise

@enduml
