package com.example.flicker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record UserProfileResponse(
        String username,
        String bio,
        String profileImageUrl,
        long followerCount,
        long followingCount,
        Instant createdAt,
        @JsonProperty("isPrivate") boolean isPrivate
) {}