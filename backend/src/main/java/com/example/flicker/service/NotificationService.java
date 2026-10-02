package com.example.flicker.service;


import com.example.flicker.Repository.NotificationRepository;
import com.example.flicker.Repository.UserRepository;
import com.example.flicker.dto.NotificationResponse;
import com.example.flicker.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable)
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .actorId(n.getActor().getId())
                        .actorUsername(n.getActor().getUsername())
                        .type(n.getType())
                        .message(n.getMessage())
                        .targetId(n.getTargetId())
                        .isRead(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build());
    }
}