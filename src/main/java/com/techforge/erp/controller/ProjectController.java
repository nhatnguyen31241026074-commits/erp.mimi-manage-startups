package com.techforge.erp.controller;

import com.techforge.erp.model.Project;
import com.techforge.erp.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Project", description = "Project management endpoints")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new project")
    public CompletableFuture<ResponseEntity<Object>> createProject(@RequestBody Project project) {
        return projectService.createProject(project)
                .<ResponseEntity<Object>>thenApply(saved -> ResponseEntity.ok(saved))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    @GetMapping
    @Operation(summary = "Get all projects")
    public CompletableFuture<ResponseEntity<Object>> getAllProjects() {
        return projectService.getAllProjects()
                .<ResponseEntity<Object>>thenApply(list -> ResponseEntity.ok(list))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by id")
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
    @Operation(summary = "Update project by id")
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
    @Operation(summary = "Delete project by id")
    public CompletableFuture<ResponseEntity<Object>> deleteProject(@PathVariable String id) {
        return projectService.deleteProject(id)
                .<ResponseEntity<Object>>thenApply(v -> ResponseEntity.ok().build())
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }
}
