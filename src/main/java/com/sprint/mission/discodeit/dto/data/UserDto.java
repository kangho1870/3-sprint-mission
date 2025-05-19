package com.sprint.mission.discodeit.dto.data;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
    UUID id,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant createdAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant updatedAt,
    String username,
    String email,
    UUID profileId,
    Boolean online
) {

}
