package com.recurringflex.userservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Tags every response with the instance that served it, so load balancing
 * across replicas is visible (e.g. curl -i shows a different X-Instance-Id per call).
 */
@Component
public class InstanceIdFilter extends OncePerRequestFilter {

    public static final String INSTANCE_HEADER = "X-Instance-Id";

    private final String instancePrefix;

    public InstanceIdFilter(@Value("${spring.application.name}") String applicationName) {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "unknown-host";
        }
        this.instancePrefix = applicationName + ":" + host + ":";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader(INSTANCE_HEADER, instancePrefix + request.getLocalPort());
        chain.doFilter(request, response);
    }
}
