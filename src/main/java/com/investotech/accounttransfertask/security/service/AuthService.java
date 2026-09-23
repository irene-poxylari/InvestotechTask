package com.investotech.accounttransfertask.security.service;

import com.investotech.accounttransfertask.exceptions.UnauthorizedException;
import com.investotech.accounttransfertask.repository.UserRepository;
import com.investotech.accounttransfertask.entity.User;
import com.investotech.accounttransfertask.security.ApiKeyHasher;
import com.investotech.accounttransfertask.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public String login(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("Missing API key");
        }
        User user = userRepository.findByApiKeyHash(apiKeyHasher.sha256(apiKey))
                .orElseThrow(() -> new UnauthorizedException("Invalid API key"));

        return jwtUtil.generateToken(user.getApiKeyHash());

    }
}
