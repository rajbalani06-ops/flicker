package com.example.flicker.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class PostResponse{
    private Long id;
    private String content;
    private Long authorId;
    private String authorUsername;
    private String authorAvatarUrl;
    private OffsetDateTime createdAt;
}
