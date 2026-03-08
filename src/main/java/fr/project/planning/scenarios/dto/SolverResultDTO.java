package fr.project.planning.scenarios.dto;

import java.util.List;

public class SolverResultDTO {

    private String status;
    private ScoreDTO score;
    private List<ScoreBreakdownItemDTO> scoreBreakdown;

    public SolverResultDTO() {}

    public SolverResultDTO(String status, ScoreDTO score, List<ScoreBreakdownItemDTO> scoreBreakdown) {
        this.status = status;
        this.score = score;
        this.scoreBreakdown = scoreBreakdown;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ScoreDTO getScore() {
        return score;
    }

    public void setScore(ScoreDTO score) {
        this.score = score;
    }

    public List<ScoreBreakdownItemDTO> getScoreBreakdown() {
        return scoreBreakdown;
    }

    public void setScoreBreakdown(List<ScoreBreakdownItemDTO> scoreBreakdown) {
        this.scoreBreakdown = scoreBreakdown;
    }
}