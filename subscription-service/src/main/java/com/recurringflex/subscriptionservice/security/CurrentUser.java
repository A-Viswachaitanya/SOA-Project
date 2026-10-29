package com.recurringflex.subscriptionservice.security;

import com.recurringflex.subscriptionservice.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Reads the caller identity that the API Gateway forwards after validating the JWT.
 * Only trusted because GatewayAuthFilter rejects requests that did not come through the gateway.
 */
public final class CurrentUser {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String ROLE_HEADER = "X-User-Role";

    private CurrentUser() {}

    public static Long id() {
        HttpServletRequest request = request();
        String value = request != null ? request.getHeader(USER_ID_HEADER) : null;
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String role() {
        HttpServletRequest request = request();
        return request != null ? request.getHeader(ROLE_HEADER) : null;
    }

    // ADMIN users and internal service-to-service calls (SYSTEM) have full access
    public static boolean isAdmin() {
        String role = role();
        return "ADMIN".equalsIgnoreCase(role) || "SYSTEM".equalsIgnoreCase(role);
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new ForbiddenException("Access denied. Admin privileges required.");
        }
    }

    public static void requireSelfOrAdmin(Long ownerId) {
        if (isAdmin()) {
            return;
        }
        Long callerId = id();
        if (callerId == null || !callerId.equals(ownerId)) {
            throw new ForbiddenException("Access denied. You can only access your own resources.");
        }
    }

    private static HttpServletRequest request() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
