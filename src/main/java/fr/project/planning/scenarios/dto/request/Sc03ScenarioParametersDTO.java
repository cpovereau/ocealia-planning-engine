package fr.project.planning.scenarios.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.project.planning.scenarios.dto.HorizonDTO;

/**
 * Sc03ScenarioParametersDTO
 *
 * Paramètres spécifiques au scénario SC-03 (couverture locale multi-ressources).
 *
 * Différences vs SC-01 :
 * - pas de ressource cible unique (multi-salariés)
 * - pas d'amplitude horaire : les créneaux viennent du dataSet
 * - prioriteCouverture : niveau de priorité global du planning
 * - periode : optionnel, si différent de l'horizon planningContext
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sc03ScenarioParametersDTO {

    /**
     * Niveau de priorité de couverture pour l'ensemble du planning.
     * Valeurs attendues : "NORMALE", "HAUTE", "CRITIQUE".
     * Non exploité par le solveur avant Phase 8 — transporté ici pour traçabilité.
     */
    private String prioriteCouverture;

    /**
     * Période de planification optionnelle.
     * Si null, l'horizon du planningContext fait foi.
     */
    private HorizonDTO periode;

    // =========================
    // Getters / Setters
    // =========================

    public String getPrioriteCouverture() {
        return prioriteCouverture;
    }

    public void setPrioriteCouverture(String prioriteCouverture) {
        this.prioriteCouverture = prioriteCouverture;
    }

    public HorizonDTO getPeriode() {
        return periode;
    }

    public void setPeriode(HorizonDTO periode) {
        this.periode = periode;
    }
}
