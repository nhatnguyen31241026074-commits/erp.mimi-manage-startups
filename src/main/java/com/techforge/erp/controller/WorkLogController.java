package com.techforge.erp.controller;

import com.techforge.erp.model.WorkLog;
import com.techforge.erp.service.WorkLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/worklogs")
@Tag(name = "WorkLog", description = "Work log / time tracking endpoints")
public class WorkLogController {

    @Autowired
    private WorkLogService workLogService;

    @PostMapping
    @Operation(summary = "Create a work log entry")
    public CompletableFuture<ResponseEntity<Object>> createWorkLog(@RequestBody WorkLog workLog) {
        return workLogService.createWorkLog(workLog)
                .<ResponseEntity<Object>>thenApply(saved -> ResponseEntity.ok(saved))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    @GetMapping
    @Operation(summary = "Get all work logs")
    public CompletableFuture<ResponseEntity<Object>> getAllWorkLogs() {
        return workLogService.getAllWorkLogs()
                .<ResponseEntity<Object>>thenApply(list -> ResponseEntity.ok(list))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<Object>> getWorkLogById(@PathVariable String id) {
        return workLogService.getWorkLogById(id)
                .<ResponseEntity<Object>>thenApply(w -> {
                    if (w == null) return ResponseEntity.notFound().build();
                    return ResponseEntity.ok(w);
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }
}
