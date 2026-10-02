package com.example.flicker.service;

import com.example.flicker.Repository.FollowRepository;
import com.example.flicker.Repository.CommentRepository;
import com.example.flicker.Repository.LikeRepository;
import com.example.flicker.Repository.NotificationRepository;
import com.example.flicker.Repository.PostRepository;
import com.example.flicker.Repository.UserRepository;
import com.example.flicker.dto.PostCreateRequest;
import com.example.flicker.dto.PostResponse;
import com.example.flicker.model.Post;
import com.example.flicker.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public PostResponse createPost(PostCreateRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Post post = Post.builder()
                .content(request.getContent())
                .author(user)
                .build();

        Post saved = postRepository.save(post);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getUserFeed(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return getFollowingFeed(username, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(Pageable pageable) {
        return postRepository.findAllWithAuthor(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(Long userId, Pageable pageable) {
        return postRepository.findByAuthorId(userId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return postRepository.findByAuthorId(user.getId(), pageable).map(this::mapToResponse);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requestingUsername = authentication == null ? null : authentication.getName();
        if (!post.getAuthor().getUsername().equals(requestingUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own posts");
        }

        notificationRepository.deletePostNotifications(postId, List.of("LIKE", "COMMENT"));
        commentRepository.deleteByPostId(postId);
        likeRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getFollowingFeed(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<Long> followingIds = followRepository.findFollowingIdsByFollowerId(user.getId());

        if (followingIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return postRepository.findByAuthorIdIn(followingIds, pageable).map(this::mapToResponse);
    }

    private PostResponse mapToResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .authorId(post.getAuthor().getId())
                .authorUsername(post.getAuthor().getUsername())
                .authorAvatarUrl(post.getAuthor().getAvatarUrl())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
