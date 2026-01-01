package com.techforge.erp.controller;

import com.techforge.erp.model.User;
import com.techforge.erp.service.EmailService;
import com.techforge.erp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, login and password reset")
public class AuthController {

    // Secret codes for role assignment
    private static final String CODE_MANAGER = "SAIYAN_GOD";
    private static final String CODE_ADMIN = "CAPSULE_CORP";
    private static final String CODE_CLIENT = "FRIEZA_FORCE";

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    /**
     * Register a new user (no authentication required).
     * POST /api/v1/auth/register
     * Supports secret code for role assignment.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public CompletableFuture<ResponseEntity<Object>> register(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        String fullName = request.get("fullName");
        String username = request.get("username");
        String secretCode = request.get("secretCode");

        if (email == null || password == null) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"))
            );
        }

        // Check if email already exists
        return userService.getUserByEmail(email)
            .thenCompose(existingUser -> {
                if (existingUser != null) {
                    return CompletableFuture.completedFuture(
                        ResponseEntity.badRequest().body(Map.of("error", "Email already registered"))
                    );
                }

                // Determine role based on secret code
                String role = determineRole(secretCode);

                // Create new user
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setPassword(password);
                newUser.setFullName(fullName != null ? fullName : email.split("@")[0]);
                newUser.setUsername(username != null ? username : email.split("@")[0]);
                newUser.setRole(role);
                newUser.setBaseSalary(0.0);
                newUser.setHourlyRateOT(0.0);
                newUser.setSalaryType("monthly");

                return userService.createUser(newUser)
                    .<ResponseEntity<Object>>thenApply(saved -> ResponseEntity.ok(Map.of(
                        "message", "User registered successfully",
                        "role", saved.getRole(),
                        "user", saved
                    )));
            })
            .exceptionally(ex -> {
                ex.printStackTrace();
                return ResponseEntity.status(500).body(Map.of("error", "Registration failed: " + ex.getMessage()));
            });
    }

    /**
     * Determine user role based on secret code.
     * Codes:
     * - KAME_HOUSE -> EMPLOYEE
     * - SAIYAN_GOD -> MANAGER
     * - CAPSULE_CORP -> ADMIN
     * - FRIEZA_FORCE -> CLIENT
     * - Empty/Unknown -> EMPLOYEE (default)
     */
    private String determineRole(String secretCode) {
        if (secretCode == null || secretCode.trim().isEmpty()) {
            return "EMPLOYEE";
        }
        String code = secretCode.trim().toUpperCase();
        switch (code) {
            case "KAME_HOUSE":
                return "EMPLOYEE";
            case "SAIYAN_GOD":
                return "MANAGER";
            case "CAPSULE_CORP":
                return "ADMIN";
            case "FRIEZA_FORCE":
                return "CLIENT";
            default:
                return "EMPLOYEE";
        }
    }

