package com.techforge.erp.controller;

import com.techforge.erp.model.Expense;
import com.techforge.erp.model.Invoice;
import com.techforge.erp.model.Payroll;
import com.techforge.erp.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {

    private final FinanceService financeService;

    @Autowired
    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @PostMapping("/payroll/calculate")
    public CompletableFuture<ResponseEntity<Object>> calculatePayroll(@RequestParam String userId,
                                                                      @RequestParam int month,
                                                                      @RequestParam int year) {
        return financeService.calculatePayroll(userId, month, year)
                .<ResponseEntity<Object>>thenApply(p -> ResponseEntity.ok(p))
                .exceptionally(ex -> ResponseEntity.status(500).body("Error calculating payroll: " + ex.getMessage()));
    }

    @PostMapping("/invoices")
    public CompletableFuture<ResponseEntity<Object>> createInvoice(@RequestBody Invoice invoice) {
        if (invoice == null) return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("invoice is required"));
        return financeService.createInvoice(invoice)
                .<ResponseEntity<Object>>thenApply(i -> ResponseEntity.ok(i))
                .exceptionally(ex -> ResponseEntity.status(500).body("Error creating invoice: " + ex.getMessage()));
    }

    @PostMapping("/expenses")
    public CompletableFuture<ResponseEntity<Object>> createExpense(@RequestBody Expense expense) {
        if (expense == null) return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("expense is required"));
        return financeService.createExpense(expense)
                .<ResponseEntity<Object>>thenApply(e -> ResponseEntity.ok(e))
                .exceptionally(ex -> ResponseEntity.status(500).body("Error creating expense: " + ex.getMessage()));
    }
}

