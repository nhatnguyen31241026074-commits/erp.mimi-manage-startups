package com.techforge.erp.model.ai;

public class AIRiskAnalysis {
    private String riskLevel;
    private String message;
    private String suggestedAction;

    public AIRiskAnalysis() {
    }

    public AIRiskAnalysis(String riskLevel, String message, String suggestedAction) {
        this.riskLevel = riskLevel;
        this.message = message;
        this.suggestedAction = suggestedAction;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }
}

