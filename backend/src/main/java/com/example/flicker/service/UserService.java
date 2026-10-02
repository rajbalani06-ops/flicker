package com.example.flicker.service;

import com.example.flicker.Repository.FollowRepository;
import com.example.flicker.Repository.UserRepository;
import com.example.flicker.dto.UpdateProfileRequest;
import com.example.flicker.dto.UpdatePrivacyRequest;
import com.example.flicker.dto.UserProfileResponse;
import com.example.flicker.dto.UserSearchResponse;
import com.example.flicker.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Value("${flicker.upload-dir:uploads/avatars}")
    private String uploadDirectory;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        return toProfileResponse(findUser(username));
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(String query, String currentUsername) {
        if (query == null || query.isBlank()) return List.of();
        User currentUser = findUser(currentUsername);
        return userRepository.searchUsers(query.trim(), currentUser.getId(), PageRequest.of(0, 20))
                .stream()
                .map(user -> new UserSearchResponse(
                        user.getId(), user.getUsername(),
                        user.getDisplayName() == null || user.getDisplayName().isBlank()
                                ? user.getUsername() : user.getDisplayName(),
                        user.getAvatarUrl()))
                .toList();
    }

    @Transactional
    public UserProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = findUser(username);
        user.setBio(request.bio());
        return toProfileResponse(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse updatePrivacy(String username, UpdatePrivacyRequest request) {
        User user = findUser(username);
        user.setPrivate(Boolean.TRUE.equals(request.isPrivate()));
        return toProfileResponse(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse uploadAvatar(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an image to upload");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image must be 5 MB or smaller");
        }

        String extension = switch (file.getContentType() == null ? "" : file.getContentType().toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use a JPEG, PNG, GIF, or WebP image");
        };

        User user = findUser(username);
        Path directory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        Path destination = directory.resolve(UUID.randomUUID() + extension).normalize();
        if (!destination.startsWith(directory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upload path");
        }

        try {
            Files.createDirectories(directory);
            file.transferTo(destination);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store avatar", exception);
        }

        user.setAvatarUrl("/uploads/avatars/" + destination.getFileName());
        return toProfileResponse(userRepository.save(user));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getUsername(),
                user.getBio(),
                user.getAvatarUrl(),
                followRepository.countByFollowingId(user.getId()),
                followRepository.countByFollowerId(user.getId()),
                user.getCreatedAt(),
                user.isPrivate()
        );
    }
}