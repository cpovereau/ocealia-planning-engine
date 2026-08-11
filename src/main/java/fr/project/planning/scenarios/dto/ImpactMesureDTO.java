package fr.project.planning.scenarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * ImpactMesureDTO — une grandeur mesurée avant et après affectation du besoin (SC-06, lot S5).
 *
 * <p>Valeurs en <strong>heures décimales</strong>, comme {@code workMetrics.byRessource}.</p>
 *
 * <p>{@code plafond} reprend la limite individuelle du salarié, ou {@code null} lorsqu'elle n'est
 * pas renseignée — auquel cas {@code depassement} vaut toujours {@code false} : une limite
 * absente n'est pas une limite à zéro.</p>
 *
 * <p><strong>Un dépassement signalé n'est pas nécessairement une règle appliquée.</strong> Ce
 * bloc décrit des conséquences ; il ne préjuge pas de ce que le moteur sanctionne. À ce jour,
 * {@code heuresJour} est mesuré mais son plafond individuel n'est encore rattaché à aucune
 * contrainte (lot S7). Voir §6.3 de {@code 92_CADRAGE_SCENARIO_SC-06.md}.</p>
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ImpactMesureDTO {

    private double avant;
    private double apres;
    private double delta;
    private Double plafond;
    private boolean depassement;

    public ImpactMesureDTO() {
    }

    public ImpactMesureDTO(double avant, double apres, Double plafond) {
        this.avant = avant;
        this.apres = apres;
        this.delta = apres - avant;
        this.plafond = plafond;
        this.depassement = plafond != null && apres > plafond;
    }

    public double getAvant() { return avant; }
    public void setAvant(double avant) { this.avant = avant; }

    public double getApres() { return apres; }
    public void setApres(double apres) { this.apres = apres; }

    public double getDelta() { return delta; }
    public void setDelta(double delta) { this.delta = delta; }

    public Double getPlafond() { return plafond; }
    public void setPlafond(Double plafond) { this.plafond = plafond; }

    public boolean isDepassement() { return depassement; }
    public void setDepassement(boolean depassement) { this.depassement = depassement; }
}
