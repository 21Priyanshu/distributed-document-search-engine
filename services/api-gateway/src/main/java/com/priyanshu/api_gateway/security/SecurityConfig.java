package com.priyanshu.api_gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        logger.info("Configuring security filter chain");
        
        try {
            return http
                .csrf(csrf -> {
                    csrf.disable();
                    logger.debug("CSRF protection disabled");
                })
                .authorizeExchange(ex -> {
                    ex.pathMatchers("/auth/**", "/actuator/health").permitAll();
                    ex.anyExchange().authenticated();
                    logger.debug("Authorization rules configured - public paths: /auth/**, /actuator/health");
                })
                .oauth2ResourceServer(oauth -> {
                    oauth.jwt();
                    logger.debug("OAuth2 resource server configured with JWT");
                })
                .build();
        } catch (Exception e) {
            logger.error("Error configuring security filter chain", e);
            throw new RuntimeException("Security configuration failed", e);
        }
    }
}
