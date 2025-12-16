package com.techforge.erp.controller;

import com.techforge.erp.model.User;
import com.techforge.erp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * Register a new user (no authentication required).
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<Object>> register(@RequestBody User user) {
        if (user == null || user.getEmail() == null || user.getPassword() == null) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "email and password are required"))
            );
        }

        // Set defaults
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("EMPLOYEE"); // Default role
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(Instant.now().toString()); // ISO-8601 string format
        }

        return userService.createUser(user)
                .<ResponseEntity<Object>>thenApply(saved -> ResponseEntity.ok(Map.of(
                    "message", "User registered successfully",
                    "user", saved
                )))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
                });
    }

    /**
     * Login endpoint (simplified - returns user if email/password match).
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<Object>> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || password == null) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "email and password are required"))
            );
        }

        return userService.getUserByEmail(email)
                .<ResponseEntity<Object>>thenApply(user -> {
                    if (user == null) {
                        return ResponseEntity.status(401).body(Map.of("error", "User not found"));
                    }
                    if (!password.equals(user.getPassword())) {
                        return ResponseEntity.status(401).body(Map.of("error", "Invalid password"));
                    }
                    return ResponseEntity.ok(Map.of(
                        "message", "Login successful",
                        "userId", user.getId(),
                        "role", user.getRole(),
                        "user", user
                    ));
                })
                .exceptionally(ex -> ResponseEntity.status(500).body(Map.of("error", ex.getMessage())));
    }
}
