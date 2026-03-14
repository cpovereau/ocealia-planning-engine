package fr.project.planning.scenarios.dto.input;

import java.time.LocalDate;

/**
 * IndisponibiliteItemDTO — Phase 1 transport uniquement.
 * Une indisponibilité individuelle d'un salarié.
 * Non exploitée par le builder avant Phase 3.
 */
public class IndisponibiliteItemDTO {

    private String ressourceId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String motif;

    public String getRessourceId() { return ressourceId; }
    public void setRessourceId(String ressourceId) { this.ressourceId = ressourceId; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
}
