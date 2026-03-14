package fr.project.planning.scenarios.dto.input;

import java.time.LocalDate;

/**
 * ContratTravailDTO — Phase 1 transport uniquement.
 * Transporte les informations contractuelles d'un salarié.
 * Non exploité par le builder avant Phase 3.
 */
public class ContratTravailDTO {

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Double dureeMoyenneHeuresParJour;

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public Double getDureeMoyenneHeuresParJour() { return dureeMoyenneHeuresParJour; }
    public void setDureeMoyenneHeuresParJour(Double dureeMoyenneHeuresParJour) {
        this.dureeMoyenneHeuresParJour = dureeMoyenneHeuresParJour;
    }
}
