package com.priyanshu.api_gateway.security;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import io.jsonwebtoken.security.Keys;

@Configuration
public class JwtDecoderConfig {

    private static final Logger logger = LoggerFactory.getLogger(JwtDecoderConfig.class);
    private static final String SECRET = "my-super-secret-key-32-bytes-min";

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        logger.info("Initializing JWT Decoder");
        
        try {
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
            ReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(key).build();
            logger.info("JWT Decoder initialized successfully");
            return decoder;
        } catch (Exception e) {
            logger.error("Failed to initialize JWT Decoder", e);
            throw new RuntimeException("JWT Decoder initialization failed", e);
        }
    }
}
