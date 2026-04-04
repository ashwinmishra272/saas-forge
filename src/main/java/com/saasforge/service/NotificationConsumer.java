package com.saasforge.service;

import com.saasforge.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final EmailService emailService;

    @KafkaListener(
            topics = "${kafka.topics.notification-events:notification-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}"
    )
    public void handleNotification(NotificationEvent event) {
        log.info("Received {} event for {}", event.type(), event.toEmail());

        try {
            switch (event.type()) {
                case NotificationEvent.PASSWORD_RESET ->
                        emailService.sendPasswordResetEmail(event.toEmail(), event.token());

                case NotificationEvent.INVITATION_SENT ->
                        emailService.sendInvitationEmail(
                                event.toEmail(),
                                event.token(),
                                event.tenantName()
                        );

                default -> log.warn("Unknown notification type: {}", event.type());
            }

            log.info("Successfully processed {} event for {}", event.type(), event.toEmail());

        } catch (Exception e) {
            log.error("Failed to process {} event for {}: {}",
                    event.type(), event.toEmail(), e.getMessage());
        }
    }
}
