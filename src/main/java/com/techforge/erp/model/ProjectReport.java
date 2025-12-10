package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectReport {
    private String projectId;
    private String projectName;
    private Double progress; // percentage 0-100
    private Double budgetUsed;
    private Double budgetRemaining;
    private Integer totalTasks;
    private Integer completedTasks;
    private List<Map<String, Object>> taskBreakdown; // list of maps (e.g., status -> count)
    private List<Map<String, Object>> workerContribution; // list of maps (userId, hours)
}

