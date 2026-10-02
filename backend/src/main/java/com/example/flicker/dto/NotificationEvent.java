package com.example.flicker.dto;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {
    private String eventType;       // e.g. "FOLLOW", "LIKE"
    private Long recipientUserId;   // Recipient user ID
    private String actorUsername;   // <-- Added this field
    private Long actorUserId;       // User who triggered the action
    private String message;         // Alert text
    private Long targetEntityId;       // Post ID or Target User ID
    private Long targetId;          // Post ID (for LIKE) or User ID (for FOLLOW)
    private OffsetDateTime timestamp;
}
