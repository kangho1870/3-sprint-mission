package com.sprint.mission.discodeit.dto.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sprint.mission.discodeit.entity.ChannelType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChannelDto(
    UUID id,
    ChannelType type,
    String name,
    String description,
    List<UUID> participantIds,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant lastMessageAt
) {

}
