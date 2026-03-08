package fr.project.planning.scenarios.dto;

public class ScoreBreakdownItemDTO {

    private String penaliteKey;
    private String unit;
    private double quantity;
    private int weightedImpact;

    public ScoreBreakdownItemDTO() {}

    public ScoreBreakdownItemDTO(String penaliteKey, String unit, double quantity, int weightedImpact) {
        this.penaliteKey = penaliteKey;
        this.unit = unit;
        this.quantity = quantity;
        this.weightedImpact = weightedImpact;
    }

    public String getPenaliteKey() {
        return penaliteKey;
    }

    public void setPenaliteKey(String penaliteKey) {
        this.penaliteKey = penaliteKey;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public int getWeightedImpact() {
        return weightedImpact;
    }

    public void setWeightedImpact(int weightedImpact) {
        this.weightedImpact = weightedImpact;
    }
}