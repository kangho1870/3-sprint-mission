package com.sprint.mission.discodeit.event;

import java.util.UUID;

public record ChannelEvent(
        String type,
        UUID channelId
) {
}
