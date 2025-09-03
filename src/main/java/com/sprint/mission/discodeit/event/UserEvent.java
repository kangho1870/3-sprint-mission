package com.sprint.mission.discodeit.event;

import java.util.UUID;

public record UserEvent(
        String type,
        UUID userId
) {
}
