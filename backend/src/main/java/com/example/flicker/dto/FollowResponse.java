package com.example.flicker.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FollowResponse {
    private Long targetUserId;
    private boolean following; // true if followed, false if unfollowed
    private long totalFollowers;
    private long totalFollowing;
}