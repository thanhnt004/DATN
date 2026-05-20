package org.example.identityservice.event.producer;

import event.EventMetadata;
import event.EventWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.identityservice.event.model.PasswordResetRequestDTO;
import org.example.identityservice.event.model.UserRegistrationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdentityProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.user-events:user-events}")
    private String userEventsTopic;

    public void publishUserCreatedEvent(UserRegistrationDTO payload) {
        EventMetadata metadata = new EventMetadata(
                UUID.randomUUID().toString(),
                "USER_REGISTERED",
                "identity-service",
                Instant.now()
        );

        EventWrapper<UserRegistrationDTO> eventWrapper = new EventWrapper<>(metadata, payload);

        kafkaTemplate.send(userEventsTopic, String.valueOf(payload.getUserId()), eventWrapper)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("USER_REGISTERED event sent successfully! ID: {}, Offset: {}",
                                metadata.eventId(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send USER_REGISTERED event ID: {}. Error: {}",
                                metadata.eventId(), ex.getMessage());
                    }
                });
    }

    public void publishPasswordResetEvent(PasswordResetRequestDTO payload) {
        EventMetadata metadata = new EventMetadata(
                UUID.randomUUID().toString(),
                "PASSWORD_RESET_REQUESTED",
                "identity-service",
                Instant.now()
        );

        EventWrapper<PasswordResetRequestDTO> eventWrapper = new EventWrapper<>(metadata, payload);

        kafkaTemplate.send(userEventsTopic, String.valueOf(payload.getUserId()), eventWrapper)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("PASSWORD_RESET_REQUESTED event sent successfully! ID: {}, User: {}",
                                metadata.eventId(), payload.getEmail());
                    } else {
                        log.error("Failed to send PASSWORD_RESET_REQUESTED event. Error: {}",
                                ex.getMessage());
                    }
                });
    }
}
