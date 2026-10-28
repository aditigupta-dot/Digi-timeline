package com.digitaltimeline.web;

import com.digitaltimeline.dto.AuthDtos.AuthResponse;
import com.digitaltimeline.dto.AuthDtos.LoginRequest;
import com.digitaltimeline.dto.AuthDtos.ProfileUpdate;
import com.digitaltimeline.dto.AuthDtos.RegisterRequest;
import com.digitaltimeline.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        return auth.register(r);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        return auth.login(r);
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal Jwt jwt) {
        return auth.profile(CurrentUser.id(jwt));
    }

    @PutMapping("/me")
    public AuthResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileUpdate u) {
        return auth.updateProfile(CurrentUser.id(jwt), u);
    }
}
