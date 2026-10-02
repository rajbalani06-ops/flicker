package com.example.flicker.service;

import com.example.flicker.Repository.NotificationRepository;
import com.example.flicker.Repository.UserRepository;
import com.example.flicker.dto.NotificationEvent;
import com.example.flicker.model.Notification;
import com.example.flicker.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @RabbitListener(queues = "flicker.notification.queue")
    @Transactional
    public void consumeNotification(NotificationEvent event) {
        log.info("Processing RabbitMQ event: [Type: {}] to User ID: {}", event.getEventType(), event.getRecipientUserId());

        User recipient = userRepository.findById(event.getRecipientUserId()).orElse(null);
        User actor = userRepository.findById(event.getActorUserId()).orElse(null);

        if (recipient == null || actor == null) {
            log.warn("Skipping notification: recipient or actor missing.");
            return;
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(event.getEventType())
                .message(event.getMessage())
                .targetId(event.getTargetId())
                .isRead(false)
                .createdAt(event.getTimestamp())
                .build();

        notificationRepository.save(notification);
        log.info("Notification saved with ID: {}", notification.getId());
    }
}