package fr.project.planning.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO racine aligné sur ScenarioContract.schema.json
 */
public class ScenarioRequestDTO {

    public String scenarioType; // SC-01 .. SC-05

    public PlanningContextDTO planningContext;

    public Object scenarioParameters; // Spécialisé par scénario (voir sous-DTO)

    public DataSetDTO dataSet;
}

// =========================
// PlanningContext
// =========================

class PlanningContextDTO {
    public HorizonDTO horizon;
    public String strategieScoring; // EXPLOITATION | ANALYSE_RH | AUDIT
}

class HorizonDTO {
    public LocalDate dateDebut;
    public LocalDate dateFin;
}

// =========================
// DataSet
// =========================

class DataSetDTO {
    public List<CreneauDTO> creneaux;
    public RessourcesDTO ressources;
}

class RessourcesDTO {
    public List<SalarieDTO> salaries;
}

// =========================
// Creneau
// =========================

class CreneauDTO {
    public String id;
    public LocalDate date;
    public String heureDebut; // HH:mm
    public String heureFin;   // HH:mm

    public String lieu;
    public String activite;
    public String type; // IMPOSE | GENERE

    public boolean isJourFerie;
    public boolean isReposHebdo;
    public boolean segmentNuit;
}

// =========================
// Salarie
// =========================

class SalarieDTO {
    public String id;
    public String statut;
    public List<String> sitesAutorises;
    public List<String> activitesCompatibles;
}
