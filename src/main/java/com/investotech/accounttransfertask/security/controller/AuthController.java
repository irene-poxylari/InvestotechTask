package com.investotech.accounttransfertask.security.controller;

import com.investotech.accounttransfertask.security.LoginResponse;
import com.investotech.accounttransfertask.security.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(
            @RequestHeader(value = "X-API-Key", required = false)
            String apiKey
    ) {
        return authService.login(apiKey);
    }
}
