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
    private String salaryType; // e.g., "monthly", "hourly"
    private Double hourlyRateOT;

    private Date createdAt;

    // Helper: check if user has one of the provided roles (case-insensitive)
    public boolean hasRole(String... roles) {
        if (this.role == null || roles == null || roles.length == 0) return false;
        for (String r : roles) {
            if (r != null && r.equalsIgnoreCase(this.role)) return true;
        }
        return false;
    }
}
