package fr.project.planning.scenarios.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * BesoinDTO — le besoin à couvrir en SC-06 (lot S4).
 *
 * <p>De 1 à n créneaux, <strong>tous sur la même journée</strong>. La date est portée ici, une
 * seule fois : l'invariant est structurel, non déclaratif.</p>
 *
 * <p>Le besoin vit dans {@code scenarioParameters}, jamais dans {@code dataSet} — séparation
 * arbitrée au §4.2 de {@code 92_cadrage_scenario_sc-06.md} :</p>
 *
 * <blockquote>
 * {@code dataSet.creneaux} = le passé, intégralement figé.<br>
 * {@code scenarioParameters.besoin} = la question posée, seule variable de décision.
 * </blockquote>
 */
public class BesoinDTO {

    /** Jour du besoin. Commun à tous ses créneaux. */
    @NotNull(message = "scenarioParameters.besoin.date est obligatoire")
    private LocalDate date;

    @NotEmpty(message = "scenarioParameters.besoin.creneaux est obligatoire et ne peut pas être vide")
    @Valid
    private List<BesoinCreneauDTO> creneaux;

    // =========================
    // Getters / Setters
    // =========================

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public List<BesoinCreneauDTO> getCreneaux() { return creneaux; }
    public void setCreneaux(List<BesoinCreneauDTO> creneaux) { this.creneaux = creneaux; }
}
