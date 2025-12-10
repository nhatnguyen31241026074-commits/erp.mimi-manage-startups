package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskAlert {
    private String id;
    private String projectId;
    private String type;
    private String severity;
    private String message;
}

