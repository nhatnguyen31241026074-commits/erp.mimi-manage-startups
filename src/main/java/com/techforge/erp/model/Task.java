package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    private String id;
    private String projectId;
    private String assignedUserId;
    private String title;
    private String description;
    private String priority; // e.g., LOW, MEDIUM, HIGH
    private String status;   // e.g., TODO, IN_PROGRESS, DONE
    private Double estimatedHours;
}

