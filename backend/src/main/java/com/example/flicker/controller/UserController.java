package com.example.flicker.controller;


import com.example.flicker.dto.UpdatePrivacyRequest;
import com.example.flicker.dto.UpdateProfileRequest;
import com.example.flicker.dto.UserProfileResponse;
import com.example.flicker.dto.UserSearchResponse;
import com.example.flicker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam(name = "q", required = false) String query,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userService.searchUsers(query, currentUsername(authentication)));
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(userService.getProfile(username));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(currentUsername(authentication), request));
    }

    @PatchMapping("/me/privacy")
    public ResponseEntity<UserProfileResponse> updatePrivacy(
            Authentication authentication,
            @Valid @RequestBody UpdatePrivacyRequest request
    ) {
        return ResponseEntity.ok(userService.updatePrivacy(currentUsername(authentication), request));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> uploadAvatar(
            Authentication authentication,
            @RequestPart("avatar") MultipartFile avatar
    ) {
        return ResponseEntity.ok(userService.uploadAvatar(currentUsername(authentication), avatar));
    }

    private String currentUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authentication.getName();
    }
}
