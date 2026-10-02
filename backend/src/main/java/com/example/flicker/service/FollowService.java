package com.example.flicker.service;

import com.example.flicker.Repository.FollowRepository;
import com.example.flicker.Repository.UserRepository;
import com.example.flicker.dto.FollowResponse;
import com.example.flicker.dto.NotificationEvent;
import com.example.flicker.model.Follow;
import com.example.flicker.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;

    @Transactional
    public FollowResponse toggleFollow(Long targetUserId, String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

        if (currentUser.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Users cannot follow themselves.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found: " + targetUserId));

        Optional<Follow> existingFollow = followRepository.findByFollowerIdAndFollowingId(
                currentUser.getId(), targetUser.getId()
        );

        boolean isFollowing;
        if (existingFollow.isPresent()) {
            followRepository.delete(existingFollow.get());
            isFollowing = false;
        } else {
            Follow follow = Follow.builder()
                    .follower(currentUser)
                    .following(targetUser)
                    .build();
            followRepository.save(follow);
            isFollowing = true;
        }

        // Dispatch notification asynchronously via RabbitMQ if a new follow occurred
        if (isFollowing) {
            NotificationEvent event = NotificationEvent.builder()
                    .eventType("FOLLOW")
                    .recipientUserId(targetUser.getId())
                    .actorUserId(currentUser.getId())
                    .actorUsername(currentUser.getUsername())
                    .targetEntityId(targetUser.getId())
                    .message(currentUser.getUsername() + " started following you!")
                    .timestamp(OffsetDateTime.now())
                    .build();
            notificationProducer.sendNotification(event);
        }

        long followersCount = followRepository.countByFollowingId(targetUserId);
        long followingCount = followRepository.countByFollowerId(targetUserId);

        return FollowResponse.builder()
                .targetUserId(targetUserId)
                .following(isFollowing)
                .totalFollowers(followersCount)
                .totalFollowing(followingCount)
                .build();
    }
}