package com.example.flicker.service;

import com.example.flicker.Repository.ConversationRepository;
import com.example.flicker.Repository.DirectMessageRepository;
import com.example.flicker.Repository.UserRepository;
import com.example.flicker.dto.ConversationResponse;
import com.example.flicker.dto.DirectMessageResponse;
import com.example.flicker.dto.SendMessageRequest;
import com.example.flicker.model.Conversation;
import com.example.flicker.model.DirectMessage;
import com.example.flicker.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(String username) {
        User currentUser = findUser(username);
        return conversationRepository.findByUserOne_IdOrUserTwo_IdOrderByUpdatedAtDesc(
                        currentUser.getId(), currentUser.getId())
                .stream()
                .map(conversation -> toConversationResponse(conversation, currentUser))
                .toList();
    }

    @Transactional
    public ConversationResponse openConversation(String username, String otherUsername) {
        User currentUser = findUser(username);
        User otherUser = findUser(otherUsername);
        if (currentUser.getId().equals(otherUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot start a conversation with yourself");
        }

        Conversation conversation = conversationRepository.findBetweenUsers(currentUser.getId(), otherUser.getId())
                .orElseGet(() -> {
                    boolean currentFirst = currentUser.getId() < otherUser.getId();
                    return conversationRepository.save(Conversation.builder()
                            .userOne(currentFirst ? currentUser : otherUser)
                            .userTwo(currentFirst ? otherUser : currentUser)
                            .build());
                });
        return toConversationResponse(conversation, currentUser);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        User currentUser = findUser(username);
        return messageRepository.countUnreadForUser(currentUser.getId());
    }

    @Transactional
    public void markConversationRead(String username, Long conversationId) {
        User currentUser = findUser(username);
        requireParticipant(conversationId, currentUser);
        messageRepository.markIncomingMessagesRead(conversationId, currentUser.getId());
    }

    @Transactional
    public List<DirectMessageResponse> getMessages(String username, Long conversationId) {
        User currentUser = findUser(username);
        Conversation conversation = requireParticipant(conversationId, currentUser);
        messageRepository.markIncomingMessagesRead(conversationId, currentUser.getId());
        return messageRepository.findByConversation_IdOrderByCreatedAtAscIdAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public DirectMessageResponse sendMessage(String username, Long conversationId, SendMessageRequest request) {
        User sender = findUser(username);
        Conversation conversation = requireParticipant(conversationId, sender);
        String content = request.content().trim();
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }

        DirectMessage message = messageRepository.save(DirectMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .build());
        conversation.setUpdatedAt(OffsetDateTime.now());
        conversationRepository.save(conversation);
        return toMessageResponse(message);
    }

    @Transactional
    public void deleteMessage(String username, Long messageId) {
        User currentUser = findUser(username);
        DirectMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        requireParticipant(message.getConversation().getId(), currentUser);
        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own messages");
        }
        messageRepository.delete(message);
    }

    @Transactional
    public void clearConversation(String username, Long conversationId) {
        User currentUser = findUser(username);
        Conversation conversation = requireParticipant(conversationId, currentUser);
        messageRepository.deleteConversationMessages(conversationId);
        conversation.setUpdatedAt(OffsetDateTime.now());
        conversationRepository.save(conversation);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Conversation requireParticipant(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!conversation.getUserOne().getId().equals(user.getId())
                && !conversation.getUserTwo().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not part of this conversation");
        }
        return conversation;
    }

    private ConversationResponse toConversationResponse(Conversation conversation, User currentUser) {
        User otherUser = conversation.getUserOne().getId().equals(currentUser.getId())
                ? conversation.getUserTwo() : conversation.getUserOne();
        String lastMessage = messageRepository.findFirstByConversation_IdOrderByCreatedAtDescIdDesc(conversation.getId())
                .map(DirectMessage::getContent)
                .orElse("");
        long unreadCount = messageRepository.countUnreadFromOtherUser(conversation.getId(), currentUser.getId());
        return new ConversationResponse(
                conversation.getId(),
                otherUser.getUsername(),
                otherUser.getDisplayName() == null || otherUser.getDisplayName().isBlank()
                        ? otherUser.getUsername() : otherUser.getDisplayName(),
                otherUser.getAvatarUrl(),
                lastMessage,
                conversation.getUpdatedAt(),
                unreadCount
        );
    }

    private DirectMessageResponse toMessageResponse(DirectMessage message) {
        return new DirectMessageResponse(
                message.getId(), message.getSender().getUsername(), message.getContent(), message.getCreatedAt());
    }
}
