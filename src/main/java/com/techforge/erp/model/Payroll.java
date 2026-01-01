package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payroll {
    private String id;
    private String userId;
    private int month;
    private int year;
    private Double baseSalary;
    private Double overtimePay;
    private Double totalPay;
    private boolean isPaid;
    private String transactionId;
}


