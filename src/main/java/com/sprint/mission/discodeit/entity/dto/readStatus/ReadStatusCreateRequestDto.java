package com.sprint.mission.discodeit.entity.dto.readStatus;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ReadStatusCreateRequestDto {
    private UUID userId;
    private UUID channelId;
    private Instant lastReadAt;

    public ReadStatusCreateRequestDto(UUID channelId, UUID userId) {
        this.channelId = channelId;
        this.userId = userId;
    }
}
