package fr.project.planning.domain.ressource;

import java.time.LocalDate;

/**
 * Indisponibilite — objet valeur domaine.
 *
 * Représente une période pendant laquelle un salarié est indisponible.
 * Transportée depuis WinDev en Phase 1, mappée vers le domaine en Phase 3.
 * Exploitée par le solveur à partir de Phase 4 (règle d'incompatibilité).
 */
public class Indisponibilite {

    private final String ressourceId;
    private final LocalDate dateDebut;
    private final LocalDate dateFin;
    private final String motif;

    public Indisponibilite(String ressourceId, LocalDate dateDebut, LocalDate dateFin, String motif) {
        this.ressourceId = ressourceId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.motif = motif;
    }

    public String getRessourceId() { return ressourceId; }
    public LocalDate getDateDebut() { return dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public String getMotif() { return motif; }
}