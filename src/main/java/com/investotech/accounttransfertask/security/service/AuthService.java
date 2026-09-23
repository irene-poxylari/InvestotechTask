package com.investotech.accounttransfertask.security.service;

import com.investotech.accounttransfertask.exceptions.UnauthorizedException;
import com.investotech.accounttransfertask.repository.UserRepository;
import com.investotech.accounttransfertask.entity.User;
import com.investotech.accounttransfertask.security.ApiKeyHasher;
import com.investotech.accounttransfertask.security.JwtUtil;
import com.investotech.accounttransfertask.security.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public LoginResponse login(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("API key is required");
        }

        String apiKeyHash = apiKeyHasher.sha256(apiKey);

        User user = userRepository.findByApiKeyHash(apiKeyHash)
                .orElseThrow(() ->
                        new UnauthorizedException("Invalid API key")
                );

        String token = jwtUtil.generateToken(user.getId());

        return new LoginResponse(token, "Bearer");
    }
}
