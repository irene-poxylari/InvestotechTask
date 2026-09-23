package com.investotech.accounttransfertask.security.service;


import com.investotech.accounttransfertask.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;


@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    public static final String CUSTOMER_ID_ATTRIBUTE = "authenticatedCustomerId";
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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
//        try {
//            String jwt = parseJwt(request);
//            if (jwt != null && jwtUtil.validateJwtToken(jwt)) {
//                final String username = jwtUtil.getUserFromToken(jwt);
//                final UserDetails userDetails =
//                        userDetailsService.loadUserByUsername(username);
//                UsernamePasswordAuthenticationToken authenticationToken =
//                        new UsernamePasswordAuthenticationToken(
//                                userDetails,
//                                null,
//                                userDetails.getAuthorities());
//                authenticationToken.setDetails(new WebAuthenticationDetailsSource()
//                        .buildDetails(request));
//                SecurityContextHolder.getContext()
//                        .setAuthentication(authenticationToken);
//            }
//        } catch (Exception e) {
//        throw new Exception(e);

    //    }
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith(BEARER_PREFIX)) {
            return headerAuth.substring(BEARER_PREFIX.length());
        }
        return null;
    }

}
