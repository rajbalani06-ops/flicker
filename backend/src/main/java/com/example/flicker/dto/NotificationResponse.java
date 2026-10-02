package com.example.flicker.dto;


import lombok.Builder;
import lombok.Getter;
import java.time.OffsetDateTime;

@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private Long actorId;
    private String actorUsername;
    private String type;
    private String message;
    private Long targetId;
    private boolean isRead;
    private OffsetDateTime createdAt;
}
