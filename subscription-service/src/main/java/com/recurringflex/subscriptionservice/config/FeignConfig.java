package com.recurringflex.subscriptionservice.config;

import com.recurringflex.subscriptionservice.security.CurrentUser;
import com.recurringflex.subscriptionservice.security.GatewayAuthFilter;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Authenticates service-to-service Feign calls. They go straight to the target
 * instance (load-balanced through Eureka), not through the gateway, so they carry
 * the internal secret and the SYSTEM role themselves.
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalAuthInterceptor(@Value("${internal.secret}") String internalSecret) {
        return template -> {
            template.header(GatewayAuthFilter.INTERNAL_SECRET_HEADER, internalSecret);
            template.header(CurrentUser.ROLE_HEADER, "SYSTEM");
            // Forward the end user's id for auditing; absent for scheduler-triggered calls
            Long userId = CurrentUser.id();
            if (userId != null) {
                template.header(CurrentUser.USER_ID_HEADER, userId.toString());
            }
        };
    }
}
