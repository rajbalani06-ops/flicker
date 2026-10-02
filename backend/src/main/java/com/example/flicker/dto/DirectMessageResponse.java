package com.example.flicker.dto;

import java.time.OffsetDateTime;

public record DirectMessageResponse(Long id, String senderUsername, String content, OffsetDateTime createdAt) {}
