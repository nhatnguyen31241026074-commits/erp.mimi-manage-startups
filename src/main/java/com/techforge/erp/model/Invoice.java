package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    private String id;
    private String projectId;
    private String clientId;
    private Double amount;
    private Date issueDate;
    private String status;
}
