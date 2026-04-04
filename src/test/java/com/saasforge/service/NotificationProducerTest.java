package com.saasforge.service;

import com.saasforge.config.KafkaTopics;
import com.saasforge.dto.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationProducerTest {

    @Mock
    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @InjectMocks
    private NotificationProducer notificationProducer;

    private CompletableFuture<SendResult<String, NotificationEvent>> pendingFuture() {
        CompletableFuture<SendResult<String, NotificationEvent>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), any(NotificationEvent.class))).thenReturn(future);
        return future;
    }

    // ── publishPasswordReset ──────────────────────────────────────────────────

    @Test
    void publishPasswordReset_sendsToCorrectTopic() {
        pendingFuture();

        notificationProducer.publishPasswordReset("user@test.com", "reset-token");

        verify(kafkaTemplate).send(eq(KafkaTopics.NOTIFICATION_EVENTS), anyString(), any());
    }

    @Test
    void publishPasswordReset_usesEmailAsKey() {
        pendingFuture();

        notificationProducer.publishPasswordReset("user@test.com", "reset-token");

        verify(kafkaTemplate).send(anyString(), eq("user@test.com"), any());
    }

    @Test
    void publishPasswordReset_buildsCorrectEvent() {
        pendingFuture();

        notificationProducer.publishPasswordReset("user@test.com", "reset-token");

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(kafkaTemplate).send(anyString(), anyString(), captor.capture());

        NotificationEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(NotificationEvent.PASSWORD_RESET);
        assertThat(event.toEmail()).isEqualTo("user@test.com");
        assertThat(event.token()).isEqualTo("reset-token");
        assertThat(event.tenantName()).isNull();
    }

    @Test
    void publishPasswordReset_onKafkaFailure_doesNotThrow() {
        CompletableFuture<SendResult<String, NotificationEvent>> future = pendingFuture();

        notificationProducer.publishPasswordReset("user@test.com", "reset-token");
        future.completeExceptionally(new RuntimeException("Kafka broker unavailable"));

        // callback logs error but must not propagate the exception
    }

    @Test
    void publishPasswordReset_onKafkaSuccess_doesNotThrow() {
        CompletableFuture<SendResult<String, NotificationEvent>> future = pendingFuture();

        notificationProducer.publishPasswordReset("user@test.com", "reset-token");

        @SuppressWarnings("unchecked")
        SendResult<String, NotificationEvent> sendResult = mock(SendResult.class);
        org.apache.kafka.clients.producer.RecordMetadata metadata =
                mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(metadata.partition()).thenReturn(0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);

        future.complete(sendResult);
        // callback logs success but must not throw
    }

    // ── publishInvitation ────────────────────────────────────────────────────

    @Test
    void publishInvitation_sendsToCorrectTopic() {
        pendingFuture();

        notificationProducer.publishInvitation("invitee@test.com", "invite-token", "Acme Corp");

        verify(kafkaTemplate).send(eq(KafkaTopics.NOTIFICATION_EVENTS), anyString(), any());
    }

    @Test
    void publishInvitation_usesEmailAsKey() {
        pendingFuture();

        notificationProducer.publishInvitation("invitee@test.com", "invite-token", "Acme Corp");

        verify(kafkaTemplate).send(anyString(), eq("invitee@test.com"), any());
    }

    @Test
    void publishInvitation_buildsCorrectEvent() {
        pendingFuture();

        notificationProducer.publishInvitation("invitee@test.com", "invite-token", "Acme Corp");

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(kafkaTemplate).send(anyString(), anyString(), captor.capture());

        NotificationEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(NotificationEvent.INVITATION_SENT);
        assertThat(event.toEmail()).isEqualTo("invitee@test.com");
        assertThat(event.token()).isEqualTo("invite-token");
        assertThat(event.tenantName()).isEqualTo("Acme Corp");
    }

    @Test
    void publishInvitation_onKafkaFailure_doesNotThrow() {
        CompletableFuture<SendResult<String, NotificationEvent>> future = pendingFuture();

        notificationProducer.publishInvitation("invitee@test.com", "invite-token", "Acme Corp");
        future.completeExceptionally(new RuntimeException("Kafka broker unavailable"));

        // callback logs error but must not propagate the exception
    }

    @Test
    void publishInvitation_onKafkaSuccess_doesNotThrow() {
        CompletableFuture<SendResult<String, NotificationEvent>> future = pendingFuture();

        notificationProducer.publishInvitation("invitee@test.com", "invite-token", "Acme Corp");

        @SuppressWarnings("unchecked")
        SendResult<String, NotificationEvent> sendResult = mock(SendResult.class);
        org.apache.kafka.clients.producer.RecordMetadata metadata =
                mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(metadata.partition()).thenReturn(2);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);

        future.complete(sendResult);
        // callback logs success but must not throw
    }
}