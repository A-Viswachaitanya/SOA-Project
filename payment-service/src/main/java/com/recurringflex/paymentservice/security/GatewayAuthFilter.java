package com.recurringflex.paymentservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Rejects API calls that bypass the API Gateway (or are not internal Feign calls).
 * Without this, anyone reaching the service port could forge the X-User-* headers.
 */
@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    public static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final byte[] internalSecret;

    public GatewayAuthFilter(@Value("${internal.secret}") String internalSecret) {
        this.internalSecret = internalSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Swagger UI and API docs stay reachable directly
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(INTERNAL_SECRET_HEADER);
        if (provided == null || !MessageDigest.isEqual(internalSecret, provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Direct access is not allowed. Use the API Gateway.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
