package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressReport {
    private String projectId;
    private Double progressPercentage;
    private Integer daysRemaining;
    private String riskLevel;
    private List<String> milestones;
}

