## 📐 Diagramme UML — Modèle métier du moteur de planification

```plantuml
@startuml
skinparam style strictuml
skinparam classAttributeIconSize 0

' =========================
' Abstractions
' =========================

abstract class Ressource {
}

' =========================
' Ressources
' =========================

class SalarieReel {
  +id
  +profilContractuel
  +statut
  +sitesAutorises
  +activitesCompatibles
  +postesComptablesCompatibles
}

class PosteVirtuel {
  +type : POTENTIEL | REVELE
  +capaciteCible
  +activitesAutorisees
  +lieuxAutorises
  +postesComptablesCompatibles
}

Ressource <|-- SalarieReel
Ressource <|-- PosteVirtuel

' =========================
' Créneau (besoin)
' =========================

class Creneau {
  +date
  +heureDebut
  +heureFin
  +duree
  +lieu
  +activite
  +posteComptable
  +priorite
  +type : IMPOSE | GENERE
}

' =========================
' Affectation (décision)
' =========================

class Affectation {
  +etat : AFFECTE | A_AFFECTER
}

Creneau "1" --> "1..*" Affectation : propose
Affectation "0..1" --> Ressource : cible

' =========================
' Contraintes
' =========================

class Contrainte {
  +categorie
  +severite
}

class ContraintePhysique
class ContrainteMetier
class ContrainteLegale
class ContrainteService
class ContraintePersonnelle

Contrainte <|-- ContraintePhysique
Contrainte <|-- ContrainteMetier
Contrainte <|-- ContrainteLegale
Contrainte <|-- ContrainteService
Contrainte <|-- ContraintePersonnelle

Contrainte --> Affectation : evalue

' =========================
' Solution globale
' =========================

class PlanningSolution {
  +score
}

PlanningSolution "1" o-- "*" Creneau
PlanningSolution "1" o-- "*" Ressource
PlanningSolution "1" o-- "*" Contrainte

@enduml
