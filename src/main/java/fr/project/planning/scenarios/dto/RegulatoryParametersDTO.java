package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * RegulatoryParametersDTO — le cadre réglementaire de la résolution (lot S8.0)
 *
 * <p>Bloc annoncé de longue date par la spécification d'interface — « présent dans les documents
 * de cadrage et dans le domaine, mais pas dans {@code PlanningContextDTO} en V1 » — et jamais
 * implémenté. Les trois scénarios construisaient des valeurs par défaut : plage de nuit figée à
 * 22:00–06:00 et calendrier de jours fériés <strong>vide</strong>, ce qui a rendu la valorisation
 * du férié inopérante jusqu'au lot S7.9a.</p>
 *
 * <p>Tous les champs sont facultatifs. Absents, le moteur retombe sur le comportement du lot
 * S7.9a : plage de nuit légale par défaut, et jours fériés déduits de ce que le dataset
 * déclare.</p>
 */
public class RegulatoryParametersDTO {

    /**
     * Début de la plage de nuit, format {@code HH:mm}.
     *
     * <p>À transmettre avec {@link #heureFinNuit} : seule, la borne est ignorée et tracée en
     * WARN. Une demi-plage n'en décrit aucune, et mélanger une borne déclarée avec une borne
     * par défaut produirait un intervalle que personne n'a voulu.</p>
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureDebutNuit;

    /** Fin de la plage de nuit, format {@code HH:mm}. Voir {@link #heureDebutNuit}. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime heureFinNuit;

    /**
     * Calendrier des jours fériés de la période.
     *
     * <p>Source de vérité quand il est transmis : il l'emporte sur les déclarations portées
     * créneau par créneau ({@code isJourFerie}) et sur {@code holidayDates} en SC-01. Une liste
     * <strong>vide</strong> signifie « aucun jour férié sur la période » et fait autorité tout
     * autant ; c'est l'<em>absence</em> du champ qui laisse le moteur déduire.</p>
     *
     * <p>C'est la seule forme qui permette de qualifier correctement un créneau traversant
     * minuit : un drapeau porté par le créneau ne dit pas lequel des deux jours civils est
     * férié.</p>
     */
    private List<LocalDate> joursFeries;

    public LocalTime getHeureDebutNuit() { return heureDebutNuit; }
    public void setHeureDebutNuit(LocalTime heureDebutNuit) { this.heureDebutNuit = heureDebutNuit; }

    public LocalTime getHeureFinNuit() { return heureFinNuit; }
    public void setHeureFinNuit(LocalTime heureFinNuit) { this.heureFinNuit = heureFinNuit; }

    public List<LocalDate> getJoursFeries() { return joursFeries; }
    public void setJoursFeries(List<LocalDate> joursFeries) { this.joursFeries = joursFeries; }
}
