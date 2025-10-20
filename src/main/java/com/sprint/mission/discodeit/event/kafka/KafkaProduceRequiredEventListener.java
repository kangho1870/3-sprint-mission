package com.sprint.mission.discodeit.event.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaProduceRequiredEventListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Async("notificationTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MessageCreatedEvent event) {
        log.info("[KafkaProduceRequiredEventListener] MessageCreatedEvent 수신: channelId={}, authorId={}", event.channelId(), event.authorId());
        try {
            String payload = objectMapper.writeValueAsString(event);
            log.info("[KafkaProduceRequiredEventListener] Kafka 전송 시작: topic=discodeit.MessageCreatedEvent, payload={}", payload);

            kafkaTemplate.send("discodeit.MessageCreatedEvent", payload);
            log.info("[KafkaProduceRequiredEventListener] Kafka 전송 완료: topic=discodeit.MessageCreatedEvent");
        } catch (RuntimeException | JsonProcessingException e) {
            log.error("[KafkaProduceRequiredEventListener] Kafka 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Async("notificationTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RoleUpdatedEvent event) {

        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("discodeit.RoleUpdatedEvent", payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener
    public void on(S3UploadFailedEvent event) {

        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("discodeit.S3UploadFiledEvent", payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener
    public void on(UserLogInOutEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("discodeit.UserLogInOutEvent", payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserEvent event) {

        String payload = null;
        try {
            payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("discodeit.UserEvent", payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ChannelEvent event) {

        String payload = null;
        try {
            payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("discodeit.ChannelEvent", payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
