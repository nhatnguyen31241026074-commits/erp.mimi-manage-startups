package com.techforge.erp.controller;

curl -X GET "http://localhost:8080/api/v1/reports/monthly?month=12&year=2025" -H "Accept: application/json"import com.techforge.erp.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/project/{id}")
    public CompletableFuture<ResponseEntity<Object>> getProjectReport(@PathVariable("id") String projectId) {
        return reportService.generateProjectReport(projectId)
                .<ResponseEntity<Object>>thenApply(r -> ResponseEntity.ok(r))
                .exceptionally(ex -> ResponseEntity.status(500).body("Error generating project report: " + ex.getMessage()));
    }

    @GetMapping("/monthly")
    public CompletableFuture<ResponseEntity<Object>> getMonthlyReport(@RequestParam("month") int month,
                                                                      @RequestParam("year") int year) {
        return reportService.generateMonthlyReport(month, year)
                .<ResponseEntity<Object>>thenApply(r -> ResponseEntity.ok(r))
                .exceptionally(ex -> ResponseEntity.status(500).body("Error generating monthly report: " + ex.getMessage()));
    }

    @GetMapping("/project/{id}/progress")
    public CompletableFuture<ResponseEntity<Object>> getProjectProgress(@PathVariable("id") String projectId) {
        return reportService.getProjectProgress(projectId)
                .<ResponseEntity<Object>>thenApply(r -> ResponseEntity.ok(r))
                .exceptionally(ex -> ResponseEntity.status(500).body("Error generating progress report: " + ex.getMessage()));
    }
}
