package com.techforge.erp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techforge.erp.model.Project;
import com.techforge.erp.model.Task;
import com.techforge.erp.model.User;
import com.techforge.erp.model.WorkLog;
import com.techforge.erp.model.ai.AIRiskAnalysis;
import com.techforge.erp.model.ai.AISuggestion;
import com.techforge.erp.model.ai.GeminiRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AIService - Uses Gemini API for intelligent recommendations.
 *
 * STRICT COMPLIANCE NOTE:
 * - User.skills was REMOVED from model per Class Diagram constraint.
 * - Recommendations now based on: User.role + WorkLog history (hours on similar projects).
 */
@Service
public class AIService {
    private final Logger logger = LoggerFactory.getLogger(AIService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private WorkLogService workLogService;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.url:}")
    private String apiUrl;

    /**
     * Suggest the best assignee for a task based on Role and WorkLog history.
     *
     * Logic (without skills field):
     * 1. Filter users by Role matching task requirements (EMPLOYEE, MANAGER)
     * 2. Use WorkLog data to determine who has experience on similar projects
     * 3. Send context to AI for intelligent recommendation
     */
    public CompletableFuture<AISuggestion> suggestAssignee(Task task, List<User> users) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build user context with role and workload (without skills)
                List<Map<String, Object>> userContext = users.stream().map(u -> {
                    Map<String, Object> ctx = new HashMap<>();
                    ctx.put("userId", u.getId());
                    ctx.put("fullName", u.getFullName());
                    ctx.put("role", u.getRole());
                    ctx.put("email", u.getEmail());
                    // Workload indicator based on hourlyRateOT (higher rate = more senior)
                    ctx.put("hourlyRate", u.getHourlyRateOT() != null ? u.getHourlyRateOT() : 0);
                    return ctx;
                }).collect(Collectors.toList());

                // Get WorkLog history to add experience context
                Map<String, Double> userHoursWorked = new HashMap<>();
                if (workLogService != null && task.getProjectId() != null) {
                    try {
                        List<WorkLog> logs = workLogService.getAllWorkLogs().join();
                        // Count hours per user on this project or similar
                        for (WorkLog log : logs) {
                            String userId = log.getUserId();
                            Double hours = log.getHours() != null ? log.getHours() : 0;
                            userHoursWorked.merge(userId, hours, Double::sum);
                        }
                    } catch (Exception e) {
                        logger.warn("Could not fetch WorkLogs for AI context", e);
                    }
                }

                // Enrich user context with experience hours
                for (Map<String, Object> uctx : userContext) {
                    String userId = (String) uctx.get("userId");
                    uctx.put("totalHoursWorked", userHoursWorked.getOrDefault(userId, 0.0));
                }

                String usersJson = objectMapper.writeValueAsString(userContext);
                String taskJson = objectMapper.writeValueAsString(task);

                // Updated prompt - no skills, use Role and WorkLog experience
                String prompt = "Act as a Project Manager. Select the best user for this task.\n"
                        + "Selection criteria:\n"
                        + "1. User Role should match task complexity (MANAGER for high-priority, EMPLOYEE for standard)\n"
                        + "2. Prefer users with more totalHoursWorked (indicates experience)\n"
                        + "3. Consider hourlyRate as a seniority indicator\n"
                        + "\nTask: " + taskJson + "\n"
                        + "Available Users: " + usersJson + "\n"
                        + "Return ONLY raw JSON (no markdown). Structure: { \"userId\": \"...\", \"reason\": \"...\", \"confidenceScore\": 0.0-1.0 }";

                GeminiRequest gr = new GeminiRequest();
                gr.setPrompt(prompt);
                gr.setMaxTokens(300);
                gr.setTemperature(0.2);

                String body = objectMapper.writeValueAsString(gr);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (apiKey != null && !apiKey.isEmpty()) {
                    headers.setBearerAuth(apiKey);
                }

                HttpEntity<String> req = new HttpEntity<>(body, headers);
                ResponseEntity<String> resp = restTemplate.postForEntity(apiUrl, req, String.class);

                String raw = resp.getBody();
                String cleaned = cleanJsonResponse(raw);

                AISuggestion suggestion = objectMapper.readValue(cleaned, AISuggestion.class);
                return suggestion;
            } catch (JsonProcessingException e) {
                logger.error("JSON processing error in suggestAssignee", e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                logger.error("Error calling Gemini API for suggestAssignee", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<AIRiskAnalysis> analyzeProjectRisk(Project project, List<Task> tasks) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String projectJson = objectMapper.writeValueAsString(project);
                String tasksJson = objectMapper.writeValueAsString(tasks);

                String prompt = "Analyze project risk based on budget/deadline/task status.\n"
                        + "Project: " + projectJson + "\n"
                        + "Tasks: " + tasksJson + "\n"
                        + "Return ONLY raw JSON (no markdown). Structure: { \"riskLevel\", \"message\", \"suggestedAction\" }";

                GeminiRequest gr = new GeminiRequest();
                gr.setPrompt(prompt);
                gr.setMaxTokens(400);
                gr.setTemperature(0.2);

                String body = objectMapper.writeValueAsString(gr);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (apiKey != null && !apiKey.isEmpty()) {
                    headers.setBearerAuth(apiKey);
                }

                HttpEntity<String> req = new HttpEntity<>(body, headers);
                ResponseEntity<String> resp = restTemplate.postForEntity(apiUrl, req, String.class);

                String raw = resp.getBody();
                String cleaned = cleanJsonResponse(raw);

                AIRiskAnalysis analysis = objectMapper.readValue(cleaned, AIRiskAnalysis.class);
                return analysis;
            } catch (JsonProcessingException e) {
                logger.error("JSON processing error in analyzeProjectRisk", e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                logger.error("Error calling Gemini API for analyzeProjectRisk", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Clean typical wrappers returned by LLM APIs so we can parse the raw JSON object.
     */
    private String cleanJsonResponse(String raw) {
        if (raw == null) return "{}";
        String s = raw.trim();

        // If response contains ```json ... ``` or ``` ... ``` remove code fences
        s = s.replaceAll("(?s)```json\\s*", "");
        s = s.replaceAll("(?s)```\\s*", "");

        // Some providers return JSON inside quotes or with leading/trailing text. Attempt to extract the first JSON object
        int firstCurly = s.indexOf('{');
        int firstBracket = s.indexOf('[');
        int start = -1;
        if (firstCurly >= 0 && (firstCurly < firstBracket || firstBracket == -1)) start = firstCurly;
        else if (firstBracket >= 0) start = firstBracket;

        if (start == -1) {
            // nothing JSON-like found, return original
            return s;
        }

        // Find matching end for either object or array. Simple approach: find last occurrence of corresponding closing char.
        char openChar = s.charAt(start);
        char closeChar = openChar == '{' ? '}' : ']';
        int end = s.lastIndexOf(closeChar);
        if (end == -1) end = s.length() - 1;

        String candidate = s.substring(start, end + 1).trim();

        // If candidate is a JSON array but our DTO expects object, take first element
        if (candidate.startsWith("[") && candidate.endsWith("]")) {
            String inside = candidate.substring(1, candidate.length() - 1).trim();
            if (inside.isEmpty()) return "{}";
            // attempt to extract first object
            int objStart = inside.indexOf('{');
            int objEnd = inside.lastIndexOf('}');
            if (objStart >= 0 && objEnd >= objStart) {
                return inside.substring(objStart, objEnd + 1);
            }
            return inside; // fallback
        }

        return candidate;
    }
}
