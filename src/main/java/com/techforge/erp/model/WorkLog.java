package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkLog {
    private String id;
    private String taskId;
    private String userId;
    private String projectId;

    private Double hours; // total hours
    private Double regularHours;
    private Double overtimeHours;

    private Date workDate;
    private String description;

    // snapshots of salary at time of logging
    private Double baseSalarySnapshot;
    private Double hourlyRateOTSnapshot;
}
