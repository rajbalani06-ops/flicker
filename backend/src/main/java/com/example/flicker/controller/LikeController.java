package com.example.flicker.controller;

import com.example.flicker.dto.LikeResponse;
import com.example.flicker.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{postId}/like")
    public ResponseEntity<LikeResponse> toggleLike(
            @PathVariable("postId") Long postId,
            Principal principal
    ) {
        return ResponseEntity.ok(likeService.toggleLike(postId, principal.getName()));
    }
}