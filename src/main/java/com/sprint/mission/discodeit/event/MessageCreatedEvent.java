package com.sprint.mission.discodeit.event;

import com.sprint.mission.discodeit.dto.data.MessageDto;

import java.util.UUID;

public record MessageCreatedEvent(
    UUID id,
    UUID channelId,
    UUID authorId,
    MessageDto message
) {
}
