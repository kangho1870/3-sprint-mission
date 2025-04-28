package com.sprint.mission.discodeit.entity.dto.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class MessageDeleteRequestDto {
    private UUID messageId;
    private UUID channelId;
    private UUID userId;
}
