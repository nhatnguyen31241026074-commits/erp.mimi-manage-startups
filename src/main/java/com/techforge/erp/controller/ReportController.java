package com.techforge.erp.controller;

import com.techforge.erp.model.MonthlyReport;
import com.techforge.erp.model.ProgressReport;
import com.techforge.erp.model.ProjectReport;
import com.techforge.erp.service.ReportService;
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
    public CompletableFuture<ResponseEntity<ProjectReport>> getProjectReport(@PathVariable("id") String projectId) {
        return reportService.generateProjectReport(projectId)
                .<ResponseEntity<ProjectReport>>thenApply(r -> ResponseEntity.ok(r))
                .exceptionally(ex -> ResponseEntity.status(500).body(null));
    }

    @GetMapping("/monthly")
    public CompletableFuture<ResponseEntity<MonthlyReport>> getMonthlyReport(@RequestParam("month") int month,
                                                                      @RequestParam("year") int year) {
        return reportService.generateMonthlyReport(month, year)
                .<ResponseEntity<MonthlyReport>>thenApply(r -> ResponseEntity.ok(r))
                .exceptionally(ex -> ResponseEntity.status(500).body(null));
    }

    @GetMapping("/project/{id}/progress")
    public CompletableFuture<ResponseEntity<ProgressReport>> getProjectProgress(@PathVariable("id") String projectId) {
        return reportService.getProjectProgress(projectId)
                .<ResponseEntity<ProgressReport>>thenApply(r -> ResponseEntity.ok(r))
                .exceptionally(ex -> ResponseEntity.status(500).body(null));
    }
}
