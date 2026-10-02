package com.example.flicker.controller;

import com.example.flicker.dto.FollowResponse;
import com.example.flicker.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users/{userId}/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping
    public ResponseEntity<FollowResponse> toggleFollow(
            @PathVariable Long userId,
            Principal principal
    ) {
        return ResponseEntity.ok(followService.toggleFollow(userId, principal.getName()));
    }
}