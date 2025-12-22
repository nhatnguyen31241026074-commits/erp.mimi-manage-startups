package com.techforge.erp.config;

import com.techforge.erp.model.User;
import com.techforge.erp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * RoleInterceptor enforces RBAC for API endpoints using the X-Requester-ID header.
 * Uses ResponseStatusException for proper HTTP status codes.
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RoleInterceptor.class);
    private static final int FIREBASE_TIMEOUT_SECONDS = 5;

    private final UserService userService;

    @Autowired
    public RoleInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Allow CORS preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Remove context path if present
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }

        // Allow public GET for clients list (Firebase-style data read) so frontend can populate dropdowns
        if (path.startsWith("/api/v1/clients") && "GET".equalsIgnoreCase(method)) {
            logger.debug("Allowing public GET to {}", path);
            return true;
        }

        String requesterId = request.getHeader("X-Requester-ID");
        if (requesterId == null || requesterId.isEmpty()) {
            logger.warn("Missing X-Requester-ID header");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-Requester-ID header");
        }

        User user;
        try {
            logger.info("Fetching user from Firebase: userId={}", requesterId);

            // Add timeout to prevent indefinite blocking
            user = userService.getUserById(requesterId)
                    .orTimeout(FIREBASE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();

            logger.info("Firebase fetch completed: userId={}, found={}", requesterId, user != null);
        } catch (CompletionException e) {
            if (e.getCause() instanceof TimeoutException) {
                logger.error("Firebase timeout after {}s for userId={}", FIREBASE_TIMEOUT_SECONDS, requesterId);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Database timeout - please try again");
            }
            logger.warn("Error fetching user {}: {}", requesterId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        } catch (Exception e) {
            logger.warn("Unexpected error fetching user {}: {}", requesterId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during authentication");
        }

        if (user == null) {
            logger.warn("User not found: {}", requesterId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        logger.debug("Authorization check: userId={}, role={}, method={}, path={}", requesterId, user.getRole(), method, path);

        // RBAC logic
        // /api/v1/finance/** -> ADMIN, FINANCE (MANAGER allowed GET only)
        if (path.startsWith("/api/v1/finance")) {
            if (user.hasRole("ADMIN", "FINANCE")) return true;
            if (user.hasRole("MANAGER") && "GET".equalsIgnoreCase(method)) return true;
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }

        // /api/v1/ai/** -> ADMIN, MANAGER, EMPLOYEE
        if (path.startsWith("/api/v1/ai")) {
            if (user.hasRole("ADMIN", "MANAGER", "EMPLOYEE")) return true;
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }

        // /api/v1/users/** -> ADMIN only
        if (path.startsWith("/api/v1/users")) {
            if (user.hasRole("ADMIN")) return true;
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }

        // /api/v1/projects/** -> GET allowed to everyone; POST/PUT/DELETE for ADMIN, MANAGER
        if (path.startsWith("/api/v1/projects")) {
            if ("GET".equalsIgnoreCase(method)) return true; // everyone allowed to read
            if (user.hasRole("ADMIN", "MANAGER")) return true; // write allowed for admin/manager
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }

        // /api/v1/tasks/** -> GET allowed to authenticated users; POST/PUT/PATCH for MANAGER, EMPLOYEE, ADMIN
        if (path.startsWith("/api/v1/tasks")) {
            if ("GET".equalsIgnoreCase(method)) {
                // All authenticated users can read tasks
                return true;
            }
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
                // MANAGER, EMPLOYEE, ADMIN can create/update tasks (for Kanban drag-and-drop)
                if (user.hasRole("ADMIN", "MANAGER", "EMPLOYEE")) return true;
            }
            if ("DELETE".equalsIgnoreCase(method)) {
                // Only ADMIN and MANAGER can delete tasks
                if (user.hasRole("ADMIN", "MANAGER")) return true;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }

        // /api/v1/worklogs/** -> MANAGER, EMPLOYEE, ADMIN can create/read
        if (path.startsWith("/api/v1/worklogs")) {
            if (user.hasRole("ADMIN", "MANAGER", "EMPLOYEE")) return true;
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }

        // Default: for any /api/v1/** path not covered above, deny
        if (path.startsWith("/api/v1/")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }

        // Non-api paths: allow through
        return true;
    }
}
