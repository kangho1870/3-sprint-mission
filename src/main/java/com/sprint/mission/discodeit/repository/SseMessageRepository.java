package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.dto.data.SseMessage;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Repository
public class SseMessageRepository {

    private final ConcurrentLinkedDeque<UUID> eventIdQueue = new ConcurrentLinkedDeque<>();
    private final Map<UUID, SseMessage> messages = new ConcurrentHashMap<>();
    private final int maxSize = 1000;

    public void save(SseMessage message) {
        messages.put(message.id(), message);
        eventIdQueue.addLast(message.id());

        if (eventIdQueue.size() > maxSize) {
            UUID oldest = eventIdQueue.pollFirst();
            if (oldest != null) {
                messages.remove(oldest);
            }
        }
    }

    public List<SseMessage> findAllAfter(UUID lastEventId) {
        List<SseMessage> result = new ArrayList<>();
        boolean found = (lastEventId == null);

        for (UUID id : eventIdQueue) {
            if (found) {
                result.add(messages.get(id));
            }
            if (id.equals(lastEventId)) {
                found = true;
            }
        }
        return result;
    }
}
