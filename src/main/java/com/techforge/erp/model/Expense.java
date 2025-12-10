package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    private String id;
    private String projectId;
    private String category;
    private Double amount;
    private Date expenseDate;
}

