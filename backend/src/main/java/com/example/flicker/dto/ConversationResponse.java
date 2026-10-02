package com.example.flicker.dto;

import java.time.OffsetDateTime;

public record ConversationResponse(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        String lastMessage,
        OffsetDateTime updatedAt,
        long unreadCount
) {}
