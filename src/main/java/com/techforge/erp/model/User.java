package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User entity - STRICT compliance with Class Diagram.
 *
 * Fields (per UML):
 * - id, username, email, passwordHash, fullName, phone, role
 * - baseSalary, hourlyRateOT, salaryType
 *
 * NOTE: "skills" field was REMOVED as it was not in the authorized diagram.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    // Core identity fields (per Class Diagram)
    private String id;
    private String username;
    private String email;
    private String password; // passwordHash in diagram, stored hashed in production
    private String fullName;
    private String phone;
    private String role;

    // Salary fields (per Class Diagram)
    private Double baseSalary;
    private Double hourlyRateOT;
    private String salaryType; // "monthly" or "hourly"

    // OTP fields for password reset (operational, not in core diagram but needed for auth)
    private String otp;
    private String otpExpiry;

    /**
     * Helper: check if user has one of the provided roles (case-insensitive).
     * Implements hasPermission() concept from Class Diagram.
     */
    public boolean hasRole(String... roles) {
        if (this.role == null || roles == null || roles.length == 0) return false;
        for (String r : roles) {
            if (r != null && r.equalsIgnoreCase(this.role)) return true;
        }
        return false;
    }
}
