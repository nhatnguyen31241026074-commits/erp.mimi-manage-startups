package com.techforge.erp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Invoice entity - STRICT compliance with Class Diagram.
 *
 * Fields (per UML):
 * - id, projectId, clientId, amount, issueDate, status
 *
 * NOTE: Extra fields (paidDate, transactionId, locked) were REMOVED
 * as they were not in the authorized diagram.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    private String id;
    private String projectId;
    private String clientId;
    private Double amount;
    private Date issueDate;
    private String status;  // "PENDING", "PAID", "OVERDUE", "CANCELLED"

    /**
     * Marks the invoice as paid.
     * Per Class Diagram constraint: Only updates the status field.
     * Transaction IDs must be stored externally (e.g., in payment gateway logs).
     *
     * @return true if successfully marked as paid, false if already paid
     */
    public boolean markAsPaid() {
        if ("PAID".equalsIgnoreCase(this.status)) {
            return false; // Already paid
        }
        this.status = "PAID";
        return true;
    }
}
