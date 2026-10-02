package com.example.flicker.controller;

import com.example.flicker.dto.ConversationResponse;
import com.example.flicker.dto.DirectMessageResponse;
import com.example.flicker.dto.SendMessageRequest;
import com.example.flicker.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/unread-count")
    public ResponseEntity<java.util.Map<String, Long>> getUnreadCount(Authentication authentication) {
        return ResponseEntity.ok(java.util.Map.of("count", messageService.getUnreadCount(currentUsername(authentication))));
    }

    @PatchMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markConversationRead(
            @PathVariable Long conversationId, Authentication authentication) {
        messageService.markConversationRead(currentUsername(authentication), conversationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations(Authentication authentication) {
        return ResponseEntity.ok(messageService.getConversations(currentUsername(authentication)));
    }

    @PostMapping("/conversations/with/{username}")
    public ResponseEntity<ConversationResponse> openConversation(
            @PathVariable String username, Authentication authentication) {
        return ResponseEntity.ok(messageService.openConversation(currentUsername(authentication), username));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<List<DirectMessageResponse>> getMessages(
            @PathVariable Long conversationId, Authentication authentication) {
        return ResponseEntity.ok(messageService.getMessages(currentUsername(authentication), conversationId));
    }

    @PostMapping("/conversations/{conversationId}")
    public ResponseEntity<DirectMessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendMessage(currentUsername(authentication), conversationId, request));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId, Authentication authentication) {
        messageService.deleteMessage(currentUsername(authentication), messageId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> clearConversation(
            @PathVariable Long conversationId, Authentication authentication) {
        messageService.clearConversation(currentUsername(authentication), conversationId);
        return ResponseEntity.noContent().build();
    }

    private String currentUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authentication.getName();
    }
}
