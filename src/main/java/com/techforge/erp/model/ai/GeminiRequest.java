package com.techforge.erp.model.ai;

import java.util.Map;

/** Minimal request DTO for Gemini-like API */
public class GeminiRequest {
    private String prompt;
    private Integer maxTokens;
    private Double temperature;
    private Map<String, Object> extra;

    public GeminiRequest() {}

    public GeminiRequest(String prompt, Integer maxTokens, Double temperature) {
        this.prompt = prompt;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
}

