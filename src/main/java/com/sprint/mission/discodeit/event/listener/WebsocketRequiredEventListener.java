package com.sprint.mission.discodeit.event.listener;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.event.MessageCreatedEvent;
import com.sprint.mission.discodeit.exception.message.MessageNotFoundException;
import com.sprint.mission.discodeit.mapper.MessageMapper;
import com.sprint.mission.discodeit.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebsocketRequiredEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessage(MessageCreatedEvent event) {
        UUID channelId = event.channelId();
        Message message = messageRepository.findById(event.id()).orElseThrow(() -> new MessageNotFoundException(event.id()));

        String destination = "/sub/channels." + channelId + ".messages";
        messagingTemplate.convertAndSend(destination, messageMapper.toDto(message));
        log.info("메세지를 보낸 채널: {}", destination);
    }
}
