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

/**
 * RoleInterceptor enforces RBAC for API endpoints using the X-Requester-ID header.
 * Uses ResponseStatusException for proper HTTP status codes.
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(RoleInterceptor.class);

    private final UserService userService;

    @Autowired
    public RoleInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Allow CORS preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String requesterId = request.getHeader("X-Requester-ID");
        if (requesterId == null || requesterId.isEmpty()) {
            logger.warn("Missing X-Requester-ID header");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-Requester-ID header");
        }

        User user;
        try {
            // Blocking fetch as requested. Consider caching in production.
            user = userService.getUserById(requesterId).join();
        } catch (Exception e) {
            logger.warn("Error fetching user {}", requesterId, e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        if (user == null) {
            logger.warn("User not found: {}", requesterId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Remove context path if present
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
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

        // Default: for any /api/v1/** path not covered above, deny
        if (path.startsWith("/api/v1/")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }

        // Non-api paths: allow through
        return true;
    }
}