    /**
     * Login endpoint (simplified - returns user if email/password match).
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
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

    /**
     * Update user profile.
     * PUT /api/v1/auth/profile
     */
    @PutMapping("/profile")
    @Operation(summary = "Update user profile")
    public CompletableFuture<ResponseEntity<Object>> updateProfile(@RequestBody Map<String, Object> profileData) {
        String userId = (String) profileData.get("userId");

        if (userId == null || userId.isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "userId is required"))
            );
        }

        return userService.getUserById(userId)
            .thenCompose(existingUser -> {
                if (existingUser == null) {
                    return CompletableFuture.completedFuture(
                        ResponseEntity.notFound().build()
                    );
                }

                // Update only provided fields
                if (profileData.containsKey("fullName")) {
                    existingUser.setFullName((String) profileData.get("fullName"));
                }
                if (profileData.containsKey("phone")) {
                    existingUser.setPhone((String) profileData.get("phone"));
                }
                if (profileData.containsKey("hourlyRateOT")) {
                    Object rate = profileData.get("hourlyRateOT");
                    if (rate instanceof Number) {
                        existingUser.setHourlyRateOT(((Number) rate).doubleValue());
                    } else if (rate instanceof String) {
                        existingUser.setHourlyRateOT(Double.parseDouble((String) rate));
                    }
                }
                if (profileData.containsKey("baseSalary")) {
                    Object salary = profileData.get("baseSalary");
                    if (salary instanceof Number) {
                        existingUser.setBaseSalary(((Number) salary).doubleValue());
                    } else if (salary instanceof String) {
                        existingUser.setBaseSalary(Double.parseDouble((String) salary));
                    }
                }

                // Handle skills update (Skill Matrix feature)
                if (profileData.containsKey("skills")) {
                    Object skillsObj = profileData.get("skills");
                    Map<String, String> skillsMap = new java.util.HashMap<>();

                    System.out.println("=== AuthController: Processing Skills ===");
                    System.out.println("Skills object type: " + (skillsObj != null ? skillsObj.getClass().getName() : "null"));
                    System.out.println("Skills object value: " + skillsObj);

                    if (skillsObj instanceof Map) {
                        // Handle Map (typical case from JSON deserialization)
                        @SuppressWarnings("unchecked")
                        Map<String, Object> rawMap = (Map<String, Object>) skillsObj;
                        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                            if (entry.getKey() != null && entry.getValue() != null) {
                                skillsMap.put(entry.getKey(), String.valueOf(entry.getValue()));
                            }
                        }
                    } else if (skillsObj instanceof String) {
                        // Handle JSON string (parse it)
                        try {
                            com.google.gson.JsonObject jsonSkills = com.google.gson.JsonParser.parseString((String) skillsObj).getAsJsonObject();
                            for (String key : jsonSkills.keySet()) {
                                skillsMap.put(key, jsonSkills.get(key).getAsString());
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to parse skills JSON string: " + e.getMessage());
                        }
                    }

                    System.out.println("Parsed skills map: " + skillsMap);
                    System.out.println("=========================================");
                }

                return userService.updateUser(existingUser)
                    .<ResponseEntity<Object>>thenApply(updated -> ResponseEntity.ok(Map.of(
                        "message", "Profile updated successfully",
                        "user", updated
                    )));
            })
            .exceptionally(ex -> {
                ex.printStackTrace();
                return ResponseEntity.status(500).body(Map.of("error", "Failed to update profile: " + ex.getMessage()));
            });
    }

    /**
     * Change password endpoint.
     * POST /api/v1/auth/change-password
     * Body: {"userId": "...", "oldPassword": "...", "newPassword": "..."}
     */
    @PostMapping("/change-password")
    @Operation(summary = "Change user password")
    public CompletableFuture<ResponseEntity<Object>> changePassword(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        // Validate required fields
        if (userId == null || userId.isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "userId is required"))
            );
        }
        if (oldPassword == null || oldPassword.isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "oldPassword is required"))
            );
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "newPassword is required"))
            );
        }
        if (newPassword.length() < 3) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 3 characters"))
            );
        }

        return userService.getUserById(userId)
            .thenCompose(user -> {
                if (user == null) {
                    return CompletableFuture.completedFuture(
                        ResponseEntity.status(404).body(Map.of("error", "User not found"))
                    );
                }

                // Verify old password matches
                if (!oldPassword.equals(user.getPassword())) {
                    return CompletableFuture.completedFuture(
                        ResponseEntity.status(401).body(Map.of("error", "Current password is incorrect"))
                    );
                }

                // Update to new password
                user.setPassword(newPassword);

                return userService.updateUser(user)
                    .<ResponseEntity<Object>>thenApply(updated -> ResponseEntity.ok(Map.of(
                        "message", "Password changed successfully"
                    )));
            })
            .exceptionally(ex -> {
                ex.printStackTrace();
                return ResponseEntity.status(500).body(Map.of("error", "Failed to change password: " + ex.getMessage()));
            });
    }

    /**
     * Forgot Password - Send OTP to email.
     * POST /api/v1/auth/forgot-password
     * Body: {"email": "..."}
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Send OTP to user email for password reset")
    public CompletableFuture<ResponseEntity<Object>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "Email is required"))
            );
        }

        CompletableFuture<ResponseEntity<Object>> result = userService.getUserByEmail(email.trim())
            .thenCompose((User user) -> {
                if (user == null) {
                    // Don't reveal if email exists or not for security
                    CompletableFuture<ResponseEntity<Object>> resp = CompletableFuture.completedFuture(
                        ResponseEntity.ok().body((Object) Map.of("message", "If an account exists for this email, an OTP has been sent."))
                    );
                    return resp;
                }

                // Generate 6-digit OTP
                String otp = generateOtp();

                // Set OTP expiry to 10 minutes from now
                LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);
                String expiryString = expiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                // Save OTP to user
                user.setOtp(otp);
                user.setOtpExpiry(expiryString);

                CompletableFuture<ResponseEntity<Object>> updateResult = userService.updateUser(user)
                    .thenApply(updated -> {
                        // Send OTP email
                        try {
                            emailService.sendOtpEmail(email, otp);
                            System.out.println("[AuthController] OTP sent to: " + email + " | OTP: " + otp);
                            return ResponseEntity.ok().body((Object) Map.of(
                                "message", "OTP sent successfully to your email.",
                                "email", email
                            ));
                        } catch (Exception e) {
                            System.err.println("[AuthController] Failed to send OTP email: " + e.getMessage());
                            return ResponseEntity.status(500).body((Object) Map.of(
                                "error", "Failed to send OTP email. Please try again."
                            ));
                        }
                    });
                return updateResult;
            });

        return result.exceptionally(ex -> {
            System.err.println("[AuthController] forgotPassword error: " + ex.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Server error: " + ex.getMessage()));
        });
    }

    /**
     * Reset Password - Verify OTP and set new password.
     * POST /api/v1/auth/reset-password
     * Body: {"email": "...", "otp": "...", "newPassword": "..."}
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset user password using OTP")
    public CompletableFuture<ResponseEntity<Object>> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        // Validate required fields
        if (email == null || email.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "Email is required"))
            );
        }
        if (otp == null || otp.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "OTP is required"))
            );
        }
        if (newPassword == null || newPassword.length() < 3) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 3 characters"))
            );
        }

        return userService.getUserByEmail(email.trim())
            .thenCompose(user -> {
                if (user == null) {
                    return CompletableFuture.completedFuture(
                        ResponseEntity.status(404).body(Map.of("error", "User not found"))
                    );
                }

                // Verify OTP matches
                if (user.getOtp() == null || !user.getOtp().equals(otp.trim())) {
                    return CompletableFuture.completedFuture(
                        ResponseEntity.status(400).body(Map.of("error", "Invalid OTP"))
                    );
                }

                // Verify OTP not expired
                if (user.getOtpExpiry() != null) {
                    try {
                        LocalDateTime expiry = LocalDateTime.parse(user.getOtpExpiry(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        if (LocalDateTime.now().isAfter(expiry)) {
                            return CompletableFuture.completedFuture(
                                ResponseEntity.status(400).body(Map.of("error", "OTP has expired. Please request a new one."))
                            );
                        }
                    } catch (Exception e) {
                        System.err.println("[AuthController] Error parsing OTP expiry: " + e.getMessage());
                    }
                }

                // Update password and clear OTP
                user.setPassword(newPassword);
                user.setOtp(null);
                user.setOtpExpiry(null);

                return userService.updateUser(user)
                    .<ResponseEntity<Object>>thenApply(updated -> ResponseEntity.ok(Map.of(
                        "message", "Password reset successfully. You can now login with your new password."
                    )));
            })
            .exceptionally(ex -> {
                ex.printStackTrace();
                return ResponseEntity.status(500).body(Map.of("error", "Server error: " + ex.getMessage()));
            });
    }

    /**
     * Generate a random 6-digit OTP.
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit number
        return String.valueOf(otp);
    }
}
