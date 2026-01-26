package com.priyanshu.documents.document_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final String secret = "dummuy_secret_key_for_jwt_signing_purposes_only"; // move to application.yml

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        
        String header = request.getHeader("Authorization");
        System.out.println("JWT Header: " + header);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            Claims claims = Jwts.parserBuilder()
                                .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
                                .build()
                                .parseClaimsJws(token)
                                .getBody();


            String subject = claims.getSubject(); // userId OR service name
            String type = claims.get("type", String.class); // "user" or "service"

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            if ("service".equals(type)) {
                authorities.add(new SimpleGrantedAuthority("SERVICE"));
            } else {
                authorities.add(new SimpleGrantedAuthority("USER"));
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(subject, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
