package com.techforge.erp.model.ai;

import java.util.Map;

/** Minimal response holder for Gemini-like API */
public class GeminiResponse {
    private String id;
    private String object;
    private Map<String, Object> data;
    private String text;

    public GeminiResponse() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

