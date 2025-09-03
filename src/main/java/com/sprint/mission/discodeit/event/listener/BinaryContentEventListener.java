package com.sprint.mission.discodeit.event.listener;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.event.BinaryContentCreatedEvent;
import com.sprint.mission.discodeit.exception.binaryContent.BinaryContentNotFoundException;
import com.sprint.mission.discodeit.mapper.BinaryContentMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.service.BinaryContentService;
import com.sprint.mission.discodeit.service.SseService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BinaryContentEventListener {

    private final BinaryContentStorage binaryContentStorage;
    private final BinaryContentService binaryContentService;
    private final ReadStatusRepository readStatusRepository;
    private final BinaryContentMapper binaryContentMapper;
    private final BinaryContentRepository binaryContentRepository;
    private final SseService sseService;

    @Async("fileTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBinaryContentCreatedEvent(BinaryContentCreatedEvent event) {
        UUID id = event.id();
        byte[] bytes = event.bytes();

        try {
            binaryContentStorage.put(id, bytes);

            binaryContentService.updateStatus(id, BinaryContentStatus.SUCCESS);
        } catch (Exception e) {
            binaryContentService.updateStatus(id, BinaryContentStatus.FAIL);
        }

        UUID channelId = event.channelId();
        List<UUID> userIds = readStatusRepository.findAllByChannelId(channelId).stream()
                .map(readStatus -> {
                    return readStatus.getUser().getId();
                })
                .toList();

        BinaryContentDto dto = binaryContentMapper.toDto(binaryContentRepository.findById(id).orElseThrow(() -> new BinaryContentNotFoundException(id)));

        sseService.send(userIds, "binaryContents.updated", dto);
    }
}
