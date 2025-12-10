package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReport {
    private int month;
    private int year;
    private Double totalRevenue;
    private Double totalExpense;
    private Double totalPayroll;
    private Double profit;
    private List<String> projects; // list of project IDs
    private List<Payroll> payrolls; // payroll details
}

