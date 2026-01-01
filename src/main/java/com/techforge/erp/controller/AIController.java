package com.techforge.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techforge.erp.model.Project;
import com.techforge.erp.model.Task;
import com.techforge.erp.model.User;
import com.techforge.erp.model.ai.AIRiskAnalysis;
import com.techforge.erp.model.ai.AISuggestion;
import com.techforge.erp.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI", description = "AI integration endpoints (Gemini)")
public class AIController {
    private final Logger logger = LoggerFactory.getLogger(AIController.class);
    private final AIService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/suggest")
    @Operation(summary = "Suggest an assignee for a Task using AI")
    public CompletableFuture<ResponseEntity<AISuggestion>> suggestAssignee(@RequestBody Map<String, Object> body) {
        try {
            Object taskObj = body.get("task");
            Object usersObj = body.get("users");
            Task task = objectMapper.convertValue(taskObj, Task.class);
            List<User> users = objectMapper.convertValue(usersObj, objectMapper.getTypeFactory().constructCollectionType(List.class, User.class));

            return aiService.suggestAssignee(task, users)
                    .thenApply(ResponseEntity::ok)
                    .exceptionally(ex -> {
                        logger.error("AI suggest error", ex);
                        return ResponseEntity.status(500).build();
                    });
        } catch (Exception e) {
            logger.error("Invalid request to /ai/suggest", e);
            CompletableFuture<ResponseEntity<AISuggestion>> f = new CompletableFuture<>();
            f.complete(ResponseEntity.badRequest().build());
            return f;
        }
    }

    @PostMapping("/risk")
    @Operation(summary = "Analyze project risk using AI")
    public CompletableFuture<ResponseEntity<AIRiskAnalysis>> analyzeRisk(@RequestBody Map<String, Object> body) {
        try {
            Object projectObj = body.get("project");
            Object tasksObj = body.get("tasks");
            Project project = objectMapper.convertValue(projectObj, Project.class);
            List<Task> tasks = objectMapper.convertValue(tasksObj, objectMapper.getTypeFactory().constructCollectionType(List.class, Task.class));

            return aiService.analyzeProjectRisk(project, tasks)
                    .thenApply(ResponseEntity::ok)
                    .exceptionally(ex -> {
                        logger.error("AI risk analysis error", ex);
                        return ResponseEntity.status(500).build();
                    });
        } catch (Exception e) {
            logger.error("Invalid request to /ai/risk", e);
            CompletableFuture<ResponseEntity<AIRiskAnalysis>> f = new CompletableFuture<>();
            f.complete(ResponseEntity.badRequest().build());
            return f;
        }
    }
}
