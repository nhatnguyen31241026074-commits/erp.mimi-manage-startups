package com.techforge.erp.controller;

import com.techforge.erp.model.Expense;
import com.techforge.erp.model.Invoice;
import com.techforge.erp.model.Payroll;
import com.techforge.erp.service.FinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/finance")
@Tag(name = "Finance", description = "Payroll and financial operations")
public class FinanceController {

    private final FinanceService financeService;

    @Autowired
    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @PostMapping("/payroll/calculate")
    @Operation(summary = "Calculate payroll for a user for a given month and year")
    public CompletableFuture<ResponseEntity<Object>> calculatePayroll(@RequestParam String userId,
                                                                      @RequestParam int month,
                                                                      @RequestParam int year) {
        return financeService.calculatePayroll(userId, month, year)
                .<ResponseEntity<Object>>thenApply(p -> ResponseEntity.ok(p))
                .exceptionally(ex -> ResponseEntity.status(500).body("Error calculating payroll: " + ex.getMessage()));
    }

    @GetMapping("/payroll")
    @Operation(summary = "Get all payroll records")
    public CompletableFuture<ResponseEntity<Object>> getAllPayroll() {
        return financeService.getAllPayroll()
                .<ResponseEntity<Object>>thenApply(list -> ResponseEntity.ok(list))
                .exceptionally(ex -> ResponseEntity.status(500).body("Error fetching payroll: " + ex.getMessage()));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get payroll transaction history")
    public ResponseEntity<Object> getTransactionHistory() {
        try {
            List<Payroll> list = financeService.getTransactionHistory();
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching transaction history: " + e.getMessage());
        }
    }

    @PostMapping("/pay")
    @Operation(summary = "Process a MoMo payment callback or mock payment")
    public ResponseEntity<Object> processMoMoPayment(@RequestBody java.util.Map<String, String> payload) {
        // Mock MoMo payment processing
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Payment processed successfully",
                "status", "SUCCESS",
                "transactionId", "MOMO_" + System.currentTimeMillis()
        ));
    }

    @PostMapping("/invoices")
    @Operation(summary = "Create an invoice")
    public CompletableFuture<ResponseEntity<Object>> createInvoice(@RequestBody Invoice invoice) {
        if (invoice == null) return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("invoice is required"));
        return financeService.createInvoice(invoice)
                .<ResponseEntity<Object>>thenApply(i -> ResponseEntity.ok(i))
                .exceptionally(ex -> ResponseEntity.status(500).body("Error creating invoice: " + ex.getMessage()));
    }

    @PostMapping("/expenses")
    @Operation(summary = "Create an expense record")
    public CompletableFuture<ResponseEntity<Object>> createExpense(@RequestBody Expense expense) {
        if (expense == null) return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("expense is required"));
        return financeService.createExpense(expense)
                .<ResponseEntity<Object>>thenApply(e -> ResponseEntity.ok(e))
                .exceptionally(ex -> ResponseEntity.status(500).body("Error creating expense: " + ex.getMessage()));
    }
}
