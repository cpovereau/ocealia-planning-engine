package fr.project.planning.domain.creneau;

import fr.project.planning.domain.ressource.Ressource;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.lookup.PlanningId;


import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Creneau
 *
 * Représente un besoin de travail à couvrir.
 * Porte la variable de décision principale du moteur.
 */
@PlanningEntity
public class Creneau implements Serializable {

    /* =========================
       Identité
       ========================= */

    @PlanningId
    private String id;

    /* =========================
       Données métier (faites d'entrée)
       ========================= */

    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;

    /**
     * Durée du créneau en minutes.
     * Calculée en amont, jamais recalculée par le moteur.
     */
    private int duree;

    private String lieu;
    private String activite;
    private String posteComptable;

    private PrioriteCreneau priorite;
    private TypeCreneau type;

    /* =========================
       Qualification temporelle
       ========================= */

    private TypePlageHoraire typePlageHoraire;
    private boolean jourFerie;

    /* =========================
        Qualification réglementaire du jour
        =========================*/

    private QualificationJour qualificationJour;

    /* =========================
       Variable de décision
       ========================= */

    @PlanningVariable(valueRangeProviderRefs = "ressourceRange")
    private Ressource ressourceAffectee;

    /* =========================
       Constructeurs
       ========================= */

    public Creneau() {
        // requis par OptaPlanner
    }

    public Creneau(
            String id,
            LocalDate date,
            LocalTime heureDebut,
            LocalTime heureFin,
            int duree,
            String lieu,
            String activite,
            String posteComptable,
            PrioriteCreneau priorite,
            TypeCreneau type,
            TypePlageHoraire typePlageHoraire,
            boolean jourFerie,
            QualificationJour qualificationJour
    ) {
        this.id = id;
        this.date = date;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.duree = duree;
        this.lieu = lieu;
        this.activite = activite;
        this.posteComptable = posteComptable;
        this.priorite = priorite;
        this.type = type;
        this.typePlageHoraire = typePlageHoraire;
        this.jourFerie = jourFerie;
        this.qualificationJour = qualificationJour;
    }

    /* =========================
       Getters
       ========================= */

    public String getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public int getDuree() {
        return duree;
    }

    public String getLieu() {
        return lieu;
    }

    public String getActivite() {
        return activite;
    }

    public String getPosteComptable() {
        return posteComptable;
    }

    public PrioriteCreneau getPriorite() {
        return priorite;
    }

    public TypeCreneau getType() {
        return type;
    }

    public TypePlageHoraire getTypePlageHoraire() {
        return typePlageHoraire;
    }

    public boolean isJourFerie() {
        return jourFerie;
    }

    public Ressource getRessourceAffectee() {
        return ressourceAffectee;
    }

    public QualificationJour getQualificationJour() {
    return qualificationJour;
    }   

public void setRessourceAffectee(Ressource ressourceAffectee) {
        this.ressourceAffectee = ressourceAffectee;
    }
}



