package com.example.flicker.service;

import com.example.flicker.Repository.LikeRepository;
import com.example.flicker.Repository.PostRepository;
import com.example.flicker.Repository.UserRepository;
import com.example.flicker.dto.LikeResponse;
import com.example.flicker.dto.NotificationEvent;
import com.example.flicker.model.Like;
import com.example.flicker.model.Post;
import com.example.flicker.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;

    @Transactional
    public LikeResponse toggleLike(Long postId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + postId));

        Optional<Like> existingLike = likeRepository.findByUserIdAndPostId(user.getId(), post.getId());

        boolean liked;
        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            liked = false;
        } else {
            Like like = Like.builder()
                    .user(user)
                    .post(post)
                    .build();
            likeRepository.save(like);
            liked = true;

            // Dispatch notification only when a new like is created and not on user's own post
            if (!post.getAuthor().getId().equals(user.getId())) {
                NotificationEvent event = NotificationEvent.builder()
                        .eventType("LIKE")
                        .recipientUserId(post.getAuthor().getId())
                        .actorUserId(user.getId())
                        .actorUsername(user.getUsername())
                        .targetEntityId(post.getId())
                        .message(user.getUsername() + " liked your post!")
                        .timestamp(OffsetDateTime.now())
                        .build();
                notificationProducer.sendNotification(event);
            }
        }

        long totalLikes = likeRepository.countByPostId(postId);
        return new LikeResponse(postId, liked, totalLikes);
    }
}