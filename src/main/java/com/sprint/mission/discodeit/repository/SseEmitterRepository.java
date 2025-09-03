package com.sprint.mission.discodeit.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
@Slf4j
public class SseEmitterRepository {

    private final ConcurrentMap<UUID, List<SseEmitter>> data = new ConcurrentHashMap<>();

    public void save(UUID receiverId, SseEmitter emitter) {
        data.computeIfAbsent(receiverId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("Emitter 저장: receiverId={}, 현재 emitter 수={}", receiverId, data.get(receiverId).size());
    }

    public List<SseEmitter> get(UUID receiverId) {
        List<SseEmitter> emitters = data.getOrDefault(receiverId, List.of());
        log.info("Emitter 조회: userId={}, 조회된 emitter 수={}", receiverId, emitters.size());
        return data.getOrDefault(receiverId, Collections.emptyList());
    }

    public void delete(UUID receiverId, SseEmitter emitter) {
        List<SseEmitter> emitters = data.get(receiverId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                data.remove(receiverId);
            }
        }
    }

    public Map<UUID, List<SseEmitter>> findAll() {
        return data;
    }
}
