package com.sprint.mission.discodeit.event.listener;

import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.event.ChannelEvent;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.mapper.ChannelMapper;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChannelEventListener {

    private final ChannelRepository channelRepository;
    private final ChannelMapper channelMapper;
    private final SseService sseService;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChannelEvent(ChannelEvent event) {
        ChannelDto dto = null;
        String eventName = "channels." + event.type();

        if (!event.type().equals("deleted")) {
            Channel channel = channelRepository.findById(event.channelId()).orElseThrow(() -> new ChannelNotFoundException(event.channelId()));
            dto = channelMapper.toDto(channel);
            sseService.broadcast(eventName, dto);
        } else {
            dto = new ChannelDto(event.channelId(), null, null, null, null, null);
            sseService.broadcast(eventName, dto);
        }

    }
}
