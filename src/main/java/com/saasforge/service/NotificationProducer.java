package com.saasforge.service;

import com.saasforge.config.KafkaTopics;
import com.saasforge.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public void publishPasswordReset(String toEmail, String token) {
        NotificationEvent event = new NotificationEvent(
                NotificationEvent.PASSWORD_RESET,
                toEmail,
                token,
                null
        );
        publish(event);
    }

    public void publishInvitation(String toEmail, String token, String tenantName) {
        NotificationEvent event = new NotificationEvent(
                NotificationEvent.INVITATION_SENT,
                toEmail,
                token,
                tenantName
        );
        publish(event);
    }

    private void publish(NotificationEvent event) {
        CompletableFuture<SendResult<String, NotificationEvent>> future =
                kafkaTemplate.send(KafkaTopics.NOTIFICATION_EVENTS, event.toEmail(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} event to {} : {}",
                        event.type(), event.toEmail(), ex.getMessage());
            } else {
                log.info("Published {} event for {} to partition {}",
                        event.type(), event.toEmail(),
                        result.getRecordMetadata().partition());
            }
        });
    }
}
