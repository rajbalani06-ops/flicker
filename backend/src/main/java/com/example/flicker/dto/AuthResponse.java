package com.example.flicker.dto;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        String username,
        String email,
        String avatarUrl,
        String role,
        Instant expiresAt
) {}
