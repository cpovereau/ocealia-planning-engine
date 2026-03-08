package fr.project.planning.scenarios.dto;

public class ScoreDTO {

    private int hard;
    private int soft;

    public ScoreDTO() {}

    public ScoreDTO(int hard, int soft) {
        this.hard = hard;
        this.soft = soft;
    }

    public int getHard() {
        return hard;
    }

    public void setHard(int hard) {
        this.hard = hard;
    }

    public int getSoft() {
        return soft;
    }

    public void setSoft(int soft) {
        this.soft = soft;
    }
}