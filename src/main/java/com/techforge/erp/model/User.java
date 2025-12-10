package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String username;
    private String email;
    private String password; // stored hashed in production
    private String fullName;
    private String phone;
    private String role;

    // Salary fields (allowing nulls so we use Double wrappers)
    private Double baseSalary;
    private Double hourlyRateOT;
    private String salaryType; // e.g., "monthly", "hourly"

    private Date createdAt;
}
