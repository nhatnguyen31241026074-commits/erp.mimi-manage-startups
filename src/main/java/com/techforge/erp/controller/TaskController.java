package com.techforge.erp.controller;

import com.techforge.erp.model.Task;
import com.techforge.erp.model.User;
import com.techforge.erp.service.TaskService;
import com.techforge.erp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @PostMapping
    public CompletableFuture<ResponseEntity<Object>> createTask(@RequestBody Task task) {
        return taskService.createTask(task)
                .<ResponseEntity<Object>>thenApply(saved -> ResponseEntity.ok(saved))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    /**
     * Get tasks with optional filtering.
     * - If ?projectId={id} is provided, filter by project.
     * - If ?assignee={email} is provided, filter by assigneeEmail.
     * - If user is EMPLOYEE, only return their assigned tasks (unless projectId is specified).
     * - If user is MANAGER/ADMIN, return all (or filtered).
     */
    @GetMapping
    public CompletableFuture<ResponseEntity<Object>> getAllTasks(
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String projectId,
            @RequestHeader(value = "X-Requester-ID", required = false) String requesterId) {

        System.out.println("[TaskController] getAllTasks called with: assignee=" + assignee + ", projectId=" + projectId + ", requesterId=" + requesterId);

        return taskService.getAllTasks()
                .thenCompose(tasks -> {
                    System.out.println("[TaskController] Total tasks from DB: " + tasks.size());

                    // PRIORITY 1: Filter by projectId if provided
                    List<Task> filtered = tasks;
                    if (projectId != null && !projectId.isEmpty()) {
                        filtered = tasks.stream()
                                .filter(t -> projectId.equals(t.getProjectId()))
                                .collect(Collectors.toList());
                        System.out.println("[TaskController] After projectId filter: " + filtered.size() + " tasks");
                        // When filtering by project, return all tasks for that project regardless of user
                        return CompletableFuture.completedFuture(filtered);
                    }

                    // If no requester ID, just return all tasks (for unauthenticated access if allowed)
                    if (requesterId == null || requesterId.isEmpty()) {
                        if (assignee != null && !assignee.isEmpty()) {
                            filtered = tasks.stream()
                                    .filter(t -> assignee.equalsIgnoreCase(t.getAssigneeEmail()))
                                    .collect(Collectors.toList());
                            return CompletableFuture.completedFuture(filtered);
                        }
                        return CompletableFuture.completedFuture(tasks);
                    }

                    // Get user to check role
                    return userService.getUserById(requesterId)
                            .thenApply(user -> {
                                if (user == null) {
                                    return tasks; // Return all if user not found
                                }

                                String role = user.getRole();

                                // EMPLOYEE: Only see their own tasks
                                if ("EMPLOYEE".equalsIgnoreCase(role)) {
                                    return tasks.stream()
                                            .filter(t -> requesterId.equals(t.getAssignedUserId())
                                                    || (user.getEmail() != null && user.getEmail().equalsIgnoreCase(t.getAssigneeEmail())))
                                            .collect(Collectors.toList());
                                }

                                // MANAGER/ADMIN: See all, but can filter by assignee
                                if (assignee != null && !assignee.isEmpty()) {
                                    return tasks.stream()
                                            .filter(t -> assignee.equalsIgnoreCase(t.getAssigneeEmail()))
                                            .collect(Collectors.toList());
                                }

                                return tasks;
                            });
                })
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

    // POST endpoint for updating task status (used by Kanban drag & drop)
    @PostMapping("/{id}")
    public CompletableFuture<ResponseEntity<Object>> updateTaskStatus(@PathVariable String id, @RequestBody java.util.Map<String, Object> payload) {
        return taskService.getTaskById(id)
                .thenCompose(existingTask -> {
                    if (existingTask == null) {
                        return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
                    }

                    // Update only the fields provided in the payload
                    if (payload.containsKey("status")) {
                        existingTask.setStatus((String) payload.get("status"));
                    }
                    if (payload.containsKey("title")) {
                        existingTask.setTitle((String) payload.get("title"));
                    }
                    if (payload.containsKey("priority")) {
                        existingTask.setPriority((String) payload.get("priority"));
                    }
                    if (payload.containsKey("description")) {
                        existingTask.setDescription((String) payload.get("description"));
                    }
                    if (payload.containsKey("assigneeEmail")) {
                        existingTask.setAssigneeEmail((String) payload.get("assigneeEmail"));
                    }
                    if (payload.containsKey("assignedUserId")) {
                        existingTask.setAssignedUserId((String) payload.get("assignedUserId"));
                    }

                    return taskService.updateTask(existingTask)
                            .<ResponseEntity<Object>>thenApply(v -> ResponseEntity.ok(existingTask));
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

