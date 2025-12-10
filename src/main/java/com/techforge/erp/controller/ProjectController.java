package com.techforge.erp.controller;

import com.techforge.erp.model.Project;
import com.techforge.erp.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @PostMapping
    public CompletableFuture<ResponseEntity<Object>> createProject(@RequestBody Project project) {
        return projectService.createProject(project)
                .<ResponseEntity<Object>>thenApply(saved -> ResponseEntity.ok(saved))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<Object>> getAllProjects() {
        return projectService.getAllProjects()
                .<ResponseEntity<Object>>thenApply(list -> ResponseEntity.ok(list))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<Object>> getProjectById(@PathVariable String id) {
        return projectService.getProjectById(id)
                .<ResponseEntity<Object>>thenApply(p -> {
                    if (p == null) return ResponseEntity.notFound().build();
                    return ResponseEntity.ok(p);
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<Object>> updateProject(@PathVariable String id, @RequestBody Project project) {
        project.setId(id);
        return projectService.updateProject(project)
                .<ResponseEntity<Object>>thenApply(v -> ResponseEntity.ok().build())
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    @DeleteMapping("/{id}")
    public CompletableFuture<ResponseEntity<Object>> deleteProject(@PathVariable String id) {
        return projectService.deleteProject(id)
                .<ResponseEntity<Object>>thenApply(v -> ResponseEntity.ok().build())
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }
}

