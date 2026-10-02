package com.example.flicker.service;

import com.example.flicker.Repository.UserRepository;
import com.example.flicker.dto.AuthResponse;
import com.example.flicker.dto.LoginRequest;
import com.example.flicker.dto.RegisterRequest;
import com.example.flicker.model.Role;
import com.example.flicker.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.access-expiration}")
    private long accessTokenExpirationMs;

    @Transactional
    public void register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username) || userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email is already taken");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.username().trim();
        User user = (identifier.contains("@")
                ? userRepository.findByEmailIgnoreCase(identifier.toLowerCase(Locale.ROOT))
                : userRepository.findByUsername(identifier))
                .orElseThrow(AuthService::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw invalidCredentials();
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                user.getUsername(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getRole().name(),
                Instant.now().plusMillis(accessTokenExpirationMs)
        );
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Invalid credentials. Please check your username and password.");
    }
}
