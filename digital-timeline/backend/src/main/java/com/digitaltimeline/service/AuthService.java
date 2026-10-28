package com.digitaltimeline.service;

import com.digitaltimeline.domain.User;
import com.digitaltimeline.dto.AuthDtos.AuthResponse;
import com.digitaltimeline.dto.AuthDtos.LoginRequest;
import com.digitaltimeline.dto.AuthDtos.ProfileUpdate;
import com.digitaltimeline.dto.AuthDtos.RegisterRequest;
import com.digitaltimeline.exception.AuthenticationFailedException;
import com.digitaltimeline.exception.DuplicateResourceException;
import com.digitaltimeline.exception.InvalidRequestException;
import com.digitaltimeline.exception.ResourceNotFoundException;
import com.digitaltimeline.repository.UserRepository;
import com.digitaltimeline.security.JwtService;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final Pattern GITHUB_NAME = Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})$");

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new DuplicateResourceException("That email is already registered");
        User saved = users.save(new User(email, r.displayName().trim(), encoder.encode(r.password())));
        return respond(saved);
    }

    public AuthResponse login(LoginRequest r) {
        User user = users.findByEmail(r.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(AuthenticationFailedException::new);       // same error for unknown email and bad password
        if (!encoder.matches(r.password(), user.getPasswordHash())) throw new AuthenticationFailedException();
        return respond(user);
    }

    public User require(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public AuthResponse profile(UUID id) {
        User u = require(id);
        return new AuthResponse(null, null, u.getId(), u.getDisplayName(), u.getEmail(), u.getGithubUsername());
    }

    public AuthResponse updateProfile(UUID id, ProfileUpdate update) {
        User u = require(id);
        String gh = update.githubUsername() == null ? null : update.githubUsername().trim();
        if (gh != null && !gh.isEmpty() && !GITHUB_NAME.matcher(gh).matches()) {
            throw new InvalidRequestException("That is not a valid GitHub username");
        }
        u.setGithubUsername(gh == null || gh.isEmpty() ? null : gh);
        users.save(u);
        return profile(id);
    }

    private AuthResponse respond(User u) {
        JwtService.Token t = jwt.issue(u);
        return new AuthResponse(t.value(), t.expiresAt(), u.getId(), u.getDisplayName(), u.getEmail(), u.getGithubUsername());
    }
}
