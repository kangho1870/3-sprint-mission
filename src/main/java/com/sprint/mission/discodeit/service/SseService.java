package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.data.SseMessage;
import com.sprint.mission.discodeit.repository.SseEmitterRepository;
import com.sprint.mission.discodeit.repository.SseMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseService {

    private final SseEmitterRepository sseEmitterRepository;
    private final SseMessageRepository sseMessageRepository;

    private static final long TIMEOUT = 1000L * 60 * 30;

    public SseEmitter connect(UUID receiverId, UUID lastEventId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        emitter.onCompletion(() -> sseEmitterRepository.delete(receiverId, emitter));
        emitter.onTimeout(() -> sseEmitterRepository.delete(receiverId, emitter));
        emitter.onError(e -> sseEmitterRepository.delete(receiverId, emitter));

        sseEmitterRepository.save(receiverId, emitter);

        log.info("sse emitter 생성");
        // 유실된 이벤트 복원
        if (lastEventId != null) {
            List<SseMessage> lostMessages = sseMessageRepository.findAllAfter(lastEventId);
            lostMessages.forEach(msg -> sendToEmitter(emitter, msg));
        } else {
            // 최초 연결 ping
            ping(emitter);
        }

        return emitter;
    }

    public void send(Collection<UUID> receiverIds, String eventName, Object data) {
        SseMessage message = new SseMessage(UUID.randomUUID(), eventName, data);
        sseMessageRepository.save(message);

        for (UUID receiverId : receiverIds) {
            for (SseEmitter emitter : sseEmitterRepository.get(receiverId)) {
                sendToEmitter(emitter, message);
                log.info("emitter 전송: {}", emitter);
            }
        }
    }

    public void broadcast(String eventName, Object data) {
        SseMessage message = new SseMessage(UUID.randomUUID(), eventName, data);
        sseMessageRepository.save(message);

        for (List<SseEmitter> emitters : sseEmitterRepository.findAll().values()) {
            for (SseEmitter emitter : emitters) {
                sendToEmitter(emitter, message);
                log.info("emitter : {}", emitter);
            }
        }
    }

    public void cleanUp() {
        for (Map.Entry<UUID, List<SseEmitter>> entry : sseEmitterRepository.findAll().entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                if (!ping(emitter)) {
                    sseEmitterRepository.delete(entry.getKey(), emitter);
                }
            }
        }
    }

    public boolean ping(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .id(UUID.randomUUID().toString())
                    .name("ping")
                    .data("keep-alive"));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void sendToEmitter(SseEmitter emitter, SseMessage message) {
        try {
            emitter.send(SseEmitter.event()
                    .id(message.id().toString())
                    .name(message.eventName())
                    .data(message.data()));
        } catch (IOException e) {
            // 연결 끊기면 삭제
        }
    }
}
