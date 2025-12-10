package com.techforge.erp.controller;

import com.techforge.erp.model.Task;
import com.techforge.erp.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @PostMapping
    public CompletableFuture<ResponseEntity<Object>> createTask(@RequestBody Task task) {
        return taskService.createTask(task)
                .<ResponseEntity<Object>>thenApply(saved -> ResponseEntity.ok(saved))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<Object>> getAllTasks() {
        return taskService.getAllTasks()
                .<ResponseEntity<Object>>thenApply(list -> ResponseEntity.ok(list))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<Object>> getTaskById(@PathVariable String id) {
        return taskService.getTaskById(id)
                .<ResponseEntity<Object>>thenApply(t -> {
                    if (t == null) return ResponseEntity.notFound().build();
                    return ResponseEntity.ok(t);
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<Object>> updateTask(@PathVariable String id, @RequestBody Task task) {
        task.setId(id);
        return taskService.updateTask(task)
                .<ResponseEntity<Object>>thenApply(v -> ResponseEntity.ok().build())
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    @DeleteMapping("/{id}")
    public CompletableFuture<ResponseEntity<Object>> deleteTask(@PathVariable String id) {
        return taskService.deleteTask(id)
                .<ResponseEntity<Object>>thenApply(v -> ResponseEntity.ok().build())
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }
}

