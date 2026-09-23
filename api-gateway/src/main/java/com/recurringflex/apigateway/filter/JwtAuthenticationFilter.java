package com.recurringflex.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:RecurringFlexSecretKeyForJWTAuthenticationMustBe256Bits}")
    private String secret;

    // Public endpoints that don't require authentication
    private final List<String> openEndpoints = List.of(
            "/api/users/register",
            "/api/users/login",
            "/swagger-ui",
            "/v3/api-docs",
            "/fallback"
    );

    // Endpoints restricted to ADMIN role only
    private final Map<String, List<String>> roleRestrictedEndpoints = Map.of(
            "ADMIN", List.of(
                    "/api/plans:POST",
                    "/api/plans:PUT",
                    "/api/plans:DELETE"
            )
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for open endpoints
        if (isOpenEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Check for Authorization header
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            if (username == null) {
                username = userId;
            }
            String role = claims.get("role", String.class);

            // Role-based access control check
            if (!isAuthorized(path, request.getMethod().name(), role)) {
                return onError(exchange, "Access denied. Insufficient privileges.", HttpStatus.FORBIDDEN);
            }

            // Add user info to headers for downstream services
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role != null ? role : "USER")
                    .header("X-User-Name", username)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isOpenEndpoint(String path) {
        return openEndpoints.stream().anyMatch(path::startsWith);
    }

    private boolean isAuthorized(String path, String method, String role) {
        // Check if the endpoint+method combination is restricted to ADMIN
        List<String> adminEndpoints = roleRestrictedEndpoints.get("ADMIN");
        if (adminEndpoints != null) {
            for (String endpoint : adminEndpoints) {
                String[] parts = endpoint.split(":");
                String restrictedPath = parts[0];
                String restrictedMethod = parts[1];
                if (path.startsWith(restrictedPath) && method.equalsIgnoreCase(restrictedMethod)) {
                    return "ADMIN".equalsIgnoreCase(role);
                }
            }
        }
        // All other endpoints are accessible to any authenticated user
        return true;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        String body = String.format("{\"timestamp\":%d,\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                System.currentTimeMillis(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getURI().getPath());
        org.springframework.core.io.buffer.DataBuffer buffer =
                exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
