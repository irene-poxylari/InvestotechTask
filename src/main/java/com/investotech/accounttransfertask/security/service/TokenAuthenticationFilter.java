package com.investotech.accounttransfertask.security.service;


import com.investotech.accounttransfertask.exceptions.ApiError;
import com.investotech.accounttransfertask.security.JwtUtil;
import com.investotech.accounttransfertask.security.UserAuthentication;
import io.jsonwebtoken.lang.Collections;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;


@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    public static final String USER_ID_ATTRIBUTE = "authenticatedUserId";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectMapper objectMapper;

    private final JwtUtil jwtUtil;
    public TokenAuthenticationFilter(ObjectMapper objectMapper,JwtUtil jwtUtil) {
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/auth/login".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "Missing bearer token");
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        UUID userId = jwtUtil.validateAndGetUserId(token).orElse(null);
        if (userId == null) {
            writeUnauthorized(response, "Invalid or expired token");
            return;
        }

        System.out.println("FILTER userId = " + userId);

        request.setAttribute(USER_ID_ATTRIBUTE, userId);

        System.out.println("BEFORE FILTER CHAIN");



        System.out.println("AFTER FILTER CHAIN");
        // Important for Spring Security
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.emptyList()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiError.of("unauthorized", message));
    }

}
