package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    private String id;
    private String clientId;
    private String name;
    private String description;
    private Double budget; // changed to Double for flexibility
    private Date startDate;
    private Date endDate;
    private String status;
    private List<String> memberUserIds;
}
