package com.techforge.erp.controller;

import com.techforge.erp.model.User;
import com.techforge.erp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "User management endpoints")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    @Operation(summary = "Create a new user (admin) or register user via AuthController")
    public CompletableFuture<ResponseEntity<Object>> createUser(@RequestBody User user) {
        if (user == null || user.getEmail() == null || user.getPassword() == null) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("email and password are required"));
        }

        return userService.createUser(user)
                .<ResponseEntity<Object>>thenApply(saved -> ResponseEntity.ok(saved))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }

    @GetMapping
    @Operation(summary = "Get all users")
    public CompletableFuture<ResponseEntity<Object>> getAllUsers() {
        return userService.getAllUsers()
                .<ResponseEntity<Object>>thenApply(list -> ResponseEntity.ok(list))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
                });
    }
}
