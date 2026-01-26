package com.priyanshu.documents.document_service.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.priyanshu.documents.document_service.security.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public Map<String,String> login(@RequestParam String userId) {
        try {
            String token = jwtService.generateToken(userId);
            logger.info("Token generated for user: {}", userId);
            return Map.of("token", token);
        } catch (Exception e) {
            logger.error("Error generating token for user: {}", userId, e);
            throw new RuntimeException("Failed to generate token");
        }
    }
}
