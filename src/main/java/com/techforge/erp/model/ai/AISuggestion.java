package com.techforge.erp.model.ai;

public class AISuggestion {
    private String userId;
    private String reason;
    private Double confidenceScore;

    public AISuggestion() {
    }

    public AISuggestion(String userId, String reason, Double confidenceScore) {
        this.userId = userId;
        this.reason = reason;
        this.confidenceScore = confidenceScore;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}

